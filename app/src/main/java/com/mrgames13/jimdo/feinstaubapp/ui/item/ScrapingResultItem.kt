/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.item

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.db.ScrapingResultDbo
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.ui.activity.AddSensorActivity
import com.mrgames13.jimdo.feinstaubapp.ui.activity.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.item_scraping_result.view.*

class ScrapingResultItem(
    private val viewModel: MainViewModel,
    private val scrapingResult: ScrapingResultDbo
) : AbstractItem<ScrapingResultItem.ViewHolder>() {
    override val layoutRes = R.layout.item_scraping_result
    override val type = R.id.itemContainer

    override fun getViewHolder(v: View) = ViewHolder(v, viewModel)

    class ViewHolder(
        private val view: View,
        private val viewModel: MainViewModel
    ) : FastAdapter.ViewHolder<ScrapingResultItem>(view) {
        override fun bindView(item: ScrapingResultItem, payloads: List<Any>) {
            val sr = item.scrapingResult
            view.run {
                // Set contents
                itemName.text = sr.name
                itemIpMac.text = String.format(context.getString(R.string.ip_mac), sr.ipAddress, sr.macAddress)
                val firmwareVersion = context.getString(R.string.firmware_version_) + " " + sr.firmwareVersion
                itemVersion.text = firmwareVersion
                itemWarning.visibility = if(sr.sendToUsEnabled) View.GONE else View.VISIBLE
                // Set click listeners
                itemWarning.setOnClickListener { showWarningDialog(context) }
                itemButtonOpenConfig.setOnClickListener {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("http://${sr.ipAddress}")
                    })
                }

                // Check if sensor is already linked
                if(viewModel.sensors.value?.find { s -> s.chipId == sr.chipID } !== null) {
                    itemButtonShowData.setOnClickListener { openDataActivity(context, sr) }
                    itemButtonShowData.visibility = View.VISIBLE
                    itemButtonAdd.visibility = View.GONE
                } else {
                    itemButtonAdd.setOnClickListener { openAddSensorActivity(context, sr) }
                    itemButtonAdd.visibility = View.VISIBLE
                    itemButtonShowData.visibility = View.GONE
                }
            }
        }

        override fun unbindView(item: ScrapingResultItem) {
            view.itemName.text = null
            view.itemIpMac.text = null
            view.itemVersion.text = null
        }

        private fun showWarningDialog(context: Context) {
            AlertDialog.Builder(context)
                .setIcon(R.drawable.warning_orange)
                .setTitle(R.string.app_name)
                .setMessage(R.string.warning_dialog_text)
                .setPositiveButton(R.string.ok, null)
                .show()
        }

        private fun openDataActivity(context: Context, sr: ScrapingResultDbo) {
            context.startActivity(Intent(context, SensorActivity::class.java).apply {
                putExtra(Constants.EXTRA_SENSOR_DATA_NAME, sr.name)
                putExtra(Constants.EXTRA_SENSOR_DATA_ID, sr.chipID)
                putExtra(Constants.EXTRA_SENSOR_DATA_COLOR, ContextCompat.getColor(context, R.color.colorPrimary))
            })
        }

        private fun openAddSensorActivity(context: Context, sr: ScrapingResultDbo) {
            context.startActivity(Intent(context, AddSensorActivity::class.java).apply {
                putExtra(Constants.EXTRA_ADD_SENSOR_MODE, Constants.CREATION_MODE_COMPLETE)
                putExtra(Constants.EXTRA_ADD_SENSOR_NAME, sr.name)
                putExtra(Constants.EXTRA_ADD_SENSOR_ID, sr.chipID)
            })
        }
    }
}