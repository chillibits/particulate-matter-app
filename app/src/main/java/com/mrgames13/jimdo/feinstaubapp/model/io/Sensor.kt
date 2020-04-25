/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.io

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sensor (
    @SerialName("chip_id") val chipId: Long,
    @SerialName("name") val name: String,
    @SerialName("color") val color: Int,
    @SerialName("owner") val isOwner: Boolean,
    @SerialName("published") val isPublished: Boolean
)