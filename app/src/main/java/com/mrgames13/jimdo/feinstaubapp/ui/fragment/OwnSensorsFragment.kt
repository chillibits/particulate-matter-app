/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.db.Sensor
import kotlinx.android.synthetic.main.fragment_own_sensors.view.*

class OwnSensorsFragment(
    private val sensors: LiveData<List<Sensor>>?
) : Fragment() {

    // Variables as objects
    private lateinit var rootView: View

    // Default constructor has to be implemented, otherwise the app crashes on configuration change
    constructor() : this(null)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_own_sensors, container, false)

        // Set function to link
        rootView.noDataText.movementMethod = LinkMovementMethod.getInstance()


        return rootView
    }
}