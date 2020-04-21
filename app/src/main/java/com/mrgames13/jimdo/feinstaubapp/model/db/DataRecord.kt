/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName

@Entity(tableName = "data-record")
data class DataRecord (
    @PrimaryKey
    @SerialName("t") @ColumnInfo(name = "timestamp") val timestamp: Long,
    @SerialName("d") @ColumnInfo(name = "data_values") val sensorDataValues: List<SensorDataValues> = emptyList()
) {
    data class SensorDataValues (
        @SerialName("t") val type: String,
        @SerialName("v") val value: Double
    )
}