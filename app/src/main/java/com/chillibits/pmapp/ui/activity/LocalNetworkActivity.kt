/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.chillibits.pmapp.model.ScrapingResult
import com.chillibits.pmapp.tasks.SensorIPSearchTask
import com.chillibits.pmapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.toolbar.*

class LocalNetworkActivity : AppCompatActivity() {

    // Variables as objects
    private lateinit var searchTask: SensorIPSearchTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_network)

        toolbar.title = getString(R.string.find_sensor_locally)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets.consumeSystemWindowInsets()
            }
        }

        searchTask = SensorIPSearchTask(this, object: SensorIPSearchTask.OnSearchEventListener {
            override fun onSensorFound(sensor: ScrapingResult) {}

            override fun onSearchFinished(sensorList: ArrayList<ScrapingResult>) {

            }

            override fun onSearchFailed() {

            }
        }, 0)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_OK)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        // Start search process
        Log.i(Constants.TAG, "Started searching ...")
        searchTask.execute()
    }

    override fun onStop() {
        super.onStop()
        // Cancel search process
        Log.i(Constants.TAG, "Search cancelled.")
        searchTask.cancel(true)
    }
}