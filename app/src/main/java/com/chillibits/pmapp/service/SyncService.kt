/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.chillibits.pmapp.tasks.SyncTask
import com.chillibits.pmapp.tasks.SyncTask.OnTaskCompleteListener
import com.chillibits.pmapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.R

class SyncService: Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(Constants.TAG, "Started service")
        // Display foreground notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = Notification.Builder(this, Constants.CHANNEL_SYSTEM)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.sync_running))
                .setAutoCancel(true)
            startForeground(10001, builder.build())
        }

        // Start sync
        val task = SyncTask(this, object: OnTaskCompleteListener {
            override fun onTaskCompleted(success: Boolean) {
                stopSelf()
            }
        }, true)
        task.execute()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}