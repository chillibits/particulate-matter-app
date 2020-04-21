/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.shared

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mrgames13.jimdo.feinstaubapp.model.db.DataRecord

class Converters {
    @TypeConverter
    fun stringToMeasurements(json: String?): List<DataRecord.SensorDataValues>? {
        val obj = Gson()
        val type = object : TypeToken<List<DataRecord.SensorDataValues?>?>() {}.type
        return obj.fromJson<List<DataRecord.SensorDataValues>>(json, type)
    }

    @TypeConverter
    fun measurementsToString(list: List<DataRecord.SensorDataValues?>?): String? {
        val obj = Gson()
        val type = object : TypeToken<List<DataRecord.SensorDataValues?>?>() {}.type
        return obj.toJson(list, type)
    }
}