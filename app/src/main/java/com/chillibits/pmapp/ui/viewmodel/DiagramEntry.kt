/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.viewmodel

import com.github.mikephil.charting.data.Entry

class DiagramEntry(x: Double, y: Double, internal val unit: String) : Entry(x.toFloat(), y.toFloat())
