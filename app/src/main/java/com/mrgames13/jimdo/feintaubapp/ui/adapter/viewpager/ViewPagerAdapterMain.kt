/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.mrgames13.jimdo.feintaubapp.ui.fragment.AllSensorsFragment
import com.mrgames13.jimdo.feintaubapp.ui.fragment.FavoritesFragment
import com.mrgames13.jimdo.feintaubapp.ui.fragment.LocalNetworkFragment
import com.mrgames13.jimdo.feintaubapp.ui.fragment.OwnSensorsFragment

class ViewPagerAdapterMain(manager: FragmentManager) : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(pos: Int): Fragment {
        return when(pos) {
            0 -> FavoritesFragment()
            1 -> AllSensorsFragment()
            2 -> OwnSensorsFragment()
            else -> LocalNetworkFragment()
        }
    }

    override fun getCount() = 4
}