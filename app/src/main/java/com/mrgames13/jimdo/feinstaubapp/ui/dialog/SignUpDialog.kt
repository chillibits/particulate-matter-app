/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.network.createUser
import io.ktor.util.sha1
import kotlinx.android.synthetic.main.dialog_sign_up.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun Context.showSignUpDialog() {
    val v = LayoutInflater.from(this).inflate(R.layout.dialog_sign_up, null)

    v.email.doOnTextChanged { text, _, _, _ ->
        v.emailCorrect.visibility =
            if(Patterns.EMAIL_ADDRESS.matcher(text.toString().trim()).matches()) View.VISIBLE else View.GONE
    }

    MaterialStyledDialog.Builder(this)
        .setHeaderDrawable(R.drawable.login_header)
        .withIconAnimation(false)
        .setIcon(R.mipmap.ic_launcher)
        .setTitle(R.string.sign_up)
        .setCustomView(v)
        .setNegativeText(R.string.cancel)
        .setPositiveText(R.string.sign_up)
        .onNegative { dialog, _ -> dialog.dismiss() }
        .onPositive {dialog, _ ->
            val email = v.email.text.toString().trim()
            val password = v.password.text.toString().trim()
            val confirmPassword = v.confirmPassword.text.toString().trim()
            createAccount(dialog, email, password, confirmPassword)
        }
        .autoDismiss(false)
        .show()
}

private fun Context.createAccount(dialog: MaterialDialog, email: String, password: String, confirmPassword: String) {
    // Check if form is filled correctly
    if(email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
        if(password == confirmPassword) {
            val buttonPos = dialog.getActionButton(DialogAction.POSITIVE)
            val buttonNeg = dialog.getActionButton(DialogAction.NEGATIVE)
            buttonPos.text = getString(R.string.loading)
            buttonPos.isEnabled = false
            buttonNeg.isEnabled = false
            // Create account on server
            CoroutineScope(Dispatchers.IO).launch {
                val passwordHash = sha1(password.toByteArray()).toString()
                val success = createUser(this@createAccount, email, passwordHash)
                withContext(Dispatchers.Main) {
                    if(success) {
                        // Save email and hashed password


                        dialog.dismiss()
                    } else {
                        buttonNeg.isEnabled = true
                        buttonPos.isEnabled = true
                        buttonPos.text = getString(R.string.sign_up)
                    }
                }
            }
        } else Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show()
    } else Toast.makeText(this, R.string.not_all_filled, Toast.LENGTH_SHORT).show()
}