package com.mrgames13.jimdo.feinstaubapp.Services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.NotificationUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SyncJobService extends JobService {

    //Konstanten

    //Variablen als Objekte
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");

    //Utils-Pakete
    private Resources res;
    private StorageUtils su;
    private ServerMessagingUtils smu;
    private NotificationUtils nu;

    //Variablen
    private int limit_p1;
    private int limit_p2;
    private int limit_temp;
    private int limit_humidity;
    private int limit_pressure;

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
        //Resourcen initialisieren
        res = getResources();

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(this, su);

        //NotificationUtils initialisieren
        nu = new NotificationUtils(this);

        //Prüfen, ob Intenet verfügbar ist
        if(smu.isInternetAvailable()) {
            //MaxLimit aus den SharedPreferences auslesen
            limit_p1 = Integer.parseInt(su.getString("limit_p1", String.valueOf(Constants.DEFAULT_P1_LIMIT)).isEmpty() ? "0" : su.getString("limit_p1", String.valueOf(Constants.DEFAULT_P1_LIMIT)));
            limit_p2 = Integer.parseInt(su.getString("limit_p2", String.valueOf(Constants.DEFAULT_P2_LIMIT)).isEmpty() ? "0" : su.getString("limit_p2", String.valueOf(Constants.DEFAULT_P2_LIMIT)));
            limit_temp = Integer.parseInt(su.getString("limit_temp", String.valueOf(Constants.DEFAULT_TEMP_LIMIT)).isEmpty() ? "0" : su.getString("limit_temp", String.valueOf(Constants.DEFAULT_TEMP_LIMIT)));
            limit_humidity = Integer.parseInt(su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT)).isEmpty() ? "0" : su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT)));
            limit_pressure = Integer.parseInt(su.getString("limit_pressure", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT)).isEmpty() ? "0" : su.getString("limit_pressure", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT)));

            if (fromForeground || (limit_p1 != 0 || limit_p2 != 0 || limit_temp != 0 || limit_humidity != 0 || limit_pressure != 0)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Date String von Heute ermitteln
                            Calendar calendar = Calendar.getInstance();
                            String date_string = sdf_date.format(calendar.getTime());

                            //Date String von Gestern ermitteln
                            Calendar c = Calendar.getInstance();
                            c.setTime(sdf_date.parse(date_string));
                            c.add(Calendar.DATE, -1);
                            String date_yesterday = sdf_date.format(c.getTime());

                            ArrayList<Sensor> sensors = new ArrayList<>();
                            sensors.addAll(su.getAllFavourites());
                            sensors.addAll(su.getAllOwnSensors());
                            for (Sensor s : sensors) {
                                //Dateien Herunterladen
                                smu.manageDownloads(s, date_string, date_yesterday);
                                //Inhalt der lokalen Dateien auslesen
                                String csv_string_day = su.getCSVFromFile(date_string, s.getId());
                                String csv_string_day_before = su.getCSVFromFile(date_yesterday, s.getId());
                                //CSV-Strings zu Objekten machen
                                ArrayList<DataRecord> records = su.getDataRecordsFromCSV(csv_string_day_before);
                                records.addAll(su.getDataRecordsFromCSV(csv_string_day));
                                if (records.size() > 0) {
                                    //Datensätze zuschneiden
                                    records = su.trimDataRecords(records, date_string);
                                    records = trimDataRecordsToSyncTime(records);
                                    //Auswerten
                                    for (DataRecord r : records) {
                                        if (!fromForeground && !su.getBoolean(date_string + "_p1_exceeded") && limit_p1 > 0 && r.getP1() > limit_p1 && r.getP1() > su.getDouble(date_string + "_p1_max")) {
                                            Log.i("FA", "P1 limit exceeded");
                                            //P1 Notification
                                            nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_p1), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                            su.putDouble(date_string + "_p1_max", r.getP1());
                                            su.putBoolean(date_string + "_p1_exceeded", true);
                                            break;
                                        } else if(limit_p1 > 0 && r.getP1() < limit_p1) {
                                            su.putBoolean(date_string + "_p1_exceeded", false);
                                        }
                                        if (!fromForeground && !su.getBoolean(date_string + "_p2_exceeded") && limit_p2 > 0 && r.getP2() > limit_p2 && r.getP2() > su.getDouble(date_string + "_p2_max")) {
                                            Log.i("FA", "P2 limit exceeded");
                                            //P2 Notification
                                            nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_p2), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                            su.putDouble(date_string + "_p2_max", r.getP2());
                                            su.putBoolean(date_string + "_p2_exceeded", true);
                                            break;
                                        } else if(limit_p1 > 0 && r.getP1() < limit_p1) {
                                            su.putBoolean(date_string + "_p2_exceeded", false);
                                        }
                                        if (!fromForeground && !su.getBoolean(date_string + "_temp_exceeded") && limit_temp > 0 && r.getTemp() > limit_temp && r.getTemp() > su.getDouble(date_string + "_temp_max")) {
                                            Log.i("FA", "Temp limit exceeded");
                                            //Temperatur Notification
                                            nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_temp), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                            su.putDouble(date_string + "_temp_max", r.getTemp());
                                            su.putBoolean(date_string + "_temp_exceeded", true);
                                            break;
                                        } else if(limit_p1 > 0 && r.getP1() < limit_p1) {
                                            su.putBoolean(date_string + "_temp_exceeded", false);
                                        }
                                        if (!fromForeground && !su.getBoolean(date_string + "_humidity_exceeded") && limit_humidity > 0 && r.getHumidity() > limit_humidity && r.getHumidity() > su.getDouble(date_string + "_humidity_max")) {
                                            Log.i("FA", "Humidity limit exceeded");
                                            //Luftfeuchtigkeit Notification
                                            nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_humidity), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                            su.putDouble(date_string + "_humidity_max", r.getHumidity());
                                            su.putBoolean(date_string + "_humidity_exceeded", true);
                                            break;
                                        } else if(limit_p1 > 0 && r.getP1() < limit_p1) {
                                            su.putBoolean(date_string + "_humidity_exceeded", false);
                                        }
                                        if (!fromForeground && !su.getBoolean(date_string + "_pressure_exceeded") && limit_pressure > 0 && r.getPressure() > limit_pressure && r.getHumidity() > su.getDouble(date_string + "_pressure_max")) {
                                            Log.i("FA", "Pressure limit exceeded");
                                            //Luftdruck Notification
                                            nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_pressure), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                            su.putDouble(date_string + "_pressure_max", r.getTemp());
                                            su.putBoolean(date_string + "_pressure_exceeded", true);
                                            break;
                                        } else if(limit_p1 > 0 && r.getP1() < limit_p1) {
                                            su.putBoolean(date_string + "_pressure_exceeded", false);
                                        }
                                    }
                                }
                            }
                            if(params != null) jobFinished(params, false);
                        } catch (Exception e) {
                            if(params != null) jobFinished(params, true);
                        }
                    }
                }).start();
            }
        }
    }

    private ArrayList<DataRecord> trimDataRecordsToSyncTime(ArrayList<DataRecord> all_records) {
        //Letzte Ausführung laden
        long last_record = su.getLong("LastRecord", System.currentTimeMillis());

        ArrayList<DataRecord> records = new ArrayList<>();
        for(DataRecord r : all_records) {
            if(r.getDateTime().getTime() > last_record) records.add(r);
        }

        //Ausführungszeit speichern
        if(all_records.size() > 0) su.putLong("LastRecord", all_records.get(all_records.size() -1).getDateTime().getTime());

        return records;
    }
}
