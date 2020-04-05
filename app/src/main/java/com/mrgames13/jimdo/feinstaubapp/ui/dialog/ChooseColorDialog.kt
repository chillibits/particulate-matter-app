/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.getPrefs
import kotlinx.android.synthetic.main.dialog_choose_color.view.*

const val WITH_COLOR_CONVERTER = 1
const val OPEN_COLOR_PICKER = 2

interface OnChooseColorDialogSelectionListener {
    fun onSelectOption(selectedOption: Int)
}

fun showChooseColorDialog(context: Context, listener: OnChooseColorDialogSelectionListener) {
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_choose_color, null)

    val dialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .show()

    dialogView.withColorConverter.setOnClickListener {
        if(dialogView.rememberSelection.isChecked)
            context.getPrefs().edit().putInt(Constants.PREFS_CHOOSE_COLOR_REMEMBER, WITH_COLOR_CONVERTER).apply()
        listener.onSelectOption(WITH_COLOR_CONVERTER)
        dialog.dismiss()
    }
    dialogView.openColorPicker.setOnClickListener {
        if(dialogView.rememberSelection.isChecked)
            context.getPrefs().edit().putInt(Constants.PREFS_CHOOSE_COLOR_REMEMBER, OPEN_COLOR_PICKER).apply()
        listener.onSelectOption(OPEN_COLOR_PICKER)
        dialog.dismiss()
    }

    dialogView.rememberSelectionContainer.setOnClickListener {
        dialogView.rememberSelection.toggle()
    }
}