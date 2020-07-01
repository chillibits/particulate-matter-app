/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.activity

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.databinding.ActivityAddSensorBinding
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.availableSoon
import com.mrgames13.jimdo.feinstaubapp.shared.getPrefs
import com.mrgames13.jimdo.feinstaubapp.shared.outputErrorMessage
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.OnChooseColorDialogSelectionListener
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.WITH_COLOR_CONVERTER
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showChooseColorDialog
import com.mrgames13.jimdo.feinstaubapp.viewmodel.AddSensorViewModel
import com.rtchagas.pingplacepicker.PingPlacePicker
import kotlinx.android.synthetic.main.activity_add_sensor.*
import kotlinx.android.synthetic.main.toolbar.*
import top.defaults.drawabletoolbox.DrawableBuilder

class AddSensorActivity : AppCompatActivity(), OnChooseColorDialogSelectionListener {

    // Variables as objects
    private lateinit var binding: ActivityAddSensorBinding
    private lateinit var viewModel: AddSensorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize data binding and view model
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_sensor)
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory
            .getInstance(application)).get(AddSensorViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

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

        binding.sensorColorPreview.setColorFilter(viewModel.selectedColor.value!!, PorterDuff.Mode.SRC)

        // Expand address preview if address field is already set
        if(!viewModel.address.value.isNullOrBlank()) expandAddressPreview()
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
        if(resultCode == Activity.RESULT_OK && data != null) {
            when(requestCode) {
                Constants.REQ_PLACE_PICKER -> {
                    PingPlacePicker.getPlace(data)?.let {
                        viewModel.selectedPlace.postValue(it.latLng)
                        viewModel.address.postValue(it.address)
                        expandAddressPreview()
                    }
                }
                Constants.REQ_COLOR_CONVERTER -> {
                    if(data.hasExtra(Constants.EXTRA_COLOR_CONVERTER)) {
                        val color = data.getIntExtra(Constants.EXTRA_COLOR_CONVERTER, viewModel.selectedColor.value!!)
                        viewModel.selectedColor.postValue(color)
                        sensorColorPreview.setColorFilter(color)
                    }
                }
            }
        } else outputErrorMessage()
    }

    fun openPlacePicker(view: View) {
        val intent = PingPlacePicker.IntentBuilder()
            .setAndroidApiKey(getString(R.string.maps_api_key))
            .setMapsApiKey(getString(R.string.maps_api_key))
            .build(this)
        startActivityForResult(intent, Constants.REQ_PLACE_PICKER)
    }

    private fun expandAddressPreview() {
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

    fun openChipIdInfoSite(view: View) {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(getString(R.string.url_chip_id_info))
        })
    }

    fun chooseColor(view: View) {
        getPrefs().getInt(Constants.PREFS_CHOOSE_COLOR_REMEMBER, 0).let {
            when(it) {
                0 -> showChooseColorDialog(this, this)
                else -> onSelectOption(it)
            }
        }
    }

    private fun addSensor() {
        availableSoon()
    }

    override fun onSelectOption(selectedOption: Int) {
        when(selectedOption) {
            WITH_COLOR_CONVERTER -> {
                startActivityForResult(Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(getString(R.string.url_instant_color_converter))
                    putExtra(Constants.EXTRA_COLOR_CONVERTER, viewModel.selectedColor.value)
                }, Constants.REQ_COLOR_CONVERTER)
            }
            else -> {
                MaterialColorPickerDialog.Builder(this)
                    .setTitle(R.string.choose_color)
                    .setColorListener { color, _ ->
                        viewModel.selectedColor.postValue(color)
                        sensorColorPreview.setColorFilter(color, PorterDuff.Mode.SRC)
                    }
                    .showBottomSheet(supportFragmentManager)
            }
        }
    }
}