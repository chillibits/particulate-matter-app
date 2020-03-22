/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.model.io

import kotlinx.serialization.Serializable

@Serializable
data class RankingItem (
    val country: String,
    val city: String,
    val count: Int
)