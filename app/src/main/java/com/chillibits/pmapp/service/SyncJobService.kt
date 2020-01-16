/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.service

import android.app.Notification
import android.app.job.JobParameters
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.util.Log
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.network.loadDataRecords
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.NotificationUtils
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.tool.Tools
import com.chillibits.pmapp.widget.WidgetProvider
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit


class SyncJobService : JobService() {

    // Variables as objects
    private var calendar: Calendar? = null
    private var records: ArrayList<com.chillibits.pmapp.model.DataRecord>? = null

    // Utils packages
    private lateinit var su: StorageUtils
    private lateinit var smu: ServerMessagingUtils
    private lateinit var nu: NotificationUtils

    // Variables
    private var limitP1: Int = 0
    private var limitP2: Int = 0
    private var limitTemp: Int = 0
    private var limitHumidity: Int = 0
    private var limitPressure: Int = 0
    private var selectedDayTimestamp: Long = 0

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        doWork(true, null)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onStartJob(params: JobParameters): Boolean {
        // Display foreground notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder: Notification.Builder = Notification.Builder(this, Constants.CHANNEL_SYSTEM)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("SmartTracker Running")
                .setAutoCancel(true)
            val notification: Notification = builder.build()
            startForeground(10001, notification)
        }

        doWork(false, params)
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        Log.w(Constants.TAG, "Job stopped before completion")
        return false
    }

    private fun doWork(fromForeground: Boolean, params: JobParameters?) {
        // Initialize StorageUtils
        su = StorageUtils(this)

        // Initialize ServerMessagingUtils
        smu = ServerMessagingUtils(this)

        // Initialize NotificationUtils
        nu = NotificationUtils(this)

        // Initialize calendar
        if (selectedDayTimestamp == 0L || calendar == null) {
            calendar = Calendar.getInstance()
            calendar!!.set(Calendar.HOUR_OF_DAY, 0)
            calendar!!.set(Calendar.MINUTE, 0)
            calendar!!.set(Calendar.SECOND, 0)
            calendar!!.set(Calendar.MILLISECOND, 0)
            selectedDayTimestamp = calendar!!.time.time
        }

        // Check if internet is available
        if (smu.isInternetAvailable) {
            // Get max limit from SharedPreferences
            try { // This try-catch-block is temporary placed because of an error occurrence of a NumberFormatException
                limitP1 = Integer.parseInt(su.getString("limit_p1", Constants.DEFAULT_P1_LIMIT.toString()))
                limitP2 = Integer.parseInt(su.getString("limit_p2", Constants.DEFAULT_P2_LIMIT.toString()))
                limitTemp = Integer.parseInt(su.getString("limit_temp", Constants.DEFAULT_TEMP_LIMIT.toString()))
                limitHumidity = Integer.parseInt(su.getString("limit_humidity", Constants.DEFAULT_HUMIDITY_LIMIT.toString()))
                limitPressure = Integer.parseInt(su.getString("limit_pressure", Constants.DEFAULT_PRESSURE_LIMIT.toString()))
            } catch (e: NumberFormatException) {
                limitP1 = Constants.DEFAULT_P1_LIMIT
                su.putString("limit_p1", limitP1.toString())
                limitP2 = Constants.DEFAULT_P2_LIMIT
                su.putString("limit_p2", limitP2.toString())
                limitTemp = Constants.DEFAULT_TEMP_LIMIT
                su.putString("limit_temp", limitTemp.toString())
                limitHumidity = Constants.DEFAULT_HUMIDITY_LIMIT
                su.putString("limit_humidity", limitHumidity.toString())
                limitPressure = Constants.DEFAULT_PRESSURE_LIMIT
                su.putString("limit_pressure", limitPressure.toString())
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Get timestamps for 'from' and 'to'
                    val from = selectedDayTimestamp
                    val to = selectedDayTimestamp + TimeUnit.DAYS.toMillis(1)

                    val sensors = ArrayList<com.chillibits.pmapp.model.Sensor>()
                    sensors.addAll(su.allFavourites)
                    sensors.addAll(su.allOwnSensors)
                    for (s in sensors) {
                        // Load existing records from the local database
                        records = su.loadRecords(s.chipID, from, to)
                        records!!.sort()
                        // Load records from server
                        val recordsExternal = loadDataRecords(applicationContext, s.chipID, if (records!!.size > 0) records!![records!!.size - 1].dateTime.time + 1000 else from, to)
                        recordsExternal?.let { records!!.addAll(recordsExternal) }
                        records!!.sort()

                        if (records!!.size > 0) {
                            // Detect a breakdown
                            if (su.getBoolean("notification_breakdown", true) && su.isSensorExisting(s.chipID) && Tools.isMeasurementBreakdown(su, records!!)) {
                                if (recordsExternal != null && !su.getBoolean("BD_" + s.chipID)) {
                                    nu.displayMissingMeasurementsNotification(s.chipID, s.name)
                                    su.putBoolean("BD_" + s.chipID, true)
                                }
                            } else {
                                nu.cancelNotification(Integer.parseInt(s.chipID) * 10)
                                su.removeKey("BD_" + s.chipID)
                            }
                            // Calculate average values
                            val averageP1 = getP1Average(records!!)
                            val averageP2 = getP2Average(records!!)
                            val averageTemp = getTempAverage(records!!)
                            val averageHumidity = getHumidityAverage(records!!)
                            val averagePressure = getPressureAverage(records!!)
                            records = trimDataRecordsToSyncTime(s.chipID, records!!)
                            // Evaluate
                            for (r in records!!) {
                                if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_p1_exceeded") && limitP1 > 0 && (if (su.getBoolean("notification_averages", true)) averageP1 > limitP1 else r.p1 > limitP1) && r.p1 > su.getDouble(selectedDayTimestamp.toString() + "_p1_max")) {
                                    Log.i(Constants.TAG, "P1 limit exceeded")
                                    // P1 notification
                                    nu.displayLimitExceededNotification(s.name + " - " + resources.getString(R.string.limit_exceeded_p1), s.chipID, r.dateTime.time)
                                    su.putDouble(selectedDayTimestamp.toString() + "_p1_max", r.p1)
                                    su.putBoolean(selectedDayTimestamp.toString() + "_p1_exceeded", true)
                                    break
                                } else if (limitP1 > 0 && r.p1 < limitP1) {
                                    su.putBoolean(selectedDayTimestamp.toString() + "_p1_exceeded", false)
                                }
                                if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_p2_exceeded") && limitP2 > 0 && (if (su.getBoolean("notification_averages", true)) averageP2 > limitP2 else r.p2 > limitP2) && r.p2 > su.getDouble(selectedDayTimestamp.toString() + "_p2_max")) {
                                    Log.i(Constants.TAG, "P2 limit exceeded")
                                    // P2 notification
                                    nu.displayLimitExceededNotification(s.name + " - " + resources.getString(R.string.limit_exceeded_p2), s.chipID, r.dateTime.time)
                                    su.putDouble(selectedDayTimestamp.toString() + "_p2_max", r.p2)
                                    su.putBoolean(selectedDayTimestamp.toString() + "_p2_exceeded", true)
                                    break
                                } else if (limitP1 > 0 && r.p1 < limitP1) {
                                    su.putBoolean(selectedDayTimestamp.toString() + "_p2_exceeded", false)
                                }
                                if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_temp_exceeded") && limitTemp > 0 && (if (su.getBoolean("notification_averages", true)) averageTemp > limitTemp else r.temp > limitTemp) && r.temp > su.getDouble(selectedDayTimestamp.toString() + "_temp_max")) {
                                    Log.i(Constants.TAG, "Temp limit exceeded")
                                    // Temperature notification
                                    nu.displayLimitExceededNotification(s.name + " - " + resources.getString(R.string.limit_exceeded_temp), s.chipID, r.dateTime.time)
                                    su.putDouble(selectedDayTimestamp.toString() + "_temp_max", r.temp)
                                    su.putBoolean(selectedDayTimestamp.toString() + "_temp_exceeded", true)
                                    break
                                } else if (limitP1 > 0 && r.p1 < limitP1) {
                                    su.putBoolean(selectedDayTimestamp.toString() + "_temp_exceeded", false)
                                }
                                if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_humidity_exceeded") && limitHumidity > 0 && (if (su.getBoolean("notification_averages", true)) averageHumidity > limitHumidity else r.humidity > limitHumidity) && r.humidity > su.getDouble(selectedDayTimestamp.toString() + "_humidity_max")) {
                                    Log.i(Constants.TAG, "Humidity limit exceeded")
                                    // Humidity notification
                                    nu.displayLimitExceededNotification(s.name + " - " + resources.getString(R.string.limit_exceeded_humidity), s.chipID, r.dateTime.time)
                                    su.putDouble(selectedDayTimestamp.toString() + "_humidity_max", r.humidity)
                                    su.putBoolean(selectedDayTimestamp.toString() + "_humidity_exceeded", true)
                                    break
                                } else if (limitP1 > 0 && r.p1 < limitP1) {
                                    su.putBoolean(selectedDayTimestamp.toString() + "_humidity_exceeded", false)
                                }
                                if (!fromForeground && !su.getBoolean(selectedDayTimestamp.toString() + "_pressure_exceeded") && limitPressure > 0 && (if (su.getBoolean("notification_averages", true)) averagePressure > limitPressure else r.pressure > limitPressure) && r.humidity > su.getDouble(selectedDayTimestamp.toString() + "_pressure_max")) {
                                    Log.i(Constants.TAG, "Pressure limit exceeded")
                                    // Pressure notification
                                    nu.displayLimitExceededNotification(s.name + " - " + resources.getString(R.string.limit_exceeded_pressure), s.chipID, r.dateTime.time)
                                    su.putDouble(selectedDayTimestamp.toString() + "_pressure_max", r.temp)
                                    su.putBoolean(selectedDayTimestamp.toString() + "_pressure_exceeded", true)
                                    break
                                } else if (limitP1 > 0 && r.p1 < limitP1) {
                                    su.putBoolean(selectedDayTimestamp.toString() + "_pressure_exceeded", false)
                                }
                            }

                            // Refresh homescreen widget
                            val updateIntent = Intent(applicationContext, WidgetProvider::class.java)
                            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            updateIntent.putExtra(Constants.WIDGET_EXTRA_SENSOR_ID, s.chipID)
                            sendBroadcast(updateIntent)
                        }
                    }

                    if (params != null) stopJob(params, false)
                } catch (e: Exception) {
                    if (params != null) stopJob(params, true)
                }
            }
        } else {
            if (params != null) stopJob(params, false)
        }
    }

    private fun stopJob(params: JobParameters, reschedule: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) stopForeground(true)
        jobFinished(params, reschedule)
    }

    private fun getP1Average(records: ArrayList<com.chillibits.pmapp.model.DataRecord>) = records.map { it.p1 }.average()
    private fun getP2Average(records: ArrayList<com.chillibits.pmapp.model.DataRecord>) = records.map { it.p2 }.average()
    private fun getTempAverage(records: ArrayList<com.chillibits.pmapp.model.DataRecord>) = records.map { it.temp }.average()
    private fun getHumidityAverage(records: ArrayList<com.chillibits.pmapp.model.DataRecord>) = records.map { it.humidity }.average()
    private fun getPressureAverage(records: ArrayList<com.chillibits.pmapp.model.DataRecord>) = records.map { it.pressure }.average()

    private fun trimDataRecordsToSyncTime(chipId: String, allRecords: List<com.chillibits.pmapp.model.DataRecord>): ArrayList<com.chillibits.pmapp.model.DataRecord> {
        // Load last execution time
        val lastRecord = su.getLong("${chipId}_LastRecord", System.currentTimeMillis())
        val records = allRecords.filter { it.dateTime.time > lastRecord }

        // Save execution time
        if (allRecords.isNotEmpty()) su.putLong("${chipId}_LastRecord", allRecords[allRecords.size - 1].dateTime.time)
        return ArrayList(records)
    }
}
