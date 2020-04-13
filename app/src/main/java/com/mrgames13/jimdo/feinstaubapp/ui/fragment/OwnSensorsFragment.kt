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
import androidx.lifecycle.ViewModelProvider
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_own_sensors.view.*

class OwnSensorsFragment : Fragment() {

    // Variables as objects
    private lateinit var rootView: View
    private lateinit var viewModel: MainViewModel

    // Default constructor has to be implemented, otherwise the app crashes on configuration change
    //constructor() : this()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_own_sensors, container, false)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(activity!!.application)).get(
            MainViewModel::class.java)

        // Set function to link
        rootView.noDataText.movementMethod = LinkMovementMethod.getInstance()


        return rootView
    }
}