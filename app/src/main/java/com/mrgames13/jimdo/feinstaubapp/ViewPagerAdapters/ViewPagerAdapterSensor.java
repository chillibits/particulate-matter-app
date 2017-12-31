package com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mrgames13.jimdo.feinstaubapp.App.DiagramActivity;
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.DataAdapter;
import com.turingtechnologies.materialscrollbar.DragScrollBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ViewPagerAdapterSensor extends FragmentPagerAdapter {

    //Konstanten

    //Variablen als Objekte
    private static Resources res;
    public static SensorActivity activity;
    private ArrayList<String> tabTitles = new ArrayList<>();
    private static Handler h;
    public static ArrayList<DataRecord> records = new ArrayList<>();

    //Variablen

    public ViewPagerAdapterSensor(FragmentManager manager, SensorActivity activity) {
        super(manager);
        this.res = activity.getResources();
        this.activity = activity;
        this.h = new Handler();
        tabTitles.add(res.getString(R.string.tab_diagram));
        tabTitles.add(res.getString(R.string.tab_data));
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

    //-------------------------------------------Fragmente------------------------------------------

    public static class DiagramFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        public static View contentView;
        private static GraphView graph_view;
        private CheckBox custom_sdsp1;
        private CheckBox custom_sdsp2;
        private CheckBox custom_temp;
        private CheckBox custom_humidity;
        private static LineGraphSeries<DataPoint> series1;
        private static LineGraphSeries<DataPoint> series2;
        private static LineGraphSeries<DataPoint> series3;
        private static LineGraphSeries<DataPoint> series4;

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
                    if(SensorActivity.custom_sdsp1 || SensorActivity.custom_sdsp2 || SensorActivity.custom_temp || SensorActivity.custom_humidity) {
                        Intent i = new Intent(activity, DiagramActivity.class);
                        i.putExtra("Show1", SensorActivity.custom_sdsp1);
                        i.putExtra("Show2", SensorActivity.custom_sdsp2);
                        i.putExtra("Show3", SensorActivity.custom_temp);
                        i.putExtra("Show4", SensorActivity.custom_humidity);
                        startActivity(i);
                    } else {
                        Toast.makeText(activity, res.getString(R.string.no_diagram_selected), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            //CustomControls initialisieren
            custom_sdsp1 = contentView.findViewById(R.id.custom_sdsp1);
            custom_sdsp2 = contentView.findViewById(R.id.custom_sdsp2);
            custom_temp = contentView.findViewById(R.id.custom_temp);
            custom_humidity = contentView.findViewById(R.id.custom_humidity);

            h.post(new Runnable() {
                @Override
                public void run() {
                    custom_sdsp1.setChecked(SensorActivity.custom_sdsp1);
                    custom_sdsp2.setChecked(SensorActivity.custom_sdsp2);
                    custom_temp.setChecked(SensorActivity.custom_temp);
                    custom_humidity.setChecked(SensorActivity.custom_humidity);
                }
            });

            custom_sdsp1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                    SensorActivity.custom_sdsp1 = value;
                    updateSDSP1(value);
                }
            });
            custom_sdsp2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                    SensorActivity.custom_sdsp2 = value;
                    updateSDSP2(value);
                }
            });
            custom_temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                    SensorActivity.custom_temp = value;
                    updateTemp(value);
                }
            });
            custom_humidity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                    SensorActivity.custom_humidity = value;
                    updateHumidity(value);
                }
            });

            return contentView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
        }

        private static void updateSDSP1(boolean value) {
            if(value) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    long first_time = sdf.parse(records.get(0).getTime()).getTime() / 1000;

                    series1 = new LineGraphSeries<>();
                    series1.setColor(res.getColor(R.color.series1));
                    for(DataRecord record : fitArrayList(records)) {
                        Date time = sdf.parse(record.getTime());
                        series1.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getSdsp1()), false, 1000000);
                    }
                    graph_view.addSeries(series1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series1);
            }
        }

        private static void updateSDSP2(boolean value) {
            if(value) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    long first_time = sdf.parse(records.get(0).getTime()).getTime() / 1000;

                    series2 = new LineGraphSeries<>();
                    series2.setColor(res.getColor(R.color.series2));
                    for(DataRecord record : fitArrayList(records)) {
                        Date time = sdf.parse(record.getTime());
                        series2.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getSdsp2()), false, 1000000);
                    }
                    graph_view.addSeries(series2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series2);
            }
        }

        private static void updateTemp(boolean value) {
            if(value) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    long first_time = sdf.parse(records.get(0).getTime()).getTime() / 1000;

                    series3 = new LineGraphSeries<>();
                    series3.setColor(res.getColor(R.color.series3));
                    for(DataRecord record : fitArrayList(records)) {
                        Date time = sdf.parse(record.getTime());
                        series3.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getTemp()), false, 1000000);
                    }
                    graph_view.addSeries(series3);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series3);
            }
        }

        private static void updateHumidity(boolean value) {
            if(value) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    long first_time = sdf.parse(records.get(0).getTime()).getTime() / 1000;

                    series4 = new LineGraphSeries<>();
                    series4.setColor(res.getColor(R.color.series4));
                    for(DataRecord record : fitArrayList(records)) {
                        Date time = sdf.parse(record.getTime());
                        series4.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getHumidity()), false, 1000000);
                    }
                    graph_view.addSeries(series4);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                graph_view.removeSeries(series4);
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
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                        long first_time = sdf.parse(records.get(0).getTime()).getTime() / 1000;
                        graph_view.getViewport().setMinX(0);
                        graph_view.getViewport().setMaxX(Math.abs(sdf.parse(records.get(records.size() -1).getTime()).getTime() / 1000 - first_time));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    updateSDSP1(false);
                    updateSDSP1(SensorActivity.custom_sdsp1);
                    updateSDSP2(false);
                    updateSDSP2(SensorActivity.custom_sdsp2);
                    updateTemp(false);
                    updateTemp(SensorActivity.custom_temp);
                    updateHumidity(false);
                    updateHumidity(SensorActivity.custom_humidity);
                } else {
                    updateSDSP1(false);
                    updateSDSP2(false);
                    updateTemp(false);
                    updateHumidity(false);
                    contentView.findViewById(R.id.no_data).setVisibility(View.VISIBLE);
                }
            } else {
                updateSDSP1(false);
                updateSDSP2(false);
                updateTemp(false);
                updateHumidity(false);
                contentView.findViewById(R.id.no_data).setVisibility(View.VISIBLE);
            }
        }

        private static ArrayList<DataRecord> fitArrayList(ArrayList<DataRecord> records) {
            int divider = records.size() / 200;
            if(divider == 0) return records;
            ArrayList<DataRecord> new_records = new ArrayList<>();
            for(int i = 0; i < records.size(); i+=divider+1) new_records.add(records.get(i));
            return new_records;
        }
    }

    public static class DataFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        public static View contentView;
        private static RecyclerView data_view;
        private static DataAdapter data_view_adapter;
        private RecyclerView.LayoutManager data_view_manager;
        private static DragScrollBar scroll_bar;

        private TextView heading_time;
        private ImageView heading_time_arrow;
        private TextView heading_sdsp1;
        private ImageView heading_sdsp1_arrow;
        private TextView heading_sdsp2;
        private ImageView heading_sdsp2_arrow;
        private TextView heading_temp;
        private ImageView heading_temp_arrow;
        private TextView heading_humidity;
        private ImageView heading_humidity_arrow;
        private static TextView footer_sdsp1;
        private static TextView footer_sdsp2;
        private static TextView footer_temp;
        private static TextView footer_humidity;
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
                contentView.findViewById(R.id.data_footer).setVisibility(records.size() == 0 ? View.GONE : View.VISIBLE);

                heading_time = contentView.findViewById(R.id.heading_time);
                heading_time_arrow = contentView.findViewById(R.id.sort_time);
                heading_sdsp1 = contentView.findViewById(R.id.heading_sdsp1);
                heading_sdsp1_arrow = contentView.findViewById(R.id.sort_sdsp1);
                heading_sdsp2 = contentView.findViewById(R.id.heading_sdsp2);
                heading_sdsp2_arrow = contentView.findViewById(R.id.sort_sdsp2);
                heading_temp = contentView.findViewById(R.id.heading_temp);
                heading_temp_arrow = contentView.findViewById(R.id.sort_temp);
                heading_humidity = contentView.findViewById(R.id.heading_humidity);
                heading_humidity_arrow = contentView.findViewById(R.id.sort_humidity);

                footer_sdsp1 = contentView.findViewById(R.id.footer_sdsp1);
                footer_sdsp2 = contentView.findViewById(R.id.footer_sdsp2);
                footer_temp = contentView.findViewById(R.id.footer_temp);
                footer_humidity = contentView.findViewById(R.id.footer_humidity);

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
                heading_sdsp1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sdsp1SortClicked();
                    }
                });
                heading_sdsp1_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sdsp1SortClicked();
                    }
                });
                heading_sdsp2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sdsp2SortClicked();
                    }
                });
                heading_sdsp2_arrow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sdsp2SortClicked();
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
            }

            scroll_bar = contentView.findViewById(R.id.scroll_bar);

            return contentView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
        }

        private void timeSortClicked() {
            heading_time_arrow.setVisibility(View.VISIBLE);
            heading_sdsp1_arrow.setVisibility(View.INVISIBLE);
            heading_sdsp2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
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

        private void sdsp1SortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_sdsp1_arrow.setVisibility(View.VISIBLE);
            heading_sdsp2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_DESC;
                heading_sdsp1_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_ASC;
                heading_sdsp1_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private void sdsp2SortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_sdsp1_arrow.setVisibility(View.INVISIBLE);
            heading_sdsp2_arrow.setVisibility(View.VISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_DESC;
                heading_sdsp2_arrow.setImageResource(R.drawable.arrow_drop_down_grey);
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_ASC;
                heading_sdsp2_arrow.setImageResource(R.drawable.arrow_drop_up_grey);
            }
            SensorActivity.resortData();
            data_view.getAdapter().notifyDataSetChanged();
        }

        private void tempSortClicked() {
            heading_time_arrow.setVisibility(View.INVISIBLE);
            heading_sdsp1_arrow.setVisibility(View.INVISIBLE);
            heading_sdsp2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.VISIBLE);
            heading_humidity_arrow.setVisibility(View.INVISIBLE);
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
            heading_sdsp1_arrow.setVisibility(View.INVISIBLE);
            heading_sdsp2_arrow.setVisibility(View.INVISIBLE);
            heading_temp_arrow.setVisibility(View.INVISIBLE);
            heading_humidity_arrow.setVisibility(View.VISIBLE);
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

        public static void refresh() {
            try{
                if(records != null) {
                    data_view_adapter.notifyDataSetChanged();

                    contentView.findViewById(R.id.loading).setVisibility(View.GONE);
                    contentView.findViewById(R.id.no_data).setVisibility(records.size() == 0 ? View.VISIBLE : View.GONE);

                    if (records.size() > 0) {
                        contentView.findViewById(R.id.data_footer).setVisibility(records.size() == 0 ? View.GONE : View.VISIBLE);
                        record_conter = contentView.findViewById(R.id.record_conter);
                        record_conter.setVisibility(View.VISIBLE);
                        String footer_string = records.size() + " " + res.getString(R.string.tab_data) + " - " + res.getString(R.string.from) + " " + records.get(0).getTime() + " " + res.getString(R.string.to) + " " + records.get(records.size() - 1).getTime();
                        record_conter.setText(String.valueOf(footer_string));

                        double average_sdsp1 = 0;
                        double average_sdsp2 = 0;
                        double average_temp = 0;
                        double average_humidity = 0;
                        for (DataRecord record : records) {
                            average_sdsp1 += record.getSdsp1();
                            average_sdsp2 += record.getSdsp2();
                            average_temp += record.getTemp();
                            average_humidity += record.getHumidity();
                        }
                        average_sdsp1 = average_sdsp1 / records.size();
                        average_sdsp2 = average_sdsp2 / records.size();
                        average_temp = average_temp / records.size();
                        average_humidity = average_humidity / records.size();
                        footer_sdsp1.setText(String.valueOf(average_sdsp1));
                        footer_sdsp2.setText(String.valueOf(average_sdsp2));
                        footer_temp.setText(String.valueOf(average_temp));
                        footer_humidity.setText(String.valueOf(average_humidity));
                    }
                }
            } catch (Exception e) {}
        }
    }
}