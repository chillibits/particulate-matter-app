/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.mrgames13.jimdo.feintaubapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Context.showRankingDialog() {
    val rankingView = LayoutInflater.from(this).inflate(R.layout.dialog_ranking, null, false)

    val d = AlertDialog.Builder(this)
        .setTitle(R.string.sensor_highscore)
        .setView(rankingView)
        .setPositiveButton(R.string.ok, null)
        .show()

    CoroutineScope(Dispatchers.IO).launch {

    }
}