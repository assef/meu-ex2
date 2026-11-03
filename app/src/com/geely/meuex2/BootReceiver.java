package com.geely.meuex2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Sobe o serviço de reaplicação ao ligar o carro / após atualizar o app. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent i){
        String a = i.getAction(); if(a==null) return;
        if (a.equals(Intent.ACTION_BOOT_COMPLETED)
                || a.equals("android.intent.action.ACTION_BOOT_HU")
                || a.equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            Log.i("MeuEX2", "boot: "+a+" -> ApplyService + CentralService");
            try { ctx.startForegroundService(new Intent(ctx, ApplyService.class)); }
            catch (Throwable e){ Log.w("MeuEX2","startForegroundService ApplyService",e); }
            try { ctx.startForegroundService(new Intent(ctx, CentralService.class)); }
            catch (Throwable e){ Log.w("MeuEX2","startForegroundService CentralService",e); }
            try { if(Store.prefs(ctx).getBoolean("abrp_enabled",false))
                    ctx.startForegroundService(new Intent(ctx, AbrpService.class)); }
            catch (Throwable e){ Log.w("MeuEX2","startForegroundService AbrpService",e); }
        }
    }
}
