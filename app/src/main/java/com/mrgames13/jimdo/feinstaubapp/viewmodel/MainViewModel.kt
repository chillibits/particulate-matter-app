/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mrgames13.jimdo.feinstaubapp.model.dao.ScrapingResultDbo
import com.mrgames13.jimdo.feinstaubapp.network.registerNetworkCallback
import com.mrgames13.jimdo.feinstaubapp.network.unregisterNetworkCallback
import com.mrgames13.jimdo.feinstaubapp.repository.ExternalSensorRepository
import com.mrgames13.jimdo.feinstaubapp.repository.ScrapingResultRepository
import com.mrgames13.jimdo.feinstaubapp.repository.SensorRepository
import com.mrgames13.jimdo.feinstaubapp.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Variables as objects
    private val context = application
    private val sensorRepository = SensorRepository(application)
    private val externalSensorRepository = ExternalSensorRepository(application)
    private val scrapingResultRepository = ScrapingResultRepository(application)
    private val userRepository = UserRepository(application)
    val sensors = sensorRepository.sensors
    val externalSensors = externalSensorRepository.externalSensors
    val scrapingResults = scrapingResultRepository.scrapingResults
    val users = userRepository.users

    // Variables
    var selectedPage = MutableLiveData(1)

    init {
        context.registerNetworkCallback()
        CoroutineScope(Dispatchers.IO).launch { manuallyRefreshExternalSensors() }
    }

    suspend fun manuallyRefreshSensors() = sensorRepository.manuallyRefreshSensors()
    suspend fun manuallyRefreshExternalSensors() = externalSensorRepository.manuallyRefreshExternalSensors()
    fun updateExternalSensorFilter() = externalSensorRepository.updateFilter()
    fun unregisterNetworkCallback() = context.unregisterNetworkCallback()
    fun addScrapingResult(sr: ScrapingResultDbo) = scrapingResultRepository.addScrapingResult(sr)
}