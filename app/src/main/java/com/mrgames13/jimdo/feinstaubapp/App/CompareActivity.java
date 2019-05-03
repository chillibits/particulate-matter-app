package com.mrgames13.jimdo.feinstaubapp.App;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CompareActivity extends AppCompatActivity {

    //Konstanten
    private static final int REQ_WRITE_EXTERNAL_STORAGE = 1;

    //Variablen als Objekte
    private Resources res;
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
    private GraphView diagram_p1;
    private GraphView diagram_p2;
    private GraphView diagram_temp;
    private GraphView diagram_humidity;
    private GraphView diagram_pressure;
    private ImageView card_date_next;

    //Variablen
    public static long selected_day_timestamp;
    public static long current_day_timestamp;
    private boolean no_data;
    private int export_option;
    private long first_time;
    private long last_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.compare_sensors);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Kalender initialisieren
        if(selected_day_timestamp == 0 || calendar == null) {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            current_day_timestamp = calendar.getTime().getTime();
            selected_day_timestamp = current_day_timestamp;
        }

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(this, su);

        //Sensoren laden
        if(!getIntent().hasExtra("Sensors")) {
            finish();
            return;
        }
        sensors = (ArrayList<Sensor>) getIntent().getSerializableExtra("Sensors");

        //Komponenten initialisieren
        final TextView card_date_value = findViewById(R.id.card_date_value);
        ImageView card_date_edit = findViewById(R.id.card_date_edit);
        ImageView card_date_today = findViewById(R.id.card_date_today);
        ImageView card_date_back = findViewById(R.id.card_date_back);
        card_date_next = findViewById(R.id.card_date_next);

        card_date_value.setText(sdf_date.format(calendar.getTime()));
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
        card_date_today.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Datum auf den heutigen Tag setzen
                calendar.setTime(new Date());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                selected_day_timestamp = calendar.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar.getTime()));

                card_date_next.setEnabled(false);

                //Daten für ausgewähltes Datum laden
                loadData();
            }
        });
        card_date_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Einen Tag zurück gehen
                calendar.add(Calendar.DATE, -1);

                selected_day_timestamp = calendar.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar.getTime()));

                Calendar current_calendar = Calendar.getInstance();
                current_calendar.set(Calendar.HOUR_OF_DAY, 0);
                current_calendar.set(Calendar.MINUTE, 0);
                current_calendar.set(Calendar.SECOND, 0);
                current_calendar.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar.before(current_calendar));

                //Daten für ausgewähltes Datum laden
                loadData();
            }
        });
        card_date_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Einen Tag vor gehen
                calendar.add(Calendar.DATE, 1);

                selected_day_timestamp = calendar.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar.getTime()));

                Calendar current_calendar = Calendar.getInstance();
                current_calendar.set(Calendar.HOUR_OF_DAY, 0);
                current_calendar.set(Calendar.MINUTE, 0);
                current_calendar.set(Calendar.SECOND, 0);
                current_calendar.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar.before(current_calendar));

                //Daten für ausgewähltes Datum laden
                loadData();
            }
        });
        card_date_next.setEnabled(false);

        diagram_p1 = findViewById(R.id.diagram_p1);
        diagram_p1.getGridLabelRenderer().setNumHorizontalLabels(3);
        diagram_p1.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
        diagram_p1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CompareActivity.this, DiagramActivity.class);
                i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA);
                i.putExtra("Show1", true);
                startActivity(i);
            }
        });
        //diagram_p1.getGridLabelRenderer().setVerticalAxisTitle("µg/m³");

        diagram_p2 = findViewById(R.id.diagram_p2);
        diagram_p2.getGridLabelRenderer().setNumHorizontalLabels(3);
        diagram_p2.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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
        diagram_p2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(CompareActivity.this, DiagramActivity.class);
                i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA);
                i.putExtra("Show2", true);
                startActivity(i);
            }
        });
        //diagram_p2.getGridLabelRenderer().setVerticalAxisTitle("µg/m³");

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
        //diagram_temp.getGridLabelRenderer().setVerticalAxisTitle("°C³");

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
        //diagram_humidity.getGridLabelRenderer().setVerticalAxisTitle("%");

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
        //diagram_pressure.getGridLabelRenderer().setVerticalAxisTitle("hPa");

        loadData();
    }

    private void chooseDate(final TextView card_date_value) {
        //Datum auswählen
        //Daten für ausgewähltes Datum laden
        DatePickerDialog date_picker_dialog = new DatePickerDialog(CompareActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Calendar calendar_new = Calendar.getInstance();
                calendar_new.set(Calendar.YEAR, year);
                calendar_new.set(Calendar.MONTH, month);
                calendar_new.set(Calendar.DAY_OF_MONTH, day);
                calendar_new.set(Calendar.HOUR_OF_DAY, 0);
                calendar_new.set(Calendar.MINUTE, 0);
                calendar_new.set(Calendar.SECOND, 0);
                calendar_new.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar_new.before(calendar));

                selected_day_timestamp = calendar_new.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar_new.getTime()));

                calendar = calendar_new;

                //Daten für ausgewähltes Datum laden
                loadData();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        date_picker_dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        date_picker_dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_compare, menu);
        progress_menu_item = menu.findItem(R.id.action_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        } else if(id == R.id.action_export) {
            boolean empty = true;
            for(ArrayList<DataRecord> r : records){
                if(!r.isEmpty()) empty = false;
            }
            if(!empty) {
                View v = getLayoutInflater().inflate(R.layout.dialog_export_compare, null);
                final RadioButton export_p1 = v.findViewById(R.id.export_diagram_p1);
                final RadioButton export_p2 = v.findViewById(R.id.export_diagram_p2);
                final RadioButton export_temp = v.findViewById(R.id.export_diagram_temp);
                final RadioButton export_humidity = v.findViewById(R.id.export_diagram_humidity);
                final RadioButton export_pressure = v.findViewById(R.id.export_diagram_pressure);
                AlertDialog d = new AlertDialog.Builder(this)
                        .setTitle(R.string.export_diagram)
                        .setView(v)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(export_p1.isChecked()) {
                                    export_option = 1;
                                } else if(export_p2.isChecked()) {
                                    export_option = 2;
                                } else if(export_temp.isChecked()) {
                                    export_option = 3;
                                } else if(export_humidity.isChecked()) {
                                    export_option = 4;
                                } else if(export_pressure.isChecked()) {
                                    export_option = 5;
                                }
                                exportData();
                            }
                        })
                        .create();
                d.show();
            } else {
                Toast.makeText(this, R.string.no_data_date, Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.action_refresh) {
            Log.i("FA", "User refreshing ...");
            //Daten neu laden
            loadData();
        } else if(id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ_WRITE_EXTERNAL_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) exportData();
    }

    private void loadData() {
        //ProgressMenuItem setzen
        if(progress_menu_item != null) progress_menu_item.setActionView(R.layout.menu_item_loading);

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setTitle(res.getString(R.string.loading_data));
        pd.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ArrayList leeren
                records.clear();
                //Diagramme leeren
                diagram_p1.removeAllSeries();
                diagram_p2.removeAllSeries();
                diagram_humidity.removeAllSeries();
                diagram_temp.removeAllSeries();
                diagram_pressure.removeAllSeries();

                //Timestamps für from und to ermitteln
                long from = selected_day_timestamp;
                long to = selected_day_timestamp + TimeUnit.DAYS.toMillis(1);

                //Zeit des ersten Datensatzes ermitteln
                first_time = Long.MAX_VALUE;
                last_time = Long.MIN_VALUE;
                pd.setMax(sensors.size());
                for(int i = 0; i < sensors.size(); i++) {
                    //Existierenden Datensätze aus der lokalen Datenbank laden
                    ArrayList<DataRecord> current_records = su.loadRecords(sensors.get(i).getChipID(), from, to);
                    //Sortieren nach Uhrzeit
                    Collections.sort(current_records);
                    //Wenn der letzte Datensatz mehr als 30s her
                    if((current_records.size() > 0 ? current_records.get(current_records.size() -1).getDateTime().getTime() : from) < System.currentTimeMillis() - 30000) {
                        //Prüfen, ob Intenet verfügbar ist
                        if(smu.isInternetAvailable()) {
                            //Internet ist verfügbar
                            current_records.addAll(smu.manageDownloadsRecords(sensors.get(i).getChipID(), current_records.size() > 0 && selected_day_timestamp == current_day_timestamp ? current_records.get(current_records.size() -1).getDateTime().getTime() +1000 : from, to));
                        }
                    }
                    //Sortieren nach Uhrzeit
                    Collections.sort(current_records);
                    //Datensätze zur Liste hinzufügen
                    records.add(current_records); // Muss add heißen, nicht addAll, weil es eine ArrayList in der ArrayList ist.
                    try{
                        long current_first_time = records.get(i).get(0).getDateTime().getTime();
                        long current_last_time = records.get(i).get(records.get(i).size() -1).getDateTime().getTime();
                        first_time = current_first_time < first_time ? current_first_time : first_time;
                        last_time = current_last_time > last_time ? current_last_time : last_time;
                    } catch (Exception e) {}
                    pd.setProgress(i+1);
                }

                no_data = true;

                for(int i = 0; i < sensors.size(); i++) {
                    ArrayList<DataRecord> current_records = records.get(i);
                    //ggf. Fehlerkorrektur(en) durchführen
                    if(su.getBoolean("enable_auto_correction", true)) {
                        current_records = Tools.measurementCorrection1(current_records);
                        current_records = Tools.measurementCorrection2(current_records);
                    }
                    if(current_records.size() > 0) {
                        no_data = false;
                        try{
                            final LineGraphSeries<DataPoint> series_p1 = new LineGraphSeries<>();
                            series_p1.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_p1.appendData(new DataPoint(time.getTime(), record.getP1()), false, 1000000);
                                } catch (Exception e) {}
                            }

                            final LineGraphSeries<DataPoint> series_p2 = new LineGraphSeries<>();
                            series_p2.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_p2.appendData(new DataPoint(time.getTime(), record.getP2()), false, 1000000);
                                } catch (Exception e) {}
                            }

                            final LineGraphSeries<DataPoint> series_temp = new LineGraphSeries<>();
                            series_temp.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_temp.appendData(new DataPoint(time.getTime(), record.getTemp()), false, 1000000);
                                } catch (Exception e) {}
                            }

                            final LineGraphSeries<DataPoint> series_humidity = new LineGraphSeries<>();
                            series_humidity.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_humidity.appendData(new DataPoint(time.getTime(), record.getHumidity()), false, 1000000);
                                } catch (Exception e) {}
                            }

                            final LineGraphSeries<DataPoint> series_pressure = new LineGraphSeries<>();
                            series_pressure.setColor(sensors.get(i).getColor());
                            for(DataRecord record : Tools.fitArrayList(su, current_records)) {
                                Date time = record.getDateTime();
                                try{
                                    series_pressure.appendData(new DataPoint(time.getTime(), record.getPressure()), false, 1000000);
                                } catch (Exception e) {}
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    diagram_p1.addSeries(series_p1);
                                    diagram_p2.addSeries(series_p2);
                                    diagram_temp.addSeries(series_temp);
                                    diagram_humidity.addSeries(series_humidity);
                                    diagram_pressure.addSeries(series_pressure);
                                }
                            });
                        } catch (Exception e) {}
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            diagram_p1.getViewport().setScalable(true);
                            diagram_p1.getViewport().setMinX(first_time);
                            diagram_p1.getViewport().setMaxX(last_time);
                            diagram_p1.getViewport().scrollToEnd();
                            diagram_p1.getViewport().setScalable(false);

                            diagram_p2.getViewport().setScalable(true);
                            diagram_p2.getViewport().setMinX(first_time);
                            diagram_p2.getViewport().setMaxX(last_time);
                            diagram_p2.getViewport().scrollToEnd();
                            diagram_p2.getViewport().setScalable(false);

                            diagram_temp.getViewport().setScalable(true);
                            diagram_temp.getViewport().setMinX(first_time);
                            diagram_temp.getViewport().setMaxX(last_time);
                            diagram_temp.getViewport().scrollToEnd();
                            diagram_temp.getViewport().setScalable(false);

                            diagram_humidity.getViewport().setScalable(true);
                            diagram_humidity.getViewport().setMinX(first_time);
                            diagram_humidity.getViewport().setMaxX(last_time);
                            diagram_humidity.getViewport().scrollToEnd();
                            diagram_humidity.getViewport().setScalable(false);

                            diagram_pressure.getViewport().setScalable(true);
                            diagram_pressure.getViewport().setMinX(first_time);
                            diagram_pressure.getViewport().setMaxX(last_time);
                            diagram_pressure.getViewport().scrollToEnd();
                            diagram_pressure.getViewport().setScalable(false);

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

    private void exportData() {
        if(ContextCompat.checkSelfPermission(CompareActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            if(export_option == 1) {
                diagram_p1.takeSnapshotAndShare(CompareActivity.this, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram));
            } else if(export_option == 2) {
                diagram_p2.takeSnapshotAndShare(CompareActivity.this, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram));
            } else if(export_option == 3) {
                diagram_temp.takeSnapshotAndShare(CompareActivity.this, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram));
            } else if(export_option == 4) {
                diagram_humidity.takeSnapshotAndShare(CompareActivity.this, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram));
            } else if(export_option == 5) {
                diagram_pressure.takeSnapshotAndShare(CompareActivity.this, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram));
            }
        } else {
            ActivityCompat.requestPermissions(CompareActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_EXTERNAL_STORAGE);
        }
    }
}