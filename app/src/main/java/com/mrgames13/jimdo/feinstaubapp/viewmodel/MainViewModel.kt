/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mrgames13.jimdo.feinstaubapp.network.registerNetworkCallback
import com.mrgames13.jimdo.feinstaubapp.network.unregisterNetworkCallback
import com.mrgames13.jimdo.feinstaubapp.repository.ExternalSensorRepository
import com.mrgames13.jimdo.feinstaubapp.repository.SensorRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Variables as objects
    private val context = application
    private val sensorRepository = SensorRepository(application)
    private val externalSensorRepository = ExternalSensorRepository(application)
    val sensors = sensorRepository.sensors
    val externalSensors = externalSensorRepository.externalSensors

    init {
        context.registerNetworkCallback()
    }

    suspend fun manuallyRefreshSensors() {
        sensorRepository.manuallyRefreshSensors()
    }

    suspend fun manuallyRefreshExternalSensors() {
        externalSensorRepository.manuallyRefreshExternalSensors()
    }

    fun unregisterNetworkCallback() {
        context.unregisterNetworkCallback()
    }
}