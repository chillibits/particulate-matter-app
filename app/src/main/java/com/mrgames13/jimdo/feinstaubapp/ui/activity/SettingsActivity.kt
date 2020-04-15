/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.activity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.getPrefs
import com.mrgames13.jimdo.feinstaubapp.ui.fragment.SettingsFragment
import kotlinx.android.synthetic.main.toolbar.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set window insets
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                v.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets.consumeSystemWindowInsets()
            }
        }

        // Initialize SettingsFragment
        inflateSettingsFragment()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_activity_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
            R.id.action_reset -> resetSettings()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ApplySharedPref")
    private fun resetSettings() {
        // Delete all preference keys
        getPrefs().edit()
            .remove(Constants.PREF_ENABLE_DAILY_AVERAGE)
            .remove(Constants.PREF_ENABLE_DAILY_MEDIAN)
            .remove(Constants.PREF_MANAGE_AUTO_CORRECTION)
            .remove(Constants.PREF_SYNC_CYCLE)
            .remove(Constants.PREF_SYNC_CYCLE_BACKGROUND)
            .remove(Constants.PREF_REDUCE_DATA_CONSUMPTION)
            .remove(Constants.PREF_DEFAULT_MAP_TYPE)
            .remove(Constants.PREF_DEFAULT_TRAFFIC)
            .remove(Constants.PREF_NOTIFICATION_THRESHOLD)
            .remove(Constants.PREF_NOTIFICATION_AVERAGES)
            .remove(Constants.PREF_NOTIFICATION_BREAKDOWN)
            .remove(Constants.PREF_ENABLE_MARKER_CLUSTERING)
            .remove(Constants.PREF_INCREASE_DIAGRAM_PERFORMANCE)
            .commit()
        inflateSettingsFragment()
    }

    private fun inflateSettingsFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_fragment, SettingsFragment())
            .commit()
    }
}
