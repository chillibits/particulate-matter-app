package com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters;

import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.mrgames13.jimdo.feinstaubapp.App.DiagramActivity;
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.DiagramEntry;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.TimeFormatter;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.DataAdapter;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class ViewPagerAdapterSensor extends FragmentPagerAdapter {

    //Konstanten

    //Variablen als Objekte
    private static Resources res;
    private static SensorActivity activity;
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
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }

    public void refreshFragments() {
        DiagramFragment.refresh();
        DataFragment.refresh();
    }

    public void exportDiagram() {
        DiagramFragment.exportDiagram();
    }

    public void showGPSData(boolean show) {
        DataFragment.showGPSData(show);
    }

    //-------------------------------------------Fragmente------------------------------------------

    public static class DiagramFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        private static View contentView;
        private static LineChart chart;
        private static CheckBox custom_p1;
        private static CheckBox custom_p2;
        private static CheckBox custom_temp;
        private static CheckBox custom_humidity;
        private static CheckBox custom_pressure;
        private static RadioButton custom_average;
        private static RadioButton custom_median;
        private static RadioButton custom_threshold_who;
        private static RadioButton custom_threshold_eu;
        private static LineDataSet p1;
        private static LineDataSet p2;
        private static LineDataSet temp;
        private static LineDataSet humidity;
        private static LineDataSet pressure;
        private static LineDataSet av_p1;
        private static LineDataSet av_p2;
        private static LineDataSet av_temp;
        private static LineDataSet av_humidity;
        private static LineDataSet av_pressure;
        private static LineDataSet med_p1;
        private static LineDataSet med_p2;
        private static LineDataSet med_temp;
        private static LineDataSet med_humidity;
        private static LineDataSet med_pressure;
        private static LineDataSet th_eu_p1;
        private static LineDataSet th_eu_p2;
        private static LineDataSet th_who_p1;
        private static LineDataSet th_who_p2;
        private static ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        private static ArrayList<ILineDataSet> dataSetsFull = new ArrayList<>();
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

            chart = contentView.findViewById(R.id.chart);
            chart.setHardwareAccelerationEnabled(true);
            chart.setDoubleTapToZoomEnabled(false);
            chart.setScaleEnabled(false);
            chart.setPinchZoom(false);
            chart.setHighlightPerTapEnabled(false);
            chart.setHighlightPerDragEnabled(false);
            chart.setDescription(null);
            chart.getLegend().setEnabled(false);
            //Linke y-Achse
            YAxis left = chart.getAxisLeft();
            left.setValueFormatter(new LargeValueFormatter());
            left.setDrawAxisLine(true);
            left.setDrawGridLines(false);
            left.setAxisMinimum(0);
            //Rechte y-Achse
            YAxis right = chart.getAxisRight();
            right.setValueFormatter(new LargeValueFormatter());
            right.setDrawAxisLine(true);
            right.setDrawGridLines(false);
            //x-Achse
            XAxis xAxis = chart.getXAxis();
            xAxis.setGranularity(60f);
            xAxis.setGranularityEnabled(true);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            //OnClickListener setzen
            chart.setOnClickListener(new View.OnClickListener() {
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
            RadioButton custom_nothing = contentView.findViewById(R.id.enable_average_median_nothing);
            custom_average = contentView.findViewById(R.id.enable_average);
            custom_median = contentView.findViewById(R.id.enable_median);
            RadioButton custom_threshold_nothing = contentView.findViewById(R.id.enable_eu_who_nothing);
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
                    if(dataSets.size() >= 5) {
                        av_p1.setVisible(custom_average.isChecked() && value);
                        med_p1.setVisible(custom_median.isChecked() && value);
                        th_eu_p1.setVisible(custom_threshold_eu.isChecked() && value);
                        th_who_p1.setVisible(custom_threshold_who.isChecked() && value);

                        /*double highest = 0;
                        if(value) highest = Tools.findHighestMeasurement(records, 1);
                        if(custom_p2.isChecked()) highest = Math.max(highest, Tools.findHighestMeasurement(records, 2));
                        Log.d("FA", String.valueOf(highest));
                        chart.getAxisLeft().setAxisMaximum((float) (highest));*/

                        showGraph(0, value);
                    }
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
                    if(dataSets.size() >= 5) {
                        av_p2.setVisible(custom_average.isChecked() && value);
                        med_p2.setVisible(custom_median.isChecked() && value);
                        th_eu_p2.setVisible(custom_threshold_eu.isChecked() && value);
                        th_who_p2.setVisible(custom_threshold_who.isChecked() && value);

                        /*double highest = 0;
                        if(custom_p1.isChecked()) highest = Tools.findHighestMeasurement(records, 1);
                        if(value) highest = Math.max(highest, Tools.findHighestMeasurement(records, 2));
                        Log.d("FA", String.valueOf(highest));
                        chart.getAxisLeft().setAxisMaximum((float) (highest));*/

                        showGraph(1, value);
                    }
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
                    if(dataSets.size() >= 5) {
                        av_temp.setVisible(custom_average.isChecked() && value);
                        med_temp.setVisible(custom_median.isChecked() && value);
                        showGraph(2, value);
                    }
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
                    if(dataSets.size() >= 5) {
                        av_humidity.setVisible(custom_average.isChecked() && value);
                        med_humidity.setVisible(custom_median.isChecked() && value);
                        showGraph(3, value);
                    }
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
                    if(dataSets.size() >= 5) {
                        av_pressure.setVisible(custom_average.isChecked() && value);
                        med_pressure.setVisible(custom_median.isChecked() && value);
                        showGraph(4, value);
                    }
                }
            });
            custom_nothing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if(checked) {
                        av_p1.setVisible(false);
                        av_p2.setVisible(false);
                        av_temp.setVisible(false);
                        av_humidity.setVisible(false);
                        av_pressure.setVisible(false);
                        med_p1.setVisible(false);
                        med_p2.setVisible(false);
                        med_temp.setVisible(false);
                        med_humidity.setVisible(false);
                        med_pressure.setVisible(false);
                        chart.invalidate();
                    }
                }
            });
            custom_average.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if(checked) {
                        if(SensorActivity.custom_p1) av_p1.setVisible(true);
                        if(SensorActivity.custom_p2) av_p2.setVisible(true);
                        if(SensorActivity.custom_temp) av_temp.setVisible(true);
                        if(SensorActivity.custom_humidity) av_humidity.setVisible(true);
                        if(SensorActivity.custom_pressure) av_pressure.setVisible(true);
                        med_p1.setVisible(false);
                        med_p2.setVisible(false);
                        med_temp.setVisible(false);
                        med_humidity.setVisible(false);
                        med_pressure.setVisible(false);
                        chart.invalidate();
                    }
                }
            });
            custom_median.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if(checked) {
                        av_p1.setVisible(false);
                        av_p2.setVisible(false);
                        av_temp.setVisible(false);
                        av_humidity.setVisible(false);
                        av_pressure.setVisible(false);
                        if(SensorActivity.custom_p1) med_p1.setVisible(true);
                        if(SensorActivity.custom_p2) med_p2.setVisible(true);
                        if(SensorActivity.custom_temp) med_temp.setVisible(true);
                        if(SensorActivity.custom_humidity) med_humidity.setVisible(true);
                        if(SensorActivity.custom_pressure) med_pressure.setVisible(true);
                        chart.invalidate();
                    }
                }
            });
            custom_threshold_nothing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if(checked) {
                        th_eu_p1.setVisible(false);
                        th_eu_p2.setVisible(false);
                        th_who_p1.setVisible(false);
                        th_who_p2.setVisible(false);
                        chart.invalidate();
                    }
                }
            });
            custom_threshold_who.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if(checked) {
                        th_eu_p1.setVisible(false);
                        th_eu_p2.setVisible(false);
                        if(SensorActivity.custom_p1) th_who_p1.setVisible(true);
                        if(SensorActivity.custom_p2) th_who_p2.setVisible(true);
                        chart.invalidate();
                    }
                }
            });
            custom_threshold_eu.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if(checked) {
                        if(SensorActivity.custom_p1) th_eu_p1.setVisible(true);
                        if(SensorActivity.custom_p2) th_eu_p2.setVisible(true);
                        th_who_p1.setVisible(false);
                        th_who_p2.setVisible(false);
                        chart.invalidate();
                    }
                }
            });

            cv_p1 = contentView.findViewById(R.id.cv_p1);
            cv_p2 = contentView.findViewById(R.id.cv_p2);
            cv_temp = contentView.findViewById(R.id.cv_temp);
            cv_humidity = contentView.findViewById(R.id.cv_humidity);
            cv_pressure = contentView.findViewById(R.id.cv_pressure);
            cv_time = contentView.findViewById(R.id.cv_time);

            return contentView;
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

        private static void showGraph(int index, boolean show) {
            dataSets.get(index).setVisible(show);
            /*if(show) dataSets.set(index, dataSetsFull.get(index));
            if(!show) dataSets.set(index, new LineDataSet(null, ""));*/
            chart.fitScreen();
            chart.invalidate();
        }

        @NotNull
        private static LineDataSet getAverageMedianPM1(boolean enable_average, boolean enable_median, long first_timestamp) {
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
                double median = Tools.calculateMedian(double_records);
                am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
                am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            }
            LineDataSet p1_am = getDashedLine(am_entries, R.color.series1);
            p1_am.setVisible(enable_average ? custom_average.isChecked() && custom_p1.isChecked() : custom_median.isChecked() && custom_p1.isChecked());
            p1_am.setAxisDependency(YAxis.AxisDependency.LEFT);
            return p1_am;
        }

        @NotNull
        private static LineDataSet getAverageMedianPM2(boolean enable_average, boolean enable_median, long first_timestamp) {
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
                double median = Tools.calculateMedian(double_records);
                am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
                am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            }
            LineDataSet p2_am = getDashedLine(am_entries, R.color.series2);
            p2_am.setVisible(enable_average ? custom_average.isChecked() && custom_p2.isChecked() : custom_median.isChecked() && custom_p2.isChecked());
            p2_am.setAxisDependency(YAxis.AxisDependency.LEFT);
            return p2_am;
        }

        @NotNull
        private static LineDataSet getAverageMedianTemperature(boolean enable_average, boolean enable_median, long first_timestamp) {
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
                double median = Tools.calculateMedian(double_records);
                am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
                am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            }
            LineDataSet temp_am = getDashedLine(am_entries, R.color.series3);
            temp_am.setVisible(enable_average ? custom_average.isChecked() && custom_temp.isChecked() : custom_median.isChecked() && custom_temp.isChecked());
            temp_am.setAxisDependency(YAxis.AxisDependency.RIGHT);
            return temp_am;
        }

        @NotNull
        private static LineDataSet getAverageMedianHumidity(boolean enable_average, boolean enable_median, long first_timestamp) {
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
                double median = Tools.calculateMedian(double_records);
                am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
                am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            }
            LineDataSet humidity_am = getDashedLine(am_entries, R.color.series4);
            humidity_am.setVisible(enable_average ? custom_average.isChecked() && custom_humidity.isChecked() : custom_median.isChecked() && custom_humidity.isChecked());
            humidity_am.setAxisDependency(YAxis.AxisDependency.RIGHT);
            return humidity_am;
        }

        @NotNull
        private static LineDataSet getAverageMedianPressure(boolean enable_average, boolean enable_median, long first_timestamp) {
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
                double median = Tools.calculateMedian(double_records);
                am_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
                am_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) median));
            }
            LineDataSet pressure_am = getDashedLine(am_entries, R.color.series5);
            pressure_am.setVisible(enable_average ? custom_average.isChecked() && custom_pressure.isChecked() : custom_median.isChecked() && custom_pressure.isChecked());
            pressure_am.setAxisDependency(YAxis.AxisDependency.RIGHT);
            return pressure_am;
        }

        @NotNull
        private static LineDataSet getThresholdPM1(boolean enable_eu_thresholds, boolean enable_who_thresholds, long first_timestamp) {
            List<Entry> th_entries;
            th_entries = new ArrayList<>();
            if(enable_eu_thresholds) {
                th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM10));
                th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM10));
            } else if(enable_who_thresholds) {
                th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM10));
                th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM10));
            }
            LineDataSet th_p1 = getDashedLine(th_entries, R.color.error);
            th_p1.setVisible(enable_eu_thresholds ? custom_threshold_eu.isChecked() && custom_p1.isChecked() : custom_threshold_who.isChecked() && custom_p1.isChecked());
            return th_p1;
        }

        @NotNull
        private static LineDataSet getThresholdPM2(boolean enable_eu_thresholds, boolean enable_who_thresholds, long first_timestamp) {
            List<Entry> th_entries;
            th_entries = new ArrayList<>();
            if(enable_eu_thresholds) {
                th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM2_5));
                th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_EU_PM2_5));
            } else if(enable_who_thresholds) {
                th_entries.add(new Entry((float) ((records.get(0).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM2_5));
                th_entries.add(new Entry((float) ((records.get(records.size() -1).getDateTime().getTime() - first_timestamp) / 1000), (float) Constants.THRESHOLD_WHO_PM2_5));
            }
            LineDataSet th_p2 = getDashedLine(th_entries, R.color.error);
            th_p2.setVisible(enable_eu_thresholds ? custom_threshold_eu.isChecked() && custom_p2.isChecked() : custom_threshold_who.isChecked() && custom_p2.isChecked());
            return th_p2;
        }

        @NotNull
        private static LineDataSet getDashedLine(List<Entry> am_entries, int color) {
            LineDataSet dl = new LineDataSet(am_entries, null);
            dl.setColor(res.getColor(color));
            dl.setLineWidth(1);
            dl.setDrawValues(false);
            dl.setDrawCircles(false);
            dl.setHighlightEnabled(false);
            dl.enableDashedLine(10f, 10f, 0);
            return dl;
        }

        public static void refresh() {
            if(records != null) {
                contentView.findViewById(R.id.loading).setVisibility(View.GONE);
                if(records.size() > 0) {
                    contentView.findViewById(R.id.no_data).setVisibility(View.GONE);
                    contentView.findViewById(R.id.diagram_container).setVisibility(View.VISIBLE);

                    //Datensätze sortieren
                    int tmp = SensorActivity.sort_mode;
                    SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC;
                    Collections.sort(records);
                    SensorActivity.sort_mode = tmp;

                    //Daten eintragen
                    List<Entry> entries_1 = new ArrayList<>();
                    List<Entry> entries_2 = new ArrayList<>();
                    List<Entry> entries_3 = new ArrayList<>();
                    List<Entry> entries_4 = new ArrayList<>();
                    List<Entry> entries_5 = new ArrayList<>();
                    long first_time = records.get(0).getDateTime().getTime();
                    chart.getXAxis().setValueFormatter(new TimeFormatter(first_time));
                    for (DataRecord r : records) {
                        entries_1.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getP1().floatValue(), "µg/m³"));
                        entries_2.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getP2().floatValue(), "µg/m³"));
                        entries_3.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getTemp().floatValue(), "°C"));
                        entries_4.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getHumidity().floatValue(), "%"));
                        entries_5.add(new DiagramEntry((float) ((r.getDateTime().getTime() - first_time) / 1000), r.getPressure().floatValue(), "hPa"));
                    }

                    //Normale Linien
                    //PM1
                    p1 = new LineDataSet(entries_1, res.getString(R.string.value1) + " (µg/m³)");
                    p1.setColor(res.getColor(R.color.series1));
                    p1.setDrawCircles(false);
                    p1.setLineWidth(1.5f);
                    p1.setDrawValues(false);
                    p1.setAxisDependency(YAxis.AxisDependency.LEFT);
                    p1.setVisible(SensorActivity.custom_p1);

                    //PM2
                    p2 = new LineDataSet(entries_2, res.getString(R.string.value2) + " (µg/m³)");
                    p2.setColor(res.getColor(R.color.series2));
                    p2.setDrawCircles(false);
                    p2.setLineWidth(1.5f);
                    p2.setDrawValues(false);
                    p2.setAxisDependency(YAxis.AxisDependency.LEFT);
                    p2.setVisible(SensorActivity.custom_p2);

                    //Temperature
                    temp = new LineDataSet(entries_3, res.getString(R.string.temperature) + " (°C)");
                    temp.setColor(res.getColor(R.color.series3));
                    temp.setDrawCircles(false);
                    temp.setLineWidth(1.5f);
                    temp.setDrawValues(false);
                    temp.setAxisDependency(YAxis.AxisDependency.RIGHT);
                    temp.setVisible(SensorActivity.custom_temp);

                    //Humidity
                    humidity = new LineDataSet(entries_4, res.getString(R.string.humidity) + " (%)");
                    humidity.setColor(res.getColor(R.color.series4));
                    humidity.setDrawCircles(false);
                    humidity.setLineWidth(1.5f);
                    humidity.setDrawValues(false);
                    humidity.setAxisDependency(YAxis.AxisDependency.RIGHT);
                    humidity.setVisible(SensorActivity.custom_humidity);

                    //Pressure
                    pressure = new LineDataSet(entries_5, res.getString(R.string.pressure) + " (hPa)");
                    pressure.setColor(res.getColor(R.color.series5));
                    pressure.setDrawCircles(false);
                    pressure.setLineWidth(1.5f);
                    pressure.setDrawValues(false);
                    pressure.setAxisDependency(YAxis.AxisDependency.RIGHT);
                    pressure.setVisible(SensorActivity.custom_pressure);

                    //Durchschnitte
                    av_p1 = getAverageMedianPM1(true, false, first_time);
                    av_p2 = getAverageMedianPM2(true, false, first_time);
                    av_temp = getAverageMedianTemperature(true, false, first_time);
                    av_humidity = getAverageMedianHumidity(true, false, first_time);
                    av_pressure = getAverageMedianPressure(true, false, first_time);

                    //Mediane
                    med_p1 = getAverageMedianPM1(false, true, first_time);
                    med_p2 = getAverageMedianPM2(false, true, first_time);
                    med_temp = getAverageMedianTemperature(false, true, first_time);
                    med_humidity = getAverageMedianHumidity(false, true, first_time);
                    med_pressure = getAverageMedianPressure(false, true, first_time);

                    //Grenzwerte
                    th_eu_p1 = getThresholdPM1(true, false, first_time);
                    th_eu_p2 = getThresholdPM2(true, false, first_time);
                    th_who_p1 = getThresholdPM1(false, true, first_time);
                    th_who_p2 = getThresholdPM2(false, true, first_time);

                    //Die einzelnen Linien zu eimem Diagramm zusammenfassen
                    dataSets.clear();
                    dataSets.add(p1);
                    dataSets.add(p2);
                    dataSets.add(temp);
                    dataSets.add(humidity);
                    dataSets.add(pressure);
                    dataSets.add(av_p1);
                    dataSets.add(av_p2);
                    dataSets.add(av_temp);
                    dataSets.add(av_humidity);
                    dataSets.add(av_pressure);
                    dataSets.add(med_p1);
                    dataSets.add(med_p2);
                    dataSets.add(med_temp);
                    dataSets.add(med_humidity);
                    dataSets.add(med_pressure);
                    dataSets.add(th_eu_p1);
                    dataSets.add(th_eu_p2);
                    dataSets.add(th_who_p1);
                    dataSets.add(th_who_p2);
                    dataSetsFull = (ArrayList<ILineDataSet>) dataSets.clone();
                    chart.setData(new LineData(dataSets));

                    //Neu zeichnen & animieren
                    chart.invalidate();
                    chart.animateY(700, Easing.EaseInCubic);
                } else {
                    contentView.findViewById(R.id.diagram_container).setVisibility(View.GONE);
                    contentView.findViewById(R.id.no_data).setVisibility(View.VISIBLE);
                }
                updateLastValues();
            } else {
                contentView.findViewById(R.id.diagram_container).setVisibility(View.GONE);
                contentView.findViewById(R.id.no_data).setVisibility(View.VISIBLE);
            }
        }

        private static void exportDiagram() {
            su.shareImage(chart.getChartBitmap(), activity.getString(R.string.export_diagram));
        }
    }

    public static class DataFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        private static View contentView;
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