/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor")
data class Sensor(
    @PrimaryKey
    @ColumnInfo(name = "chip_id") val chipId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color") val color: Int,
    @ColumnInfo(name = "sensor_type") val sensorType: Int
): Comparable<Sensor> {
    override fun compareTo(other: Sensor) = name.compareTo(other.name)
}