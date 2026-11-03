package com.geely.meuex2;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Componentes estilo iOS, dimensionados pra tela da central (grande, horizontal). */
public class UI {
    final Context c; final Theme t; final float d;
    public UI(Context c, Theme t){ this.c=c; this.t=t; this.d=c.getResources().getDisplayMetrics().density; }
    public int dp(float v){ return Math.round(v*d); }

    public GradientDrawable rounded(int color, float radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }

    public TextView largeTitle(String s){
        TextView tv=new TextView(c); tv.setText(s);
        tv.setTextColor(t.label); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,40);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    public TextView sectionHeader(String s){
        TextView tv=new TextView(c); tv.setText(s.toUpperCase());
        tv.setTextColor(t.secondary); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);
        tv.setAllCaps(true); tv.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(dp(8),dp(20),dp(8),dp(8)); tv.setLayoutParams(lp);
        return tv;
    }

    public TextView footnote(String s){
        TextView tv=new TextView(c); tv.setText(s);
        tv.setTextColor(t.secondary); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(dp(8),dp(8),dp(8),dp(4)); tv.setLayoutParams(lp);
        return tv;
    }

    public LinearLayout card(){
        LinearLayout ll=new LinearLayout(c); ll.setOrientation(LinearLayout.VERTICAL);
        ll.setBackground(rounded(t.card,18)); ll.setClipToOutline(true);
        return ll;
    }

    public View separator(){
        View v=new View(c); v.setBackgroundColor(t.separator);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Math.max(1,dp(0.5f))); lp.setMargins(dp(20),0,0,0); v.setLayoutParams(lp);
        return v;
    }

    public LinearLayout row(String title, View control){
        LinearLayout r=new LinearLayout(c); r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(20),dp(16),dp(20),dp(16));
        r.setMinimumHeight(dp(66));
        TextView tv=new TextView(c); tv.setText(title); tv.setTextColor(t.label); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,21);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(0,-2,1f); tv.setLayoutParams(tlp);
        r.addView(tv);
        if(control!=null) r.addView(control);
        return r;
    }

    public TextView valueLabel(String s){
        TextView tv=new TextView(c); tv.setText(s); tv.setTextColor(t.secondary); tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,21);
        return tv;
    }

    public interface OnSelect { void sel(int i); }

    public class Segmented extends LinearLayout {
        final TextView[] segs; int selected=-1;
        public Segmented(String[] opts, OnSelect cb){
            super(UI.this.c); setOrientation(HORIZONTAL);
            setBackground(rounded(t.segBg,11)); int p=dp(3); setPadding(p,p,p,p);
            segs=new TextView[opts.length];
            for(int i=0;i<opts.length;i++){
                final int idx=i; TextView s=new TextView(UI.this.c); s.setText(opts[i]);
                s.setGravity(Gravity.CENTER); s.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                s.setTextColor(t.segTx); s.setPadding(0,dp(11),0,dp(11));
                LayoutParams lp=new LayoutParams(0,-2,1f); if(i>0) lp.setMargins(dp(3),0,0,0); s.setLayoutParams(lp);
                s.setOnClickListener(v->{ setSelected(idx); if(cb!=null) cb.sel(idx); });
                segs[i]=s; addView(s);
            }
        }
        public void setSelected(int i){
            if(i==selected) return; selected=i;
            for(int k=0;k<segs.length;k++){
                if(k==i){ segs[k].setBackground(rounded(t.segSel,9)); segs[k].setTextColor(t.segSelTx);
                          segs[k].setTypeface(segs[k].getTypeface(),android.graphics.Typeface.BOLD); }
                else { segs[k].setBackground(null); segs[k].setTextColor(t.segTx);
                       segs[k].setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.NORMAL)); }
            }
        }
    }
    /** segmented ocupando a largura cheia do card */
    public Segmented segmented(String[] opts, OnSelect cb){
        Segmented s=new Segmented(opts,cb);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); s.setLayoutParams(lp); return s;
    }

    /** envolve um controle ocupando a largura, com respiro */
    public LinearLayout controlRow(View v){
        LinearLayout r=new LinearLayout(c); r.setOrientation(LinearLayout.HORIZONTAL);
        r.setPadding(dp(20),dp(6),dp(20),dp(16));
        r.addView(v);
        return r;
    }
}
