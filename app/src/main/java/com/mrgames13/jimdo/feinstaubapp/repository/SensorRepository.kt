/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.repository

import android.app.Application
import com.mrgames13.jimdo.feinstaubapp.model.dbo.SensorDbo
import com.mrgames13.jimdo.feinstaubapp.network.isInternetAvailable
import com.mrgames13.jimdo.feinstaubapp.network.loadSensors
import com.mrgames13.jimdo.feinstaubapp.shared.getDatabase

class SensorRepository(application: Application) {

    // Variables as objects
    private val context = application
    private val sensorDao = context.getDatabase().sensorDao()
    val sensors = sensorDao.getAll()

    suspend fun insert(sensor: SensorDbo) {
        sensorDao.insert(listOf(sensor))
    }

    suspend fun insert(sensors: List<SensorDbo>) {
        sensorDao.insert(sensors)
    }

    suspend fun manuallyRefreshSensors() {
        if(isInternetAvailable) {
            // Internet is available -> Download data
            val sensors = loadSensors(context)
            insert(sensors)
        }
    }
}