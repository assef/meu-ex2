package com.geely.meuex2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    Theme t; UI ui; SharedPreferences prefs;
    final CarController car = new CarController();
    volatile boolean conn=false;
    final Handler main=new Handler(Looper.getMainLooper());

    UI.Segmented driveSeg, regenSeg;
    TextView driveNow, regenNow;
    IosToggle aebToggle, avasToggle;
    EditText abrpToken, abrpKey; TextView abrpStatus;

    static final String[] DRIVE={"Eco","Conforto","Sport"};
    static final String[] REGEN={"Baixa","Média","Alta"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        t=Theme.of(this); ui=new UI(this,t); prefs=Store.prefs(this);
        if(getIntent()!=null){
            String k=getIntent().getStringExtra("set_key"); if(k!=null) prefs.edit().putString("abrp_key",k).apply();
            String tk=getIntent().getStringExtra("set_token"); if(tk!=null) prefs.edit().putString("abrp_token",tk).apply();
            if("1".equals(getIntent().getStringExtra("set_abrp"))) prefs.edit().putBoolean("abrp_enabled",true).apply();
            if("1".equals(getIntent().getStringExtra("root_test")))
                new Thread(()->android.util.Log.i("MeuEX2","ROOTTEST id=["+RootShell.run(6000,"id")+"]")).start();
        }

        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(t.bg); scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int g=ui.dp(32); root.setPadding(g,ui.dp(22),g,ui.dp(28));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        setContentView(scroll);

        TextView title=ui.largeTitle("Meu EX2");
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-2,-2); tlp.setMargins(ui.dp(8),0,0,ui.dp(10)); title.setLayoutParams(tlp);
        root.addView(title);

        // ===== TOPO: Condução | Regeneração (mesmo escopo, uso frequente) =====
        LinearLayout cols1=new LinearLayout(this); cols1.setOrientation(LinearLayout.HORIZONTAL);
        cols1.setLayoutParams(new LinearLayout.LayoutParams(-1,-2)); root.addView(cols1);
        LinearLayout tl=column(), tr=column(); addCols(cols1,tl,tr);

        tl.addView(ui.sectionHeader("Condução"));
        LinearLayout c1=ui.card();
        driveNow=ui.valueLabel("—");
        c1.addView(ui.row("Modo", driveNow));
        c1.addView(ui.separator());
        driveSeg=ui.segmented(DRIVE, i-> { prefs.edit().putInt("val_drive",i+1).apply(); apply(()->car.setDrive(i+1)); });
        c1.addView(ui.controlRow(driveSeg));
        c1.addView(ui.separator());
        c1.addView(ui.row("Lembrar ao ligar", pref("rem_drive")));
        tl.addView(c1);

        tr.addView(ui.sectionHeader("Regeneração (freio motor)"));
        LinearLayout c2=ui.card();
        regenNow=ui.valueLabel("—");
        c2.addView(ui.row("Intensidade", regenNow));
        c2.addView(ui.separator());
        regenSeg=ui.segmented(REGEN, i-> { prefs.edit().putInt("val_regen",i+1).apply(); apply(()->car.setRegen(i+1)); });
        c2.addView(ui.controlRow(regenSeg));
        c2.addView(ui.separator());
        c2.addView(ui.row("Lembrar ao ligar", pref("rem_regen")));
        tr.addView(c2);

        // ===== ABAIXO: Segurança | Carro =====
        LinearLayout cols2=new LinearLayout(this); cols2.setOrientation(LinearLayout.HORIZONTAL);
        cols2.setLayoutParams(new LinearLayout.LayoutParams(-1,-2)); root.addView(cols2);
        LinearLayout left=column(), right=column(); addCols(cols2,left,right);

        left.addView(ui.sectionHeader("Segurança"));
        LinearLayout c3=ui.card();
        aebToggle=toggle(false); aebToggle.setOnToggle(on-> { prefs.edit().putBoolean("val_aeb",on).apply(); apply(()->car.setAeb(on)); });
        c3.addView(ui.row("Frenagem automática (AEB)", aebToggle));
        c3.addView(ui.separator());
        c3.addView(ui.row("Manter minha escolha ao ligar", pref("rem_aeb")));
        c3.addView(ui.separator());
        avasToggle=toggle(prefs.getBoolean("avas_mute",false));
        avasToggle.setOnToggle(on->{
            prefs.edit().putBoolean("avas_mute",on).apply();
            apply(()->{
                if(on){ Integer m=car.getAvasMode(); if(m!=null&&m>0) prefs.edit().putInt("avas_prev",m).apply(); car.setAvasMode(0); }
                else { int p=prefs.getInt("avas_prev",1); car.setAvasMode(p>0?p:1); }
            });
        });
        c3.addView(ui.row("Silenciar alerta de pedestre (AVAS)", avasToggle));
        left.addView(c3);
        left.addView(ui.footnote("Você pode desligar o AEB. “Manter ao ligar” reaplica sua escolha a cada partida."));

        right.addView(ui.sectionHeader("Carro"));
        LinearLayout cc=ui.card();
        LinearLayout wifiRow=ui.row("Abrir configuração de Wi-Fi", chevron());
        wifiRow.setOnClickListener(v-> openWifi());
        cc.addView(wifiRow);
        cc.addView(ui.separator());
        cc.addView(ui.row("Janela acompanha a porta", pref("window_on_door")));
        cc.addView(ui.separator());
        cc.addView(ui.row("Só a janela oposta", pref("window_opposite")));
        cc.addView(ui.separator());
        cc.addView(ui.row("Câmera ao engatar o D", pref("camera_on_drive")));
        right.addView(cc);
        right.addView(ui.footnote("Ao abrir a porta, o vidro abre um dedo (10%) e fecha ao fechar a porta."));

        // ===== Telemetria (ABRP) — largura cheia =====
        LinearLayout tcol=new LinearLayout(this); tcol.setOrientation(LinearLayout.VERTICAL);
        tcol.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
        tcol.addView(ui.sectionHeader("Telemetria (ABRP)"));
        LinearLayout tc=ui.card();
        IosToggle abrpEn=toggle(prefs.getBoolean("abrp_enabled",false));
        abrpEn.setOnToggle(on->{ prefs.edit().putBoolean("abrp_enabled",on).apply(); if(on) startAbrp(); });
        tc.addView(ui.row("Habilitar envio ao ABRP", abrpEn));
        tc.addView(ui.separator());
        abrpToken=field("Token do ABRP", prefs.getString("abrp_token",""));
        tc.addView(ui.controlRow(abrpToken));
        tc.addView(ui.separator());
        abrpKey=field("API key do ABRP", prefs.getString("abrp_key",""));
        tc.addView(ui.controlRow(abrpKey));
        tc.addView(ui.separator());
        tc.addView(ui.row("Enviar GPS", pref("abrp_gps")));
        tc.addView(ui.separator());
        Button test=new Button(this); test.setText("Salvar e testar");
        test.setAllCaps(false); test.setTextColor(t.blue);
        test.setBackground(null); test.setOnClickListener(v-> saveAndTestAbrp());
        tc.addView(ui.controlRow(test));
        abrpStatus=ui.footnote("Cole o token (do app ABRP → conexão Genérica) e a API key.");
        tcol.addView(tc); tcol.addView(abrpStatus);
        root.addView(tcol);

        // ===== Sistema =====
        LinearLayout scol=new LinearLayout(this); scol.setOrientation(LinearLayout.VERTICAL);
        scol.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
        scol.addView(ui.sectionHeader("Sistema"));
        LinearLayout sc=ui.card();
        LinearLayout rebootRow=ui.row("Reiniciar a central", chevron());
        rebootRow.setOnClickListener(v-> Modal.confirm(this, t,
            "Reiniciar a central?",
            "A tela desliga e volta em alguns segundos. Útil se a central travar.",
            "Reiniciar", false,
            ()-> new Thread(()->
                RootShell.run(8000,"svc power reboot 2>/dev/null || setprop sys.powerctl reboot 2>/dev/null || reboot")).start()));
        sc.addView(rebootRow);
        scol.addView(sc);
        root.addView(scol);

        String ver; try { ver="v"+getPackageManager().getPackageInfo(getPackageName(),0).versionName; } catch(Throwable e){ ver="v2.6"; }
        TextView foot=ui.footnote(ver+" · por Leonardo Assef · github @assef"); foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams flp=new LinearLayout.LayoutParams(-1,-2); flp.setMargins(0,ui.dp(20),0,0); foot.setLayoutParams(flp);
        root.addView(foot);

        try { startForegroundService(new Intent(this, CentralService.class)); } catch(Throwable e){}
        if(prefs.getBoolean("abrp_enabled",false)) startAbrp();
        new Thread(()->{ conn=car.connect(this); main.post(this::refresh); }).start();
        startRefreshLoop();
    }

    void openWifi(){
        try { startActivity(new Intent("android.settings.WIFI_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch(Throwable e){
            try { startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch(Throwable e2){}
        }
    }

    TextView chevron(){
        TextView tv=new TextView(this); tv.setText("›"); tv.setTextColor(t.secondary);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,26);
        return tv;
    }

    interface Act{ void run(); }
    void apply(Act a){ new Thread(()->{ try{a.run();}catch(Throwable e){} main.postDelayed(this::refresh,400);}).start(); }

    void startRefreshLoop(){ main.postDelayed(new Runnable(){ public void run(){ refresh(); main.postDelayed(this,4000);} },4000); }

    void refresh(){
        if(!conn) return;
        new Thread(()->{
            final int dl=car.driveLevel(), rl=car.regenLevel(), aeb=car.aebState();
            final Integer avas=car.getAvasMode();
            main.post(()->{
                driveNow.setText(dl>=1&&dl<=3?DRIVE[dl-1]:"—");
                regenNow.setText(rl>=1&&rl<=3?REGEN[rl-1]:"—");
                if(dl>=1&&dl<=3) driveSeg.setSelected(dl-1);
                if(rl>=1&&rl<=3) regenSeg.setSelected(rl-1);
                if(aeb>=0) aebToggle.setChecked(aeb==1,false);
                if(avas!=null) avasToggle.setChecked(avas==0,false);
            });
        }).start();
    }

    LinearLayout column(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }

    void addCols(LinearLayout cols, LinearLayout left, LinearLayout right){
        LinearLayout.LayoutParams lcp=new LinearLayout.LayoutParams(0,-2,1f); lcp.setMargins(0,0,ui.dp(14),0); left.setLayoutParams(lcp);
        LinearLayout.LayoutParams rcp=new LinearLayout.LayoutParams(0,-2,1f); rcp.setMargins(ui.dp(14),0,0,0); right.setLayoutParams(rcp);
        cols.addView(left); cols.addView(right);
    }

    EditText field(String hint, String value){
        EditText e=new EditText(this);
        e.setHint(hint); e.setText(value);
        e.setTextColor(t.label); e.setHintTextColor(t.secondary);
        e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,17);
        e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setSingleLine(true);
        e.setBackground(ui.rounded(t.segBg,10));
        e.setPadding(ui.dp(14),ui.dp(12),ui.dp(14),ui.dp(12));
        e.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
        return e;
    }

    void startAbrp(){ try{ startForegroundService(new Intent(this, AbrpService.class)); }catch(Throwable e){} }

    void saveAndTestAbrp(){
        String tok=abrpToken.getText().toString().trim(), key=abrpKey.getText().toString().trim();
        prefs.edit().putString("abrp_token",tok).putString("abrp_key",key).putBoolean("abrp_enabled",true).apply();
        abrpStatus.setText("Enviando teste…");
        startAbrp();
        main.postDelayed(()->{
            long ms=prefs.getLong("abrp_last_ms",0); boolean ok=prefs.getBoolean("abrp_last_ok",false);
            if(ms>0 && System.currentTimeMillis()-ms<15000)
                abrpStatus.setText(ok?"Enviado (OK) ✓ — confira no app ABRP":"Falhou — verifique token, API key e internet");
            else abrpStatus.setText("Sem resposta ainda — toque de novo em alguns segundos");
        }, 8000);
    }

    IosToggle toggle(boolean on){ IosToggle s=new IosToggle(this, t.green, t.separator); s.setChecked(on,false); return s; }

    IosToggle pref(final String key){
        IosToggle s=toggle(prefs.getBoolean(key,false));
        s.setOnToggle(on-> prefs.edit().putBoolean(key,on).apply());
        return s;
    }

    TextView soonRow(String label){
        TextView tv=new TextView(this); tv.setText(label); tv.setTextColor(t.secondary);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP,17);
        tv.setPadding(ui.dp(16),ui.dp(13),ui.dp(16),ui.dp(13));
        return tv;
    }
}
