/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.DataRecord
import com.mrgames13.jimdo.feinstaubapp.model.Sensor
import com.mrgames13.jimdo.feinstaubapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.tool.TimeFormatter
import com.mrgames13.jimdo.feinstaubapp.tool.Tools
import com.mrgames13.jimdo.feinstaubapp.ui.model.DiagramEntry
import com.mrgames13.jimdo.feinstaubapp.ui.view.DiagramMarkerView
import kotlinx.android.synthetic.main.activity_diagram.*
import java.util.*

class DiagramActivity : AppCompatActivity() {

    // Variables as objects
    private lateinit var records: ArrayList<DataRecord>
    private lateinit var compareRecords: ArrayList<ArrayList<DataRecord>>
    private lateinit var compareSensors: ArrayList<Sensor>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContentView(R.layout.activity_diagram)

        // Get intent extras
        val intent = intent
        val mode = intent.getIntExtra("Mode", MODE_SENSOR_DATA)

        val show1 = intent.hasExtra("Show1") && intent.getBooleanExtra("Show1", false)
        val show2 = intent.hasExtra("Show2") && intent.getBooleanExtra("Show2", false)
        val show3 = intent.hasExtra("Show3") && intent.getBooleanExtra("Show3", false)
        val show4 = intent.hasExtra("Show4") && intent.getBooleanExtra("Show4", false)
        val show5 = intent.hasExtra("Show5") && intent.getBooleanExtra("Show5", false)
        val enableAverage = intent.hasExtra("EnableAverage") && intent.getBooleanExtra("EnableAverage", false)
        val enableMedian = intent.hasExtra("EnableMedian") && intent.getBooleanExtra("EnableMedian", false)
        val enableThresholdWho = intent.hasExtra("EnableThresholdWHO") && intent.getBooleanExtra("EnableThresholdWHO", false)
        val enableThresholdEu = intent.hasExtra("EnableThresholdEU") && intent.getBooleanExtra("EnableThresholdEU", false)

        if (mode == MODE_SENSOR_DATA) {
            // Receive data from SensorActivity
            records = SensorActivity.records

            // Prepare data
            SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC
            records.sort()
        } else if (mode == MODE_COMPARE_DATA) {
            compareRecords = CompareActivity.records
            compareSensors = CompareActivity.sensors

            // Prepare data
            SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC
            for (current_records in compareRecords) current_records.sort()
        }

        try {
            // Initialize diagram
            chart.setHardwareAccelerationEnabled(true)
            chart.keepScreenOn = true
            chart.isKeepPositionOnRotation = true
            chart.description = null
            // Left y axis
            val left = chart.axisLeft
            left.valueFormatter = LargeValueFormatter()
            // x axis
            val xAxis = chart.xAxis
            xAxis.isGranularityEnabled = true
            xAxis.granularity = 60f
            xAxis.position = XAxis.XAxisPosition.BOTTOM

            // Sensor mode or comparison mode
            var firstTime: Long = 0
            if (mode == MODE_SENSOR_DATA) {
                // Plot data
                val entries1 = ArrayList<Entry>()
                val entries2 = ArrayList<Entry>()
                val entries3 = ArrayList<Entry>()
                val entries4 = ArrayList<Entry>()
                val entries5 = ArrayList<Entry>()
                firstTime = records[0].dateTime.time
                xAxis.valueFormatter = TimeFormatter(firstTime)
                for (r in records) {
                    if (show1) entries1.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toDouble(), r.p1, "µg/m³"))
                    if (show2) entries2.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toDouble(), r.p2, "µg/m³"))
                    if (show3) entries3.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toDouble(), r.temp, "°C"))
                    if (show4) entries4.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toDouble(), r.humidity, "%"))
                    if (show5) entries5.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toDouble(), r.pressure, "hPa"))
                }

                // PM1
                val p1 = LineDataSet(entries1, getString(R.string.value1) + " (µg/m³)")
                p1.color = ContextCompat.getColor(this, R.color.series1)
                p1.setCircleColor(ContextCompat.getColor(this, R.color.series1))
                p1.lineWidth = 2f
                p1.setDrawValues(false)
                p1.axisDependency = YAxis.AxisDependency.LEFT
                p1.highLightColor = ContextCompat.getColor(this, R.color.series1)
                //p1.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // PM2
                val p2 = LineDataSet(entries2, getString(R.string.value2) + " (µg/m³)")
                p2.color = ContextCompat.getColor(this, R.color.series2)
                p2.setCircleColor(ContextCompat.getColor(this, R.color.series2))
                p2.lineWidth = 2f
                p2.setDrawValues(false)
                p2.axisDependency = YAxis.AxisDependency.LEFT
                p2.highLightColor = ContextCompat.getColor(this, R.color.series2)
                //p2.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Temperature
                val temp = LineDataSet(entries3, getString(R.string.temperature) + " (°C)")
                temp.color = ContextCompat.getColor(this, R.color.series3)
                temp.setCircleColor(ContextCompat.getColor(this, R.color.series3))
                temp.lineWidth = 2f
                temp.setDrawValues(false)
                temp.axisDependency = YAxis.AxisDependency.RIGHT
                temp.highLightColor = ContextCompat.getColor(this, R.color.series3)
                //temp.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Humidity
                val humidity = LineDataSet(entries4, getString(R.string.humidity) + " (%)")
                humidity.color = ContextCompat.getColor(this, R.color.series4)
                humidity.setCircleColor(ContextCompat.getColor(this, R.color.series4))
                humidity.lineWidth = 2f
                humidity.setDrawValues(false)
                humidity.axisDependency = YAxis.AxisDependency.RIGHT
                humidity.highLightColor = ContextCompat.getColor(this, R.color.series4)
                //humidity.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Pressure
                val pressure = LineDataSet(entries5, getString(R.string.pressure) + " (hPa)")
                pressure.color = ContextCompat.getColor(this, R.color.series5)
                pressure.setCircleColor(ContextCompat.getColor(this, R.color.series5))
                pressure.lineWidth = 2f
                pressure.setDrawValues(false)
                pressure.axisDependency = YAxis.AxisDependency.RIGHT
                pressure.highLightColor = ContextCompat.getColor(this, R.color.series5)
                //pressure.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                // Add single lines
                val dataSets = ArrayList<ILineDataSet>()
                if (show1) dataSets.add(p1)
                if (show1 && (enableAverage || enableMedian)) dataSets.add(getAverageMedianPM1(enableAverage, enableMedian, firstTime))
                if (show2) dataSets.add(p2)
                if (show2 && (enableAverage || enableMedian)) dataSets.add(getAverageMedianPM2(enableAverage, enableMedian, firstTime))
                if (show3) dataSets.add(temp)
                if (show3 && (enableAverage || enableMedian)) dataSets.add(getAverageMedianTemperature(enableAverage, enableMedian, firstTime))
                if (show4) dataSets.add(humidity)
                if (show4 && (enableAverage || enableMedian)) dataSets.add(getAverageMedianHumidity(enableAverage, enableMedian, firstTime))
                if (show5) dataSets.add(pressure)
                if (show5 && (enableAverage || enableMedian)) dataSets.add(getAverageMedianPressure(enableAverage, enableMedian, firstTime))
                if ((show1 || show2) && (enableThresholdEu || enableThresholdWho)) {
                    dataSets.add(getThresholdPM1(enableThresholdEu, enableThresholdWho, firstTime))
                    dataSets.add(getThresholdPM2(enableThresholdEu, enableThresholdWho, firstTime))
                }
                chart.data = LineData(dataSets)
            } else if (mode == MODE_COMPARE_DATA) {
                // Get first time
                firstTime = java.lang.Long.MAX_VALUE
                for (i in compareSensors.indices) {
                    try {
                        val currentFirstTime = compareRecords[i][0].dateTime.time
                        firstTime = if (currentFirstTime < firstTime) currentFirstTime else firstTime
                    } catch (e: Exception) {}
                }
                xAxis.valueFormatter = TimeFormatter(firstTime)
                // Plot data
                val dataSets = ArrayList<ILineDataSet>()
                for (i in compareSensors.indices) {
                    if (compareRecords[i].size > 0) {
                        val entries = ArrayList<Entry>()
                        for (r in compareRecords[i]) {
                            try {
                                when {
                                    show1 -> entries.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toFloat().toDouble(), r.p1.toFloat().toDouble(), "µg/m³"))
                                    show2 -> entries.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toFloat().toDouble(), r.p2.toFloat().toDouble(), "µg/m³"))
                                    show3 -> entries.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toFloat().toDouble(), r.temp.toFloat().toDouble(), "°C"))
                                    show4 -> entries.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toFloat().toDouble(), r.humidity.toFloat().toDouble(), "%"))
                                    show5 -> entries.add(DiagramEntry(((r.dateTime.time - firstTime) / 1000).toFloat().toDouble(), r.pressure.toFloat().toDouble(), "hPa"))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                        var setName = getString(R.string.error_try_again)
                        if (show1) setName = compareSensors[i].name + " - " + getString(R.string.value1) + " (µg/m³)"
                        if (show2) setName = compareSensors[i].name + " - " + getString(R.string.value2) + " (µg/m³)"
                        if (show3) setName = compareSensors[i].name + " - " + getString(R.string.temperature) + " (°C)"
                        if (show4) setName = compareSensors[i].name + " - " + getString(R.string.humidity) + " (%)"
                        if (show5) setName = compareSensors[i].name + " - " + getString(R.string.pressure) + " (hPa)³"
                        val set = LineDataSet(entries, setName)
                        set.color = compareSensors[i].color
                        set.setCircleColor(compareSensors[i].color)
                        set.lineWidth = 2f
                        set.setDrawValues(false)
                        set.axisDependency = if (show1 || show2) YAxis.AxisDependency.LEFT else YAxis.AxisDependency.RIGHT
                        set.highLightColor = compareSensors[i].color
                        dataSets.add(set)
                    }
                }
                chart.data = LineData(dataSets)
            }

            chart.marker = DiagramMarkerView(this, R.layout.diagram_marker_view, firstTime)
            // Customize legend
            chart.legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            chart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            chart.legend.isWordWrapEnabled = true
            // Redraw & animate
            chart.invalidate()
            chart.animateY(700, Easing.EaseInCubic)
        } catch (ignored: Exception) {}
    }

    private fun getAverageMedianPM1(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries = ArrayList<Entry>()
        if (enable_average) {
            var average = 0.0
            for (record in records) average += record.p1
            average /= records.size.toDouble()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val doubleRecords = ArrayList<Double>()
            for (record in records) doubleRecords.add(record.p1)
            val median = Tools.calculateMedian(doubleRecords)
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
        }
        return getDashedLine(amEntries, R.color.series1)
    }

    private fun getAverageMedianPM2(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries: MutableList<Entry>
        amEntries = ArrayList()
        if (enable_average) {
            var average = 0.0
            for (record in records) average += record.p2
            average /= records.size.toDouble()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val doubleRecords = ArrayList<Double>()
            for (record in records) doubleRecords.add(record.p2)
            val median = Tools.calculateMedian(doubleRecords)
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
        }
        return getDashedLine(amEntries, R.color.series2)
    }

    private fun getAverageMedianTemperature(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries: MutableList<Entry>
        amEntries = ArrayList()
        if (enable_average) {
            var average = 0.0
            for (record in records) average += record.temp
            average /= records.size.toDouble()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val doubleRecords = ArrayList<Double>()
            for (record in records) doubleRecords.add(record.temp)
            val median = Tools.calculateMedian(doubleRecords)
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
        }
        val averageMedianTemperature = getDashedLine(amEntries, R.color.series3)
        averageMedianTemperature.axisDependency = YAxis.AxisDependency.RIGHT
        return averageMedianTemperature
    }

    private fun getAverageMedianHumidity(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries: MutableList<Entry>
        amEntries = ArrayList()
        if (enable_average) {
            var average = 0.0
            for (record in records) average += record.humidity
            average /= records.size.toDouble()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val doubleRecords = ArrayList<Double>()
            for (record in records) doubleRecords.add(record.humidity)
            val median = Tools.calculateMedian(doubleRecords)
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
        }
        val averageMedianHumidity = getDashedLine(amEntries, R.color.series4)
        averageMedianHumidity.axisDependency = YAxis.AxisDependency.RIGHT
        return averageMedianHumidity
    }

    private fun getAverageMedianPressure(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries: MutableList<Entry>
        amEntries = ArrayList()
        if (enable_average) {
            var average = 0.0
            for (record in records) average += record.pressure
            average /= records.size.toDouble()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val doubleRecords = ArrayList<Double>()
            for (record in records) doubleRecords.add(record.pressure)
            val median = Tools.calculateMedian(doubleRecords)
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
        }
        val averageMedianPressure = getDashedLine(amEntries, R.color.series5)
        averageMedianPressure.axisDependency = YAxis.AxisDependency.RIGHT
        return averageMedianPressure
    }

    private fun getThresholdPM1(enable_eu_thresholds: Boolean, enable_who_thresholds: Boolean, first_timestamp: Long): LineDataSet {
        val thEntries: MutableList<Entry>
        thEntries = ArrayList()
        if (enable_eu_thresholds) {
            thEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM10.toFloat()))
            thEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM10.toFloat()))
        } else if (enable_who_thresholds) {
            thEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM10.toFloat()))
            thEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM10.toFloat()))
        }
        return getDashedLine(thEntries, R.color.error)
    }

    private fun getThresholdPM2(enable_eu_thresholds: Boolean, enable_who_thresholds: Boolean, first_timestamp: Long): LineDataSet {
        val thEntries: MutableList<Entry>
        thEntries = ArrayList()
        if (enable_eu_thresholds) {
            thEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM2_5.toFloat()))
            thEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM2_5.toFloat()))
        } else if (enable_who_thresholds) {
            thEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM2_5.toFloat()))
            thEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM2_5.toFloat()))
        }
        return getDashedLine(thEntries, R.color.error)
    }

    private fun getDashedLine(am_entries: List<Entry>, color: Int): LineDataSet {
        val dl = LineDataSet(am_entries, null)
        dl.color = ContextCompat.getColor(this, color)
        dl.lineWidth = 1f
        dl.setDrawValues(false)
        dl.setDrawCircles(false)
        dl.isHighlightEnabled = false
        dl.enableDashedLine(10f, 10f, 0f)
        return dl
    }

    companion object {
        // Constants
        const val MODE_SENSOR_DATA = 10001
        const val MODE_COMPARE_DATA = 10002
    }
}