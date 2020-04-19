/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.io

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatsItem(
    @SerialName("chipId") val chipId: Long,
    @SerialName("timestamp") val timestamp: Long,
    @SerialName("sensorsTotal") val sensorsTotal: Long,
    @SerialName("sensorsMapTotal") val sensorsMapTotal: Long,
    @SerialName("sensorsMapActive") val sensorsMapActive: Long,
    @SerialName("serverRequestsTotal") val serverRequestsTotal: Long,
    @SerialName("serverRequestsTodayApp") val serverRequestsTodayApp: Long,
    @SerialName("serverRequestsTodayWebApp") val serverRequestsTodayWebApp: Long,
    @SerialName("serverRequestsTodayGoogleActions") val serverRequestsTodayGoogleActions: Long,
    @SerialName("serverRequestsYesterdayApp") val serverRequestsYesterdayApp: Long,
    @SerialName("serverRequestsYesterdayWebApp") val serverRequestsYesterdayWebApp: Long,
    @SerialName("serverRequestsYesterdayGoogleActions") val serverRequestsYesterdayGoogleActions: Long,
    @SerialName("dataRecordsTotal") val dataRecordsTotal: Long,
    @SerialName("dataRecordsThisMonth") val dataRecordsThisMonth: Long,
    @SerialName("dataRecordsPrevMonth") val dataRecordsPrevMonth: Long,
    @SerialName("dataRecordsToday") val dataRecordsToday: Long,
    @SerialName("dataRecordsYesterday") val dataRecordsYesterday: Long
)