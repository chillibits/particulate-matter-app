/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.view

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.network.loadSensorInfo
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_sensor_properties.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.util.*

fun showSensorInfoWindow(activity: Activity, smu: ServerMessagingUtils, sensorId: String, sensorName: String) {
    if (smu.checkConnection(activity.container)) {
        val v = LayoutInflater.from(activity).inflate(R.layout.dialog_sensor_properties, activity.container, false)

        v.sensor_name_value.text = sensorName
        v.sensor_chip_id_value.text = sensorId

        AlertDialog.Builder(activity)
            .setIcon(R.drawable.info_outline)
            .setTitle(R.string.properties)
            .setView(v)
            .setPositiveButton(R.string.ok, null)
            .show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = loadSensorInfo(activity, sensorId)
                if(result != null) {
                    val df = DateFormat.getDateInstance()
                    val c = Calendar.getInstance()
                    c.timeInMillis = result.creationDate * 1000

                    val nf = NumberFormat.getInstance(Locale.getDefault())
                    setInfoWindowValues(
                        activity,
                        v,
                        true,
                        result.firmwareVersion,
                        df.format(c.time),
                        nf.format(result.lat),
                        nf.format(result.lng),
                        nf.format(result.alt) + " m"
                    )
                } else {
                    setInfoWindowValues(activity, v)
                }
            } catch (e: Exception) {
                setInfoWindowValues(activity, v)
            }
        }
    }
}

fun setInfoWindowValues(activity: Activity, view: View, public: Boolean = false, firmwareVersion: String = "-", creationDate: String = "-", lat: String = "-", lng: String = "-", alt: String = "-") {
    CoroutineScope(Dispatchers.Main).launch {
        view.run {
            sensor_public_value.text = if(public) activity.getString(R.string.yes) else activity.getString(R.string.no)
            sensor_firmware_version_value.text = firmwareVersion
            sensor_creation_value.text = creationDate
            sensor_lat_value.text = lat
            sensor_lng_value.text = lng
            sensor_alt_value.text = alt
        }
    }
}