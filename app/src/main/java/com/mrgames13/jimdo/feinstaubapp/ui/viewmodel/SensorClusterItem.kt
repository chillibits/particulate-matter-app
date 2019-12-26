/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.viewmodel

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class SensorClusterItem(lat: Double, lng: Double, private val title: String, private val snippet: String, val marker: MarkerItem): ClusterItem {

    // Variables as objects
    private val position: LatLng = LatLng(lat, lng)

    override fun getPosition() = position
    override fun getTitle() = title
    override fun getSnippet() = snippet
}