/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.CommonObjects

class ExternalSensor {

    // Variables
    var chipID = "no_id"
    var lat = 0.0
    var lng = 0.0

    constructor()

    constructor(chip_id: String, lat: Double, lng: Double) {
        this.chipID = chip_id
        this.lat = lat
        this.lng = lng
    }
}
