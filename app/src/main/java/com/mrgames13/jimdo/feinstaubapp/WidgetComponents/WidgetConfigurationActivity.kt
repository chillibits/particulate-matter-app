/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.WidgetComponents

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SelectSensorAdapter
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils
import java.util.*

class WidgetConfigurationActivity : AppCompatActivity() {

    // Utils packages
    private lateinit var su: StorageUtils

    // Variables as objects
    private lateinit var toolbar: Toolbar
    private lateinit var sensor_view: RecyclerView
    private lateinit var sensor_view_adapter: SelectSensorAdapter
    private lateinit var sensors: ArrayList<Sensor>

    // Variables
    private var app_widget_id: Int = 0

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
                sensor_view.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                insets
            }
        }

        // Load AppWidgetID
        val intent = intent
        val extras = intent.extras
        if (extras != null) app_widget_id = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        // Load Sensors
        su = StorageUtils(this)
        sensors = su.allFavourites
        sensors.addAll(su.allOwnSensors)
        Collections.sort(sensors)
        if (sensors.size > 0) {
            // Initialize RecyclerView
            sensor_view = findViewById(R.id.sensors)
            sensor_view_adapter = SelectSensorAdapter(this, su, sensors, SelectSensorAdapter.MODE_SELECTION_SINGLE)
            sensor_view.setItemViewCacheSize(100)
            sensor_view.layoutManager = LinearLayoutManager(this)
            sensor_view.adapter = sensor_view_adapter
        } else {
            findViewById<View>(R.id.no_data).visibility = View.VISIBLE
            val btn_add_favourite = findViewById<Button>(R.id.add_sensor)
            btn_add_favourite.setOnClickListener {
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
        if (sensor_view_adapter.selectedSensor != null) {
            startService(Intent(this, SyncService::class.java))
            su.putInt("Widget_" + sensor_view_adapter.selectedSensor.chipID, app_widget_id)
            su.putString("Widget_$app_widget_id", sensor_view_adapter.selectedSensor.chipID)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            val views = RemoteViews(packageName, R.layout.widget)
            appWidgetManager.updateAppWidget(app_widget_id, views)

            val update_intent = Intent(applicationContext, WidgetProvider::class.java)
            update_intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            update_intent.putExtra(Constants.WIDGET_EXTRA_SENSOR_ID, sensor_view_adapter.selectedSensor.chipID)
            sendBroadcast(update_intent)

            val resultValue = Intent()
            resultValue.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, app_widget_id)
            setResult(Activity.RESULT_OK, resultValue)
        }
        finish()
    }
}
