/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.adapter.viewpager

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.ExternalSensor
import com.mrgames13.jimdo.feinstaubapp.model.Sensor
import com.mrgames13.jimdo.feinstaubapp.network.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.network.loadClusterAverage
import com.mrgames13.jimdo.feinstaubapp.network.loadSensorsNonSync
import com.mrgames13.jimdo.feinstaubapp.network.loadSensorsSync
import com.mrgames13.jimdo.feinstaubapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.tool.Tools
import com.mrgames13.jimdo.feinstaubapp.ui.activity.CompareActivity
import com.mrgames13.jimdo.feinstaubapp.ui.activity.MainActivity
import com.mrgames13.jimdo.feinstaubapp.ui.activity.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.ui.adapter.recyclerview.SensorAdapter
import com.mrgames13.jimdo.feinstaubapp.ui.model.ClusterRenderer
import com.mrgames13.jimdo.feinstaubapp.ui.model.MarkerItem
import com.mrgames13.jimdo.feinstaubapp.ui.model.SensorClusterItem
import com.mrgames13.jimdo.feinstaubapp.ui.view.ProgressDialog
import com.mrgames13.jimdo.feinstaubapp.ui.view.showSensorInfoWindow
import kotlinx.android.synthetic.main.dialog_add_sensor.view.*
import kotlinx.android.synthetic.main.tab_all_sensors.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.margaritov.preference.colorpicker.ColorPickerDialog
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

class ViewPagerAdapterMain(manager: FragmentManager, activity: MainActivity, su: StorageUtils, smu: ServerMessagingUtils) : FragmentPagerAdapter(manager) {

    val selectedSensors: ArrayList<Sensor>
        get() {
            val selectedSensors = FavoritesFragment.selectedSensors + OwnSensorsFragment.selectedSensors
            return ArrayList(selectedSensors.distinctBy { it.chipID })
        }

    init {
        Companion.activity = activity
        random = Random()
        Companion.su = su
        Companion.smu = smu
    }

    override fun getItem(pos: Int): Fragment {
        return if (pos == 0) FavoritesFragment() else if(pos == 1) AllSensorsFragment() else OwnSensorsFragment()
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return ""
    }

    fun refresh() {
        FavoritesFragment.refresh()
        AllSensorsFragment.refresh()
        OwnSensorsFragment.refresh()
    }

    fun refreshFavourites() {
        FavoritesFragment.refresh()
    }

    fun refreshMySensors() {
        OwnSensorsFragment.refresh()
    }

    fun deselectAllSensors() {
        FavoritesFragment.deselectAllSensors()
        OwnSensorsFragment.deselectAllSensors()
    }

    fun search(query: String, mode: Int) {
        if (mode == SensorAdapter.MODE_FAVOURITES) FavoritesFragment.search(query)
        if (mode == SensorAdapter.MODE_OWN_SENSORS) OwnSensorsFragment.search(query)
    }

    fun closeInfoWindow(): Boolean {
        return AllSensorsFragment.closeInfoWindow()
    }

    //-------------------------------------------Fragments------------------------------------------

    class FavoritesFragment: Fragment() {
        override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
            contentView = LayoutInflater.from(parent?.context).inflate(R.layout.tab_my_favourites, parent, false)

            sensor_view = contentView.findViewById(R.id.sensor_view)
            sensor_view.setItemViewCacheSize(100)
            sensor_view.layoutManager = LinearLayoutManager(activity)

            refresh()

            return contentView
        }

        companion object {
            // Variables as objects
            private lateinit var contentView: View
            private lateinit var sensor_view: RecyclerView
            private lateinit var sensor_view_adapter: SensorAdapter
            private lateinit var sensors: ArrayList<Sensor>

            fun refresh() {
                sensors = su.allFavourites
                sensors.addAll(su.allOwnSensors)

                sensor_view_adapter = SensorAdapter(activity, sensors, su, smu, SensorAdapter.MODE_FAVOURITES)
                sensor_view.run {
                    adapter = sensor_view_adapter
                    setHasFixedSize(true)
                    visibility = if (sensors.size == 0) View.GONE else View.VISIBLE
                }
                contentView.findViewById<View>(R.id.no_data).visibility = if (sensors.size == 0) View.VISIBLE else View.GONE
            }

            val selectedSensors: ArrayList<Sensor>
                get() = sensor_view_adapter.selectedSensors

            fun deselectAllSensors() {
                sensor_view_adapter.deselectAllSensors()
            }

            fun search(query: String) {
                val searchValues: ArrayList<Sensor>?
                if (query.isEmpty()) {
                    searchValues = sensors
                } else {
                    searchValues = ArrayList()
                    for (s in sensors) {
                        if (s.name.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault())) || s.chipID.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) searchValues.add(s)
                    }
                }
                sensor_view_adapter = SensorAdapter(activity, searchValues, su, smu, SensorAdapter.MODE_FAVOURITES)
                sensor_view.adapter = sensor_view_adapter
            }
        }
    }

    class AllSensorsFragment: Fragment(), OnMapReadyCallback {
        private lateinit var mapType: Spinner
        private lateinit var mapTraffic: Spinner
        private lateinit var mapSensorRefresh: ImageView
        private lateinit var sensorChipId: TextView
        private lateinit var sensorCoordinates: TextView
        private lateinit var sensorLocation: TextView
        private lateinit var sensorInfo: ImageView
        private lateinit var sensorShowData: Button
        private lateinit var sensorLink: Button
        private lateinit var infoSensorCount: TextView
        private lateinit var infoAverageValue: TextView
        private lateinit var infoCompareSensors: Button
        private lateinit var infoZoomIn: Button

        // Variables
        private var currentColor: Int = 0

        private val isGPSPermissionGranted: Boolean
            get() = ActivityCompat.checkSelfPermission(ViewPagerAdapterMain.activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                ViewPagerAdapterMain.activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
            contentView = inflater.inflate(R.layout.tab_all_sensors, null) // Have to be like that. Otherwise the map onMapReady will not be called

            map_fragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

            mapType = contentView.map_type
            val mapTypes = ArrayList<String>(resources.getStringArray(R.array.map_type).toList())
            val adapterType = ArrayAdapter(ViewPagerAdapterMain.activity, android.R.layout.simple_spinner_item, mapTypes)
            adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mapType.adapter = adapterType
            mapType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    changeMapType(pos)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            val defaultMapType = su.getString("default_map_type", "0").toInt()
            changeMapType(defaultMapType)
            mapType.setSelection(defaultMapType)

            mapTraffic = contentView.findViewById(R.id.map_traffic)
            val mapTrafficItems = ArrayList<String>(listOf(getString(R.string.traffic_hide), getString(R.string.traffic_show)))
            val adapterTraffic = ArrayAdapter(ViewPagerAdapterMain.activity, android.R.layout.simple_spinner_item, mapTrafficItems)
            adapterTraffic.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mapTraffic.adapter = adapterTraffic
            mapTraffic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                    map?.isTrafficEnabled = pos != 0
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }
            val trafficEnabled = su.getBoolean("default_traffic", false)
            map?.isTrafficEnabled = trafficEnabled
            mapTraffic.setSelection(if(trafficEnabled) 1 else 0)

            map_sensor_count = contentView.findViewById(R.id.map_sensor_count)
            map_sensor_count.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("https://h2801469.stratoserver.net/stats.php")
                startActivity(i)
            }

            mapSensorRefresh = contentView.findViewById(R.id.map_sensor_refresh)
            mapSensorRefresh.setOnClickListener {
                pd = ProgressDialog(requireContext()).show()
                loadAllSensorsNonSync()
            }

            map_fragment?.getMapAsync(this)

            // Initialize sensor info window
            sensor_container = contentView.sensor_container
            sensorChipId = contentView.sensor_chip_id
            sensorCoordinates = contentView.sensor_coordinates
            sensorLocation = contentView.sensor_location
            sensorInfo = contentView.sensor_info
            sensorShowData = contentView.sensor_show_data
            sensorLink = contentView.sensor_link

            // Initialize sensor cluster info window
            sensor_cluster_container = contentView.sensor_cluster_container
            infoSensorCount = contentView.info_sensor_count
            infoAverageValue = contentView.info_average_value
            infoCompareSensors = contentView.info_sensors_compare
            infoZoomIn = contentView.info_sensors_zoom

            return contentView
        }

        private fun changeMapType(pos: Int) {
            when (pos) {
                0 -> map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                1 -> map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                2 -> map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                3 -> map?.mapType = GoogleMap.MAP_TYPE_HYBRID
            }
        }

        override fun onMapReady(googleMap: GoogleMap) {
            map = googleMap
            map?.uiSettings?.isRotateGesturesEnabled = false
            map?.uiSettings?.isZoomControlsEnabled = true

            // relocate MyLocationButton
            val locationButton = (map_fragment?.view!!.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(Integer.parseInt("2"))
            val rlp = locationButton.layoutParams as RelativeLayout.LayoutParams
            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            rlp.setMargins(0, 0, 27, 430)

            @SuppressLint("ResourceType") val zoomControls = map_fragment?.view!!.findViewById<View>(0x1)
            if (zoomControls != null && zoomControls.layoutParams is RelativeLayout.LayoutParams) {
                val params = zoomControls.layoutParams as RelativeLayout.LayoutParams

                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)

                val margin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70f, resources.displayMetrics).toInt()
                params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, margin)
            }

            enableOwnLocation()

            map?.setMapStyle(MapStyleOptions.loadRawResourceStyle(ViewPagerAdapterMain.activity, if (ContextCompat.getColor(requireContext(), R.color.colorPrimary) == ContextCompat.getColor(requireContext(), R.color.dark_mode_indicator)) R.raw.map_style_dark else R.raw.map_style_silver))

            // Initialize ClusterManager
            clusterManager = ClusterManager(ViewPagerAdapterMain.activity, map)
            clusterManager.renderer = ClusterRenderer(
                ViewPagerAdapterMain.activity,
                map!!,
                clusterManager,
                su
            )
            if (su.getBoolean("enable_marker_clustering", true)) {
                map?.setOnMarkerClickListener(clusterManager)
                clusterManager.setOnClusterItemClickListener { sensorClusterItem ->
                    showInfoWindow(sensorClusterItem.marker)
                    true
                }
                clusterManager.setOnClusterClickListener { cluster ->
                    showClusterWindow(cluster)
                    true
                }
                map?.setOnCameraIdleListener(clusterManager)
            } else {
                map?.setOnMarkerClickListener { marker ->
                    val m = MarkerItem(marker.title, marker.snippet, marker.position)
                    showInfoWindow(m)
                    true
                }
            }

            // Zoom to current country
            try {
                val telManager = ViewPagerAdapterMain.activity.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val iso = telManager.simCountryIso
                current_country = Tools.getLocationFromAddress(ViewPagerAdapterMain.activity, iso)
            } catch (ignored: Exception) {}

            map?.setOnCameraMoveListener {
                if (selected_marker_position != null) {
                    val p = map!!.projection
                    val screenPos = p.toScreenLocation(selected_marker_position)
                    val lp = RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT)
                    var x = max(0, screenPos.x - 275)
                    var y = max(0, screenPos.y - 680)
                    x = if (x + 550 > map_fragment?.view!!.width) map_fragment?.view!!.width - 550 else x
                    y = if (y + sensor_container.height > map_fragment?.view!!.height) map_fragment?.view!!.height - sensor_container.height else y
                    lp.setMargins(x, y, 0, 0)
                    sensor_container.layoutParams = lp
                }
                if (selected_cluster_position != null) {
                    val p = map!!.projection
                    val screenPos = p.toScreenLocation(selected_cluster_position)
                    val lp = RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT)
                    var x = max(0, screenPos.x - 275)
                    var y = max(0, screenPos.y - 650)
                    x = if (x + 550 > map_fragment?.view!!.width) map_fragment?.view!!.width - 550 else x
                    y = if (y + sensor_cluster_container.height > map_fragment?.view!!.height) map_fragment?.view!!.height - sensor_cluster_container.height else y
                    lp.setMargins(x, y, 0, 0)
                    sensor_cluster_container.layoutParams = lp
                }
            }

            map?.setOnMapClickListener {
                when {
                    selected_marker_position != null -> exitReveal(sensor_container)
                    selected_cluster_position != null -> exitReveal(sensor_cluster_container)
                    else -> ViewPagerAdapterMain.activity.toggleToolbar()
                }
            }

            // Load sensors
            loadAllSensors()
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            if (isGPSPermissionGranted) {
                if (!isGPSEnabled(ViewPagerAdapterMain.activity)) startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                enableOwnLocation()
            }
        }

        @SuppressLint("MissingPermission")
        private fun enableOwnLocation() {
            if (isGPSPermissionGranted && isGPSEnabled(ViewPagerAdapterMain.activity)) {
                map?.isMyLocationEnabled = true
                map?.setOnMyLocationChangeListener { location ->
                    moveCamera(LatLng(location.latitude, location.longitude))
                    map?.setOnMyLocationChangeListener(null)
                }
            } else if (isGPSEnabled(ViewPagerAdapterMain.activity)) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                    REQ_LOCATION_PERMISSION
                )
            }
        }

        private fun chooseColor(sensor_color: ImageView) {
            // Show color picker dialog
            val colorPicker = ColorPickerDialog(activity, currentColor)
            colorPicker.alphaSliderVisible = false
            colorPicker.hexValueEnabled = true
            colorPicker.setTitle(getString(R.string.choose_color))
            colorPicker.setOnColorChangedListener { color ->
                currentColor = color
                sensor_color.setColorFilter(color, PorterDuff.Mode.SRC)
            }
            colorPicker.show()
        }

        private fun isGPSEnabled(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        private fun showInfoWindow(marker: MarkerItem) {
            if(selected_cluster_position != null) exitReveal(
                sensor_cluster_container
            )
            if (selected_marker_position != null && selected_marker_position?.latitude == marker.position.latitude && selected_marker_position?.longitude == marker.position.longitude) {
                exitReveal(
                    sensor_container
                )
            } else {
                sensorChipId.text = marker.title
                sensorCoordinates.text = marker.snippet
                marker.tag = marker.title
                sensorInfo.setOnClickListener {
                    showSensorInfoWindow(requireActivity(), smu, marker.title, if(marker.tag != marker.title) marker.tag!! else getString(R.string.unknown_sensor))
                }
                sensorShowData.setOnClickListener {
                    exitReveal(sensor_container)

                    random = Random()
                    currentColor = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))

                    val i = Intent(activity, SensorActivity::class.java)
                    i.putExtra("Name", marker.tag)
                    i.putExtra("ID", marker.title)
                    i.putExtra("Color", currentColor)
                    ViewPagerAdapterMain.activity.startActivity(i)
                }
                sensorLink.setOnClickListener {
                    if (!su.isFavouriteExisting(marker.title) && !su.isSensorExisting(marker.title)) {
                        val v = layoutInflater.inflate(R.layout.dialog_add_sensor, null)

                        v.sensor_name_value.hint = marker.tag
                        v.sensor_chip_id_value.setText(marker.title)

                        // Initialize randomizer and generate random color
                        random = Random()
                        currentColor = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
                        v.sensor_color.setColorFilter(currentColor, PorterDuff.Mode.SRC)

                        v.sensor_color.setOnClickListener { chooseColor(v.sensor_color) }
                        v.choose_sensor_color.setOnClickListener { chooseColor(v.sensor_color) }

                        AlertDialog.Builder(ViewPagerAdapterMain.activity)
                            .setTitle(R.string.add_favourite)
                            .setView(v)
                            .setPositiveButton(R.string.done) { _, _ ->
                                // Save new sensor
                                var nameString: String? = v.sensor_name_value.text.toString().trim()
                                if (nameString!!.isEmpty()) nameString = marker.tag
                                su.addFavourite(Sensor(marker.title, nameString!!, currentColor), false)
                                FavoritesFragment.refresh()
                                refresh()
                                selected_marker_position = null
                                sensor_container.visibility = View.GONE
                                Toast.makeText(activity, getString(R.string.favourite_added), Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    } else {
                        // Sensor is already linked
                        Toast.makeText(activity, getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show()
                    }
                }

                val p = map!!.projection
                selected_marker_position = marker.position
                val screenPos = p.toScreenLocation(selected_marker_position)

                val lp = RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT)
                var x = max(0, screenPos.x - 275)
                var y = max(0, screenPos.y - 680)
                x = if (x + 550 > map_fragment?.view!!.width) map_fragment?.view!!.width - 550 else x
                y = if (y + sensor_container.height > map_fragment?.view!!.height) map_fragment?.view!!.height - sensor_container.height else y
                lp.setMargins(x, y, 0, 0)
                sensor_container.layoutParams = lp
                enterReveal(sensor_container)

                try {
                    val geoCoder = Geocoder(activity, Locale.getDefault())
                    val addresses = geoCoder.getFromLocation(marker.position.latitude, marker.position.longitude, 1)

                    var countryCityText = ViewPagerAdapterMain.activity.getString(R.string.unknown_location)
                    if (addresses[0].countryName != null) countryCityText = addresses[0].countryName
                    if (addresses[0].locality != null) countryCityText += " - " + addresses[0].locality
                    sensorLocation.text = countryCityText
                    marker.tag = if(addresses[0].locality.isNotEmpty()) addresses[0].locality else marker.title
                } catch (e: Exception) {
                    sensorLocation.setText(R.string.unknown_location)
                    marker.tag = marker.title
                }
            }
        }

        private fun showClusterWindow(cluster: Cluster<*>) {
            if (selected_marker_position != null) exitReveal(
                sensor_container
            )
            if (selected_cluster_position?.latitude == cluster.position.latitude && selected_cluster_position?.longitude == cluster.position.longitude) {
                exitReveal(
                    sensor_cluster_container
                )
            } else {
                infoSensorCount.text = String.format(getString(R.string.sensors), cluster.items.size)
                infoAverageValue.setText(R.string.loading)
                infoCompareSensors.isEnabled = cluster.items.size <= 15

                val p = map!!.projection
                selected_cluster_position = cluster.position
                val screenPos = p.toScreenLocation(selected_cluster_position)

                val lp = RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT)
                var x = max(0, screenPos.x - 275)
                var y = max(0, screenPos.y - 650)
                x = if (x + 550 > map_fragment?.view!!.width) map_fragment?.view!!.width - 550 else x
                y = if (y + sensor_cluster_container.height > map_fragment?.view!!.height) map_fragment?.view!!.height - sensor_cluster_container.height else y
                lp.setMargins(x, y, 0, 0)
                sensor_cluster_container.layoutParams = lp
                enterReveal(sensor_cluster_container)

                val items = cluster.items
                val sensors = ArrayList<Sensor>()
                val ids = ArrayList<String>()
                val rand = Random()
                for (s in items) {
                    ids.add(s.title)
                    sensors.add(Sensor(s.title, s.snippet, Color.argb(255, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256))))
                }

                infoCompareSensors.setOnClickListener {
                    exitReveal(sensor_cluster_container)
                    // Launch CompareActivity
                    val i = Intent(activity, CompareActivity::class.java)
                    i.putExtra("Sensors", sensors)
                    startActivity(i)
                }
                infoZoomIn.setOnClickListener {
                    exitReveal(sensor_cluster_container)
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.position, min(map!!.maxZoomLevel, map!!.cameraPosition.zoom + 3)))
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val result = loadClusterAverage(ViewPagerAdapterMain.activity, ids)
                    CoroutineScope(Dispatchers.Main).launch {
                        infoAverageValue.text = if(result == 0.0) getString(R.string.error_try_again) else "Ø $result µg/m³"
                    }
                }
            }
        }

        private fun enterReveal(v: View) {
            val finalRadius = max(v.width, v.height) + 40
            val anim = ViewAnimationUtils.createCircularReveal(v, 275, v.measuredHeight, 0f, finalRadius.toFloat())
            anim.duration = 350
            anim.start()
            v.visibility = View.VISIBLE
        }

        companion object {
            // Variables as objects
            private lateinit var contentView: View
            private var map_fragment: SupportMapFragment? = null
            private lateinit var map_sensor_count: TextView
            private var map: GoogleMap? = null
            private lateinit var clusterManager: ClusterManager<SensorClusterItem>
            private lateinit var sensors: ArrayList<ExternalSensor>
            private var current_country: LatLng? = null
            private lateinit var pd: ProgressDialog
            // Sensor info window
            private lateinit var sensor_container: RelativeLayout
            private var selected_marker_position: LatLng? = null
            // Sensor cluster info window
            private lateinit var sensor_cluster_container: RelativeLayout
            private var selected_cluster_position: LatLng? = null

            fun moveCamera(coords: LatLng) {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 11f))
            }

            fun refresh() {
                val pos = map?.cameraPosition
                map?.clear()
                loadAllSensors()
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(pos?.target, pos!!.zoom))
            }

            private fun loadAllSensors() {
                if (smu.checkConnection(contentView)) {
                    // The device is online. We're able to load data from the server
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Load old sensors from the local db
                            sensors = su.externalSensors
                            val chipSum = sensors.map { it.chipId.toLong() }.sum() / 10000.0
                            val sensorHash = Tools.md5(Tools.round(chipSum, 0).toInt().toString())
                            // Load new sensors from server
                            val lastRequest = su.getLong("LastRequest", 0)
                            val lastRequestString = if (lastRequest.toString().length > 10) lastRequest.toString().substring(0, 10) else lastRequest.toString()
                            val newLastRequest = System.currentTimeMillis()
                            // Send request
                            val syncPackage = loadSensorsSync(activity, lastRequestString, sensorHash)
                            if (syncPackage != null) {
                                // Remove sensors, that are not in the ids list
                                if(syncPackage.ids.isNotEmpty()) {
                                    for(s in sensors) {
                                        if(s.chipId !in syncPackage.ids) su.deleteExternalSensor(s.chipId)
                                    }
                                }
                                // Add or edit sensors, that are in the update list
                                su.addAllExternalSensors(ArrayList(syncPackage.update.map { ExternalSensor(chipId = it.i, lat = it.l, lng = it.b) }))
                                // Save, reload sensors and redraw
                                su.putLong("LastRequest", newLastRequest)
                                sensors = su.externalSensors
                                CoroutineScope(Dispatchers.Main).launch { drawSensorsToMap() }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch { Toast.makeText(activity, R.string.error_try_again, Toast.LENGTH_SHORT).show() }
                            }
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    // We're offline, load the old data from the local db
                    sensors = su.externalSensors
                    drawSensorsToMap()
                }
            }

            private fun drawSensorsToMap() {
                // Draw the sensors on the map
                map_sensor_count.text = sensors.size.toString()
                val isMarkerClusteringEnabled = su.getBoolean("enable_marker_clustering", true)
                if (isMarkerClusteringEnabled) {
                    clusterManager.clearItems()
                } else {
                    map?.clear()
                }
                for (sensor in sensors) {
                    if (isMarkerClusteringEnabled) {
                        val m = MarkerItem(sensor.chipId, sensor.lat.toString() + ", " + sensor.lng, LatLng(sensor.lat, sensor.lng))
                        clusterManager.addItem(SensorClusterItem(sensor.lat, sensor.lng, sensor.chipId, sensor.lat.toString() + ", " + sensor.lng, m))
                    } else {
                        map?.addMarker(
                            MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(if (su.isFavouriteExisting(sensor.chipId)) BitmapDescriptorFactory.HUE_RED else if (su.isSensorExisting(sensor.chipId)) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_BLUE))
                                .position(LatLng(sensor.lat, sensor.lng))
                                .title(sensor.chipId)
                                .snippet(sensor.lat.toString() + ", " + sensor.lng)
                        )
                    }
                }
                if(current_country != null) map?.moveCamera(CameraUpdateFactory.newLatLngZoom(current_country, 5f))
                if (su.getBoolean("enable_marker_clustering", true)) clusterManager.cluster()
            }

            private fun loadAllSensorsNonSync() {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Load all sensors from server
                        val newLastRequest = System.currentTimeMillis()
                        val result = ArrayList(loadSensorsNonSync(activity))
                        Log.i(Constants.TAG, "Loading time: " + (System.currentTimeMillis() - newLastRequest))
                        if (result.isNotEmpty()) {
                            su.clearExternalSensors()
                            su.addAllExternalSensors(result)
                            su.putLong("LastRequest", newLastRequest)
                            sensors = result

                            // Draw sensors on the map
                            CoroutineScope(Dispatchers.Main).launch {
                                drawSensorsToMap()
                                if (pd.isShowing()) pd.dismiss()
                            }
                        } else {
                            CoroutineScope(Dispatchers.Main).launch { pd.dismiss() }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            internal fun exitReveal(v: View?) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    val initialRadius = max(v!!.width, v.height) + 40
                    val anim = ViewAnimationUtils.createCircularReveal(v, 275, v.measuredHeight, initialRadius.toFloat(), 0f)
                    anim.duration = 350
                    anim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            v.visibility = View.INVISIBLE
                        }
                    })
                    anim.start()
                } else {
                    v!!.visibility = View.INVISIBLE
                }
                selected_marker_position = null
                selected_cluster_position = null
            }

            fun closeInfoWindow(): Boolean {
                if(selected_marker_position != null) {
                    exitReveal(sensor_container)
                    selected_marker_position = null
                    return true
                } else if(selected_cluster_position != null) {
                    exitReveal(sensor_cluster_container)
                    selected_cluster_position = null
                    return true
                }
                return false
            }
        }
    }

    class OwnSensorsFragment : Fragment() {
        private var noDataText: TextView? = null

        override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
            contentView = LayoutInflater.from(parent?.context).inflate(R.layout.tab_my_sensors, parent, false)

            sensor_view = contentView.findViewById(R.id.sensor_view)
            sensor_view.setItemViewCacheSize(100)
            sensor_view.layoutManager = LinearLayoutManager(activity)

            noDataText = contentView.findViewById(R.id.no_data_text)
            noDataText!!.movementMethod = LinkMovementMethod.getInstance()

            refresh()

            return contentView
        }

        companion object {
            // Variables as objects
            private lateinit var contentView: View
            private lateinit var sensor_view: RecyclerView
            private lateinit var sensor_view_adapter: SensorAdapter
            private lateinit var sensors: ArrayList<Sensor>

            fun refresh() {
                sensors = su.allOwnSensors
                sensor_view_adapter = SensorAdapter(activity, sensors, su, smu, SensorAdapter.MODE_OWN_SENSORS)
                sensor_view.adapter = sensor_view_adapter
                sensor_view.visibility = if (sensors.size == 0) View.GONE else View.VISIBLE
                contentView.findViewById<View>(R.id.no_data).visibility = if (sensors.size == 0) View.VISIBLE else View.GONE
            }

            val selectedSensors: ArrayList<Sensor>
                get() = sensor_view_adapter.selectedSensors

            fun deselectAllSensors() {
                sensor_view_adapter.deselectAllSensors()
            }

            fun search(query: String) {
                val searchValues: ArrayList<Sensor>?
                if (query.isEmpty()) {
                    searchValues = sensors
                } else {
                    searchValues = ArrayList()
                    for (s in sensors) {
                        if (s.name.toLowerCase().contains(query.toLowerCase()) || s.chipID.toLowerCase().contains(query.toLowerCase())) searchValues.add(s)
                    }
                }
                sensor_view_adapter = SensorAdapter(activity, searchValues, su, smu, SensorAdapter.MODE_OWN_SENSORS)
                sensor_view.adapter = sensor_view_adapter
            }
        }
    }

    companion object {
        // Constants
        private const val REQ_LOCATION_PERMISSION = 10001

        // Variables as objects
        private lateinit var activity: MainActivity
        private lateinit var random: Random

        // Utils packages
        private lateinit var su: StorageUtils
        private lateinit var smu: ServerMessagingUtils
    }
}