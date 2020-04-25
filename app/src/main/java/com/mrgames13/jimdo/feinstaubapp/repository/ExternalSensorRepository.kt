/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.repository

import android.app.Application
import androidx.lifecycle.MediatorLiveData
import com.mrgames13.jimdo.feinstaubapp.model.db.ExternalSensorDbo
import com.mrgames13.jimdo.feinstaubapp.network.isInternetAvailable
import com.mrgames13.jimdo.feinstaubapp.network.loadExternalSensors
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.getDatabase
import com.mrgames13.jimdo.feinstaubapp.shared.getPreferenceValue

class ExternalSensorRepository(application: Application) {

    // Variables as objects
    private val context = application
    private val externalSensorDao = context.getDatabase().externalSensorDao()
    private val externalSensorsRaw = externalSensorDao.getAll()
    val externalSensors = MediatorLiveData<List<ExternalSensorDbo>>()

    init {
        externalSensors.addSource(externalSensorsRaw) {result: List<ExternalSensorDbo>? ->
            result?.let { externalSensors.value = filterExternalSensors(it) }
        }
    }

    fun updateFilter() {
        externalSensorsRaw.value?.let {
            externalSensors.value = filterExternalSensors(it)
        }
    }

    suspend fun manuallyRefreshExternalSensors() {
        if(isInternetAvailable) {
            val sensors = loadExternalSensors(context, true)
            val sensorsDbo = sensors.map { ExternalSensorDbo(it.chipId, it.latitude, it.longitude, it.active) }
            externalSensorDao.insert(sensorsDbo)
        }
    }

    private fun filterExternalSensors(sensorsUnfiltered: List<ExternalSensorDbo>): List<ExternalSensorDbo> {
        if(context.getPreferenceValue(Constants.PREF_SHOW_INACTIVE_SENSORS, false))
            return sensorsUnfiltered
        return sensorsUnfiltered.filter { it.active }
    }
}