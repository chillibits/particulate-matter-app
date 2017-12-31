package com.mrgames13.jimdo.feinstaubapp.App;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class DiagramActivity extends AppCompatActivity {

    //Konstanten

    //Variablen als Objekte
    private Resources res;
    private GraphView graph_view;
    private ArrayList<DataRecord> records = SensorActivity.records;

    //Variablen
    private boolean show_series_1;
    private boolean show_series_2;
    private boolean show_series_3;
    private boolean show_series_4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagram);

        //Resourcen initialisieren
        res = getResources();

        //SimpleDateFormat initialisieren
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        //Daten vorbereiten
        SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
        Collections.sort(records);

        //Intent-Extras auslesen
        Intent i = getIntent();
        show_series_1 = i.hasExtra("Show1") && i.getBooleanExtra("Show1", false);
        show_series_2 = i.hasExtra("Show2") && i.getBooleanExtra("Show2", false);
        show_series_3 = i.hasExtra("Show3") && i.getBooleanExtra("Show3", false);
        show_series_4 = i.hasExtra("Show4") && i.getBooleanExtra("Show4", false);

        try{
            //Diagramm initialisieren
            graph_view = findViewById(R.id.diagram);

            long first_time = sdf.parse(records.get(0).getTime()).getTime() / 1000;

            graph_view.getViewport().setMinX(Math.abs(sdf.parse(records.get(records.size() -1).getTime()).getTime() / 1000 - first_time - 600));
            graph_view.getViewport().setMaxX(Math.abs(sdf.parse(records.get(records.size() -1).getTime()).getTime() / 1000 - first_time));
            graph_view.getViewport().setScalable(true);
            graph_view.getViewport().setScrollable(true);
            graph_view.getGridLabelRenderer().setHorizontalAxisTitle(res.getString(R.string.time));
            graph_view.getLegendRenderer().setVisible(true);
            graph_view.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
            graph_view.getLegendRenderer().setTextColor(res.getColor(R.color.white));
            graph_view.getLegendRenderer().setBackgroundColor(res.getColor(R.color.colorPrimary));

            LineGraphSeries<DataPoint> series1 = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> series3 = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> series4 = new LineGraphSeries<>();
            series1.setDrawDataPoints(true);
            series2.setDrawDataPoints(true);
            series3.setDrawDataPoints(true);
            series4.setDrawDataPoints(true);
            series1.setDataPointsRadius(10);
            series2.setDataPointsRadius(10);
            series3.setDataPointsRadius(10);
            series4.setDataPointsRadius(10);
            series1.setColor(res.getColor(R.color.series1));
            series2.setColor(res.getColor(R.color.series2));
            series3.setColor(res.getColor(R.color.series3));
            series4.setColor(res.getColor(R.color.series4));
            series1.setTitle(res.getString(R.string.value1));
            series2.setTitle(res.getString(R.string.value2));
            series3.setTitle(res.getString(R.string.temperature));
            series4.setTitle(res.getString(R.string.humidity));
            for(DataRecord record : records) {
                try{
                    Date time = sdf.parse(record.getTime());
                    series1.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getSdsp1()), false, 1000000);
                    series2.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getSdsp2()), false, 1000000);
                    series3.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getTemp()), false, 1000000);
                    series4.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getHumidity()), false, 1000000);
                } catch (Exception e) {}
            }
            series1.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    showDetailPopup(series, dataPoint);
                }
            });
            series2.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    showDetailPopup(series, dataPoint);
                }
            });
            series3.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    showDetailPopup(series, dataPoint);
                }
            });
            series4.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    showDetailPopup(series, dataPoint);
                }
            });
            if(show_series_1) graph_view.addSeries(series1);
            if(show_series_2) graph_view.addSeries(series2);
            if(show_series_3) graph_view.addSeries(series3);
            if(show_series_4) graph_view.addSeries(series4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDetailPopup(Series series, DataPointInterface dataPoint) {
        View popup_layout = getLayoutInflater().inflate(R.layout.popup_layout, null);

        TextView info = popup_layout.findViewById(R.id.text);
        info.setText(String.valueOf(dataPoint.getX()));

        PopupWindow popUp = new PopupWindow();
        popUp.setContentView(popup_layout);
        popUp.setFocusable(true);
        popUp.setOutsideTouchable(true);
        popUp.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popUp.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popUp.showAsDropDown(popup_layout);
                    /*if (android.os.Build.VERSION.SDK_INT >=24) {
                        int[] a = new int[2];
                        popup_layout.getLocationInWindow(a);
                        popUp.showAtLocation(getWindow().getDecorView(), Gravity.NO_GRAVITY, 0 , a[1] + popup_layout.getHeight());
                    } else{
                        popUp.showAsDropDown(popup_layout);
                    }*/
        popUp.update();
    }
}