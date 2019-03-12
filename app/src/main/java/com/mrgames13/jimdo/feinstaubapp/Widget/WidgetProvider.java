package com.mrgames13.jimdo.feinstaubapp.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class WidgetProvider extends AppWidgetProvider {
    //Konstanten

    //Utils-Pakete
    private StorageUtils su;

    //Variablen als Objekte
    private Resources res;
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");
    private SimpleDateFormat sdf_datetime = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    //Variablen

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] app_widget_id) {
        super.onUpdate(context, appWidgetManager, app_widget_id);
        initialize(context);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        //App öffnen
        Intent open_app = new Intent(context, MainActivity.class);
        PendingIntent open_app_pi = PendingIntent.getActivity(context, 0, open_app, 0);
        rv.setOnClickPendingIntent(R.id.open_app, open_app_pi);

        for(int widget_id : app_widget_id) {
            //Refresh-Button
            Intent refresh = new Intent(context, getClass());
            refresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refresh.putExtra(Constants.WIDGET_EXTRA_WIDGET_ID, widget_id);
            PendingIntent refresh_pi = PendingIntent.getBroadcast(context, 0, refresh, 0);
            rv.setOnClickPendingIntent(R.id.widget_refresh, refresh_pi);
            //Daten updaten
            updateData(context, rv, widget_id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        initialize(context);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) && intent.hasExtra(Constants.WIDGET_EXTRA_SENSOR_ID)) {
            //WidgetID herausfinden
            int widget_id = su.getInt("Widget_" + intent.getStringExtra(Constants.WIDGET_EXTRA_SENSOR_ID), AppWidgetManager.INVALID_APPWIDGET_ID);
            if(widget_id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                rv.setViewVisibility(R.id.widget_refreshing, View.GONE);
                rv.setViewVisibility(R.id.widget_refresh, View.VISIBLE);

                initializeComponents(context, rv, widget_id);

                updateData(context, rv, widget_id);
            }
        } else if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) && intent.hasExtra(Constants.WIDGET_EXTRA_WIDGET_ID)) {
            int widget_id = intent.getIntExtra(Constants.WIDGET_EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            rv.setViewVisibility(R.id.widget_refreshing, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_refresh, View.INVISIBLE);

            initializeComponents(context, rv, widget_id);

            update(context, rv, widget_id);
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, SyncService.class));
            } else {
                context.startService(new Intent(context, SyncService.class));
            }
        }
    }

    private void initialize(Context context) {
        su = new StorageUtils(context);
        res = context.getResources();
    }

    private void initializeComponents(Context context, RemoteViews rv, int widget_id) {
        //App öffnen
        Intent open_app = new Intent(context, MainActivity.class);
        PendingIntent open_app_pi = PendingIntent.getActivity(context, 0, open_app, 0);
        rv.setOnClickPendingIntent(R.id.open_app, open_app_pi);

        //Refresh-Button
        Intent refresh = new Intent(context, getClass());
        refresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refresh.putExtra(Constants.WIDGET_EXTRA_WIDGET_ID, widget_id);
        PendingIntent refresh_pi = PendingIntent.getBroadcast(context, 0, refresh, 0);
        rv.setOnClickPendingIntent(R.id.widget_refresh, refresh_pi);
    }

    private void updateData(Context context, RemoteViews rv, int widget_id) {
        try {
            //Sensor laden
            Sensor sensor = su.getSensor(su.getString("Widget_" + widget_id));
            //Date String von Heute ermitteln
            Calendar calendar = Calendar.getInstance();
            String date_string = sdf_date.format(calendar.getTime());
            //Date String von Gestern ermitteln
            Calendar c = Calendar.getInstance();
            c.setTime(sdf_date.parse(date_string));
            c.add(Calendar.DATE, -1);
            String date_yesterday = sdf_date.format(c.getTime());
            //Inhalt der lokalen Dateien auslesen
            String csv_string_day = su.getCSVFromFile(date_string, sensor.getChipID());
            String csv_string_day_before = su.getCSVFromFile(date_yesterday, sensor.getChipID());
            //CSV-Strings zu Objekten machen
            ArrayList<DataRecord> records = su.getDataRecordsFromCSV(csv_string_day_before);
            records.addAll(su.getDataRecordsFromCSV(csv_string_day));
            if (records.size() > 0) {
                //Datensätze zuschneiden
                records = su.trimDataRecords(records, date_string);
                DataRecord last_record = records.get(records.size() -1);

                rv.setTextViewText(R.id.cv_title, res.getString(R.string.current_values) + " - " + sensor.getName());
                rv.setTextViewText(R.id.cv_p1, Tools.round(last_record.getP1(), 2) + " µg/m³");
                rv.setTextViewText(R.id.cv_p2, Tools.round(last_record.getP2(), 2) + " µg/m³");
                rv.setTextViewText(R.id.cv_temp, Tools.round(last_record.getTemp(), 1) + " °C");
                rv.setTextViewText(R.id.cv_humidity, Tools.round(last_record.getHumidity(), 2) + " %");
                rv.setTextViewText(R.id.cv_pressure, Tools.round(last_record.getPressure(), 3) + " hPa");
                rv.setTextViewText(R.id.cv_time, res.getString(R.string.state_of_) + " " + sdf_datetime.format(last_record.getDateTime()));
                rv.setViewVisibility(R.id.no_data, View.GONE);
            } else {
                rv.setViewVisibility(R.id.no_data, View.VISIBLE);
            }
            update(context, rv, widget_id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void update(Context context, RemoteViews rv, int widget_id) {
        AppWidgetManager.getInstance(context).updateAppWidget(widget_id, rv);
    }
}
