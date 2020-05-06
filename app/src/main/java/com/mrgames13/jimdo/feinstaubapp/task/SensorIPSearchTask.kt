/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.task

import android.content.Context
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.util.Log
import com.mrgames13.jimdo.feinstaubapp.model.db.ScrapingResultDbo
import com.mrgames13.jimdo.feinstaubapp.network.networkClient
import com.mrgames13.jimdo.feinstaubapp.shared.Constants.JOB_COUNT
import com.mrgames13.jimdo.feinstaubapp.shared.Constants.TAG
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import kotlinx.coroutines.*
import java.net.ConnectException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.nio.charset.MalformedInputException
import kotlin.math.round

class SensorIPSearchTask(val context: Context, private val listener: OnSearchEventListener, private val searchedChipId: Int): AsyncTask<Void, Int, Void?>() {

    // Variables as objects
    private val sensorList = ArrayList<ScrapingResultDbo>()
    private var sensor: ScrapingResultDbo? = null

    // Variables
    private var nextHostPart = 1

    // Interfaces
    interface OnSearchEventListener {
        fun onProgressUpdate(progress: Int)
        fun onSensorFound(sensor: ScrapingResultDbo?)
        fun onSearchFinished(sensorList: ArrayList<ScrapingResultDbo>)
        fun onSearchFailed()
    }

    override fun doInBackground(vararg params: Void?): Void? {
        Log.i(TAG, "Starting search ...")
        try {
            // Get information about the current network
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ownIPAddressString = (InetAddress.getByAddress(ByteArray(4) { i ->
                wm.connectionInfo.ipAddress.shr(i * 8).and(255).toByte()
            }) as Inet4Address).hostAddress
            val networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(ownIPAddressString))
            val ipAddressPrefix = ownIPAddressString.substring(0, ownIPAddressString.lastIndexOf(".") + 1)
            // Setting up several threads to split work
            val jobs = ArrayList<Job>()
            for (i in 0 until JOB_COUNT) {
                jobs.add(CoroutineScope(Dispatchers.IO).launch {
                    scanNetwork(networkInterface, ipAddressPrefix)
                })
            }
            runBlocking {
                jobs.forEach { it.join() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error occurred while searching ip address.")
        }
        return null
    }

    override fun onProgressUpdate(vararg values: Int?) {
        // super.onProgressUpdate(*values) // check if we need this
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

    private suspend fun scanNetwork(networkInterface: NetworkInterface, ipAddressPrefix: String) {
        while (nextHostPart < 255) nextIPAddress(networkInterface, ipAddressPrefix)
    }

    private suspend fun nextIPAddress(networkInterface: NetworkInterface, ipAddressPrefix: String) {
        val currentIpSuffix = nextHostPart++
        val ipAddress = ipAddressPrefix + currentIpSuffix.toString()
        if (InetAddress.getByName(ipAddress).isReachable(networkInterface, 200, 100)) {
            // Test, if we can establish a connection to scrape the chip id
            sensor = scrapeSensorConfigSite(ipAddress)
            if(sensor != null && searchedChipId == 0) {
                Log.i(TAG, "Found sensor with ip: $ipAddress")
                sensorList.add(sensor!!)
            }
        }
        publishProgress(nextHostPart)
    }

    private suspend fun scrapeSensorConfigSite(ipAddress: String): ScrapingResultDbo? {
        try {
            val response = networkClient
                .submitForm<HttpStatement>("http://$ipAddress/config", Parameters.Empty, encodeInQuery = true)
                .execute()
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
                return ScrapingResultDbo(
                    0,
                    chipID,
                    name,
                    ipAddress,
                    macAddress,
                    firmwareVersion,
                    sendToUsEnabled
                )
            }
        } catch (e1: MalformedInputException) {
        } catch (e2: ConnectException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}