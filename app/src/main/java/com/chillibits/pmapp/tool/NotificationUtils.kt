/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.tool

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.chillibits.pmapp.ui.activity.MainActivity
import com.mrgames13.jimdo.feinstaubapp.R

class NotificationUtils(private val context: Context) {

    // Variables as objects
    private val nm: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    fun displayLimitExceededNotification(message: String, chip_id: String, time: Long) {
        val i = Intent(context, MainActivity::class.java)
        i.putExtra("ChipID", chip_id)
        displayNotification(Constants.CHANNEL_LIMIT, context.getString(R.string.limit_exceeded), message, Integer.parseInt(chip_id), i, longArrayOf(0, VIBRATION_SHORT.toLong(), VIBRATION_SHORT.toLong(), VIBRATION_SHORT.toLong()), time)
    }

    fun displayMissingMeasurementsNotification(chip_id: String, sensor_name: String) {
        val i = Intent(context, MainActivity::class.java)
        i.putExtra("ChipID", chip_id)
        displayNotification(Constants.CHANNEL_MISSING_MEASUREMENTS, context.getString(R.string.sensor_breakdown), "$sensor_name ($chip_id)", Integer.parseInt(chip_id) * 10, i, longArrayOf(0, VIBRATION_SHORT.toLong(), VIBRATION_SHORT.toLong(), VIBRATION_SHORT.toLong()), System.currentTimeMillis())
    }

    private fun displayNotification(channel_id: String, title: String, message: String, id: Int = (Math.random()* Integer.MAX_VALUE).toInt(), i: Intent?, vibration: LongArray, time: Long) {
        // Setup notification
        val notification = buildNotification(channel_id, title, message)
        notification.run {
            setAutoCancel(true)
            setSmallIcon(R.drawable.notification_icon)
            setWhen(time)
            i?.let { setContentIntent(PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)) }
            // Get id
            priority = NotificationCompat.PRIORITY_HIGH
            setLights(ContextCompat.getColor(context, R.color.colorPrimary), LIGHT_SHORT, LIGHT_SHORT)
            setVibrate(vibration)
            nm.notify(id, build())
        }
    }

    private fun buildNotification(channel_id: String, title: String, message: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channel_id)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
    }

    fun cancelNotification(id: Int) {
        nm.cancel(id)
    }

    companion object {
        // Vibrations
        private const val VIBRATION_SHORT = 300
        // Lights
        private const val LIGHT_SHORT = 500

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager
                // System channel
                var importance = NotificationManager.IMPORTANCE_LOW
                val channelSystem = NotificationChannel(Constants.CHANNEL_SYSTEM, context.getString(R.string.nc_system_name), importance)
                channelSystem.setShowBadge(false)
                channelSystem.setSound(null, null)
                channelSystem.description = context.getString(R.string.nc_system_description)
                notificationManager.createNotificationChannel(channelSystem)
                // Limit channel
                importance = NotificationManager.IMPORTANCE_HIGH
                val channelLimit = NotificationChannel(Constants.CHANNEL_LIMIT, context.getString(R.string.nc_limit_name), importance)
                channelLimit.description = context.getString(R.string.nc_limit_description)
                notificationManager.createNotificationChannel(channelLimit)
                // Missing measurements channel
                importance = NotificationManager.IMPORTANCE_HIGH
                val channelMissingMeasurements = NotificationChannel(Constants.CHANNEL_MISSING_MEASUREMENTS, context.getString(R.string.nc_missing_measurements_name), importance)
                channelMissingMeasurements.description = context.getString(R.string.nc_missing_measurements_description)
                notificationManager.createNotificationChannel(channelMissingMeasurements)
            }
        }
    }
}