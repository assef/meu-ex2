package com.geely.meuex2;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Alerta estilo iOS (substitui o AlertDialog nativo). Claro/escuro conforme o tema. */
public class Modal {
    public interface OnConfirm { void run(); }

    static int dp(float d, float v){ return Math.round(v*d); }

    /** Confirmação com dois botões (Cancelar | Ação). destructive pinta a ação de vermelho. */
    public static void confirm(Context ctx, Theme t, String title, String message,
                               String confirmLabel, boolean destructive, OnConfirm onConfirm){
        final float d=ctx.getResources().getDisplayMetrics().density;

        LinearLayout card=new LinearLayout(ctx); card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(t.card); bg.setCornerRadius(dp(d,20));
        card.setBackground(bg); card.setClipToOutline(true);

        LinearLayout texts=new LinearLayout(ctx); texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(d,22),dp(d,22),dp(d,22),dp(d,18));
        TextView tv=new TextView(ctx); tv.setText(title); tv.setTextColor(t.label);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,18); tv.setGravity(Gravity.CENTER);
        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
        texts.addView(tv);
        if(message!=null && message.length()>0){
            TextView mv=new TextView(ctx); mv.setText(message); mv.setTextColor(t.secondary);
            mv.setTextSize(TypedValue.COMPLEX_UNIT_SP,14); mv.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(-1,-2); mlp.setMargins(0,dp(d,8),0,0);
            mv.setLayoutParams(mlp); texts.addView(mv);
        }
        card.addView(texts);

        View hsep=new View(ctx); hsep.setBackgroundColor(t.separator);
        card.addView(hsep, new LinearLayout.LayoutParams(-1,Math.max(1,dp(d,0.5f))));

        final Dialog dlg=new Dialog(ctx);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout btns=new LinearLayout(ctx); btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(d,52)));
        TextView cancel=btn(ctx,"Cancelar",t.blue,false,d);
        cancel.setOnClickListener(v->dlg.dismiss());
        TextView ok=btn(ctx,confirmLabel, destructive?t.red:t.blue, true, d);
        ok.setOnClickListener(v->{ dlg.dismiss(); if(onConfirm!=null) onConfirm.run(); });
        View vsep=new View(ctx); vsep.setBackgroundColor(t.separator);
        btns.addView(cancel);
        btns.addView(vsep,new LinearLayout.LayoutParams(Math.max(1,dp(d,0.5f)),-1));
        btns.addView(ok);
        card.addView(btns);

        LinearLayout wrap=new LinearLayout(ctx); wrap.setGravity(Gravity.CENTER);
        wrap.setPadding(dp(d,24),dp(d,24),dp(d,24),dp(d,24));
        wrap.addView(card, new LinearLayout.LayoutParams(dp(d,300),-2));

        dlg.setContentView(wrap);
        Window w=dlg.getWindow();
        if(w!=null) w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dlg.show();
    }

    static TextView btn(Context ctx, String label, int color, boolean bold, float d){
        TextView b=new TextView(ctx); b.setText(label); b.setTextColor(color);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP,17); b.setGravity(Gravity.CENTER);
        if(bold) b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1f));
        b.setClickable(true); b.setFocusable(true);
        return b;
    }
}
