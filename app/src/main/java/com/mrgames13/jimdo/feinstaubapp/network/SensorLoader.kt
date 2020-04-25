/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import android.util.Log
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.db.SensorDbo
import com.mrgames13.jimdo.feinstaubapp.model.dto.SensorDto
import com.mrgames13.jimdo.feinstaubapp.model.io.Link
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

suspend fun loadSensors(context: Context) : List<SensorDbo> {
    try {
        /*val response = networkClient.get<HttpStatement>(context.getString(R.string.api_root) + "/sensor?compressed").execute()
        if(response.status == HttpStatusCode.OK) {
            return ArrayList(Json.parse(Sensor.serializer().list, URLDecoder.decode(response.readText(), StandardCharsets.UTF_8.name())))
        } else {
            Log.e(Constants.TAG, response.status.toString())
        }*/
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

suspend fun loadSingleSensor(context: Context, chipId: Long): SensorDto? {
    try {
        val response = networkClient.get<HttpStatement>(context.getString(R.string.api_root) + "/sensor/" + chipId.toString()).execute()
        if(response.status == HttpStatusCode.OK) {
            return Json.parse(SensorDto.serializer(), URLDecoder.decode(response.readText(), StandardCharsets.UTF_8.name()))
        } else {
            Log.e(Constants.TAG, response.status.toString())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

suspend fun addLink(context: Context, link: Link, chipId: Long): Boolean {
    try {
        val response = networkClient.get<HttpStatement>(context.getString(R.string.api_root) + "/link?chipId=$chipId").execute()
        return response.status == HttpStatusCode.OK
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}