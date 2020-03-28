/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.activity

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
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.shared.Constants
import com.mrgames13.jimdo.feintaubapp.shared.availableSoon
import com.mrgames13.jimdo.feintaubapp.shared.getPrefs
import com.mrgames13.jimdo.feintaubapp.ui.dialog.OnChooseColorDialogSelectionListener
import com.mrgames13.jimdo.feintaubapp.ui.dialog.WITH_COLOR_CONVERTER
import com.mrgames13.jimdo.feintaubapp.ui.dialog.showChooseColorDialog
import kotlinx.android.synthetic.main.activity_add_sensor.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlin.random.Random

class AddSensorActivity : AppCompatActivity(), OnChooseColorDialogSelectionListener {

    // Variables as objects

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
        val random = Random(System.currentTimeMillis())
        selectedColor = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255))
        sensorColorPreview.setColorFilter(selectedColor, PorterDuff.Mode.SRC)
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

    private fun addSensor() {
        availableSoon()
    }

    private fun openChipIdInfoSite() {
        Intent(Intent.ACTION_VIEW).run {
            data = Uri.parse(getString(R.string.url_chip_id_info))
            startActivity(this)
        }
    }

    private fun chooseColor() {
        getPrefs().getInt(Constants.PREFS_CHOOSE_COLOR_REMEMBER, 0).let {
            when(it) {
                0 -> showChooseColorDialog(this, this)
                else -> onSelectOption(it)
            }
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
