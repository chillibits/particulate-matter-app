/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.viewmodel

import com.google.android.gms.maps.model.LatLng

class MarkerItem(val title: String, val snippet: String, val position: LatLng) {
    var tag: String? = null
}