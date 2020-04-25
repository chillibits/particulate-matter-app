/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.storage.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrgames13.jimdo.feinstaubapp.model.db.ScrapingResultDbo

@Dao
interface ScrapingResultDao {
    @Query("SELECT * FROM `scraping-result`")
    fun getAll(): LiveData<List<ScrapingResultDbo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sensors: List<ScrapingResultDbo>)
}