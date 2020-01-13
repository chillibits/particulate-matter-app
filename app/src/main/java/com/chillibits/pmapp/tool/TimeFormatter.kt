/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.tool

import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class TimeFormatter(private val first_timestamp: Long) : ValueFormatter() {

    // Variables as objects
    private val sdf: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getAxisLabel(value: Float, axis: AxisBase?): String = sdf.format(Date((value * 1000 + first_timestamp).toLong()))
}