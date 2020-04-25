/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.io

import com.mrgames13.jimdo.feinstaubapp.model.dto.LinkDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User (
    @SerialName("id") val id: Int,
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("firstName")val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("sensorLinks") val sensorLinks: List<LinkDto> = emptyList(),
    @SerialName("role") val role: Int,
    @SerialName("status") val status: Int,
    @SerialName("creationTimestamp") val creationTimestamp: Long,
    @SerialName("lastEditTimestamp") val lastEditTimestamp: Long
)