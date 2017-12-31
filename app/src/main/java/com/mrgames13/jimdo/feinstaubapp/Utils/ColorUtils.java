package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.graphics.Color;

public class ColorUtils {

    //Konstanten

    //Variablen als Objekte

    //Variablen

    public ColorUtils() {

    }

    public static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    public static int addTransparency(int color) {
        int alpha = Math.round(Color.alpha(color) * 0.6f);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }
}