/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses

object Constants {

    // Default values
    const val DEFAULT_SYNC_CYCLE = 30 // 30 secs
    const val MIN_SYNC_CYCLE = 15 // 15 secs
    const val DEFAULT_SYNC_CYCLE_BACKGROUND = 15 // 15 mins
    const val MIN_SYNC_CYCLE_BACKGROUND = 10 // 10 mins
    const val DEFAULT_FIT_ARRAY_LIST_ENABLED = true
    const val DEFAULT_FIT_ARRAY_LIST_CONSTANT = 200 // Optimize as of 200 data records
    const val DEFAULT_P1_LIMIT = 40
    const val DEFAULT_P2_LIMIT = 25
    const val DEFAULT_TEMP_LIMIT = 0
    const val DEFAULT_HUMIDITY_LIMIT = 0
    const val DEFAULT_PRESSURE_LIMIT = 0
    const val DEFAULT_MISSING_MEASUREMENT_NUMBER = 5

    // Thresholds
    const val THRESHOLD_WHO_PM10 = 20.0
    const val THRESHOLD_WHO_PM2_5 = 10.0
    const val THRESHOLD_EU_PM10 = 40.0
    const val THRESHOLD_EU_PM2_5 = 25.0

    // Notification channels
    const val CHANNEL_SYSTEM = "System"
    const val CHANNEL_LIMIT = "Limit"
    const val CHANNEL_MISSING_MEASUREMENTS = "Missing Measurements"

    // Global request codes
    const val REQ_ALARM_MANAGER_BACKGROUND_SYNC = 10001

    // JobScheduler ids
    const val JOB_SYNC_ID = 10001

    // Homescreen widget
    const val WIDGET_EXTRA_SENSOR_ID = "SensorID"
    const val WIDGET_EXTRA_WIDGET_ID = "WidgetID"
}