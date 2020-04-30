/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.io

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DataRecord (
    @SerialName("t") val timestamp: Long,
    @SerialName("d") val sensorDataValues: List<SensorDataValues> = emptyList()
) {
    @Serializable
    data class SensorDataValues (
        @SerialName("t") val type: String,
        @SerialName("v") val value: Double
    )
}