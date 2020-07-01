/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.storage.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrgames13.jimdo.feinstaubapp.model.dao.SensorDbo

@Dao
interface SensorDao {
    @Query("SELECT * FROM sensor")
    fun getAll(): LiveData<List<SensorDbo>>

    @Query("SELECT * FROM sensor WHERE owner = 0")
    fun getFavorites(): List<SensorDbo>

    @Query("SELECT * FROM sensor WHERE owner = 1")
    fun getOwnSensors(): List<SensorDbo>

    @Query("SELECT * FROM sensor WHERE chip_id = :chipId LIMIT 1")
    fun getSensor(chipId: Int): SensorDbo

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sensors: List<SensorDbo>)
}