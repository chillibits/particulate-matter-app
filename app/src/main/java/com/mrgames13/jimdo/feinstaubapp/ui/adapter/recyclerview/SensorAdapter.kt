/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.adapter.recyclerview

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.Sensor
import com.mrgames13.jimdo.feinstaubapp.network.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.network.isSensorExisting
import com.mrgames13.jimdo.feinstaubapp.network.loadSensorInfo
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.ui.activity.AddSensorActivity
import com.mrgames13.jimdo.feinstaubapp.ui.activity.MainActivity
import com.mrgames13.jimdo.feinstaubapp.ui.activity.SensorActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_sensor_properties.view.*
import kotlinx.android.synthetic.main.item_sensor.view.*
import kotlinx.android.synthetic.main.sensor_view_header.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

class SensorAdapter(private val activity: MainActivity, private val sensors: ArrayList<Sensor>, private val su: StorageUtils, private val smu: ServerMessagingUtils, private val mode: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Variables as objects
    val selectedSensors = ArrayList<Sensor>()
    private val viewHolders = ArrayList<ViewHolder>()

    // Variables
    private var clickStart: Long = 0

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)

    private inner class HeaderViewHolder

    internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Variables as objects
        internal val headerClose: ImageView = itemView.findViewById(R.id.header_close)
    }

    override fun getItemViewType(pos: Int): Int {
        return if (shallShowHeader()) if (pos == 0) TYPE_HEADER else TYPE_ITEM else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.sensor_view_header, parent, false)
            HeaderViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false)
            ViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        if (holder is ViewHolder) {
            viewHolders.add(holder)
            // Fill in data
            val sensor = sensors[if (shallShowHeader()) pos - 1 else pos]

            holder.itemView.item_icon.frontLayout.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(sensor.color, BlendModeCompat.SRC_IN)
            holder.itemView.item_name.text = sensor.name
            holder.itemView.item_id.text = activity.getString(R.string.chip_id) + " " + sensor.chipID

            holder.itemView.setOnClickListener {
                if (System.currentTimeMillis() > clickStart + 1000) {
                    if (selectedSensors.size > 0) {
                        holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped)
                    } else {
                        val i = Intent(activity, SensorActivity::class.java)
                        i.putExtra("Name", sensor.name)
                        i.putExtra("ID", sensor.chipID)
                        i.putExtra("Color", sensor.color)
                        activity.startActivity(i)
                        clickStart = System.currentTimeMillis()
                    }
                }
            }
            holder.itemView.setOnLongClickListener {
                holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped)
                true
            }
            holder.itemView.item_icon.setOnClickListener { holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped) }
            holder.itemView.item_icon.setOnLongClickListener {
                holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped)
                true
            }
            holder.itemView.item_icon.setOnFlippingListener { _, checked ->
                if (checked) selectedSensors.add(sensor)
                if (!checked) selectedSensors.remove(sensor)
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, if (checked) R.color.color_selection else R.color.transparent))
                activity.updateSelectionMode()
            }

            holder.itemView.item_more.setOnClickListener {
                val popup = PopupMenu(activity, holder.itemView.item_more)
                popup.inflate(R.menu.menu_sensor_more)
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_sensor_edit -> {
                            val i = Intent(activity, AddSensorActivity::class.java)
                            i.putExtra("Mode", AddSensorActivity.MODE_EDIT)
                            i.putExtra("Name", sensor.name)
                            i.putExtra("ID", sensor.chipID)
                            i.putExtra("Color", sensor.color)
                            if (mode == MODE_FAVOURITES) i.putExtra("Target", AddSensorActivity.TARGET_FAVOURITE)
                            activity.startActivity(i)
                        }
                        R.id.action_sensor_unlink -> {
                            AlertDialog.Builder(activity)
                                    .setCancelable(true)
                                    .setIcon(R.drawable.delete_red)
                                    .setTitle(R.string.unlink_sensor)
                                    .setMessage(activity.getString(R.string.really_unlink_sensor_1) + sensor.name + activity.getString(R.string.really_unlink_sensor_2))
                                    .setNegativeButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.unlink_sensor) { _, _ ->
                                        // Delete database for this sensor
                                        su.deleteDataDatabase(sensor.chipID)
                                        // Delete sensor from the database
                                        if (mode == MODE_FAVOURITES) {
                                            su.removeFavourite(sensor.chipID, false)
                                        } else {
                                            su.removeOwnSensor(sensor.chipID, false)
                                        }
                                        activity.refresh()
                                    }
                                    .show()
                        }
                        R.id.action_sensor_properties -> {
                            if (smu.checkConnection(activity.container)) {
                                val v = activity.layoutInflater.inflate(R.layout.dialog_sensor_properties, null)

                                v.sensor_public_value.isSelected = true
                                v.sensor_firmware_version_value.isSelected = true
                                v.sensor_creation_value.isSelected = true
                                v.sensor_lat_value.isSelected = true
                                v.sensor_lng_value.isSelected = true
                                v.sensor_alt_value.isSelected = true

                                v.sensor_name_value.text = sensor.name
                                v.sensor_chip_id_value.text = sensor.chipID

                                AlertDialog.Builder(activity)
                                    .setIcon(R.drawable.info_outline)
                                    .setTitle(R.string.properties)
                                    .setCancelable(true)
                                    .setView(v)
                                    .setPositiveButton(R.string.ok, null)
                                    .show()

                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val result = loadSensorInfo(activity, sensor.chipID)
                                        if(result != null) {
                                            val df = DateFormat.getDateInstance()
                                            val c = Calendar.getInstance()
                                            c.timeInMillis = result.creationDate * 1000
                                            activity.runOnUiThread {
                                                v.sensor_public_value.text = activity.getString(R.string.yes)
                                                v.sensor_firmware_version_value.text = result.firmwareVersion
                                                v.sensor_creation_value.text = df.format(c.time)
                                                v.sensor_lat_value.text = result.lat.toString().replace(".", ",")
                                                v.sensor_lng_value.text = result.lng.toString().replace(".", ",")
                                                v.sensor_alt_value.text = result.alt.toString().replace(".", ",") + " m"
                                            }
                                        } else {
                                            activity.runOnUiThread {
                                                v.sensor_public_value.text = activity.getString(R.string.no)
                                                v.sensor_firmware_version_value.text = "-"
                                                v.sensor_creation_value.text = "-"
                                                v.sensor_lat_value.text = "-"
                                                v.sensor_lng_value.text = "-"
                                                v.sensor_alt_value.text = "-"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        activity.runOnUiThread {
                                            v.sensor_public_value.text = activity.getString(R.string.no)
                                            v.sensor_firmware_version_value.text = "-"
                                            v.sensor_creation_value.text = "-"
                                            v.sensor_lat_value.text = "-"
                                            v.sensor_lng_value.text = "-"
                                            v.sensor_alt_value.text = "-"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    true
                }
                popup.show()
            }

            holder.itemView.item_more.visibility = if (su.isSensorExisting(sensor.chipID) && mode == MODE_FAVOURITES) View.GONE else View.VISIBLE
            holder.itemView.findViewById<View>(R.id.item_own_sensor).visibility = if (su.isSensorExisting(sensor.chipID) && mode == MODE_FAVOURITES) View.VISIBLE else View.GONE

            if (mode == MODE_OWN_SENSORS && !su.isSensorInOfflineMode(sensor.chipID)) { // TODO: Remove this part for the next update
                CoroutineScope(Dispatchers.IO).launch {
                    if (!isSensorExisting(activity, sensor.chipID)) {
                        activity.runOnUiThread {
                            holder.itemView.item_warning.visibility = View.VISIBLE
                            holder.itemView.item_warning.setOnClickListener {
                                val i = Intent(activity, AddSensorActivity::class.java)
                                i.putExtra("Mode", AddSensorActivity.MODE_COMPLETE)
                                i.putExtra("Name", sensor.name)
                                i.putExtra("ID", sensor.chipID)
                                i.putExtra("Color", sensor.color)
                                activity.startActivity(i)
                            }
                        }
                    }
                }
            }
        } else if (holder is HeaderViewHolder && shallShowHeader()) {
            holder.itemView.header_text.text = activity.getString(R.string.compare_instruction)
            holder.headerClose.setOnClickListener {
                su.putBoolean("SensorViewHeader", false)
                activity.refresh()
            }
        }
    }

    override fun getItemCount(): Int {
        return if (shallShowHeader()) sensors.size + 1 else sensors.size
    }

    private fun shallShowHeader(): Boolean {
        return su.getBoolean("SensorViewHeader", true)
    }

    fun deselectAllSensors() {
        CoroutineScope(Dispatchers.Default).launch {
            for (h in viewHolders) {
                try {
                    if (h.itemView.item_icon.isFlipped) {
                        activity.runOnUiThread { h.itemView.item_icon.flip(false) }
                        Thread.sleep(100)
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    companion object {
        // Constants
        const val MODE_FAVOURITES = 10001
        const val MODE_OWN_SENSORS = 10002
        private const val TYPE_ITEM = 10003
        private const val TYPE_HEADER = 10004
    }
}