/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.adapter.viewpager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.chillibits.pmapp.model.Sensor
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.ui.activity.SensorActivity
import com.chillibits.pmapp.ui.fragment.SensorDataFragment
import com.chillibits.pmapp.ui.fragment.SensorDiagramFragment

class ViewPagerAdapterSensorNew(manager: FragmentManager, activity: SensorActivity, su: StorageUtils, sensor: Sensor) : FragmentStatePagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    // Variables as objects

    override fun getItem(pos: Int): Fragment {
        return if (pos == 1) SensorDiagramFragment() else SensorDataFragment()
    }

    override fun getCount() = 2
}