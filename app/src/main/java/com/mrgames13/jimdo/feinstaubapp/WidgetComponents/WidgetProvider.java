/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.WidgetComponents;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import java.text.SimpleDateFormat;

public class WidgetProvider extends AppWidgetProvider {

    // Utils packages
    private StorageUtils su;

    // Variables as objects
    private Resources res;
    private SimpleDateFormat sdf_datetime = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] app_widget_id) {
        super.onUpdate(context, appWidgetManager, app_widget_id);
        initialize(context);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        for(int widget_id : app_widget_id) {
            // Refresh button
            Intent refresh = new Intent(context, getClass());
            refresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refresh.putExtra(Constants.INSTANCE.getWIDGET_EXTRA_WIDGET_ID(), widget_id);
            PendingIntent refresh_pi = PendingIntent.getBroadcast(context, 0, refresh, 0);
            rv.setOnClickPendingIntent(R.id.widget_refresh, refresh_pi);
            // Update data
            updateData(context, rv, widget_id);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        initialize(context);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) && intent.hasExtra(Constants.INSTANCE.getWIDGET_EXTRA_SENSOR_ID())) {
            // Get WidgetID
            int widget_id = su.getInt("Widget_" + intent.getStringExtra(Constants.INSTANCE.getWIDGET_EXTRA_SENSOR_ID()), AppWidgetManager.INVALID_APPWIDGET_ID);
            if(widget_id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                rv.setViewVisibility(R.id.widget_refreshing, View.GONE);
                rv.setViewVisibility(R.id.widget_refresh, View.VISIBLE);

                initializeComponents(context, rv, widget_id);

                updateData(context, rv, widget_id);
            }
        } else if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) && intent.hasExtra(Constants.INSTANCE.getWIDGET_EXTRA_WIDGET_ID())) {
            int widget_id = intent.getIntExtra(Constants.INSTANCE.getWIDGET_EXTRA_WIDGET_ID(), AppWidgetManager.INVALID_APPWIDGET_ID);
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
        // Refresh button
        Intent refresh = new Intent(context, getClass());
        refresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refresh.putExtra(Constants.INSTANCE.getWIDGET_EXTRA_WIDGET_ID(), widget_id);
        PendingIntent refresh_pi = PendingIntent.getBroadcast(context, 0, refresh, 0);
        rv.setOnClickPendingIntent(R.id.widget_refresh, refresh_pi);
    }

    private void updateData(Context context, RemoteViews rv, int widget_id) {
        try {
            // Load sensors
            Sensor sensor = su.getSensor(su.getString("Widget_" + widget_id));
            // Get last record from the db
            DataRecord last_record = su.getLastRecord(sensor.getChipID());
            if (last_record != null) {
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
