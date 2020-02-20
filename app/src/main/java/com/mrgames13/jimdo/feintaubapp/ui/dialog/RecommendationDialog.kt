/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.dialog

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feintaubapp.R

fun Context.showRecommendationDialog() {
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.recommend))
        .setMessage(getString(R.string.recommend_m))
        .setIcon(R.mipmap.ic_launcher)
        .setPositiveButton(getString(R.string.recommend)) { _, _ ->
            val i = Intent(Intent.ACTION_SEND)
            i.putExtra(Intent.EXTRA_TEXT, getString(R.string.recommend_string))
            i.type = "text/plain"
            startActivity(i)
        }
        .setNegativeButton(getString(R.string.cancel), null)
        .show()
}