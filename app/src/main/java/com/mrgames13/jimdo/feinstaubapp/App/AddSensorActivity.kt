/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.App

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.google.android.libraries.places.api.model.Place
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools
import com.rtchagas.pingplacepicker.PingPlacePicker
import net.margaritov.preference.colorpicker.ColorPickerDialog
import java.util.*

class AddSensorActivity : AppCompatActivity() {

    // Variables as objects
    private lateinit var toolbar: Toolbar
    private lateinit var revealView: View
    private lateinit var revealBackgroundView: View
    private lateinit var ivColor: ImageView
    private lateinit var sensorName1: EditText
    private lateinit var chipId: EditText
    private lateinit var sensorPublic: SwitchCompat
    private lateinit var chooseLocation: Button
    private lateinit var lat: EditText
    private lateinit var lng: EditText
    private lateinit var coordinatesInfo: ImageView
    private lateinit var alt: EditText

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
        toolbar = findViewById(R.id.toolbar)
        toolbar.title = getString(R.string.add_own_sensor)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

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
        smu = ServerMessagingUtils(this, su)

        // Initialize RevealView
        revealView = findViewById(R.id.reveal)
        revealBackgroundView = findViewById(R.id.reveal_background)

        // Initialize Components
        ivColor = findViewById(R.id.sensor_color)
        ivColor.setOnClickListener { selectNewColor() }

        val chooseColor = findViewById<Button>(R.id.choose_sensor_color)
        chooseColor.setOnClickListener { selectNewColor() }

        // Initialize randomizer and choose random color
        val random = Random()
        currentColor = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
        ivColor.setColorFilter(currentColor, PorterDuff.Mode.SRC)

        sensorName1 = findViewById(R.id.sensor_name_value)
        chipId = findViewById(R.id.chip_id_value)

        val info = findViewById<ImageView>(R.id.chip_id_info)
        info.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_id_info)))) }

        sensorPublic = findViewById(R.id.sensor_public)
        sensorPublic.setOnCheckedChangeListener { _, b ->
            chooseLocation.isEnabled = b
            lat.isEnabled = b
            lng.isEnabled = b
            alt.isEnabled = b
            coordinatesInfo.isEnabled = b
        }

        chooseLocation = findViewById(R.id.choose_location)
        chooseLocation.setOnClickListener {
            val builder = PingPlacePicker.IntentBuilder()
            builder.setAndroidApiKey(getString(R.string.maps_api_key))
            builder.setMapsApiKey(getString(R.string.maps_api_key))
            startActivityForResult(builder.build(this@AddSensorActivity), REQ_SELECT_PLACE)
        }

        lat = findViewById(R.id.lat)
        lng = findViewById(R.id.lng)
        alt = findViewById(R.id.height_value)

        coordinatesInfo = findViewById(R.id.coordinates_info)
        coordinatesInfo.setOnClickListener {
            val d = AlertDialog.Builder(this@AddSensorActivity)
                    .setCancelable(true)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.coordinates_info)
                    .setPositiveButton(R.string.ok, null)
                    .create()
            d.show()
        }

        // Get intent extras
        val i = intent
        if (i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_EDIT) {
            mode = MODE_EDIT
            sensorName1.setText(i.getStringExtra("Name"))
            chipId.setText(i.getStringExtra("ID"))
            chipId.isEnabled = false
            currentColor = i.getIntExtra("Color", currentColor)
            ivColor.setColorFilter(currentColor, PorterDuff.Mode.SRC)
            toolbar.setTitle(R.string.edit_sensor)
            sensorPublic.isChecked = false
            findViewById<View>(R.id.additional_info).visibility = View.GONE

            if (i.hasExtra("Target")) target = i.getIntExtra("Target", TARGET_OWN_SENSOR)

            findViewById<View>(R.id.edit_position_info).visibility = View.VISIBLE
            val infoText = findViewById<TextView>(R.id.edit_position_info_text)
            infoText.movementMethod = LinkMovementMethod.getInstance()
        } else if (i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_COMPLETE) {
            mode = MODE_COMPLETE
            sensorName1.setText(i.getStringExtra("Name"))
            chipId.setText(i.getStringExtra("ID"))
            chipId.isEnabled = false
            currentColor = i.getIntExtra("Color", currentColor)
            ivColor.setColorFilter(currentColor, PorterDuff.Mode.SRC)
            toolbar.setTitle(R.string.complete_sensor)
            chooseLocation.requestFocus()

            val d = AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.complete_sensor)
                    .setMessage(R.string.sensor_position_completion_m_short)
                    .setPositiveButton(R.string.ok, null)
                    .create()
            d.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_activity_add_own_sensor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        } else if (id == R.id.action_done) {
            addSensor(item)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun selectNewColor() {
        // Show color selection dialog
        val colorPicker = ColorPickerDialog(this@AddSensorActivity, currentColor)
        colorPicker.alphaSliderVisible = false
        colorPicker.hexValueEnabled = true
        colorPicker.setTitle(getString(R.string.choose_color))
        colorPicker.setOnColorChangedListener { color ->
            currentColor = color
            animateToolAndStatusBar(color)
            ivColor.setColorFilter(color, PorterDuff.Mode.SRC)
        }
        colorPicker.show()
    }

    private fun animateToolAndStatusBar(toColor: Int) {
        // Show animation for statusBar and toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val animator = ViewAnimationUtils.createCircularReveal(revealView, toolbar.width / 2, toolbar.height / 2, 0f, (toolbar.width / 2 + 50).toFloat())
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    revealView.setBackgroundColor(toColor)
                }

                override fun onAnimationEnd(animation: Animator) {
                    revealBackgroundView.setBackgroundColor(toColor)
                }
            })

            animator.duration = 480
            animator.start()
            revealView.visibility = View.VISIBLE
        } else {
            revealView.setBackgroundColor(toColor)
            revealBackgroundView.setBackgroundColor(toColor)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SELECT_PLACE && resultCode == Activity.RESULT_OK) {
            val place: Place? = PingPlacePicker.getPlace(data!!)
            lat.setText(Tools.round(place?.latLng!!.latitude, 5).toString())
            lng.setText(Tools.round(place.latLng!!.longitude, 5).toString())
            chooseLocation.text = place.name
        }
    }

    private fun addSensor(item: MenuItem) {
        val chipId = this.chipId.text.toString().trim { it <= ' ' }
        val sensorName = this.sensorName1.text.toString().trim { it <= ' ' }
        val lat = this.lat.text.toString()
        val lng = this.lng.text.toString()
        val alt = this.alt.text.toString()

        if (chipId.isNotEmpty() && sensorName.isNotEmpty() && (!sensorPublic.isChecked || lat.isNotEmpty() && lng.isNotEmpty() && alt.isNotEmpty())) {
            if (mode == MODE_NEW) {
                if (!su.isSensorExisting(chipId)) {
                    val pd = ProgressDialog(this)
                    pd.setMessage(getString(R.string.please_wait_))
                    pd.setCancelable(false)
                    pd.show()

                    if (smu.isInternetAvailable) {
                        Thread(Runnable {
                            //Check, if data already is available on server
                            var result = smu.sendRequest(findViewById(R.id.container), object : HashMap<String, String>() {
                                init {
                                    put("command", "issensordataexisting")
                                    put("chipId", chipId)
                                }
                            })
                            pd.dismiss()
                            if (java.lang.Boolean.parseBoolean(result)) {
                                // Possibly add sensor on server
                                if (sensorPublic.isChecked) {
                                    result = smu.sendRequest(null, object : HashMap<String, String>() {
                                        init {
                                            put("command", "addsensor")
                                            put("chipId", chipId)
                                            put("lat", Tools.round(java.lang.Double.parseDouble(lat), 3).toString())
                                            put("lng", Tools.round(java.lang.Double.parseDouble(lng), 3).toString())
                                            put("alt", alt)
                                        }
                                    })
                                    if (result == "1") {
                                        // Save new sensor
                                        if (su.isFavouriteExisting(chipId)) su.removeFavourite(chipId, false)
                                        su.addOwnSensor(Sensor(chipId, sensorName, currentColor), offline = false, request_from_realtime_sync_service = false)
                                        runOnUiThread {
                                            try {
                                                MainActivity.own_instance.refresh()
                                            } catch (ignored: Exception) {}
                                            finish()
                                        }
                                    } else {
                                        runOnUiThread {
                                            pd.dismiss()
                                            Toast.makeText(this@AddSensorActivity, getString(R.string.error_try_again), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    // Save new sensor
                                    if (su.isFavouriteExisting(chipId)) su.removeFavourite(chipId, false)
                                    su.addOwnSensor(Sensor(chipId, sensorName, currentColor), offline = true, request_from_realtime_sync_service = false)
                                    runOnUiThread {
                                        try {
                                            MainActivity.own_instance.refresh()
                                        } catch (ignored: Exception) {}
                                        finish()
                                    }
                                }
                            } else {
                                runOnUiThread {
                                    val d = AlertDialog.Builder(this@AddSensorActivity)
                                            .setCancelable(true)
                                            .setTitle(R.string.app_name)
                                            .setMessage(R.string.add_sensor_tick_not_set_message_duty)
                                            .setPositiveButton(R.string.ok, null)
                                            .create()
                                    d.show()
                                }
                            }
                        }).start()
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
                try {
                    MainActivity.own_instance.refresh()
                } catch (e: Exception) {}
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