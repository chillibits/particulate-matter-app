/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools
import java.text.SimpleDateFormat
import java.util.*

class DataAdapter : RecyclerView.Adapter<DataAdapter.ViewHolder>() {

    // Variables as objects
    private val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val holders = ArrayList<ViewHolder>()

    // Variables
    private var showGpsData: Boolean = false

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Variables as objects
        internal var itemTime: TextView = itemView.findViewById(R.id.item_time)
        internal var itemP1: TextView = itemView.findViewById(R.id.item_p1)
        internal var itemP2: TextView = itemView.findViewById(R.id.item_p2)
        internal var itemTemp: TextView = itemView.findViewById(R.id.item_temp)
        internal var itemHumidity: TextView = itemView.findViewById(R.id.item_humidity)
        internal var itemPressure: TextView = itemView.findViewById(R.id.item_pressure)
        internal var itemLat: TextView = itemView.findViewById(R.id.item_lat)
        internal var itemLng: TextView = itemView.findViewById(R.id.item_lng)
        internal var itemAlt: TextView = itemView.findViewById(R.id.item_alt)
        internal var itemGpsContainer: LinearLayout = itemView.findViewById(R.id.item_gps_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {
        // Fill in data
        try {
            val record = SensorActivity.records[pos]
            holder.itemTime.text = sdf.format(record.dateTime)
            holder.itemP1.text = Tools.round(record.p1, 1).toString().replace(".", ",") + " µg/m³"
            holder.itemP2.text = Tools.round(record.p2, 1).toString().replace(".", ",") + " µg/m³"
            holder.itemTemp.text = record.temp.toString().replace(".", ",") + " °C"
            holder.itemHumidity.text = record.humidity.toString().replace(".", ",") + " %"
            holder.itemPressure.text = Tools.round(record.pressure, 2).toString().replace(".", ",") + " hPa"
            holder.itemLat.text = Tools.round(record.lat, 3).toString() + " °"
            holder.itemLng.text = Tools.round(record.lng, 3).toString() + " °"
            holder.itemAlt.text = Tools.round(record.alt, 1).toString() + " m"
            holder.itemGpsContainer.visibility = if (showGpsData) View.VISIBLE else View.GONE
            this.holders.add(holder)
        } catch (ignored: Exception) {}
    }

    fun showGPSData(show: Boolean) {
        showGpsData = show
        for (h in holders) h.itemGpsContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        return if (SensorActivity.records == null) 0 else SensorActivity.records.size
    }
}