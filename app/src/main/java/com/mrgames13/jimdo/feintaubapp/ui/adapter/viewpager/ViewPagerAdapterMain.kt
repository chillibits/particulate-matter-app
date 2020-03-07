/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mrgames13.jimdo.feintaubapp.ui.fragment.AllSensorsFragment
import com.mrgames13.jimdo.feintaubapp.ui.fragment.FavoritesFragment
import com.mrgames13.jimdo.feintaubapp.ui.fragment.LocalNetworkFragment
import com.mrgames13.jimdo.feintaubapp.ui.fragment.OwnSensorsFragment

class ViewPagerAdapterMain(
    fm: FragmentManager,
    l: Lifecycle,
    private val listener: AllSensorsFragment.OnAdapterEventListener
) : FragmentStateAdapter(fm, l) {
    override fun createFragment(pos: Int): Fragment {
        return when(pos) {
            0 -> FavoritesFragment()
            1 -> AllSensorsFragment(listener)
            2 -> OwnSensorsFragment()
            else -> LocalNetworkFragment()
        }
    }

    override fun getItemCount() = 4
}