/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses

import com.github.mikephil.charting.data.Entry

class DiagramEntry(x: Double, y: Double, internal val unit: String) : Entry(x.toFloat(), y.toFloat())
