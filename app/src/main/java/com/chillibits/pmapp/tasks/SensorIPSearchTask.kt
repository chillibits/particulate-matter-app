/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.tasks

import android.content.Context
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.text.format.Formatter
import android.util.Log
import com.chillibits.pmapp.model.ScrapingResult
import com.chillibits.pmapp.network.getNetworkClient
import com.chillibits.pmapp.tool.Constants.TAG
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.utils.io.charsets.MalformedInputException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.ConnectException
import java.net.InetAddress

class SensorIPSearchTask(val context: Context, private val listener: OnSearchEventListener, private val searchedChipId: Int): AsyncTask<Void, Void, Void?>() {

    // Interfaces
    interface OnSearchEventListener {
        fun onSensorFound(sensor: ScrapingResult)
        fun onSearchFinished(sensorList: ArrayList<ScrapingResult>)
        fun onSearchFailed()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        Log.i(TAG, "Starting search ...")
        try {
            val sensorList = ArrayList<ScrapingResult>()

            // Loop over local network ip addresses
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wm.connectionInfo
            val ownIpAddress = connectionInfo.ipAddress
            val ownIPAddressString: String = Formatter.formatIpAddress(ownIpAddress)
            val ipAddressPrefix = ownIPAddressString.substring(0, ownIPAddressString.lastIndexOf(".") + 1)
            for (i in 1..254) {
                val ipAddress = ipAddressPrefix + i.toString()
                val name: InetAddress = InetAddress.getByName(ipAddress)
                if (name.isReachable(100)) {
                    Log.d(TAG, ipAddress)
                    // Test, if we can establish http connection to scrape the chip id
                    runBlocking(Dispatchers.IO) {
                        val result = scrapeSensorConfigSite(ipAddress)
                        if(result != null) {
                            if(searchedChipId > 0) {
                                // If we search for a single sensor, return it immediately
                                listener.onSensorFound(result)
                            } else {
                                // If we search after a list of sensors, append to list
                                sensorList.add(result)
                            }
                        }
                    }
                }
            }
            listener.onSearchFinished(sensorList)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error occurred while searching ip address.")
        }

        listener.onSearchFailed()
        return null
    }

    private suspend fun scrapeSensorConfigSite(ipAddress: String): ScrapingResult? {
        try {
            val client = getNetworkClient()
            val request = client.submitForm<HttpStatement>(
                "http://$ipAddress/config",
                Parameters.Empty,
                encodeInQuery = true
            )
            val response = request.execute()
            client.close()
            if (response.status == HttpStatusCode.OK) {
                val html = response.readText()
                val chipID = html.substringAfter("ID: ").substringBefore("<br/>")
                val macAddress = html.substringAfter("MAC: ").substringBefore("<br/>")
                val name = html.substringAfter("id='fs_ssid' placeholder='Name' value='").substringBefore("'")
                val sendToUsEnabled = html.substringAfter("name='send2fsapp' value='").substringBefore("'") == "1"
                return ScrapingResult(chipID, name, macAddress, sendToUsEnabled)
            }
        } catch (e1: MalformedInputException) {
        } catch (e2: ConnectException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}