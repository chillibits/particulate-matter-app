/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chillibits.pmapp.model.Sensor
import com.chillibits.pmapp.service.SyncJobService
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.ui.adapter.recyclerview.SelectSensorAdapter
import com.chillibits.pmapp.widget.WidgetProviderLarge
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.activity_widget_configuration.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class WidgetConfigurationActivity : AppCompatActivity() {

    // Utils packages
    private lateinit var su: StorageUtils

    // Variables as objects
    private lateinit var sensorViewAdapter: SelectSensorAdapter
    private lateinit var sensors: ArrayList<Sensor>

    // Variables
    private var appWidgetId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget_configuration)

        // Initialize toolbar
        toolbar.setTitle(R.string.widget_select_sensor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                sensor_view.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                insets
            }
        }

        // Load AppWidgetID
        if (intent.extras != null) appWidgetId = intent.extras!!.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        // Load Sensors
        su = StorageUtils(this)
        sensors = su.allFavourites
        sensors.addAll(su.allOwnSensors)
        sensors.sort()
        if (sensors.size > 0) {
            // Initialize RecyclerView
            sensorViewAdapter = SelectSensorAdapter(
                su,
                sensors,
                SelectSensorAdapter.MODE_SELECTION_SINGLE
            )
            sensor_view.run {
                setItemViewCacheSize(100)
                layoutManager = LinearLayoutManager(this@WidgetConfigurationActivity)
                adapter = sensorViewAdapter
            }
        } else {
            no_data.visibility = View.VISIBLE
            add_sensor.setOnClickListener {
                startActivity(Intent(this@WidgetConfigurationActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_select_sensor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_done) {
            finishConfiguration()
        } else if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun finishConfiguration() {
        if(sensorViewAdapter.selectedSensor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, SyncJobService::class.java))
            } else {
                startService(Intent(this, SyncJobService::class.java))
            }
            su.putInt("Widget_" + sensorViewAdapter.selectedSensor!!.chipID, appWidgetId)
            su.putString("Widget_$appWidgetId", sensorViewAdapter.selectedSensor!!.chipID)

            val widgetManager = AppWidgetManager.getInstance(this)
            val views = RemoteViews(packageName, R.layout.widget_large)
            widgetManager.updateAppWidget(appWidgetId, views)

            val update = Intent(applicationContext, WidgetProviderLarge::class.java)
            update.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            update.putExtra(Constants.WIDGET_EXTRA_SENSOR_ID, sensorViewAdapter.selectedSensor!!.chipID)
            sendBroadcast(update)

            val result = Intent()
            result.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, result)
        }
        finish()
    }
}
