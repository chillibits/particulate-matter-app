/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import com.google.maps.android.clustering.ClusterItem
import com.mrgames13.jimdo.feinstaubapp.model.dao.ExternalSensorDbo
import com.mrgames13.jimdo.feinstaubapp.ui.item.MarkerItem

class SensorClusterItem (
    val marker: MarkerItem,
    val externalSensor: ExternalSensorDbo
): ClusterItem {
    override fun getPosition() = marker.position
    override fun getTitle() = marker.title
    override fun getSnippet() = marker.snippet
}