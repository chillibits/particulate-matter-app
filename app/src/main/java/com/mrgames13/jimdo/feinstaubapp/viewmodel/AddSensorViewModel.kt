/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.viewmodel

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import kotlin.random.Random

class AddSensorViewModel(application: Application) : AndroidViewModel(application) {

    // Variables as objects
    var selectedPlace = MutableLiveData<LatLng>()

    // Variables
    var name = MutableLiveData("")
    var chipId = MutableLiveData(0)
    var selectedColor = MutableLiveData(Color.BLACK)
    var indoorSensor = MutableLiveData(false)
    var showSensorOnMap = MutableLiveData(true)
    var address = MutableLiveData("")
    var heightAboveGround = MutableLiveData(0)
    var publishExactPosition = MutableLiveData(false)

    init {
        createRandomColor()
    }

    private fun createRandomColor() {
        val random = Random(System.currentTimeMillis())
        selectedColor.value = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
    }

    fun setName(value: CharSequence) { name.value = value.toString() }
    fun setChipId(value: CharSequence) { chipId.value = value.toString().toInt() }
    fun setIndoor(value: Boolean) { indoorSensor.value = value }
    fun setShowSensorOnMap(value: Boolean) { showSensorOnMap.value = value }
    fun setAddress(value: CharSequence) { address.value = value.toString() }
    fun setHeightAboveGround(value: CharSequence) { heightAboveGround.value = value.toString().toInt() }
    fun setPublishExactPosition(value: Boolean) { publishExactPosition.value = value }
}