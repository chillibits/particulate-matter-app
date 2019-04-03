package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.github.mikephil.charting.data.Entry;

public class DiagramEntry extends Entry {

    //Konstanten

    //Variablen als Objekte

    //Variablen
    private String unit;

    public DiagramEntry(float x, float y, String unit) {
        super(x, y);
        this.unit = unit;
    }

    public String getUnit() {
        return unit;
    }
}
