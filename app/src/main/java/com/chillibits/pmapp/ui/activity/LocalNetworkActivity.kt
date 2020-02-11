/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chillibits.pmapp.model.ScrapingResult
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.tasks.SensorIPSearchTask
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.tool.Tools
import com.chillibits.pmapp.ui.adapter.recyclerview.ScrapeResultAdapter
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.activity_local_network.*
import kotlinx.android.synthetic.main.toolbar.*

class LocalNetworkActivity : AppCompatActivity() {

    // Constants
    val REQ_ADD_OWN_SENSOR = 10001

    // Utils packages
    private lateinit var su: StorageUtils
    private lateinit var smu: ServerMessagingUtils

    // Variables as objects
    private lateinit var searchTask: SensorIPSearchTask

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_network)

        // Initialize StorageUtils
        su = StorageUtils(this)
        smu = ServerMessagingUtils(this)

        // Initialize toolbar
        toolbar.title = getString(R.string.sensors_local_network)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Apply window insets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                button_bar.setPadding(Tools.dpToPx(10), Tools.dpToPx(10), Tools.dpToPx(10), insets.systemWindowInsetBottom)
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets.consumeSystemWindowInsets()
            }
        }

        // Initializing UI
        scraping_results.layoutManager = LinearLayoutManager(this@LocalNetworkActivity)
        retry_button.setOnClickListener { search() }
        refresh.setOnRefreshListener { search() }
        reload.setOnClickListener { search() }
        refresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark)

        // Start search process
        search()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_OK)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel search process
        Log.i(Constants.TAG, "Search cancelled.")
        if(searchTask.status == AsyncTask.Status.RUNNING) searchTask.cancel(true)
    }

    private fun search() {
        if(smu.isInternetAvailable && smu.isWifi) {
            scraping_results.adapter = ScrapeResultAdapter(this, su, emptyList())
            retry_container.visibility = View.GONE
            loading_text.text = getString(R.string.searching_for_sensors)
            loading_container.visibility = View.VISIBLE
            reload.isEnabled = false
            reloading.visibility = View.VISIBLE

            // Setup searching task
            searchTask = SensorIPSearchTask(this, object: SensorIPSearchTask.OnSearchEventListener {
                override fun onProgressUpdate(progress: Int) {
                    loading_text.text = getString(R.string.searching_for_sensors) + " ($progress %)"
                }

                override fun onSensorFound(sensor: ScrapingResult?) {}

                override fun onSearchFinished(sensorList: ArrayList<ScrapingResult>) {
                    scraping_results.adapter = ScrapeResultAdapter(this@LocalNetworkActivity, su, sensorList)
                    loading_container.visibility = View.GONE
                    refresh.isRefreshing = false
                    reload.isEnabled = true
                    reloading.visibility = View.GONE
                }

                override fun onSearchFailed() {
                    loading_container.visibility = View.GONE
                    retry_container.visibility = View.VISIBLE
                    refresh.isRefreshing = false
                    reload.isEnabled = true
                    reloading.visibility = View.GONE
                }
            }, 0)
            searchTask.execute()
        } else {
            Toast.makeText(this, R.string.only_with_wifi, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(Constants.TAG, "Test")
        if(requestCode == REQ_ADD_OWN_SENSOR && resultCode == Activity.RESULT_OK) search()
    }
}