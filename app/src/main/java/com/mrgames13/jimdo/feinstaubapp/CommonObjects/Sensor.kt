/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.CommonObjects

import java.io.Serializable

class Sensor: Comparable<Any>, Serializable {

    // Variables
    var chipID = "no_id"
    var name = "unknown"
    var color = 0

    constructor()

    constructor(id: String, name: String, color: Int) {
        this.chipID = id
        this.name = name
        this.color = color
    }

    fun setId(id: String) {
        this.chipID = id
    }

    override operator fun compareTo(other: Any): Int {
        val other_sensor = other as Sensor
        return name.compareTo(other_sensor.name)
    }
}