/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.github.mikephil.charting.data.Entry;

public class DiagramEntry extends Entry {

    // Variables
    private String unit;

    public DiagramEntry(float x, float y, String unit) {
        super(x, y);
        this.unit = unit;
    }

    String getUnit() {
        return unit;
    }
}
