/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.shared.availableSoon
import kotlinx.android.synthetic.main.activity_add_sensor.*
import kotlinx.android.synthetic.main.toolbar.*

class AddSensorActivity : AppCompatActivity() {

    // Variables as objects

    // Variables

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_sensor)

        // Initialize toolbar
        toolbar.setTitle(R.string.add_own_sensor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Apply window insets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets
            }
        }

        // Info icon
        chipIdInfo.setOnClickListener { openChipIdInfoSite() }

        // Select color buttons
        selectSensorColor.setOnClickListener { selectSensorColor() }
        sensorColorPreview.setOnClickListener { selectSensorColor() }
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

    private fun selectSensorColor() {
        availableSoon()
    }

    private fun openChipIdInfoSite() {
        Intent(Intent.ACTION_VIEW).run {
            data = Uri.parse(getString(R.string.url_chip_id_info))
            startActivity(this)
        }
    }
}
