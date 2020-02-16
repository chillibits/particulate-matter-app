/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.storage.dao

import androidx.room.Dao
import androidx.room.Query
import com.mrgames13.jimdo.feintaubapp.model.Sensor

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensor")
    fun getAll(): List<Sensor>

    @Query("SELECT * FROM sensor WHERE own_sensor = 0")
    fun getFavorites(): List<Sensor>

    @Query("SELECT * FROM sensor WHERE own_sensor = 1")
    fun getOwnSensors(): List<Sensor>

    @Query("SELECT * FROM sensor WHERE chip_id = :chipId LIMIT 1")
    fun getSensor(chipId: Int): Sensor


}