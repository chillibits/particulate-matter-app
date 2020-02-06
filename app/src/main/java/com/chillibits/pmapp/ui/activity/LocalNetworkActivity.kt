/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.app.Activity
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chillibits.pmapp.model.ScrapingResult
import com.chillibits.pmapp.tasks.SensorIPSearchTask
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.ui.adapter.recyclerview.ScrapeResultAdapter
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.activity_local_network.*
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

        scraping_results.layoutManager = LinearLayoutManager(this@LocalNetworkActivity)

        retry_button.setOnClickListener { refresh() }
        refresh.setOnRefreshListener { refresh() }

        refresh()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_OK)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Start search process
        Log.i(Constants.TAG, "Started searching ...")
        if(searchTask.status != AsyncTask.Status.RUNNING) refresh()
    }

    override fun onPause() {
        super.onPause()
        // Cancel search process
        Log.i(Constants.TAG, "Search cancelled.")
        if(searchTask.status == AsyncTask.Status.RUNNING) searchTask.cancel(true)
    }

    private fun refresh() {
        retry_container.visibility = View.GONE
        refresh.isRefreshing = true
        loading_container.visibility = View.VISIBLE

        // Setup searching task
        searchTask = SensorIPSearchTask(this, object: SensorIPSearchTask.OnSearchEventListener {
            override fun onProgressUpdate(progress: Int) {
                loading_text.text = getString(R.string.searching_ip_address) + " ($progress %)"
            }

            override fun onSensorFound(sensor: ScrapingResult?) {}

            override fun onSearchFinished(sensorList: ArrayList<ScrapingResult>) {
                scraping_results.adapter = ScrapeResultAdapter(sensorList)
                loading_container.visibility = View.GONE
                refresh.isRefreshing = false
            }

            override fun onSearchFailed() {
                scraping_results.adapter = ScrapeResultAdapter(ArrayList())
                loading_container.visibility = View.GONE
                retry_container.visibility = View.VISIBLE
                refresh.isRefreshing = false
            }
        }, 0)
        searchTask.execute()
    }
}