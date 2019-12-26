/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.content.Context
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.mrgames13.jimdo.feinstaubapp.ui.viewmodel.DiagramEntry
import kotlinx.android.synthetic.main.diagram_marker_view.view.*
import java.text.SimpleDateFormat
import java.util.*

class DiagramMarkerView(context: Context, layoutResource: Int, private val first_timestamp: Long) : MarkerView(context, layoutResource), IMarker {

    // Variables as objects
    private val sdf: SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var mOffset: MPPointF? = null

    override fun refreshContent(e: Entry, highlight: Highlight?) {
        try {
            val entry = e as DiagramEntry
            val date = Date((e.getX() * 1000 + first_timestamp).toLong())
            marker_time.text = sdf.format(date)
            marker_value.text = e.getY().toString() + " " + entry.unit
        } catch (ex: ClassCastException) {
            val date = Date((e.x * 1000 + first_timestamp).toLong())
            marker_time.text = sdf.format(date)
            marker_value.text = e.y.toString()
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        if (mOffset == null) mOffset = MPPointF((-(width / 2)).toFloat(), (-height - 30).toFloat())
        return mOffset as MPPointF
    }
}