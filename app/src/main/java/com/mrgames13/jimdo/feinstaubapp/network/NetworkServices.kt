/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.net.ConnectivityManager
import android.net.Network
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer

var isInternetAvailable = false

val networkClient = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }
}

private val networkInfo = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        isInternetAvailable = true
    }

    override fun onUnavailable() {
        isInternetAvailable = false
    }
}