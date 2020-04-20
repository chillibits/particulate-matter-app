/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "sensor")
@Serializable
data class Sensor(
    @PrimaryKey
    @SerialName("chip_id") @ColumnInfo(name = "chip_id") val chipId: Long,
    @SerialName("name") @ColumnInfo(name = "name") val name: String,
    @SerialName("color") @ColumnInfo(name = "color") val color: Int,
    @SerialName("owner") @ColumnInfo(name = "owner") val isOwner: Boolean,
    @SerialName("published") @ColumnInfo(name= "published") val isPublished: Boolean
): Comparable<Sensor> {
    override fun compareTo(other: Sensor) = name.compareTo(other.name)
}