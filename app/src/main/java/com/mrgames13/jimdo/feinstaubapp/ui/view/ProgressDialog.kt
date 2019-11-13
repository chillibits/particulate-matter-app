/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.dialog_loading.view.*

class ProgressDialog(context: Context) {

    // Constants
    private val context = context

    // Variables as objects
    private val rootView: View = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
    private var dialog: AlertDialog

    // Variables

    init {
        dialog = AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(rootView)
                .create()
    }

    fun setTitle(message: String): ProgressDialog {
        dialog.setTitle(message)
        return this
    }

    fun setTitle(resId: Int): ProgressDialog {
        dialog.setTitle(resId)
        return this
    }

    fun setMessage(message: String): ProgressDialog {
        rootView.loading_label.text = message
        return this
    }

    fun setMessage(resId: Int): ProgressDialog {
        rootView.loading_label.text = context.getString(resId)
        return this
    }

    fun setDialogCancelable(cancelable: Boolean): ProgressDialog {
        dialog.setCancelable(cancelable)
        return this
    }

    fun show(): ProgressDialog {
        dialog.show()
        return this
    }

    fun dismiss() {
        dialog.dismiss()
    }

    fun isShowing(): Boolean {
        return dialog.isShowing
    }
}