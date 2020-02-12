/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.network

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.chillibits.pmapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.BuildConfig
import com.mrgames13.jimdo.feinstaubapp.R
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun getNetworkClient() = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }
}

fun handlePossibleErrors(activity: Activity, status: HttpStatusCode): Boolean {
    if(status != HttpStatusCode.OK) {
        Log.e(Constants.TAG, "Something went wrong during the backend request: " + status.value + " - " + status.description)
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(activity, R.string.error_try_again, Toast.LENGTH_SHORT).show()
        }
        return false
    }
    return true
}

fun getBackendMainUrl(context: Context) = "https://" + context.getString(R.string.host) + String.format(context.getString(R.string.path_main), BuildConfig.VERSION_CODE)
fun getBackendDataUrl(context: Context) = "https://" + context.getString(R.string.host) + context.getString(R.string.path_data)