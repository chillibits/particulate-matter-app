/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto (
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val sensorLinks: List<LinkDto>,
    val role: Int,
    val status: Int
)