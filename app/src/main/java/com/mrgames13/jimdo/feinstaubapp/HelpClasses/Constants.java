/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

public class Constants {

    // Default values
    public static final int DEFAULT_SYNC_CYCLE = 30; // 30 secs
    public static final int MIN_SYNC_CYCLE = 15; // 15 secs
    public static final int DEFAULT_SYNC_CYCLE_BACKGROUND = 15; // 15 mins
    public static final int MIN_SYNC_CYCLE_BACKGROUND = 10; // 10 mins
    public static final boolean DEFAULT_FIT_ARRAY_LIST_ENABLED = true;
    public static final int DEFAULT_FIT_ARRAY_LIST_CONSTANT = 200; // Optimize as of 200 data records
    public static final int DEFAULT_P1_LIMIT = 40;
    public static final int DEFAULT_P2_LIMIT = 25;
    public static final int DEFAULT_TEMP_LIMIT = 0;
    public static final int DEFAULT_HUMIDITY_LIMIT = 0;
    public static final int DEFAULT_PRESSURE_LIMIT = 0;
    public static final int DEFAULT_MISSING_MEASUREMENT_NUMBER = 5;

    // Thresholds
    public static final double THRESHOLD_WHO_PM10 = 20;
    public static final double THRESHOLD_WHO_PM2_5 = 10;
    public static final double THRESHOLD_EU_PM10 = 40;
    public static final double THRESHOLD_EU_PM2_5 = 25;

    // Notification channels
    public static final String CHANNEL_SYSTEM = "System";
    public static final String CHANNEL_LIMIT = "Limit";
    public static final String CHANNEL_MISSING_MEASUREMENTS = "Missing Measurements";

    // Global request codes
    public static final int REQ_ALARM_MANAGER_BACKGROUND_SYNC = 10001;

    // JobScheduler ids
    public static final int JOB_SYNC_ID = 10001;

    // Homescreen widget
    public static final String WIDGET_EXTRA_SENSOR_ID = "SensorID";
    public static final String WIDGET_EXTRA_WIDGET_ID = "WidgetID";
}