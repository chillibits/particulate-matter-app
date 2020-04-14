/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.item

import com.google.android.gms.maps.model.LatLng

class MarkerItem(val title: String, val snippet: String, val position: LatLng) {
    var tag: String? = null
}