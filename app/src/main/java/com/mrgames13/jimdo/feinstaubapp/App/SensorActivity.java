package com.mrgames13.jimdo.feinstaubapp.App;

import android.animation.LayoutTransition;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.ColorUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters.ViewPagerAdapterSensor;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorActivity extends AppCompatActivity {

    //Konstanten
    private final int REFRESH_PERIOD = 0; // 0 für den Wert der über die Einstellugen eingestellt wurde
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

    //Utils-Pakete
    private ServerMessagingUtils smu;
    private StorageUtils su;

    //Variablen
    private String current_date_string;
    private String date_string;
    public static int sort_mode = SORT_MODE_TIME_ASC; // Vorsicht! Bei verstellen arbeitet der ViewPagerAdapterSensor nicht mehr richtig
    public static boolean custom_sdsp1 = true;
    public static boolean custom_sdsp2 = true;
    public static boolean custom_temp = false;
    public static boolean custom_humidity = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        toolbar.setLayoutTransition(new LayoutTransition());
        setSupportActionBar(toolbar);

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(SensorActivity.this, su);

        //ViewPager initialisieren
        view_pager = findViewById(R.id.view_pager);
        view_pager_adapter = new ViewPagerAdapterSensor(getSupportFragmentManager(), SensorActivity.this);
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
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        current_date_string = sdf.format(calendar.getTime());
        date_string = current_date_string;

        //CardView-Komponenten initialisieren
        CardView card_date = findViewById(R.id.card_date);
        final TextView card_date_value = findViewById(R.id.card_date_value);
        ImageView card_date_edit = findViewById(R.id.card_date_edit);

        card_date_value.setText(date_string);
        card_date_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Datum auswählen
                date_picker_dialog = new DatePickerDialog(SensorActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);

                        date_string = sdf.format(calendar.getTime());
                        card_date_value.setText(date_string);

                        //Daten für ausgewähltes Datum laden
                        loadData(true);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                date_picker_dialog.show();
            }
        });

        //Intent-Extras auslesen
        sensor = new Sensor();
        if(getIntent().hasExtra("Name")) {
            sensor.setName(getIntent().getStringExtra("Name"));
            getSupportActionBar().setTitle(getIntent().getStringExtra("Name"));
        }
        if(getIntent().hasExtra("ID")) sensor.setId(getIntent().getStringExtra("ID"));
        if(getIntent().hasExtra("Color")) sensor.setColor(getIntent().getIntExtra("Color", res.getColor(R.color.colorPrimary)));
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
            loadData(true);
        } else if(id == R.id.action_settings) {
            startActivity(new Intent(SensorActivity.this, SettingsActivity.class));
        } else if(id == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Toolbar Text und Farbe setzen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(Build.VERSION.SDK_INT >= 21) getWindow().setStatusBarColor(ColorUtils.darkenColor(res.getColor(R.color.colorPrimary)));

        //RefreshPeriod setzen
        int period = REFRESH_PERIOD;
        if(period == 0) period = 1000 * Integer.parseInt(su.getString("sync_cycle", "10"));

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
        }, period, period, TimeUnit.MILLISECONDS);

        if(!sensor.getId().equals("no_id")) loadData(true);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(service != null) service.shutdown();
    }

    //-----------------------------------Private Methoden-------------------------------------------

    private void loadData(final boolean from_user) {
        //ProgressMenuItem setzen
        if(progress_menu_item != null) progress_menu_item.setActionView(R.layout.menu_item_loading);

        if((!from_user && smu.isInternetAvailable()) || (from_user && smu.checkConnection(findViewById(R.id.container)))) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Daten analysieren
                    records = new ArrayList<>();
                    if(smu.downloadCSVFile(date_string, sensor.getId())) {
                        //Inhalt der Datei auslesen
                        String csv_string = su.getCSVFromFile(date_string, sensor.getId());
                        records = getDataRecordsFromCSV(csv_string);
                        view_pager_adapter.records = records;
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
        } else {
            try{
                //Datum umformatieren
                SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
                Date newDate = format.parse(date_string);
                format = new SimpleDateFormat("yyyy-MM-dd");

                //Datei ermitteln
                String file_name = format.format(newDate) + ".csv";
                File dir = new File(getFilesDir(), "/SensorData");
                File file = new File(dir, sensor.getId() + file_name);

                if(su.isFileExisting(file.getAbsolutePath())) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Inhalt der Datei auslesen
                            String csv_string = su.getCSVFromFile(date_string, sensor.getId());
                            records = getDataRecordsFromCSV(csv_string);
                            view_pager_adapter.records = records;
                            resortData();

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
            } catch (Exception e) {}
        }
    }

    private ArrayList<DataRecord> getDataRecordsFromCSV(String csv_string) {
        try{
            ArrayList<DataRecord> records = new ArrayList<>();
            //In Zeilen aufspalten
            String[] lines = csv_string.split("\\r?\\n");
            for(int i = 1; i < lines.length; i++) {
                String time = "00:00";
                Double sdsp1 = 0.0;
                Double sdsp2 = 0.0;
                Double temp = 0.0;
                Double humidity = 0.0;
                //Zeile aufspalten
                String[] line_contents = lines[i].split(";");
                if(!line_contents[0].equals("")) time = line_contents[0].substring(line_contents[0].indexOf(" ") +1);
                if(!line_contents[7].equals("")) sdsp1 = Double.parseDouble(line_contents[7]);
                if(!line_contents[8].equals("")) sdsp2 = Double.parseDouble(line_contents[8]);
                if(!line_contents[9].equals("")) temp = Double.parseDouble(line_contents[9]);
                if(!line_contents[10].equals("")) humidity = Double.parseDouble(line_contents[10]);
                if(!line_contents[11].equals("")) temp = Double.parseDouble(line_contents[11]);
                if(!line_contents[12].equals("")) humidity = Double.parseDouble(line_contents[12]);

                //Unsere Zeitzone einstellen
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
                SimpleDateFormat format_out = new SimpleDateFormat("HH:mm:ss");
                try { time = format_out.format(format.parse(time)); } catch (ParseException e) {}
                records.add(new DataRecord(time, sdsp1, sdsp2, temp, humidity));
            }
            return records;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void resortData() {
        try{ Collections.sort(records); } catch (Exception e) {}
    }
}
