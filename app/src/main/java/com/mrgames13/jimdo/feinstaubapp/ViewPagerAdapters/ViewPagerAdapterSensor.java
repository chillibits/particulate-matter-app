package com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mrgames13.jimdo.feinstaubapp.App.DiagramActivity;
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Point;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.SeriesReducer;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.DataAdapter;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimeZone;

public class ViewPagerAdapterSensor extends FragmentPagerAdapter {

    //Konstanten

    //Variablen als Objekte
    private static Resources res;
    public static SensorActivity activity;
    private ArrayList<String> tabTitles = new ArrayList<>();
    private static Handler h;
    public static ArrayList<DataRecord> records = new ArrayList<>();
    private static SimpleDateFormat df_time = new SimpleDateFormat("HH:mm:ss");

    //Utils-Pakete
    private static StorageUtils su;

    //Variablen
    private static boolean show_gps_data;

    public ViewPagerAdapterSensor(FragmentManager manager, SensorActivity activity, StorageUtils su, boolean show_gps_data) {
        super(manager);
        res = activity.getResources();
        ViewPagerAdapterSensor.activity = activity;
        h = new Handler();
        ViewPagerAdapterSensor.su = su;
        tabTitles.add(res.getString(R.string.tab_diagram));
        tabTitles.add(res.getString(R.string.tab_data));
        df_time.setTimeZone(TimeZone.getDefault());
        ViewPagerAdapterSensor.show_gps_data = show_gps_data;
    }

    @Override
    public Fragment getItem(int pos) {
        if(pos == 0) return new DiagramFragment();
        if(pos == 1) return new DataFragment();
        return null;
    }

    @Override
    public int getCount() {
        return tabTitles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }

    public void refreshFragments() {
        DiagramFragment.refresh();
        DataFragment.refresh();
    }

    public void exportDiagram(Context context) {
        DiagramFragment.exportDiagram(context);
    }

    public void showGPSData(boolean show) {
        DataFragment.showGPSData(show);
    }

    //-------------------------------------------Fragmente------------------------------------------

    public static class DiagramFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        private static View contentView;
        private static GraphView graph_view;
        private CheckBox custom_p1;
        private CheckBox custom_p2;
        private CheckBox custom_temp;
        private CheckBox custom_humidity;
        private CheckBox custom_pressure;
        private RadioButton custom_nothing;
        private static RadioButton custom_average;
        private static RadioButton custom_median;
        private RadioButton custom_threshold_nothing;
        private static RadioButton custom_threshold_who;
        private static RadioButton custom_threshold_eu;
        private static SeekBar curve_smoothness;
        private static LineGraphSeries<DataPoint> series1;
        private static LineGraphSeries<DataPoint> series1_average_median;
        private static LineGraphSeries<DataPoint> series1_threshold;
        private static LineGraphSeries<DataPoint> series2;
        private static LineGraphSeries<DataPoint> series2_average_median;
        private static LineGraphSeries<DataPoint> series2_threshold;
        private static LineGraphSeries<DataPoint> series3;
        private static LineGraphSeries<DataPoint> series3_average_median;
        private static LineGraphSeries<DataPoint> series4;
        private static LineGraphSeries<DataPoint> series4_average_median;
        private static LineGraphSeries<DataPoint> series5;
        private static LineGraphSeries<DataPoint> series5_average_median;
        private static TextView cv_p1;
        private static TextView cv_p2;
        private static TextView cv_temp;
        private static TextView cv_humidity;
        private static TextView cv_pressure;
        private static TextView cv_time;

        //Variablen

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            contentView = inflater.inflate(R.layout.tab_diagram, null);

            graph_view = contentView.findViewById(R.id.diagram);
            graph_view.getViewport().setScalable(true);
            graph_view.getGridLabelRenderer().setHorizontalLabelsVisible(false);
            graph_view.getGridLabelRenderer().setVerticalLabelsVisible(false);
            graph_view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(SensorActivity.custom_p1 || SensorActivity.custom_p2 || SensorActivity.custom_temp || SensorActivity.custom_humidity || SensorActivity.custom_pressure) {
                        Intent i = new Intent(activity, DiagramActivity.class);
                        i.putExtra("Show1", SensorActivity.custom_p1);
                        i.putExtra("Show2", SensorActivity.custom_p2);
                        i.putExtra("Show3", SensorActivity.custom_temp);
                        i.putExtra("Show4", SensorActivity.custom_humidity);
                        i.putExtra("Show5", SensorActivity.custom_pressure);
                        i.putExtra("EnableAverage", custom_average.isChecked());
                        i.putExtra("EnableMedian", custom_median.isChecked());
                        i.putExtra("EnableThresholdWHO", custom_threshold_who.isChecked());
                        i.putExtra("EnableThresholdEU", custom_threshold_eu.isChecked());
                        startActivity(i);
                    } else {
                        Toast.makeText(activity, res.getString(R.string.no_diagram_selected), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //CustomControls initialisieren
            custom_p1 = contentView.findViewById(R.id.custom_p1);
            custom_p2 = contentView.findViewById(R.id.custom_p2);
            custom_temp = contentView.findViewById(R.id.custom_temp);
            custom_humidity = contentView.findViewById(R.id.custom_humidity);
            custom_pressure = contentView.findViewById(R.id.custom_pressure);
            custom_nothing = contentView.findViewById(R.id.enable_average_median_nothing);
            custom_average = contentView.findViewById(R.id.enable_average);
            custom_median = contentView.findViewById(R.id.enable_median);
            custom_threshold_nothing = contentView.findViewById(R.id.enable_eu_who_nothing);
            custom_threshold_who = contentView.findViewById(R.id.enable_who);
            custom_threshold_eu = contentView.findViewById(R.id.enable_eu);

            h.post(new Runnable() {
                @Override
                public void run() {
                    custom_p1.setChecked(SensorActivity.custom_p1);
                    custom_p2.setChecked(SensorActivity.custom_p2);
                    custom_temp.setChecked(SensorActivity.custom_temp);
                    custom_humidity.setChecked(SensorActivity.custom_humidity);
                    custom_pressure.setChecked(SensorActivity.custom_pressure);
                }
            });

            custom_p1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean value) {
                    if(!value && !custom_p2.isChecked() && !custom_temp.isChecked() && !custom_humidity.isChecked() && !custom_pressure.isChecked()) {
                        cb.setChecked(true);
                        return;
                    }
                    SensorActivity.custom_p1 = value;
                    updateP1(records, value);
                }
            });
            custom_p2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean value) {
                    if(!custom_p1.isChecked() && !value && !custom_temp.isChecked() && !custom_humidity.isChecked() && !custom_pressure.isChecked()) {
                        cb.setChecked(true);
                        return;
                    }
                    SensorActivity.custom_p2 = value;
                    updateP2(records, value);
                }
            });
            custom_temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean value) {
                    if(!custom_p1.isChecked() && !custom_p2.isChecked() && !value && !custom_humidity.isChecked() && !custom_pressure.isChecked()) {
                        cb.setChecked(true);
                        return;
                    }
                    SensorActivity.custom_temp = value;
                    updateTemp(records, value);
                }
            });
            custom_humidity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean value) {
                    if(!custom_p1.isChecked() && !custom_p2.isChecked() && !custom_temp.isChecked() && !value && !custom_pressure.isChecked()) {
                        cb.setChecked(true);
                        return;
                    }
                    SensorActivity.custom_humidity = value;
                    updateHumidity(records, value);
                }
            });
            custom_pressure.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean value) {
                    if(!custom_p1.isChecked() && !custom_p2.isChecked() && !custom_temp.isChecked() && !custom_humidity.isChecked() && !value) {
                        cb.setChecked(true);
                        return;
                    }
                    SensorActivity.custom_pressure = value;
                    updatePressure(records, value);
                }
            });
            custom_nothing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    updateP1(null, false);
                    updateP1(records, custom_p1.isChecked());
                    updateP2(null, false);
                    updateP2(records, custom_p2.isChecked());
                    updateTemp(null, false);
                    updateTemp(records, custom_temp.isChecked());
                    updateHumidity(null, false);
                    updateHumidity(records, custom_humidity.isChecked());
                    updatePressure(null, false);
                    updatePressure(records, custom_pressure.isChecked());
                }
            });
            custom_average.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    updateP1(null, false);
                    updateP1(records, custom_p1.isChecked());
                    updateP2(null, false);
                    updateP2(records, custom_p2.isChecked());
                    updateTemp(null, false);
                    updateTemp(records, custom_temp.isChecked());
                    updateHumidity(null, false);
                    updateHumidity(records, custom_humidity.isChecked());
                    updatePressure(null, false);
                    updatePressure(records, custom_pressure.isChecked());
                }
            });
            custom_median.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    updateP1(null, false);
                    updateP1(records, custom_p1.isChecked());
                    updateP2(null, false);
                    updateP2(records, custom_p2.isChecked());
                    updateTemp(null, false);
                    updateTemp(records, custom_temp.isChecked());
                    updateHumidity(null, false);
                    updateHumidity(records, custom_humidity.isChecked());
                    updatePressure(null, false);
                    updatePressure(records, custom_pressure.isChecked());
                }
            });
            custom_threshold_nothing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    updateP1(null, false);
                    updateP1(records, custom_p1.isChecked());
                    updateP2(null, false);
                    updateP2(records, custom_p2.isChecked());
                }
            });
            custom_threshold_who.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    updateP1(null, false);
                    updateP1(records, custom_p1.isChecked());
                    updateP2(null, false);
                    updateP2(records, custom_p2.isChecked());
                }
            });
            custom_threshold_eu.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    updateP1(null, false);
                    updateP1(records, custom_p1.isChecked());
                    updateP2(null, false);
                    updateP2(records, custom_p2.isChecked());
                }
            });

            curve_smoothness = contentView.findViewById(R.id.curve_smoothness);
            curve_smoothness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int value, boolean b) {
                    SensorActivity.curve_smoothness = (500 - value) / 100.0;
                    updateCurveSmoothness((500 -  value) / 100.0);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            cv_p1 = contentView.findViewById(R.id.cv_p1);
            cv_p2 = contentView.findViewById(R.id.cv_p2);
            cv_temp = contentView.findViewById(R.id.cv_temp);
            cv_humidity = contentView.findViewById(R.id.cv_humidity);
            cv_pressure = contentView.findViewById(R.id.cv_pressure);
            cv_time = contentView.findViewById(R.id.cv_time);

            return contentView;
        }

        private static void updateP1(ArrayList<DataRecord> records, boolean value) {
            if(value) {
                try {
                    if(records.size() > 0) {
                        long first_time = records.get(0).getDateTime().getTime() / 1000;

                        series1 = new LineGraphSeries<>();
                        series1.setColor(res.getColor(R.color.series1));
                        for(DataRecord record : Tools.fitArrayList(su, records)) {
                            series1.appendData(new DataPoint(record.getDateTime().getTime() / 1000 - first_time, record.getP1()), false, 1000000);
                        }
                        graph_view.addSeries(series1);

                        if(custom_average.isChecked()) {
                            //Mittelwert einzeichnen
                            double average = 0;
                            for(DataRecord record : records) average+=record.getP1();
                            average /= records.size();
                            series1_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series1));
                            graph_view.addSeries(series1_average_median);
                        } else if(custom_median.isChecked()) {
                            //Median einzeichnen
                            ArrayList<Double> double_records = new ArrayList<>();
                            for(DataRecord record : records) double_records.add(record.getP1());
                            double median = Tools.calculateMedian(double_records);
                            series1_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series1));
                            graph_view.addSeries(series1_average_median);
                        }

                        if(custom_threshold_who.isChecked()) {
                            series1_threshold = drawHorizontalLine(Constants.THRESHOLD_WHO_PM10, first_time, Color.RED);
                            graph_view.addSeries(series1_threshold);
                        } else if(custom_threshold_eu.isChecked()) {
                            series1_threshold = drawHorizontalLine(Constants.THRESHOLD_EU_PM10, first_time, Color.RED);
                            graph_view.addSeries(series1_threshold);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series1);
                graph_view.removeSeries(series1_average_median);
                graph_view.removeSeries(series1_threshold);
            }
        }

        private static void updateP2(ArrayList<DataRecord> records, boolean value) {
            if(value) {
                try {
                    if(records.size() > 0) {
                        long first_time = records.get(0).getDateTime().getTime() / 1000;

                        series2 = new LineGraphSeries<>();
                        series2.setColor(res.getColor(R.color.series2));
                        for(DataRecord record : Tools.fitArrayList(su, records)) {
                            series2.appendData(new DataPoint(record.getDateTime().getTime() / 1000 - first_time, record.getP2()), false, 1000000);
                        }
                        graph_view.addSeries(series2);

                        if(custom_average.isChecked()) {
                            //Mittelwert einzeichnen
                            double average = 0;
                            for(DataRecord record : records) average+=record.getP2();
                            average /= records.size();
                            series2_average_median = new LineGraphSeries<>();
                            series2_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series2));
                            graph_view.addSeries(series2_average_median);
                        } else if(custom_median.isChecked()) {
                            //Median einzeichnen
                            ArrayList<Double> double_records = new ArrayList<>();
                            for(DataRecord record : records) double_records.add(record.getP2());
                            double median = Tools.calculateMedian(double_records);
                            series2_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series2));
                            graph_view.addSeries(series2_average_median);
                        }

                        if(custom_threshold_who.isChecked()) {
                            series2_threshold = drawHorizontalLine(Constants.THRESHOLD_WHO_PM2_5, first_time, Color.RED);
                            graph_view.addSeries(series2_threshold);
                        } else if(custom_threshold_eu.isChecked()) {
                            series2_threshold = drawHorizontalLine(Constants.THRESHOLD_EU_PM2_5, first_time, Color.RED);
                            graph_view.addSeries(series2_threshold);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series2);
                graph_view.removeSeries(series2_average_median);
                graph_view.removeSeries(series2_threshold);
            }
        }

        private static void updateTemp(ArrayList<DataRecord> records, boolean value) {
            if(value) {
                try {
                    if(records.size() > 0) {
                        long first_time = records.get(0).getDateTime().getTime() / 1000;

                        series3 = new LineGraphSeries<>();
                        series3.setColor(res.getColor(R.color.series3));
                        for(DataRecord record : Tools.fitArrayList(su, records)) {
                            series3.appendData(new DataPoint(record.getDateTime().getTime() / 1000 - first_time, record.getTemp()), false, 1000000);
                        }
                        graph_view.addSeries(series3);

                        if(custom_average.isChecked()) {
                            //Mittelwert einzeichnen
                            double average = 0;
                            for(DataRecord record : records) average+=record.getTemp();
                            average /= records.size();
                            series3_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series3));
                            graph_view.addSeries(series3_average_median);
                        } else if(custom_median.isChecked()) {
                            //Median einzeichnen
                            ArrayList<Double> double_records = new ArrayList<>();
                            for(DataRecord record : records) double_records.add(record.getTemp());
                            double median = Tools.calculateMedian(double_records);
                            series3_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series3));
                            graph_view.addSeries(series3_average_median);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series3);
                graph_view.removeSeries(series3_average_median);
            }
        }

        private static void updateHumidity(ArrayList<DataRecord> records, boolean value) {
            if(value) {
                try {
                    if(records.size() > 0) {
                        long first_time = records.get(0).getDateTime().getTime() / 1000;

                        series4 = new LineGraphSeries<>();
                        series4.setColor(res.getColor(R.color.series4));
                        for(DataRecord record : Tools.fitArrayList(su, records)) {
                            series4.appendData(new DataPoint(record.getDateTime().getTime() / 1000 - first_time, record.getHumidity()), false, 1000000);
                        }
                        graph_view.addSeries(series4);

                        if(custom_average.isChecked()) {
                            //Mittelwert einzeichnen
                            double average = 0;
                            for(DataRecord record : records) average+=record.getHumidity();
                            average /= records.size();
                            series4_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series4));
                            graph_view.addSeries(series4_average_median);
                        } else if(custom_median.isChecked()) {
                            //Median einzeichnen
                            ArrayList<Double> double_records = new ArrayList<>();
                            for(DataRecord record : records) double_records.add(record.getHumidity());
                            double median = Tools.calculateMedian(double_records);
                            series4_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series4));
                            graph_view.addSeries(series4_average_median);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series4);
                graph_view.removeSeries(series4_average_median);
            }
        }

        private static void updatePressure(ArrayList<DataRecord> records, boolean value) {
            if(value) {
                try {
                    if(records.size() > 0) {
                        long first_time = records.get(0).getDateTime().getTime() / 1000;

                        series5 = new LineGraphSeries<>();
                        series5.setColor(res.getColor(R.color.series5));
                        for(DataRecord record : Tools.fitArrayList(su, records)) {
                            series5.appendData(new DataPoint(record.getDateTime().getTime() / 1000 - first_time, record.getPressure()), false, 1000000);
                        }
                        graph_view.addSeries(series5);

                        if(custom_average.isChecked()) {
                            //Mittelwert einzeichnen
                            double average = 0;
                            for(DataRecord record : records) average+=record.getPressure();
                            average /= records.size();
                            series5_average_median = drawHorizontalLine(average, first_time, res.getColor(R.color.series5));
                            graph_view.addSeries(series5_average_median);
                        } else if(custom_median.isChecked()) {
                            //Median einzeichnen
                            ArrayList<Double> double_records = new ArrayList<>();
                            for(DataRecord record : records) double_records.add(record.getPressure());
                            double median = Tools.calculateMedian(double_records);
                            series5_average_median = drawHorizontalLine(median, first_time, res.getColor(R.color.series5));
                            graph_view.addSeries(series5_average_median);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series5);
                graph_view.removeSeries(series5_average_median);
            }
        }

        private static LineGraphSeries drawHorizontalLine(double value, long first_time, int color) {
            LineGraphSeries series = new LineGraphSeries<>();
            series.appendData(new DataPoint(0, value), false, 2);
            series.appendData(new DataPoint(records.get(records.size() -1).getDateTime().getTime() / 1000 - first_time, value), false, 2);

            Paint p = new Paint();
            p.setColor(color);
            p.setStyle(Paint.Style.STROKE);
            p.setPathEffect(new DashPathEffect(new float[] { 20, 10 }, 0));
            p.setStrokeWidth(3);

            series.setDrawAsPath(true);
            series.setCustomPaint(p);
            series.setDrawDataPoints(false);
            return series;
        }

        private static void updateCurveSmoothness(double epsilon) {
            ArrayList<Point> reduced_p1 = new ArrayList<>();
            reduced_p1.addAll(SeriesReducer.reduce(Tools.convertDataRecordsToPointsP1(Tools.fitArrayList(su, records)), epsilon));
            ArrayList<Point> reduced_p2 = new ArrayList<>();
            reduced_p2.addAll(SeriesReducer.reduce(Tools.convertDataRecordsToPointsP2(Tools.fitArrayList(su, records)), epsilon));
            ArrayList<Point> reduced_temp = new ArrayList<>();
            reduced_temp.addAll(SeriesReducer.reduce(Tools.convertDataRecordsToPointsTemp(Tools.fitArrayList(su, records)), epsilon));
            ArrayList<Point> reduced_humidity = new ArrayList<>();
            reduced_humidity.addAll(SeriesReducer.reduce(Tools.convertDataRecordsToPointsHumidity(Tools.fitArrayList(su, records)), epsilon));
            ArrayList<Point> reduced_pressure = new ArrayList<>();
            reduced_pressure.addAll(SeriesReducer.reduce(Tools.convertDataRecordsToPointsPressure(Tools.fitArrayList(su, records)), epsilon));

            //Diagram leeren
            updateP1(null, false);
            updateP2(null, false);
            updateTemp(null, false);
            updateHumidity(null, false);
            updatePressure(null, false);
            //Veränderte Kurve einblenden
            updateP1(Tools.convertPointsToDataRecordsP1(reduced_p1), SensorActivity.custom_p1);
            updateP2(Tools.convertPointsToDataRecordsP2(reduced_p2), SensorActivity.custom_p2);
            updateTemp(Tools.convertPointsToDataRecordsTemp(reduced_temp), SensorActivity.custom_temp);
            updateHumidity(Tools.convertPointsToDataRecordsHumidity(reduced_humidity), SensorActivity.custom_humidity);
            updatePressure(Tools.convertPointsToDataRecordsPressure(reduced_pressure), SensorActivity.custom_pressure);
        }

        private static void updateLastValues() {
            if(SensorActivity.records.size() > 0 && SensorActivity.selected_day_timestamp == SensorActivity.current_day_timestamp) {
                cv_p1.setText(String.valueOf(SensorActivity.records.get(SensorActivity.records.size() -1).getP1()).concat(" µg/m³"));
                cv_p2.setText(String.valueOf(SensorActivity.records.get(SensorActivity.records.size() -1).getP2()).concat(" µg/m³"));
                cv_temp.setText(String.valueOf(SensorActivity.records.get(SensorActivity.records.size() -1).getTemp()).concat(" °C"));
                cv_humidity.setText(String.valueOf(SensorActivity.records.get(SensorActivity.records.size() -1).getHumidity()).concat(" %"));
                cv_pressure.setText(String.valueOf(Tools.round(SensorActivity.records.get(SensorActivity.records.size() -1).getPressure(), 3)).concat(" hPa"));
                SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                cv_time.setText(res.getString(R.string.state_of_) + " " + sdf_date.format(SensorActivity.records.get(SensorActivity.records.size() -1).getDateTime()));

                contentView.findViewById(R.id.title_current_values).setVisibility(View.VISIBLE);
                contentView.findViewById(R.id.cv_container).setVisibility(View.VISIBLE);
            } else {
                contentView.findViewById(R.id.title_current_values).setVisibility(View.GONE);
                contentView.findViewById(R.id.cv_container).setVisibility(View.GONE);
            }
        }

        public static void refresh() {
            if(records != null) {
                contentView.findViewById(R.id.loading).setVisibility(View.GONE);
                if (records.size() > 0) {
                    contentView.findViewById(R.id.no_data).setVisibility(View.GONE);
                    contentView.findViewById(R.id.diagram_container).setVisibility(View.VISIBLE);

                    int tmp = SensorActivity.sort_mode;
                    SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
                    Collections.sort(records);
                    SensorActivity.sort_mode = tmp;

                    try{
                        long first_time = records.get(0).getDateTime().getTime() / 1000;
                        graph_view.getViewport().setMinX(0);
                        graph_view.getViewport().setMaxX(Math.abs(records.get(records.size() -1).getDateTime().getTime() / 1000 - first_time));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    updateP1(null, false);
                    updateP1(records, SensorActivity.custom_p1);
                    updateP2(null, false);
                    updateP2(records, SensorActivity.custom_p2);
                    updateTemp(null, false);
                    updateTemp(records, SensorActivity.custom_temp);
                    updateHumidity(null, false);
                    updateHumidity(records, SensorActivity.custom_humidity);
                    updatePressure(null, false);
                    updatePressure(records, SensorActivity.custom_pressure);
                    curve_smoothness.setProgress(curve_smoothness.getMax());
                } else {
                    updateP1(null, false);
                    updateP2(null, false);
                    updateTemp(null, false);
                    updateHumidity(null, false);
                    updatePressure(null, false);
                    contentView.findViewById(R.id.diagram_container).setVisibility(View.GONE);
                    contentView.findViewById(R.id.no_data).setVisibility(View.VISIBLE);
                }

                updateLastValues();
            } else {
                updateP1(null, false);
                updateP2(null, false);
                updateTemp(null, false);
                updateHumidity(null, false);
                updatePressure(null, false);
                contentView.findViewById(R.id.diagram_container).setVisibility(View.GONE);
                contentView.findViewById(R.id.no_data).setVisibility(View.VISIBLE);
            }
        }

        public static void exportDiagram(Context context) {
            graph_view.takeSnapshotAndShare(context, "export_" + System.currentTimeMillis(), res.getString(R.string.export_diagram));
        }
    }

    public static class DataFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        public static View contentView;
        private static RecyclerView data_view;
        private static DataAdapter data_view_adapter;
        private RecyclerView.LayoutManager data_view_manager;

        private static LinearLayout heading;
        private TextView heading_time;
        private ImageView heading_time_arrow;
        private TextView heading_p1;
        private ImageView heading_p1_arrow;
        private TextView heading_p2;
        private ImageView heading_p2_arrow;
        private TextView heading_temp;
        private ImageView heading_temp_arrow;
        private TextView heading_humidity;
        private ImageView heading_humidity_arrow;
        private TextView heading_pressure;
        private ImageView heading_pressure_arrow;
        private static RelativeLayout footer;
        private static TextView footer_average_p1;
        private static TextView footer_average_p2;
        private static TextView footer_average_temp;
        private static TextView footer_average_humidity;
        private static TextView footer_average_pressure;
        private static TextView footer_median_p1;
        private static TextView footer_median_p2;
        private static TextView footer_median_temp;
        private static TextView footer_median_humidity;
        private static TextView footer_median_pressure;
        private static TextView record_conter;

        //Variablen

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            contentView = inflater.inflate(R.layout.tab_data, null);
            //Komponenten initialisieren
            data_view_adapter = new DataAdapter();
            data_view = contentView.findViewById(R.id.data);
            data_view_manager = new LinearLayoutManager(getContext());
            data_view.setLayoutManager(data_view_manager);
            data_view.setAdapter(data_view_adapter);
            data_view.setHasFixedSize(true);
            if(records != null) {
                contentView.findViewById(R.id.loading).setVisibility(View.GONE);
                contentView.findViewById(R.id.no_data).setVisibility(records.size() == 0 ? View.VISIBLE : View.GONE);
                contentView.findViewById(R.id.data_footer).setVisibility(records.size() == 0 ? View.INVISIBLE : View.VISIBLE);

                heading = contentView.findViewById(R.id.data_heading);
                heading_time = contentView.findViewById(R.id.heading_time);
                heading_time_arrow = contentView.findViewById(R.id.sort_time);
                heading_p1 = contentView.findViewById(R.id.heading_p1);
                heading_p1_arrow = contentView.findViewById(R.id.sort_p1);
                heading_p2 = contentView.findViewById(R.id.heading_p2);
                heading_p2_arrow = contentView.findViewById(R.id.sort_p2);
                heading_temp = contentView.findViewById(R.id.heading_temp);
                heading_temp_arrow = contentView.findViewById(R.id.sort_temp);
                heading_humidity = contentView.findViewById(R.id.heading_humidity);
                heading_humidity_arrow = contentView.findViewById(R.id.sort_humidity);
                heading_pressure = contentView.findViewById(R.id.heading_pressure);
                heading_pressure_arrow = contentView.findViewById(R.id.sort_pressure);

                footer = contentView.findViewById(R.id.data_footer);
                footer_average_p1 = contentView.findViewById(R.id.footer_average_p1);
                footer_average_p2 = contentView.findViewById(R.id.footer_average_p2);
                footer_average_temp = contentView.findViewById(R.id.footer_average_temp);
                footer_average_humidity = contentView.findViewById(R.id.footer_average_humidity);
                footer_average_pressure = contentView.findViewById(R.id.footer_average_pressure);
                footer_median_p1 = contentView.findViewById(R.id.footer_median_p1);
                footer_median_p2 = contentView.findViewById(R.id.footer_median_p2);
                footer_median_temp = contentView.findViewById(R.id.footer_median_temp);
                footer_median_humidity = contentView.findViewById(R.id.footer_median_humidity);
                footer_median_pressure = contentView.findViewById(R.id.footer_median_pressure);

                heading_time.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        timeSortClicked();
                    }
                });
                heading_time_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        timeSortClicked();
                    }
                });
                heading_p1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        p1SortClicked();
                    }
                });
                heading_p1_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        p1SortClicked();
                    }
                });
                heading_p2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        p2SortClicked();
                    }
                });
                heading_p2_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        p2SortClicked();
                    }
                });
                heading_temp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tempSortClicked();
                    }
                });
                heading_temp_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tempSortClicked();
                    }
                });
                heading_humidity.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        humiditySortClicked();
                    }
                });
                heading_humidity_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        humiditySortClicked();
                    }
                });
                heading_pressure.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pressureSortClicked();
                    }
                });
                heading_pressure_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pressureSortClicked();
                    }
                });
            }

            showGPSData(show_gps_data);

            return contentView;
        }

        private void timeSortClicked() {
            heading_time_arrow.setVisibility(View.VISIBLE);
            heading_p1_arrow.setVisibility(View.INVISIBLE);
            heading_p2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
            heading_pressure_arrow.setVisibility(View.INVISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_DESC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
                heading_time_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_DESC;
                heading_time_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private void p1SortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_p1_arrow.setVisibility(View.VISIBLE);
            heading_p2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
            heading_pressure_arrow.setVisibility(View.INVISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_DESC;
                heading_p1_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_ASC;
                heading_p1_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private void p2SortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_p1_arrow.setVisibility(View.INVISIBLE);
            heading_p2_arrow.setVisibility(View.VISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
            heading_pressure_arrow.setVisibility(View.INVISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_DESC;
                heading_p2_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_ASC;
                heading_p2_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private void tempSortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_p1_arrow.setVisibility(View.INVISIBLE);
            heading_p2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.VISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
            heading_pressure_arrow.setVisibility(View.INVISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TEMP_DESC;
                heading_temp_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TEMP_ASC;
                heading_temp_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private void humiditySortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_p1_arrow.setVisibility(View.INVISIBLE);
            heading_p2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.VISIBLE);
            heading_pressure_arrow.setVisibility(View.INVISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_HUMIDITY_DESC;
                heading_humidity_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_HUMIDITY_ASC;
                heading_humidity_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private void pressureSortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_p1_arrow.setVisibility(View.INVISIBLE);
            heading_p2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
            heading_pressure_arrow.setVisibility(View.VISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_PRESSURE_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_PRESSURE_DESC;
                heading_pressure_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_PRESSURE_ASC;
                heading_pressure_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private static void showGPSData(boolean show) {
            contentView.findViewById(R.id.heading_gps).setVisibility(show ? View.VISIBLE : View.GONE);
            contentView.findViewById(R.id.footer_average_gps).setVisibility(show ? View.VISIBLE : View.GONE);
            contentView.findViewById(R.id.footer_median_gps).setVisibility(show ? View.VISIBLE : View.GONE);
            data_view.getLayoutParams().width = Math.round(res.getDisplayMetrics().density * (show ? 830 : 530));
            heading.getLayoutParams().width = Math.round(res.getDisplayMetrics().density * (show ? 830 : 530));
            footer.getLayoutParams().width = Math.round(res.getDisplayMetrics().density * (show ? 830 : 530));
            data_view_adapter.showGPSData(show);
        }

        public static void refresh() {
            try{
                if(records != null) {
                    data_view_adapter.notifyDataSetChanged();

                    contentView.findViewById(R.id.loading).setVisibility(View.GONE);
                    contentView.findViewById(R.id.no_data).setVisibility(records.size() == 0 ? View.VISIBLE : View.GONE);

                    if (records.size() > 0) {
                        record_conter = contentView.findViewById(R.id.record_counter);
                        record_conter.setVisibility(View.VISIBLE);
                        contentView.findViewById(R.id.data_heading).setVisibility(View.VISIBLE);
                        contentView.findViewById(R.id.data_footer).setVisibility(View.VISIBLE);
                        contentView.findViewById(R.id.data_footer_average).setVisibility(su.getBoolean("enable_daily_average", true) ? View.VISIBLE : View.GONE);
                        contentView.findViewById(R.id.data_footer_median).setVisibility(su.getBoolean("enable_daily_median", false) ? View.VISIBLE : View.GONE);
                        String footer_string = records.size() + " " + res.getString(R.string.tab_data) + " - " + res.getString(R.string.from) + " " + df_time.format(records.get(0).getDateTime()) + " " + res.getString(R.string.to) + " " + df_time.format(records.get(records.size() - 1).getDateTime());
                        record_conter.setText(footer_string);

                        if(su.getBoolean("enable_daily_average", true)) {
                            //Mittelwerte berechnen
                            double average_p1 = 0;
                            double average_p2 = 0;
                            double average_temp = 0;
                            double average_humidity = 0;
                            double average_pressure = 0;
                            for (DataRecord record : records) {
                                average_p1 += record.getP1();
                                average_p2 += record.getP2();
                                average_temp += record.getTemp();
                                average_humidity += record.getHumidity();
                                average_pressure += record.getPressure();
                            }
                            average_p1 = average_p1 / records.size();
                            average_p2 = average_p2 / records.size();
                            average_temp = average_temp / records.size();
                            average_humidity = average_humidity / records.size();
                            average_pressure = average_pressure / records.size();

                            footer_average_p1.setText(String.valueOf(Tools.round(average_p1, 1)).replace(".", ",").concat(" µg/m³"));
                            footer_average_p2.setText(String.valueOf(Tools.round(average_p2, 1)).replace(".", ",").concat(" µg/m³"));
                            footer_average_temp.setText(String.valueOf(Tools.round(average_temp, 1)).replace(".", ",").concat(" °C"));
                            footer_average_humidity.setText(String.valueOf(Tools.round(average_humidity, 1)).replace(".", ",").concat(" %"));
                            footer_average_pressure.setText(String.valueOf(Tools.round(average_pressure, 1)).replace(".", ",").concat(" hPa"));
                        }

                        if(su.getBoolean("enable_daily_median")) {
                            //Mediane berechnen
                            double median_p1;
                            double median_p2;
                            double median_temp;
                            double median_humidity;
                            double median_pressure;
                            int current_sort_mode = SensorActivity.sort_mode;
                            //P1
                            SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_ASC;
                            Collections.sort(records);
                            median_p1 = records.get(records.size() / 2).getP1();
                            //P2
                            SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_ASC;
                            Collections.sort(records);
                            median_p2 = records.get(records.size() / 2).getP2();
                            //Temp
                            SensorActivity.sort_mode = SensorActivity.SORT_MODE_TEMP_ASC;
                            Collections.sort(records);
                            median_temp = records.get(records.size() / 2).getTemp();
                            //Humidity
                            SensorActivity.sort_mode = SensorActivity.SORT_MODE_HUMIDITY_ASC;
                            Collections.sort(records);
                            median_humidity = records.get(records.size() / 2).getHumidity();
                            //Pressure
                            SensorActivity.sort_mode = SensorActivity.SORT_MODE_PRESSURE_ASC;
                            Collections.sort(records);
                            median_pressure = records.get(records.size() / 2).getPressure();
                            //Alten SortMode wiederherstellen
                            SensorActivity.sort_mode = current_sort_mode;
                            Collections.sort(records);

                            footer_median_p1.setText(String.valueOf(Tools.round(median_p1, 1)).replace(".", ",").concat(" µg/m³"));
                            footer_median_p2.setText(String.valueOf(Tools.round(median_p2, 1)).replace(".", ",").concat(" µg/m³"));
                            footer_median_temp.setText(String.valueOf(Tools.round(median_temp, 1)).replace(".", ",").concat(" °C"));
                            footer_median_humidity.setText(String.valueOf(Tools.round(median_humidity, 1)).replace(".", ",").concat(" %"));
                            footer_median_pressure.setText(String.valueOf(Tools.round(median_pressure, 1)).replace(".", ",").concat(" hPa"));
                        }
                    } else {
                        contentView.findViewById(R.id.data_heading).setVisibility(View.INVISIBLE);
                        contentView.findViewById(R.id.data_footer).setVisibility(View.INVISIBLE);
                    }
                } else {
                    contentView.findViewById(R.id.data_heading).setVisibility(View.INVISIBLE);
                    contentView.findViewById(R.id.data_footer).setVisibility(View.INVISIBLE);
                    contentView.findViewById(R.id.record_counter).setVisibility(View.INVISIBLE);
                    contentView.findViewById(R.id.no_data).setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {}
        }
    }
}