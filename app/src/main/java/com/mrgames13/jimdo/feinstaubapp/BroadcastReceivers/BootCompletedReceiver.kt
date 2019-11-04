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
    private var su: StorageUtils? = null

    // Variables
    private var background_sync_frequency: Int = 0

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && (intent.action == "android.intent.action.BOOT_COMPLETED" || intent.action == "android.intent.action.QUICKBOOT_POWERON" || intent.action == "com.htc.intent.action.QUICKBOOT_POWERON")) {
            // Initialize StorageUtils
            su = StorageUtils(context)
            background_sync_frequency = Integer.parseInt(su!!.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) * 1000 * 60

            // Setup AlarmManager
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val start_service_intent = Intent(context, SyncService::class.java)
            start_service_intent.putExtra("FromBackground", true)
            val start_service_pending_intent = PendingIntent.getService(context, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, start_service_intent, 0)
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), background_sync_frequency.toLong(), start_service_pending_intent)
        }
    }
}