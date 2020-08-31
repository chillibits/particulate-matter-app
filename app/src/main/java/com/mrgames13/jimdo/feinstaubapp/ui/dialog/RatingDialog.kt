/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.chillibits.simplesettings.tool.openGooglePlayAppSite
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.github.javiersantos.materialstyleddialogs.enums.Style
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic
import com.mikepenz.iconics.utils.colorInt
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
        .onPositive { openGooglePlayAppSite() }
        .show()
}