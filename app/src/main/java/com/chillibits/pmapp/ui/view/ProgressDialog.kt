/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.ui.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.chillibits.pmapp.R
import kotlinx.android.synthetic.main.dialog_loading.view.*

class ProgressDialog(private val context: Context) {

    // Variables as objects
    private val rootView: View = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
    private var dialog: AlertDialog

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