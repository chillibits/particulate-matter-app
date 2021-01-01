/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.item

import com.google.android.gms.maps.model.LatLng
import com.mrgames13.jimdo.feinstaubapp.model.dbo.ExternalSensorDbo

class MarkerItem(val title: String, val snippet: String, val position: LatLng) {
    var externalSensor: ExternalSensorDbo? = null
}