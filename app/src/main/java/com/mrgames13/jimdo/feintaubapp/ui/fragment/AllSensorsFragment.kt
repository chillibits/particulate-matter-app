/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.shared.getPrefs
import com.mrgames13.jimdo.feintaubapp.shared.outputErrorMessage
import kotlinx.android.synthetic.main.fragment_all_sensors.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AllSensorsFragment(private val listener: OnAdapterEventListener) : Fragment(), OnMapReadyCallback {

    // Variables as objects
    private lateinit var mapFragment: SupportMapFragment
    private var map: GoogleMap? = null

    // Variables

    // Interfaces
    interface OnAdapterEventListener

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        inflater.inflate(R.layout.fragment_all_sensors, container, false).run {
            // Initialize Spinners
            val mapTypeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, resources.getStringArray(R.array.map_type))
            mapTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mapType.adapter = mapTypeAdapter
            mapType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    // Set selected map type
                    when (pos) {
                        0 -> map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        1 -> map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                        2 -> map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        3 -> map?.mapType = GoogleMap.MAP_TYPE_HYBRID
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            mapType.setSelection(context.getPrefs().getString("default_map_type", 0.toString()).toString().toInt())

            val mapTrafficAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, listOf(getString(R.string.traffic_hide), getString(R.string.traffic_show)))
            mapTrafficAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mapTraffic.adapter = mapTrafficAdapter
            mapTraffic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    // Enable/disable traffic on the map
                    map?.isTrafficEnabled = pos != 0
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            mapTraffic.setSelection(if(context.getPrefs().getBoolean("default_traffic", false)) 1 else 0)

            // Initialize sensor number display
            mapSensorCount.setOnClickListener {
                Intent(Intent.ACTION_VIEW).run {
                    data = Uri.parse("https://h2801469.stratoserver.net/stats.php")
                    startActivity(this)
                }
            }

            // Initialize refresh button
            mapRefresh.setOnClickListener {
                TODO("not implemented")
            }

            // Initialize ranking button
            mapRanking.setOnClickListener {
                displayRankingDialog()
            }

            // Initialize map
            mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this@AllSensorsFragment)

            return this
        }
    }

    @SuppressLint("ResourceType")
    override fun onMapReady(map: GoogleMap?) {
        if(map != null) {
            this.map = map
            // Initialize map ui
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isZoomControlsEnabled = true

            // Relocate MyLocationButton
            val locationButton = (mapFragment.view?.findViewById<View>("1".toInt())?.parent as View).findViewById<View>("2".toInt())
            val rlp = locationButton.layoutParams as RelativeLayout.LayoutParams
            rlp.run {
                addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                setMargins(0, 0, 27, 430)
            }

            // Relocate zoom button
            val zoomControls = mapFragment.view?.findViewById<View>(0x1)
            if (zoomControls != null && zoomControls.layoutParams is RelativeLayout.LayoutParams) {
                val params = zoomControls.layoutParams as RelativeLayout.LayoutParams
                params.run {
                    addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                    addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                    setMargins(
                        params.leftMargin,
                        params.topMargin,
                        params.rightMargin,
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70f, resources.displayMetrics).toInt()
                    )
                }
            }

            // Apply map style
            val nightModeFlags = requireContext().resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val themeResId = if(nightModeFlags == Configuration.UI_MODE_NIGHT_YES) R.raw.map_style_dark else R.raw.map_style_silver
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, themeResId))
        } else {
            context?.outputErrorMessage()
        }
    }

    private fun displayRankingDialog() {
        val rankingView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ranking, null, false)

        val d = AlertDialog.Builder(requireContext())
            .setTitle(R.string.sensor_highscore)
            .setView(rankingView)
            .setPositiveButton(R.string.ok, null)
            .show()

        CoroutineScope(Dispatchers.IO).launch {
            TODO("not implemented")
        }
    }
}