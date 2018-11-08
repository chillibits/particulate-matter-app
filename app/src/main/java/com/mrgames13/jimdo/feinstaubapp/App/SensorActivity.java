package com.mrgames13.jimdo.feinstaubapp.App;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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

import com.google.android.material.tabs.TabLayout;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters.ViewPagerAdapterSensor;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

public class SensorActivity extends AppCompatActivity {

    //Konstanten
    public static final int SORT_MODE_TIME_ASC = 101;
    public static final int SORT_MODE_TIME_DESC = 102;
    public static final int SORT_MODE_VALUE1_ASC = 103;
    public static final int SORT_MODE_VALUE1_DESC = 104;
    public static final int SORT_MODE_VALUE2_ASC = 105;
    public static final int SORT_MODE_VALUE2_DESC = 106;
    public static final int SORT_MODE_TEMP_ASC = 107;
    public static final int SORT_MODE_TEMP_DESC = 108;
    public static final int SORT_MODE_HUMIDITY_ASC = 109;
    public static final int SORT_MODE_HUMIDITY_DESC = 110;
    public static final int SORT_MODE_PRESSURE_ASC = 111;
    public static final int SORT_MODE_PRESSURE_DESC = 112;
    private final int REQ_WRITE_EXTERNAL_STORAGE = 1;

    //Variablen als Objekte
    private Resources res;
    private Toolbar toolbar;
    private TabLayout tab_layout;
    private ViewPager view_pager;
    private ViewPagerAdapterSensor view_pager_adapter;
    private DatePickerDialog date_picker_dialog;
    private Calendar calendar;
    private MenuItem progress_menu_item;
    private ScheduledExecutorService service;
    public static ArrayList<DataRecord> records = new ArrayList<>();
    private Sensor sensor;
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");

    //Utils-Pakete
    private ServerMessagingUtils smu;
    private StorageUtils su;

    //Variablen
    public static String current_date_string;
    public static String date_string;
    public static int sort_mode = SORT_MODE_TIME_ASC; // Vorsicht!! Nach dem Verstellen funktioniert der ViewPagerAdapterSensor nicht mehr richtig
    public static boolean custom_p1 = true;
    public static boolean custom_p2 = true;
    public static boolean custom_temp = false;
    public static boolean custom_humidity = false;
    public static boolean custom_pressure = false;
    public static double curve_smoothness = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(SensorActivity.this, su);

        //ViewPager initialisieren
        view_pager = findViewById(R.id.view_pager);
        view_pager_adapter = new ViewPagerAdapterSensor(getSupportFragmentManager(), SensorActivity.this, su);
        view_pager.setAdapter(view_pager_adapter);

        //TabLayout aufsetzen
        tab_layout = findViewById(R.id.tablayout);
        tab_layout.setTabGravity(TabLayout.GRAVITY_FILL);
        tab_layout.setupWithViewPager(view_pager);
        tab_layout.setBackgroundColor(res.getColor(R.color.colorPrimary));
        tab_layout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                view_pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        //Kalender initialisieren
        calendar = Calendar.getInstance();
        current_date_string = sdf_date.format(calendar.getTime());
        date_string = current_date_string;

        //CardView-Komponenten initialisieren
        final TextView card_date_value = findViewById(R.id.card_date_value);
        ImageView card_date_edit = findViewById(R.id.card_date_edit);
        ImageView card_date_today = findViewById(R.id.card_date_today);
        ImageView card_date_back = findViewById(R.id.card_date_back);
        final ImageView card_date_next = findViewById(R.id.card_date_next);

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
        card_date_today.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Datum auf den heutigen Tag setzen
                calendar.setTime(new Date());

                date_string = sdf_date.format(calendar.getTime());
                card_date_value.setText(date_string);

                card_date_next.setEnabled(false);

                //Daten für ausgewähltes Datum laden
                loadData(true);
            }
        });
        card_date_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Einen Tag zurück gehen
                calendar.add(Calendar.DATE, -1);

                date_string = sdf_date.format(calendar.getTime());
                card_date_value.setText(date_string);

                Calendar current_calendar = Calendar.getInstance();
                current_calendar.set(Calendar.HOUR_OF_DAY, 0);
                current_calendar.set(Calendar.MINUTE, 0);
                current_calendar.set(Calendar.SECOND, 0);
                current_calendar.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar.before(current_calendar));

                //Daten für ausgewähltes Datum laden
                loadData(true);
            }
        });
        card_date_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Einen Tag vor gehen
                calendar.add(Calendar.DATE, 1);

                date_string = sdf_date.format(calendar.getTime());
                card_date_value.setText(date_string);

                Calendar current_calendar = Calendar.getInstance();
                current_calendar.set(Calendar.HOUR_OF_DAY, 0);
                current_calendar.set(Calendar.MINUTE, 0);
                current_calendar.set(Calendar.SECOND, 0);
                current_calendar.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar.before(current_calendar));

                //Daten für ausgewähltes Datum laden
                loadData(true);
            }
        });
        card_date_next.setEnabled(false);

        //Intent-Extras auslesen
        sensor = new Sensor();
        if(getIntent().hasExtra("Name")) {
            sensor.setName(getIntent().getStringExtra("Name"));
            getSupportActionBar().setTitle(getIntent().getStringExtra("Name"));
        }
        if(getIntent().hasExtra("ID")) sensor.setId(getIntent().getStringExtra("ID"));
        if(getIntent().hasExtra("Color")) sensor.setColor(getIntent().getIntExtra("Color", res.getColor(R.color.colorPrimary)));


        //RefreshPeriod setzen
        int period = Integer.parseInt(su.getString("sync_cycle", String.valueOf(Constants.DEFAULT_SYNC_CYCLE)));

        //ScheduledExecutorService aufsetzen
        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(date_string.equals(current_date_string)) {
                    Log.i("FA", "Auto refreshing ...");
                    loadData(false);
                }
            }
        }, period, period, TimeUnit.SECONDS);

        if(!sensor.getId().equals("no_id")) loadData(true);

        //Check if sensor is existing on the server
        checkSensorAvailability();
    }

    private void chooseDate(final TextView card_date_value) {
        //Datum auswählen
        date_picker_dialog = new DatePickerDialog(SensorActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);

                date_string = sdf_date.format(calendar.getTime());
                card_date_value.setText(date_string);

                //Daten für ausgewähltes Datum laden
                loadData(true);
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
        } else if(id == R.id.action_export) {
            if(records.size() > 0) {
                exportData();
            } else {
                Toast.makeText(this, R.string.no_data_date, Toast.LENGTH_SHORT).show();
            }
        } else if(id == R.id.action_refresh) {
            //Daten neu laden
            Log.i("FA", "User refreshing ...");
            loadData(true);
        } else if(id == R.id.action_settings) {
            //SettingsActivity starten
            startActivity(new Intent(SensorActivity.this, SettingsActivity.class));
        } else if(id == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(service != null) service.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ_WRITE_EXTERNAL_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) exportData();
    }

    public static void resortData() {
        try{ Collections.sort(records); } catch (Exception e) {}
    }

    //-----------------------------------Private Methoden-------------------------------------------

    private void loadData(final boolean from_user) {
        //ProgressMenuItem setzen
        if(progress_menu_item != null) progress_menu_item.setActionView(R.layout.menu_item_loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //Datensätze leeren
                records.clear();

                //Date String von Gestern ermitteln
                String date_yesterday = date_string;
                try{
                    Calendar c = Calendar.getInstance();
                    c.setTime(sdf_date.parse(date_yesterday));
                    c.add(Calendar.DATE, -1);
                    date_yesterday = sdf_date.format(c.getTime());
                } catch (Exception e) {}

                //Prüfen, ob Intenet verfügbar ist
                if((!from_user && smu.isInternetAvailable()) || (from_user && smu.checkConnection(findViewById(R.id.container)))) {
                    //Internet ist verfügbar
                    smu.manageDownloads(sensor, date_string, date_yesterday);
                }
                //Kein Internet
                if(su.isCSVFileExisting(date_string, sensor.getId()) || su.isCSVFileExisting(date_yesterday, sensor.getId())) {
                    Log.d("FA", "Local CSV Files existing");
                    //Inhalt der lokalen Dateien auslesen
                    String csv_string_day = su.getCSVFromFile(date_string, sensor.getId());
                    String csv_string_day_before = su.getCSVFromFile(date_yesterday, sensor.getId());
                    //CSV-Strings zu Objekten machen
                    records = su.getDataRecordsFromCSV(csv_string_day_before);
                    records.addAll(su.getDataRecordsFromCSV(csv_string_day));
                    //Datensätze zuschneiden
                    ViewPagerAdapterSensor.records = records = su.trimDataRecords(records, date_string);
                    //Sortieren
                    resortData();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //ViewpagerAdapter refreshen
                        view_pager_adapter.refreshFragments();
                        //ProgressMenuItem zurücksetzen
                        if(progress_menu_item != null) progress_menu_item.setActionView(null);
                    }
                });
            }
        }).start();
    }

    private void checkSensorAvailability() {
        if(!su.getBoolean("DontShowAgain_" + sensor.getId()) && smu.isInternetAvailable()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = smu.sendRequest(findViewById(R.id.container), "command=issensordataexisting&chip_id=" + URLEncoder.encode(sensor.getId()));
                    if(!Boolean.parseBoolean(result)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog d = new AlertDialog.Builder(SensorActivity.this)
                                        .setCancelable(true)
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.add_sensor_tick_not_set_message)
                                        .setPositiveButton(R.string.ok, null)
                                        .setNegativeButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                su.putBoolean("DontShowAgain_" + sensor.getId(), true);
                                            }
                                        })
                                        .create();
                                d.show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void exportData() {
        if(ContextCompat.checkSelfPermission(SensorActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            View v = getLayoutInflater().inflate(R.layout.dialog_export, null);
            final RadioButton export_diagram = v.findViewById(R.id.export_diagram);
            AlertDialog d = new AlertDialog.Builder(this)
                    .setTitle(R.string.export_data)
                    .setView(v)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(export_diagram.isChecked()) {
                                //Diagramm exportieren
                                view_pager_adapter.exportDiagram(SensorActivity.this);
                            } else {
                                //Datensätze exportieren
                                if(su.isCSVFileExisting(date_string, sensor.getId())) su.shareCSVFile(date_string, sensor.getId());
                            }
                        }
                    })
                    .create();
            d.show();
        } else {
            Log.d("FA", "Test");
            ActivityCompat.requestPermissions(SensorActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_EXTERNAL_STORAGE);
        }
    }
}