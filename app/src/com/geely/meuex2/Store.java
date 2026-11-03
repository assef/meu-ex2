package com.geely.meuex2;

import android.content.Context;
import android.content.SharedPreferences;

/** Prefs em device-protected storage: legíveis já no boot (antes de desbloquear). */
public class Store {
    public static SharedPreferences prefs(Context c){
        try { return c.createDeviceProtectedStorageContext().getSharedPreferences("meuex2", Context.MODE_PRIVATE); }
        catch (Throwable e){ return c.getSharedPreferences("meuex2", Context.MODE_PRIVATE); }
    }
}
