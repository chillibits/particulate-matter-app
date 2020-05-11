/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scraping-result")
data class ScrapingResultDbo(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "chip_id") val chipID: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "ip_address") val ipAddress: String,
    @ColumnInfo(name = "mac_address") val macAddress: String,
    @ColumnInfo(name = "firmware_version") val firmwareVersion: String,
    @ColumnInfo(name = "send_enabled") val sendToUsEnabled: Boolean
)