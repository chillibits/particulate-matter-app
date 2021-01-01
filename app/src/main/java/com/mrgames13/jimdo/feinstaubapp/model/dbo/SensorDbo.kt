/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.dbo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor")
data class SensorDbo(
    @PrimaryKey
    @ColumnInfo(name = "chip_id") val chipId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color") val color: Int,
    @ColumnInfo(name = "owner") val isOwner: Boolean,
    @ColumnInfo(name= "published") val isPublished: Boolean
)