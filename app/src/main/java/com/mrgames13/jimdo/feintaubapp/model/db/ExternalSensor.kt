/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializable
@Entity(tableName = "external-sensor")
data class ExternalSensor(
    @PrimaryKey
    @ColumnInfo(name = "chip_id") val chipId: Int,
    @ColumnInfo(name = "lat") val latitude: Double,
    @ColumnInfo(name = "lng") val longitude: Double
)

@Serializable
data class ExternalSensorSyncPackage (
    val ids: List<String>,
    val update: List<ExternalSensorCompressed>
)

@Serializable
data class ExternalSensorCompressedList (val items: List<ExternalSensorCompressed> = emptyList()) {
    @Serializer(ExternalSensorCompressedList::class)
    companion object : KSerializer<ExternalSensorCompressedList> {
        override val descriptor: SerialDescriptor = StringDescriptor.withName("ExternalSensorCompressedList")

        override fun serialize(encoder: Encoder, obj: ExternalSensorCompressedList) {
            ExternalSensorCompressed.serializer().list.serialize(encoder, obj.items)
        }

        override fun deserialize(decoder: Decoder) = ExternalSensorCompressedList(
            ExternalSensorCompressed.serializer().list.deserialize(decoder))
    }
}

@Serializable
data class ExternalSensorCompressed (
    val i: String,
    val l: Double,
    val b: Double
)