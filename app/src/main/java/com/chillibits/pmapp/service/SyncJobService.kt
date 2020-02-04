/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build
import android.util.Log
import com.chillibits.pmapp.tasks.SyncTask
import com.chillibits.pmapp.tool.Constants

class SyncJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        // Do the work
        val task = SyncTask(this, object: SyncTask.OnTaskCompleteListener {
            override fun onTaskCompleted(success: Boolean) {
                jobFinished(params, !success)
            }
        }, false)
        task.execute()

        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        Log.w(Constants.TAG, "Job stopped before completion")
        return false
    }

    private fun stopJob(params: JobParameters, reschedule: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) stopForeground(true)
        jobFinished(params, reschedule)
    }
}