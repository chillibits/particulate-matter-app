/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feintaubapp.R

fun Context.showRatingDialog() {
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.rate))
        .setMessage(getString(R.string.rate_m))
        .setIcon(R.mipmap.ic_launcher)
        .setPositiveButton(getString(R.string.rate)) { _, _ ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }
        .setNegativeButton(getString(R.string.cancel), null)
        .show()
}