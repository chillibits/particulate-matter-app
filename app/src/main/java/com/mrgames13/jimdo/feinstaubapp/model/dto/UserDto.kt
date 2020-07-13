/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto (
    private val id: Int,
    private val firstName: String,
    private val lastName: String,
    private val sensorLinks: List<LinkDto>,
    private val role: Int,
    private val status: Int
)