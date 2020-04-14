/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName

@Entity(tableName = "client")
data class ClientInfo (
    @PrimaryKey
    @SerialName("name") @ColumnInfo(name = "name") private val name: String,
    @SerialName("readable_name") @ColumnInfo(name = "readable_name") private val readableName: String,
    @SerialName("type") @ColumnInfo(name = "type") private val type: Int,
    @SerialName("status") @ColumnInfo(name = "status") private val status: Int,
    @SerialName("min_version") @ColumnInfo(name = "min_version") private val minVersion: Int,
    @SerialName("min_version_string") @ColumnInfo(name = "min_version_string") private val minVersionString: String,
    @SerialName("latest_version") @ColumnInfo(name = "latest_version") private val latestVersion: Int,
    @SerialName("latest_version_string") @ColumnInfo(name = "latest_version_string") private val latestVersionString: String,
    @SerialName("owner") @ColumnInfo(name = "owner") private val owner: String,
    @SerialName("user_message") @ColumnInfo(name = "user_message") private val userMessage: String
)