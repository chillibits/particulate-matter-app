/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "data-record")
data class DataRecordDbo (
    @PrimaryKey
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "data_values") val sensorDataValues: List<SensorDataValues> = emptyList()
) {
    data class SensorDataValues (
        val type: String,
        val value: Double
    )
}