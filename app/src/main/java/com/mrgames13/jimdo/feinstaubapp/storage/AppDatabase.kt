/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mrgames13.jimdo.feinstaubapp.model.dbo.*
import com.mrgames13.jimdo.feinstaubapp.shared.Converters
import com.mrgames13.jimdo.feinstaubapp.storage.dao.*

// Increase version whenever the structure of the local db changes
@Database(entities = [
    SensorDbo::class,
    ExternalSensorDbo::class,
    ScrapingResultDbo::class,
    DataRecordDbo::class,
    UserDbo::class
], exportSchema = false, version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun externalSensorDao(): ExternalSensorDao
    abstract fun scrapingResultDao(): ScrapingResultDao
    abstract fun dataDao(): DataDao
    abstract fun userDao(): UserDao
}