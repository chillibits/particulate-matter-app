/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.item

import android.content.Context
import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.db.SensorDbo
import kotlinx.android.synthetic.main.item_sensor.view.*

open class SensorItem(
    private val context: Context,
    private val sensor: SensorDbo
) : AbstractItem<SensorItem.ViewHolder>() {

    override val type = R.id.itemContainer
    override val layoutRes = R.layout.item_sensor

    override fun getViewHolder(v: View) = ViewHolder(context, v)

    class ViewHolder(
        private val context: Context,
        private val view: View
    ) : FastAdapter.ViewHolder<SensorItem>(view) {
        override fun bindView(item: SensorItem, payloads: List<Any>) {
            val sensor = item.sensor
            view.itemName.text = sensor.name
            view.itemDetails.text = String.format(context.getString(R.string.chip_id), sensor.chipId)
        }

        override fun unbindView(item: SensorItem) {
            view.itemName.text = null
            view.itemDetails.text = null
        }
    }
}