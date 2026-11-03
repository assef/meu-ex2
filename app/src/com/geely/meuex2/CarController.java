package com.geely.meuex2;

import android.car.Car;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.Log;

/** Acesso ao veiculo via CarPropertyManager. IDs confirmados no IHU629G. */
public class CarController {
    static final String T = "MeuEX2";
    // Powertrain
    public static final int DRIVE = 570491136;   // Eco=+1 Comfort=+2 Sport=+3, areas 0/1
    public static final int REGEN = 537003264;   // Low=+1 Mid=+2 High=+3, areas 0/1
    // ADAS
    public static final int AEB   = 557858874;   // 0x2140403A on/off, area 0
    // Telemetria (ABRP)
    public static final int SOC   = 557885165;   // bateria % (float)
    public static final int SPEED = 291504647;   // km/h (float)
    public static final int PORT  = 557887621;   // 3=plug conectado
    public static final int CHG_A = 605291008;   // corrente de carga A (float)
    public static final int GEAR  = 289408001;   // 1=N 2=R 4=P 8=D
    public static final int ODO   = 289407492;   // km (float)
    public static final int RANGE = 289407752;   // km (float)
    public static final int AMB   = 557884279;   // temp ext: (raw-80)/2
    private CarPropertyManager cpm;

    private Float tryFloat(int id,int area){ try{ return cpm.getFloatProperty(id,area); }catch(Throwable e){ return null; } }

    public Float socPct(){ return tryFloat(SOC,0); }
    public Float speedKmh(){ Float v=tryFloat(SPEED,0); return v; }
    public Float odoKm(){ Float v=tryFloat(ODO,0); if(v==null)v=tryFloat(ODO,1); return v; }
    public Float rangeKm(){ Float v=tryFloat(RANGE,0); if(v==null)v=tryFloat(RANGE,1); return v; }
    public Integer gear(){ Integer v=tryInt(GEAR,0); if(v==null)v=tryInt(GEAR,1); return v; }
    public Float extTempC(){ Integer r=tryInt(AMB,0); return r==null?null:(float)((r-80)/2.0); }
    public Boolean isCharging(){
        Integer p=tryInt(PORT,0); Float a=tryFloat(CHG_A,0);
        if(p!=null && p==3) return true;
        if(a!=null && a>0.2f) return true;
        if(p!=null) return false;
        return null;
    }

    private Object audio;   // CarAudioManager (via reflection; não está no stub)

    public boolean connect(Context c) {
        try {
            Car car = Car.createCar(c);
            for (int i=0;i<40 && !car.isConnected();i++){ try{Thread.sleep(100);}catch(Exception e){} }
            cpm = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
            try { audio = car.getCarManager("audio"); } catch (Throwable e){ audio=null; }
            return cpm != null;
        } catch (Throwable t){ Log.w(T,"connect",t); return false; }
    }

    // AVAS (som de pedestre) via CarAudioManager: 0 = mudo, !=0 = algum som
    public Integer getAvasMode(){
        try{ return (Integer) audio.getClass().getMethod("getAVASMode").invoke(audio); }catch(Throwable e){ return null; }
    }
    public boolean setAvasMode(int mode){
        try{ audio.getClass().getMethod("setAVASMode", int.class).invoke(audio, mode); return true; }
        catch(Throwable e){ Log.w(T,"setAvasMode",e); return false; }
    }

    private Integer tryInt(int id, int area){
        try { return cpm.getIntProperty(id, area); } catch (Throwable e){ return null; }
    }
    // le em qualquer area 0/1 que responder
    public int driveLevel(){ Integer v=tryInt(DRIVE,0); if(v==null)v=tryInt(DRIVE,1); return v==null?-1:(v-DRIVE); }
    public int regenLevel(){ Integer v=tryInt(REGEN,0); if(v==null)v=tryInt(REGEN,1); return v==null?-1:(v-REGEN); }
    public int aebState(){
        try{ CarPropertyValue<?> v=cpm.getProperty(AEB,0); if(v==null)return -1;
             Object o=v.getValue(); if(o instanceof Boolean)return ((Boolean)o)?1:0; return ((Number)o).intValue()!=0?1:0;
        }catch(Throwable e){ return -1; }
    }

    // ---- escritas (so chamadas por acao explicita do usuario) ----
    public boolean setDrive(int level){ return writeIntBothAreas(DRIVE, DRIVE+level); }
    public boolean setRegen(int level){ return writeIntBothAreas(REGEN, REGEN+level); }
    public boolean setAeb(boolean on){
        try{ cpm.setIntProperty(AEB, 0, on?1:0); return true; }catch(Throwable e){ Log.w(T,"setAeb",e); return false; }
    }
    private boolean writeIntBothAreas(int id, int val){
        boolean ok=false;
        for(int a: new int[]{0,1}){ try{ cpm.setIntProperty(id,a,val); ok=true; }catch(Throwable e){} }
        return ok;
    }
}
