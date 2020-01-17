/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import com.chillibits.pmapp.model.DataRecord
import com.chillibits.pmapp.model.Sensor
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.network.loadDataRecords
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.tool.Tools
import com.chillibits.pmapp.ui.view.ProgressDialog
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.activity_compare.*
import kotlinx.android.synthetic.main.dialog_export_compare.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class CompareActivity : AppCompatActivity() {

    // Variables as objects
    private lateinit var calendar: Calendar
    private var progressMenuItem: MenuItem? = null
    private val sdfDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Util packages
    private lateinit var smu: ServerMessagingUtils
    
    // Components
    private var noData: Boolean = false
    private var exportOption: Int = 0
    private var firstTime: Long = 0
    private var lastTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compare)

        // Initialize toolbar
        toolbar.setTitle(R.string.compare_sensors)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                container.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                insets
            }
        }

        // Initialize calendar
        calendar = Calendar.getInstance()
        if (selected_day_timestamp == 0L) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            current_day_timestamp = calendar.time.time
            selected_day_timestamp = current_day_timestamp
        }

        // Initialize StorageUtils
        su = StorageUtils(this)

        // Initialize ServiceMessagingUtils
        smu = ServerMessagingUtils(this)

        // Load sensors
        if(!intent.hasExtra("Sensors")) {
            finish()
            return
        }
        sensors = intent.getSerializableExtra("Sensors") as ArrayList<Sensor>

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
            calendar.run {
                time = Date()
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
            // Select previous day
            calendar.add(Calendar.DATE, -1)
            zap()
        }
        card_date_next.setOnClickListener {
            // Select next day
            calendar.add(Calendar.DATE, 1)
            zap()
        }
        card_date_next.isEnabled = false
        card_date_today.isEnabled = false

        diagram_p1.gridLabelRenderer.numHorizontalLabels = 3
        diagram_p1.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = value.toLong()
                    sdfTime.format(cal.time)
                } else {
                    super.formatLabel(value, isValueX).replace(".000", "k")
                }
            }
        }
        diagram_p1.setOnClickListener {
            val i = Intent(this@CompareActivity, DiagramActivity::class.java)
            i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA)
            i.putExtra("Show1", true)
            startActivity(i)
        }
        //diagramP1.getGridLabelRenderer().setVerticalAxisTitle("µg/m³");

        diagram_p2.gridLabelRenderer.numHorizontalLabels = 3
        diagram_p2.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = value.toLong()
                    sdfTime.format(cal.time)
                } else {
                    super.formatLabel(value, isValueX).replace(".000", "k")
                }
            }
        }
        diagram_p2.setOnClickListener {
            val i = Intent(this@CompareActivity, DiagramActivity::class.java)
            i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA)
            i.putExtra("Show2", true)
            startActivity(i)
        }
        //diagramP2.getGridLabelRenderer().setVerticalAxisTitle("µg/m³");

        diagram_temp.gridLabelRenderer.numHorizontalLabels = 3
        diagram_temp.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                if (!isValueX) return super.formatLabel(value, isValueX)
                val cal = Calendar.getInstance()
                cal.timeInMillis = value.toLong()
                return sdfTime.format(cal.time)
            }
        }
        diagram_temp.setOnClickListener {
            val i = Intent(this@CompareActivity, DiagramActivity::class.java)
            i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA)
            i.putExtra("Show3", true)
            startActivity(i)
        }
        //diagramTemp.getGridLabelRenderer().setVerticalAxisTitle("°C³");

        diagram_humidity.gridLabelRenderer.numHorizontalLabels = 3
        diagram_humidity.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                if (!isValueX) return super.formatLabel(value, isValueX)
                val cal = Calendar.getInstance()
                cal.timeInMillis = value.toLong()
                return sdfTime.format(cal.time)
            }
        }
        diagram_humidity.setOnClickListener {
            val i = Intent(this@CompareActivity, DiagramActivity::class.java)
            i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA)
            i.putExtra("Show4", true)
            startActivity(i)
        }
        //diagramHumidity.getGridLabelRenderer().setVerticalAxisTitle("%");

        diagram_pressure.gridLabelRenderer.numHorizontalLabels = 3
        diagram_pressure.gridLabelRenderer.labelFormatter = object : DefaultLabelFormatter() {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                if (!isValueX) return super.formatLabel(value, isValueX)
                val cal = Calendar.getInstance()
                cal.timeInMillis = value.toLong()
                return sdfTime.format(cal.time)
            }
        }
        diagram_pressure.setOnClickListener {
            val i = Intent(this@CompareActivity, DiagramActivity::class.java)
            i.putExtra("Mode", DiagramActivity.MODE_COMPARE_DATA)
            i.putExtra("Show5", true)
            startActivity(i)
        }
        //diagramPressure.getGridLabelRenderer().setVerticalAxisTitle("hPa");

        loadData()
    }

    private fun zap() {
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

    private fun chooseDate(card_date_value: TextView) {
        // Select date
        val datePickerDialog = DatePickerDialog(this@CompareActivity, DatePickerDialog.OnDateSetListener { _, year, month, day ->
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
        menuInflater.inflate(R.menu.menu_activity_compare, menu)
        progressMenuItem = menu.findItem(R.id.action_refresh)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        } else if (id == R.id.action_export) {
            var empty = true
            for (r in records) {
                if (r.isNotEmpty()) empty = false
            }
            if (!empty) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.export_diagram)
                    .setView(LayoutInflater.from(this).inflate(R.layout.dialog_export_compare, container, false))
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.done) { _, _ ->
                        when {
                            export_diagram_p1.isChecked -> exportOption = 1
                            export_diagram_p2.isChecked -> exportOption = 2
                            export_diagram_temp.isChecked -> exportOption = 3
                            export_diagram_humidity.isChecked -> exportOption = 4
                            export_diagram_pressure.isChecked -> exportOption = 5
                        }
                        exportData()
                    }
                    .show()
            } else {
                Toast.makeText(this, R.string.no_data_date, Toast.LENGTH_SHORT).show()
            }
        } else if (id == R.id.action_refresh) {
            Log.i(Constants.TAG, "User refreshing ...")
            // Reload data
            loadData()
        } else if (id == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_WRITE_EXTERNAL_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) exportData()
    }

    private fun loadData() {
        // Set ProgressMenuItem
        progressMenuItem?.setActionView(R.layout.menu_item_loading)

        val pd = ProgressDialog(this)
        pd.setDialogCancelable(false)
        pd.setTitle(getString(R.string.loading_data))
        pd.show()

        CoroutineScope(Dispatchers.IO).launch {
            // Clear ArrayList
            records.clear()

            // Clear diagrams
            diagram_p1.removeAllSeries()
            diagram_p2.removeAllSeries()
            diagram_humidity.removeAllSeries()
            diagram_temp.removeAllSeries()
            diagram_pressure.removeAllSeries()

            // Get timestamps for 'from' and 'to'
            val from = selected_day_timestamp
            val to = selected_day_timestamp + TimeUnit.DAYS.toMillis(1)

            // Get time of first record
            firstTime = java.lang.Long.MAX_VALUE
            lastTime = java.lang.Long.MIN_VALUE
            for (i in sensors.indices) {
                // Load existing data records from local database
                val currentRecords = su.loadRecords(sensors[i].chipID, from, to)
                // Sort by time
                currentRecords.sort()
                // If previous record was more than 30 secs ago, reload data from server
                if((if(currentRecords.size > 0) currentRecords[currentRecords.size - 1].dateTime.time else from) < System.currentTimeMillis() - 30000) {
                    // Check if internet is available
                    if (smu.isInternetAvailable) {
                        currentRecords.addAll(
                            loadDataRecords(
                                this@CompareActivity,
                                sensors[i].chipID,
                                if (currentRecords.size > 0 && selected_day_timestamp == current_day_timestamp) currentRecords[currentRecords.size - 1].dateTime.time + 1000 else from,
                                to
                            )!!
                        )
                    }
                }
                // Sort by time
                currentRecords.sort()
                // Add records to the list
                records.add(currentRecords) // Has to be 'add', not 'addAll' cause it's an ArrayList within an ArrayList
                try {
                    val currentFirstTime = records[i][0].dateTime.time
                    val currentLastTime = records[i][records[i].size - 1].dateTime.time
                    firstTime = if (currentFirstTime < firstTime) currentFirstTime else firstTime
                    lastTime = if (currentLastTime > lastTime) currentLastTime else lastTime
                } catch (ignored: Exception) {}
                CoroutineScope(Dispatchers.Main).launch {
                    pd.setMessage("${i * 100 / sensors.size}%")
                }
            }

            noData = true

            for (i in sensors.indices) {
                var currentRecords = records[i]
                // Possibly execute error correction
                if (su.getBoolean("enable_auto_correction", true)) {
                    currentRecords = Tools.measurementCorrection1(currentRecords)
                    currentRecords = Tools.measurementCorrection2(currentRecords)
                }
                if (currentRecords.size > 0) {
                    noData = false
                    try {
                        val seriesP1 = LineGraphSeries<DataPoint>()
                        seriesP1.color = sensors[i].color
                        Tools.fitArrayList(su, currentRecords).forEach {
                            try {
                                val time = it.dateTime
                                seriesP1.appendData(DataPoint(time.time.toDouble(), it.p1), false, 1000000)
                            } catch (ignored: Exception) {}
                        }

                        val seriesP2 = LineGraphSeries<DataPoint>()
                        seriesP2.color = sensors[i].color
                        Tools.fitArrayList(su, currentRecords).forEach {
                            try {
                                val time = it.dateTime
                                seriesP2.appendData(DataPoint(time.time.toDouble(), it.p2), false, 1000000)
                            } catch (ignored: Exception) {}
                        }

                        val seriesTemp = LineGraphSeries<DataPoint>()
                        seriesTemp.color = sensors[i].color
                        Tools.fitArrayList(su, currentRecords).forEach {
                            try {
                                val time = it.dateTime
                                seriesTemp.appendData(DataPoint(time.time.toDouble(), it.temp), false, 1000000)
                            } catch (ignored: Exception) {}
                        }

                        val seriesHumidity = LineGraphSeries<DataPoint>()
                        seriesHumidity.color = sensors[i].color
                        Tools.fitArrayList(su, currentRecords).forEach {
                            try {
                                val time = it.dateTime
                                seriesHumidity.appendData(DataPoint(time.time.toDouble(), it.humidity), false, 1000000)
                            } catch (ignored: Exception) {}
                        }

                        val seriesPressure = LineGraphSeries<DataPoint>()
                        seriesPressure.color = sensors[i].color
                        Tools.fitArrayList(su, currentRecords).forEach {
                            try {
                                val time = it.dateTime
                                seriesPressure.appendData(DataPoint(time.time.toDouble(), it.pressure), false, 1000000)
                            } catch (ignored: Exception) {}
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            diagram_p1.addSeries(seriesP1)
                            diagram_p2.addSeries(seriesP2)
                            diagram_temp.addSeries(seriesTemp)
                            diagram_humidity.addSeries(seriesHumidity)
                            diagram_pressure.addSeries(seriesPressure)
                        }
                    } catch (ignored: Exception) {}
                }
            }
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    diagram_p1.viewport.run {
                        isScalable = true
                        setMinX(firstTime.toDouble())
                        setMaxX(lastTime.toDouble())
                        scrollToEnd()
                        isScalable = false
                    }
                    diagram_p2.viewport.run {
                        isScalable = true
                        setMinX(firstTime.toDouble())
                        setMaxX(lastTime.toDouble())
                        scrollToEnd()
                        isScalable = false
                    }
                    diagram_temp.viewport.run {
                        isScalable = true
                        setMinX(firstTime.toDouble())
                        setMaxX(lastTime.toDouble())
                        scrollToEnd()
                        isScalable = false
                    }
                    diagram_humidity.viewport.run {
                        isScalable = true
                        setMinX(firstTime.toDouble())
                        setMaxX(lastTime.toDouble())
                        scrollToEnd()
                        isScalable = false
                    }
                    diagram_pressure.viewport.run {
                        isScalable = true
                        setMinX(firstTime.toDouble())
                        setMaxX(lastTime.toDouble())
                        scrollToEnd()
                        isScalable = false
                    }

                    no_data.visibility = if (noData) View.VISIBLE else View.GONE
                    container.visibility = if (noData) View.GONE else View.VISIBLE
                    // Reset ProgressMenuItem
                    if (progressMenuItem != null) progressMenuItem!!.actionView = null
                    pd.dismiss()
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun exportData() {
        if (ContextCompat.checkSelfPermission(this@CompareActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            when (exportOption) {
                1 -> diagram_p1.takeSnapshotAndShare(this@CompareActivity, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram))
                2 -> diagram_p2.takeSnapshotAndShare(this@CompareActivity, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram))
                3 -> diagram_temp.takeSnapshotAndShare(this@CompareActivity, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram))
                4 -> diagram_humidity.takeSnapshotAndShare(this@CompareActivity, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram))
                5 -> diagram_pressure.takeSnapshotAndShare(this@CompareActivity, "export_" + System.currentTimeMillis(), getString(R.string.export_diagram))
            }
        } else {
            ActivityCompat.requestPermissions(this@CompareActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_WRITE_EXTERNAL_STORAGE)
        }
    }

    companion object {
        // Constants
        private const val REQ_WRITE_EXTERNAL_STORAGE = 1
        lateinit var sensors: ArrayList<Sensor>
        var records = ArrayList<ArrayList<DataRecord>>()

        // Utils packages
        private lateinit var su: StorageUtils

        // Variables
        var selected_day_timestamp: Long = 0
        var current_day_timestamp: Long = 0
    }
}