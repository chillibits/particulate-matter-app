/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.item

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.item_sensor.view.*

open class SensorItem : AbstractItem<SensorItem.ViewHolder>() {
    var name: String? = null
    var description: String? = null

    override val type = R.id.itemContainer
    override val layoutRes = R.layout.item_sensor

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(private val view: View) : FastAdapter.ViewHolder<SensorItem>(view) {
        override fun bindView(item: SensorItem, payloads: List<Any>) {
            view.itemName.text = item.name
        }

        override fun unbindView(item: SensorItem) {
            view.itemName.text = null
        }
    }
}