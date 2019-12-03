/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.network.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.network.loadSensorInfo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_sensor_properties.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

fun showSensorInfoWindow(activity: Activity, smu: ServerMessagingUtils, sensorId: String, sensorName: String = activity.getString(R.string.unknown_sensor)) {
    if (smu.checkConnection(activity.container)) {
        val v = activity.layoutInflater.inflate(R.layout.dialog_sensor_properties, null)

        v.sensor_public_value.isSelected = true
        v.sensor_firmware_version_value.isSelected = true
        v.sensor_creation_value.isSelected = true
        v.sensor_lat_value.isSelected = true
        v.sensor_lng_value.isSelected = true
        v.sensor_alt_value.isSelected = true

        v.sensor_name_value.text = sensorName
        v.sensor_chip_id_value.text = sensorId

        AlertDialog.Builder(activity)
            .setIcon(R.drawable.info_outline)
            .setTitle(R.string.properties)
            .setCancelable(true)
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
                    activity.runOnUiThread {
                        v.sensor_public_value.text = activity.getString(R.string.yes)
                        v.sensor_firmware_version_value.text = result.firmwareVersion
                        v.sensor_creation_value.text = df.format(c.time)
                        v.sensor_lat_value.text = result.lat.toString().replace(".", ",")
                        v.sensor_lng_value.text = result.lng.toString().replace(".", ",")
                        v.sensor_alt_value.text = result.alt.toString().replace(".", ",") + " m"
                    }
                } else {
                    activity.runOnUiThread {
                        v.sensor_public_value.text = activity.getString(R.string.no)
                        v.sensor_firmware_version_value.text = "-"
                        v.sensor_creation_value.text = "-"
                        v.sensor_lat_value.text = "-"
                        v.sensor_lng_value.text = "-"
                        v.sensor_alt_value.text = "-"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity.runOnUiThread {
                    v.sensor_public_value.text = activity.getString(R.string.no)
                    v.sensor_firmware_version_value.text = "-"
                    v.sensor_creation_value.text = "-"
                    v.sensor_lat_value.text = "-"
                    v.sensor_lng_value.text = "-"
                    v.sensor_alt_value.text = "-"
                }
            }
        }
    }
}