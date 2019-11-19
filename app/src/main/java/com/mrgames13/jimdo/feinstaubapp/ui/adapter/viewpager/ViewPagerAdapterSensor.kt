/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.adapter.viewpager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.DataRecord
import com.mrgames13.jimdo.feinstaubapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.tool.TimeFormatter
import com.mrgames13.jimdo.feinstaubapp.tool.Tools
import com.mrgames13.jimdo.feinstaubapp.ui.activity.DiagramActivity
import com.mrgames13.jimdo.feinstaubapp.ui.activity.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.ui.adapter.recyclerview.DataAdapter
import com.mrgames13.jimdo.feinstaubapp.ui.model.DiagramEntry
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class ViewPagerAdapterSensor(manager: FragmentManager, activity: SensorActivity, su: StorageUtils, show_gps_data: Boolean) : FragmentStatePagerAdapter(manager) {

    // Variables as objects
    private val tabTitles = ArrayList<String>()

    // Interfaces
    interface OnFragmentsLoadedListener {
        fun onDiagramFragmentLoaded(view: View)
        fun onDataFragmentLoaded(view: View?)
    }

    init {
        Companion.activity = activity
        h = Handler()
        Companion.su = su
        tabTitles.add(Companion.activity.getString(R.string.tab_diagram))
        tabTitles.add(activity.getString(R.string.tab_data))
        df_time.timeZone = TimeZone.getDefault()
        Companion.show_gps_data = show_gps_data
    }

    override fun getItem(pos: Int): Fragment {
        return if (pos == 0) DiagramFragment() else DataFragment()
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return tabTitles[position]
    }

    fun refreshFragments() {
        DiagramFragment.refresh()
        DataFragment.refresh()
    }

    fun exportDiagram() {
        DiagramFragment.exportDiagram()
    }

    fun showGPSData(show: Boolean) {
        DataFragment.showGPSData(
            show
        )
    }

    //-------------------------------------------Fragments------------------------------------------

    class DiagramFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
            contentView = LayoutInflater.from(parent?.context).inflate(R.layout.tab_diagram, parent, false)

            chart = contentView.findViewById(R.id.chart)
            chart.setHardwareAccelerationEnabled(true)
            chart.isDoubleTapToZoomEnabled = false
            chart.setScaleEnabled(false)
            chart.setPinchZoom(false)
            chart.isHighlightPerTapEnabled = false
            chart.isHighlightPerDragEnabled = false
            chart.description = null
            chart.legend.isEnabled = false
            // Left y axis
            val left = chart.axisLeft
            left.valueFormatter = LargeValueFormatter()
            left.setDrawAxisLine(true)
            left.setDrawGridLines(false)
            left.spaceBottom = 0f
            // Right y axis
            val right = chart.axisRight
            right.valueFormatter = LargeValueFormatter()
            right.setDrawAxisLine(true)
            right.setDrawGridLines(false)
            right.setDrawZeroLine(true)
            // x axis
            val xAxis = chart.xAxis
            xAxis.granularity = 60f
            xAxis.isGranularityEnabled = true
            xAxis.position = XAxis.XAxisPosition.BOTTOM

            // Set OnClickListener
            chart.setOnClickListener {
                if (SensorActivity.custom_p1 || SensorActivity.custom_p2 || SensorActivity.custom_temp || SensorActivity.custom_humidity || SensorActivity.custom_pressure) {
                    val i = Intent(activity, DiagramActivity::class.java)
                    i.putExtra("Show1", SensorActivity.custom_p1)
                    i.putExtra("Show2", SensorActivity.custom_p2)
                    i.putExtra("Show3", SensorActivity.custom_temp)
                    i.putExtra("Show4", SensorActivity.custom_humidity)
                    i.putExtra("Show5", SensorActivity.custom_pressure)
                    i.putExtra("EnableAverage", custom_average.isChecked)
                    i.putExtra("EnableMedian", custom_median.isChecked)
                    i.putExtra("EnableThresholdWHO", custom_threshold_who.isChecked)
                    i.putExtra("EnableThresholdEU", custom_threshold_eu.isChecked)
                    startActivity(i)
                } else {
                    Toast.makeText(activity, getString(R.string.no_diagram_selected), Toast.LENGTH_SHORT).show()
                }
            }

            // Initialize custom controls
            custom_p1 = contentView.findViewById(R.id.custom_p1)
            custom_p2 = contentView.findViewById(R.id.custom_p2)
            custom_temp = contentView.findViewById(R.id.custom_temp)
            custom_humidity = contentView.findViewById(R.id.custom_humidity)
            custom_pressure = contentView.findViewById(R.id.custom_pressure)
            val customNothing = contentView.findViewById<RadioButton>(R.id.enable_average_median_nothing)
            custom_average = contentView.findViewById(R.id.enable_average)
            custom_median = contentView.findViewById(R.id.enable_median)
            val customThresholdNothing = contentView.findViewById<RadioButton>(R.id.enable_eu_who_nothing)
            custom_threshold_who = contentView.findViewById(R.id.enable_who)
            custom_threshold_eu = contentView.findViewById(R.id.enable_eu)

            h.post {
                custom_p1.isChecked = SensorActivity.custom_p1
                custom_p2.isChecked = SensorActivity.custom_p2
                custom_temp.isChecked = SensorActivity.custom_temp
                custom_humidity.isChecked = SensorActivity.custom_humidity
                custom_pressure.isChecked = SensorActivity.custom_pressure
            }

            custom_p1.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { cb, value ->
                if (!value && !custom_p2.isChecked && !custom_temp.isChecked && !custom_humidity.isChecked && !custom_pressure.isChecked) {
                    cb.isChecked = true
                    return@OnCheckedChangeListener
                }
                SensorActivity.custom_p1 = value
                if (dataSets.size >= 5) {
                    av_p1.isVisible = custom_average.isChecked && value
                    med_p1.isVisible = custom_median.isChecked && value
                    th_eu_p1.isVisible = custom_threshold_eu.isChecked && value
                    th_who_p1.isVisible = custom_threshold_who.isChecked && value

                    var highest = 1.0
                    if(custom_p2.isChecked) highest = Tools.findMaxMeasurement(records, 2)
                    if(value) highest = max(highest, Tools.findMaxMeasurement(records, 1))
                    left.axisMaximum = highest.toFloat()
                    left.calculate(0f, highest.toFloat())

                    showGraph(0, value)
                }
            })
            custom_p2.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { cb, value ->
                if (!custom_p1.isChecked && !value && !custom_temp.isChecked && !custom_humidity.isChecked && !custom_pressure.isChecked) {
                    cb.isChecked = true
                    return@OnCheckedChangeListener
                }
                SensorActivity.custom_p2 = value
                if (dataSets.size >= 5) {
                    av_p2.isVisible = custom_average.isChecked && value
                    med_p2.isVisible = custom_median.isChecked && value
                    th_eu_p2.isVisible = custom_threshold_eu.isChecked && value
                    th_who_p2.isVisible = custom_threshold_who.isChecked && value

                    var highest = 1.0
                    if(custom_p1.isChecked) highest = Tools.findMaxMeasurement(records, 1)
                    if(value) highest = max(highest, Tools.findMaxMeasurement(records, 2))
                    left.axisMaximum = highest.toFloat()
                    left.calculate(0f, highest.toFloat())

                    showGraph(1, value)
                }
            })
            custom_temp.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { cb, value ->
                if (!custom_p1.isChecked && !custom_p2.isChecked && !value && !custom_humidity.isChecked && !custom_pressure.isChecked) {
                    cb.isChecked = true
                    return@OnCheckedChangeListener
                }
                SensorActivity.custom_temp = value
                if (dataSets.size >= 5) {
                    av_temp.isVisible = custom_average.isChecked && value
                    med_temp.isVisible = custom_median.isChecked && value

                    var highest = 1.0
                    var lowest = 0.0
                    if(value) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 3))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 3))
                    }
                    if(custom_humidity.isChecked) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 4))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 4))
                    }
                    if(custom_pressure.isChecked) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 5))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 5))
                    }
                    right.calculate(lowest.toFloat(), highest.toFloat())

                    showGraph(2, value)
                }
            })
            custom_humidity.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { cb, value ->
                if (!custom_p1.isChecked && !custom_p2.isChecked && !custom_temp.isChecked && !value && !custom_pressure.isChecked) {
                    cb.isChecked = true
                    return@OnCheckedChangeListener
                }
                SensorActivity.custom_humidity = value
                if (dataSets.size >= 5) {
                    av_humidity.isVisible = custom_average.isChecked && value
                    med_humidity.isVisible = custom_median.isChecked && value

                    var highest = 1.0
                    var lowest = 0.0
                    if(custom_temp.isChecked) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 3))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 3))
                    }
                    if(value) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 4))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 4))
                    }
                    if(custom_pressure.isChecked) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 5))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 5))
                    }
                    right.calculate(lowest.toFloat(), highest.toFloat())

                    showGraph(3, value)
                }
            })
            custom_pressure.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { cb, value ->
                if (!custom_p1.isChecked && !custom_p2.isChecked && !custom_temp.isChecked && !custom_humidity.isChecked && !value) {
                    cb.isChecked = true
                    return@OnCheckedChangeListener
                }
                SensorActivity.custom_pressure = value
                if (dataSets.size >= 5) {
                    av_pressure.isVisible = custom_average.isChecked && value
                    med_pressure.isVisible = custom_median.isChecked && value

                    var highest = 1.0
                    var lowest = 0.0
                    if(custom_temp.isChecked) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 3))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 3))
                    }
                    if(custom_humidity.isChecked) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 4))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 4))
                    }
                    if(value) {
                        highest = max(highest, Tools.findMaxMeasurement(records, 5))
                        lowest = min(lowest, Tools.findMinMeasurement(records, 5))
                    }
                    right.calculate(lowest.toFloat(), highest.toFloat())

                    showGraph(4, value)
                }
            })

            customNothing.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    av_p1.isVisible = false
                    av_p2.isVisible = false
                    av_temp.isVisible = false
                    av_humidity.isVisible = false
                    av_pressure.isVisible = false
                    med_p1.isVisible = false
                    med_p2.isVisible = false
                    med_temp.isVisible = false
                    med_humidity.isVisible = false
                    med_pressure.isVisible = false
                    chart.invalidate()
                }
            }
            custom_average.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    if (SensorActivity.custom_p1) av_p1.isVisible = true
                    if (SensorActivity.custom_p2) av_p2.isVisible = true
                    if (SensorActivity.custom_temp) av_temp.isVisible = true
                    if (SensorActivity.custom_humidity) av_humidity.isVisible = true
                    if (SensorActivity.custom_pressure) av_pressure.isVisible = true
                    med_p1.isVisible = false
                    med_p2.isVisible = false
                    med_temp.isVisible = false
                    med_humidity.isVisible = false
                    med_pressure.isVisible = false
                    chart.invalidate()
                }
            }
            custom_median.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    av_p1.isVisible = false
                    av_p2.isVisible = false
                    av_temp.isVisible = false
                    av_humidity.isVisible = false
                    av_pressure.isVisible = false
                    if (SensorActivity.custom_p1) med_p1.isVisible = true
                    if (SensorActivity.custom_p2) med_p2.isVisible = true
                    if (SensorActivity.custom_temp) med_temp.isVisible = true
                    if (SensorActivity.custom_humidity) med_humidity.isVisible = true
                    if (SensorActivity.custom_pressure) med_pressure.isVisible = true
                    chart.invalidate()
                }
            }
            customThresholdNothing.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    th_eu_p1.isVisible = false
                    th_eu_p2.isVisible = false
                    th_who_p1.isVisible = false
                    th_who_p2.isVisible = false
                    chart.invalidate()
                }
            }
            custom_threshold_who.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    th_eu_p1.isVisible = false
                    th_eu_p2.isVisible = false
                    if (SensorActivity.custom_p1) th_who_p1.isVisible = true
                    if (SensorActivity.custom_p2) th_who_p2.isVisible = true
                    chart.invalidate()
                }
            }
            custom_threshold_eu.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    if (SensorActivity.custom_p1) th_eu_p1.isVisible = true
                    if (SensorActivity.custom_p2) th_eu_p2.isVisible = true
                    th_who_p1.isVisible = false
                    th_who_p2.isVisible = false
                    chart.invalidate()
                }
            }

            cv_p1 = contentView.findViewById(R.id.cv_p1)
            cv_p2 = contentView.findViewById(R.id.cv_p2)
            cv_temp = contentView.findViewById(R.id.cv_temp)
            cv_humidity = contentView.findViewById(R.id.cv_humidity)
            cv_pressure = contentView.findViewById(R.id.cv_pressure)
            cv_time = contentView.findViewById(R.id.cv_time)

            listener.onDiagramFragmentLoaded(contentView.findViewById(R.id.diagram_container))

            return contentView
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            try {
                listener = context as OnFragmentsLoadedListener
            } catch (e: ClassCastException) {
                throw ClassCastException("$context must implement OnCompleteListener")
            }
        }

        companion object {
            // Variables as objects
            private lateinit var contentView: View
            private lateinit var chart: LineChart
            private lateinit var custom_p1: CheckBox
            private lateinit var custom_p2: CheckBox
            private lateinit var custom_temp: CheckBox
            private lateinit var custom_humidity: CheckBox
            private lateinit var custom_pressure: CheckBox
            private lateinit var custom_average: RadioButton
            private lateinit var custom_median: RadioButton
            private lateinit var custom_threshold_who: RadioButton
            private lateinit var custom_threshold_eu: RadioButton
            private lateinit var p1: LineDataSet
            private lateinit var p2: LineDataSet
            private lateinit var temp: LineDataSet
            private lateinit var humidity: LineDataSet
            private lateinit var pressure: LineDataSet
            private lateinit var av_p1: LineDataSet
            private lateinit var av_p2: LineDataSet
            private lateinit var av_temp: LineDataSet
            private lateinit var av_humidity: LineDataSet
            private lateinit var av_pressure: LineDataSet
            private lateinit var med_p1: LineDataSet
            private lateinit var med_p2: LineDataSet
            private lateinit var med_temp: LineDataSet
            private lateinit var med_humidity: LineDataSet
            private lateinit var med_pressure: LineDataSet
            private lateinit var th_eu_p1: LineDataSet
            private lateinit var th_eu_p2: LineDataSet
            private lateinit var th_who_p1: LineDataSet
            private lateinit var th_who_p2: LineDataSet
            private val dataSets = ArrayList<ILineDataSet>()
            private var dataSetsFull = ArrayList<ILineDataSet>()
            private lateinit var cv_p1: TextView
            private lateinit var cv_p2: TextView
            private lateinit var cv_temp: TextView
            private lateinit var cv_humidity: TextView
            private lateinit var cv_pressure: TextView
            private lateinit var cv_time: TextView

            private fun updateLastValues() {
                if (SensorActivity.records.size > 0 && SensorActivity.selected_day_timestamp == SensorActivity.current_day_timestamp) {
                    cv_p1.text = SensorActivity.records[SensorActivity.records.size - 1].p1.toString() + " µg/m³"
                    cv_p2.text = SensorActivity.records[SensorActivity.records.size - 1].p2.toString() + " µg/m³"
                    cv_temp.text = SensorActivity.records[SensorActivity.records.size - 1].temp.toString() + " °C"
                    cv_humidity.text = SensorActivity.records[SensorActivity.records.size - 1].humidity.toString() + " %"
                    cv_pressure.text = Tools.round(SensorActivity.records[SensorActivity.records.size - 1].pressure, 3).toString() + " hPa"
                    val sdfDate = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    cv_time.text = activity.getString(R.string.state_of_) + " " + sdfDate.format(SensorActivity.records[SensorActivity.records.size - 1].dateTime)

                    contentView.findViewById<View>(R.id.title_current_values).visibility = View.VISIBLE
                    contentView.findViewById<View>(R.id.cv_container).visibility = View.VISIBLE
                } else {
                    contentView.findViewById<View>(R.id.title_current_values).visibility = View.GONE
                    contentView.findViewById<View>(R.id.cv_container).visibility = View.GONE
                }
            }

            private fun showGraph(index: Int, show: Boolean) {
                dataSets[index].isVisible = show
                chart.fitScreen()
                chart.invalidate()
            }

            private fun getAverageMedianPM1(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
                val amEntries = ArrayList<Entry>()
                if (enable_average) {
                    var average = 0.0
                    for (record in records!!) average += record.p1
                    average /= records!!.size.toDouble()
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                } else if (enable_median) {
                    val doubleRecords = ArrayList<Double>()
                    for (record in records!!) doubleRecords.add(record.p1)
                    val median = Tools.calculateMedian(doubleRecords)
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                }
                val p1Am =
                    getDashedLine(
                        amEntries,
                        R.color.series1
                    )
                p1Am.isVisible = if (enable_average) custom_average.isChecked && custom_p1.isChecked else custom_median.isChecked && custom_p1.isChecked
                p1Am.axisDependency = YAxis.AxisDependency.LEFT
                return p1Am
            }

            private fun getAverageMedianPM2(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
                val amEntries: MutableList<Entry>
                amEntries = ArrayList()
                if (enable_average) {
                    var average = 0.0
                    for (record in records!!) average += record.p2
                    average /= records!!.size.toDouble()
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                } else if (enable_median) {
                    val doubleRecords = ArrayList<Double>()
                    for (record in records!!) doubleRecords.add(record.p2)
                    val median = Tools.calculateMedian(doubleRecords)
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                }
                val p2Am =
                    getDashedLine(
                        amEntries,
                        R.color.series2
                    )
                p2Am.isVisible = if (enable_average) custom_average.isChecked && custom_p2.isChecked else custom_median.isChecked && custom_p2.isChecked
                p2Am.axisDependency = YAxis.AxisDependency.LEFT
                return p2Am
            }

            private fun getAverageMedianTemperature(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
                val amEntries: MutableList<Entry>
                amEntries = ArrayList()
                if (enable_average) {
                    var average = 0.0
                    for (record in records!!) average += record.temp
                    average /= records!!.size.toDouble()
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                } else if (enable_median) {
                    val doubleRecords = ArrayList<Double>()
                    for (record in records!!) doubleRecords.add(record.temp)
                    val median = Tools.calculateMedian(doubleRecords)
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                }
                val tempAm =
                    getDashedLine(
                        amEntries,
                        R.color.series3
                    )
                tempAm.isVisible = if (enable_average) custom_average.isChecked && custom_temp.isChecked else custom_median.isChecked && custom_temp.isChecked
                tempAm.axisDependency = YAxis.AxisDependency.RIGHT
                return tempAm
            }

            private fun getAverageMedianHumidity(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
                val amEntries: MutableList<Entry>
                amEntries = ArrayList()
                if (enable_average) {
                    var average = 0.0
                    for (record in records!!) average += record.humidity
                    average /= records!!.size.toDouble()
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                } else if (enable_median) {
                    val doubleRecords = ArrayList<Double>()
                    for (record in records!!) doubleRecords.add(record.humidity)
                    val median = Tools.calculateMedian(doubleRecords)
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                }
                val humidityAm =
                    getDashedLine(
                        amEntries,
                        R.color.series4
                    )
                humidityAm.isVisible = if (enable_average) custom_average.isChecked && custom_humidity.isChecked else custom_median.isChecked && custom_humidity.isChecked
                humidityAm.axisDependency = YAxis.AxisDependency.RIGHT
                return humidityAm
            }

            private fun getAverageMedianPressure(enable_average: Boolean, enable_median: Boolean, first_timestamp: Long): LineDataSet {
                val amEntries: MutableList<Entry>
                amEntries = ArrayList()
                if (enable_average) {
                    var average = 0.0
                    for (record in records!!) average += record.pressure
                    average /= records!!.size.toDouble()
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), average.toFloat()))
                } else if (enable_median) {
                    val doubleRecords = ArrayList<Double>()
                    for (record in records!!) doubleRecords.add(record.pressure)
                    val median = Tools.calculateMedian(doubleRecords)
                    amEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                    amEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), median.toFloat()))
                }
                val pressureAm =
                    getDashedLine(
                        amEntries,
                        R.color.series5
                    )
                pressureAm.isVisible = if (enable_average) custom_average.isChecked && custom_pressure.isChecked else custom_median.isChecked && custom_pressure.isChecked
                pressureAm.axisDependency = YAxis.AxisDependency.RIGHT
                return pressureAm
            }

            private fun getThresholdPM1(enable_eu_thresholds: Boolean, enable_who_thresholds: Boolean, first_timestamp: Long): LineDataSet {
                val thEntries: MutableList<Entry>
                thEntries = ArrayList()
                if (enable_eu_thresholds) {
                    thEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM10.toFloat()))
                    thEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM10.toFloat()))
                } else if (enable_who_thresholds) {
                    thEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM10.toFloat()))
                    thEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM10.toFloat()))
                }
                val thP1 =
                    getDashedLine(
                        thEntries,
                        R.color.error
                    )
                thP1.isVisible = if (enable_eu_thresholds) custom_threshold_eu.isChecked && custom_p1.isChecked else custom_threshold_who.isChecked && custom_p1.isChecked
                return thP1
            }

            private fun getThresholdPM2(enable_eu_thresholds: Boolean, enable_who_thresholds: Boolean, first_timestamp: Long): LineDataSet {
                val thEntries: MutableList<Entry>
                thEntries = ArrayList()
                if (enable_eu_thresholds) {
                    thEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM2_5.toFloat()))
                    thEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_EU_PM2_5.toFloat()))
                } else if (enable_who_thresholds) {
                    thEntries.add(Entry(((records!![0].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM2_5.toFloat()))
                    thEntries.add(Entry(((records!![records!!.size - 1].dateTime.time - first_timestamp) / 1000).toFloat(), Constants.THRESHOLD_WHO_PM2_5.toFloat()))
                }
                val thP2 =
                    getDashedLine(
                        thEntries,
                        R.color.error
                    )
                thP2.isVisible = if (enable_eu_thresholds) custom_threshold_eu.isChecked && custom_p2.isChecked else custom_threshold_who.isChecked && custom_p2.isChecked
                return thP2
            }

            private fun getDashedLine(am_entries: List<Entry>, color: Int): LineDataSet {
                val dl = LineDataSet(am_entries, null)
                dl.color = ContextCompat.getColor(activity, color)
                dl.lineWidth = 1f
                dl.setDrawValues(false)
                dl.setDrawCircles(false)
                dl.isHighlightEnabled = false
                dl.enableDashedLine(10f, 10f, 0f)
                return dl
            }

            fun refresh() {
                if (records != null) {
                    contentView.findViewById<View>(R.id.loading).visibility = View.GONE
                    if (records!!.size > 0) {
                        contentView.findViewById<View>(R.id.no_data).visibility = View.GONE
                        contentView.findViewById<View>(R.id.diagram_container).visibility = View.VISIBLE

                        // Sort records
                        val tmp = SensorActivity.sort_mode
                        SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC
                        records!!.sort()
                        SensorActivity.sort_mode = tmp

                        // Plot data
                        val entries1 = ArrayList<Entry>()
                        val entries2 = ArrayList<Entry>()
                        val entries3 = ArrayList<Entry>()
                        val entries4 = ArrayList<Entry>()
                        val entries5 = ArrayList<Entry>()
                        val firstTime = records!![0].dateTime.time
                        chart.xAxis.valueFormatter = TimeFormatter(firstTime)
                        for (r in records!!) {
                            entries1.add(DiagramEntry((r.dateTime.time - firstTime) / 1000.0, r.p1, "µg/m³"))
                            entries2.add(DiagramEntry((r.dateTime.time - firstTime) / 1000.0, r.p2, "µg/m³"))
                            entries3.add(DiagramEntry((r.dateTime.time - firstTime) / 1000.0, r.temp, "°C"))
                            entries4.add(DiagramEntry((r.dateTime.time - firstTime) / 1000.0, r.humidity, "%"))
                            entries5.add(DiagramEntry((r.dateTime.time - firstTime) / 1000.0, r.pressure, "hPa"))
                        }

                        // Generic lines
                        // PM1
                        p1 = LineDataSet(entries1, activity.getString(R.string.value1) + " (µg/m³)")
                        p1.color = ContextCompat.getColor(
                            activity, R.color.series1)
                        p1.setDrawCircles(false)
                        p1.lineWidth = 1.5f
                        p1.setDrawValues(false)
                        p1.axisDependency = YAxis.AxisDependency.LEFT
                        p1.isVisible = SensorActivity.custom_p1

                        // PM2
                        p2 = LineDataSet(entries2, activity.getString(R.string.value2) + " (µg/m³)")
                        p2.color = ContextCompat.getColor(
                            activity, R.color.series2)
                        p2.setDrawCircles(false)
                        p2.lineWidth = 1.5f
                        p2.setDrawValues(false)
                        p2.axisDependency = YAxis.AxisDependency.LEFT
                        p2.isVisible = SensorActivity.custom_p2

                        // Temperature
                        temp = LineDataSet(entries3, activity.getString(R.string.temperature) + " (°C)")
                        temp.color = ContextCompat.getColor(
                            activity, R.color.series3)
                        temp.setDrawCircles(false)
                        temp.lineWidth = 1.5f
                        temp.setDrawValues(false)
                        temp.axisDependency = YAxis.AxisDependency.RIGHT
                        temp.isVisible = SensorActivity.custom_temp

                        // Humidity
                        humidity = LineDataSet(entries4, activity.getString(R.string.humidity) + " (%)")
                        humidity.color = ContextCompat.getColor(
                            activity, R.color.series4)
                        humidity.setDrawCircles(false)
                        humidity.lineWidth = 1.5f
                        humidity.setDrawValues(false)
                        humidity.axisDependency = YAxis.AxisDependency.RIGHT
                        humidity.isVisible = SensorActivity.custom_humidity

                        // Pressure
                        pressure = LineDataSet(entries5, activity.getString(R.string.pressure) + " (hPa)")
                        pressure.color = ContextCompat.getColor(
                            activity, R.color.series5)
                        pressure.setDrawCircles(false)
                        pressure.lineWidth = 1.5f
                        pressure.setDrawValues(false)
                        pressure.axisDependency = YAxis.AxisDependency.RIGHT
                        pressure.isVisible = SensorActivity.custom_pressure

                        // Averages
                        av_p1 =
                            getAverageMedianPM1(
                                enable_average = true,
                                enable_median = false,
                                first_timestamp = firstTime
                            )
                        av_p2 =
                            getAverageMedianPM2(
                                enable_average = true,
                                enable_median = false,
                                first_timestamp = firstTime
                            )
                        av_temp =
                            getAverageMedianTemperature(
                                enable_average = true,
                                enable_median = false,
                                first_timestamp = firstTime
                            )
                        av_humidity =
                            getAverageMedianHumidity(
                                enable_average = true,
                                enable_median = false,
                                first_timestamp = firstTime
                            )
                        av_pressure =
                            getAverageMedianPressure(
                                enable_average = true,
                                enable_median = false,
                                first_timestamp = firstTime
                            )

                        // Medians
                        med_p1 =
                            getAverageMedianPM1(
                                enable_average = false,
                                enable_median = true,
                                first_timestamp = firstTime
                            )
                        med_p2 =
                            getAverageMedianPM2(
                                enable_average = false,
                                enable_median = true,
                                first_timestamp = firstTime
                            )
                        med_temp =
                            getAverageMedianTemperature(
                                enable_average = false,
                                enable_median = true,
                                first_timestamp = firstTime
                            )
                        med_humidity =
                            getAverageMedianHumidity(
                                enable_average = false,
                                enable_median = true,
                                first_timestamp = firstTime
                            )
                        med_pressure =
                            getAverageMedianPressure(
                                enable_average = false,
                                enable_median = true,
                                first_timestamp = firstTime
                            )

                        // Thresholds
                        th_eu_p1 =
                            getThresholdPM1(
                                enable_eu_thresholds = true,
                                enable_who_thresholds = false,
                                first_timestamp = firstTime
                            )
                        th_eu_p2 =
                            getThresholdPM2(
                                enable_eu_thresholds = true,
                                enable_who_thresholds = false,
                                first_timestamp = firstTime
                            )
                        th_who_p1 =
                            getThresholdPM1(
                                enable_eu_thresholds = false,
                                enable_who_thresholds = true,
                                first_timestamp = firstTime
                            )
                        th_who_p2 =
                            getThresholdPM2(
                                enable_eu_thresholds = false,
                                enable_who_thresholds = true,
                                first_timestamp = firstTime
                            )

                        // Combine all lines to one diagram
                        dataSets.clear()
                        dataSets.add(
                            p1
                        )
                        dataSets.add(
                            p2
                        )
                        dataSets.add(
                            temp
                        )
                        dataSets.add(
                            humidity
                        )
                        dataSets.add(
                            pressure
                        )
                        dataSets.add(
                            av_p1
                        )
                        dataSets.add(
                            av_p2
                        )
                        dataSets.add(
                            av_temp
                        )
                        dataSets.add(
                            av_humidity
                        )
                        dataSets.add(
                            av_pressure
                        )
                        dataSets.add(
                            med_p1
                        )
                        dataSets.add(
                            med_p2
                        )
                        dataSets.add(
                            med_temp
                        )
                        dataSets.add(
                            med_humidity
                        )
                        dataSets.add(
                            med_pressure
                        )
                        dataSets.add(
                            th_eu_p1
                        )
                        dataSets.add(
                            th_eu_p2
                        )
                        dataSets.add(
                            th_who_p1
                        )
                        dataSets.add(
                            th_who_p2
                        )
                        dataSetsFull = dataSets.clone() as ArrayList<ILineDataSet>
                        chart.data = LineData(
                            dataSets
                        )

                        // Redraw and animate
                        chart.invalidate()
                        chart.animateY(700, Easing.EaseInCubic)
                    } else {
                        contentView.findViewById<View>(R.id.diagram_container).visibility = View.GONE
                        contentView.findViewById<View>(R.id.no_data).visibility = View.VISIBLE
                    }
                    updateLastValues()
                } else {
                    contentView.findViewById<View>(R.id.diagram_container).visibility = View.GONE
                    contentView.findViewById<View>(R.id.no_data).visibility = View.VISIBLE
                }
            }

            internal fun exportDiagram() {
                su.shareImage(
                    chart.chartBitmap, activity.getString(R.string.export_diagram))
            }
        }
    }

    class DataFragment : Fragment() {
        private var dataViewManager: RecyclerView.LayoutManager? = null
        private lateinit var headingTime: TextView
        private lateinit var headingTimeArrow: ImageView
        private lateinit var headingP1: TextView
        private lateinit var headingP1Arrow: ImageView
        private lateinit var headingP2: TextView
        private lateinit var headingP2Arrow: ImageView
        private lateinit var headingTemp: TextView
        private lateinit var headingTempArrow: ImageView
        private lateinit var headingHumidity: TextView
        private lateinit var headingHumidityArrow: ImageView
        private lateinit var headingPressure: TextView
        private lateinit var headingPressureArrow: ImageView

        override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
            contentView = LayoutInflater.from(parent?.context).inflate(R.layout.tab_data, parent, false)

            // Initialize components
            data_view_adapter =
                DataAdapter()
            data_view = contentView.findViewById(R.id.data)
            dataViewManager = LinearLayoutManager(context)
            data_view.layoutManager = dataViewManager
            data_view.adapter =
                data_view_adapter
            data_view.setHasFixedSize(true)
            if (records != null) {
                contentView.findViewById<View>(R.id.loading).visibility = View.GONE
                contentView.findViewById<View>(R.id.no_data).visibility = if (records!!.size == 0) View.VISIBLE else View.GONE
                contentView.findViewById<View>(R.id.data_footer).visibility = if (records!!.size == 0) View.INVISIBLE else View.VISIBLE

                heading = contentView.findViewById(R.id.data_heading)
                headingTime = contentView.findViewById(R.id.heading_time)
                headingTimeArrow = contentView.findViewById(R.id.sort_time)
                headingP1 = contentView.findViewById(R.id.heading_p1)
                headingP1Arrow = contentView.findViewById(R.id.sort_p1)
                headingP2 = contentView.findViewById(R.id.heading_p2)
                headingP2Arrow = contentView.findViewById(R.id.sort_p2)
                headingTemp = contentView.findViewById(R.id.heading_temp)
                headingTempArrow = contentView.findViewById(R.id.sort_temp)
                headingHumidity = contentView.findViewById(R.id.heading_humidity)
                headingHumidityArrow = contentView.findViewById(R.id.sort_humidity)
                headingPressure = contentView.findViewById(R.id.heading_pressure)
                headingPressureArrow = contentView.findViewById(R.id.sort_pressure)

                footer = contentView.findViewById(R.id.data_footer)
                footer_average_p1 = contentView.findViewById(R.id.footer_average_p1)
                footer_average_p2 = contentView.findViewById(R.id.footer_average_p2)
                footer_average_temp = contentView.findViewById(R.id.footer_average_temp)
                footer_average_humidity = contentView.findViewById(R.id.footer_average_humidity)
                footer_average_pressure = contentView.findViewById(R.id.footer_average_pressure)
                footer_median_p1 = contentView.findViewById(R.id.footer_median_p1)
                footer_median_p2 = contentView.findViewById(R.id.footer_median_p2)
                footer_median_temp = contentView.findViewById(R.id.footer_median_temp)
                footer_median_humidity = contentView.findViewById(R.id.footer_median_humidity)
                footer_median_pressure = contentView.findViewById(R.id.footer_median_pressure)
                record_counter = contentView.findViewById(R.id.record_counter)

                headingTime.setOnClickListener { timeSortClicked() }
                headingTimeArrow.setOnClickListener { timeSortClicked() }
                headingP1.setOnClickListener { p1SortClicked() }
                headingP1Arrow.setOnClickListener { p1SortClicked() }
                headingP2.setOnClickListener { p2SortClicked() }
                headingP2Arrow.setOnClickListener { p2SortClicked() }
                headingTemp.setOnClickListener { tempSortClicked() }
                headingTempArrow.setOnClickListener { tempSortClicked() }
                headingHumidity.setOnClickListener { humiditySortClicked() }
                headingHumidityArrow.setOnClickListener { humiditySortClicked() }
                headingPressure.setOnClickListener { pressureSortClicked() }
                headingPressureArrow.setOnClickListener { pressureSortClicked() }
            }

            showGPSData(
                show_gps_data
            )

            listener.onDataFragmentLoaded(
                record_counter
            )

            return contentView
        }

        override fun onAttach(context: Context) {
            super.onAttach(context)
            try {
                listener = context as OnFragmentsLoadedListener
            } catch (e: ClassCastException) {
                throw ClassCastException("$context must implement OnCompleteListener")
            }
        }

        private fun timeSortClicked() {
            headingTimeArrow.visibility = View.VISIBLE
            headingP1Arrow.visibility = View.INVISIBLE
            headingP2Arrow.visibility = View.INVISIBLE
            headingTempArrow.visibility = View.INVISIBLE
            headingHumidityArrow.visibility = View.INVISIBLE
            headingPressureArrow.visibility = View.INVISIBLE
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_DESC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_ASC
                headingTimeArrow.setImageResource(R.drawable.arrow_drop_up_grey)
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TIME_DESC
                headingTimeArrow.setImageResource(R.drawable.arrow_drop_down_grey)
            }
            SensorActivity.resortData()
            data_view.adapter!!.notifyDataSetChanged()
        }

        private fun p1SortClicked() {
            headingTimeArrow.visibility = View.INVISIBLE
            headingP1Arrow.visibility = View.VISIBLE
            headingP2Arrow.visibility = View.INVISIBLE
            headingTempArrow.visibility = View.INVISIBLE
            headingHumidityArrow.visibility = View.INVISIBLE
            headingPressureArrow.visibility = View.INVISIBLE
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_DESC
                headingP1Arrow.setImageResource(R.drawable.arrow_drop_down_grey)
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_ASC
                headingP1Arrow.setImageResource(R.drawable.arrow_drop_up_grey)
            }
            SensorActivity.resortData()
            data_view.adapter!!.notifyDataSetChanged()
        }

        private fun p2SortClicked() {
            headingTimeArrow.visibility = View.INVISIBLE
            headingP1Arrow.visibility = View.INVISIBLE
            headingP2Arrow.visibility = View.VISIBLE
            headingTempArrow.visibility = View.INVISIBLE
            headingHumidityArrow.visibility = View.INVISIBLE
            headingPressureArrow.visibility = View.INVISIBLE
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_DESC
                headingP2Arrow.setImageResource(R.drawable.arrow_drop_down_grey)
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_ASC
                headingP2Arrow.setImageResource(R.drawable.arrow_drop_up_grey)
            }
            SensorActivity.resortData()
            data_view.adapter!!.notifyDataSetChanged()
        }

        private fun tempSortClicked() {
            headingTimeArrow.visibility = View.INVISIBLE
            headingP1Arrow.visibility = View.INVISIBLE
            headingP2Arrow.visibility = View.INVISIBLE
            headingTempArrow.visibility = View.VISIBLE
            headingHumidityArrow.visibility = View.INVISIBLE
            headingPressureArrow.visibility = View.INVISIBLE
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TEMP_DESC
                headingTempArrow.setImageResource(R.drawable.arrow_drop_down_grey)
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TEMP_ASC
                headingTempArrow.setImageResource(R.drawable.arrow_drop_up_grey)
            }
            SensorActivity.resortData()
            data_view.adapter!!.notifyDataSetChanged()
        }

        private fun humiditySortClicked() {
            headingTimeArrow.visibility = View.INVISIBLE
            headingP1Arrow.visibility = View.INVISIBLE
            headingP2Arrow.visibility = View.INVISIBLE
            headingTempArrow.visibility = View.INVISIBLE
            headingHumidityArrow.visibility = View.VISIBLE
            headingPressureArrow.visibility = View.INVISIBLE
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_HUMIDITY_DESC
                headingHumidityArrow.setImageResource(R.drawable.arrow_drop_down_grey)
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_HUMIDITY_ASC
                headingHumidityArrow.setImageResource(R.drawable.arrow_drop_up_grey)
            }
            SensorActivity.resortData()
            data_view.adapter!!.notifyDataSetChanged()
        }

        private fun pressureSortClicked() {
            headingTimeArrow.visibility = View.INVISIBLE
            headingP1Arrow.visibility = View.INVISIBLE
            headingP2Arrow.visibility = View.INVISIBLE
            headingTempArrow.visibility = View.INVISIBLE
            headingHumidityArrow.visibility = View.INVISIBLE
            headingPressureArrow.visibility = View.VISIBLE
            if (SensorActivity.sort_mode == SensorActivity.SORT_MODE_PRESSURE_ASC) {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_PRESSURE_DESC
                headingPressureArrow.setImageResource(R.drawable.arrow_drop_down_grey)
            } else {
                SensorActivity.sort_mode = SensorActivity.SORT_MODE_PRESSURE_ASC
                headingPressureArrow.setImageResource(R.drawable.arrow_drop_up_grey)
            }
            SensorActivity.resortData()
            data_view.adapter!!.notifyDataSetChanged()
        }

        companion object {
            // Variables as objects
            private lateinit var contentView: View
            private lateinit var data_view: RecyclerView
            private lateinit var data_view_adapter: DataAdapter
            private lateinit var heading: LinearLayout
            private lateinit var footer: RelativeLayout
            private lateinit var footer_average_p1: TextView
            private lateinit var footer_average_p2: TextView
            private lateinit var footer_average_temp: TextView
            private lateinit var footer_average_humidity: TextView
            private lateinit var footer_average_pressure: TextView
            private lateinit var footer_median_p1: TextView
            private lateinit var footer_median_p2: TextView
            private lateinit var footer_median_temp: TextView
            private lateinit var footer_median_humidity: TextView
            private lateinit var footer_median_pressure: TextView
            private lateinit var record_counter: TextView

            internal fun showGPSData(show: Boolean) {
                contentView.findViewById<View>(R.id.heading_gps).visibility = if (show) View.VISIBLE else View.GONE
                contentView.findViewById<View>(R.id.footer_average_gps).visibility = if (show) View.VISIBLE else View.GONE
                contentView.findViewById<View>(R.id.footer_median_gps).visibility = if (show) View.VISIBLE else View.GONE
                data_view.layoutParams.width = round(
                    activity.resources.displayMetrics.density * if (show) 830 else 530).toInt()
                heading.layoutParams.width = round(
                    activity.resources.displayMetrics.density * if (show) 830 else 530).toInt()
                footer.layoutParams.width = round(
                    activity.resources.displayMetrics.density * if (show) 830 else 530).toInt()
                data_view_adapter.showGPSData(show)
            }

            fun refresh() {
                try {
                    if (records != null) {
                        data_view_adapter.notifyDataSetChanged()

                        contentView.findViewById<View>(R.id.loading).visibility = View.GONE
                        contentView.findViewById<View>(R.id.no_data).visibility = if (records!!.size == 0) View.VISIBLE else View.GONE

                        if (records!!.size > 0) {
                            record_counter.visibility = View.VISIBLE
                            contentView.findViewById<View>(R.id.data_heading).visibility = View.VISIBLE
                            contentView.findViewById<View>(R.id.data_footer).visibility = View.VISIBLE
                            contentView.findViewById<View>(R.id.data_footer_average).visibility = if (su.getBoolean("enable_daily_average", true)) View.VISIBLE else View.GONE
                            contentView.findViewById<View>(R.id.data_footer_median).visibility = if (su.getBoolean("enable_daily_median", false)) View.VISIBLE else View.GONE
                            val footerString = records!!.size.toString() + " " + activity.getString(R.string.tab_data) + " - " + activity.getString(R.string.from) + " " + df_time.format(
                                records!![0].dateTime) + " " + activity.getString(R.string.to) + " " + df_time.format(
                                records!![records!!.size - 1].dateTime)
                            record_counter.text = footerString

                            if (su.getBoolean("enable_daily_average", true)) {
                                // Calculate averages
                                var averageP1 = 0.0
                                var averageP2 = 0.0
                                var averageTemp = 0.0
                                var averageHumidity = 0.0
                                var averagePressure = 0.0
                                for (record in records!!) {
                                    averageP1 += record.p1
                                    averageP2 += record.p2
                                    averageTemp += record.temp
                                    averageHumidity += record.humidity
                                    averagePressure += record.pressure
                                }
                                averageP1 /= records!!.size
                                averageP2 /= records!!.size
                                averageTemp /= records!!.size
                                averageHumidity /= records!!.size
                                averagePressure /= records!!.size

                                footer_average_p1.text = Tools.round(averageP1, 1).toString().replace(".", ",") + " µg/m³"
                                footer_average_p2.text = Tools.round(averageP2, 1).toString().replace(".", ",") + " µg/m³"
                                footer_average_temp.text = Tools.round(averageTemp, 1).toString().replace(".", ",") + " °C"
                                footer_average_humidity.text = Tools.round(averageHumidity, 1).toString().replace(".", ",") + " %"
                                footer_average_pressure.text = Tools.round(averagePressure, 1).toString().replace(".", ",") + " hPa"
                            }

                            if (su.getBoolean("enable_daily_median")) {
                                // Calculate medians
                                val medianP1: Double = records!![records!!.size / 2].p1
                                val medianP2: Double = records!![records!!.size / 2].p2
                                val medianTemp: Double = records!![records!!.size / 2].temp
                                val medianHumidity: Double = records!![records!!.size / 2].humidity
                                val medianPressure: Double = records!![records!!.size / 2].pressure
                                // Save current sort mode
                                val currentSortMode = SensorActivity.sort_mode
                                // P1
                                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE1_ASC
                                records!!.sort()
                                // P2
                                SensorActivity.sort_mode = SensorActivity.SORT_MODE_VALUE2_ASC
                                records!!.sort()
                                // Temperature
                                SensorActivity.sort_mode = SensorActivity.SORT_MODE_TEMP_ASC
                                records!!.sort()
                                // Humidity
                                SensorActivity.sort_mode = SensorActivity.SORT_MODE_HUMIDITY_ASC
                                records!!.sort()
                                // Pressure
                                SensorActivity.sort_mode = SensorActivity.SORT_MODE_PRESSURE_ASC
                                records!!.sort()
                                // Restore old sort mode
                                SensorActivity.sort_mode = currentSortMode
                                records!!.sort()

                                footer_median_p1.text = Tools.round(medianP1, 1).toString().replace(".", ",") + " µg/m³"
                                footer_median_p2.text = Tools.round(medianP2, 1).toString().replace(".", ",") + " µg/m³"
                                footer_median_temp.text = Tools.round(medianTemp, 1).toString().replace(".", ",") + " °C"
                                footer_median_humidity.text = Tools.round(medianHumidity, 1).toString().replace(".", ",") + " %"
                                footer_median_pressure.text = Tools.round(medianPressure, 1).toString().replace(".", ",") + " hPa"
                            }
                        } else {
                            contentView.findViewById<View>(R.id.data_heading).visibility = View.INVISIBLE
                            contentView.findViewById<View>(R.id.data_footer).visibility = View.INVISIBLE
                        }
                    } else {
                        contentView.findViewById<View>(R.id.data_heading).visibility = View.INVISIBLE
                        contentView.findViewById<View>(R.id.data_footer).visibility = View.INVISIBLE
                        record_counter.visibility = View.INVISIBLE
                        contentView.findViewById<View>(R.id.no_data).visibility = View.VISIBLE
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    companion object {
        // Variables as objects
        private lateinit var activity: SensorActivity
        private lateinit var h: Handler
        var records: ArrayList<DataRecord>? = ArrayList()
        private val df_time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        private lateinit var listener: OnFragmentsLoadedListener

        // Utils packages
        private lateinit var su: StorageUtils

        // Variables
        private var show_gps_data: Boolean = false
    }
}