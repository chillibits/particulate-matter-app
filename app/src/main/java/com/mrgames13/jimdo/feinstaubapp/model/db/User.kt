/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "user")
data class User(
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "role") val role: Int,
    @ColumnInfo(name = "status") val status: Int
)