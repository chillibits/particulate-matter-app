/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.gms.maps.GoogleMap
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.convertDpToPx
import com.mrgames13.jimdo.feinstaubapp.ui.item.MarkerItem
import kotlinx.android.synthetic.main.fragment_all_sensors.view.*
import kotlinx.android.synthetic.main.info_window_marker.view.*
import kotlin.math.max

fun Context.showMarkerInfoWindow(map: GoogleMap, view: View, marker: MarkerItem) {
    // Collapse other windows
    if(view.clusterWindow1.isVisible) exitReveal(view.clusterWindow1)
    if(view.clusterWindow1.isVisible) exitReveal(view.clusterWindow2)
    val window = if(view.markerWindow1.isInvisible) {
        if(view.markerWindow2.isVisible) exitReveal(view.markerWindow2)
        // Expand new window
        view.markerWindow1
    } else {
        if(view.markerWindow1.isVisible) exitReveal(view.markerWindow1)
        // Expand new window
        view.markerWindow2
    }
    // Fill views with information
    val externalSensor = marker.externalSensor
    window.chipId.text = externalSensor?.chipId.toString()
    window.coordinates.text = marker.snippet
    window.countryCity.text = externalSensor?.chipId.toString()
    window.properties.setOnClickListener {

    }
    window.stats.setOnClickListener {

    }
    window.showMeasurements.setOnClickListener {
        exitReveal(window)
        // Open sensor activity

    }
    window.addFavourite.setOnClickListener {

    }

    // Set move listener for map
    map.setOnCameraMoveListener { updateWindowPosition(map, view, marker, window) }

    // Set position
    updateWindowPosition(map, view, marker, window)

    // Show it
    enterReveal(window)
}

private fun updateWindowPosition(map: GoogleMap, view: View, marker: MarkerItem, window: View) = window.run {
    val width = context.convertDpToPx(210.0).toInt()
    measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
    val screenPos = map.projection.toScreenLocation(marker.position)
    val xNew = max(0, screenPos.x - width / 2)
    val yNew = max(0, screenPos.y - measuredHeight - 140)
    Log.d(Constants.TAG, xNew.toString())
    Log.d(Constants.TAG, yNew.toString())
    left = if (xNew + width > view.width) view.width - width else xNew
    top = if (yNew + measuredHeight > view.height) view.height - measuredHeight else yNew
    bottom = top + measuredHeight
    right = left + width
}