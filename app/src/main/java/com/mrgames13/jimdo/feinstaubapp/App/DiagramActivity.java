package com.mrgames13.jimdo.feinstaubapp.App;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagram);

        //Resourcen initialisieren
        res = getResources();

        //Intent-Extras auslesen
        Intent intent = getIntent();
        int mode = intent.getIntExtra("Mode", MODE_SENSOR_DATA);
        //Variablen
        boolean show_series_1 = intent.hasExtra("Show1") && intent.getBooleanExtra("Show1", false);
        boolean show_series_2 = intent.hasExtra("Show2") && intent.getBooleanExtra("Show2", false);
        boolean show_series_3 = intent.hasExtra("Show3") && intent.getBooleanExtra("Show3", false);
        boolean show_series_4 = intent.hasExtra("Show4") && intent.getBooleanExtra("Show4", false);
        boolean show_series_5 = intent.hasExtra("Show5") && intent.getBooleanExtra("Show5", false);
        boolean enable_average = intent.hasExtra("EnableAverage") && intent.getBooleanExtra("EnableAverage", false);
        boolean enable_median = intent.hasExtra("EnableMedian") && intent.getBooleanExtra("EnableMedian", false);
        boolean enable_threshold_who = intent.hasExtra("EnableThresholdWHO") && intent.getBooleanExtra("EnableThresholdWHO", false);
        boolean enable_threshold_eu = intent.hasExtra("EnableThresholdEU") && intent.getBooleanExtra("EnableThresholdEU", false);

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
            /*LineChart chart = findViewById(R.id.chart);
            chart.setKeepScreenOn(true);
            chart.setKeepPositionOnRotation(true);
            chart.setDescription(null);
            YAxis left = chart.getAxisLeft();
            left.setValueFormatter(new LargeValueFormatter());
            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new TimeFormatter());

            //Daten eintragen
            List<Entry> entries_1 = new ArrayList<>();
            List<Entry> entries_2 = new ArrayList<>();
            List<Entry> entries_3 = new ArrayList<>();
            List<Entry> entries_4 = new ArrayList<>();
            List<Entry> entries_5 = new ArrayList<>();
            for (DataRecord r : records) {
                if(show_series_1) entries_1.add(new Entry((float) r.getDateTime().getTime(), r.getP1().floatValue()));
                if(show_series_2) entries_2.add(new Entry((float) r.getDateTime().getTime(), r.getP2().floatValue()));
                if(show_series_3) entries_3.add(new Entry((float) r.getDateTime().getTime(), r.getTemp().floatValue()));
                if(show_series_4) entries_4.add(new Entry((float) r.getDateTime().getTime(), r.getHumidity().floatValue()));
                if(show_series_5) entries_5.add(new Entry((float) r.getDateTime().getTime(), r.getPressure().floatValue()));
            }

            //PM1
            LineDataSet p1 = new LineDataSet(entries_1, res.getString(R.string.value1));
            p1.setColor(res.getColor(R.color.series1));
            p1.setCircleColor(res.getColor(R.color.series1));
            p1.setLineWidth(2);
            p1.setDrawValues(false);

            //PM2
            LineDataSet p2 = new LineDataSet(entries_2, res.getString(R.string.value2));
            p2.setColor(res.getColor(R.color.series2));
            p2.setCircleColor(res.getColor(R.color.series2));
            p2.setLineWidth(2);
            p2.setDrawValues(false);

            //Temperature
            LineDataSet temp = new LineDataSet(entries_3, res.getString(R.string.temperature));
            temp.setColor(res.getColor(R.color.series3));
            temp.setCircleColor(res.getColor(R.color.series3));
            temp.setLineWidth(2);
            temp.setDrawValues(false);

            //Humidity
            LineDataSet humidity = new LineDataSet(entries_4, res.getString(R.string.humidity));
            humidity.setColor(res.getColor(R.color.series4));
            humidity.setCircleColor(res.getColor(R.color.series4));
            humidity.setLineWidth(2);
            humidity.setDrawValues(false);


            //Pressure
            LineDataSet pressure = new LineDataSet(entries_5, res.getString(R.string.pressure));
            pressure.setColor(res.getColor(R.color.series5));
            pressure.setCircleColor(res.getColor(R.color.series5));
            pressure.setLineWidth(2);
            pressure.setDrawValues(false);

            List<ILineDataSet> dataSets = new ArrayList<>();
            if(show_series_1) dataSets.add(p1);
            if(show_series_2) dataSets.add(p2);
            if(show_series_3) dataSets.add(temp);
            if(show_series_4) dataSets.add(humidity);
            if(show_series_5) dataSets.add(pressure);
            LineData lineData = new LineData(dataSets);
            chart.setData(lineData);
            //chart.setMarker(new MarkerView(this, ));
            chart.invalidate();
            chart.animateY(700, Easing.EasingOption.EaseInCubic);*/




            graph_view = findViewById(R.id.diagram);

            //Initialisierungen am Viewport und an der Legende vornehmen
            if(mode == MODE_SENSOR_DATA) {
                graph_view.getViewport().setMinX(Math.abs(records.get(0).getDateTime().getTime()));
                graph_view.getViewport().setMaxX(Math.abs(records.get(records.size() -1).getDateTime().getTime()));
            } else if(mode == MODE_COMPARE_DATA) {
                long last_time = Long.MIN_VALUE;
                long first_time = Long.MAX_VALUE;
                for(int i = 0; i < compare_sensors.size(); i++) {
                    try{
                        long current_last_time = compare_records.get(i).get(compare_records.get(i).size() -1).getDateTime().getTime();
                        long current_first_time = compare_records.get(i).get(0).getDateTime().getTime();
                        last_time = current_last_time > last_time ? current_last_time : last_time;
                        first_time = current_first_time < first_time ? current_first_time : first_time;
                    } catch (Exception e) {}
                }
                graph_view.getViewport().setMinX(first_time);
                graph_view.getViewport().setMaxX(last_time);
            }
            graph_view.getViewport().setScalable(true);
            graph_view.getViewport().setScrollable(true);
            graph_view.getGridLabelRenderer().setHorizontalAxisTitle(getString(R.string.date_time));
            if(!enable_average && !enable_median) {
                graph_view.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
                graph_view.getLegendRenderer().setTextColor(res.getColor(R.color.white));
                graph_view.getLegendRenderer().setBackgroundColor(res.getColor(R.color.gray_transparent));
                graph_view.getLegendRenderer().setVisible(true);
            }

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
                LineGraphSeries<DataPoint> series1 = drawSeries(res.getString(R.string.value1) + " (µg/m³)", res.getColor(R.color.series1));
                LineGraphSeries<DataPoint> series2 = drawSeries(res.getString(R.string.value2) + " (µg/m³)", res.getColor(R.color.series2));
                LineGraphSeries<DataPoint> series3 = drawSeries(res.getString(R.string.temperature) + " (°C)", res.getColor(R.color.series3));
                LineGraphSeries<DataPoint> series4 = drawSeries(res.getString(R.string.humidity) + " (%)", res.getColor(R.color.series4));
                LineGraphSeries<DataPoint> series5 = drawSeries(res.getString(R.string.pressure) + " (hPa)", res.getColor(R.color.series5));

                for(DataRecord record : records) {
                    try{
                        series1.appendData(new DataPoint(record.getDateTime().getTime(), record.getP1()), false, 1000000);
                        series2.appendData(new DataPoint(record.getDateTime().getTime(), record.getP2()), false, 1000000);
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
                long first_time = records.get(0).getDateTime().getTime();
                if(show_series_1) {
                    graph_view.addSeries(series1);
                    LineGraphSeries<DataPoint> series1_average_median;
                    if(enable_average) {
                        //Mittelwert einzeichnen
                        double average = 0;
                        for(DataRecord record : records) average+=record.getP1();
                        average /= records.size();
                        series1_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series1));
                        graph_view.addSeries(series1_average_median);
                    } else if(enable_median) {
                        //Median einzeichnen
                        ArrayList<Double> double_records = new ArrayList<>();
                        for(DataRecord record : records) double_records.add(record.getP1());
                        double median = Tools.calculateMedian(double_records);
                        series1_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series1));
                        graph_view.addSeries(series1_average_median);
                    }

                    LineGraphSeries<DataPoint> series1_threshold;
                    if(enable_threshold_who) {
                        series1_threshold = drawHorizontalLine(Constants.THRESHOLD_WHO_PM10, first_time, Color.RED);
                        graph_view.addSeries(series1_threshold);
                    } else if(enable_threshold_eu) {
                        series1_threshold = drawHorizontalLine(Constants.THRESHOLD_EU_PM10, first_time, Color.RED);
                        graph_view.addSeries(series1_threshold);
                    }
                }
                if(show_series_2) {
                    graph_view.addSeries(series2);
                    LineGraphSeries<DataPoint> series2_average_median;
                    if(enable_average) {
                        //Mittelwert einzeichnen
                        double average = 0;
                        for(DataRecord record : records) average+=record.getP2();
                        average /= records.size();
                        series2_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series2));
                        graph_view.addSeries(series2_average_median);
                    } else if(enable_median) {
                        //Median einzeichnen
                        ArrayList<Double> double_records = new ArrayList<>();
                        for(DataRecord record : records) double_records.add(record.getP2());
                        double median = Tools.calculateMedian(double_records);
                        series2_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series2));
                        graph_view.addSeries(series2_average_median);
                    }

                    LineGraphSeries<DataPoint> series2_threshold;
                    if(enable_threshold_who) {
                        series2_threshold = drawHorizontalLine(Constants.THRESHOLD_WHO_PM2_5, first_time, Color.RED);
                        graph_view.addSeries(series2_threshold);
                    } else if(enable_threshold_eu) {
                        series2_threshold = drawHorizontalLine(Constants.THRESHOLD_EU_PM2_5, first_time, Color.RED);
                        graph_view.addSeries(series2_threshold);
                    }
                }
                if(show_series_3) {
                    graph_view.addSeries(series3);
                    LineGraphSeries<DataPoint> series3_average_median;
                    if(enable_average) {
                        //Mittelwert einzeichnen
                        double average = 0;
                        for(DataRecord record : records) average+=record.getTemp();
                        average /= records.size();
                        series3_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series3));
                        graph_view.addSeries(series3_average_median);
                    } else if(enable_median) {
                        //Median einzeichnen
                        ArrayList<Double> double_records = new ArrayList<>();
                        for(DataRecord record : records) double_records.add(record.getTemp());
                        double median = Tools.calculateMedian(double_records);
                        series3_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series3));
                        graph_view.addSeries(series3_average_median);
                    }
                }
                if(show_series_4) {
                    graph_view.addSeries(series4);
                    LineGraphSeries<DataPoint> series4_average_median;
                    if(enable_average) {
                        //Mittelwert einzeichnen
                        double average = 0;
                        for(DataRecord record : records) average+=record.getHumidity();
                        average /= records.size();
                        series4_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series4));
                        graph_view.addSeries(series4_average_median);
                    } else if(enable_median) {
                        //Median einzeichnen
                        ArrayList<Double> double_records = new ArrayList<>();
                        for(DataRecord record : records) double_records.add(record.getHumidity());
                        double median = Tools.calculateMedian(double_records);
                        series4_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series4));
                        graph_view.addSeries(series4_average_median);
                    }
                }
                if(show_series_5) {
                    graph_view.addSeries(series5);
                    LineGraphSeries<DataPoint> series5_average_median;
                    if(enable_average) {
                        //Mittelwert einzeichnen
                        double average = 0;
                        for(DataRecord record : records) average+=record.getPressure();
                        average /= records.size();
                        series5_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series5));
                        graph_view.addSeries(series5_average_median);
                    } else if(enable_median) {
                        //Median einzeichnen
                        ArrayList<Double> double_records = new ArrayList<>();
                        for(DataRecord record : records) double_records.add(record.getPressure());
                        double median = Tools.calculateMedian(double_records);
                        series5_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series5));
                        graph_view.addSeries(series5_average_median);
                    }
                }
            } else if(mode == MODE_COMPARE_DATA) {
                for(int i = 0; i < compare_sensors.size(); i++) {
                    if(compare_records.get(i).size() > 0) {
                        LineGraphSeries<DataPoint> current_series = new LineGraphSeries<>();
                        current_series.setDrawDataPoints(true);
                        current_series.setDataPointsRadius(8);
                        current_series.setColor(compare_sensors.get(i).getColor());
                        if(show_series_1) {
                            graph_view.getGridLabelRenderer().setVerticalAxisTitle("µg/m³");
                            current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.value1));
                        } else if(show_series_2) {
                            graph_view.getGridLabelRenderer().setVerticalAxisTitle("µg/m³");
                            current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.value2));
                        } else if(show_series_3) {
                            graph_view.getGridLabelRenderer().setVerticalAxisTitle("°C");
                            current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.temperature));
                        } else if(show_series_4) {
                            graph_view.getGridLabelRenderer().setVerticalAxisTitle("%");
                            current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.humidity));
                        } else if(show_series_5) {
                            graph_view.getGridLabelRenderer().setVerticalAxisTitle("hPa");
                            current_series.setTitle(compare_sensors.get(i).getName() + " - " + res.getString(R.string.pressure));
                        }

                        for(DataRecord record : compare_records.get(i)) {
                            try{
                                if(show_series_1) {
                                    current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getP1()), false, 1000000);
                                } else if(show_series_2) {
                                    current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getP2()), false, 1000000);
                                } else if(show_series_3) {
                                    current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getTemp()), false, 1000000);
                                } else if(show_series_4) {
                                    current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getHumidity()), false, 1000000);
                                } else if(show_series_5) {
                                    current_series.appendData(new DataPoint(record.getDateTime().getTime(), record.getPressure()), false, 1000000);
                                }
                            } catch (Exception e) {}
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LineGraphSeries<DataPoint> drawSeries(String title, int color) {
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(8);
        series.setColor(color);
        series.setTitle(title);
        series.setAnimated(true);
        return series;
    }

    private LineGraphSeries<DataPoint> drawHorizontalLine(double value, long first_time, int color) {
        LineGraphSeries series = new LineGraphSeries<>();
        series.appendData(new DataPoint(first_time, value), false, 2);
        series.appendData(new DataPoint(records.get(records.size() -1).getDateTime().getTime(), value), false, 2);

        Paint p = new Paint();
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setPathEffect(new DashPathEffect(new float[] { 20, 10 }, 0));
        p.setStrokeWidth(3);

        series.setDrawAsPath(true);
        series.setDrawDataPoints(false);
        series.setCustomPaint(p);
        series.setDrawDataPoints(false);
        series.setAnimated(true);
        return series;
    }

    private void showDetailPopup(DataPointInterface dataPoint) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((long) dataPoint.getX());

        View popup_layout = getLayoutInflater().inflate(R.layout.popup_layout, null);
        TextView time = popup_layout.findViewById(R.id.x_value);
        TextView value = popup_layout.findViewById(R.id.y_value);
        time.setText(res.getString(R.string.time_) + " " + sdf_time.format(cal.getTime()));
        value.setText(res.getString(R.string.value_) + " " + dataPoint.getY());

        final PopupWindow popup = new PopupWindow();
        popup.setContentView(popup_layout);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        popup.setTouchable(false);
        popup.setFocusable(false);
        popup.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                popup.dismiss();
                return false;
            }
        });
        popup.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        popup.setAnimationStyle(android.R.style.Animation_Dialog);
        popup.showAtLocation(graph_view, Gravity.NO_GRAVITY, 25, 100);
        popup.update();
    }
}