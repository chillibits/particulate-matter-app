/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.network.RANKING_CITY
import com.mrgames13.jimdo.feinstaubapp.network.RANKING_COUNTRY
import com.mrgames13.jimdo.feinstaubapp.network.loadRanking
import com.mrgames13.jimdo.feinstaubapp.ui.item.RankingItem
import kotlinx.android.synthetic.main.fragment_ranking.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RankingFragment(private val mode: Int) : Fragment() {

    // Variables as objects
    private lateinit var adapter: ItemAdapter<RankingItem>

    // Default constructor has to be implemented, otherwise the app crashes on configuration change
    constructor() : this(0)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ranking, container, false).run {
            // Initialize recycler view
            rankingList.layoutManager = LinearLayoutManager(context)
            rankingList.setHasFixedSize(true)
            adapter = ItemAdapter()
            rankingList.adapter = FastAdapter.with(adapter)

            // Load ranking
            loadData(this)
            this
        }
    }

    private fun loadData(view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                view.rankingLoading.visibility = View.GONE
                view.rankingLoading.visibility = View.VISIBLE
            }
            val mode = if(mode == 0) RANKING_CITY else RANKING_COUNTRY
            val rankingItems = ArrayList<RankingItem>()
            loadRanking(requireContext(), mode).forEachIndexed { index, item ->
                val rankingItem = RankingItem()
                rankingItem.rank = index +1
                rankingItem.countryCity = if(mode == RANKING_CITY) item.country + ", " + item.city else item.country
                rankingItem.sensorCount = item.count
                rankingItems.add(rankingItem)
            }
            withContext(Dispatchers.Main) {
                adapter.add(rankingItems)
                view.rankingLoading.visibility = View.GONE
                if(rankingItems.size == 0) view.connectionFailed.visibility = View.VISIBLE
            }
        }
    }
}