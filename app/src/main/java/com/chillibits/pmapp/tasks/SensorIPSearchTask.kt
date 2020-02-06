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
import kotlin.math.round

class SensorIPSearchTask(val context: Context, private val listener: OnSearchEventListener, private val searchedChipId: Int): AsyncTask<Void, Int, Void?>() {

    // Variables as objects
    private val sensorList = ArrayList<ScrapingResult>()
    private var sensor: ScrapingResult? = null

    // Interfaces
    interface OnSearchEventListener {
        fun onProgressUpdate(progress: Int)
        fun onSensorFound(sensor: ScrapingResult?)
        fun onSearchFinished(sensorList: ArrayList<ScrapingResult>)
        fun onSearchFailed()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        Log.i(TAG, "Starting search ...")
        try {
            // Loop over local network ip addresses
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wm.connectionInfo
            val ownIpAddress = connectionInfo.ipAddress
            val ownIPAddressString: String = Formatter.formatIpAddress(ownIpAddress)
            val ipAddressPrefix = ownIPAddressString.substring(0, ownIPAddressString.lastIndexOf(".") + 1)
            for (i in 1..254) {
                val ipAddress = ipAddressPrefix + i.toString()
                val name: InetAddress = InetAddress.getByName(ipAddress)
                if (name.isReachable(250)) {
                    // Test, if we can establish http connection to scrape the chip id
                    runBlocking(Dispatchers.IO) {
                        sensor = scrapeSensorConfigSite(ipAddress)
                        if(sensor != null && searchedChipId == 0) {
                            Log.i(TAG, "Found sensor with ip: $ipAddress")
                            sensorList.add(sensor!!)
                        }
                    }
                }
                publishProgress(i)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error occurred while searching ip address.")
        }
        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)
        listener.onProgressUpdate(round(100.0 / 255.0 * values[0]!!).toInt())
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        when {
            searchedChipId > 0 -> listener.onSensorFound(sensor)
            sensorList.size > 0 -> listener.onSearchFinished(sensorList)
            else -> listener.onSearchFailed()
        }
    }

    private suspend fun scrapeSensorConfigSite(ipAddress: String): ScrapingResult? {
        try {
            val client = getNetworkClient()
            val request = client.submitForm<HttpStatement>("http://$ipAddress/config", Parameters.Empty, encodeInQuery = true)
            val response = request.execute()
            client.close()
            if (response.status == HttpStatusCode.OK) {
                val html = response.readText()
                val chipID = html.substringAfter("ID: ").substringBefore("<br/>")
                val macAddress = html.substringAfter("MAC: ").substringBefore("<br/>")
                val firmwareVersion = html.substringAfter("Firmware: ").substringBefore("&nbsp;")
                val name = html.substringAfter("id='fs_ssid' placeholder='Name' value='").substringBefore("'")
                val sendToUsEnabled = html.substringAfter("name='send2fsapp' value='").substringBefore("'") == "1"
                return ScrapingResult(chipID, name, ipAddress, macAddress, firmwareVersion, sendToUsEnabled)
            }
        } catch (e1: MalformedInputException) {
            e1.printStackTrace()
        } catch (e2: ConnectException) {
            e2.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}