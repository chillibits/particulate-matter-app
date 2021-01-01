/*
 * Copyright © Marc Auberer 2017 - 2021. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.dbo.UserDbo
import kotlinx.android.synthetic.main.dialog_account.view.*

fun Context.showAccountDialog(user: UserDbo) {
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_account, null)

    // Initialize components
    var editingActive = false
    view.dataProtection.movementMethod = LinkMovementMethod.getInstance()
    view.dataProtection.paint?.isUnderlineText = true
    view.dataProtection.setOnClickListener {

    }
    view.deleteAccount.movementMethod = LinkMovementMethod.getInstance()
    view.deleteAccount.paint?.isUnderlineText = true
    view.deleteAccount.setOnClickListener { showDeleteAccountDialog(user) }

    view.changePersonalData.setOnClickListener {
        editingActive = !editingActive
        view.firstName.isEnabled = editingActive
        view.lastName.isEnabled = editingActive
        view.email.isEnabled = editingActive
        view.dataProtection.dataProtection.text = if(editingActive) getString(R.string.save_changes) else getString(R.string.loading)

        val firstNameValue = view.firstName.text.toString()
        val lastNameValue = view.lastName.text.toString()
        val emailValue = view.email.text.toString()

        if(!editingActive) savePersonalData(firstNameValue, lastNameValue, emailValue)
    }

    view.email.addTextChangedListener { text ->
        view.changePersonalData.isEnabled = text.toString().isNotEmpty()
    }

    AlertDialog.Builder(this)
        .setView(view)
        .setPositiveButton(R.string.cancel, null)
        .setNeutralButton(R.string.sign_out) { _, _ ->
            // Sign out
            // TODO: Implement sign out process
        }
        .show()
}

private fun savePersonalData(firstName: String, lastName: String, email: String) {

}

private fun Context.showDeleteAccountDialog(user: UserDbo) {
    // Show delete dialog
    val dialog = AlertDialog.Builder(this)
        .setIcon(R.drawable.delete_red)
        .setTitle(R.string.delete_account)
        .setMessage(R.string.delete_account_message)
        .setPositiveButton(R.string.delete_account) { _, _ ->
            // Delete account
            // TODO: Implement delete account process
        }
        .setNegativeButton(R.string.cancel, null)
        .show()

    // Set red color to delete button
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.red))
}