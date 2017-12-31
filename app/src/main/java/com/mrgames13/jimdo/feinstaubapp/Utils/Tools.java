package com.mrgames13.jimdo.feinstaubapp.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Tools {

    //Konstanten

    //Variablen als Objekte

    //Variablen

    public Tools() {

    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}