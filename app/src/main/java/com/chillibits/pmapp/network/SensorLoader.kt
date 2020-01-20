/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.network

import android.app.Activity
import android.util.Log
import com.chillibits.pmapp.model.ExternalSensor
import com.chillibits.pmapp.model.ExternalSensorCompressedList
import com.chillibits.pmapp.model.ExternalSensorSyncPackage
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json

suspend fun loadSensorsSync(activity: Activity, lastRequest: String, hash: String): ExternalSensorSyncPackage? {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getall")
        append("last_request", lastRequest)
        append("cs", hash)
    }
    val request = client.submitForm<HttpStatement>(getBackendMainUrl(activity), params, encodeInQuery = false)
    val response = request.execute()
    client.close()
    if(handlePossibleErrors(activity, response.status)) {
        val result = response.readText()
        Log.d("FA", "Result: $result")
        return Json.parse(ExternalSensorSyncPackage.serializer(), result)
    }
    return null
}

suspend fun loadSensorsNonSync(activity: Activity): List<ExternalSensor> {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getallnonsync")
    }
    val request = client.submitForm<HttpStatement>(getBackendMainUrl(activity), params, encodeInQuery = false)
    val response = request.execute()
    client.close()
    return if(handlePossibleErrors(activity, response.status))
        Json.parse(ExternalSensorCompressedList.serializer(), response.readText()).items.map { ExternalSensor(chipId = it.i, lat = it.l, lng = it.b) }
    else emptyList()
}

suspend fun loadClusterAverage(activity: Activity, ids: ArrayList<String>): Double {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getclusterinfo")
        append("ids", ids.joinToString(";"))
    }
    val resuest = client.submitForm<HttpStatement>(getBackendMainUrl(activity), params, encodeInQuery = false)
    val response = resuest.execute()
    client.close()
    return if(handlePossibleErrors(activity, response.status))
        try { response.readText().toDouble() } catch (ignored: Exception) { 0.0 }
    else 0.0
}

suspend fun isSensorExisting(activity: Activity, chipId: String): Boolean {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "issensorexisting")
        append("chip_id", chipId)
    }
    val request = client.submitForm<HttpStatement>(getBackendMainUrl(activity), params, encodeInQuery = false)
    val response = request.execute()
    client.close()
    if(handlePossibleErrors(activity, response.status)) return response.readText() == "1"
    return false
}

suspend fun isSensorDataExisting(activity: Activity, chipId: String): Boolean {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "issensordataexisting")
        append("chip_id", chipId)
    }
    val request = client.submitForm<HttpStatement>(getBackendMainUrl(activity), params, encodeInQuery = false)
    val response = request.execute()
    client.close()
    if(handlePossibleErrors(activity, response.status)) return response.readText() == "1"
    return false
}

suspend fun addSensorOnServer(activity: Activity, chipId: String, lat: String, lng: String, alt: String): Boolean {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "addsensor")
        append("chip_id", chipId)
        append("lat", lat)
        append("lng", lng)
        append("alt", alt)
    }
    val request = client.submitForm<HttpStatement>(getBackendMainUrl(activity), params, encodeInQuery = false)
    val response = request.execute()
    client.close()
    if(handlePossibleErrors(activity, response.status)) return response.readText() == "1"
    return false
}

suspend fun loadSensorInfo(activity: Activity, chipId: String): ExternalSensor? {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getsensorinfo")
        append("chip_id", chipId)
    }
    val request = client.submitForm<HttpStatement>(getBackendMainUrl(activity), params, encodeInQuery = false)
    val response = request.execute()
    client.close()
    return if(handlePossibleErrors(activity, response.status))
        Json.parse(ExternalSensor.serializer(), response.readText())
    else null
}