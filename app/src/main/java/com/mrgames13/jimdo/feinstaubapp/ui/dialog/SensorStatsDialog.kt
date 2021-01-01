/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.network.loadStats
import kotlinx.android.synthetic.main.dialog_sensor_stats.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat

fun Context.showSensorStatsDialog(chipId: Long) {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_sensor_stats,null)

    val dialogBuilder = AlertDialog.Builder(this)
        .setTitle(R.string.sensor_stats)
        .setView(view)
        .setPositiveButton(R.string.ok, null)

    if(chipId == 0L) {
        dialogBuilder.setTitle(R.string.stats)
        if(chipId == 0L) dialogBuilder.setNeutralButton(R.string.more_info) { _, _ ->
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getString(R.string.url_stats_page))
            })
        }
    } else {
        dialogBuilder.setTitle(R.string.sensor_stats)
    }
    dialogBuilder.show()

    CoroutineScope(Dispatchers.IO).launch {
        loadStats(this@showSensorStatsDialog, chipId).let {
            withContext(Dispatchers.Main) {
                if(it != null) {
                    val formatter = NumberFormat.getNumberInstance()
                    view.stats_server_requests_today_value.text = formatter.format(it.serverRequestsTodayApp +
                            it.serverRequestsTodayWebApp + it.serverRequestsTodayGoogleActions)
                    view.stats_server_requests_yesterday_value.text = formatter.format(it.serverRequestsYesterdayApp +
                            it.serverRequestsYesterdayWebApp + it.serverRequestsYesterdayGoogleActions)
                    view.stats_server_requests_total_value.text = formatter.format(it.serverRequestsTotal)
                    view.stats_data_records_today_value.text = formatter.format(it.dataRecordsToday)
                    view.stats_data_records_yesterday_value.text = formatter.format(it.dataRecordsYesterday)
                    view.stats_data_records_this_month_value.text = formatter.format(it.dataRecordsThisMonth)
                    view.stats_data_records_prev_month_value.text = formatter.format(it.dataRecordsPrevMonth)
                    view.stats_data_records_total_value.text = formatter.format(it.dataRecordsTotal)
                } else {
                    view.stats_server_requests_today_value.text = getString(R.string.error)
                    view.stats_server_requests_yesterday_value.text = getString(R.string.error)
                    view.stats_server_requests_total_value.text = getString(R.string.error)
                    view.stats_data_records_today_value.text = getString(R.string.error)
                    view.stats_data_records_yesterday_value.text = getString(R.string.error)
                    view.stats_data_records_this_month_value.text = getString(R.string.error)
                    view.stats_data_records_prev_month_value.text = getString(R.string.error)
                    view.stats_data_records_total_value.text = getString(R.string.error)
                }
            }
        }
    }
}