/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mrgames13.jimdo.feinstaubapp.model.db.DataRecord
import com.mrgames13.jimdo.feinstaubapp.model.db.ExternalSensor
import com.mrgames13.jimdo.feinstaubapp.model.db.ScrapingResult
import com.mrgames13.jimdo.feinstaubapp.model.db.Sensor
import com.mrgames13.jimdo.feinstaubapp.shared.Converters
import com.mrgames13.jimdo.feinstaubapp.storage.dao.DataDao
import com.mrgames13.jimdo.feinstaubapp.storage.dao.ExternalSensorDao
import com.mrgames13.jimdo.feinstaubapp.storage.dao.ScrapingResultDao
import com.mrgames13.jimdo.feinstaubapp.storage.dao.SensorDao

// Increase version whenever the structure of the local db changes
@Database(entities = [Sensor::class, ExternalSensor::class, ScrapingResult::class, DataRecord::class], exportSchema = false, version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun externalSensorDao(): ExternalSensorDao
    abstract fun scrapingResultDao(): ScrapingResultDao
    abstract fun dataDao(): DataDao
}