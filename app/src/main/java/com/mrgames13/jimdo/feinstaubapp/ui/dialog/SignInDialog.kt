/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.android.synthetic.main.dialog_sign_in.view.*

fun Context.showSignInDialog() {
    val v = LayoutInflater.from(this).inflate(R.layout.dialog_sign_in, null)

    val d = MaterialStyledDialog.Builder(this)
        .setHeaderDrawable(R.drawable.login_header)
        .withIconAnimation(false)
        .setIcon(R.mipmap.ic_launcher)
        .setTitle(R.string.sign_in)
        .setCustomView(v)
        .setNegativeText(R.string.cancel)
        .setPositiveText(R.string.sign_in)
        .onNegative { dialog, _ -> dialog.dismiss() }
        .onPositive {dialog, which ->

        }
        .autoDismiss(false)
        .show()

    v.signUp.movementMethod = LinkMovementMethod.getInstance()
    v.signUp.paint?.isUnderlineText = true
    v.signUp.setOnClickListener {
        d.dismiss()
        showSignUpDialog()
    }
}