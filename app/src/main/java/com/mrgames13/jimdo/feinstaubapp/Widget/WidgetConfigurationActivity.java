package com.mrgames13.jimdo.feinstaubapp.Widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;

import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SelectSensorAdapter;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.util.ArrayList;
import java.util.Collections;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class WidgetConfigurationActivity extends AppCompatActivity {

    //Konstanten

    //Utils-Pakete
    private StorageUtils su;

    //Variablen als Objekte
    private Toolbar toolbar;
    private RecyclerView sensor_view;
    private SelectSensorAdapter sensor_view_adapter;
    private ArrayList<Sensor> sensors;

    //Variablen
    private int app_widget_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_configuration);

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //AppWidgetID laden
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if(extras != null) {
            app_widget_id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        //Sensoren laden
        su = new StorageUtils(this);
        sensors = su.getAllFavourites();
        sensors.addAll(su.getAllOwnSensors());
        Collections.sort(sensors);
        if(sensors.size() > 0) {
            //RecyclerView initialisieren
            sensor_view = findViewById(R.id.sensors);
            sensor_view_adapter = new SelectSensorAdapter(this, su, sensors);
            sensor_view.setItemViewCacheSize(100);
            sensor_view.setLayoutManager(new LinearLayoutManager(this));
            sensor_view.setAdapter(sensor_view_adapter);
        } else {
            findViewById(R.id.no_data).setVisibility(View.VISIBLE);
            Button btn_add_favourite = findViewById(R.id.add_sensor);
            btn_add_favourite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(WidgetConfigurationActivity.this, MainActivity.class));
                    finish();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_select_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_done) {
            finishConfiguration();
        } else if(id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void finishConfiguration() {
        if(sensor_view_adapter.getSelectedSensor() != null) {
            startService(new Intent(this, SyncService.class));
            su.putInt("Widget_" + sensor_view_adapter.getSelectedSensor().getId(), app_widget_id);
            su.putString("Widget_" + String.valueOf(app_widget_id), sensor_view_adapter.getSelectedSensor().getId());

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
            appWidgetManager.updateAppWidget(app_widget_id, views);

            Intent update_intent = new Intent(getApplicationContext(), WidgetProvider.class);
            update_intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            update_intent.putExtra(Constants.WIDGET_EXTRA_SENSOR_ID, sensor_view_adapter.getSelectedSensor().getId());
            sendBroadcast(update_intent);

            Intent resultValue = new Intent();
            resultValue.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, app_widget_id);
            setResult(RESULT_OK, resultValue);
        }
        finish();
    }
}
