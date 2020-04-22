/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SensorDto (
    val chipId: Long,
    val firmwareVersion: String,
    val creationTimestamp: Long,
    val notes: String,
    val gpsLatitude: Double,
    val gpsLongitude: Double,
    val gpsAltitude: Int,
    val country: String,
    val city: String,
    val indoor: Boolean,
    val published: Boolean
)