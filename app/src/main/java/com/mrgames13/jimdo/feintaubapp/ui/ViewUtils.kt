/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.ui

import android.app.Activity
import android.content.Intent
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mrgames13.jimdo.feintaubapp.R

fun openActivityWithRevealAnimation(activity: Activity, fab: FloatingActionButton, revealSheet: View, intent: Intent, requestCode: Int) {
    val fabLoc = IntArray(2)
    fab.getLocationOnScreen(fabLoc)
    val screenMetrics = DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(screenMetrics)
    ViewAnimationUtils.createCircularReveal(revealSheet, fabLoc[0] + fab.width / 2, fabLoc[1] + fab.width / 2, fab.width/ 2f, screenMetrics.heightPixels.toFloat()).run {
        duration = 400
        interpolator = AccelerateDecelerateInterpolator()
        doOnEnd {
            ActivityCompat.startActivityForResult(activity, intent, requestCode, null)
            activity.overridePendingTransition(R.anim.activity_transition_slide_up, R.anim.activity_transition_fade_out)
        }
        start()
    }
    revealSheet.visibility = View.VISIBLE
}

fun closeActivityWithRevealAnimation(activity: Activity, fab: FloatingActionButton, revealSheet: View) {
    val fabLoc = IntArray(2)
    fab.getLocationOnScreen(fabLoc)
    val screenMetrics = DisplayMetrics()
    activity.windowManager.defaultDisplay.getMetrics(screenMetrics)
    ViewAnimationUtils.createCircularReveal(revealSheet, fabLoc[0] + fab.width / 2, fabLoc[1] + fab.width / 2, screenMetrics.heightPixels.toFloat(), fab.width/ 2f).run {
        duration = 400
        interpolator = AccelerateDecelerateInterpolator()
        doOnEnd {
            revealSheet.visibility = View.GONE
        }
        start()
    }
}