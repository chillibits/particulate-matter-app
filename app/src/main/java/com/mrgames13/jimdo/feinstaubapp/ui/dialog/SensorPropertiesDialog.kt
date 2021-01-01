/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dto.SensorDto
import com.mrgames13.jimdo.feinstaubapp.network.loadSingleSensor
import kotlinx.android.synthetic.main.dialog_sensor_properties.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.NumberFormat
import java.util.*

fun Context.showSensorPropertiesDialog(sensor: SensorDto?, chipId: Long) {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_sensor_properties, null)

    AlertDialog.Builder(this)
        .setTitle(R.string.properties)
        .setView(view)
        .setPositiveButton(R.string.ok, null)
        .show()

    // Fill view with data
    if(sensor != null) {
        // Sensor already loaded, show data
        showPropertiesData(view, sensor)
    } else {
        // Sensor not loaded, load it!
        CoroutineScope(Dispatchers.IO).launch {
            loadSingleSensor(view.context, chipId)?.let {
                withContext(Dispatchers.Main) { showPropertiesData(view, it) }
            }
        }
    }
}

private fun Context.showPropertiesData(view: View, sensor: SensorDto) {
    view.sensor_chip_id_value.text = sensor.chipId.toString()
    view.sensor_public_value.text = getString(if (sensor.published) R.string.yes else R.string.no)
    view.sensor_firmware_version_value.text = sensor.firmwareVersion
    val format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT, Locale.getDefault())
    view.sensor_creation_value.text = format.format(Date(sensor.creationTimestamp))
    view.sensor_lat_value.text = sensor.gpsLatitude.toString()
    view.sensor_lng_value.text = sensor.gpsLongitude.toString()
    view.sensor_indoor_value.text = getString(if (sensor.indoor) R.string.yes else R.string.no)
    view.sensor_alt_value.text = String.format(getString(R.string.altitude_unit),
        NumberFormat.getInstance().format(sensor.gpsAltitude))
}