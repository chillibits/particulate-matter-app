/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import java.net.MalformedURLException
import java.net.URL

class ServerMessagingUtils(private val context: Context, private val su: StorageUtils) {

    // Variables as objects
    private val cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    //private val client: OkHttpClient = OkHttpClient()
    private lateinit var mainUrl: URL
    private lateinit var getUrl: URL

    // Variables
    private var repeatCount = 0

    val isInternetAvailable: Boolean
        get() {
            val ni = cm.activeNetworkInfo
            return ni != null && ni.isConnectedOrConnecting
        }

    init {
        // Create URL
        try { mainUrl = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) URL(SERVER_MAIN_SCRIPT_HTTP) else URL(SERVER_MAIN_SCRIPT_HTTPS) } catch (ignored: MalformedURLException) {}
        try { getUrl = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) URL(SERVER_GET_SCRIPT_HTTP) else URL(SERVER_GET_SCRIPT_HTTPS) } catch (ignored: MalformedURLException) {}
    }

    /*fun sendRequest(v: View?, params: HashMap<String, String>): String {
        /*if (isInternetAvailable) {
            try {
                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                for (key in params.keys) body.addFormDataPart(key, params[key]!!)
                val request = Request.Builder()
                        .url(mainUrl)
                        .post(body.build())
                        .build()
                client.newCall(request).execute().use { response -> return response.body()!!.string() }
            } catch (e: IOException) {
                e.printStackTrace()
                repeatCount++
                return if (repeatCount <= MAX_REQUEST_REPEAT) sendRequest(v, params) else ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            if (v != null) checkConnection(v)
        }*/
        return ""
    }*/

    fun checkConnection(v: View): Boolean {
        return if (isInternetAvailable) {
            true
        } else {
            Snackbar.make(v, context.resources.getString(R.string.internet_is_not_available), Snackbar.LENGTH_LONG)
                    .setAction(R.string.activate_wlan) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                        } else {
                            wifiManager.isWifiEnabled = true
                        }
                    }
                    .show()
            false
        }
    }

    companion object {
        // Constants
        private const val SERVER_ADRESS_HTTP = "http://h2801469.stratoserver.net/"
        private const val SERVER_ADRESS_HTTPS = "https://h2801469.stratoserver.net/"
        private const val SERVER_MAIN_SCRIPT_HTTP = SERVER_ADRESS_HTTP + "ServerScript_v310.php"
        private const val SERVER_MAIN_SCRIPT_HTTPS = SERVER_ADRESS_HTTPS + "ServerScript_v310.php"
        private const val SERVER_GET_SCRIPT_HTTP = SERVER_ADRESS_HTTP + "get.php"
        private const val SERVER_GET_SCRIPT_HTTPS = SERVER_ADRESS_HTTPS + "get.php"
        private const val MAX_REQUEST_REPEAT = 10
    }
}