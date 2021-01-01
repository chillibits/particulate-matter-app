/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.adapter.viewpager

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.RankingFragment

class RankingAdapter(fm: FragmentManager, l: Lifecycle) : FragmentStateAdapter(fm, l) {
    override fun createFragment(pos: Int) = RankingFragment(pos)
    override fun getItemCount() = 2
}