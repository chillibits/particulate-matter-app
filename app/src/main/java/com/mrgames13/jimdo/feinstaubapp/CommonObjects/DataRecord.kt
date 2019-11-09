/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.CommonObjects

import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity
import java.util.*

class DataRecord(
    // Variables
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
        try {
            val otherRecord = other as DataRecord

            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_ASC) return dateTime.compareTo(otherRecord.dateTime)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_DESC) return otherRecord.dateTime.compareTo(dateTime)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_ASC) return p1.compareTo(otherRecord.p1)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_DESC) return otherRecord.p1.compareTo(p1)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_ASC) return p2.compareTo(otherRecord.p2)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_DESC) return otherRecord.p2.compareTo(p2)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_ASC) return temp.compareTo(otherRecord.temp)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_DESC) return otherRecord.temp.compareTo(temp)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_ASC) return humidity.compareTo(otherRecord.humidity)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_DESC) return otherRecord.humidity.compareTo(humidity)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_PRESSURE_ASC) return pressure.compareTo(otherRecord.pressure)
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_PRESSURE_DESC) return otherRecord.pressure.compareTo(pressure)
        } catch (ignored: Exception) {}
        return 0
    }
}