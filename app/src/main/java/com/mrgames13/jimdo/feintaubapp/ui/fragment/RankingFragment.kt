/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.network.RANKING_CITY
import com.mrgames13.jimdo.feintaubapp.network.RANKING_COUNTRY
import com.mrgames13.jimdo.feintaubapp.network.loadRanking
import kotlinx.android.synthetic.main.fragment_ranking.view.*
import kotlinx.android.synthetic.main.item_ranking.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RankingFragment(private val mode: Int) : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ranking, container, false).run {
            loadData(this)
            this
        }
    }

    private fun loadData(rv: View) {
        CoroutineScope(Dispatchers.IO).launch {
            val mode = if(mode == 0) RANKING_CITY else RANKING_COUNTRY
            val ranking = loadRanking(requireContext(), mode)
            CoroutineScope(Dispatchers.Main).launch {
                ranking.forEachIndexed {index, item ->
                    val rank = LayoutInflater.from(requireContext()).inflate(R.layout.item_ranking, null, false)
                    rank.itemRank.text = "${index +1}"
                    rank.itemCountryCity.text = if(mode == RANKING_CITY) item.country + ", " + item.city else item.country
                    rank.itemCount.text = item.count.toString()
                    rv.rankingContainer.addView(rank)
                }
                rv.rankingLoading.visibility = GONE
            }
        }
    }
}