/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import com.mrgames13.jimdo.feinstaubapp.model.DataRecord
import com.mrgames13.jimdo.feinstaubapp.model.DataRecordCompressedList
import io.ktor.client.request.forms.submitForm
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

suspend fun loadDataRecords(context: Context, chipId: String, from: Long, to: Long): ArrayList<DataRecord>? {
    try{
        val client = getNetworkClient()
        val params = Parameters.build {
            append("id", chipId)
            append("from", (from / 1000).toString())
            append("to", (to / 1000).toString())
            append("minimize", "true")
            append("gps", "true")
        }
        val response = client.submitForm<HttpResponse>(getBackendDataUrl(context), params, encodeInQuery = true)
        client.close()
        if(response.status == HttpStatusCode.OK) {
            return ArrayList(Json.parse(DataRecordCompressedList.serializer(), response.readText()).items.map { DataRecord(
                Date(it.time * 1000), it.p1, it.p2, it.t, it.h, it.p, it.la, it.ln, it.a)
            })
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ArrayList()
}