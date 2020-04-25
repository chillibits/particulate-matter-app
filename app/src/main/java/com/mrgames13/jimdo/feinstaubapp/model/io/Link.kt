/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.io

import com.mrgames13.jimdo.feinstaubapp.model.db.SensorDbo
import com.mrgames13.jimdo.feinstaubapp.model.db.UserDbo

data class Link (
    val id: Int,
    val user: UserDbo,
    val sensor: SensorDbo,
    val owner: Boolean,
    val name: String,
    val color: Int,
    val creationTimestamp: Long
)