/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dto.UserDto
import com.mrgames13.jimdo.feinstaubapp.model.other.ApiError
import com.mrgames13.jimdo.feinstaubapp.model.other.User
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.getStringIdentifier
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

suspend fun loadUser(context: Context, email: String, password: String): UserDto? {
    try {
        val response = getNetworkClientWithAuth(context)
            .get<HttpStatement>(context.getString(R.string.api_root) + "/user/$email?password=$password").execute()
        val responseContent = URLDecoder.decode(response.readText().trim(), StandardCharsets.UTF_8.name())
        when (response.status) {
            HttpStatusCode.OK -> return Json.decodeFromString(responseContent)
            HttpStatusCode.NotAcceptable -> {
                // An error occurred, extract error json and show error message
                val error = Json.decodeFromString<ApiError>(responseContent)
                val message = context.getString(context.getStringIdentifier("error_message_" + error.errorCode))
                withContext(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            }
            else -> Log.e(Constants.TAG, response.status.toString())
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

suspend fun createUser(context: Context, email: String, password: String): User? {
    try {
        val user = User(0, email, password, "", "", emptyList(), Constants.ROLE_USER,
            Constants.STATUS_EMAIL_CONFIRMATION_PENDING, 0, 0)
        val response = getNetworkClientWithAuth(context).post<HttpStatement> {
            url(context.getString(R.string.api_root) + "/user")
            body = TextContent(Json.encodeToString(user), ContentType.Application.Json)
        }.execute()
        val responseContent = URLDecoder.decode(response.readText().trim(), StandardCharsets.UTF_8.name())
        when (response.status) {
            HttpStatusCode.OK -> return Json.decodeFromString(responseContent)
            HttpStatusCode.NotAcceptable -> {
                // An error occurred, extract error json and show error message
                val error = Json.decodeFromString<ApiError>(responseContent)
                val message = context.getString(context.getStringIdentifier("error_message_" + error.errorCode))
                withContext(Dispatchers.Main) { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
            }
            else -> {
                Log.e(Constants.TAG, response.status.toString())
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}