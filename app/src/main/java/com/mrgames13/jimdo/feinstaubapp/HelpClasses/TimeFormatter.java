/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeFormatter extends ValueFormatter {

    // Variables as objects
    private SimpleDateFormat sdf;

    // Variables
    private long first_timestamp;

    public TimeFormatter(long first_timestamp) {
        sdf = new SimpleDateFormat("HH:mm");
        this.first_timestamp = first_timestamp;
    }

    @Override
    public String getAxisLabel(float value, AxisBase axis) {
        Date date = new Date();
        date.setTime((long) (value * 1000 + first_timestamp));
        return sdf.format(date);
    }
}
