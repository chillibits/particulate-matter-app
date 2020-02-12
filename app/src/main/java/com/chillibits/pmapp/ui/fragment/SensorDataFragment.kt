/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.tab_data.view.*

class SensorDataFragment : Fragment() {

    // Variables as objects
    private lateinit var contentView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        contentView = LayoutInflater.from(container?.context).inflate(R.layout.tab_data, container, false)

        // Initialize RecyclerView
        val rv = contentView.data
        rv.setHasFixedSize(true)

        return contentView
    }

}