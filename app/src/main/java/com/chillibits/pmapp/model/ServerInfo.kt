/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerInfo(
    val clientName: String,
    val serverStatus: Int,
    val minAppVersion: Int,
    val minAppVersionName: String,
    val latestAppVersion: Int,
    val latestAppVersionName: String,
    val serverOwner: String,
    val userMessage: String
) {
    companion object {
        const val SERVER_STATUS_ONLINE = 1
        const val SERVER_STATUS_OFFLINE = 2
        const val SERVER_STATUS_MAINTENANCE = 3
        const val SERVER_STATUS_SUPPORT_ENDED = 4
        const val SERVER_STATUS_ONLINE_WITH_CAMPAIGN = 5
    }
}