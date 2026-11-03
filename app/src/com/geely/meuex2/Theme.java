package com.geely.meuex2;

import android.content.Context;
import android.content.res.Configuration;

/** Paleta estilo iOS, clara/escura conforme o sistema. */
public class Theme {
    public int bg, card, label, secondary, separator, green, blue, red, segBg, segSel, segSelTx, segTx;

    public static Theme of(Context c){
        boolean night = (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Theme t = new Theme();
        if (night){
            t.bg=0xFF000000; t.card=0xFF1C1C1E; t.label=0xFFFFFFFF; t.secondary=0xFF98989F;
            t.separator=0xFF38383A; t.green=0xFF30D158; t.blue=0xFF0A84FF; t.red=0xFFFF453A;
            t.segBg=0xFF2C2C2E; t.segSel=0xFF636366; t.segSelTx=0xFFFFFFFF; t.segTx=0xFFFFFFFF;
        } else {
            t.bg=0xFFF2F2F7; t.card=0xFFFFFFFF; t.label=0xFF000000; t.secondary=0xFF8E8E93;
            t.separator=0xFFD1D1D6; t.green=0xFF34C759; t.blue=0xFF007AFF; t.red=0xFFFF3B30;
            t.segBg=0xFFE4E4EA; t.segSel=0xFFFFFFFF; t.segSelTx=0xFF000000; t.segTx=0xFF000000;
        }
        return t;
    }
}
