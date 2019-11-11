/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters

import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils
import kotlinx.android.synthetic.main.item_sensor.view.*
import java.util.*

class SelectSensorAdapter(private var context: Context, private val su: StorageUtils, private val sensors: ArrayList<Sensor>, private val selection_mode: Int) : RecyclerView.Adapter<SelectSensorAdapter.ViewHolder>() {

    // Variables as objects
    private val h: Handler = Handler()
    internal var selectedSensor: Sensor? = null
    private val selectedSensors = ArrayList<Sensor>()
    private var selectedSensorHolder: ViewHolder? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            // Initialize UI components
            itemView.item_more.visibility = View.GONE
        }

        internal fun deselect() {
            itemView.item_icon.flip(false)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        // Fill in data
        val sensor = sensors[pos]

        h.itemView.item_icon.frontLayout.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(sensor.color, BlendModeCompat.SRC_IN)
        h.itemView.item_name.text = sensor.name
        h.itemView.item_id.text = context.getString(R.string.chip_id) + " " + sensor.chipID

        h.itemView.setOnClickListener { h.itemView.item_icon.flip(!h.itemView.item_icon.isFlipped) }
        h.itemView.setOnLongClickListener {
            h.itemView.item_icon.flip(!h.itemView.item_icon.isFlipped)
            true
        }
        h.itemView.item_icon.setOnClickListener { h.itemView.item_icon.flip(!h.itemView.item_icon.isFlipped) }
        h.itemView.item_icon.setOnLongClickListener {
            h.itemView.item_icon.flip(!h.itemView.item_icon.isFlipped)
            true
        }
        h.itemView.item_icon.setOnFlippingListener { _, checked ->
            if (checked) {
                if (selection_mode == MODE_SELECTION_SINGLE) {
                    this@SelectSensorAdapter.h.postDelayed({
                        selectedSensorHolder?.deselect()
                        selectedSensorHolder = h
                    }, 50)
                    selectedSensor = sensor
                } else if (selection_mode == MODE_SELECTION_MULTI) {
                    selectedSensors.add(sensor)
                }
            } else {
                if (selection_mode == MODE_SELECTION_SINGLE) {
                    if (selectedSensor?.chipID == sensor.chipID) {
                        selectedSensor = null
                        selectedSensorHolder = null
                    }
                } else if (selection_mode == MODE_SELECTION_MULTI) {
                    selectedSensors.remove(sensor)
                }
            }
            h.itemView.setBackgroundColor(ContextCompat.getColor(context, if (checked) R.color.color_selection else R.color.transparent))
        }

        h.itemView.item_own_sensor.visibility = if (su.isSensorExisting(sensor.chipID)) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return sensors.size
    }

    companion object {
        // Constants
        const val MODE_SELECTION_SINGLE = 10001
        private const val MODE_SELECTION_MULTI = 10002
    }
}