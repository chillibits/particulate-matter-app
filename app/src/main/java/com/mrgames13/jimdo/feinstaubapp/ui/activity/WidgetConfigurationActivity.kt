/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.activity

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
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.Sensor
import com.mrgames13.jimdo.feinstaubapp.service.SyncService
import com.mrgames13.jimdo.feinstaubapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.ui.adapter.SelectSensorAdapter
import com.mrgames13.jimdo.feinstaubapp.widget.WidgetProvider
import kotlinx.android.synthetic.main.activity_sensor_selection.*
import java.util.*

class WidgetConfigurationActivity : AppCompatActivity() {

    // Utils packages
    private lateinit var su: StorageUtils

    // Variables as objects
    private lateinit var toolbar: Toolbar
    private lateinit var sensorView: RecyclerView
    private lateinit var sensorViewAdapter: SelectSensorAdapter
    private lateinit var sensors: ArrayList<Sensor>

    // Variables
    private var appWidgetId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_selection)

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar)
        toolbar.setTitle(R.string.widget_select_sensor)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                sensorView.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                insets
            }
        }

        // Load AppWidgetID
        val intent = intent
        val extras = intent.extras
        if (extras != null) appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        // Load Sensors
        su = StorageUtils(this)
        sensors = su.allFavourites
        sensors.addAll(su.allOwnSensors)
        sensors.sort()
        if (sensors.size > 0) {
            // Initialize RecyclerView
            sensorView = findViewById(R.id.sensors)
            sensorViewAdapter = SelectSensorAdapter(this, su, sensors, SelectSensorAdapter.MODE_SELECTION_SINGLE)
            sensorView.setItemViewCacheSize(100)
            sensorView.layoutManager = LinearLayoutManager(this)
            sensorView.adapter = sensorViewAdapter
        } else {
            findViewById<View>(R.id.no_data).visibility = View.VISIBLE
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
            startService(Intent(this, SyncService::class.java))
            su.putInt("Widget_" + sensorViewAdapter.selectedSensor!!.chipID, appWidgetId)
            su.putString("Widget_$appWidgetId", sensorViewAdapter.selectedSensor!!.chipID)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            val views = RemoteViews(packageName, R.layout.widget)
            appWidgetManager.updateAppWidget(appWidgetId, views)

            val updateIntent = Intent(applicationContext, WidgetProvider::class.java)
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent.putExtra(Constants.WIDGET_EXTRA_SENSOR_ID, sensorViewAdapter.selectedSensor!!.chipID)
            sendBroadcast(updateIntent)

            val resultValue = Intent()
            resultValue.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
        }
        finish()
    }
}
