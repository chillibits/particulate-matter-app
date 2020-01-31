/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import com.chillibits.pmapp.service.SyncJobService
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.tool.Tools
import com.mrgames13.jimdo.feinstaubapp.R

class WidgetProviderSmall : AppWidgetProvider() {

    // Utils packages
    private lateinit var su: StorageUtils

    // Variables as objects

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, app_widget_id: IntArray) {
        super.onUpdate(context, appWidgetManager, app_widget_id)
        initialize(context)

        val rv = RemoteViews(context.packageName, R.layout.widget_large)

        for (widget_id in app_widget_id) {
            // Refresh button
            val refresh = Intent(context, javaClass)
            refresh.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            refresh.putExtra(Constants.WIDGET_EXTRA_SMALL_WIDGET_ID, widget_id)
            val refreshPi = PendingIntent.getBroadcast(context, 0, refresh, 0)
            rv.setOnClickPendingIntent(R.id.widget_refresh, refreshPi)
            // Update data
            updateData(context, rv, widget_id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        initialize(context)

        val rv = RemoteViews(context.packageName, R.layout.widget_small)

        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && intent.hasExtra(Constants.WIDGET_EXTRA_SENSOR_ID)) {
            // Get WidgetID
            val widgetId = su.getInt("Widget_" + intent.getStringExtra(Constants.WIDGET_EXTRA_SENSOR_ID)!!, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) updateData(context, rv, widgetId)
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && intent.hasExtra(Constants.WIDGET_EXTRA_WIDGET_ID)) {
            val widgetId = intent.getIntExtra(Constants.WIDGET_EXTRA_SMALL_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            update(context, rv, widgetId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, SyncJobService::class.java))
            } else {
                context.startService(Intent(context, SyncJobService::class.java))
            }
        }
    }

    private fun initialize(context: Context) {
        su = StorageUtils(context)
    }

    private fun updateData(context: Context, rv: RemoteViews, widget_id: Int) {
        try {
            // Load sensors
            val sensor = su.getSensor(su.getString("Widget_$widget_id"))
            // Get last record from the db
            val lastRecord = su.getLastRecord(sensor!!.chipID)
            if (lastRecord != null) {
                rv.run {
                    setTextViewText(R.id.title, context.getString(R.string.current_values) + " - " + sensor.name)
                    setTextViewText(R.id.p1, Tools.round(lastRecord.p1, 1).toString() + " µg/m³")
                    setTextViewText(R.id.p2, Tools.round(lastRecord.p2, 1).toString() + " µg/m³")
                    setViewVisibility(R.id.no_data, View.GONE)
                }
            } else {
                rv.setViewVisibility(R.id.no_data, View.VISIBLE)
            }
            update(context, rv, widget_id)
        } catch (ignored: Exception) {}
    }

    private fun update(context: Context, rv: RemoteViews, widget_id: Int) {
        AppWidgetManager.getInstance(context).updateAppWidget(widget_id, rv)
    }
}