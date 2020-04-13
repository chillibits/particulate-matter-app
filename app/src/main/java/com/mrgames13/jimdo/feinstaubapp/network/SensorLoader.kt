/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import android.util.Log
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.db.Sensor
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

suspend fun loadSensors(context: Context) : List<Sensor> {
    try {
        val response = networkClient.get<HttpStatement>(context.getString(R.string.api_root) + "sensor?compressed").execute()
        if(response.status == HttpStatusCode.OK) {
            return ArrayList(Json.parse(Sensor.serializer().list, URLDecoder.decode(response.readText(), StandardCharsets.UTF_8.name())))
        } else {
            Log.e(Constants.TAG, response.status.toString())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}