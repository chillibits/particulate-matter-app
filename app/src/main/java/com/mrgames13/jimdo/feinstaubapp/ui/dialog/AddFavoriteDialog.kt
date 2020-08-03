/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.FragmentManager
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.github.javiersantos.materialstyleddialogs.enums.Style
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.materialdesigniconic.MaterialDesignIconic
import com.mikepenz.iconics.utils.colorInt
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dbo.SensorDbo
import com.mrgames13.jimdo.feinstaubapp.model.dbo.UserDbo
import com.mrgames13.jimdo.feinstaubapp.model.dto.SensorDto
import com.mrgames13.jimdo.feinstaubapp.model.other.Link
import com.mrgames13.jimdo.feinstaubapp.network.addLink
import kotlinx.android.synthetic.main.dialog_add_favorite.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

private var selectedColor = 0
private lateinit var sensorColorPreview: AppCompatImageView

fun Context.showAddFavoriteDialog(sensor: SensorDto, user: UserDbo?, fragmentManager: FragmentManager) {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_favorite, null)
    sensorColorPreview = view.sensor_color

    val rnd = Random(System.currentTimeMillis())
    selectedColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    sensorColorPreview.setColorFilter(selectedColor)

    view.sensor_name_value.setText(sensor.city)
    view.sensor_chip_id_value.setText(sensor.chipId.toString())

    view.sensor_color.setOnClickListener { chooseColor(fragmentManager) }
    view.choose_sensor_color.setOnClickListener { chooseColor(fragmentManager) }

    val dialog = MaterialStyledDialog.Builder(this)
        .setStyle(Style.HEADER_WITH_ICON)
        .withIconAnimation(false)
        .setIcon(IconicsDrawable(this, MaterialDesignIconic.Icon.gmi_star_border).apply {
            colorInt = Color.WHITE
        })
        .setCustomView(view)
        .setPositiveText(R.string.add_favourite)
        .setNegativeText(R.string.cancel)
        .onPositive {
            if(user != null) {
                // Add sensor on the server and sync the changes later
                addSensorToFavorites(sensor, user, view.sensor_name_value.text.toString().trim(), selectedColor)
            } else {
                // Add the sensor only locally, because the app user skipped the sign in process
                // TODO: Implement the possibility to add a favorite only locally
            }
        }
        .show()

    view.sensor_name_value.selectAll()
    view.sensor_name_value.requestFocus()
    dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
}

private fun Context.addSensorToFavorites(sensorDto: SensorDto, user: UserDbo, name: String, color: Int) {
    // Add Favorite on server
    val pd = ProgressDialog(this)
        .setDialogCancelable(false)
        .show()
    CoroutineScope(Dispatchers.IO).launch {
        val sensor = SensorDbo(sensorDto.chipId, name, color, isOwner = false, isPublished = true)
        val link = Link(0, user, sensor, false, name, color, 0)
        val result = addLink(this@addSensorToFavorites, link, sensor.chipId)
        withContext(Dispatchers.Main) {
            pd.dismiss()
            if(!result) Toast.makeText(this@addSensorToFavorites, R.string.error_try_again, Toast.LENGTH_SHORT).show()
        }
    }
}

private fun Context.chooseColor(fragmentManager: FragmentManager) {
    MaterialColorPickerDialog.Builder(this)
        .setTitle(R.string.choose_color)
        .setColorListener { color, _ ->
            selectedColor = color
            sensorColorPreview.setColorFilter(color, PorterDuff.Mode.SRC)
        }
        .showBottomSheet(fragmentManager)
}