/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.adapter.recyclerview


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chillibits.pmapp.tool.Tools
import com.chillibits.pmapp.ui.activity.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.item_data.view.*
import java.text.SimpleDateFormat
import java.util.*

class DataAdapter : RecyclerView.Adapter<DataAdapter.ViewHolder>() {

    // Variables as objects
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val holders = ArrayList<ViewHolder>()

    // Variables
    private var showGpsData: Boolean = false

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        // Fill in data
        try {
            val record = SensorActivity.records[pos]
            holder.itemView.run {
                item_time.text = sdf.format(record.dateTime)
                item_p1.text = Tools.round(record.p1, 1).toString().replace(".", ",") + " µg/m³"
                item_p2.text = Tools.round(record.p2, 1).toString().replace(".", ",") + " µg/m³"
                item_temp.text = record.temp.toString().replace(".", ",") + " °C"
                item_humidity.text = record.humidity.toString().replace(".", ",") + " %"
                item_pressure.text = Tools.round(record.pressure, 1).toString().replace(".", ",") + " hPa"
                item_lat.text = Tools.round(record.lat, 3).toString() + " °"
                item_lng.text = Tools.round(record.lng, 3).toString() + " °"
                item_alt.text = Tools.round(record.alt, 1).toString() + " m"
                item_gps_container.visibility = if (showGpsData) View.VISIBLE else View.GONE
            }
            this.holders.add(holder)
        } catch (ignored: Exception) {}
    }

    fun showGPSData(show: Boolean) {
        showGpsData = show
        for (h in holders) h.itemView.item_gps_container.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return SensorActivity.records.size
    }
}