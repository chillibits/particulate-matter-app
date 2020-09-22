/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.mrgames13.jimdo.feinstaubapp.model.dbo.ScrapingResultDbo
import com.mrgames13.jimdo.feinstaubapp.model.dbo.UserDbo
import com.mrgames13.jimdo.feinstaubapp.model.dto.UserDto
import com.mrgames13.jimdo.feinstaubapp.network.registerNetworkCallback
import com.mrgames13.jimdo.feinstaubapp.network.unregisterNetworkCallback
import com.mrgames13.jimdo.feinstaubapp.repository.ExternalSensorRepository
import com.mrgames13.jimdo.feinstaubapp.repository.ScrapingResultRepository
import com.mrgames13.jimdo.feinstaubapp.repository.SensorRepository
import com.mrgames13.jimdo.feinstaubapp.repository.UserRepository
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
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
    fun addScrapingResult(sr: ScrapingResultDbo) = scrapingResultRepository.addScrapingResult(sr)

    suspend fun signIn(userDto: UserDto) {
        val userDbo = UserDbo(userDto.id, userDto.firstName, userDto.lastName, userDto.role, userDto.status, System.currentTimeMillis())
        Log.d(Constants.TAG, userDbo.firstName)
        userRepository.insert(userDbo)
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterNetworkCallback()
    }
}