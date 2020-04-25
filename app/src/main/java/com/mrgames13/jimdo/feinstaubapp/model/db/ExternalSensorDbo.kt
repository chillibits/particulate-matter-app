/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "external-sensor")
data class ExternalSensorDbo(
    @PrimaryKey
    @ColumnInfo(name = "chip_id") val chipId: Long,
    @ColumnInfo(name = "lat") val latitude: Double,
    @ColumnInfo(name = "lng") val longitude: Double,
    @ColumnInfo(name = "active") val active: Boolean
)