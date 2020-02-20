/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chillibits.pmapp.storage.dao.ExternalSensorDao
import com.chillibits.pmapp.storage.dao.SensorDao
import com.mrgames13.jimdo.feintaubapp.model.Sensor

@Database(entities = arrayOf(Sensor::class), exportSchema = false, version = 1) // Increase version whenever the structure of the local db changes
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorDao(): SensorDao
    abstract fun externalSensorDao(): ExternalSensorDao
}