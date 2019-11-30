/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.app.Activity
import com.mrgames13.jimdo.feinstaubapp.model.ExternalSensor
import com.mrgames13.jimdo.feinstaubapp.model.ExternalSensorCompressedList
import com.mrgames13.jimdo.feinstaubapp.model.ExternalSensorSyncPackage
import io.ktor.client.request.forms.submitForm
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json

suspend fun loadSensorsSync(activity: Activity, lastRequest: String, hash: String): ExternalSensorSyncPackage? {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getall")
        append("last_request", lastRequest)
        append("cs", hash)
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    if(handlePossibleErrors(activity, response.status)) return Json.parse(ExternalSensorSyncPackage.serializer(), response.readText())
    return null
}

suspend fun loadSensorsNonSync(activity: Activity): List<ExternalSensor> {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getallnonsync")
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    if(handlePossibleErrors(activity, response.status)) {
        return Json.parse(ExternalSensorCompressedList.serializer(), response.readText()).items.map { ExternalSensor(chipId = it.i, lat = it.l, lng = it.b) }
    }
    return emptyList()
}

suspend fun loadClusterAverage(activity: Activity, ids: ArrayList<String>): Double {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getclusterinfo")
        append("ids", ids.joinToString(";"))
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    if(handlePossibleErrors(activity, response.status)) {
        return try { response.readText().toDouble() } catch (ignored: Exception) { 0.0 }
    }
    return 0.0
}

suspend fun isSensorExisting(activity: Activity, chipId: String): Boolean {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "issensorexisting")
        append("chip_id", chipId)
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
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
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
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
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
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
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    if(handlePossibleErrors(activity, response.status)) return Json.parse(ExternalSensor.serializer(), response.readText())
    return null
}