/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feintaubapp.shared

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat
import com.mrgames13.jimdo.feintaubapp.R

fun setFullscreenMode(window: Window, fullscreen: Boolean) {
    if (fullscreen) {
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
        window.decorView.systemUiVisibility = flags
    } else {
        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.decorView.systemUiVisibility = flags
    }
    setTranslucentStatusBar(window)
}

fun getStatusBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if(resourceId > 0 ) context.resources.getDimensionPixelSize(resourceId) else 0
}

fun getNavigationBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
}

// ---------------------------------------Additional-Functions--------------------------------------

private fun setTranslucentStatusBar(window: Window?) {
    if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) setTranslucentStatusBarLollipop(window)
}

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private fun setTranslucentStatusBarLollipop(window: Window) {
    window.statusBarColor = ContextCompat.getColor(window.context, R.color.blackLight)
}