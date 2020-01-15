/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.network

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.mrgames13.jimdo.feinstaubapp.BuildConfig
import com.mrgames13.jimdo.feinstaubapp.R
import io.ktor.client.request.forms.submitForm
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json

suspend fun loadServerInfo(activity: Activity): com.chillibits.pmapp.model.ServerInfo? {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getserverinfo")
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    if(handlePossibleErrors(activity, response.status)) return Json.parse(com.chillibits.pmapp.model.ServerInfo.serializer(), response.readText())
    return null
}

fun handleServerInfo(activity: Activity, view: View, serverInfo: com.chillibits.pmapp.model.ServerInfo) {
    var title = ""
    var message = ""
    when(serverInfo.serverStatus) {
        com.chillibits.pmapp.model.ServerInfo.SERVER_STATUS_OFFLINE -> {
            title = activity.getString(R.string.offline_t)
            message = if (serverInfo.userMessage.isEmpty()) activity.getString(R.string.offline_m) else serverInfo.userMessage
        }
        com.chillibits.pmapp.model.ServerInfo.SERVER_STATUS_MAINTENANCE -> {
            title = activity.getString(R.string.maintenance_t)
            message = if (serverInfo.userMessage.isEmpty()) activity.getString(R.string.maintenance_m) else serverInfo.userMessage
        }
        com.chillibits.pmapp.model.ServerInfo.SERVER_STATUS_SUPPORT_ENDED -> {
            title = activity.getString(R.string.support_end_t)
            message = if (serverInfo.userMessage.isEmpty()) activity.getString(R.string.support_end_m) else serverInfo.userMessage
        }
    }
    if(serverInfo.serverStatus == com.chillibits.pmapp.model.ServerInfo.SERVER_STATUS_ONLINE) {
        // Check for app updates
        if (BuildConfig.VERSION_CODE < serverInfo.minAppVersion) {
            AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(activity.getString(R.string.update_necessary_t))
                .setMessage(activity.getString(R.string.update_necessary_m))
                .setPositiveButton(activity.getString(R.string.download_update)) { _, _ ->
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")))
                    } catch (e: ActivityNotFoundException) {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
                    }
                    activity.finish()
                }
                .setNegativeButton(activity.getString(R.string.cancel)) { _, _ ->
                    activity.finish()
                }
                .show()
        } else if(BuildConfig.VERSION_CODE < serverInfo.latestAppVersion) {
            Snackbar.make(view, activity.getString(R.string.update_available), Snackbar.LENGTH_LONG)
                .setAction(activity.getString(R.string.download)) {
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")))
                    } catch (e: ActivityNotFoundException) {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
                    }
                }
                .show()
        }
    } else {
        // Show info dialog
        AlertDialog.Builder(activity)
            .setCancelable(false)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(activity.getString(R.string.ok)) { _, _ ->
                activity.finish()
            }
            .show()
    }
}