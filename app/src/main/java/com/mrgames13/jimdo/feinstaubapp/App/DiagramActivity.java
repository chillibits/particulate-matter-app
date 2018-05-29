package com.mrgames13.jimdo.feinstaubapp.App;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class DiagramActivity extends AppCompatActivity {

    //Konstanten
    public static final int MODE_SENSOR_DATA = 10001;
    public static final int MODE_COMPARE_DATA = 10002;

    //Variablen als Objekte
    private Resources res;
    private GraphView graph_view;
    private ArrayList<DataRecord> records;
    private ArrayList<ArrayList<DataRecord>> compare_records;
    private ArrayList<Sensor> compare_sensors;
    private SimpleDateFormat sdf_time = new SimpleDateFormat("HH:mm");

    //Variablen
    private boolean show_series_1;
    private boolean show_series_2;
    private boolean show_series_3;
    private boolean show_series_4;
    private boolean show_series_5;
    private int mode = MODE_SENSOR_DATA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagram);

        //Resourcen initialisieren
        res = getResources();

        //Intent-Extras auslesen
        Intent intent = getIntent();
        mode = intent.getIntExtra("Mode", MODE_SENSOR_DATA);
        show_series_1 = intent.hasExtra("Show1") && intent.getBooleanExtra("Show1", false);
        show_series_2 = intent.hasExtra("Show2") && intent.getBooleanExtra("Show2", false);
        show_series_3 = intent.hasExtra("Show3") && intent.getBooleanExtra("Show3", false);
        show_series_4 = intent.hasExtra("Show4") && intent.getBooleanExtra("Show4", false);
        show_series_5 = intent.hasExtra("Show5") && intent.getBooleanExtra("Show5", false);

        if(mode == MODE_SENSOR_DATA) {
            //Daten von der SensorActivity übernehmen
            records = SensorActivity.records;

            //Daten vorbereiten
            SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
            Collections.sort(records);
        } else if(mode == MODE_COMPARE_DATA) {
            compare_records = CompareActivity.records;
            compare_sensors = CompareActivity.sensors;

            //Daten vorbereiten
            SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
            for(ArrayList<DataRecord> current_records : compare_records) Collections.sort(current_records);
        }

        //Diagramm initialisieren
        try{
            graph_view = findViewById(R.id.diagram);

            //Initialisierungen am Viewport und an der Legende vornehmen
            if(mode == MODE_SENSOR_DATA) {
                graph_view.getViewport().setMinX(Math.abs(records.get(records.size() -1).getDateTime().getTime() - 1000000));
                graph_view.getViewport().setMaxX(Math.abs(records.get(records.size() -1).getDateTime().getTime()));
            } else if(mode == MODE_COMPARE_DATA) {
                long last_time = Long.MIN_VALUE;
                for(int i = 0; i < compare_sensors.size(); i++) {
                    try{
                        long current_last_time = compare_records.get(i).get(compare_records.get(i).size() -1).getDateTime().getTime();
                        last_time = current_last_time > last_time ? current_last_time : last_time;
                    } catch (Exception e) {}
                }
                graph_view.getViewport().setMinX(last_time - 1000000);
                graph_view.getViewport().setMaxX(last_time);
            }
            graph_view.getViewport().setScalable(true);
            graph_view.getViewport().setScrollable(true);
            graph_view.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
            graph_view.getLegendRenderer().setTextColor(res.getColor(R.color.white));
            graph_view.getLegendRenderer().setBackgroundColor(res.getColor(R.color.gray_transparent));
            graph_view.getLegendRenderer().setVisible(true);

            //Label-Formatter auf Zeit stellen
            graph_view.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if(isValueX) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis((long) value);
                        return sdf_time.format(cal.getTime());
                    } else {
                        return super.formatLabel(value, isValueX).replace(".000", "k");
                    }
                }
            });

            if(mode == MODE_SENSOR_DATA) {
                LineGraphSeries<DataPoint> series1 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> series3 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> series4 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> series5 = new LineGraphSeries<>();
                series1.setDrawDataPoints(true);
                series2.setDrawDataPoints(true);
                series3.setDrawDataPoints(true);
                series4.setDrawDataPoints(true);
                series5.setDrawDataPoints(true);
                series1.setDataPointsRadius(8);
                series2.setDataPointsRadius(8);
                series3.setDataPointsRadius(8);
                series4.setDataPointsRadius(8);
                series5.setDataPointsRadius(8);
                series1.setColor(res.getColor(R.color.series1));
                series2.setColor(res.getColor(R.color.series2));
                series3.setColor(res.getColor(R.color.series3));
                series4.setColor(res.getColor(R.color.series4));
                series5.setColor(res.getColor(R.color.series5));
                series1.setTitle(res.getString(R.string.value1));
                series2.setTitle(res.getString(R.string.value2));
                series3.setTitle(res.getString(R.string.temperature));
                series4.setTitle(res.getString(R.string.humidity));
                series5.setTitle(res.getString(R.string.pressure));
                for(DataRecord record : records) {
                    try{
                        series1.appendData(new DataPoint(record.getDateTime().getTime(), record.getSdsp1()), false, 1000000);
                        series2.appendData(new DataPoint(record.getDateTime().getTime(), record.getSdsp2()), false, 1000000);
                        series3.appendData(new DataPoint(record.getDateTime().getTime(), record.getTemp()), false, 1000000);
                        series4.appendData(new DataPoint(record.getDateTime().getTime(), record.getHumidity()), false, 1000000);
                        series5.appendData(new DataPoint(record.getDateTime().getTime(), record.getPressure()), false, 1000000);
                    } catch (Exception e) {}
                }
                series1.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        showDetailPopup(dataPoint);
                    }
                });
                series2.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        showDetailPopup(dataPoint);
                    }
                });
                series3.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        showDetailPopup(dataPoint);
                    }
                });
                series4.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        showDetailPopup(dataPoint);
                    }
                });
                series5.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        showDetailPopup(dataPoint);
                    }
                });
                if(show_series_1) graph_view.addSeries(series1);
                if(show_series_2) graph_view.addSeries(series2);
                if(show_series_3) graph_view.addSeries(series3);
                if(show_series_4) graph_view.addSeries(series4);
                if(show_series_5) graph_view.addSeries(series5);
            } else if(mode == MODE_COMPARE_DATA) {
                for(int i = 0; i < compare_sensors.size(); i++) {
                    LineGraphSeries<DataPoint> current_series = new LineGraphSeries<>();
                    current_series.setDrawDataPoints(true);
                    current_series.setDataPointsRadius(8);
                    current_series.setColor(compare_sensors.get(i).getColor());
                    if(show_series_1) {
                        current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.value1));
                    } else if(show_series_2) {
                        current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.value2));
                    } else if(show_series_3) {
                        current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.temperature));
                    } else if(show_series_4) {
                        current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.humidity));
                    } else if(show_series_5) {
                        current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.pressure));
                    }

                    for(DataRecord record : compare_records.get(i)) {
                        try{
                            if(show_series_1) {
                                current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getSdsp1()), false, 1000000);
                            } else if(show_series_2) {
                                current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getSdsp2()), false, 1000000);
                            } else if(show_series_3) {
                                current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getTemp()), false, 1000000);
                            } else if(show_series_4) {
                                current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getHumidity()), false, 1000000);
                            } else if(show_series_5) {
                                current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getPressure()), false, 1000000);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    current_series.setOnDataPointTapListener(new OnDataPointTapListener() {
                        @Override
                        public void onTap(Series series, DataPointInterface dataPoint) {
                            showDetailPopup(dataPoint);
                        }
                    });
                    graph_view.addSeries(current_series);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDetailPopup(DataPointInterface dataPoint) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((long) dataPoint.getX());

        View popup_layout = getLayoutInflater().inflate(R.layout.popup_layout, null);
        TextView time = popup_layout.findViewById(R.id.x_value);
        TextView value = popup_layout.findViewById(R.id.y_value);
        time.setText(res.getString(R.string.time_) + " " + sdf_time.format(cal.getTime()));
        value.setText(res.getString(R.string.value_) + " " + String.valueOf(dataPoint.getY()));

        PopupWindow popup = new PopupWindow();
        popup.setContentView(popup_layout);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setAnimationStyle(android.R.style.Animation_Dialog);
        popup.showAtLocation(graph_view, Gravity.NO_GRAVITY, 25, 100);
        popup.update();
    }
}