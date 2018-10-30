package com.mrgames13.jimdo.feinstaubapp.Services;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
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

import androidx.annotation.Nullable;

public class SyncService extends Service {

    //Konstanten
    public static int MODE_FOREGROUND = 10001;
    public static int MODE_BACKGROUND = 10002;

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("FA", "SyncService started ...");

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

            if((intent.hasExtra("Mode") && intent.getIntExtra("Mode", MODE_BACKGROUND) == MODE_FOREGROUND) || (limit_p1 != 0 || limit_p2 != 0 || limit_temp != 0 || limit_humidity != 0 || limit_pressure != 0)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //Date String von Heute ermitteln
                        Calendar calendar = Calendar.getInstance();
                        String date_string = sdf_date.format(calendar.getTime());

                        //Date String von Gestern ermitteln
                        String date_yesterday = date_string;
                        try{
                            Calendar c = Calendar.getInstance();
                            c.setTime(sdf_date.parse(date_yesterday));
                            c.add(Calendar.DATE, -1);
                            date_yesterday = sdf_date.format(c.getTime());
                        } catch (Exception e) {}

                        ArrayList<Sensor> array = new ArrayList<>();
                        array.addAll(su.getAllFavourites());
                        array.addAll(su.getAllOwnSensors());
                        for(Sensor s : array) {
                            //Dateien Herunterladen
                            smu.manageDownloads(s, date_string, date_yesterday);
                            //Inhalt der lokalen Dateien auslesen
                            String csv_string_day = su.getCSVFromFile(date_string, s.getId());
                            String csv_string_day_before = su.getCSVFromFile(date_yesterday, s.getId());
                            //CSV-Strings zu Objekten machen
                            ArrayList<DataRecord> records = su.getDataRecordsFromCSV(csv_string_day_before);
                            records.addAll(su.getDataRecordsFromCSV(csv_string_day));
                            if(records.size() > 0) {
                                //Datensätze zuschneiden
                                records = su.trimDataRecords(records, date_string);
                                records = trimDataRecordsToSyncTime(records);
                                //Auswerten
                                for(DataRecord r : records) {
                                    if(limit_p1 > 0 && r.getP1() > limit_p1) {
                                        Log.i("FA", "P1 limit exceeded");
                                        //P1 Notification
                                        nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_p1), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                    }
                                    if(limit_p2 > 0 && r.getP2() > limit_p2) {
                                        Log.i("FA", "P2 limit exceeded");
                                        //P2 Notification
                                        nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_p2), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                    }
                                    if(limit_temp > 0 && r.getTemp() > limit_temp) {
                                        Log.i("FA", "Temp limit exceeded");
                                        //Temperatur Notification
                                        nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_temp), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                    }
                                    if(limit_humidity > 0 && r.getHumidity() > limit_humidity) {
                                        Log.i("FA", "Humidity limit exceeded");
                                        //Luftfeuchtigkeit Notification
                                        nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_humidity), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                    }
                                    if(limit_pressure > 0 && r.getPressure() > limit_pressure) {
                                        Log.i("FA", "Pressure limit exceeded");
                                        //Luftdruck Notification
                                        nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_pressure), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                    }
                                }
                            }
                        }
                    }
                }).start();
            }
        }

        stopSelf();

        return super.onStartCommand(intent, flags, startId);
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