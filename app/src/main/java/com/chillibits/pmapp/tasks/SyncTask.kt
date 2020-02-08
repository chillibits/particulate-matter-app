/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.tasks

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import com.chillibits.pmapp.model.DataRecord
import com.chillibits.pmapp.model.Sensor
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.network.loadDataRecords
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.NotificationUtils
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.tool.Tools
import com.chillibits.pmapp.widget.WidgetProviderLarge
import com.chillibits.pmapp.widget.WidgetProviderSmall
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.concurrent.TimeUnit

class SyncTask(val context: Context, val listener: OnTaskCompleteListener, val fromForeground: Boolean) : AsyncTask<Int, Int, Boolean>() {

    // Variables as objects
    private val su = StorageUtils(context)
    private val smu = ServerMessagingUtils(context)
    private val nu = NotificationUtils(context)

    // Variables
    private var limitP1: Int = 0
    private var limitP2: Int = 0
    private var limitTemp: Int = 0
    private var limitHumidity: Int = 0
    private var limitPressure: Int = 0
    private var selectedDayTimestamp: Long = 0
    private var syncSuccess = true

    // Interfaces
    interface OnTaskCompleteListener {
        fun onTaskCompleted(success: Boolean)
    }

    override fun doInBackground(vararg params: Int?): Boolean {
        Log.i(Constants.TAG, "Sync started ...")

        // Initialize calendar
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        selectedDayTimestamp = calendar.time.time

        // Check if internet is available
        if (smu.isInternetAvailable) {
            // Get max limit from SharedPreferences
            limitP1 = su.getString("limit_p1", Constants.DEFAULT_P1_LIMIT.toString()).toInt()
            limitP2 = su.getString("limit_p2", Constants.DEFAULT_P2_LIMIT.toString()).toInt()
            limitTemp = su.getString("limit_temp", Constants.DEFAULT_TEMP_LIMIT.toString()).toInt()
            limitHumidity = su.getString("limit_humidity", Constants.DEFAULT_HUMIDITY_LIMIT.toString()).toInt()
            limitPressure = su.getString("limit_pressure", Constants.DEFAULT_PRESSURE_LIMIT.toString()).toInt()
            runBlocking(Dispatchers.IO) {
                try {
                    // Get timestamps for 'from' and 'to'
                    val from = selectedDayTimestamp
                    val to = selectedDayTimestamp + TimeUnit.DAYS.toMillis(1)

                    val sensors = ArrayList<Sensor>()
                    sensors.addAll(su.allFavourites)
                    sensors.addAll(su.allOwnSensors)
                    for (s in sensors) {
                        // Load existing records from the local database
                        var records = su.loadRecords(s.chipID, from, to)
                        records.sort()
                        // Load records from server
                        val recordsExternal = loadDataRecords(context, s.chipID, if (records.size > 0) records[records.size - 1].dateTime.time + 1000 else from, to)
                        recordsExternal?.let { records.addAll(recordsExternal) }
                        records.sort()

                        if (records.size > 0) {
                            // Detect a breakdown
                            if (su.getBoolean("notification_breakdown", true) && su.isSensorExisting(s.chipID) && Tools.isMeasurementBreakdown(su, records)) {
                                if (recordsExternal != null && recordsExternal.size > 0 && !su.getBoolean("BD_" + s.chipID)) {
                                    nu.displayMissingMeasurementsNotification(s.chipID, s.name)
                                    su.putBoolean("BD_" + s.chipID, true)
                                }
                            } else {
                                nu.cancelNotification(Integer.parseInt(s.chipID) * 10)
                                su.removeKey("BD_" + s.chipID)
                            }
                            // Calculate average values
                            if(su.getBoolean("notification_threshold", true)) {
                                val averageP1 = getP1Average(records)
                                val averageP2 = getP2Average(records)
                                val averageTemp = getTempAverage(records)
                                val averageHumidity = getHumidityAverage(records)
                                val averagePressure = getPressureAverage(records)
                                records = trimDataRecordsToSyncTime(s.chipID, records)
                                // Evaluate
                                for (r in records) {
                                    if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_p1_exceeded") && limitP1 > 0 && (if (su.getBoolean("notification_averages", true)) averageP1 > limitP1 else r.p1 > limitP1) && r.p1 > su.getDouble(selectedDayTimestamp.toString() + "_p1_max")) {
                                        Log.i(Constants.TAG, "P1 limit exceeded")
                                        // P1 notification
                                        nu.displayLimitExceededNotification(s.name + " - " + context.getString(
                                            R.string.limit_exceeded_p1), s.chipID, r.dateTime.time)
                                        su.putDouble(selectedDayTimestamp.toString() + "_p1_max", r.p1)
                                        su.putBoolean(selectedDayTimestamp.toString() + "_p1_exceeded", true)
                                        break
                                    } else if (limitP1 > 0 && r.p1 < limitP1) {
                                        su.putBoolean(selectedDayTimestamp.toString() + "_p1_exceeded", false)
                                    }
                                    if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_p2_exceeded") && limitP2 > 0 && (if (su.getBoolean("notification_averages", true)) averageP2 > limitP2 else r.p2 > limitP2) && r.p2 > su.getDouble(selectedDayTimestamp.toString() + "_p2_max")) {
                                        Log.i(Constants.TAG, "P2 limit exceeded")
                                        // P2 notification
                                        nu.displayLimitExceededNotification(s.name + " - " + context.getString(
                                            R.string.limit_exceeded_p2), s.chipID, r.dateTime.time)
                                        su.putDouble(selectedDayTimestamp.toString() + "_p2_max", r.p2)
                                        su.putBoolean(selectedDayTimestamp.toString() + "_p2_exceeded", true)
                                        break
                                    } else if (limitP1 > 0 && r.p1 < limitP1) {
                                        su.putBoolean(selectedDayTimestamp.toString() + "_p2_exceeded", false)
                                    }
                                    if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_temp_exceeded") && limitTemp > 0 && (if (su.getBoolean("notification_averages", true)) averageTemp > limitTemp else r.temp > limitTemp) && r.temp > su.getDouble(selectedDayTimestamp.toString() + "_temp_max")) {
                                        Log.i(Constants.TAG, "Temp limit exceeded")
                                        // Temperature notification
                                        nu.displayLimitExceededNotification(s.name + " - " + context.getString(
                                            R.string.limit_exceeded_temp), s.chipID, r.dateTime.time)
                                        su.putDouble(selectedDayTimestamp.toString() + "_temp_max", r.temp)
                                        su.putBoolean(selectedDayTimestamp.toString() + "_temp_exceeded", true)
                                        break
                                    } else if (limitP1 > 0 && r.p1 < limitP1) {
                                        su.putBoolean(selectedDayTimestamp.toString() + "_temp_exceeded", false)
                                    }
                                    if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_humidity_exceeded") && limitHumidity > 0 && (if (su.getBoolean("notification_averages", true)) averageHumidity > limitHumidity else r.humidity > limitHumidity) && r.humidity > su.getDouble(selectedDayTimestamp.toString() + "_humidity_max")) {
                                        Log.i(Constants.TAG, "Humidity limit exceeded")
                                        // Humidity notification
                                        nu.displayLimitExceededNotification(s.name + " - " + context.getString(
                                            R.string.limit_exceeded_humidity), s.chipID, r.dateTime.time)
                                        su.putDouble(selectedDayTimestamp.toString() + "_humidity_max", r.humidity)
                                        su.putBoolean(selectedDayTimestamp.toString() + "_humidity_exceeded", true)
                                        break
                                    } else if (limitP1 > 0 && r.p1 < limitP1) {
                                        su.putBoolean(selectedDayTimestamp.toString() + "_humidity_exceeded", false)
                                    }
                                    if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_pressure_exceeded") && limitPressure > 0 && (if (su.getBoolean("notification_averages", true)) averagePressure > limitPressure else r.pressure > limitPressure) && r.humidity > su.getDouble(selectedDayTimestamp.toString() + "_pressure_max")) {
                                        Log.i(Constants.TAG, "Pressure limit exceeded")
                                        // Pressure notification
                                        nu.displayLimitExceededNotification(s.name + " - " + context.getString(
                                            R.string.limit_exceeded_pressure), s.chipID, r.dateTime.time)
                                        su.putDouble(selectedDayTimestamp.toString() + "_pressure_max", r.temp)
                                        su.putBoolean(selectedDayTimestamp.toString() + "_pressure_exceeded", true)
                                        break
                                    } else if (limitP1 > 0 && r.p1 < limitP1) {
                                        su.putBoolean(selectedDayTimestamp.toString() + "_pressure_exceeded", false)
                                    }
                                }
                            }
                        }

                        // Refresh home screen widgets
                        val updateLargeIntent = Intent(context, WidgetProviderLarge::class.java)
                        updateLargeIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        updateLargeIntent.putExtra(Constants.WIDGET_LARGE_EXTRA_SENSOR_ID, s.chipID)
                        context.sendBroadcast(updateLargeIntent)

                        val updateSmallIntent = Intent(context, WidgetProviderSmall::class.java)
                        updateSmallIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        updateSmallIntent.putExtra(Constants.WIDGET_SMALL_EXTRA_SENSOR_ID, s.chipID)
                        context.sendBroadcast(updateSmallIntent)
                    }
                } catch (e: Exception) {
                    syncSuccess = false
                }
            }
        }
        return syncSuccess
    }

    private fun getP1Average(records: ArrayList<DataRecord>) = records.map { it.p1 }.average()
    private fun getP2Average(records: ArrayList<DataRecord>) = records.map { it.p2 }.average()
    private fun getTempAverage(records: ArrayList<DataRecord>) = records.map { it.temp }.average()
    private fun getHumidityAverage(records: ArrayList<DataRecord>) = records.map { it.humidity }.average()
    private fun getPressureAverage(records: ArrayList<DataRecord>) = records.map { it.pressure }.average()

    private fun trimDataRecordsToSyncTime(chipId: String, allRecords: List<DataRecord>): ArrayList<DataRecord> {
        // Load last execution time
        val lastRecord = su.getLong("${chipId}_LastRecord", System.currentTimeMillis())
        val records = allRecords.filter { it.dateTime.time > lastRecord }

        // Save execution time
        if (allRecords.isNotEmpty()) su.putLong("${chipId}_LastRecord", allRecords[allRecords.size - 1].dateTime.time)
        return ArrayList(records)
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        Log.i(Constants.TAG, "Sync ended.")
        listener.onTaskCompleted(result == true)
    }
}