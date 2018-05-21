package com.mrgames13.jimdo.feinstaubapp.App;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SensorAdapter;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    //Konstanten

    //Variablen als Objekte
    private Resources res;
    private Toolbar toolbar;
    private FloatingActionButton add_sensor;
    private RecyclerView sensor_view;
    private SensorAdapter sensor_view_adapter;
    private ArrayList<Sensor> sensors;
    public ArrayList<Sensor> selected_sensors = new ArrayList<>();
    public static MainActivity own_instance;

    //Utils-Pakete
    private StorageUtils su;

    //Variablen
    private boolean pressedOnce;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Eigene Intanz initialisieren
        own_instance = this;

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(res.getString(R.string.app_name));
        setSupportActionBar(toolbar);

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //Komponenten initialisieren
        add_sensor = findViewById(R.id.add_sensor);
        add_sensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddSensorActivity.class));
            }
        });

        sensor_view = findViewById(R.id.sensor_view);
        sensor_view.setLayoutManager(new LinearLayoutManager(this));

        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();

        int background_sync_frequency = Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) * 1000 * 60;

        //Alarmmanager aufsetzen
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent start_service_intent = new Intent(this, SyncService.class);
        PendingIntent start_service_pending_intent = PendingIntent.getService(this, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, start_service_intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), background_sync_frequency, start_service_pending_intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(selected_sensors.size() == 0) {
            getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        } else if(selected_sensors.size() == 1) {
            getMenuInflater().inflate(R.menu.menu_activity_main_one, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_activity_main_compare, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if(id == R.id.action_add) {
            startActivity(new Intent(MainActivity.this, AddSensorActivity.class));
        } else if(id == R.id.action_details) {
            Sensor sensor = selected_sensors.get(0);
            Intent i = new Intent(this, SensorActivity.class);
            i.putExtra("Name", sensor.getName());
            i.putExtra("ID", sensor.getId());
            i.putExtra("Color", sensor.getColor());
            startActivity(i);
        } else if(id == R.id.action_edit) {
            Sensor sensor = selected_sensors.get(0);
            Intent i = new Intent(this, AddSensorActivity.class);
            i.putExtra("Mode", AddSensorActivity.MODE_EDIT);
            i.putExtra("Name", sensor.getName());
            i.putExtra("ID", sensor.getId());
            i.putExtra("Color", sensor.getColor());
            startActivity(i);
        } else if(id == R.id.action_delete) {
            AlertDialog d = new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.delete_sensor)
                    .setMessage(R.string.really_delete_sensor)
                    .setNegativeButton(R.string.cancel,null)
                    .setPositiveButton(R.string.delete_sensor, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Sensor aus der Datenbank löschen
                            su.deleteSensor(selected_sensors.get(0).getId());
                            refresh();
                        }
                    })
                    .create();
            d.show();
        } else if(id == R.id.action_compare) {
            startActivity(new Intent(MainActivity.this, CompareActivity.class));
        } else if(id == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void refresh() {
        selected_sensors.clear();
        invalidateOptionsMenu();
        sensors = su.getAllSensors();
        sensor_view_adapter = new SensorAdapter(this, sensors);
        sensor_view.setAdapter(sensor_view_adapter);
        findViewById(R.id.no_data).setVisibility(sensors.size() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!pressedOnce) {
                pressedOnce = true;
                Toast.makeText(MainActivity.this, R.string.tap_again_to_exit_app, Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pressedOnce = false;
                    }
                }, 2500);
            } else {
                pressedOnce = false;
                onBackPressed();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void updateToolbar(ArrayList<Sensor> selected_sensors) {
        this.selected_sensors = selected_sensors;
        invalidateOptionsMenu();
    }
}