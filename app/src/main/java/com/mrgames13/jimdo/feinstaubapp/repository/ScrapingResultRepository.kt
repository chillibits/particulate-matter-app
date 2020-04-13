/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.repository

import android.app.Application
import com.mrgames13.jimdo.feinstaubapp.shared.getDatabase

class ScrapingResultRepository(application: Application) {

    // Variables as objects
    private val context = application
    private val scrapingResultDao = context.getDatabase().scrapingResultDao()
    val scrapingResults = scrapingResultDao.getAll()


}