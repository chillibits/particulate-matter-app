/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.dialog

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feinstaubapp.R

fun Context.showRestartAppDialog() {
    AlertDialog.Builder(this)
        .setTitle(R.string.app_restart_t)
        .setMessage(R.string.app_restart_m)
        .setCancelable(false)
        .setPositiveButton(R.string.ok) { _, _ ->
            packageManager.getLaunchIntentForPackage(packageName).run {
                this!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(this)
            }
        }
        .show()
}