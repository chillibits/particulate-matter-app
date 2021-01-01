/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class LinkDto(
    private val id: Int,
    private val sensor: SensorDto,
    private val owner: Boolean,
    private val name: String,
    private val color: Int
)