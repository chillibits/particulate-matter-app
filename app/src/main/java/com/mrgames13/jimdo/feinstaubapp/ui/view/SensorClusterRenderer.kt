/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.content.Context
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.mrgames13.jimdo.feinstaubapp.model.dao.SensorDbo

class SensorClusterRenderer(
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<SensorClusterItem>?,
    private val sensors: LiveData<List<SensorDbo>>
) : DefaultClusterRenderer<SensorClusterItem>(context, map, clusterManager) {
    override fun onBeforeClusterItemRendered(item: SensorClusterItem, markerOptions: MarkerOptions) {
        val sensor = sensors.value?.find { s -> s.chipId == item.externalSensor.chipId }
        markerOptions.icon(
            BitmapDescriptorFactory.defaultMarker(
                when {
                    sensor != null && !sensor.isOwner -> BitmapDescriptorFactory.HUE_RED
                    sensor != null && sensor.isOwner -> BitmapDescriptorFactory.HUE_GREEN
                    !item.externalSensor.active -> BitmapDescriptorFactory.HUE_ORANGE
                    else -> BitmapDescriptorFactory.HUE_BLUE
                }
            )
        )
    }
}