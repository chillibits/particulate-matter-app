/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.GoogleMap
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dbo.UserDbo
import com.mrgames13.jimdo.feinstaubapp.network.loadSingleSensor
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.SignInDialog
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showAddFavoriteDialog
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showSensorPropertiesDialog
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showSensorStatsDialog
import com.mrgames13.jimdo.feinstaubapp.ui.item.MarkerItem
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_all_sensors.view.*
import kotlinx.android.synthetic.main.info_window_marker.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

fun showMarkerInfoWindow(
    map: GoogleMap,
    view: View,
    marker: MarkerItem,
    fragmentManager: FragmentManager,
    user: UserDbo?,
    viewModel: MainViewModel
) {
    // Collapse other windows
    if(view.clusterWindow1.isVisible) exitReveal(view.clusterWindow1)
    if(view.clusterWindow2.isVisible) exitReveal(view.clusterWindow2)
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
    window.countryCity.text = view.context.getString(R.string.loading)
    window.properties.setOnClickListener(null)
    window.stats.setOnClickListener {
        externalSensor?.chipId?.let { view.context.showSensorStatsDialog(it) }
    }
    window.showMeasurements.setOnClickListener {
        exitReveal(window)
        // Open sensor activity

    }
    window.addFavourite.setOnClickListener(null)

    // Set move listener for map
    map.setOnCameraMoveListener { updateWindowPosition(map, view, marker, window) }

    // Set position
    updateWindowPosition(map, view, marker, window)

    // Show it
    enterReveal(window)

    // Load sensor info
    externalSensor?.let {
        CoroutineScope(Dispatchers.IO).launch {
            loadSingleSensor(view.context, it.chipId).let {sensor ->
                withContext(Dispatchers.Main) {
                    window.properties.setOnClickListener {
                        view.context.showSensorPropertiesDialog(sensor, 0)
                    }
                    window.addFavourite.setOnClickListener {
                        user?.let {
                            view.context.showAddFavoriteDialog(sensor!!, it, fragmentManager)
                        } ?: run {
                            SignInDialog(view.context, viewModel)
                                .withSkipOption()
                                .setOnSignInListener(object: SignInDialog.OnSignInListener {
                                    override fun onSignedIn() { exitReveal(window) }
                                    override fun onSkipOrCancelled() { view.context.showAddFavoriteDialog(sensor!!, null, fragmentManager) }
                                })
                                .show()
                        }
                        exitReveal(window)
                    }
                    window.countryCity.text = if(sensor != null)
                        String.format(view.context.getString(R.string.country_city), sensor.country, sensor.city)
                    else
                        view.context.getString(R.string.error_server_not_reachable)
                    // Enable / disable buttons
                    window.showMeasurements.isEnabled = sensor != null
                    window.addFavourite.isEnabled = sensor != null
                    window.properties.isEnabled = sensor != null
                    window.properties.setColorFilter(ContextCompat.getColor(view.context, if(sensor != null) R.color.colorPrimary else R.color.grayLight))
                    window.stats.isEnabled = sensor != null
                    window.stats.setColorFilter(ContextCompat.getColor(view.context, if(sensor != null) R.color.colorPrimary else R.color.grayLight))
                }
            }
        }
    }
}

private fun updateWindowPosition(map: GoogleMap, view: View, marker: MarkerItem, window: View) = window.run {
    val screenPos = map.projection.toScreenLocation(marker.position)
    val xNew = max(0, screenPos.x - measuredWidth / 2)
    val yNew = max(0, screenPos.y - measuredHeight - 140)
    left = if (xNew + width > view.width) view.width - measuredWidth else xNew
    top = if (yNew + measuredHeight > view.height) view.height - measuredHeight else yNew
    bottom = top + measuredHeight
    right = left + measuredWidth
}