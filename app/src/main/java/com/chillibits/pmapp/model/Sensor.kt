/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.model

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

    override operator fun compareTo(other: Any) = name.compareTo((other as Sensor).name)
}