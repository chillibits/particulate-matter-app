/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "external-sensor")
data class ExternalSensor(
    @PrimaryKey
    @SerialName("i") @ColumnInfo(name = "chip_id") val chipId: Long,
    @SerialName("la") @ColumnInfo(name = "lat") val latitude: Double,
    @SerialName("lo") @ColumnInfo(name = "lng") val longitude: Double,
    @SerialName("a") @ColumnInfo(name = "active") val active: Boolean
)

@Serializable
data class ExternalSensorSyncPackage (
    val ids: List<String>,
    val update: List<ExternalSensor>
)