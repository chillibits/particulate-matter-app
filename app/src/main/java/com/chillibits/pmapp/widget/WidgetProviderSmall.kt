/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
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
import com.chillibits.pmapp.service.SyncService
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.tool.Tools
import com.mrgames13.jimdo.feinstaubapp.R

class WidgetProviderSmall : AppWidgetProvider() {

    // Utils packages
    private lateinit var su: StorageUtils

    // Variables as objects
    private lateinit var context: Context

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, app_widget_id: IntArray) {
        super.onUpdate(context, appWidgetManager, app_widget_id)
        initialize(context)

        val rv = RemoteViews(context.packageName, R.layout.widget_small)

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

        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && intent.hasExtra(Constants.WIDGET_SMALL_EXTRA_SENSOR_ID)) {
            // Get WidgetID
            val widgetId = su.getInt("Widget_Small_" + intent.getStringExtra(Constants.WIDGET_SMALL_EXTRA_SENSOR_ID)!!, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) updateData(context, rv, widgetId)
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && intent.hasExtra(Constants.WIDGET_EXTRA_LARGE_WIDGET_ID)) {
            val widgetId = intent.getIntExtra(Constants.WIDGET_EXTRA_SMALL_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            AppWidgetManager.getInstance(context).updateAppWidget(widgetId, rv)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, SyncService::class.java))
            } else {
                context.startService(Intent(context, SyncService::class.java))
            }
        }
    }

    private fun initialize(context: Context) {
        this.context = context
        su = StorageUtils(context)
    }

    private fun updateData(context: Context, rv: RemoteViews, widgetId: Int) {
        try {
            // Load sensors
            val sensor = su.getSensor(su.getString("Widget_Small_$widgetId"))
            // Get last record from the db
            val lastRecord = su.getLastRecord(sensor!!.chipID)
            if (lastRecord != null) {
                rv.run {
                    setTextViewText(R.id.name, sensor.name)
                    setTextViewText(R.id.p1, context.getString(R.string.value1) + ": " + Tools.round(lastRecord.p1, 1).toString() + " µg/m³")
                    setTextViewText(R.id.p2, context.getString(R.string.value2) + ": " + Tools.round(lastRecord.p2, 1).toString() + " µg/m³")
                    setProgressBar(R.id.progress, 40, lastRecord.p1.toInt(), false)
                    setViewVisibility(R.id.data_container, View.VISIBLE)
                    setViewVisibility(R.id.no_data, View.GONE)
                }
            } else {
                rv.setViewVisibility(R.id.data_container, View.GONE)
                rv.setViewVisibility(R.id.no_data, View.VISIBLE)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(widgetId, rv)
        } catch (ignored: Exception) {}
    }
}