/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.App;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.DiagramEntry;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.DiagramMarkerView;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.TimeFormatter;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiagramActivity extends AppCompatActivity {

    // Constants
    public static final int MODE_SENSOR_DATA = 10001;
    public static final int MODE_COMPARE_DATA = 10002;

    // Variables as objects
    private Resources res;
    private ArrayList<DataRecord> records;
    private ArrayList<ArrayList<DataRecord>> compare_records;
    private ArrayList<Sensor> compare_sensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_diagram);

        // Get resources
        res = getResources();

        // Get intent extras
        Intent intent = getIntent();
        int mode = intent.getIntExtra("Mode", MODE_SENSOR_DATA);

        boolean show_1 = intent.hasExtra("Show1") && intent.getBooleanExtra("Show1", false);
        boolean show_2 = intent.hasExtra("Show2") && intent.getBooleanExtra("Show2", false);
        boolean show_3 = intent.hasExtra("Show3") && intent.getBooleanExtra("Show3", false);
        boolean show_4 = intent.hasExtra("Show4") && intent.getBooleanExtra("Show4", false);
        boolean show_5 = intent.hasExtra("Show5") && intent.getBooleanExtra("Show5", false);
        boolean enable_average = intent.hasExtra("EnableAverage") && intent.getBooleanExtra("EnableAverage", false);
        boolean enable_median = intent.hasExtra("EnableMedian") && intent.getBooleanExtra("EnableMedian", false);
        boolean enable_threshold_who = intent.hasExtra("EnableThresholdWHO") && intent.getBooleanExtra("EnableThresholdWHO", false);
        boolean enable_threshold_eu = intent.hasExtra("EnableThresholdEU") && intent.getBooleanExtra("EnableThresholdEU", false);

        if(mode == MODE_SENSOR_DATA) {
            // Receive data from SensorActivity
            records = SensorActivity.records;

            // Prepare data
            SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
            Collections.sort(records);
        } else if(mode == MODE_COMPARE_DATA) {
            compare_records = CompareActivity.records;
            compare_sensors = CompareActivity.sensors;

            // Prepare data
            SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
            for(ArrayList<DataRecord> current_records : compare_records) Collections.sort(current_records);
        }

        try{
            // Initialize diagram
            LineChart chart = findViewById(R.id.chart);
            chart.setHardwareAccelerationEnabled(true);
            chart.setKeepScreenOn(true);
            chart.setKeepPositionOnRotation(true);
            chart.setDescription(null);
            // Left y axis
            YAxis left = chart.getAxisLeft();
            left.setValueFormatter(new LargeValueFormatter());
            // x axis
            XAxis xAxis = chart.getXAxis();
            xAxis.setGranularityEnabled(true);
            xAxis.setGranularity(60f);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            // Sensor mode or comparison mode
            long first_time = 0;
            if(mode == MODE_SENSOR_DATA) {
                // Plot data
                List<Entry> entries_1 = new ArrayList<>();
                List<Entry> entries_2 = new ArrayList<>();
                List<Entry> entries_3 = new ArrayList<>();
                List<Entry> entries_4 = new ArrayList<>();
                List<Entry> entries_5 = new ArrayList<>();
                first_time = records.get(0).getDateTime().getTime();
                xAxis.setValueFormatter(new TimeFormatter(first_time));
                for (DataRecord r : records) {
                    if(show_1) entries_1.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getP1().floatValue(), "µg/m³"));
                    if(show_2) entries_2.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getP2().floatValue(), "µg/m³"));
                    if(show_3) entries_3.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getTemp().floatValue(), "°C"));
                    if(show_4) entries_4.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getHumidity().floatValue(), "%"));
                    if(show_5) entries_5.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getPressure().floatValue(), "hPa"));
                }

                // PM1
                LineDataSet p1 = new LineDataSet(entries_1, getString(R.string.value1) + " (µg/m³)");
                p1.setColor(res.getColor(R.color.series1));
                p1.setCircleColor(res.getColor(R.color.series1));
                p1.setLineWidth(2);
                p1.setDrawValues(false);
                p1.setAxisDependency(YAxis.AxisDependency.LEFT);
                p1.setHighLightColor(res.getColor(R.color.series1));
                //p1.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // PM2
                LineDataSet p2 = new LineDataSet(entries_2, getString(R.string.value2) + " (µg/m³)");
                p2.setColor(res.getColor(R.color.series2));
                p2.setCircleColor(res.getColor(R.color.series2));
                p2.setLineWidth(2);
                p2.setDrawValues(false);
                p2.setAxisDependency(YAxis.AxisDependency.LEFT);
                p2.setHighLightColor(res.getColor(R.color.series2));
                //p2.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Temperature
                LineDataSet temp = new LineDataSet(entries_3, getString(R.string.temperature) + " (°C)");
                temp.setColor(res.getColor(R.color.series3));
                temp.setCircleColor(res.getColor(R.color.series3));
                temp.setLineWidth(2);
                temp.setDrawValues(false);
                temp.setAxisDependency(YAxis.AxisDependency.RIGHT);
                temp.setHighLightColor(res.getColor(R.color.series3));
                //temp.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Humidity
                LineDataSet humidity = new LineDataSet(entries_4, getString(R.string.humidity) + " (%)");
                humidity.setColor(res.getColor(R.color.series4));
                humidity.setCircleColor(res.getColor(R.color.series4));
                humidity.setLineWidth(2);
                humidity.setDrawValues(false);
                humidity.setAxisDependency(YAxis.AxisDependency.RIGHT);
                humidity.setHighLightColor(res.getColor(R.color.series4));
                //humidity.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Pressure
                LineDataSet pressure = new LineDataSet(entries_5, getString(R.string.pressure) + " (hPa)");
                pressure.setColor(res.getColor(R.color.series5));
                pressure.setCircleColor(res.getColor(R.color.series5));
                pressure.setLineWidth(2);
                pressure.setDrawValues(false);
                pressure.setAxisDependency(YAxis.AxisDependency.RIGHT);
                pressure.setHighLightColor(res.getColor(R.color.series5));
                //pressure.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Add single lines
                List<ILineDataSet> dataSets = new ArrayList<>();
                if(show_1) dataSets.add(p1);
                if(show_1 && (enable_average || enable_median)) dataSets.add(getAverageMedianPM1(enable_average, enable_median, first_time));
                if(show_2) dataSets.add(p2);
                if(show_2 && (enable_average || enable_median)) dataSets.add(getAverageMedianPM2(enable_average, enable_median, first_time));
                if(show_3) dataSets.add(temp);
                if(show_3 && (enable_average || enable_median)) dataSets.add(getAverageMedianTemperature(enable_average, enable_median, first_time));
                if(show_4) dataSets.add(humidity);
                if(show_4 && (enable_average || enable_median)) dataSets.add(getAverageMedianHumidity(enable_average, enable_median, first_time));
                if(show_5) dataSets.add(pressure);
                if(show_5 && (enable_average || enable_median)) dataSets.add(getAverageMedianPressure(enable_average, enable_median, first_time));
                if((show_1 || show_2) && (enable_threshold_eu || enable_threshold_who)) {
                    dataSets.add(getThresholdPM1(enable_threshold_eu, enable_threshold_who, first_time));
                    dataSets.add(getThresholdPM2(enable_threshold_eu, enable_threshold_who, first_time));
                }
                chart.setData(new LineData(dataSets));
            } else if(mode == MODE_COMPARE_DATA) {
                // Get first time
                first_time = Long.MAX_VALUE;
                for(int i = 0; i < compare_sensors.size(); i++) {
                    try{
                        long current_first_time = compare_records.get(i).get(0).getDateTime().getTime();
                        first_time = current_first_time < first_time ? current_first_time : first_time;
                    } catch (Exception e) {}
                }
                xAxis.setValueFormatter(new TimeFormatter(first_time));
                // Plot data
                List<ILineDataSet> dataSets = new ArrayList<>();
                for(int i = 0; i < compare_sensors.size(); i++) {
                    if(compare_records.get(i).size() > 0) {
                        List<Entry> entries = new ArrayList<>();
                        for(DataRecord r : compare_records.get(i)) {
                            try{
                                if(show_1) {
                                    entries.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getP1().floatValue(), "µg/m³"));
                                } else if(show_2) {
                                    entries.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getP2().floatValue(), "µg/m³"));
                                } else if(show_3) {
                                    entries.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getTemp().floatValue(), "°C"));
                                } else if(show_4) {
                                    entries.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getHumidity().floatValue(), "%"));
                                } else if(show_5) {
                                    entries.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getPressure().floatValue(), "hPa"));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        String set_name = getString(R.string.error_try_again);
                        if(show_1) set_name = compare_sensors.get(i).getName() + " - " + getString(R.string.value1) + " (µg/m³)";
                        if(show_2) set_name = compare_sensors.get(i).getName() + " - " + getString(R.string.value2) + " (µg/m³)";
                        if(show_3) set_name = compare_sensors.get(i).getName() + " - " + getString(R.string.temperature) + " (°C)";
                        if(show_4) set_name = compare_sensors.get(i).getName() + " - " + getString(R.string.humidity) + " (%)";
                        if(show_5) set_name = compare_sensors.get(i).getName() + " - " + getString(R.string.pressure) + " (hPa)³";
                        LineDataSet set = new LineDataSet(entries, set_name);
                        set.setColor(compare_sensors.get(i).getColor());
                        set.setCircleColor(compare_sensors.get(i).getColor());
                        set.setLineWidth(2);
                        set.setDrawValues(false);
                        set.setAxisDependency(show_1 || show_2 ? YAxis.AxisDependency.LEFT : YAxis.AxisDependency.RIGHT);
                        set.setHighLightColor(compare_sensors.get(i).getColor());
                        dataSets.add(set);
                    }
                }
                chart.setData(new LineData(dataSets));
            }

            chart.setMarker(new DiagramMarkerView(this, R.layout.diagram_marker_view, first_time));
            // Customize legend
            chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
            chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
            chart.getLegend().setWordWrapEnabled(true);
            // Redraw & animate
            chart.invalidate();
            chart.animateY(700, Easing.EaseInCubic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private LineDataSet getAverageMedianPM1(boolean enable_average, boolean enable_median, long first_timestamp) {
        List<Entry> am_entries = new ArrayList<>();
        if(enable_average) {
            double average = 0;
            for(DataRecord record : records) average+=record.getP1();
            average /= records.size();
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
        } else if(enable_median) {
            ArrayList<Double> double_records = new ArrayList<>();
            for(DataRecord record : records) double_records.add(record.getP1());
            double median = Tools.INSTANCE.calculateMedian(double_records);
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
        }
        return getDashedLine(am_entries, R.color.series1);
    }

    @NotNull
    private LineDataSet getAverageMedianPM2(boolean enable_average, boolean enable_median, long first_timestamp) {
        List<Entry> am_entries;
        am_entries = new ArrayList<>();
        if(enable_average) {
            double average = 0;
            for(DataRecord record : records) average+=record.getP2();
            average /= records.size();
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
        } else if(enable_median) {
            ArrayList<Double> double_records = new ArrayList<>();
            for(DataRecord record : records) double_records.add(record.getP2());
            double median = Tools.INSTANCE.calculateMedian(double_records);
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
        }
        return getDashedLine(am_entries, R.color.series2);
    }

    @NotNull
    private LineDataSet getAverageMedianTemperature(boolean enable_average, boolean enable_median, long first_timestamp) {
        List<Entry> am_entries;
        am_entries = new ArrayList<>();
        if(enable_average) {
            double average = 0;
            for(DataRecord record : records) average+=record.getTemp();
            average /= records.size();
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
        } else if(enable_median) {
            ArrayList<Double> double_records = new ArrayList<>();
            for(DataRecord record : records) double_records.add(record.getTemp());
            double median = Tools.INSTANCE.calculateMedian(double_records);
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
        }
        LineDataSet average_median_temperature = getDashedLine(am_entries, R.color.series3);
        average_median_temperature.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return average_median_temperature;
    }

    @NotNull
    private LineDataSet getAverageMedianHumidity(boolean enable_average, boolean enable_median, long first_timestamp) {
        List<Entry> am_entries;
        am_entries = new ArrayList<>();
        if(enable_average) {
            double average = 0;
            for(DataRecord record : records) average+=record.getHumidity();
            average /= records.size();
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
        } else if(enable_median) {
            ArrayList<Double> double_records = new ArrayList<>();
            for(DataRecord record : records) double_records.add(record.getHumidity());
            double median = Tools.INSTANCE.calculateMedian(double_records);
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
        }
        LineDataSet average_median_humidity = getDashedLine(am_entries, R.color.series4);
        average_median_humidity.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return average_median_humidity;
    }

    @NotNull
    private LineDataSet getAverageMedianPressure(boolean enable_average, boolean enable_median, long first_timestamp) {
        List<Entry> am_entries;
        am_entries = new ArrayList<>();
        if(enable_average) {
            double average = 0;
            for(DataRecord record : records) average+=record.getPressure();
            average /= records.size();
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) average));
        } else if(enable_median) {
            ArrayList<Double> double_records = new ArrayList<>();
            for(DataRecord record : records) double_records.add(record.getPressure());
            double median = Tools.INSTANCE.calculateMedian(double_records);
            am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
        }
        LineDataSet average_median_pressure = getDashedLine(am_entries, R.color.series5);
        average_median_pressure.setAxisDependency(YAxis.AxisDependency.RIGHT);
        return average_median_pressure;
    }

    @NotNull
    private LineDataSet getThresholdPM1(boolean enable_eu_thresholds, boolean enable_who_thresholds, long first_timestamp) {
        List<Entry> th_entries;
        th_entries = new ArrayList<>();
        if(enable_eu_thresholds) {
            th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM10));
            th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM10));
        } else if(enable_who_thresholds) {
            th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM10));
            th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM10));
        }
        return getDashedLine(th_entries, R.color.error);
    }

    @NotNull
    private LineDataSet getThresholdPM2(boolean enable_eu_thresholds, boolean enable_who_thresholds, long first_timestamp) {
        List<Entry> th_entries;
        th_entries = new ArrayList<>();
        if(enable_eu_thresholds) {
            th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM2_5));
            th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM2_5));
        } else if(enable_who_thresholds) {
            th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM2_5));
            th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM2_5));
        }
        return getDashedLine(th_entries, R.color.error);
    }

    @NotNull
    private LineDataSet getDashedLine(List<Entry> am_entries, int color) {
        LineDataSet dl = new LineDataSet(am_entries, null);
        dl.setColor(res.getColor(color));
        dl.setLineWidth(1);
        dl.setDrawValues(false);
        dl.setDrawCircles(false);
        dl.setHighlightEnabled(false);
        dl.enableDashedLine(10f, 10f, 0);
        return dl;
    }
}