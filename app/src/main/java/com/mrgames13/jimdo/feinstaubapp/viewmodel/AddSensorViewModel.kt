/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
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
    var chipId = MutableLiveData("")
    var selectedColor = MutableLiveData(createRandomColor())
    var indoorSensor = MutableLiveData(false)
    var showSensorOnMap = MutableLiveData(true)
    var address = MutableLiveData("")
    var heightAboveGround = MutableLiveData("")
    var publishExactPosition = MutableLiveData(false)

    private fun createRandomColor(): Int {
        val random = Random(System.currentTimeMillis())
        return Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
    }
}