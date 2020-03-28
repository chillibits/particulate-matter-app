/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.activity

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.shared.Constants
import com.mrgames13.jimdo.feintaubapp.shared.availableSoon
import com.mrgames13.jimdo.feintaubapp.shared.getPrefs
import com.mrgames13.jimdo.feintaubapp.shared.outputErrorMessage
import com.mrgames13.jimdo.feintaubapp.ui.dialog.OnChooseColorDialogSelectionListener
import com.mrgames13.jimdo.feintaubapp.ui.dialog.WITH_COLOR_CONVERTER
import com.mrgames13.jimdo.feintaubapp.ui.dialog.showChooseColorDialog
import com.rtchagas.pingplacepicker.PingPlacePicker
import kotlinx.android.synthetic.main.activity_add_sensor.*
import kotlinx.android.synthetic.main.toolbar.*
import top.defaults.drawabletoolbox.DrawableBuilder
import kotlin.random.Random

class AddSensorActivity : AppCompatActivity(), OnChooseColorDialogSelectionListener {

    // Variables as objects
    private var selectedPlace: LatLng? = null

    // Variables
    private var selectedColor = Color.BLACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_sensor)

        // Initialize toolbar
        toolbar.setTitle(R.string.add_own_sensor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Apply window insets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets
            }
        }

        // Info icon
        chipIdInfo.setOnClickListener { openChipIdInfoSite() }

        // Select color buttons
        selectSensorColor.setOnClickListener { chooseColor() }
        sensorColorPreview.setOnClickListener { chooseColor() }

        // Randomize initial color
        createRandomColor()

        // Initialize pick sensor position button
        selectSensorPosition.setOnClickListener { openPlacePicker() }

        // Initialize publish switch
        publishSensor.setOnCheckedChangeListener { _, isChecked ->
            selectSensorPosition.isEnabled = isChecked
            addressPreview.isEnabled = isChecked
            lblHeight.isEnabled = isChecked
            height.isEnabled = isChecked
            publishExactPosition.isEnabled = isChecked
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_add_sensor, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
            R.id.action_done -> addSensor()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK) {
            when(requestCode) {
                Constants.REQ_PLACE_PICKER -> {
                    val place = PingPlacePicker.getPlace(data!!)
                    place?.let {
                        selectedPlace = it.latLng
                        onPlaceSelected(it)
                    }
                }
            }
        } else outputErrorMessage()
    }

    private fun addSensor() {
        availableSoon()
    }

    private fun openChipIdInfoSite() {
        Intent(Intent.ACTION_VIEW).run {
            data = Uri.parse(getString(R.string.url_chip_id_info))
            startActivity(this)
        }
    }

    private fun createRandomColor() {
        val random = Random(System.currentTimeMillis())
        selectedColor = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
        sensorColorPreview.setColorFilter(selectedColor, PorterDuff.Mode.SRC)
    }

    private fun chooseColor() {
        getPrefs().getInt(Constants.PREFS_CHOOSE_COLOR_REMEMBER, 0).let {
            when(it) {
                0 -> showChooseColorDialog(this, this)
                else -> onSelectOption(it)
            }
        }
    }

    private fun openPlacePicker() {
        val intent = PingPlacePicker.IntentBuilder()
            .setAndroidApiKey(getString(R.string.maps_api_key))
            .setMapsApiKey(getString(R.string.maps_api_key))
            .build(this)
        startActivityForResult(intent, Constants.REQ_PLACE_PICKER)
    }

    private fun onPlaceSelected(place: Place) {
        addressPreview.text = place.address
        // Initialize sensorPositionContainerBackground
        sensorPositionContainer.background = DrawableBuilder()
            .rectangle()
            .solidColor(ContextCompat.getColor(this, R.color.colorAddressPreview))
            .bottomLeftRadius(selectSensorPosition.height / 2)
            .topLeftRadius(selectSensorPosition.height / 2)
            .bottomRightRadius(selectSensorPosition.height / 2)
            .topRightRadius(selectSensorPosition.height / 2)
            .build()
        // run animation
        val initialHeight = selectSensorPosition.height
        ValueAnimator.ofFloat(0f, addressPreview.height.toFloat()).run {
            duration = 300
            addUpdateListener {
                val layoutParams = sensorPositionContainer.layoutParams
                layoutParams.height = (initialHeight + animatedValue as Float).toInt()
                sensorPositionContainer.layoutParams = layoutParams
            }
            start()
        }
    }

    override fun onSelectOption(selectedOption: Int) {
        when(selectedOption) {
            WITH_COLOR_CONVERTER -> {
                Intent(Intent.ACTION_VIEW).run {
                    data = Uri.parse(getString(R.string.color_converter_instant_url))
                    startActivity(this)
                }
            }
            else -> {
                MaterialColorPickerDialog.Builder(this)
                    .setTitle(R.string.choose_color)
                    .setColorListener { color, _ ->
                        selectedColor = color
                        sensorColorPreview.setColorFilter(color, PorterDuff.Mode.SRC)
                    }
                    .showBottomSheet(supportFragmentManager)
            }
        }
    }
}
