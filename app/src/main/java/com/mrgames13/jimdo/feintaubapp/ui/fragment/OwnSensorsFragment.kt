/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.fragment

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mrgames13.jimdo.feintaubapp.R
import kotlinx.android.synthetic.main.fragment_own_sensors.view.*

class OwnSensorsFragment : Fragment() {

    // Variables as objects
    private lateinit var rootView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_own_sensors, container, false)

        // Set function to link
        rootView.noDataText.movementMethod = LinkMovementMethod.getInstance()


        return rootView
    }
}