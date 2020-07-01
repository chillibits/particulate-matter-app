/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.model.other

import com.mrgames13.jimdo.feinstaubapp.model.dao.SensorDbo
import com.mrgames13.jimdo.feinstaubapp.model.dao.UserDbo

data class Link (
    val id: Int,
    val user: UserDbo,
    val sensor: SensorDbo,
    val owner: Boolean,
    val name: String,
    val color: Int,
    val creationTimestamp: Long
)