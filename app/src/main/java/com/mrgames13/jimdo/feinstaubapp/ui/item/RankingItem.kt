/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.item

import android.view.View
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.item_ranking.view.*

class RankingItem : AbstractItem<RankingItem.ViewHolder>() {
    var rank: Int? = null
    var countryCity: String? = null
    var sensorCount: Int? = null

    override val type = R.id.itemContainer
    override val layoutRes = R.layout.item_ranking

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(private val view: View) : FastAdapter.ViewHolder<RankingItem>(view) {
        override fun bindView(item: RankingItem, payloads: List<Any>) {
            view.itemRank.text = item.rank.toString()
            view.itemCountryCity.text = item.countryCity
            view.itemCount.text = item.sensorCount.toString()
        }

        override fun unbindView(item: RankingItem) {
            view.itemRank.text = null
            view.itemCountryCity.text = null
            view.itemCount.text = null
        }
    }
}