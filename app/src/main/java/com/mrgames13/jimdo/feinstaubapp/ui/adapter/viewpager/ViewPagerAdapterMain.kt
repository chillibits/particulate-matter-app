/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.adapter.viewpager

import android.app.Application
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.AllSensorsFragment
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.FavoritesFragment
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.LocalNetworkFragment
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.OwnSensorsFragment

class ViewPagerAdapterMain(
    application: Application,
    onAdapterEventListener: AllSensorsFragment.OnAdapterEventListener,
    onLocalSearchListener: LocalNetworkFragment.LocalSearchListener,
    fm: FragmentManager,
    l: Lifecycle
) : FragmentStateAdapter(fm, l) {
    private val favoritesFragment = FavoritesFragment()
    val allSensorsFragment = AllSensorsFragment(application, onAdapterEventListener)
    private val ownSensorsFragment = OwnSensorsFragment()
    val localNetworkFragment = LocalNetworkFragment(application, onLocalSearchListener)

    override fun createFragment(pos: Int): Fragment {
        return when(pos) {
            0 -> favoritesFragment
            1 -> allSensorsFragment
            2 -> ownSensorsFragment
            else -> localNetworkFragment
        }
    }

    override fun getItemCount() = 4
}