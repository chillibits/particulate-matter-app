/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.activity

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.DataRecord
import com.mrgames13.jimdo.feinstaubapp.model.Sensor
import com.mrgames13.jimdo.feinstaubapp.network.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.network.isSensorDataExisting
import com.mrgames13.jimdo.feinstaubapp.network.loadDataRecords
import com.mrgames13.jimdo.feinstaubapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.tool.NotificationUtils
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.tool.Tools
import com.mrgames13.jimdo.feinstaubapp.ui.adapter.viewpager.ViewPagerAdapterSensor
import com.mrgames13.jimdo.feinstaubapp.widget.WidgetProvider
import kotlinx.android.synthetic.main.activity_main.view_pager
import kotlinx.android.synthetic.main.activity_sensor.*
import kotlinx.android.synthetic.main.activity_sensor.container
import kotlinx.android.synthetic.main.dialog_share.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class SensorActivity : AppCompatActivity(), ViewPagerAdapterSensor.OnFragmentsLoadedListener {

    // Variables as objects
    private lateinit var viewPagerAdapter: ViewPagerAdapterSensor
    private lateinit var calendar: Calendar
    private var progressMenuItem: MenuItem? = null
    private lateinit var service: ScheduledExecutorService
    private lateinit var sensor: Sensor
    private val sdfDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    // Utils packages
    private lateinit var smu: ServerMessagingUtils
    private lateinit var su: StorageUtils
    private lateinit var nu: NotificationUtils
    private var exportMode: Int = 0
    private var bottomInsets = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        // Initialize toolbar
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                bottomInsets = insets.systemWindowInsetBottom
                insets
            }
        }

        // Initialize StorageUtils
        su = StorageUtils(this)

        // Initialize ServerMessagingUtils
        smu = ServerMessagingUtils(this@SensorActivity)

        // Initialize NotificationUtils
        nu = NotificationUtils(this)

        // Get intent extras
        sensor = Sensor()
        if (intent.hasExtra("Name")) {
            sensor.name = intent.getStringExtra("Name")!!
            supportActionBar!!.title = intent.getStringExtra("Name")
        }
        if (intent.hasExtra("ID")) sensor.chipID = intent.getStringExtra("ID")!!
        if (intent.hasExtra("Color")) sensor.color = intent.getIntExtra("Color", ContextCompat.getColor(this, R.color.colorPrimary))

        // Initialize ViewPager
        view_pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or if (position == 0) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
                }
            }
        })
        viewPagerAdapter = ViewPagerAdapterSensor(supportFragmentManager, this@SensorActivity, su, su.getBoolean("ShowGPS_" + sensor.chipID))
        view_pager.adapter = viewPagerAdapter

        // Setup TabLayout
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL
        tabLayout.setupWithViewPager(view_pager)
        tabLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                view_pager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Initialize calendar
        if (selected_day_timestamp == 0L || !::calendar.isInitialized) {
            calendar = Calendar.getInstance()
            calendar.run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            current_day_timestamp = calendar.time.time
            selected_day_timestamp = current_day_timestamp
        }

        card_date_value.text = sdfDate.format(calendar.time)
        card_date_value.setOnClickListener {
            // Select date
            chooseDate(card_date_value)
        }
        card_date_edit.setOnClickListener {
            // Select date
            chooseDate(card_date_value)
        }
        card_date_today.setOnClickListener {
            // Set date to the current day
            calendar.time = Date()
            calendar.run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            selected_day_timestamp = calendar.time.time
            card_date_value.text = sdfDate.format(calendar.time)

            card_date_next.isEnabled = false
            card_date_today.isEnabled = false

            // Load data for selected date
            loadData()
        }
        card_date_back.setOnClickListener {
            // Go to previous day
            calendar.add(Calendar.DATE, -1)

            selected_day_timestamp = calendar.time.time
            card_date_value.text = sdfDate.format(calendar.time)

            val currentCalendar = Calendar.getInstance()
            currentCalendar.run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            card_date_next.isEnabled = calendar.before(currentCalendar)
            card_date_today.isEnabled = calendar.before(currentCalendar)

            // Load data for selected date
            loadData()
        }
        card_date_next.setOnClickListener {
            // Go to next day
            calendar.add(Calendar.DATE, 1)

            selected_day_timestamp = calendar.time.time
            card_date_value.text = sdfDate.format(calendar.time)

            val currentCalendar = Calendar.getInstance()
            currentCalendar.run {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            card_date_next.isEnabled = calendar.before(currentCalendar)
            card_date_today.isEnabled = calendar.before(currentCalendar)

            // Load data for selected date
            loadData()
        }
        card_date_next.isEnabled = false
        card_date_today.isEnabled = false

        // Set refresh period
        val period = Integer.parseInt(su.getString("sync_cycle", Constants.DEFAULT_SYNC_CYCLE.toString()))

        // Setup ScheduledExecutorService
        service = Executors.newSingleThreadScheduledExecutor()
        service.scheduleAtFixedRate({
            if (selected_day_timestamp == current_day_timestamp) {
                Log.i(Constants.TAG, "Auto refreshing ...")
                loadData()
            }
        }, period.toLong(), period.toLong(), TimeUnit.SECONDS)

        if (sensor.chipID != "no_id") loadData()

        //Check if sensor is existing on the server
        checkSensorAvailability()
    }

    private fun chooseDate(card_date_value: TextView) {
        // Select date
        val datePickerDialog = DatePickerDialog(this@SensorActivity, DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val calendarNew = Calendar.getInstance()
            calendarNew.run {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            card_date_next.isEnabled = calendarNew.before(calendar)
            card_date_today.isEnabled = calendarNew.before(calendar)

            selected_day_timestamp = calendarNew.time.time
            card_date_value.text = sdfDate.format(calendarNew.time)

            calendar = calendarNew

            // Load data for selected date
            loadData()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_sensor, menu)
        menu.findItem(R.id.action_show_gps).isChecked = su.getBoolean("ShowGPS_" + sensor.chipID)
        progressMenuItem = menu.findItem(R.id.action_refresh)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_export -> exportData()
            R.id.action_show_gps -> {
                item.isChecked = !item.isChecked
                viewPagerAdapter.showGPSData(item.isChecked)
                su.putBoolean("ShowGPS_" + sensor.chipID, item.isChecked)
            }
            R.id.action_refresh -> {
                // Reload data
                Log.i(Constants.TAG, "User refreshing ...")
                loadData()
            }
            R.id.action_settings -> //Launch SettingsActivity
                startActivity(Intent(this@SensorActivity, SettingsActivity::class.java))
            R.id.action_exit -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        service.shutdown()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_WRITE_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (exportMode == 1) {
                exportDiagram()
            } else if (exportMode == 2) {
                exportDataRecords()
            }
        }
    }

    //-----------------------------------Private Methods-------------------------------------------

    private fun loadData() {
        // Set ProgressMenuItem
        progressMenuItem?.setActionView(R.layout.menu_item_loading)

        CoroutineScope(Dispatchers.IO).launch {
            // Clear records
            records.clear()

            // Get timestamps for 'from' and'to'
            var from = selected_day_timestamp
            val to = selected_day_timestamp + TimeUnit.DAYS.toMillis(1)

            if (su.getBoolean("reduce_data_consumption", true) && records.size > 0 && selected_day_timestamp == current_day_timestamp) {
                // Load existing records from local database
                records = su.loadRecords(sensor.chipID, from, to)
                // Sort by time
                sortData()
                from = records[records.size - 1].dateTime.time + 1000
            }

            // If previous record was more than 30 secs ago
            if ((if (records.size > 0) records[records.size - 1].dateTime.time else from) < System.currentTimeMillis() - 30000) {
                // Check if internet is available
                if (smu.isInternetAvailable) {
                    // Internet is available
                    records.addAll(loadDataRecords(this@SensorActivity, sensor.chipID, from, to)!!)
                } else {
                    // Internet is not available
                    smu.checkConnection(container)
                }
            }

            // Sort by time
            sortData()
            // Execute error correction
            if (su.getBoolean("enable_auto_correction", true)) {
                records = Tools.measurementCorrection1(records)
                records = Tools.measurementCorrection2(records)
            }
            // Detect sensor breakdown
            if (smu.isInternetAvailable) {
                if (su.getBoolean("notification_breakdown", true) && su.isSensorExisting(sensor.chipID) && selected_day_timestamp == current_day_timestamp && Tools.isMeasurementBreakdown(su, records)) {
                    if (!su.getBoolean("BD_" + sensor.chipID)) {
                        nu.displayMissingMeasurementsNotification(sensor.chipID, sensor.name)
                        su.putBoolean("BD_" + sensor.chipID, true)
                    }
                } else {
                    nu.cancelNotification(Integer.parseInt(sensor.chipID) * 10)
                    su.removeKey("BD_" + sensor.chipID)
                }
            }
            // Push data records into adapter
            ViewPagerAdapterSensor.records = records
            // If there is a widget for this sensor, refresh it
            val updateIntent = Intent(applicationContext, WidgetProvider::class.java)
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent.putExtra(Constants.WIDGET_EXTRA_SENSOR_ID, sensor.chipID)
            sendBroadcast(updateIntent)

            CoroutineScope(Dispatchers.Main).launch {
                // Refresh ViewpagerAdapter
                viewPagerAdapter.refreshFragments()
                // Reset ProgressMenuItem
                progressMenuItem?.actionView = null
            }
        }
    }

    private fun checkSensorAvailability() {
        if (!su.getBoolean("DontShowAgain_" + sensor.chipID) && smu.isInternetAvailable) {
            CoroutineScope(Dispatchers.IO).launch {
                if(!isSensorDataExisting(this@SensorActivity, sensor.chipID)) {
                    CoroutineScope(Dispatchers.Main).launch {
                        AlertDialog.Builder(this@SensorActivity)
                            .setTitle(R.string.app_name)
                            .setMessage(R.string.add_sensor_tick_not_set_message)
                            .setPositiveButton(R.string.ok, null)
                            .setNegativeButton(R.string.do_not_show_again) { _, _ -> su.putBoolean("DontShowAgain_" + sensor.chipID, true) }
                            .show()
                    }
                }
            }
        }
    }

    private fun exportData() {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_share, container, false)
        val d = AlertDialog.Builder(this)
            .setView(v)
            .show()

        v.share_sensor.setOnClickListener {
            Handler().postDelayed({
                shareSensor()
                d.dismiss()
            }, 200)
        }
        v.share_diagram.setOnClickListener {
            if (records.size > 0) {
                if (ContextCompat.checkSelfPermission(this@SensorActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Handler().postDelayed({
                        exportDiagram()
                        d.dismiss()
                    }, 200)
                } else {
                    exportMode = 1
                    d.dismiss()
                    ActivityCompat.requestPermissions(this@SensorActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_WRITE_EXTERNAL_STORAGE)
                }
            } else {
                Toast.makeText(this@SensorActivity, R.string.no_data_date, Toast.LENGTH_SHORT).show()
            }
        }
        v.share_data_records.setOnClickListener {
            if (records.size > 0) {
                if (ContextCompat.checkSelfPermission(this@SensorActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Handler().postDelayed({
                        exportDataRecords()
                        d.dismiss()
                    }, 200)
                } else {
                    exportMode = 2
                    d.dismiss()
                    ActivityCompat.requestPermissions(this@SensorActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_WRITE_EXTERNAL_STORAGE)
                }
            } else {
                Toast.makeText(this@SensorActivity, R.string.no_data_date, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareSensor() {
        // Share sensor
        val i = Intent(Intent.ACTION_SEND)
        i.run {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_sensor))
            putExtra(Intent.EXTRA_TEXT, "https://pm.chillibits.com/s/" + sensor.chipID)
        }
        startActivity(Intent.createChooser(i, getString(R.string.share_sensor)))
    }

    private fun exportDiagram() {
        // Eyport Diagram
        viewPagerAdapter.exportDiagram()
    }

    private fun exportDataRecords() {
        // Export data records
        val exportUri = su.exportDataRecords(records)
        if (exportUri != null) {
            val i = Intent(Intent.ACTION_SEND)
            i.type = URLConnection.guessContentTypeFromName(exportUri.path)
            i.putExtra(Intent.EXTRA_STREAM, exportUri)
            startActivity(Intent.createChooser(i, getString(R.string.export_data_records)))
        }
    }

    override fun onDiagramFragmentLoaded(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) view.setPadding(0, 0, 0, bottomInsets + Tools.dpToPx(3))
    }

    override fun onDataFragmentLoaded(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val paddingInPixels = Tools.dpToPx(3)
            view!!.setPadding(paddingInPixels, paddingInPixels, paddingInPixels, bottomInsets + paddingInPixels)
        }
    }

    companion object {
        // Constants
        const val SORT_MODE_TIME_ASC = 101
        const val SORT_MODE_TIME_DESC = 102
        const val SORT_MODE_VALUE1_ASC = 103
        const val SORT_MODE_VALUE1_DESC = 104
        const val SORT_MODE_VALUE2_ASC = 105
        const val SORT_MODE_VALUE2_DESC = 106
        const val SORT_MODE_TEMP_ASC = 107
        const val SORT_MODE_TEMP_DESC = 108
        const val SORT_MODE_HUMIDITY_ASC = 109
        const val SORT_MODE_HUMIDITY_DESC = 110
        const val SORT_MODE_PRESSURE_ASC = 111
        const val SORT_MODE_PRESSURE_DESC = 112
        private const val REQ_WRITE_EXTERNAL_STORAGE = 1
        var records = ArrayList<DataRecord>()

        // Variables
        var selected_day_timestamp: Long = 0
        var current_day_timestamp: Long = 0
        var sort_mode = SORT_MODE_TIME_ASC // Attention!! When you alter that attribute, the ViewPagerAdapterSensor does not work correctly any more
        var custom_p1 = true
        var custom_p2 = true
        var custom_temp = false
        var custom_humidity = false
        var custom_pressure = false

        fun sortData() {
            try {
                records.sort()
            } catch (ignored: Exception) {}
        }
    }
}