package com.mrgames13.jimdo.feinstaubapp.App;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CompareActivity extends AppCompatActivity {

    //Konstanten

    //Variablen als Objekte
    private Resources res;
    private Toolbar toolbar;
    private Calendar calendar;
    public static ArrayList<Sensor> sensors;
    public static ArrayList<ArrayList<DataRecord>> records = new ArrayList<>();
    private MenuItem progress_menu_item;
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");
    private SimpleDateFormat sdf_time = new SimpleDateFormat("HH:mm");

    //Utils-Pakete
    private static StorageUtils su;
    private ServerMessagingUtils smu;

    //Komponenten
    private GraphView diagram_sdsp1;
    private GraphView diagram_sdsp2;
    private GraphView diagram_temp;
    private GraphView diagram_humidity;
    private GraphView diagram_pressure;
    private DatePickerDialog date_picker_dialog;

    //Variablen
    private String current_date_string;
    private String date_string;
    private boolean no_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getIntent().hasExtra("Title")) getSupportActionBar().setTitle(getIntent().getStringExtra("Title"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Kalender initialisieren
        if(date_string == null) {
            calendar = Calendar.getInstance();
            current_date_string = sdf_date.format(calendar.getTime());
            date_string = current_date_string;
        }

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(this, su);

        //Sensoren laden
        sensors = MainActivity.own_instance.selected_sensors;

        //Komponenten initialisieren
        final TextView card_date_value = findViewById(R.id.card_date_value);
        ImageView card_date_edit = findViewById(R.id.card_date_edit);
        ImageView card_date_back = findViewById(R.id.card_date_back);
        ImageView card_date_next = findViewById(R.id.card_date_next);

        card_date_value.setText(date_string);
        card_date_value.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Datum auswählen
                chooseDate(card_date_value);
            }
        });
        card_date_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Datum auswählen
                chooseDate(card_date_value);
            }
        });
        card_date_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Einen Tag zurück gehen
                calendar.add(Calendar.DATE, -1);

                date_string = sdf_date.format(calendar.getTime());
                card_date_value.setText(date_string);

                //Daten für ausgewähltes Datum laden
                reloadData();
            }
        });
        card_date_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Einen Tag vor gehen
                calendar.add(Calendar.DATE, 1);

                date_string = sdf_date.format(calendar.getTime());
                card_date_value.setText(date_string);

                //Daten für ausgewähltes Datum laden
                reloadData();
            }
        });

        diagram_sdsp1 = findViewById(R.id.diagram_sdsp1);
        diagram_sdsp1.getGridLabelRenderer().setNumHorizontalLabels(3);
        diagram_sdsp1.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
        diagram_sdsp1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CompareActivity.this, DiagramActivity.class);
                i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA);
                i.putExtra("Show1", true);
                startActivity(i);
            }
        });

        diagram_sdsp2 = findViewById(R.id.diagram_sdsp2);
        diagram_sdsp2.getGridLabelRenderer().setNumHorizontalLabels(3);
        diagram_sdsp2.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
        diagram_sdsp2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CompareActivity.this, DiagramActivity.class);
                i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA);
                i.putExtra("Show2", true);
                startActivity(i);
            }
        });

        diagram_temp = findViewById(R.id.diagram_temp);
        diagram_temp.getGridLabelRenderer().setNumHorizontalLabels(3);
        diagram_temp.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(!isValueX) return super.formatLabel(value, isValueX);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((long) value);
                return sdf_time.format(cal.getTime());
            }
        });
        diagram_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CompareActivity.this, DiagramActivity.class);
                i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA);
                i.putExtra("Show3", true);
                startActivity(i);
            }
        });

        diagram_humidity = findViewById(R.id.diagram_humidity);
        diagram_humidity.getGridLabelRenderer().setNumHorizontalLabels(3);
        diagram_humidity.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(!isValueX) return super.formatLabel(value, isValueX);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((long) value);
                return sdf_time.format(cal.getTime());
            }
        });
        diagram_humidity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CompareActivity.this, DiagramActivity.class);
                i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA);
                i.putExtra("Show4", true);
                startActivity(i);
            }
        });

        diagram_pressure = findViewById(R.id.diagram_pressure);
        diagram_pressure.getGridLabelRenderer().setNumHorizontalLabels(3);
        diagram_pressure.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(!isValueX) return super.formatLabel(value, isValueX);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis((long) value);
                return sdf_time.format(cal.getTime());
            }
        });
        diagram_pressure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CompareActivity.this, DiagramActivity.class);
                i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA);
                i.putExtra("Show5", true);
                startActivity(i);
            }
        });

        reloadData();
    }

    private void chooseDate(final TextView card_date_value) {
        //Datum auswählen
        date_picker_dialog = new DatePickerDialog(CompareActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);

                date_string = sdf_date.format(calendar.getTime());
                card_date_value.setText(date_string);

                //Daten für ausgewähltes Datum laden
                reloadData();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        date_picker_dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_sensor, menu);
        progress_menu_item = menu.findItem(R.id.action_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        } else if(id == R.id.action_refresh) {
            Log.i("FA", "User refreshing ...");
            //Daten neu laden
            reloadData();
        } else if(id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void reloadData() {
        //ProgressMenuItem setzen
        if(progress_menu_item != null) progress_menu_item.setActionView(R.layout.menu_item_loading);

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setMessage(res.getString(R.string.loading_data));
        pd.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ArrayList leeren
                records.clear();
                //Diagramme leeren
                diagram_sdsp1.removeAllSeries();
                diagram_sdsp2.removeAllSeries();
                diagram_humidity.removeAllSeries();
                diagram_temp.removeAllSeries();
                diagram_pressure.removeAllSeries();

                //Date String von Gestern ermitteln
                String date_yesterday = date_string;
                try{
                    Calendar c = Calendar.getInstance();
                    c.setTime(sdf_date.parse(date_yesterday));
                    c.add(Calendar.DATE, -1);
                    date_yesterday = sdf_date.format(c.getTime());
                } catch (Exception e) {}

                //Zeit des ersten Datensatzes ermitteln
                long first_time = Long.MAX_VALUE;
                long last_time = Long.MIN_VALUE;
                for(int i = 0; i < sensors.size(); i++) {
                    smu.manageDownloads(sensors.get(i), date_string, date_yesterday);

                    ArrayList<DataRecord> temp = su.getDataRecordsFromCSV(su.getCSVFromFile(date_yesterday, sensors.get(i).getId()));
                    temp.addAll(su.getDataRecordsFromCSV(su.getCSVFromFile(date_string, sensors.get(i).getId())));
                    temp = su.trimDataRecords(temp, date_string);
                    records.add(temp); // Muss add heißen, nicht addAll, weil es eine ArrayList in der ArrayList ist.
                    try{
                        long current_first_time = records.get(i).get(0).getDateTime().getTime();
                        long current_last_time = records.get(i).get(records.get(i).size() -1).getDateTime().getTime();
                        first_time = current_first_time < first_time ? current_first_time : first_time;
                        last_time = current_last_time > last_time ? current_last_time : last_time;
                    } catch (Exception e) {}
                }

                no_data = true;
                for(int i = 0; i < sensors.size(); i++) {
                    ArrayList<DataRecord> current_records = records.get(i);
                    if(current_records.size() > 0) {
                        no_data = false;
                        try{
                            LineGraphSeries<DataPoint> series_sdsp1 = new LineGraphSeries<>();
                            series_sdsp1.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_sdsp1.appendData(new DataPoint(time.getTime(), record.getSdsp1()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_sdsp1.addSeries(series_sdsp1);
                            diagram_sdsp1.getViewport().setScalable(true);
                            diagram_sdsp1.getViewport().setMinX(first_time);
                            diagram_sdsp1.getViewport().setMaxX(last_time);
                            diagram_sdsp1.getViewport().scrollToEnd();
                            diagram_sdsp1.getViewport().setScalable(false);

                            LineGraphSeries<DataPoint> series_sdsp2 = new LineGraphSeries<>();
                            series_sdsp2.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_sdsp2.appendData(new DataPoint(time.getTime(), record.getSdsp2()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_sdsp2.addSeries(series_sdsp2);
                            diagram_sdsp2.getViewport().setScalable(true);
                            diagram_sdsp2.getViewport().setMinX(first_time);
                            diagram_sdsp2.getViewport().setMaxX(last_time);
                            diagram_sdsp2.getViewport().scrollToEnd();
                            diagram_sdsp2.getViewport().setScalable(false);

                            LineGraphSeries<DataPoint> series_temp = new LineGraphSeries<>();
                            series_temp.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_temp.appendData(new DataPoint(time.getTime(), record.getTemp()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_temp.addSeries(series_temp);
                            diagram_temp.getViewport().setScalable(true);
                            diagram_temp.getViewport().setMinX(first_time);
                            diagram_temp.getViewport().setMaxX(last_time);
                            diagram_temp.getViewport().scrollToEnd();
                            diagram_temp.getViewport().setScalable(false);

                            LineGraphSeries<DataPoint> series_humidity = new LineGraphSeries<>();
                            series_humidity.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_humidity.appendData(new DataPoint(time.getTime(), record.getHumidity()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_humidity.addSeries(series_humidity);
                            diagram_humidity.getViewport().setScalable(true);
                            diagram_humidity.getViewport().setMinX(first_time);
                            diagram_humidity.getViewport().setMaxX(last_time);
                            diagram_humidity.getViewport().scrollToEnd();
                            diagram_humidity.getViewport().setScalable(false);

                            LineGraphSeries<DataPoint> series_pressure = new LineGraphSeries<>();
                            series_pressure.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_pressure.appendData(new DataPoint(time.getTime(), record.getPressure()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_pressure.addSeries(series_pressure);
                            diagram_pressure.getViewport().setScalable(true);
                            diagram_pressure.getViewport().setMinX(first_time);
                            diagram_pressure.getViewport().setMaxX(last_time);
                            diagram_pressure.getViewport().scrollToEnd();
                            diagram_pressure.getViewport().setScalable(false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            findViewById(R.id.no_data).setVisibility(no_data ? View.VISIBLE : View.GONE);
                            findViewById(R.id.container).setVisibility(no_data ? View.GONE : View.VISIBLE);
                            //ProgressMenuItem zurücksetzen
                            if(progress_menu_item != null) progress_menu_item.setActionView(null);
                            pd.dismiss();
                        } catch (Exception e) {}
                    }
                });
            }
        }).start();
    }
}