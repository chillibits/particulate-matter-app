/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.chillibits.pmapp.R
import com.chillibits.pmapp.tool.Constants
import com.chillibits.pmapp.tool.TimeFormatter
import com.chillibits.pmapp.tool.Tools
import com.chillibits.pmapp.ui.view.DiagramMarkerView
import com.chillibits.pmapp.ui.viewmodel.DiagramEntry
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.android.synthetic.main.activity_diagram.*
import java.util.*

class DiagramActivity : AppCompatActivity() {

    // Variables as objects
    private lateinit var records: ArrayList<com.chillibits.pmapp.model.DataRecord>
    private lateinit var compareRecords: ArrayList<ArrayList<com.chillibits.pmapp.model.DataRecord>>
    private lateinit var compareSensors: ArrayList<com.chillibits.pmapp.model.Sensor>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        setContentView(R.layout.activity_diagram)

        // Get intent extras
        val intent = intent
        val mode = intent.getIntExtra("Mode", MODE_SENSOR_DATA)

        val show1 = getIntentExtra("Show1")
        val show2 = getIntentExtra("Show2")
        val show3 = getIntentExtra("Show3")
        val show4 = getIntentExtra("Show4")
        val show5 = getIntentExtra("Show5")
        val enableAverage = getIntentExtra("EnableAverage")
        val enableMedian = getIntentExtra("EnableMedian")
        val enableThresholdWho = getIntentExtra("EnableThresholdWHO")
        val enableThresholdEu = getIntentExtra("EnableThresholdEU")

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
                records.forEach {
                    if (show1) entries1.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toDouble(), it.p1, "µg/m³"))
                    if (show2) entries2.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toDouble(), it.p2, "µg/m³"))
                    if (show3) entries3.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toDouble(), it.temp, "°C"))
                    if (show4) entries4.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toDouble(), it.humidity, "%"))
                    if (show5) entries5.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toDouble(), it.pressure, "hPa"))
                }

                // PM1
                val p1 = LineDataSet(entries1, getString(R.string.value1) + " (µg/m³)")
                p1.run {
                    color = ContextCompat.getColor(this@DiagramActivity, R.color.series1)
                    setCircleColor(ContextCompat.getColor(this@DiagramActivity, R.color.series1))
                    lineWidth = 2f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                    highLightColor = ContextCompat.getColor(this@DiagramActivity, R.color.series1)
                    //setMode(LineDataSet.Mode.CUBIC_BEZIER);
                }

                // PM2
                val p2 = LineDataSet(entries2, getString(R.string.value2) + " (µg/m³)")
                p2.run {
                    color = ContextCompat.getColor(this@DiagramActivity, R.color.series2)
                    setCircleColor(ContextCompat.getColor(this@DiagramActivity, R.color.series2))
                    lineWidth = 2f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                    highLightColor = ContextCompat.getColor(this@DiagramActivity, R.color.series2)
                    //setMode(LineDataSet.Mode.CUBIC_BEZIER);
                }

                // Temperature
                val temp = LineDataSet(entries3, getString(R.string.temperature) + " (°C)")
                temp.run {
                    color = ContextCompat.getColor(this@DiagramActivity, R.color.series3)
                    setCircleColor(ContextCompat.getColor(this@DiagramActivity, R.color.series3))
                    lineWidth = 2f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.RIGHT
                    highLightColor = ContextCompat.getColor(this@DiagramActivity, R.color.series3)
                    //setMode(LineDataSet.Mode.CUBIC_BEZIER);
                }

                // Humidity
                val humidity = LineDataSet(entries4, getString(R.string.humidity) + " (%)")
                humidity.run {
                    color = ContextCompat.getColor(this@DiagramActivity, R.color.series4)
                    setCircleColor(ContextCompat.getColor(this@DiagramActivity, R.color.series4))
                    lineWidth = 2f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.RIGHT
                    highLightColor = ContextCompat.getColor(this@DiagramActivity, R.color.series4)
                    //setMode(LineDataSet.Mode.CUBIC_BEZIER);
                }

                // Pressure
                val pressure = LineDataSet(entries5, getString(R.string.pressure) + " (hPa)")
                pressure.run {
                    color = ContextCompat.getColor(this@DiagramActivity, R.color.series5)
                    setCircleColor(ContextCompat.getColor(this@DiagramActivity, R.color.series5))
                    lineWidth = 2f
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.RIGHT
                    highLightColor = ContextCompat.getColor(this@DiagramActivity, R.color.series5)
                    //setMode(LineDataSet.Mode.CUBIC_BEZIER);
                }

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
                compareSensors.forEachIndexed { i, _ ->
                    try {
                        val currentFirstTime = compareRecords[i][0].dateTime.time
                        firstTime = if (currentFirstTime < firstTime) currentFirstTime else firstTime
                    } catch (e: Exception) {}
                }
                xAxis.valueFormatter = TimeFormatter(firstTime)
                // Plot data
                val dataSets = ArrayList<ILineDataSet>()
                compareSensors.forEachIndexed { i, sensor ->
                    if (compareRecords[i].size > 0) {
                        val entries = ArrayList<Entry>()
                        compareRecords[i].forEach {
                            try {
                                when {
                                    show1 -> entries.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toFloat().toDouble(), it.p1.toFloat().toDouble(), "µg/m³"))
                                    show2 -> entries.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toFloat().toDouble(), it.p2.toFloat().toDouble(), "µg/m³"))
                                    show3 -> entries.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toFloat().toDouble(), it.temp.toFloat().toDouble(), "°C"))
                                    show4 -> entries.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toFloat().toDouble(), it.humidity.toFloat().toDouble(), "%"))
                                    show5 -> entries.add(DiagramEntry(((it.dateTime.time - firstTime) / 1000).toFloat().toDouble(), it.pressure.toFloat().toDouble(), "hPa"))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        var setName = getString(R.string.error_try_again)
                        if (show1) setName = sensor.name + " - " + getString(R.string.value1) + " (µg/m³)"
                        if (show2) setName = sensor.name + " - " + getString(R.string.value2) + " (µg/m³)"
                        if (show3) setName = sensor.name + " - " + getString(R.string.temperature) + " (°C)"
                        if (show4) setName = sensor.name + " - " + getString(R.string.humidity) + " (%)"
                        if (show5) setName = sensor.name + " - " + getString(R.string.pressure) + " (hPa)³"
                        val set = LineDataSet(entries, setName)
                        set.run {
                            color = sensor.color
                            setCircleColor(sensor.color)
                            lineWidth = 2f
                            setDrawValues(false)
                            axisDependency = if (show1 || show2) YAxis.AxisDependency.LEFT else YAxis.AxisDependency.RIGHT
                            highLightColor = sensor.color
                        }
                        dataSets.add(set)
                    }
                }
                chart.data = LineData(dataSets)
            }

            chart.run {
                marker = DiagramMarkerView(this@DiagramActivity, R.layout.diagram_marker_view, firstTime)
                // Customize legend
                legend.run {
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    isWordWrapEnabled = true
                }
                // Redraw & animate
                invalidate()
                animateY(700, Easing.EaseInCubic)
            }
        } catch (ignored: Exception) {}
    }

    private fun getIntentExtra(extraName: String) = intent.hasExtra(extraName) && intent.getBooleanExtra(extraName, false)

    private fun getAverageMedianPM1(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries = ArrayList<Entry>()
        if (enable_average) {
            val average = records.map { it.p1 }.average()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val median = Tools.calculateMedian(records.map { it.p1 })
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
        }
        return getDashedLine(amEntries, R.color.series1)
    }

    private fun getAverageMedianPM2(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries: MutableList<Entry>
        amEntries = ArrayList()
        if (enable_average) {
            val average = records.map { it.p2 }.average()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val median = Tools.calculateMedian(records.map { it.p2 })
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
        }
        return getDashedLine(amEntries, R.color.series2)
    }

    private fun getAverageMedianTemperature(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
        val amEntries: MutableList<Entry>
        amEntries = ArrayList()
        if (enable_average) {
            val average = records.map { it.temp }.average()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val median = Tools.calculateMedian(records.map { it.temp })
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
            val average = records.map { it.humidity }.average()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val median = Tools.calculateMedian(records.map { it.humidity })
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
            val average = records.map { it.pressure }.average()
            amEntries.add(Entry(((records[0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
            amEntries.add(Entry(((records[records.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
        } else if (enable_median) {
            val median = Tools.calculateMedian(records.map { it.pressure })
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
        dl.run {
            this.color = ContextCompat.getColor(this@DiagramActivity, color)
            lineWidth = 1f
            setDrawValues(false)
            setDrawCircles(false)
            isHighlightEnabled = false
            enableDashedLine(10f, 10f, 0f)
        }
        return dl
    }

    companion object {
        // Constants
        const val MODE_SENSOR_DATA = 10001
        const val MODE_COMPARE_DATA = 10002
    }
}