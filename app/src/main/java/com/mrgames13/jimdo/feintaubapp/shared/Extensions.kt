/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.shared

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.widget.Toast
import com.mrgames13.jimdo.feintaubapp.R

fun Context.getPrefs(): SharedPreferences {
    return getSharedPreferences("com.mrgames13.jimdo.feinstaubapp_preferences", Context.MODE_PRIVATE)
}

fun Context.outputErrorMessage() {
    Toast.makeText(this, R.string.error_try_again, Toast.LENGTH_SHORT).show()
}

fun Context.isNightModeEnabled() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES