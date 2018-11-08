package com.mrgames13.jimdo.feinstaubapp.HelpClasses;

public class Constants {

    //Standardwerte
    public static final int DEFAULT_SYNC_CYCLE = 30; // 30 Sekunden
    public static final int DEFAULT_SYNC_CYCLE_BACKGROUND = 15; // 15 Minuten
    public static final boolean DEFAULT_REDUCE_DATA_CONSUMPTION = true;
    public static final boolean DEFAULT_FIT_ARRAY_LIST_ENABLED = true;
    public static final int DEFAULT_FIT_ARRAY_LIST_CONSTANT = 200; // bei über 200 Datensätzen wird optimiert
    public static final int DEFAULT_P1_LIMIT = 40;
    public static final int DEFAULT_P2_LIMIT = 25;
    public static final int DEFAULT_TEMP_LIMIT = 0;
    public static final int DEFAULT_HUMIDITY_LIMIT = 0;
    public static final int DEFAULT_PRESSURE_LIMIT = 0;

    //NotificationChannels
    public static final String CHANNEL_SYSTEM = "System";
    public static final String CHANNEL_LIMIT = "Limit";

    //Globale Requestcodes
    public static final int REQ_ALARM_MANAGER_BACKGROUND_SYNC = 10001;

    //JobScheduler IDs
    public static final int JOB_SYNC_ID = 10001;

}