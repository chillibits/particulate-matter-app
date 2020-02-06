/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.adapter.recyclerview

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chillibits.pmapp.model.ScrapingResult
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.item_scraping_result.view.*

class ScrapeResultAdapter(private val scrapeResults: ArrayList<ScrapingResult>) : RecyclerView.Adapter<ScrapeResultAdapter.ViewHolder>() {

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
            item.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("http://${scrapeResult.ipAddress}")
                context.startActivity(i)
            }
            item_name.text = scrapeResult.name
            item_ip_mac.text = String.format(context.getString(R.string.ip_mac), scrapeResult.ipAddress, scrapeResult.macAddress)
            item_version.text = scrapeResult.firmwareVersion
            item_warning.visibility = if(scrapeResult.sendToUsEnabled) View.GONE else View.VISIBLE
            item_warning.setOnClickListener {

            }
        }
    }
}