package com.geely.meuex2;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Toggle estilo iOS, desenhado e animado. */
public class IosToggle extends View {
    public interface OnToggle { void onToggle(boolean on); }
    private boolean checked=false;
    private float pos=0f; // 0=off 1=on
    private int onColor, offColor;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private OnToggle listener;
    private final float d;

    public IosToggle(Context c, int onColor, int offColor){
        super(c); this.onColor=onColor; this.offColor=offColor;
        this.d=c.getResources().getDisplayMetrics().density;
    }
    public void setOnToggle(OnToggle l){ this.listener=l; }

    public void setChecked(boolean on, boolean notify){
        if(on==checked){ pos=on?1f:0f; invalidate(); return; }
        checked=on; animateTo(on?1f:0f);
        if(notify && listener!=null) listener.onToggle(on);
    }
    public boolean isChecked(){ return checked; }

    private void animateTo(float target){
        ValueAnimator a=ValueAnimator.ofFloat(pos,target); a.setDuration(180);
        a.addUpdateListener(an->{ pos=(float)an.getAnimatedValue(); invalidate(); });
        a.start();
    }

    @Override protected void onMeasure(int w,int h){
        setMeasuredDimension(Math.round(62*d), Math.round(37*d));
    }

    @Override protected void onDraw(Canvas cv){
        float w=getWidth(), h=getHeight(), r=h/2f;
        p.setColor(blend(offColor,onColor,pos));
        cv.drawRoundRect(new RectF(0,0,w,h), r, r, p);
        float pad=2*d, tr=r-pad;
        float cx = (r) + pos*(w-2*r);
        // sombra leve
        p.setColor(0x33000000); cv.drawCircle(cx, h/2f+1*d, tr, p);
        p.setColor(Color.WHITE); cv.drawCircle(cx, h/2f, tr, p);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_UP){ setChecked(!checked, true); performClick(); }
        return true;
    }
    @Override public boolean performClick(){ super.performClick(); return true; }

    private static int blend(int c0,int c1,float f){
        int a=(int)(Color.alpha(c0)+(Color.alpha(c1)-Color.alpha(c0))*f);
        int r=(int)(Color.red(c0)+(Color.red(c1)-Color.red(c0))*f);
        int g=(int)(Color.green(c0)+(Color.green(c1)-Color.green(c0))*f);
        int b=(int)(Color.blue(c0)+(Color.blue(c1)-Color.blue(c0))*f);
        return Color.argb(a,r,g,b);
    }
}
