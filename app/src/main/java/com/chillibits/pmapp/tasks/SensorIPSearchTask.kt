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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.ConnectException
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.math.round


class SensorIPSearchTask(val context: Context, private val listener: OnSearchEventListener, private val searchedChipId: Int): AsyncTask<Void, Int, Void?>() {

    // Variables as objects
    private val sensorList = ArrayList<ScrapingResult>()
    private var sensor: ScrapingResult? = null

    // Variables
    private var nextIpSuffix = 1

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
            // Get information about the current network
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val connectionInfo = wm.connectionInfo
            val ownIpAddress = connectionInfo.ipAddress
            val ownIPAddressString: String = Formatter.formatIpAddress(ownIpAddress)
            val networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(ownIPAddressString))
            val ipAddressPrefix = ownIPAddressString.substring(0, ownIPAddressString.lastIndexOf(".") + 1)
            // Setting up several threads to split work
            val job1 = CoroutineScope(Dispatchers.IO).launch {
                while (nextIpSuffix < 255) nextIPAddress(networkInterface, ipAddressPrefix)
            }
            val job2 = CoroutineScope(Dispatchers.IO).launch {
                while (nextIpSuffix < 255) nextIPAddress(networkInterface, ipAddressPrefix)
            }
            val job3 = CoroutineScope(Dispatchers.IO).launch {
                while (nextIpSuffix < 255) nextIPAddress(networkInterface, ipAddressPrefix)
            }
            val job4 = CoroutineScope(Dispatchers.IO).launch {
                while (nextIpSuffix < 255) nextIPAddress(networkInterface, ipAddressPrefix)
            }
            val job5 = CoroutineScope(Dispatchers.IO).launch {
                while (nextIpSuffix < 255) nextIPAddress(networkInterface, ipAddressPrefix)
            }
            runBlocking {
                job1.join()
                job2.join()
                job3.join()
                job4.join()
                job5.join()
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

    private suspend fun nextIPAddress(networkInterface: NetworkInterface, ipAddressPrefix: String) {
        val currentIpSuffix = nextIpSuffix++
        Log.d(TAG, currentIpSuffix.toString())
        val ipAddress = ipAddressPrefix + currentIpSuffix.toString()
        val name: InetAddress = InetAddress.getByName(ipAddress)
        if (name.isReachable(networkInterface, 200, 100)) {
            // Test, if we can establish http connection to scrape the chip id
            sensor = scrapeSensorConfigSite(ipAddress)
            if(sensor != null && searchedChipId == 0) {
                Log.i(TAG, "Found sensor with ip: $ipAddress")
                sensorList.add(sensor!!)
            }
        }
        publishProgress(nextIpSuffix)
    }

    private suspend fun scrapeSensorConfigSite(ipAddress: String): ScrapingResult? {
        try {
            val client = getNetworkClient()
            val request = client.submitForm<HttpStatement>("http://$ipAddress/config", Parameters.Empty, encodeInQuery = true)
            val response = request.execute()
            client.close()
            if (response.status == HttpStatusCode.OK) {
                val html = response.readText()
                val chipID = html
                    .substringAfter("ID: ")
                    .substringBefore("<br/>")
                val macAddress = html
                    .substringAfter("MAC: ").substringBefore("<br/>")
                val firmwareVersion = html
                    .substring(html.indexOf("<br/>", html.indexOf("<br/>") +1) +4)
                    .substringAfter(": ")
                    .substringBefore("<br/>").replace("&nbsp;", " ")
                val name = html
                    .substringAfter("id='fs_ssid'")
                    .substringAfter("value='")
                    .substringBefore("'")
                val sendToUsEnabled = html
                    .substringAfter("id='send2fsapp'")
                    .substringBefore("/>")
                    .contains("checked='checked'")
                return ScrapingResult(chipID, name, ipAddress, macAddress, firmwareVersion, sendToUsEnabled)
            }
        } catch (e1: MalformedInputException) {
        } catch (e2: ConnectException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}