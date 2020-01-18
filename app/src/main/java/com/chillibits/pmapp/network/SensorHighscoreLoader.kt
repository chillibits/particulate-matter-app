/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.network

import android.content.Context
import com.chillibits.pmapp.model.HighScoreList
import com.chillibits.pmapp.model.Highscore
import io.ktor.client.request.forms.submitForm
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import java.net.URLDecoder

suspend fun loadSensorHighscore(context: Context): ArrayList<Highscore?> {
    try{
        val client = getNetworkClient()
        val params = Parameters.build {
            append("command", "gethighscore")
        }
        val response = client.submitForm<HttpResponse>(getBackendMainUrl(context), params, encodeInQuery = false)
        client.close()
        if(response.status == HttpStatusCode.OK) {
            return ArrayList(Json.parse(HighScoreList.serializer(), URLDecoder.decode(response.readText(), "UTF-8")).items.map {
                Highscore(it.country, it.city, it.sensors)
            })
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ArrayList()
}