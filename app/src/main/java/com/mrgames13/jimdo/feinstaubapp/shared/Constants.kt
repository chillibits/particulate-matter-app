/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.shared

object Constants {
    // Logging tag
    const val TAG = "FA"

    // Thresholds
    const val THRESHOLD_WHO_PM10 = 20.0
    const val THRESHOLD_WHO_PM2_5 = 10.0
    const val THRESHOLD_EU_PM10 = 40.0
    const val THRESHOLD_EU_PM2_5 = 25.0

    // Default values
    const val DEFAULT_SYNC_CYCLE = 30 // 30 seconds
    const val MIN_SYNC_CYCLE = 15 // 15 seconds
    const val DEFAULT_SYNC_CYCLE_BACKGROUND = 15 // 15 minutes
    const val MIN_SYNC_CYCLE_BACKGROUND = 10 // 10 minutes
    const val DEFAULT_FIT_ARRAY_LIST_ENABLED = true
    const val DEFAULT_FIT_ARRAY_LIST_CONSTANT = 200 // Optimize as of 200 data records
    const val DEFAULT_P1_LIMIT = THRESHOLD_EU_PM10.toInt()
    const val DEFAULT_P2_LIMIT = THRESHOLD_EU_PM2_5.toInt()
    const val DEFAULT_TEMP_LIMIT = 0
    const val DEFAULT_HUMIDITY_LIMIT = 0
    const val DEFAULT_PRESSURE_LIMIT = 0
    const val DEFAULT_MISSING_MEASUREMENT_NUMBER0 = 5
    const val DEFAULT_MAP_ZOOM = 11f

    // Constants, relevant for backend requests
    const val GLOBAL_CHIP_ID = 0

    // Notification channels
    const val CHANNEL_SYSTEM = "System"
    const val CHANNEL_LIMIT = "Limit"
    const val CHANNEL_MISSING_MEASUREMENTS = "Missing Measurements"

    // SharedPreferences
    const val SHARED_PREFS_NAME = "com.mrgames13.jimdo.feinstaubapp_preferences"
    const val PREF_ENABLE_DAILY_AVERAGE = "enableDailyAverage"
    const val PREF_ENABLE_DAILY_MEDIAN = "enableDailyMedian"
    const val PREF_MANAGE_AUTO_CORRECTION = "manageAutoCorrection"
    const val PREF_SHOW_INACTIVE_SENSORS = "showInactiveSensors"
    const val PREF_SYNC_CYCLE = "syncCycle"
    const val PREF_SYNC_CYCLE_BACKGROUND = "syncCycleBackground"
    const val PREF_REDUCE_DATA_CONSUMPTION = "reduceDataConsumption"
    const val PREF_NOTIFICATION_THRESHOLD = "notificationThreshold"
    const val PREF_NOTIFICATION_AVERAGES = "notificationAverages"
    const val PREF_NOTIFICATION_BREAKDOWN = "notificationBreakdown"
    const val PREF_ENABLE_MARKER_CLUSTERING = "enableMarkerClustering"
    const val PREF_INCREASE_DIAGRAM_PERFORMANCE = "increaseDiagramPerformance"
    const val PREF_CLEAR_SENSOR_DATA = "clearSensorData"
    const val PREF_OPEN_SOURCE_LICENSES = "openSourceLicenses"
    const val PREF_OPEN_SOURCE = "openSource"
    const val PREF_APP_VERSION = "appVersion"
    const val PREF_DEVELOPERS = "developers"
    const val PREF_MORE_APPS = "moreApps"
    const val RECENT_MAP_TYPE = "recentMapType"
    const val RECENT_TRAFFIC = "recentTraffic"
    const val RECENT_CAMERA_LAT = "recentCameraLat"
    const val RECENT_CAMERA_LNG = "recentCameraLng"
    const val RECENT_CAMERA_ZOOM = "recentCameraZoom"

    // JobScheduler ids
    const val JOB_SYNC_ID = 1001

    // Request codes
    const val REQ_ALARM_MANAGER_BACKGROUND_SYNC = 1002
    const val REQ_SCAN_WEB = 1003
    const val REQ_ADD_SENSOR = 1004
    const val REQ_PLACE_PICKER = 1005
    const val REQ_COLOR_CONVERTER = 1006

    // Permission request codes
    const val PERMISSION_LOCATION = 10001

    // Intent extras
    const val EXTRA_COLOR_CONVERTER = "ChooseColor" // Link to Color Converter
    const val EXTRA_ADD_SENSOR_MODE = "Mode"
    const val EXTRA_ADD_SENSOR_NAME = "Name"
    const val EXTRA_ADD_SENSOR_ID = "Id"
    const val EXTRA_SENSOR_DATA_NAME = "Name"
    const val EXTRA_SENSOR_DATA_ID = "Id"
    const val EXTRA_SENSOR_DATA_COLOR = "Color"

    // HomeScreen widget
    const val WIDGET_LARGE_EXTRA_SENSOR_ID = "WidgetLargeSensorID"
    const val WIDGET_SMALL_EXTRA_SENSOR_ID = "WidgetSmallSensorID"
    const val WIDGET_EXTRA_LARGE_WIDGET_ID = "LargeWidgetID"
    const val WIDGET_EXTRA_SMALL_WIDGET_ID = "SmallWidgetID"

    // Local db constants
    const val DB_NAME = "pmapp-main.db"

    // NetworkScan constants
    const val JOB_COUNT = 5

    // SharedPreferences keys
    const val PREFS_CHOOSE_COLOR_REMEMBER = "ChooseColorRemember"

    // User constants
    const val ROLE_USER = 1
    const val ROLE_OPERATOR = 2
    const val ROLE_ADMIN = 3
    const val STATUS_ACTIVE = 1
    const val STATUS_EMAIL_CONFIRMATION_PENDING = 2
    const val STATUS_SUSPENDED = 3
    const val STATUS_LOCKED = 4

    // API constants
    const val API_AUTH_USERNAME = "pmapp"

    // Creation modes
    const val CREATION_MODE_COMPLETE = 1
}