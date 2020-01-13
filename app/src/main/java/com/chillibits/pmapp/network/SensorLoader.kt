/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.network

import android.app.Activity
import io.ktor.client.request.forms.submitForm
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json

suspend fun loadSensorsSync(activity: Activity, lastRequest: String, hash: String): com.chillibits.pmapp.model.ExternalSensorSyncPackage? {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getall")
        append("last_request", lastRequest)
        append("cs", hash)
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    if(handlePossibleErrors(activity, response.status)) return Json.parse(com.chillibits.pmapp.model.ExternalSensorSyncPackage.serializer(), response.readText())
    return null
}

suspend fun loadSensorsNonSync(activity: Activity): List<com.chillibits.pmapp.model.ExternalSensor> {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getallnonsync")
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    return if(handlePossibleErrors(activity, response.status))
        Json.parse(com.chillibits.pmapp.model.ExternalSensorCompressedList.serializer(), response.readText()).items.map {
            com.chillibits.pmapp.model.ExternalSensor(
                chipId = it.i,
                lat = it.l,
                lng = it.b
            )
        }
    else emptyList()
}

suspend fun loadClusterAverage(activity: Activity, ids: ArrayList<String>): Double {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getclusterinfo")
        append("ids", ids.joinToString(";"))
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
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

suspend fun loadSensorInfo(activity: Activity, chipId: String): com.chillibits.pmapp.model.ExternalSensor? {
    val client = getNetworkClient()
    val params = Parameters.build {
        append("command", "getsensorinfo")
        append("chip_id", chipId)
    }
    val response = client.submitForm<HttpResponse>(getBackendMainUrl(activity), params, encodeInQuery = false)
    client.close()
    return if(handlePossibleErrors(activity, response.status))
        Json.parse(com.chillibits.pmapp.model.ExternalSensor.serializer(), response.readText())
    else null
}