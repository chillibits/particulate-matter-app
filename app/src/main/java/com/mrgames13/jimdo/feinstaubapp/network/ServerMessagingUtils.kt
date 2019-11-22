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

class ServerMessagingUtils(private val context: Context) {

    // Variables as objects
    private val cm: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // Variables

    val isInternetAvailable: Boolean
        get() {
            val ni = cm.activeNetworkInfo
            return ni != null && ni.isConnectedOrConnecting
        }

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
}