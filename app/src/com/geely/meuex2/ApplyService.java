package com.geely.meuex2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

/** No boot: reaplica modo/regeneração/AEB salvos, com retries (o carro demora a ficar pronto). */
public class ApplyService extends Service {
    static final String T="MeuEX2";

    @Override public int onStartCommand(Intent i, int flags, int id){
        startForeground(1, notif());
        new Thread(this::work).start();
        return START_NOT_STICKY;
    }

    void work(){
        SharedPreferences p = Store.prefs(this);
        boolean remD=p.getBoolean("rem_drive",false), remR=p.getBoolean("rem_regen",false), remA=p.getBoolean("rem_aeb",false);
        int vD=p.getInt("val_drive",-1), vR=p.getInt("val_regen",-1);
        boolean vA=p.getBoolean("val_aeb",true); boolean hasA=p.contains("val_aeb");
        boolean avasMute=p.getBoolean("avas_mute",false);

        if(!(remD&&vD>0) && !(remR&&vR>0) && !(remA&&hasA) && !avasMute){
            Log.i(T,"boot: nada pra reaplicar"); done(); return;
        }

        CarController car=new CarController();
        long start=SystemClock.elapsedRealtime();
        long[] schedule={500,1800,4200,8000,16000};
        boolean connected=false;
        for(long ts: schedule){
            long wait=start+ts-SystemClock.elapsedRealtime(); if(wait>0){ try{Thread.sleep(wait);}catch(Exception e){} }
            if(!connected) connected=car.connect(this);
            if(!connected) continue;
            try{
                if(remD&&vD>0) car.setDrive(vD);
                if(remR&&vR>0) car.setRegen(vR);
                if(remA&&hasA) car.setAeb(vA);
                if(avasMute) car.setAvasMode(0);
            }catch(Throwable e){ Log.w(T,"reapply",e); }
            // confere se já pegou; se sim, encerra cedo
            boolean ok=true;
            if(remD&&vD>0) ok &= car.driveLevel()==vD;
            if(remR&&vR>0) ok &= car.regenLevel()==vR;
            if(remA&&hasA) ok &= (car.aebState()==(vA?1:0));
            if(avasMute){ Integer am=car.getAvasMode(); ok &= (am!=null && am==0); }
            Log.i(T,"boot reapply t="+ts+" ok="+ok+" (D="+car.driveLevel()+" R="+car.regenLevel()+" AEB="+car.aebState()+")");
            if(ok) break;
        }
        done();
    }

    void done(){ try{ stopForeground(true); }catch(Throwable e){} stopSelf(); }

    Notification notif(){
        NotificationManager nm=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null){
            NotificationChannel ch=new NotificationChannel("meuex2","Meu EX2",NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false); nm.createNotificationChannel(ch);
        }
        return new Notification.Builder(this,"meuex2")
                .setContentTitle("Meu EX2")
                .setContentText("Aplicando suas preferências…")
                .setSmallIcon(android.R.drawable.ic_menu_preferences)
                .build();
    }

    @Override public IBinder onBind(Intent i){ return null; }
}
