/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.shared

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.room.Room
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.storage.AppDatabase

// --------------------------------------- Context Extensions --------------------------------------

fun Context.getPrefs(): SharedPreferences
        = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
fun Context.getDatabase()
        = Room.databaseBuilder(applicationContext, AppDatabase::class.java, Constants.DB_NAME).build()
fun Context.outputErrorMessage()
        = Toast.makeText(this, R.string.error_try_again, Toast.LENGTH_SHORT).show()
fun Context.isNightModeEnabled()
        = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
fun Context.availableSoon()
        = Toast.makeText(this, getString(R.string.available_soon), Toast.LENGTH_SHORT).show()
fun Context.getPreferenceValue(name: String, defaultValue: Int)
        = getPrefs().getInt(name, defaultValue)
fun Context.getPreferenceValue(name: String, defaultValue: Boolean)
        = getPrefs().getBoolean(name, defaultValue)
fun Context.getPreferenceValue(name: String, defaultValue: Double)
        = getPrefs().getFloat(name, defaultValue.toFloat()).toDouble()
fun Context.getPreferenceValue(name: String, defaultValue: String)
        = getPrefs().getString(name, defaultValue)
fun Context.openGooglePlayAppSite() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}

fun Context.openGooglePlayDeveloperSite() {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_store_developer_site))))
}

fun Context.convertDpToPx(dp: Double) = dp * resources.displayMetrics.density