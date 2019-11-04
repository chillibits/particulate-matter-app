package com.mrgames13.jimdo.feinstaubapp.HelpClasses

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class SensorClusterItem(lat: Double, lng: Double, private val title: String, private val snippet: String, val marker: MarkerItem) : ClusterItem {

    // Variables as objects
    private val position: LatLng

    init {
        this.position = LatLng(lat, lng)
    }

    override fun getPosition(): LatLng {
        return position
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }
}