package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.IMarker;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.mrgames13.jimdo.feinstaubapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DiagramMarkerView extends MarkerView implements IMarker {

    //Konstanten

    //Variablen als Objekte
    private SimpleDateFormat sdf;
    private TextView time;
    private TextView value;

    private MPPointF mOffset;

    //Variablen
    private long first_timestamp;

    public DiagramMarkerView(Context context, int layoutResource, long first_timestamp) {
        super(context, layoutResource);
        sdf = new SimpleDateFormat("HH:mm:ss");
        this.first_timestamp = first_timestamp;
        time = findViewById(R.id.marker_time);
        value = findViewById(R.id.marker_value);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        try{
            DiagramEntry entry = (DiagramEntry) e;
            Date date = new Date((long) (e.getX() * 1000 + first_timestamp));
            time.setText(sdf.format(date));
            value.setText(e.getY() + " " + entry.getUnit());
        } catch (ClassCastException ex) {
            Date date = new Date((long) (e.getX() * 1000 + first_timestamp));
            time.setText(sdf.format(date));
            value.setText(String.valueOf(e.getY()));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        if(mOffset == null) mOffset = new MPPointF(-(getWidth() / 2), -getHeight() - 30);
        return mOffset;
    }
}