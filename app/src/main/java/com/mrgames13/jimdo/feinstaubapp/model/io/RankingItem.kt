/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.io

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RankingItem (
    @SerialName("co")
    val country: String,
    @SerialName("ci")
    val city: String = "",
    @SerialName("ct")
    val count: Int
)