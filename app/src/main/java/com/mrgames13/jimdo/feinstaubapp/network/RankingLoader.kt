/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import android.util.Log
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.io.RankingItem
import com.mrgames13.jimdo.feinstaubapp.shared.Constants.TAG
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

const val RANKING_CITY = 1
const val RANKING_COUNTRY = 2

suspend fun loadRanking(context: Context, mode: Int): List<RankingItem> {
    try {
        val subRes = if (mode == RANKING_CITY) "/ranking/city" else "/ranking/country"
        val response = networkClient.get<HttpStatement>(context.getString(R.string.api_root) + subRes + "?compressed").execute()
        if(response.status == HttpStatusCode.OK) {
            return ArrayList(Json.parse(RankingItem.serializer().list, URLDecoder.decode(response.readText(), StandardCharsets.UTF_8.name())).map {
                RankingItem(it.country, it.city, it.count)
            })
        } else {
            Log.e(TAG, response.status.toString())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}