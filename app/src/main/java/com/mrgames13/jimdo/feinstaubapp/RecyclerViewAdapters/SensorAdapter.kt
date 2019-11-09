/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.mrgames13.jimdo.feinstaubapp.App.AddSensorActivity
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils
import eu.davidea.flipview.FlipView
import org.json.JSONArray
import java.text.DateFormat
import java.util.*

class SensorAdapter(private val activity: MainActivity, private val sensors: ArrayList<Sensor>, private val su: StorageUtils, private val smu: ServerMessagingUtils, private val mode: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Variables as objects
    val selectedSensors = ArrayList<Sensor>()
    private val viewHolders = ArrayList<ViewHolder>()

    // Variables
    private var clickStart: Long = 0

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Components
        internal val itemIcon: FlipView = itemView.findViewById(R.id.item_icon)
        internal val itemName: TextView = itemView.findViewById(R.id.item_name)
        internal val itemId: TextView = itemView.findViewById(R.id.item_id)
        internal val itemWarning: ImageView = itemView.findViewById(R.id.item_warning)
        internal val itemMore: ImageView = itemView.findViewById(R.id.item_more)
    }

    private inner class HeaderViewHolder

    internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Variables as objects
        internal val headerText: TextView = itemView.findViewById(R.id.header_text)
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

            holder.itemIcon.frontLayout.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(sensor.color, BlendModeCompat.SRC_IN)
            holder.itemName.text = sensor.name
            holder.itemId.text = activity.getString(R.string.chip_id) + " " + sensor.chipID

            holder.itemView.setOnClickListener {
                if (System.currentTimeMillis() > clickStart + 1000) {
                    if (selectedSensors.size > 0) {
                        holder.itemIcon.flip(!holder.itemIcon.isFlipped)
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
                holder.itemIcon.flip(!holder.itemIcon.isFlipped)
                true
            }
            holder.itemIcon.setOnClickListener { holder.itemIcon.flip(!holder.itemIcon.isFlipped) }
            holder.itemIcon.setOnLongClickListener {
                holder.itemIcon.flip(!holder.itemIcon.isFlipped)
                true
            }
            holder.itemIcon.setOnFlippingListener { flipView, checked ->
                if (checked) selectedSensors.add(sensor)
                if (!checked) selectedSensors.remove(sensor)
                holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, if (checked) R.color.color_selection else R.color.transparent))
                activity.updateSelectionMode()
            }

            holder.itemMore.setOnClickListener {
                val popup = PopupMenu(activity, holder.itemMore)
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
                            val d = AlertDialog.Builder(activity)
                                    .setCancelable(true)
                                    .setIcon(R.drawable.delete_red)
                                    .setTitle(R.string.unlink_sensor)
                                    .setMessage(activity.getString(R.string.really_unlink_sensor_1) + sensor.name + activity.getString(R.string.really_unlink_sensor_2))
                                    .setNegativeButton(R.string.cancel, null)
                                    .setPositiveButton(R.string.unlink_sensor) { dialogInterface, i ->
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
                                    .create()
                            d.show()
                        }
                        R.id.action_sensor_properties -> {
                            val v = activity.layoutInflater.inflate(R.layout.dialog_sensor_properties, null)
                            val sensorName = v.findViewById<TextView>(R.id.sensor_name_value)
                            val sensorChipId = v.findViewById<TextView>(R.id.sensor_chip_id_value)
                            val sensorPublic = v.findViewById<TextView>(R.id.sensor_public_value)
                            val sensorCreation = v.findViewById<TextView>(R.id.sensor_creation_value)
                            val sensorLat = v.findViewById<TextView>(R.id.sensor_lat_value)
                            val sensorLng = v.findViewById<TextView>(R.id.sensor_lng_value)
                            val sensorAlt = v.findViewById<TextView>(R.id.sensor_alt_value)

                            sensorPublic.isSelected = true
                            sensorCreation.isSelected = true
                            sensorLat.isSelected = true
                            sensorLng.isSelected = true
                            sensorAlt.isSelected = true

                            sensorName.text = sensor.name
                            sensorChipId.text = sensor.chipID

                            val d = AlertDialog.Builder(activity)
                                    .setIcon(R.drawable.info_outline)
                                    .setTitle(R.string.properties)
                                    .setCancelable(true)
                                    .setView(v)
                                    .setPositiveButton(R.string.ok, null)
                                    .create()
                            d.show()

                            Thread(Runnable {
                                if (smu.isInternetAvailable) {
                                    try {
                                        val result = smu.sendRequest(null, object : HashMap<String, String>() {
                                            init {
                                                put("command", "getsensorinfo")
                                                put("chip_id", sensor.chipID)
                                            }
                                        })
                                        if (result.isNotEmpty()) {
                                            val array = JSONArray(result)
                                            val jsonobject = array.getJSONObject(0)

                                            val df = DateFormat.getDateInstance()
                                            val c = Calendar.getInstance()
                                            c.timeInMillis = jsonobject.getLong("creation_date") * 1000
                                            activity.runOnUiThread {
                                                try {
                                                    sensorPublic.text = activity.getString(R.string.yes)
                                                    sensorCreation.text = df.format(c.time)
                                                    sensorLat.text = jsonobject.getDouble("lat").toString().replace(".", ",")
                                                    sensorLng.text = jsonobject.getDouble("lng").toString().replace(".", ",")
                                                    sensorAlt.text = jsonobject.getDouble("alt").toString().replace(".", ",") + " m"
                                                } catch (e: Exception) {
                                                    sensorPublic.text = activity.getString(R.string.no)
                                                    sensorCreation.text = "-"
                                                    sensorLat.text = "-"
                                                    sensorLng.text = "-"
                                                    sensorAlt.text = "-"
                                                }
                                            }
                                        } else {
                                            activity.runOnUiThread {
                                                sensorPublic.text = activity.getString(R.string.no)
                                                sensorCreation.text = "-"
                                                sensorLat.text = "-"
                                                sensorLng.text = "-"
                                                sensorAlt.text = "-"
                                            }
                                        }
                                    } catch (e: Exception) {
                                        activity.runOnUiThread {
                                            sensorPublic.text = activity.getString(R.string.no)
                                            sensorCreation.text = "-"
                                            sensorLat.text = "-"
                                            sensorLng.text = "-"
                                            sensorAlt.text = "-"
                                        }
                                    }

                                } else {
                                    activity.runOnUiThread {
                                        sensorPublic.text = "-"
                                        sensorCreation.text = "-"
                                        sensorLat.text = "-"
                                        sensorLng.text = "-"
                                        sensorAlt.text = "-"
                                    }
                                }
                            }).start()
                        }
                    }
                    true
                }
                popup.show()
            }

            holder.itemMore.visibility = if (su.isSensorExisting(sensor.chipID) && mode == MODE_FAVOURITES) View.GONE else View.VISIBLE
            holder.itemView.findViewById<View>(R.id.item_own_sensor).visibility = if (su.isSensorExisting(sensor.chipID) && mode == MODE_FAVOURITES) View.VISIBLE else View.GONE

            if (mode == MODE_OWN_SENSORS && !su.isSensorInOfflineMode(sensor.chipID)) { // TODO: Remove this part for the next update
                Thread(Runnable {
                    val result = smu.sendRequest(null, object : HashMap<String, String>() {
                        init {
                            put("command", "issensorexisting")
                            put("chip_id", sensor.chipID)
                        }
                    })
                    if (result == "0") {
                        activity.runOnUiThread {
                            holder.itemWarning.visibility = View.VISIBLE
                            holder.itemWarning.setOnClickListener {
                                val i = Intent(activity, AddSensorActivity::class.java)
                                i.putExtra("Mode", AddSensorActivity.MODE_COMPLETE)
                                i.putExtra("Name", sensor.name)
                                i.putExtra("ID", sensor.chipID)
                                i.putExtra("Color", sensor.color)
                                activity.startActivity(i)
                            }
                        }
                    }
                }).start()
            }
        } else if (holder is HeaderViewHolder && shallShowHeader()) {
            holder.headerText.text = activity.getString(R.string.compare_instruction)
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
        Thread(Runnable {
            for (h in viewHolders) {
                try {
                    if (h.itemIcon.isFlipped) {
                        activity.runOnUiThread { h.itemIcon.flip(false) }
                        Thread.sleep(100)
                    }
                } catch (ignored: Exception) {}
            }
        }).start()
    }

    companion object {
        // Constants
        const val MODE_FAVOURITES = 10001
        const val MODE_OWN_SENSORS = 10002
        private const val TYPE_ITEM = 10003
        private const val TYPE_HEADER = 10004
    }
}