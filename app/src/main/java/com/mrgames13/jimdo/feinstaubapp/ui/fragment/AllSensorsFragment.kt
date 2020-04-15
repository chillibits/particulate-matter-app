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
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.db.ExternalSensor
import com.mrgames13.jimdo.feinstaubapp.network.isLocationEnabled
import com.mrgames13.jimdo.feinstaubapp.shared.*
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.ProgressDialog
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showRankingDialog
import com.mrgames13.jimdo.feinstaubapp.ui.item.MarkerItem
import com.mrgames13.jimdo.feinstaubapp.ui.view.SensorClusterItem
import com.mrgames13.jimdo.feinstaubapp.ui.view.SensorClusterRenderer
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_all_sensors.*
import kotlinx.android.synthetic.main.fragment_all_sensors.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllSensorsFragment(
    private val application: Application,
    private val listener: OnAdapterEventListener
) : Fragment(), OnMapReadyCallback, Observer<List<ExternalSensor>> {

    // Variables as objects
    private lateinit var mapFragment: SupportMapFragment
    private var map: GoogleMap? = null
    private lateinit var viewModel: MainViewModel
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var clusterManager: ClusterManager<SensorClusterItem>

    // Variables


    // Interfaces
    interface OnAdapterEventListener {

    }

    // Default constructor has to be implemented, otherwise the app crashes on configuration change
    constructor() : this(Application(), object: OnAdapterEventListener {})

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Initialize ViewModel
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(MainViewModel::class.java)

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

            val mapTrafficAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, listOf(getString(R.string.traffic_hide), getString(R.string.traffic_show)))
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
            /*mapSensorCount.setOnClickListener {
                Intent(Intent.ACTION_VIEW).run {
                    data = Uri.parse("https://h2801469.stratoserver.net/stats.php")
                    startActivity(this)
                }
            }*/

            // Initialize refresh button
            mapRefresh.setOnClickListener { refresh(false) }

            // Initialize ranking button
            mapRanking.setOnClickListener {
                showRankingDialog(context, requireFragmentManager(), lifecycle)
            }

            // Initialize map
            mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this@AllSensorsFragment)

            return this
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        if(map != null) {
            this.map = map
            // Initialize map ui
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isZoomControlsEnabled = true

            // Relocate controls
            relocationOwnLocationControls()
            relocateZoomControls()

            // Enable own location
            enableOwnLocationControls()

            // Register observer for live data
            viewModel.externalSensors.observe(viewLifecycleOwner, this)

            // Apply map style
            val themeResId = if(requireContext().isNightModeEnabled()) R.raw.map_style_dark else R.raw.map_style_silver
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, themeResId))

            refresh(true)
        } else {
            context?.outputErrorMessage()
        }
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

    fun applyPlaceSearch(coordinates: LatLng) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 11f))
    }

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
                        if (location != null) {
                            moveCamera(LatLng(location.latitude, location.longitude))
                            map?.setOnMyLocationChangeListener(null)
                        }
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
            clusterManager.renderer = SensorClusterRenderer(requireContext(), map!!, clusterManager, viewModel.sensors)
            clusterManager.setOnClusterItemClickListener { item ->
                showMarkerInfoWindow(item)
                true
            }
            clusterManager.setOnClusterClickListener {cluster ->
                showClusterInfoWindow(cluster)
                true
            }
            map?.setOnMarkerClickListener(clusterManager)
            map?.setOnCameraIdleListener(clusterManager)
        }
    }

    private fun showMarkerInfoWindow(item: ClusterItem) {

    }

    private fun showClusterInfoWindow(cluster: Cluster<SensorClusterItem>) {

    }

    private fun drawSensors(sensors: List<ExternalSensor>?) {
        sensors?.let {
            if(requireContext().getPreferenceValue(Constants.PREF_ENABLE_MARKER_CLUSTERING, true)) {
                // Initialize cluster manager if necessary
                if(!this::clusterManager.isInitialized) initializeClusterManager()
                clusterManager.clearItems()
                map?.clear()
                sensors.forEach {
                    if(it.latitude != 0.0 || it.longitude != 0.0)  {
                        val snippet = String.format(getString(R.string.marker_snippet), it.latitude, it.longitude)
                        val position = LatLng(it.latitude, it.longitude)
                        val markerItem = MarkerItem(it.chipId.toString(), snippet, position)
                        clusterManager.addItem(SensorClusterItem(markerItem, it))
                    }
                }
                clusterManager.cluster()
            } else {
                map?.clear()
                sensors.forEach {
                    if(it.latitude != 0.0 || it.longitude != 0.0) {
                        val snippet = String.format(getString(R.string.marker_snippet), it.latitude, it.longitude)
                        val position = LatLng(it.latitude, it.longitude)
                        val markerOptions = MarkerOptions()
                            .position(position)
                            .title(it.chipId.toString())
                            .snippet(snippet)
                        val sensor = viewModel.sensors.value?.find { s -> s.chipId == it.chipId }
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(
                            when {
                                sensor != null && !sensor.isOwner -> BitmapDescriptorFactory.HUE_RED
                                sensor != null && sensor.isOwner -> BitmapDescriptorFactory.HUE_GREEN
                                !it.active -> BitmapDescriptorFactory.HUE_ORANGE
                                else -> BitmapDescriptorFactory.HUE_BLUE
                            }
                        ))
                        map?.addMarker(markerOptions)
                    }
                }
            }
        }
    }

    private fun moveCamera(latLng: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, Constants.DEFAULT_MAP_ZOOM)
        map?.animateCamera(cameraUpdate)
    }

    @SuppressLint("ResourceType")
    private fun relocationOwnLocationControls() {
        val locationButton = (mapFragment.view?.findViewById<View>(0x1)?.parent as View).findViewById<View>(0x2)
        if(locationButton != null && locationButton.layoutParams is RelativeLayout.LayoutParams) {
            val params = locationButton.layoutParams as RelativeLayout.LayoutParams
            params.run {
                addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                setMargins(0, 0, 27, 430)
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

    override fun onChanged(sensors: List<ExternalSensor>?) {
        Log.i(Constants.TAG, "Refreshing external sensors ...")
        mapSensorCount.text = (sensors?.size ?: 0).toString()
        drawSensors(sensors)
    }
}