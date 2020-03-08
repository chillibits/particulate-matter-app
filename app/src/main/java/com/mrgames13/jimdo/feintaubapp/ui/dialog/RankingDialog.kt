/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import com.google.android.material.tabs.TabLayoutMediator
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.ui.adapter.viewpager.RankingAdapter
import kotlinx.android.synthetic.main.dialog_ranking.view.*

fun showRankingDialog(context: Context, fm: FragmentManager, lifecycle: Lifecycle) {
    val view = LayoutInflater.from(context).inflate(R.layout.dialog_ranking, null, false)
    val d = AlertDialog.Builder(context)
        //.setTitle(R.string.sensor_highscore)
        .setView(view)
        .setPositiveButton(R.string.ok, null)
        .show()

    view.rankingViewPager.adapter = RankingAdapter(fm, lifecycle, object : RankingAdapter.OnRankingLoadingEventListener {
        override fun onPageLoaded() {
            // Refresh dialog view after loading
            d.setView(view)
        }
    })
    TabLayoutMediator(view.rankingTabLayout, view.rankingViewPager, TabLayoutMediator.TabConfigurationStrategy { tab, position ->
        tab.text = context.getString(if(position == 0) R.string.cities else R.string.countries)
    }).attach()
}