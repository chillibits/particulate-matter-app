/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.core.content.ContextCompat
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.github.javiersantos.materialstyleddialogs.enums.Style
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.material_design_iconic_typeface_library.MaterialDesignIconic
import com.mrgames13.jimdo.feinstaubapp.R

fun Context.showRatingDialog() {
    MaterialStyledDialog.Builder(this)
        .setStyle(Style.HEADER_WITH_ICON)
        .setHeaderColorInt(ContextCompat.getColor(this, R.color.googlePlayHeaderColor))
        .withIconAnimation(false)
        .setIcon(IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_google_play).apply {
            colorInt = Color.WHITE
        })
        .setTitle(R.string.rate)
        .setDescription(R.string.rate_m)
        .setPositiveText(R.string.rate)
        .setNegativeText(R.string.cancel)
        .onPositive { _, _ ->
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }
        .show()
}