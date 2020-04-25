/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.storage.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrgames13.jimdo.feinstaubapp.model.db.UserDbo

@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): LiveData<List<UserDbo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(sensors: List<UserDbo>)
}