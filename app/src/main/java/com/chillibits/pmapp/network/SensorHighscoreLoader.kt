/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.network

import android.content.Context
import com.chillibits.pmapp.model.HighScoreItem
import com.chillibits.pmapp.model.HighScoreList
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import java.net.URLDecoder

suspend fun loadSensorHighscore(context: Context): ArrayList<HighScoreItem?> {
    try{
        val client = getNetworkClient()
        val params = Parameters.build {
            append("command", "gethighscore")
        }
        val request = client.submitForm<HttpStatement>(getBackendMainUrl(context), params, encodeInQuery = false)
        val response = request.execute()
        client.close()
        if(response.status == HttpStatusCode.OK) {
            return ArrayList(Json.parse(HighScoreList.serializer(), URLDecoder.decode(response.readText(), "UTF-8")).items.map {
                HighScoreItem(it.country, it.city, it.sensors)
            })
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ArrayList()
}