/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.toolbar.*

class LocalNetworkActivity : AppCompatActivity() {

    // Variables as objects

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_network)

        toolbar.title = getString(R.string.find_sensor_locally)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


    }
}