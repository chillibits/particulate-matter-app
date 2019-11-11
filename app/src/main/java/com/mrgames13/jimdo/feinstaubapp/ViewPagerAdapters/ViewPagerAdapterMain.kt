/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.ProgressDialog
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
import com.mrgames13.jimdo.feinstaubapp.App.CompareActivity
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.ExternalSensor
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.ClusterRenderer
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.MarkerItem
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.SensorClusterItem
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SensorAdapter
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools
import net.margaritov.preference.colorpicker.ColorPickerDialog
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.max
import kotlin.math.min

class ViewPagerAdapterMain(manager: FragmentManager, activity: MainActivity, su: StorageUtils, smu: ServerMessagingUtils) : FragmentPagerAdapter(manager) {

    val selectedSensors: ArrayList<Sensor>
        get() {
            val selectedSensors = ArrayList<Sensor>()
            selectedSensors.addAll(MyFavouritesFragment.selectedSensors)
            selectedSensors.addAll(MySensorsFragment.selectedSensors)
            return Tools.removeDuplicateSensors(selectedSensors)
        }

    init {
        ViewPagerAdapterMain.activity = activity
        random = Random()
        ViewPagerAdapterMain.su = su
        ViewPagerAdapterMain.smu = smu
    }

    override fun getItem(pos: Int): Fragment {
        return if (pos == 0) MyFavouritesFragment() else if(pos == 1) AllSensorsFragment() else MySensorsFragment()
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return ""
    }

    fun refresh() {
        MyFavouritesFragment.refresh()
        AllSensorsFragment.refresh()
        MySensorsFragment.refresh()
    }

    fun refreshFavourites() {
        MyFavouritesFragment.refresh()
    }

    fun refreshMySensors() {
        MySensorsFragment.refresh()
    }

    fun deselectAllSensors() {
        MyFavouritesFragment.deselectAllSensors()
        MySensorsFragment.deselectAllSensors()
    }

    fun search(query: String, mode: Int) {
        if (mode == SensorAdapter.MODE_FAVOURITES) MyFavouritesFragment.search(query)
        if (mode == SensorAdapter.MODE_OWN_SENSORS) MySensorsFragment.search(query)
    }

    fun closeInfoWindow(): Boolean {
        return AllSensorsFragment.closeInfoWindow()
    }

    //-------------------------------------------Fragments------------------------------------------

    class MyFavouritesFragment : Fragment() {
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
                sensor_view.adapter = sensor_view_adapter
                sensor_view.setHasFixedSize(true)
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
                sensor_view_adapter = SensorAdapter(activity, searchValues, su, smu, SensorAdapter.MODE_FAVOURITES)
                sensor_view.adapter = sensor_view_adapter
            }
        }
    }

    class AllSensorsFragment : Fragment(), OnMapReadyCallback {
        private lateinit var mapType: Spinner
        private lateinit var mapTraffic: Spinner
        private lateinit var mapSensorRefresh: ImageView
        private lateinit var sensorChipId: TextView
        private lateinit var sensorCoordinates: TextView
        private lateinit var sensorLocation: TextView
        private lateinit var sensorShowData: Button
        private lateinit var sensorLink: Button
        private lateinit var infoSensorCount: TextView
        private lateinit var infoAverageValue: TextView
        private lateinit var infoCompareSensors: Button
        private lateinit var infoZoomIn: Button

        // Variables
        private var currentColor: Int = 0

        private val isGPSPermissionGranted: Boolean
            get() = ActivityCompat.checkSelfPermission(ViewPagerAdapterMain.activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ViewPagerAdapterMain.activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
            contentView = inflater.inflate(R.layout.tab_all_sensors, null) // Have to be like that. Otherwise the map onMapReady will not be called

            map_fragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

            mapType = contentView.findViewById(R.id.map_type)
            val mapTypes = ArrayList<String>()
            mapTypes.add(getString(R.string.normal))
            mapTypes.add(getString(R.string.terrain))
            mapTypes.add(getString(R.string.satellite))
            mapTypes.add(getString(R.string.hybrid))
            val adapterType = ArrayAdapter(ViewPagerAdapterMain.activity, android.R.layout.simple_spinner_item, mapTypes)
            adapterType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mapType.adapter = adapterType
            mapType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, type: Int, l: Long) {
                    when (type) {
                        0 -> map.mapType = GoogleMap.MAP_TYPE_NORMAL
                        1 -> map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                        2 -> map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        3 -> map.mapType = GoogleMap.MAP_TYPE_HYBRID
                    }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }

            mapTraffic = contentView.findViewById(R.id.map_traffic)
            val mapTrafficItems = ArrayList<String>()
            mapTrafficItems.add(getString(R.string.traffic_hide))
            mapTrafficItems.add(getString(R.string.traffic_show))
            val adapterTraffic = ArrayAdapter(ViewPagerAdapterMain.activity, android.R.layout.simple_spinner_item, mapTrafficItems)
            adapterTraffic.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mapTraffic.adapter = adapterTraffic
            mapTraffic.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, type: Int, l: Long) {
                    map.isTrafficEnabled = type != 0
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }

            map_sensor_count = contentView.findViewById(R.id.map_sensor_count)
            map_sensor_count.setOnClickListener {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("https://h2801469.stratoserver.net/stats.php")
                startActivity(i)
            }

            mapSensorRefresh = contentView.findViewById(R.id.map_sensor_refresh)
            mapSensorRefresh.setOnClickListener {
                pd = ProgressDialog(activity)
                pd.setMessage(resources.getString(R.string.loading_data))
                pd.setCancelable(false)
                pd.show()
                loadAllSensorsNonSync()
            }

            map_fragment?.getMapAsync(this)

            // Initialize sensor info window
            sensor_container = contentView.findViewById(R.id.sensor_container)
            sensorChipId = contentView.findViewById(R.id.sensor_chip_id)
            sensorCoordinates = contentView.findViewById(R.id.sensor_coordinates)
            sensorLocation = contentView.findViewById(R.id.sensor_location)
            sensorShowData = contentView.findViewById(R.id.sensor_show_data)
            sensorLink = contentView.findViewById(R.id.sensor_link)

            // Initialize sensor cluster info window
            sensor_cluster_container = contentView.findViewById(R.id.sensor_cluster_container)
            infoSensorCount = contentView.findViewById(R.id.info_sensor_count)
            infoAverageValue = contentView.findViewById(R.id.info_average_value)
            infoCompareSensors = contentView.findViewById(R.id.info_sensors_compare)
            infoZoomIn = contentView.findViewById(R.id.info_sensors_zoom)

            return contentView
        }

        override fun onMapReady(googleMap: GoogleMap) {
            map = googleMap
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isZoomControlsEnabled = true

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

            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(ViewPagerAdapterMain.activity, if (resources.getColor(R.color.colorPrimary) == resources.getColor(R.color.dark_mode_indicator)) R.raw.map_style_dark else R.raw.map_style_silver))

            // Initialize ClusterManager
            clusterManager = ClusterManager(ViewPagerAdapterMain.activity, map)
            clusterManager.renderer = ClusterRenderer(ViewPagerAdapterMain.activity, map, clusterManager, su)
            if (su.getBoolean("enable_marker_clustering", true)) {
                map.setOnMarkerClickListener(clusterManager)
                clusterManager.setOnClusterItemClickListener { sensorClusterItem ->
                    showInfoWindow(sensorClusterItem.marker)
                    true
                }
                clusterManager.setOnClusterClickListener { cluster ->
                    showClusterWindow(cluster)
                    true
                }
                map.setOnCameraIdleListener(clusterManager)
            } else {
                map.setOnMarkerClickListener { marker ->
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

            map.setOnCameraMoveListener {
                if (selected_marker_position != null) {
                    val p = map.projection
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
                    val p = map.projection
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

            map.setOnMapClickListener {
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
                map.isMyLocationEnabled = true
                map.setOnMyLocationChangeListener { location ->
                    moveCamera(LatLng(location.latitude, location.longitude))
                    map.setOnMyLocationChangeListener(null)
                }
            } else if (isGPSEnabled(ViewPagerAdapterMain.activity)) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), REQ_LOCATION_PERMISSION)
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
            if(selected_cluster_position != null) exitReveal(sensor_cluster_container)
            if (selected_marker_position != null && selected_marker_position?.latitude == marker.position.latitude && selected_marker_position?.longitude == marker.position.longitude) {
                exitReveal(sensor_container)
            } else {
                sensorChipId.text = marker.title
                sensorCoordinates.text = marker.snippet
                marker.tag = marker.title
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
                        val name = v.findViewById<EditText>(R.id.sensor_name_value)
                        val chipId = v.findViewById<EditText>(R.id.sensor_chip_id_value)
                        val chooseColor = v.findViewById<Button>(R.id.choose_sensor_color)
                        val sensorColor = v.findViewById<ImageView>(R.id.sensor_color)

                        name.hint = marker.tag
                        chipId.setText(marker.title)

                        // Initialize randomizer and generate random color
                        random = Random()
                        currentColor = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
                        sensorColor.setColorFilter(currentColor, PorterDuff.Mode.SRC)

                        sensorColor.setOnClickListener { chooseColor(sensorColor) }
                        chooseColor.setOnClickListener { chooseColor(sensorColor) }

                        val d = AlertDialog.Builder(ViewPagerAdapterMain.activity)
                                .setCancelable(true)
                                .setTitle(R.string.add_favourite)
                                .setView(v)
                                .setPositiveButton(R.string.done) { dialogInterface, i ->
                                    // Save new sensor
                                    var nameString: String? = name.text.toString().trim { it <= ' ' }
                                    if (nameString!!.isEmpty()) nameString = marker.tag
                                    su.addFavourite(Sensor(marker.title, nameString!!, currentColor), false)
                                    MyFavouritesFragment.refresh()
                                    refresh()
                                    selected_marker_position = null
                                    sensor_container.visibility = View.GONE
                                    Toast.makeText(activity, getString(R.string.favourite_added), Toast.LENGTH_SHORT).show()
                                }
                                .create()
                        d.show()
                    } else {
                        // Sensor is already linked
                        Toast.makeText(activity, getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show()
                    }
                }

                val p = map.projection
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
                } catch (e: Exception) {
                    sensorLocation.setText(R.string.unknown_location)
                    marker.tag = marker.title
                }
            }
        }

        private fun showClusterWindow(cluster: Cluster<*>) {
            if (selected_marker_position != null) exitReveal(sensor_container)
            if (selected_cluster_position?.latitude == cluster.position.latitude && selected_cluster_position?.longitude == cluster.position.longitude) {
                exitReveal(sensor_cluster_container)
            } else {
                infoSensorCount.text = cluster.items.size.toString() + " " + getString(R.string.sensors)
                infoAverageValue.setText(R.string.loading)
                infoCompareSensors.isEnabled = cluster.items.size <= 15

                val p = map.projection
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
                val param = StringBuilder()
                val sensors = ArrayList<Sensor>()
                val rand = Random()
                for (s in items) {
                    param.append(";").append(s.title)
                    sensors.add(Sensor(s.title, s.snippet, Color.argb(255, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256))))
                }
                val paramString = param.substring(1)

                infoCompareSensors.setOnClickListener {
                    exitReveal(sensor_cluster_container)
                    // Launch CompareActivity
                    val i = Intent(activity, CompareActivity::class.java)
                    i.putExtra("Sensors", sensors)
                    startActivity(i)
                }
                infoZoomIn.setOnClickListener {
                    exitReveal(sensor_cluster_container)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.position, min(map.maxZoomLevel, map.cameraPosition.zoom + 3)))
                }

                Thread(Runnable {
                    // Get information from the server
                    val result = smu.sendRequest(null, object : HashMap<String, String>() {
                        init {
                            put("command", "getclusterinfo")
                            put("ids", paramString)
                        }
                    })
                    ViewPagerAdapterMain.activity.runOnUiThread {
                        try {
                            infoAverageValue.text = "Ø " + Tools.round(java.lang.Double.parseDouble(result), 2) + " µg/m³"
                        } catch (e: Exception) {
                            infoAverageValue.setText(R.string.error_try_again)
                        }
                    }
                }).start()
            }
        }

        private fun enterReveal(v: View) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val finalRadius = max(v.width, v.height) + 40
                val anim = ViewAnimationUtils.createCircularReveal(v, 275, v.measuredHeight, 0f, finalRadius.toFloat())
                anim.duration = 350
                anim.start()
            }
            v.visibility = View.VISIBLE
        }

        companion object {
            // Variables as objects
            private lateinit var contentView: View
            private var map_fragment: SupportMapFragment? = null
            private lateinit var map_sensor_count: TextView
            private lateinit var map: GoogleMap
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
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 11f))
            }

            fun refresh() {
                val pos = map.cameraPosition
                map.clear()
                loadAllSensors()
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos.target, pos.zoom))
            }

            private fun loadAllSensors() {
                if (smu.checkConnection(contentView)) {
                    // The device is online. We're able to load data from the server
                    Thread(Runnable {
                        try {
                            // Load old sensors from the local db
                            sensors = su.externalSensors
                            // Create hash
                            var chipSum = 0.0
                            for (s in sensors) chipSum += java.lang.Long.parseLong(s.chipID) / 1000.0
                            val sensorHash = Tools.md5(Tools.round(chipSum, 0).toInt().toString())
                            // Load new sensors from server
                            val lastRequest = su.getLong("LastRequest", 0)
                            val lastRequestString = if (lastRequest.toString().length > 10) lastRequest.toString().substring(0, 10) else lastRequest.toString()
                            val newLastRequest = System.currentTimeMillis()
                            val result = smu.sendRequest(contentView.findViewById(R.id.container), object : HashMap<String, String>() {
                                init {
                                    put("command", "getall")
                                    put("last_request", lastRequestString)
                                    put("cs", sensorHash)
                                }
                            })
                            if (result.isNotEmpty()) {
                                val array = JSONObject(result)
                                val arrayUpdate = array.getJSONArray("update")
                                val arrayIds = array.getJSONArray("ids")
                                if (arrayIds.length() > 0) {
                                    for (s in sensors) {
                                        var found = false
                                        for (i in 0 until arrayIds.length()) {
                                            val chipId = arrayIds.get(i).toString()
                                            if (chipId == s.chipID) {
                                                found = true
                                                break
                                            }
                                        }
                                        if (!found) su.deleteExternalSensor(s.chipID)
                                    }
                                }
                                // Process update
                                for (i in 0 until arrayUpdate.length()) {
                                    val jsonObject = arrayUpdate.getJSONObject(i)
                                    val sensor = ExternalSensor()
                                    sensor.chipID = jsonObject.getString("i")
                                    sensor.lat = jsonObject.getDouble("l")
                                    sensor.lng = jsonObject.getDouble("b")
                                    su.addExternalSensor(sensor)
                                }
                                su.putLong("LastRequest", newLastRequest)
                                sensors = su.externalSensors
                                activity.runOnUiThread { drawSensorsToMap() }
                            } else {
                                activity.runOnUiThread { Toast.makeText(activity, R.string.error_try_again, Toast.LENGTH_SHORT).show() }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }).start()
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
                    map.clear()
                }
                for (sensor in sensors) {
                    if (isMarkerClusteringEnabled) {
                        val m = MarkerItem(sensor.chipID, sensor.lat.toString() + ", " + sensor.lng, LatLng(sensor.lat, sensor.lng))
                        clusterManager.addItem(SensorClusterItem(sensor.lat, sensor.lng, sensor.chipID, sensor.lat.toString() + ", " + sensor.lng, m))
                    } else {
                        map.addMarker(MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(if (su.isFavouriteExisting(sensor.chipID)) BitmapDescriptorFactory.HUE_RED else if (su.isSensorExisting(sensor.chipID)) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_BLUE))
                                .position(LatLng(sensor.lat, sensor.lng))
                                .title(sensor.chipID)
                                .snippet(sensor.lat.toString() + ", " + sensor.lng)
                        )
                    }
                }
                if(current_country != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(current_country, 5f))
                if (su.getBoolean("enable_marker_clustering", true)) clusterManager.cluster()
            }

            private fun loadAllSensorsNonSync() {
                Thread(Runnable {
                    try {
                        val start = System.currentTimeMillis()
                        // Load new sensors from server
                        val newLastRequest = System.currentTimeMillis()
                        val result = smu.sendRequest(contentView.findViewById(R.id.container), object : HashMap<String, String>() {
                            init {
                                put("command", "getallnonsync")
                            }
                        })
                        Log.i("FA", "Loading time: " + (System.currentTimeMillis() - start))
                        if (result.isNotEmpty()) {
                            su.clearExternalSensors()
                            val array = JSONArray(result)
                            sensors = ArrayList()
                            for (i in 0 until array.length()) {
                                val jsonObject = array.getJSONObject(i)
                                val sensor = ExternalSensor()
                                sensor.chipID = jsonObject.getString("i")
                                sensor.lat = jsonObject.getDouble("l")
                                sensor.lng = jsonObject.getDouble("b")
                                su.addExternalSensor(sensor)
                            }
                            su.putLong("LastRequest", newLastRequest)
                            sensors = su.externalSensors

                            // Draw sensors on the map
                            activity.runOnUiThread {
                                drawSensorsToMap()
                                if (pd.isShowing) pd.dismiss()
                            }
                        } else {
                            activity.runOnUiThread { pd.dismiss() }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }).start()
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

    class MySensorsFragment : Fragment() {
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