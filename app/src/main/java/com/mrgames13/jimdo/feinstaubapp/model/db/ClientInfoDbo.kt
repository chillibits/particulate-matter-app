/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "client")
data class ClientInfoDbo (
    @PrimaryKey
    @ColumnInfo(name = "name") private val name: String,
    @ColumnInfo(name = "readable_name") private val readableName: String,
    @ColumnInfo(name = "type") private val type: Int,
    @ColumnInfo(name = "status") private val status: Int,
    @ColumnInfo(name = "min_version") private val minVersion: Int,
    @ColumnInfo(name = "min_version_string") private val minVersionString: String,
    @ColumnInfo(name = "latest_version") private val latestVersion: Int,
    @ColumnInfo(name = "latest_version_string") private val latestVersionString: String,
    @ColumnInfo(name = "owner") private val owner: String,
    @ColumnInfo(name = "user_message") private val userMessage: String
)