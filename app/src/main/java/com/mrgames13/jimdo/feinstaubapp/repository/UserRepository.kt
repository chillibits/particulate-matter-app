/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.repository

import android.app.Application
import com.mrgames13.jimdo.feinstaubapp.model.dbo.UserDbo
import com.mrgames13.jimdo.feinstaubapp.shared.getDatabase

class UserRepository(application: Application) {

    // Variables as objects
    private val context = application
    private val userDao = context.getDatabase().userDao()
    val users = userDao.getAll()

    suspend fun insert(user: UserDbo) {
        userDao.insert(listOf(user))
    }
}