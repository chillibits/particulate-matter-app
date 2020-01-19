/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.model

import com.chillibits.pmapp.ui.activity.SensorActivity
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import java.util.*

class DataRecord(
    val dateTime: Date,
    val p1: Double,
    val p2: Double,
    val temp: Double,
    val humidity: Double,
    val pressure: Double,
    val lat: Double,
    val lng: Double,
    val alt: Double
): Comparable<Any> {
    override operator fun compareTo(other: Any): Int {
        val otherRecord = other as DataRecord
        return when(SensorActivity.sort_mode) {
            SensorActivity.SORT_MODE_TIME_ASC -> return dateTime.compareTo(otherRecord.dateTime)
            SensorActivity.SORT_MODE_TIME_DESC -> return otherRecord.dateTime.compareTo(dateTime)
            SensorActivity.SORT_MODE_VALUE1_ASC -> return p1.compareTo(otherRecord.p1)
            SensorActivity.SORT_MODE_VALUE1_DESC -> return otherRecord.p1.compareTo(p1)
            SensorActivity.SORT_MODE_VALUE2_ASC -> return p2.compareTo(otherRecord.p2)
            SensorActivity.SORT_MODE_VALUE2_DESC -> return otherRecord.p2.compareTo(p2)
            SensorActivity.SORT_MODE_TEMP_ASC -> return temp.compareTo(otherRecord.temp)
            SensorActivity.SORT_MODE_TEMP_DESC -> return otherRecord.temp.compareTo(temp)
            SensorActivity.SORT_MODE_HUMIDITY_ASC -> return humidity.compareTo(otherRecord.humidity)
            SensorActivity.SORT_MODE_HUMIDITY_DESC -> return otherRecord.humidity.compareTo(humidity)
            SensorActivity.SORT_MODE_PRESSURE_ASC -> return pressure.compareTo(otherRecord.pressure)
            SensorActivity.SORT_MODE_PRESSURE_DESC -> return otherRecord.pressure.compareTo(pressure)
            else -> 0
        }
    }
}

@Serializable
data class DataRecordCompressedList(val items: List<DataRecordCompressed> = emptyList()) {
    @Serializer(DataRecordCompressedList::class)
    companion object : KSerializer<DataRecordCompressedList> {
        override val descriptor: SerialDescriptor = StringDescriptor.withName("DataRecordCompressedList")

        override fun serialize(encoder: Encoder, obj: DataRecordCompressedList) {
            DataRecordCompressed.serializer().list.serialize(encoder, obj.items)
        }

        override fun deserialize(decoder: Decoder): DataRecordCompressedList {
            return DataRecordCompressedList(DataRecordCompressed.serializer().list.deserialize(decoder))
        }
    }
}

@Serializable
data class DataRecordCompressed (
    val time: Long,
    val p1: Double,
    val p2: Double,
    val t: Double,
    val h: Double,
    val p: Double,
    val la: Double,
    val ln: Double,
    val a: Double
)