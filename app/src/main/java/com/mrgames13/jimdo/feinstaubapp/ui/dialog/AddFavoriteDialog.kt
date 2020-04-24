/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.WindowManager
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.github.javiersantos.materialstyleddialogs.enums.Style
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic
import com.mikepenz.iconics.utils.colorInt
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dto.SensorDto
import com.mrgames13.jimdo.feinstaubapp.shared.openGooglePlayAppSite
import kotlinx.android.synthetic.main.dialog_add_favorite.view.*

fun Context.showAddFavoriteDialog(sensor: SensorDto) {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null)

    view.sensor_name_value.setText(sensor.city)
    view.sensor_chip_id_value.setText(sensor.chipId.toString())
    view.sensor_color.setOnClickListener {  }

    val dialog = MaterialStyledDialog.Builder(this)
        .setStyle(Style.HEADER_WITH_ICON)
        .withIconAnimation(false)
        .setIcon(IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_star_border).apply {
            colorInt = Color.WHITE
        })
        .setCustomView(view)
        .setPositiveText(R.string.add_favourite)
        .setNegativeText(R.string.cancel)
        .onPositive { _, _ -> openGooglePlayAppSite() }
        .show()

    view.sensor_name_value.selectAll()
    view.sensor_name_value.requestFocus()
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
}