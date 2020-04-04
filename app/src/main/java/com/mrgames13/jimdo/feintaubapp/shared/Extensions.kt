/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.shared

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.widget.Toast
import androidx.room.Room
import com.mrgames13.jimdo.feintaubapp.R
import com.mrgames13.jimdo.feintaubapp.storage.AppDatabase

// --------------------------------------- Context Extensions --------------------------------------

fun Context.getPrefs(): SharedPreferences {
    return getSharedPreferences("com.mrgames13.jimdo.feinstaubapp_preferences", Context.MODE_PRIVATE)
}

fun Context.getDatabase(): AppDatabase {
    return Room.databaseBuilder(applicationContext, AppDatabase::class.java, Constants.DB_NAME).build()
}

fun Context.outputErrorMessage() {
    Toast.makeText(this, R.string.error_try_again, Toast.LENGTH_SHORT).show()
}

fun Context.isNightModeEnabled() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

fun Context.availableSoon() {
    Toast.makeText(this, getString(R.string.available_soon), Toast.LENGTH_SHORT).show()
}