/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.adapter.recyclerview

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.chillibits.pmapp.model.Sensor
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.network.isSensorExisting
import com.chillibits.pmapp.tasks.SensorIPSearchTask
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.ui.activity.AddSensorActivity
import com.chillibits.pmapp.ui.activity.MainActivity
import com.chillibits.pmapp.ui.activity.SensorActivity
import com.chillibits.pmapp.ui.view.ProgressDialog
import com.chillibits.pmapp.ui.view.showSensorInfoWindow
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.item_sensor.view.*
import kotlinx.android.synthetic.main.sensor_view_header.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SensorAdapter(private val activity: MainActivity, private val sensors: ArrayList<Sensor>, private val su: StorageUtils, private val smu: ServerMessagingUtils, private val mode: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Variables as objects
    val selectedSensors = ArrayList<Sensor>()
    private val viewHolders = ArrayList<ViewHolder>()

    // Variables
    private var clickStart: Long = 0

    inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)

    private inner class HeaderViewHolder

    internal constructor(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun getItemViewType(pos: Int) = if (shallShowHeader())
        if (pos == 0) TYPE_HEADER else TYPE_ITEM
    else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.sensor_view_header, parent, false))
        } else {
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        if (holder is ViewHolder) {
            viewHolders.add(holder)
            // Fill in data
            val sensor = sensors[if (shallShowHeader()) pos - 1 else pos]

            holder.itemView.run {
                item_icon.frontLayout.background.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(sensor.color, BlendModeCompat.SRC_IN)
                item_name.text = sensor.name
                item_id.text = activity.getString(R.string.chip_id) + " " + sensor.chipID

                setOnClickListener {
                    if (System.currentTimeMillis() > clickStart + 1000) {
                        if (selectedSensors.size > 0) {
                            holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped)
                        } else {
                            val i = Intent(activity, SensorActivity::class.java)
                            i.run {
                                putExtra("Name", sensor.name)
                                putExtra("ID", sensor.chipID)
                                putExtra("Color", sensor.color)
                            }
                            activity.startActivity(i)
                            clickStart = System.currentTimeMillis()
                        }
                    }
                }
                setOnLongClickListener {
                    holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped)
                    true
                }
                item_icon.setOnClickListener { holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped) }
                item_icon.setOnLongClickListener {
                    holder.itemView.item_icon.flip(!holder.itemView.item_icon.isFlipped)
                    true
                }
                item_icon.setOnFlippingListener { _, checked ->
                    if (checked) selectedSensors.add(sensor)
                    if (!checked) selectedSensors.remove(sensor)
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(activity, if (checked) R.color.color_selection else R.color.transparent))
                    activity.updateSelectionMode()
                }

                item_more.setOnClickListener {
                    val popup = PopupMenu(activity, holder.itemView.item_more)
                    popup.inflate(R.menu.menu_sensor_more)
                    if(mode == MODE_FAVOURITES) {
                        popup.menu.getItem(0).isVisible = false
                    } else if(!smu.isWifi) {
                        popup.menu.getItem(0).isEnabled = false
                    }
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_find_locally -> {
                                findSensorLocally(sensor.chipID.toInt())
                            }
                            R.id.action_sensor_edit -> {
                                val i = Intent(activity, AddSensorActivity::class.java)
                                i.run {
                                    putExtra("Mode", AddSensorActivity.MODE_EDIT)
                                    putExtra("Name", sensor.name)
                                    putExtra("ID", sensor.chipID)
                                    putExtra("Color", sensor.color)
                                }
                                if (mode == MODE_FAVOURITES) i.putExtra("Target", AddSensorActivity.TARGET_FAVOURITE)
                                activity.startActivity(i)
                            }
                            R.id.action_sensor_unlink -> {
                                AlertDialog.Builder(activity)
                                    .setIcon(R.drawable.delete_red)
                                    .setTitle(R.string.unlink_sensor)
                                    .setMessage(String.format(activity.getString(R.string.really_unlink_sensor), sensor.name))
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
                                showSensorInfoWindow(activity, smu, sensor.chipID, sensor.name)
                            }
                        }
                        true
                    }
                    popup.show()
                }

                item_more.visibility = if (su.isSensorExisting(sensor.chipID) && mode == MODE_FAVOURITES) View.GONE else View.VISIBLE
                item_own_sensor.visibility = if (su.isSensorExisting(sensor.chipID) && mode == MODE_FAVOURITES) View.VISIBLE else View.GONE
            }

            if (mode == MODE_OWN_SENSORS && !su.isSensorInOfflineMode(sensor.chipID)) { // TODO: Remove this part for the next update
                CoroutineScope(Dispatchers.IO).launch {
                    if (!isSensorExisting(activity, sensor.chipID)) {
                        CoroutineScope(Dispatchers.Main).launch {
                            holder.itemView.item_warning.visibility = View.VISIBLE
                            holder.itemView.item_warning.setOnClickListener {
                                val i = Intent(activity, AddSensorActivity::class.java)
                                i.run {
                                    putExtra("Mode", AddSensorActivity.MODE_COMPLETE)
                                    putExtra("Name", sensor.name)
                                    putExtra("ID", sensor.chipID)
                                    putExtra("Color", sensor.color)
                                }
                                activity.startActivity(i)
                            }
                        }
                    }
                }
            }
        } else if (holder is HeaderViewHolder && shallShowHeader()) {
            holder.itemView.header_text.text = activity.getString(R.string.compare_instruction)
            holder.itemView.header_close.setOnClickListener {
                su.putBoolean("SensorViewHeader", false)
                activity.refresh()
            }
        }
    }

    override fun getItemCount() = if (shallShowHeader()) sensors.size + 1 else sensors.size
    private fun shallShowHeader() = su.getBoolean("SensorViewHeader", true)

    fun deselectAllSensors() {
        CoroutineScope(Dispatchers.Default).launch {
            viewHolders.forEach {
                if (it.itemView.item_icon.isFlipped) {
                    CoroutineScope(Dispatchers.Main).launch { it.itemView.item_icon.flip(false) }
                    delay(100)
                }
            }
        }
    }

    private fun findSensorLocally(chipId: Int) {
        val pd = ProgressDialog(activity)
        pd.setMessage(R.string.searching_ip_address)
        pd.show()

        val searchTask = SensorIPSearchTask(activity, object: SensorIPSearchTask.OnSearchEventListener{
            override fun onSensorFound(ipAddress: String) {
                pd.dismiss()
            }

            override fun onSearchFinished(sensorList: ArrayList<Sensor>) {}

            override fun onSearchFailed() {
                pd.dismiss()
                Toast.makeText(activity, R.string.error_try_again, Toast.LENGTH_SHORT).show()
            }
        }, chipId)
        searchTask.execute()
    }

    companion object {
        // Constants
        const val MODE_FAVOURITES = 10001
        const val MODE_OWN_SENSORS = 10002
        private const val TYPE_ITEM = 10003
        private const val TYPE_HEADER = 10004
    }
}