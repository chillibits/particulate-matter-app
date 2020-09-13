/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.other.User
import com.mrgames13.jimdo.feinstaubapp.network.loadUser
import com.mrgames13.jimdo.feinstaubapp.shared.hashSha256
import com.mrgames13.jimdo.feinstaubapp.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.dialog_sign_in.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignInDialog(
    private val context: Context,
    private val viewModel: MainViewModel
) : SignUpDialog.OnSignUpListener {

    // Variables as objects
    private val dialogBuilder: MaterialStyledDialog.Builder
    private val view = LayoutInflater.from(context).inflate(R.layout.dialog_sign_in, null)
    private var dialog: MaterialStyledDialog? = null
    private var listener: OnSignInListener? = null

    // Interfaces
    interface OnSignInListener {
        fun onSignedIn()
        fun onSkipOrCancelled()
    }

    init {
        dialogBuilder = MaterialStyledDialog.Builder(context)
            .setHeaderDrawable(R.drawable.login_header)
            .withIconAnimation(false)
            .setIcon(R.mipmap.ic_launcher)
            .setTitle(R.string.sign_in)
            .setCustomView(view)
            .setNegativeText(R.string.cancel)
            .setPositiveText(R.string.sign_in)
            .onNegative {
                dialog?.dismiss()
                listener?.onSkipOrCancelled()
            }
            .onPositive {
                val email = view.email.text.toString().trim()
                val password = view.password.text.toString().trim()
                signIn(email, password)
            }
            .autoDismiss(false)

        view.signUp.movementMethod = LinkMovementMethod.getInstance()
        view.signUp.paint?.isUnderlineText = true
        view.signUp.setOnClickListener {
            dialog?.dismiss()
            // Open SignUpDialog
            SignUpDialog(context)
                .setListener(this)
                .show()
        }
    }

    fun setOnSignInListener(listener: OnSignInListener): SignInDialog {
        this.listener = listener
        return this
    }

    fun withSkipOption(): SignInDialog {
        dialogBuilder.setNegativeText(R.string.skip)
        return this
    }

    fun show() {
        dialog = dialogBuilder.show()
    }

    private fun signIn(email: String, password: String) {
        startStopSignInProcess(true)
        // Sign in
        CoroutineScope(Dispatchers.IO).launch {
            val userDto = loadUser(context, email, hashSha256(password))
            if(userDto != null) {
                // Save user as signed in
                viewModel.signIn(userDto)
                // Close dialog and call callback
                withContext(Dispatchers.Main) {
                    listener?.onSignedIn()
                    dialog?.dismiss()
                }
            } else {
                withContext(Dispatchers.Main) { startStopSignInProcess(false) }
            }
        }
    }

    private fun startStopSignInProcess(running: Boolean) {
        val buttonPos = dialog?.positiveButton()
        val buttonNeg = dialog?.negativeButton()
        buttonPos?.text = context.getString(if(running) R.string.loading else R.string.sign_in)
        buttonPos?.isEnabled = !running
        buttonNeg?.isEnabled = !running
    }

    override fun onSignedUp(user: User) = signIn(user.email, user.password)

    override fun onCancelled() {
        listener?.onSkipOrCancelled()
    }
}