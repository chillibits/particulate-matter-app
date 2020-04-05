/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mrgames13.jimdo.feinstaubapp.model.db.Sensor
import com.mrgames13.jimdo.feinstaubapp.storage.dao.ExternalSensorDao
import com.mrgames13.jimdo.feinstaubapp.storage.dao.SensorDao

@Database(entities = arrayOf(Sensor::class), exportSchema = false, version = 1) // Increase version whenever the structure of the local db changes
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun externalSensorDao(): ExternalSensorDao
}