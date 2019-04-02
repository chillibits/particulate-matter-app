package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import android.content.Context;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.mrgames13.jimdo.feinstaubapp.R;

public class DiagramMarkerView extends MarkerView implements IMarker {

    //Konstanten

    //Variablen als Objekte
    private RelativeLayout container;
    private TextView time;
    private TextView value_p1;
    private TextView value_p2;
    private TextView temp;
    private TextView humidity;
    private TextView pressure;

    private MPPointF mOffset;

    //Variablen

    public DiagramMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        container = findViewById(R.id.time);
        time = findViewById(R.id.marker_value_p1);
        value_p1 = findViewById(R.id.marker_value_p2);
        value_p2 = findViewById(R.id.marker_value_temp);
        temp = findViewById(R.id.marker_value_humidity);
        pressure = findViewById(R.id.marker_value_pressure);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {




        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        if(mOffset == null) mOffset = new MPPointF(-(getWidth() / 2), -getHeight());
        return mOffset;
    }
}