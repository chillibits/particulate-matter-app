/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.core.location.LocationManagerCompat
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import io.ktor.client.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*

var isInternetAvailable = false

val networkClient = HttpClient {
    install(JsonFeature) {
        serializer = KotlinxSerializer()
    }
    expectSuccess = false
}

fun getNetworkClientWithAuth(context: Context): HttpClient {
    return HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
        install(Auth) {
            basic {
                username = Constants.API_AUTH_USERNAME
                password = context.getString(R.string.api_client_key)
            }
        }
        expectSuccess = false
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

fun Context.registerNetworkCallback() {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkRequest = NetworkRequest.Builder().build()
    connectivityManager.registerNetworkCallback(networkRequest, networkInfo)
}

fun Context.unregisterNetworkCallback() {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    connectivityManager.unregisterNetworkCallback(networkInfo)
}

fun Context.isLocationEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}