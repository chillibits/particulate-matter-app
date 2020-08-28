/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.other.User
import com.mrgames13.jimdo.feinstaubapp.network.createUser
import com.mrgames13.jimdo.feinstaubapp.shared.hashSha1
import kotlinx.android.synthetic.main.dialog_sign_up.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpDialog(
    private val context: Context
) {

    // Variables as objects
    private val dialogBuilder: MaterialStyledDialog.Builder
    private val view = LayoutInflater.from(context).inflate(R.layout.dialog_sign_up, null)
    var dialog: MaterialStyledDialog? = null
    private var listener: OnSignUpListener? = null

    // Interfaces
    interface OnSignUpListener {
        fun onSignedUp(user: User)
        fun onCancelled()
    }

    init {
        dialogBuilder = MaterialStyledDialog.Builder(context)
            .setHeaderDrawable(R.drawable.login_header)
            .withIconAnimation(false)
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.sign_up)
            .setCustomView(view)
            .setNegativeText(R.string.cancel)
            .setPositiveText(R.string.sign_up)
            .onNegative { dialog?.dismiss() }
            .onPositive {
                val email = view.email.text.toString().trim()
                val password = view.password.text.toString().trim()
                val confirmPassword = view.confirmPassword.text.toString().trim()
                createAccount(dialog, email, password, confirmPassword)
            }
            .autoDismiss(false)
    }

    fun setListener(listener: OnSignUpListener): SignUpDialog {
        this.listener = listener
        return this
    }

    fun show() {
        dialog = dialogBuilder.show()
    }

    private fun createAccount(dialog: MaterialStyledDialog?, email: String, password: String, confirmPassword: String) {
        // Check if form is filled correctly
        if(email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
            if(password == confirmPassword) {
                val buttonPos = dialog?.positiveButton()
                val buttonNeg = dialog?.negativeButton()
                buttonPos?.text = context.getString(R.string.loading)
                buttonPos?.isEnabled = false
                buttonNeg?.isEnabled = false
                // Create account on server
                CoroutineScope(Dispatchers.IO).launch {
                    val user = createUser(context, email, hashSha1(password))
                    withContext(Dispatchers.Main) {
                        if(user != null) {
                            // Sign in
                            listener?.onSignedUp(user)
                            dialog?.dismiss()
                        } else {
                            buttonNeg?.isEnabled = true
                            buttonPos?.isEnabled = true
                            buttonPos?.text = context.getString(R.string.sign_up)
                        }
                    }
                }
            } else Toast.makeText(context, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show()
        } else Toast.makeText(context, R.string.not_all_filled, Toast.LENGTH_SHORT).show()
    }
}