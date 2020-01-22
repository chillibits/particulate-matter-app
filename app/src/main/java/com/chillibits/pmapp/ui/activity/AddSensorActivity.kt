/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.chillibits.pmapp.model.Sensor
import com.chillibits.pmapp.network.ServerMessagingUtils
import com.chillibits.pmapp.network.addSensorOnServer
import com.chillibits.pmapp.network.isSensorDataExisting
import com.chillibits.pmapp.tool.StorageUtils
import com.chillibits.pmapp.tool.Tools
import com.chillibits.pmapp.ui.view.ProgressDialog
import com.google.android.libraries.places.api.model.Place
import com.mrgames13.jimdo.feinstaubapp.R
import com.rtchagas.pingplacepicker.PingPlacePicker
import kotlinx.android.synthetic.main.activity_add_sensor.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.margaritov.preference.colorpicker.ColorPickerDialog
import java.util.*

class AddSensorActivity : AppCompatActivity() {

    // Utils packages
    private lateinit var su: StorageUtils
    private lateinit var smu: ServerMessagingUtils

    // Variables
    private var currentColor: Int = 0
    private var mode = MODE_NEW
    private var target = TARGET_OWN_SENSOR

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_sensor)

        // Initialize toolbar
        toolbar.title = getString(R.string.add_own_sensor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets.consumeSystemWindowInsets()
            }
        }

        // Initialize StorageUtils
        su = StorageUtils(this)

        // Initialize ServerMessagingUtils
        smu = ServerMessagingUtils(this)

        // Initialize Components
        sensor_color.setOnClickListener { selectNewColor() }
        choose_sensor_color.setOnClickListener { selectNewColor() }
        chip_id_info.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_id_info)))) }

        // Initialize randomizer and choose random color
        val random = Random()
        currentColor = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
        sensor_color.setColorFilter(currentColor, PorterDuff.Mode.SRC)

        sensor_public.setOnCheckedChangeListener { _, b ->
            choose_location.isEnabled = b
            height_value.isEnabled = b
            coordinates_info.isEnabled = b
        }

        choose_location.setOnClickListener {
            val builder = PingPlacePicker.IntentBuilder()
            builder.setAndroidApiKey(getString(R.string.maps_api_key))
            builder.setMapsApiKey(getString(R.string.maps_api_key))
            startActivityForResult(builder.build(this), REQ_SELECT_PLACE)
        }

        coordinates_info.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.coordinates_info)
                .setPositiveButton(R.string.ok, null)
                .show()
        }

        // Get intent extras
        val i = intent
        if (i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_EDIT) {
            mode = MODE_EDIT
            sensor_name_value.setText(i.getStringExtra("Name"))
            chip_id_value.setText(i.getStringExtra("ID"))
            chip_id_value.isEnabled = false
            currentColor = i.getIntExtra("Color", currentColor)
            sensor_color.setColorFilter(currentColor, PorterDuff.Mode.SRC)
            toolbar.setTitle(R.string.edit_sensor)
            sensor_public.isChecked = false
            additional_info.visibility = View.GONE

            if (i.hasExtra("Target")) target = i.getIntExtra("Target", TARGET_OWN_SENSOR)

            edit_position_info.visibility = View.VISIBLE
            get_in_touch.setOnClickListener {
                val mail = Intent(Intent.ACTION_SENDTO)
                mail.data = Uri.parse("mailto:")
                mail.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.contact_email)))
                mail.putExtra(Intent.EXTRA_SUBJECT, "Change sensor position - chip id: ${i.getStringExtra("ID")}")
                mail.putExtra(Intent.EXTRA_TEXT, "Please write in English or in German and provide the exact address or the gps coordinates of the new position.")
                startActivity(Intent.createChooser(mail, getString(R.string.get_in_touch)))
            }
        } else if (i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_COMPLETE) {
            mode = MODE_COMPLETE
            sensor_name_value.setText(i.getStringExtra("Name"))
            chip_id_value.setText(i.getStringExtra("ID"))
            chip_id_value.isEnabled = false
            currentColor = i.getIntExtra("Color", currentColor)
            sensor_color.setColorFilter(currentColor, PorterDuff.Mode.SRC)
            toolbar.setTitle(R.string.complete_sensor)
            choose_location.requestFocus()

             AlertDialog.Builder(this)
                .setTitle(R.string.complete_sensor)
                .setMessage(R.string.sensor_position_completion_m_short)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_add_own_sensor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
            R.id.action_done -> addSensor(item)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectNewColor() {
        // Show color selection dialog
        val colorPicker = ColorPickerDialog(this, currentColor)
        colorPicker.alphaSliderVisible = false
        colorPicker.hexValueEnabled = true
        colorPicker.setTitle(getString(R.string.choose_color))
        colorPicker.setOnColorChangedListener { color ->
            currentColor = color
            sensor_color.setColorFilter(color, PorterDuff.Mode.SRC)
        }
        colorPicker.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SELECT_PLACE && resultCode == Activity.RESULT_OK) {
            val place: Place? = PingPlacePicker.getPlace(data!!)
            lat.setText(Tools.round(place?.latLng!!.latitude, 4).toString())
            lng.setText(Tools.round(place.latLng!!.longitude, 4).toString())
            choose_location.text = place.name
        }
    }

    private fun addSensor(item: MenuItem) {
        val chipId = chip_id_value.text.toString().trim()
        val sensorName = sensor_name_value.text.toString().trim()
        val lat = lat.text.toString()
        val lng = lng.text.toString()
        val alt = height_value.text.toString()

        if (chipId.isNotEmpty() && sensorName.isNotEmpty() && (!sensor_public.isChecked || lat.isNotEmpty() && lng.isNotEmpty() && alt.isNotEmpty())) {
            if (mode == MODE_NEW) {
                if (!su.isSensorExisting(chipId)) {
                    val pd = ProgressDialog(this)
                    pd.setMessage(getString(R.string.please_wait_))
                    pd.setDialogCancelable(false)
                    pd.show()

                    if (smu.isInternetAvailable) {
                        CoroutineScope(Dispatchers.IO).launch {
                            //Check, if data already is available on server
                            if (isSensorDataExisting(this@AddSensorActivity, chipId)) {
                                // Add sensor on server, if needed
                                if (sensor_public.isChecked) {
                                    val result = addSensorOnServer(this@AddSensorActivity, chipId, lat, lng, alt)
                                    if (result) {
                                        // Save new sensor
                                        if (su.isFavouriteExisting(chipId)) su.removeFavourite(chipId, false)
                                        su.addOwnSensor(Sensor(chipId, sensorName, currentColor), offline = false, requestFromRealtimeSyncService = false)
                                        CoroutineScope(Dispatchers.Main).launch {
                                            pd.dismiss()
                                            try { MainActivity.own_instance?.refresh() } catch (ignored: Exception) {}
                                            finish()
                                        }
                                    } else {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            pd.dismiss()
                                            Toast.makeText(this@AddSensorActivity, getString(R.string.error_try_again), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    // Save new sensor
                                    if (su.isFavouriteExisting(chipId)) su.removeFavourite(chipId, false)
                                    su.addOwnSensor(Sensor(chipId, sensorName, currentColor), offline = true, requestFromRealtimeSyncService = false)
                                    CoroutineScope(Dispatchers.Main).launch {
                                        try {
                                            MainActivity.own_instance?.refresh()
                                        } catch (ignored: Exception) {}
                                        finish()
                                    }
                                }
                            } else {
                                CoroutineScope(Dispatchers.Main).launch {
                                    AlertDialog.Builder(this@AddSensorActivity)
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.add_sensor_tick_not_set_message_required)
                                        .setPositiveButton(R.string.ok, null)
                                        .show()
                                }
                            }
                        }
                    } else {
                        pd.dismiss()
                        Toast.makeText(this@AddSensorActivity, getString(R.string.internet_is_not_available), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Sensor is already linked
                    Toast.makeText(this, getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show()
                }
            } else if (mode == MODE_EDIT) {
                // Update sensor
                if (target == TARGET_FAVOURITE) {
                    su.updateFavourite(Sensor(chipId, sensorName, currentColor), false)
                } else {
                    su.updateOwnSensor(Sensor(chipId, sensorName, currentColor), false)
                }
                try { MainActivity.own_instance?.refresh() } catch (e: Exception) {}
                finish()
            } else if (mode == MODE_COMPLETE) {
                su.removeOwnSensor(chipId, false)
                mode = MODE_NEW
                onOptionsItemSelected(item)
            }
        } else {
            // Not all fields filled
            Toast.makeText(this, getString(R.string.not_all_filled), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        // Constants
        private const val REQ_SELECT_PLACE = 10001
        const val MODE_NEW = 10001
        const val MODE_EDIT = 10002
        const val MODE_COMPLETE = 10003
        const val TARGET_FAVOURITE = 10003
        const val TARGET_OWN_SENSOR = 10004
    }
}