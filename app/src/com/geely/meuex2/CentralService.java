package com.geely.meuex2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.car.Car;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

/** Serviço persistente: vigia eventos do carro (porta→vidro agora; gear/ACC depois). */
public class CentralService extends Service {
    static final String T="MeuEX2";
    static final int DOOR=373295872;   // 1=aberta 2=fechada
    static final int WIN=322964416;     // 0..100
    static final Map<Integer,Integer> DOOR2WIN=new HashMap<>();
    static final Map<Integer,Integer> OPP=new HashMap<>();
    static {
        DOOR2WIN.put(1,16); DOOR2WIN.put(4,64); DOOR2WIN.put(16,256); DOOR2WIN.put(64,1024);
        OPP.put(16,64); OPP.put(64,16); OPP.put(256,1024); OPP.put(1024,256);
    }
    static final int GEAR=289408001;   // 1=N 2=R 4=P 8=D
    CarPropertyManager cpm;
    SharedPreferences prefs;
    final Map<Integer,Boolean> lastDoorOpen=new HashMap<>();
    boolean registered=false;
    // estado câmera-no-D (espelha a máquina de estado do Sapo)
    boolean gearInD=false, gearArmed=true;

    @Override public int onStartCommand(Intent i, int f, int id){
        startForeground(2, notif());
        prefs=Store.prefs(this);
        if(!registered) new Thread(this::setup).start();
        return START_STICKY;
    }

    void setup(){
        try{
            Car car=Car.createCar(this);
            for(int k=0;k<60 && !car.isConnected();k++){ try{Thread.sleep(100);}catch(Exception e){} }
            cpm=(CarPropertyManager)car.getCarManager(Car.PROPERTY_SERVICE);
            if(cpm==null){ Log.w(T,"central: CPM null"); return; }
            cpm.registerCallback(new DoorCb(), DOOR, 0f);
            GearCb gc=new GearCb();
            try{ cpm.registerCallback(gc, GEAR, 0f); }catch(Throwable e){}
            try{ cpm.registerCallback(gc, 289408000, 0f); }catch(Throwable e){}
            registered=true;
            Log.i(T,"central: vigiando porta->vidro + gear->câmera");
        }catch(Throwable t){ Log.w(T,"central setup",t); }
    }

    class DoorCb implements CarPropertyManager.CarPropertyEventCallback {
        @Override public void onChangeEvent(CarPropertyValue v){
            try{
                if(v==null || v.getPropertyId()!=DOOR) return;
                if(!prefs.getBoolean("window_on_door",false)) return;
                int area=v.getAreaId();
                Object val=v.getValue(); if(!(val instanceof Integer)) return;
                int door=((Integer)val).intValue();           // 1=aberta 2=fechada
                Integer winArea=DOOR2WIN.get(area); if(winArea==null) return;
                if(prefs.getBoolean("window_opposite",false)){ Integer o=OPP.get(winArea); if(o!=null) winArea=o; }

                boolean open; if(door==1) open=true; else if(door==2) open=false; else return;
                Boolean prev=lastDoorOpen.get(area);
                lastDoorOpen.put(area, open);
                if(prev!=null && prev.booleanValue()==open) return; // só na transição

                int pos; try{ pos=cpm.getIntProperty(WIN, winArea); }catch(Throwable e){ pos=-1; }
                if(open){
                    if(pos>7){ Log.i(T,"vidro já aberto (pos="+pos+") — não mexo"); return; }
                    cpm.setIntProperty(WIN, winArea, 10);
                    Log.i(T,"porta 0x"+Integer.toHexString(area)+" abriu → vidro 0x"+Integer.toHexString(winArea)+" = 10");
                } else {
                    if(pos>=0 && Math.abs(pos-10)>3){ Log.i(T,"vidro movido (pos="+pos+") — não fecho"); return; }
                    cpm.setIntProperty(WIN, winArea, 0);
                    Log.i(T,"porta 0x"+Integer.toHexString(area)+" fechou → vidro 0x"+Integer.toHexString(winArea)+" = 0");
                }
            }catch(Throwable e){ Log.w(T,"doorCb",e); }
        }
        @Override public void onErrorEvent(int prop, int zone){}
    }

    class GearCb implements CarPropertyManager.CarPropertyEventCallback {
        @Override public void onChangeEvent(CarPropertyValue v){
            try{
                if(v==null) return;
                Object val=v.getValue(); if(!(val instanceof Integer)) return;
                int g=((Integer)val).intValue();
                boolean inD = g>=8;   // 8 = D
                boolean fire;
                if(inD){ fire = gearArmed && !gearInD; gearInD=true; if(fire) gearArmed=false; }
                else { gearInD=false; gearArmed=true; fire=false; }
                if(fire && prefs.getBoolean("camera_on_drive",false)){
                    Log.i(T,"entrou em D → abrindo câmera (AVM)");
                    try{
                        Intent i=new Intent();
                        i.setComponent(new android.content.ComponentName("com.ecarx.parking","com.ecarx.parking.MainAvmActivity"));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }catch(Throwable e){ Log.w(T,"abrir câmera",e); }
                }
            }catch(Throwable e){ Log.w(T,"gearCb",e); }
        }
        @Override public void onErrorEvent(int prop, int zone){}
    }

    Notification notif(){
        NotificationManager nm=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null){
            NotificationChannel ch=new NotificationChannel("meuex2bg","Meu EX2 (fundo)",NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false); nm.createNotificationChannel(ch);
        }
        return new Notification.Builder(this,"meuex2bg")
                .setContentTitle("Meu EX2").setContentText("Monitorando o carro")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation).build();
    }
    @Override public IBinder onBind(Intent i){ return null; }
}
