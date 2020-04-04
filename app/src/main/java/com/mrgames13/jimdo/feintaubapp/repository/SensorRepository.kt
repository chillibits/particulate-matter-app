/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.mrgames13.jimdo.feintaubapp.model.db.Sensor
import com.mrgames13.jimdo.feintaubapp.network.isInternetAvailable
import com.mrgames13.jimdo.feintaubapp.network.loadSensors
import com.mrgames13.jimdo.feintaubapp.shared.getDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SensorRepository(application: Application) {

    // Variables as objects
    private val context = application
    private val sensorDao = application.getDatabase().sensorDao()
    val sensors: LiveData<List<Sensor>>

    init {
        sensors = sensorDao.getAll()
        tryToRefreshFromServer()
    }

    suspend fun insert(sensor: Sensor) {
        sensorDao.insert(listOf(sensor))
    }

    suspend fun insert(sensors: List<Sensor>) {
        sensorDao.insert(sensors)
    }

    fun tryToRefreshFromServer(): Boolean {
        return if(isInternetAvailable) {
            // Internet is available -> Download data
            CoroutineScope(Dispatchers.IO).launch {
                val sensors = loadSensors()
                insert(sensors)
            }
            true
        } else false
    }
}