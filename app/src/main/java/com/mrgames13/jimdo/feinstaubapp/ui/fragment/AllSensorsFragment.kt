/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.ktx.MapsExperimentalFeature
import com.google.maps.android.ktx.addMarker
import com.google.maps.android.ktx.awaitMap
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dbo.ExternalSensorDbo
import com.mrgames13.jimdo.feinstaubapp.network.isLocationEnabled
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.getPreferenceValue
import com.mrgames13.jimdo.feinstaubapp.shared.getPrefs
import com.mrgames13.jimdo.feinstaubapp.shared.isNightModeEnabled
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.ProgressDialog
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showRankingDialog
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showSensorStatsDialog
import com.mrgames13.jimdo.feinstaubapp.ui.item.MarkerItem
import com.mrgames13.jimdo.feinstaubapp.ui.view.*
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_all_sensors.*
import kotlinx.android.synthetic.main.fragment_all_sensors.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ibrahimsn.library.LiveSharedPreferences

class AllSensorsFragment(
    private val application: Application,
    private val listener: OnAdapterEventListener
) : Fragment(), Observer<List<ExternalSensorDbo>> {

    // Variables as objects
    private lateinit var mapFragment: SupportMapFragment
    private var map: GoogleMap? = null
    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var clusterManager: ClusterManager<SensorClusterItem>? = null

    // Interfaces
    interface OnAdapterEventListener {
        fun onToggleFullscreen()
    }

    // Default constructor has to be implemented, otherwise the app crashes on configuration change
    constructor() : this(Application(), object: OnAdapterEventListener {
        override fun onToggleFullscreen() {}
    })

    @MapsExperimentalFeature
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(MainViewModel::class.java)

        return inflater.inflate(R.layout.fragment_all_sensors, container, false).apply {
            // Initialize map
            mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            lifecycle.coroutineScope.launchWhenCreated {
                map = mapFragment.awaitMap()
                onMapReady()
            }

            // Initialize map type spinner
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
                    context.getPrefs().edit().putInt(Constants.RECENT_MAP_TYPE, pos).apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            mapType.setSelection(context.getPrefs().getInt(Constants.RECENT_MAP_TYPE, 0))

            // Initialize traffic spinner
            val mapTrafficAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, listOf(getString(R.string.traffic_hide), getString(R.string.traffic_show)))
            mapTrafficAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mapTraffic.adapter = mapTrafficAdapter
            mapTraffic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    // Enable/disable traffic on the map
                    map?.isTrafficEnabled = pos != 0
                    context.getPrefs().edit().putInt(Constants.RECENT_TRAFFIC, pos).apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            mapTraffic.setSelection(context.getPrefs().getInt(Constants.RECENT_TRAFFIC, 0))

            // Initialize sensor number display
            mapSensorCount.setOnClickListener { context.showSensorStatsDialog(0) }

            // Initialize refresh button
            mapRefresh.setOnClickListener { refresh(false) }

            // Initialize ranking button
            mapRanking.setOnClickListener { showRankingDialog(context, requireFragmentManager(), lifecycle) }
        }
    }

    override fun onStop() {
        super.onStop()
        map?.let {
            // Save viewport to be able to restore it on the next app launch
            val latLng = it.projection.visibleRegion.latLngBounds
            requireContext().getPrefs().edit()
                .putFloat(Constants.RECENT_CAMERA_LAT, latLng.center.latitude.toFloat())
                .putFloat(Constants.RECENT_CAMERA_LNG, latLng.center.longitude.toFloat())
                .putFloat(Constants.RECENT_CAMERA_ZOOM, it.cameraPosition.zoom)
                .apply()
        }
    }

    private fun onMapReady() {
        // Initialize map ui
        map?.uiSettings?.isRotateGesturesEnabled = false
        map?.uiSettings?.isZoomControlsEnabled = true

        // Relocate controls
        relocationOwnLocationControls()
        relocateZoomControls()

        // Enable own location
        enableOwnLocationControls()

        // Add onClickListener for map
        map?.setOnMapClickListener { handleMapClick() }

        // Register observer for live data
        viewModel.externalSensors.observe(viewLifecycleOwner, this)

        // Register observer for preferences keys
        val liveSharedPreferences = LiveSharedPreferences(requireContext().getPrefs())
        liveSharedPreferences.getBoolean(Constants.PREF_ENABLE_MARKER_CLUSTERING, true)
            .observe(viewLifecycleOwner, Observer {
                drawSensors(viewModel.externalSensors.value)
            })
        liveSharedPreferences.getBoolean(Constants.PREF_SHOW_INACTIVE_SENSORS, false)
            .observe(viewLifecycleOwner, Observer {
                viewModel.updateExternalSensorFilter()
            })

        // Apply map style
        val themeResId = if(requireContext().isNightModeEnabled()) R.raw.map_style_dark else R.raw.map_style_silver
        map?.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, themeResId))

        // Initialize cluster manager
        initializeClusterManager()

        // Set the viewport to the recent state
        val prefs = requireContext().getPrefs()
        val latLng = LatLng(
            prefs.getFloat(Constants.RECENT_CAMERA_LAT, 0f).toDouble(),
            prefs.getFloat(Constants.RECENT_CAMERA_LNG, 0f).toDouble()
        )
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, prefs.getFloat(Constants.RECENT_CAMERA_ZOOM, 0f))
        map?.moveCamera(cameraUpdate)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.PERMISSION_LOCATION) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                enableOwnLocationControls()
        }
    }

    private fun refresh(silent: Boolean) {
        if(silent) {
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.manuallyRefreshExternalSensors()
            }
        } else {
            val progressDialog = ProgressDialog(requireContext())
                .setDialogCancelable(false)
                .setMessage(R.string.loading_data)
                .show()
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.manuallyRefreshExternalSensors()
                withContext(Dispatchers.Main) { progressDialog.dismiss() }
            }
        }
    }

    fun applyPlaceSearch(coordinates: LatLng) =
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 11f))

    private fun enableOwnLocationControls() {
        // Check for permission
        val isLocationPermissionGranted =
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if(requireContext().isLocationEnabled()) {
            if(isLocationPermissionGranted) {
                // Show button and register callback
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
                fusedLocationProviderClient.lastLocation.addOnSuccessListener {
                    OnSuccessListener<Location> { location ->
                        if (location != null) moveCameraToPlace(LatLng(location.latitude, location.longitude))
                    }
                }
            } else {
                // Ask for permission
                requestPermissions(arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), Constants.PERMISSION_LOCATION)
            }
        }
    }

    private fun initializeClusterManager() {
        if(requireContext().getPreferenceValue(Constants.PREF_ENABLE_MARKER_CLUSTERING, true)) {
            // Marker clustering is enabled
            clusterManager = ClusterManager(context, map)
            clusterManager?.renderer = SensorClusterRenderer(requireContext(), map!!, clusterManager, viewModel.sensors)
            clusterManager?.setOnClusterItemClickListener { item ->
                showMarkerInfoWindow(item.marker)
                true
            }
            clusterManager?.setOnClusterClickListener {cluster ->
                showClusterInfoWindow(cluster)
                true
            }
            map?.setOnMarkerClickListener(clusterManager)
            map?.setOnCameraIdleListener(clusterManager)
        } else {
            clusterManager = null
            map?.setOnMarkerClickListener {marker ->
                val m = MarkerItem(marker.title, marker.snippet, marker.position)
                viewModel.externalSensors.value?.forEach {
                    if(it.chipId == marker.title.toLong()) m.externalSensor = it
                }
                showMarkerInfoWindow(m)
                true
            }
            map?.setOnCameraIdleListener(null)
        }
    }

    private fun showMarkerInfoWindow(item: MarkerItem) {
        view?.let { view ->
            map?.let { showMarkerInfoWindow(it, view, item, childFragmentManager, viewModel.users.value?.get(0)) }
        }
    }

    private fun showClusterInfoWindow(cluster: Cluster<SensorClusterItem>) {
        view?.let { view ->
            map?.let { showClusterInfoWindow(it, view, cluster) }
        }
    }

    private fun drawSensors(sensors: List<ExternalSensorDbo>?) {
        sensors?.let {
            // Close all open windows
            markerWindow1.isInvisible = true
            markerWindow2.isInvisible = true
            clusterWindow1.isInvisible = true
            clusterWindow2.isInvisible = true

            if(requireContext().getPreferenceValue(Constants.PREF_ENABLE_MARKER_CLUSTERING, true)) {
                // Re-initialize cluster manager if necessary
                if(clusterManager == null) initializeClusterManager()
                clusterManager?.clearItems()
                map?.clear()
                sensors.forEach {
                    if(it.latitude != 0.0 || it.longitude != 0.0)  {
                        val snippet = String.format(getString(R.string.country_city), it.latitude, it.longitude)
                        val position = LatLng(it.latitude, it.longitude)
                        val markerItem = MarkerItem(it.chipId.toString(), snippet, position)
                        markerItem.externalSensor = it
                        clusterManager?.addItem(SensorClusterItem(markerItem, it))
                    }
                }
                clusterManager?.cluster()
            } else {
                // Re-initialize cluster manager if necessary
                if(clusterManager != null) initializeClusterManager()
                // Clear map
                map?.clear()
                // Fill map with new sensors
                sensors.forEach {
                    if(it.latitude != 0.0 || it.longitude != 0.0) {
                        val snippet = String.format(getString(R.string.country_city), it.latitude, it.longitude)
                        val latLng = LatLng(it.latitude, it.longitude)
                        val sensor = viewModel.sensors.value?.find { s -> s.chipId == it.chipId }
                        val markerIcon = BitmapDescriptorFactory.defaultMarker(
                            when {
                                sensor != null && !sensor.isOwner -> BitmapDescriptorFactory.HUE_RED
                                sensor != null && sensor.isOwner -> BitmapDescriptorFactory.HUE_GREEN
                                !it.active -> BitmapDescriptorFactory.HUE_ORANGE
                                else -> BitmapDescriptorFactory.HUE_BLUE
                            }
                        )
                        map?.addMarker {
                            position(latLng)
                            title(it.chipId.toString())
                            snippet(snippet)
                            icon(markerIcon)
                        }
                    }
                }
            }
        }
    }

    private fun handleMapClick() {
        when {
            markerWindow1.isVisible -> exitReveal(markerWindow1)
            markerWindow2.isVisible -> exitReveal(markerWindow2)
            clusterWindow1.isVisible -> exitReveal(clusterWindow1)
            clusterWindow2.isVisible -> exitReveal(clusterWindow2)
            else -> listener.onToggleFullscreen()
        }
    }

    private fun moveCameraToPlace(latLng: LatLng) =
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, Constants.DEFAULT_MAP_ZOOM))

    @SuppressLint("ResourceType")
    private fun relocationOwnLocationControls() {
        val locationButton = (mapFragment.view?.findViewById<View>(0x1)?.parent as View).findViewById<View>(0x2)
        if(locationButton != null && locationButton.layoutParams is RelativeLayout.LayoutParams) {
            val params = locationButton.layoutParams as RelativeLayout.LayoutParams
            params.run {
                addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                setMargins(
                    0,
                    0,
                    params.leftMargin,
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        155f,
                        resources.displayMetrics
                    ).toInt()
                )
            }
        }
    }

    @SuppressLint("ResourceType")
    private fun relocateZoomControls() {
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
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        70f,
                        resources.displayMetrics
                    ).toInt()
                )
            }
        }
    }

    override fun onChanged(sensors: List<ExternalSensorDbo>?) {
        Log.i(Constants.TAG, "Refreshing external sensors ...")
        mapSensorCount.text = (sensors?.size ?: 0).toString()
        drawSensors(sensors)
    }
}