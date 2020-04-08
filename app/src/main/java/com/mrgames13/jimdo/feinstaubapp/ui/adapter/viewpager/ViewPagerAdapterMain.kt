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
    private val application: Application,
    fm: FragmentManager,
    l: Lifecycle,
    private val listener: AllSensorsFragment.OnAdapterEventListener
) : FragmentStateAdapter(fm, l) {
    lateinit var favoritesFragment: FavoritesFragment
    lateinit var allSensorsFragment: AllSensorsFragment
    lateinit var ownSensorsFragment: OwnSensorsFragment
    lateinit var localNetworkFragment: LocalNetworkFragment

    override fun createFragment(pos: Int): Fragment {
        return when(pos) {
            0 -> {
                favoritesFragment = FavoritesFragment()
                favoritesFragment
            }
            1 -> {
                allSensorsFragment = AllSensorsFragment(application, listener)
                allSensorsFragment
            }
            2 -> {
                ownSensorsFragment = OwnSensorsFragment()
                ownSensorsFragment
            }
            else -> {
                localNetworkFragment = LocalNetworkFragment()
                localNetworkFragment
            }
        }
    }

    override fun getItemCount() = 4
}