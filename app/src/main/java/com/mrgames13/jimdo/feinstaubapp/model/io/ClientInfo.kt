/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.io

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientInfo (
    @SerialName("name") private val name: String,
    @SerialName("readable_name")  private val readableName: String,
    @SerialName("type")  private val type: Int,
    @SerialName("status")  private val status: Int,
    @SerialName("min_version") private val minVersion: Int,
    @SerialName("min_version_string") private val minVersionString: String,
    @SerialName("latest_version") private val latestVersion: Int,
    @SerialName("latest_version_string") private val latestVersionString: String,
    @SerialName("owner") private val owner: String,
    @SerialName("user_message")  private val userMessage: String
)