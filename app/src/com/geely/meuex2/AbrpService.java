package com.geely.meuex2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/** Envia telemetria do carro pro ABRP (api.iternio.com). Chave/token vêm das prefs. */
public class AbrpService extends Service {
    static final String T="MeuEX2";
    static final String SEND="https://api.iternio.com/1/tlm/send";
    volatile boolean running=false;
    SharedPreferences prefs;
    CarController car;
    volatile Location lastLoc;

    @Override public int onStartCommand(Intent i,int f,int id){
        startForeground(3, notif());
        prefs=Store.prefs(this);
        if(!running){ running=true; startGps(); new Thread(this::loop).start(); }
        return START_STICKY;
    }

    void startGps(){
        try{
            LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);
            if(lm==null) return;
            LocationListener l=new LocationListener(){
                public void onLocationChanged(Location loc){ lastLoc=loc; }
                public void onStatusChanged(String p,int s,Bundle b){}
                public void onProviderEnabled(String p){}
                public void onProviderDisabled(String p){}
            };
            try{ lastLoc=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); }catch(Throwable e){}
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5, l);
        }catch(Throwable e){ Log.w(T,"gps",e); }
    }

    void loop(){
        car=new CarController(); car.connect(this);
        while(running){
            try{
                if(prefs.getBoolean("abrp_enabled",false)){
                    String token=prefs.getString("abrp_token","").trim();
                    String key=prefs.getString("abrp_key","").trim();
                    if(!token.isEmpty() && !key.isEmpty()){
                        JSONObject tlm=buildTlm();
                        boolean ok=send(token,key,tlm,null);
                        prefs.edit().putLong("abrp_last_ms",System.currentTimeMillis()).putBoolean("abrp_last_ok",ok).apply();
                    }
                }
            }catch(Throwable e){ Log.w(T,"abrp loop",e); }
            Integer g=car!=null?car.gear():null;
            boolean parked = g!=null && g==4;
            try{ Thread.sleep(parked?60000:6000); }catch(Exception e){}
        }
    }

    JSONObject buildTlm(){
        JSONObject t=new JSONObject();
        try{
            t.put("utc", System.currentTimeMillis()/1000);
            Float soc=car.socPct(); if(soc!=null) t.put("soc", Math.round(soc*10)/10.0);
            Float sp=car.speedKmh(); if(sp!=null) t.put("speed", Math.round(sp*10)/10.0);
            Boolean ch=car.isCharging(); if(ch!=null) t.put("is_charging", ch?1:0);
            Integer g=car.gear(); if(g!=null) t.put("is_parked", g==4?1:0);
            Float odo=car.odoKm(); if(odo!=null) t.put("odometer", Math.round(odo));
            Float rng=car.rangeKm(); if(rng!=null) t.put("est_battery_range", Math.round(rng));
            Float temp=car.extTempC(); if(temp!=null) t.put("ext_temp", temp);
            if(prefs.getBoolean("abrp_gps",true) && lastLoc!=null){
                t.put("lat", lastLoc.getLatitude()); t.put("lon", lastLoc.getLongitude());
                if(lastLoc.hasAltitude()) t.put("elevation", lastLoc.getAltitude());
                if(lastLoc.hasBearing()) t.put("heading", lastLoc.getBearing());
            }
        }catch(Throwable e){}
        return t;
    }

    boolean send(String token,String key,JSONObject tlm,StringBuilder outMsg){
        if(trySend(token,key,tlm,null,outMsg)) return true;
        // fallback: força a rede do TBox / celular
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if(cm!=null) for(Network n: cm.getAllNetworks()){
                NetworkCapabilities nc=cm.getNetworkCapabilities(n);
                LinkProperties lp=cm.getLinkProperties(n);
                String iff=lp!=null?lp.getInterfaceName():null;
                boolean net = nc!=null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                if(net && ("tbox0".equals(iff) || (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)))){
                    if(trySend(token,key,tlm,n,outMsg)) return true;
                }
            }
        }catch(Throwable e){ if(outMsg!=null) outMsg.append(" ").append(e); }
        return false;
    }

    boolean trySend(String token,String key,JSONObject tlm,Network net,StringBuilder outMsg){
        HttpURLConnection c=null;
        try{
            String url=SEND+"?token="+URLEncoder.encode(token,"UTF-8")+"&tlm="+URLEncoder.encode(tlm.toString(),"UTF-8");
            URL u=new URL(url);
            c=(HttpURLConnection)(net!=null?net.openConnection(u):u.openConnection());
            c.setConnectTimeout(8000); c.setReadTimeout(8000); c.setRequestMethod("POST");
            c.setRequestProperty("Authorization","APIKEY "+key);
            c.setDoOutput(true); OutputStream os=c.getOutputStream(); os.write(new byte[0]); os.close();
            int code=c.getResponseCode();
            if(outMsg!=null) outMsg.append("HTTP ").append(code);
            Log.i(T,"ABRP send HTTP "+code+(net!=null?" (tbox)":""));
            return code>=200 && code<300;
        }catch(Throwable e){ Log.w(T,"ABRP send",e); if(outMsg!=null) outMsg.append(e.getClass().getSimpleName()); return false; }
        finally{ if(c!=null) c.disconnect(); }
    }

    @Override public void onDestroy(){ running=false; super.onDestroy(); }
    @Override public IBinder onBind(Intent i){ return null; }

    Notification notif(){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(nm!=null){ NotificationChannel ch=new NotificationChannel("abrp","ABRP",NotificationManager.IMPORTANCE_MIN); ch.setShowBadge(false); nm.createNotificationChannel(ch); }
        return new Notification.Builder(this,"abrp").setContentTitle("Meu EX2 — ABRP").setContentText("Enviando telemetria").setSmallIcon(android.R.drawable.ic_menu_directions).build();
    }
}
