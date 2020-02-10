/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.adapter.recyclerview

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chillibits.pmapp.model.ScrapingResult
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.ui.activity.AddSensorActivity
import com.chillibits.pmapp.ui.activity.AddSensorActivity.Companion.MODE_COMPLETE
import com.chillibits.pmapp.ui.activity.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.item_scraping_result.view.*

class ScrapeResultAdapter(private val su: StorageUtils, private val scrapeResults: List<ScrapingResult>) : RecyclerView.Adapter<ScrapeResultAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_scraping_result, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount() = scrapeResults.size

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        // Fill in data
        val scrapeResult = scrapeResults[pos]

        h.itemView.run {
            item_name.text = scrapeResult.name
            item_ip_mac.text = String.format(context.getString(R.string.ip_mac), scrapeResult.ipAddress, scrapeResult.macAddress)
            item_version.text = scrapeResult.firmwareVersion
            item_warning.visibility = if(scrapeResult.sendToUsEnabled) View.GONE else View.VISIBLE
            item_warning.setOnClickListener {
                AlertDialog.Builder(context)
                    .setIcon(R.drawable.warning)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.warning_dialog_text)
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
            item_button_open_config.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("http://${scrapeResult.ipAddress}")
                context.startActivity(i)
            }
            val alreadyLinked = su.isSensorLinked(scrapeResult.chipID)
            if(alreadyLinked) {
                item_button_show_data.setOnClickListener {
                    val i = Intent(context, SensorActivity::class.java)
                    i.run {
                        putExtra("Name", scrapeResult.name)
                        putExtra("ID", scrapeResult.chipID)
                        putExtra("Color", ContextCompat.getColor(context, R.color.colorPrimary))
                    }
                    context.startActivity(i)
                }
                item_button_show_data.visibility = View.VISIBLE
                item_button_add.visibility = View.GONE
            } else {
                item_button_add.setOnClickListener {
                    val i = Intent(context, AddSensorActivity::class.java)
                    i.run {
                        putExtra("Mode", MODE_COMPLETE)
                        putExtra("Name", scrapeResult.name)
                        putExtra("ID", scrapeResult.chipID)
                    }
                    context.startActivity(i)
                }
                item_button_add.isEnabled = scrapeResult.sendToUsEnabled
            }
        }
    }
}