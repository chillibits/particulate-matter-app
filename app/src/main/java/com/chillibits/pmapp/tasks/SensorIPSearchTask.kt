/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.tasks

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.text.format.Formatter
import android.util.Log
import com.chillibits.pmapp.tool.Constants.TAG
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.net.InetAddress

class SensorIPSearchTask(val context: Context, val listener: OnSearchEventListener): AsyncTask<Void, Void, Void?>() {

    // Interfaces
    interface OnSearchEventListener {
        fun onSensorFound(ipAddress: String)
        fun onSearchFailed()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        var foundAddress = ""
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wm.connectionInfo
            val ipAddress = connectionInfo.ipAddress
            val ipString: String = Formatter.formatIpAddress(ipAddress)
            val prefix = ipString.substring(0, ipString.lastIndexOf(".") + 1)
            for (i in 1..254) {
                val testIp = prefix + i.toString()
                val name: InetAddress = InetAddress.getByName(testIp)
                if (name.isReachable(100)) {
                    val macAddress = getMacAddress(testIp)
                    Log.d(TAG, "$testIp : $macAddress")
                    // Found a device
                    foundAddress = testIp
                }
            }
            listener.onSensorFound(foundAddress)
        } catch (t: Throwable) {
            Log.e(TAG, "Error occurred while searching ip address.")
        }

        listener.onSearchFailed()
        return null
    }

    private fun getMacAddress(ipAddress: String?): String? {
        try {
            val br = BufferedReader(FileReader(File("/proc/net/arp")))
            var line: String
            while (br.readLine().also { line = it } != null) {
                if (line.contains(ipAddress!!)) return line
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}