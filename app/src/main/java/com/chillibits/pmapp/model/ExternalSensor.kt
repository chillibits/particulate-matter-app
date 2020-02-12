/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.model

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializable
data class ExternalSensor (
    val chipId: String,
    val firmwareVersion: String = "unknown version",
    val lat: Double,
    val lng: Double,
    val alt: Double = 0.0,
    val creationDate: Long = 0
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

        override fun deserialize(decoder: Decoder) = ExternalSensorCompressedList(ExternalSensorCompressed.serializer().list.deserialize(decoder))
    }
}

@Serializable
data class ExternalSensorCompressed (
    val i: String,
    val l: Double,
    val b: Double
)