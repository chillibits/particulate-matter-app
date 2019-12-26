/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.viewmodel

import com.google.android.gms.maps.model.LatLng

class MarkerItem(val title: String, val snippet: String, val position: LatLng) {
    var tag: String? = null
}