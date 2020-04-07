/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.network

import android.content.Context

fun createUser(context: Context, email: String, password: String): Boolean {
    try {

        return true
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return false
}