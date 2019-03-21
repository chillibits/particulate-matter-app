package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeFormatter implements IAxisValueFormatter {
    private SimpleDateFormat sdf;

    public TimeFormatter() {
        sdf = new SimpleDateFormat("HH:mm");
    }


    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        Date date = new Date();
        date.setTime((long) value);
        return sdf.format(date);
    }
}
