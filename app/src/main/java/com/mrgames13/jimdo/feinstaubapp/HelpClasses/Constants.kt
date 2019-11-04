/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses

object Constants {

    // Default values
    val DEFAULT_SYNC_CYCLE = 30 // 30 secs
    val MIN_SYNC_CYCLE = 15 // 15 secs
    val DEFAULT_SYNC_CYCLE_BACKGROUND = 15 // 15 mins
    val MIN_SYNC_CYCLE_BACKGROUND = 10 // 10 mins
    val DEFAULT_FIT_ARRAY_LIST_ENABLED = true
    val DEFAULT_FIT_ARRAY_LIST_CONSTANT = 200 // Optimize as of 200 data records
    val DEFAULT_P1_LIMIT = 40
    val DEFAULT_P2_LIMIT = 25
    val DEFAULT_TEMP_LIMIT = 0
    val DEFAULT_HUMIDITY_LIMIT = 0
    val DEFAULT_PRESSURE_LIMIT = 0
    val DEFAULT_MISSING_MEASUREMENT_NUMBER = 5

    // Thresholds
    val THRESHOLD_WHO_PM10 = 20.0
    val THRESHOLD_WHO_PM2_5 = 10.0
    val THRESHOLD_EU_PM10 = 40.0
    val THRESHOLD_EU_PM2_5 = 25.0

    // Notification channels
    val CHANNEL_SYSTEM = "System"
    val CHANNEL_LIMIT = "Limit"
    val CHANNEL_MISSING_MEASUREMENTS = "Missing Measurements"

    // Global request codes
    val REQ_ALARM_MANAGER_BACKGROUND_SYNC = 10001

    // JobScheduler ids
    val JOB_SYNC_ID = 10001

    // Homescreen widget
    val WIDGET_EXTRA_SENSOR_ID = "SensorID"
    val WIDGET_EXTRA_WIDGET_ID = "WidgetID"
}