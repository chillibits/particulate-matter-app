/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SensorDto(
    private val chipId: Long,
    private val firmwareVersion: String,
    private val notes: String,
    private val gpsLatitude: Double,
    private val gpsLongitude: Double,
    private val gpsAltitude: Int,
    private val country: String,
    private val city: String,
    private val indoor: Boolean,
    private val published: Boolean
)