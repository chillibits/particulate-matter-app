/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.repository

import android.app.Application
import com.mrgames13.jimdo.feinstaubapp.model.db.Sensor
import com.mrgames13.jimdo.feinstaubapp.network.isInternetAvailable
import com.mrgames13.jimdo.feinstaubapp.network.loadSensors
import com.mrgames13.jimdo.feinstaubapp.shared.getDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SensorRepository(application: Application) {

    // Variables as objects
    private val context = application
    private val sensorDao = context.getDatabase().sensorDao()
    val sensors = sensorDao.getAll()

    init {
        manuallyRefreshSensors()
    }

    suspend fun insert(sensor: Sensor) {
        sensorDao.insert(listOf(sensor))
    }

    suspend fun insert(sensors: List<Sensor>) {
        sensorDao.insert(sensors)
    }

    fun manuallyRefreshSensors() {
        if(isInternetAvailable) {
            // Internet is available -> Download data
            CoroutineScope(Dispatchers.IO).launch {
                val sensors = loadSensors()
                insert(sensors)
            }
        }
    }
}