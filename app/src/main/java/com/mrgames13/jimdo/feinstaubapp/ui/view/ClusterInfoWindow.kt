/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.Cluster
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.network.loadAverageOfMultipleChipIds
import com.mrgames13.jimdo.feinstaubapp.shared.round
import kotlinx.android.synthetic.main.fragment_all_sensors.view.*
import kotlinx.android.synthetic.main.info_window_cluster.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import kotlin.math.max

fun showClusterInfoWindow(map: GoogleMap, view: View, cluster: Cluster<SensorClusterItem>) {
    // Collapse other windows
    if(view.markerWindow1.isVisible) exitReveal(view.markerWindow1)
    if(view.markerWindow2.isVisible) exitReveal(view.markerWindow2)
    val window = if(view.clusterWindow1.isInvisible) {
        if(view.clusterWindow2.isVisible) exitReveal(view.clusterWindow2)
        // Expand new window
        view.clusterWindow1
    } else {
        if(view.clusterWindow1.isVisible) exitReveal(view.clusterWindow1)
        // Expand new window
        view.clusterWindow2
    }
    // Fill views with information
    window.sensorCount.text = String.format(view.context.getString(R.string.sensors), cluster.size)
    window.averageP1.text = view.context.getString(R.string.loading)
    window.averageP2.text = view.context.getString(R.string.loading)

    window.compareSensors.setOnClickListener {
        exitReveal(window)
        // Open compare activity

    }
    window.zoomIn.setOnClickListener {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(cluster.position, map.cameraPosition.zoom + 3)
        map.animateCamera(cameraUpdate)
        exitReveal(window)
    }

    // Set move listener for map
    map.setOnCameraMoveListener { updateWindowPosition(map, view, cluster, window) }

    // Set position
    updateWindowPosition(map, view, cluster, window)

    // Show it
    enterReveal(window)

    // Load cluster data
    CoroutineScope(Dispatchers.IO).launch {
        val chipIds = cluster.items.map { item -> item.title.toLong() }
        loadAverageOfMultipleChipIds(view.context, chipIds).let {
            withContext(Dispatchers.Main) {
                window.averageP1.text = String.format(
                    view.context.getString(R.string.average_p1),
                    NumberFormat.getInstance().format(it.sensorDataValues.singleOrNull { it.type == "SDS_P1" }?.value?.round(2))
                )
                window.averageP2.text = String.format(
                    view.context.getString(R.string.average_p2),
                    NumberFormat.getInstance().format(it.sensorDataValues.singleOrNull { it.type == "SDS_P2" }?.value?.round(2))
                )
            }
        }
    }
}

private fun updateWindowPosition(map: GoogleMap, view: View, cluster: Cluster<SensorClusterItem>, window: View) = window.run {
    val screenPos = map.projection.toScreenLocation(cluster.position)
    val xNew = max(0, screenPos.x - measuredWidth / 2)
    val yNew = max(0, screenPos.y - measuredHeight - 160)
    left = if (xNew + width > view.width) view.width - measuredWidth else xNew
    top = if (yNew + measuredHeight > view.height) view.height - measuredHeight else yNew
    bottom = top + measuredHeight
    right = left + measuredWidth
}