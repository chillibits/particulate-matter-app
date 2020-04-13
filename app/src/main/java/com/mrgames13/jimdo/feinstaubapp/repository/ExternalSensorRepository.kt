/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.repository

import android.app.Application
import com.mrgames13.jimdo.feinstaubapp.network.isInternetAvailable
import com.mrgames13.jimdo.feinstaubapp.network.loadExternalSensors
import com.mrgames13.jimdo.feinstaubapp.shared.getDatabase

class ExternalSensorRepository(application: Application) {

    // Variables as objects
    private val context = application
    private val externalSensorDao = context.getDatabase().externalSensorDao()
    val externalSensors = externalSensorDao.getAll()

    suspend fun manuallyRefreshExternalSensors() {
        if(isInternetAvailable) {
            val sensors = loadExternalSensors(context, true)
            externalSensorDao.insert(sensors)
        }
    }
}