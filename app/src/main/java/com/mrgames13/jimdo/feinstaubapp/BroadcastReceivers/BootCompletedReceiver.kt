/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.BroadcastReceivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils

class BootCompletedReceiver : BroadcastReceiver() {

    // Variables as objects
    private lateinit var su: StorageUtils

    // Variables
    private var backgroundSyncFrequency: Int = 0

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && (intent.action == "android.intent.action.BOOT_COMPLETED" || intent.action == "android.intent.action.QUICKBOOT_POWERON" || intent.action == "com.htc.intent.action.QUICKBOOT_POWERON")) {
            // Initialize StorageUtils
            su = StorageUtils(context)
            backgroundSyncFrequency = Integer.parseInt(su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) * 1000 * 60

            // Setup AlarmManager
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val startServiceIntent = Intent(context, SyncService::class.java)
            startServiceIntent.putExtra("FromBackground", true)
            val startServicePendingIntent = PendingIntent.getService(context, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, startServiceIntent, 0)
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), backgroundSyncFrequency.toLong(), startServicePendingIntent)
        }
    }
}