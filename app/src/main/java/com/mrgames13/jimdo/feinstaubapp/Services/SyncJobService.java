/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.Services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.NotificationUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;
import com.mrgames13.jimdo.feinstaubapp.WidgetComponents.WidgetProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SyncJobService extends JobService {

    // Variables as objects
    private Calendar calendar;
    private ArrayList<DataRecord> records;

    // Utils packages
    private Resources res;
    private StorageUtils su;
    private ServerMessagingUtils smu;
    private NotificationUtils nu;

    // Variables
    private int limit_p1;
    private int limit_p2;
    private int limit_temp;
    private int limit_humidity;
    private int limit_pressure;
    private long selected_day_timestamp;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("FA", "Job started from foreground");
        doWork(true, null);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("FA", "Job started from background");
        doWork(false, params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.w("FA", "Job stopped before completion");
        return false;
    }

    private void doWork(final boolean fromForeground, final JobParameters params) {
        // Initialize StorageUtils
        su = new StorageUtils(this);

        // Initialize ServerMessagingUtils
        smu = new ServerMessagingUtils(this, su);

        // Initialize NotificationUtils
        nu = new NotificationUtils(this);

        // Initialize calendar
        if(selected_day_timestamp == 0 || calendar == null) {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            selected_day_timestamp = calendar.getTime().getTime();
        }

        // Check if internet is available
        if(smu.isInternetAvailable()) {
            // Get max limit from SharedPreferences
            try{ // This try-catch-block is temporary placed because of an error occurrence of a NumberFormatException
                limit_p1 = Integer.parseInt(su.getString("limit_p1", String.valueOf(Constants.INSTANCE.getDEFAULT_P1_LIMIT())));
                limit_p2 = Integer.parseInt(su.getString("limit_p2", String.valueOf(Constants.INSTANCE.getDEFAULT_P2_LIMIT())));
                limit_temp = Integer.parseInt(su.getString("limit_temp", String.valueOf(Constants.INSTANCE.getDEFAULT_TEMP_LIMIT())));
                limit_humidity = Integer.parseInt(su.getString("limit_humidity", String.valueOf(Constants.INSTANCE.getDEFAULT_HUMIDITY_LIMIT())));
                limit_pressure = Integer.parseInt(su.getString("limit_pressure", String.valueOf(Constants.INSTANCE.getDEFAULT_PRESSURE_LIMIT())));
            } catch (NumberFormatException e) {
                su.putString("limit_p1", String.valueOf(limit_p1 = Constants.INSTANCE.getDEFAULT_P1_LIMIT()));
                su.putString("limit_p2", String.valueOf(limit_p2 = Constants.INSTANCE.getDEFAULT_P2_LIMIT()));
                su.putString("limit_temp", String.valueOf(limit_temp = Constants.INSTANCE.getDEFAULT_TEMP_LIMIT()));
                su.putString("limit_humidity", String.valueOf(limit_humidity = Constants.INSTANCE.getDEFAULT_HUMIDITY_LIMIT()));
                su.putString("limit_pressure", String.valueOf(limit_pressure = Constants.INSTANCE.getDEFAULT_PRESSURE_LIMIT()));
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Get timestamps for 'from' and 'to'
                        long from = selected_day_timestamp;
                        long to = selected_day_timestamp + TimeUnit.DAYS.toMillis(1);

                        ArrayList<Sensor> sensors = new ArrayList<>();
                        sensors.addAll(su.getAllFavourites());
                        sensors.addAll(su.getAllOwnSensors());
                        for (Sensor s : sensors) {
                            // Load existing records from the local database
                            records = su.loadRecords(s.getChipID(), from, to);
                            // Sort by time
                            Collections.sort(records);
                            // Load records from server
                            ArrayList<DataRecord> records_external = smu.manageDownloadsRecords(s.getChipID(), records.size() > 0 ? records.get(records.size() -1).getDateTime().getTime() +1000 : from, to);
                            if(records_external != null) records.addAll(records_external);
                            // Sort by time
                            Collections.sort(records);

                            if (records.size() > 0) {
                                // Detect a breakdown
                                if(su.getBoolean("notification_breakdown", true) && su.isSensorExisting(s.getChipID()) && Tools.isMeasurementBreakdown(su, records)) {
                                    if (records_external != null && !su.getBoolean("BD_" + s.getChipID())) {
                                        nu.displayMissingMeasurementsNotification(s.getChipID(), s.getName());
                                        su.putBoolean("BD_" + s.getChipID(), true);
                                    }
                                } else {
                                    nu.cancelNotification(Integer.parseInt(s.getChipID()) * 10);
                                    su.removeKey("BD_" + s.getChipID());
                                }
                                // Calculate average values
                                double average_p1 = getP1Average(records);
                                double average_p2 = getP2Average(records);
                                double average_temp = getTempAverage(records);
                                double average_humidity = getHumidityAverage(records);
                                double average_pressure = getPressureAverage(records);
                                records = trimDataRecordsToSyncTime(s.getChipID(), records);
                                // Evaluate
                                for (DataRecord r : records) {
                                    if (!fromForeground && !su.getBoolean(selected_day_timestamp + "_p1_exceeded") && limit_p1 > 0 && (su.getBoolean("notification_averages", true) ? average_p1 > limit_p1 : (r.getP1() > limit_p1)) && r.getP1() > su.getDouble(selected_day_timestamp + "_p1_max")) {
                                        Log.i("FA", "P1 limit exceeded");
                                        // P1 notification
                                        nu.displayLimitExceededNotification(s.getName() + " - " + res.getString(R.string.limit_exceeded_p1), s.getChipID(), r.getDateTime().getTime());
                                        su.putDouble(selected_day_timestamp + "_p1_max", r.getP1());
                                        su.putBoolean(selected_day_timestamp + "_p1_exceeded", true);
                                        break;
                                    } else if (limit_p1 > 0 && r.getP1() < limit_p1) {
                                        su.putBoolean(selected_day_timestamp + "_p1_exceeded", false);
                                    }
                                    if (!fromForeground && !su.getBoolean(selected_day_timestamp + "_p2_exceeded") && limit_p2 > 0 && (su.getBoolean("notification_averages", true) ? average_p2 > limit_p2 : (r.getP2() > limit_p2)) && r.getP2() > su.getDouble(selected_day_timestamp + "_p2_max")) {
                                        Log.i("FA", "P2 limit exceeded");
                                        // P2 notification
                                        nu.displayLimitExceededNotification(s.getName() + " - " + res.getString(R.string.limit_exceeded_p2), s.getChipID(), r.getDateTime().getTime());
                                        su.putDouble(selected_day_timestamp + "_p2_max", r.getP2());
                                        su.putBoolean(selected_day_timestamp + "_p2_exceeded", true);
                                        break;
                                    } else if (limit_p1 > 0 && r.getP1() < limit_p1) {
                                        su.putBoolean(selected_day_timestamp + "_p2_exceeded", false);
                                    }
                                    if (!fromForeground && !su.getBoolean(selected_day_timestamp + "_temp_exceeded") && limit_temp > 0 && (su.getBoolean("notification_averages", true) ? average_temp > limit_temp : (r.getTemp() > limit_temp)) && r.getTemp() > su.getDouble(selected_day_timestamp + "_temp_max")) {
                                        Log.i("FA", "Temp limit exceeded");
                                        // Temperature notification
                                        nu.displayLimitExceededNotification(s.getName() + " - " + res.getString(R.string.limit_exceeded_temp), s.getChipID(), r.getDateTime().getTime());
                                        su.putDouble(selected_day_timestamp + "_temp_max", r.getTemp());
                                        su.putBoolean(selected_day_timestamp + "_temp_exceeded", true);
                                        break;
                                    } else if (limit_p1 > 0 && r.getP1() < limit_p1) {
                                        su.putBoolean(selected_day_timestamp + "_temp_exceeded", false);
                                    }
                                    if (!fromForeground && !su.getBoolean(selected_day_timestamp + "_humidity_exceeded") && limit_humidity > 0 && (su.getBoolean("notification_averages", true) ? average_humidity > limit_humidity : (r.getHumidity() > limit_humidity)) && r.getHumidity() > su.getDouble(selected_day_timestamp + "_humidity_max")) {
                                        Log.i("FA", "Humidity limit exceeded");
                                        // Humidity notification
                                        nu.displayLimitExceededNotification(s.getName() + " - " + res.getString(R.string.limit_exceeded_humidity), s.getChipID(), r.getDateTime().getTime());
                                        su.putDouble(selected_day_timestamp + "_humidity_max", r.getHumidity());
                                        su.putBoolean(selected_day_timestamp + "_humidity_exceeded", true);
                                        break;
                                    } else if (limit_p1 > 0 && r.getP1() < limit_p1) {
                                        su.putBoolean(selected_day_timestamp + "_humidity_exceeded", false);
                                    }
                                    if (!fromForeground && !su.getBoolean(selected_day_timestamp + "_pressure_exceeded") && limit_pressure > 0 && (su.getBoolean("notification_averages", true) ? average_pressure > limit_pressure : (r.getPressure() > limit_pressure)) && r.getHumidity() > su.getDouble(selected_day_timestamp + "_pressure_max")) {
                                        Log.i("FA", "Pressure limit exceeded");
                                        // Pressure notification
                                        nu.displayLimitExceededNotification(s.getName() + " - " + res.getString(R.string.limit_exceeded_pressure), s.getChipID(), r.getDateTime().getTime());
                                        su.putDouble(selected_day_timestamp + "_pressure_max", r.getTemp());
                                        su.putBoolean(selected_day_timestamp + "_pressure_exceeded", true);
                                        break;
                                    } else if (limit_p1 > 0 && r.getP1() < limit_p1) {
                                        su.putBoolean(selected_day_timestamp + "_pressure_exceeded", false);
                                    }
                                }

                                // Refresh homescreen widget
                                Intent update_intent = new Intent(getApplicationContext(), WidgetProvider.class);
                                update_intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                                update_intent.putExtra(Constants.INSTANCE.getWIDGET_EXTRA_SENSOR_ID(), s.getChipID());
                                sendBroadcast(update_intent);
                            }
                        }

                        if(params != null) jobFinished(params, false);
                    } catch (Exception e) {
                        if(params != null) jobFinished(params, true);
                    }
                }
            }).start();
        } else {
            if(params != null) jobFinished(params, false);
        }
    }

    private double getP1Average(ArrayList<DataRecord> records) {
        double average = 0;
        for(DataRecord r : records) average+=r.getP1();
        return average / records.size();
    }

    private double getP2Average(ArrayList<DataRecord> records) {
        double average = 0;
        for(DataRecord r : records) average+=r.getP2();
        return average / records.size();
    }

    private double getTempAverage(ArrayList<DataRecord> records) {
        double average = 0;
        for(DataRecord r : records) average+=r.getTemp();
        return average / records.size();
    }

    private double getHumidityAverage(ArrayList<DataRecord> records) {
        double average = 0;
        for(DataRecord r : records) average+=r.getHumidity();
        return average / records.size();
    }

    private double getPressureAverage(ArrayList<DataRecord> records) {
        double average = 0;
        for(DataRecord r : records) average+=r.getPressure();
        return average / records.size();
    }

    private ArrayList<DataRecord> trimDataRecordsToSyncTime(String chip_id, ArrayList<DataRecord> all_records) {
        // Load last execution time
        long last_record = su.getLong(chip_id + "_LastRecord", System.currentTimeMillis());

        ArrayList<DataRecord> records = new ArrayList<>();
        for(DataRecord r : all_records) {
            if(r.getDateTime().getTime() > last_record) records.add(r);
        }

        // Save execution time
        if(all_records.size() > 0) su.putLong(chip_id + "_LastRecord", all_records.get(all_records.size() -1).getDateTime().getTime());

        return records;
    }
}
