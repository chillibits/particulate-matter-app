package com.mrgames13.jimdo.feinstaubapp.Services;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.annotation.Nullable;
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

public class SyncService extends Service {

    //Konstanten

    //Variablen als Objekte
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");

    //Utils-Pakete
    private Resources res;
    private StorageUtils su;
    private ServerMessagingUtils smu;
    private NotificationUtils nu;

    //Variablen
    private int max_limit_sdsp1;
    private int max_limit_sdsp2;
    private int max_limit_temp;
    private int max_limit_humidity;

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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    //MaxLimit aus den SharedPreferences auslesen
                    max_limit_sdsp1 = Integer.parseInt(su.getString("limit_sdsp1", String.valueOf(Constants.DEFAULT_SDSP1_LIMIT)));
                    max_limit_sdsp2 = Integer.parseInt(su.getString("limit_sdsp2", String.valueOf(Constants.DEFAULT_SDSP2_LIMIT)));
                    max_limit_temp = Integer.parseInt(su.getString("limit_temp", String.valueOf(Constants.DEFAULT_TEMP_LIMIT)));
                    max_limit_humidity = Integer.parseInt(su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT)));

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

                    for(Sensor s : su.getAllSensors()) {
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
                                if(max_limit_sdsp1 > 0 && r.getSdsp1() > max_limit_sdsp1) {
                                    Log.i("FA", "SDSP1 limit exceeded");
                                    //Sdsp1 Notification
                                    nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_sdsp1), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                }
                                if(max_limit_sdsp2 > 0 && r.getSdsp2() > max_limit_sdsp2) {
                                    Log.i("FA", "SDSP2 limit exceeded");
                                    //Sdsp2 Notification
                                    nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_sdsp2), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                }
                                if(max_limit_temp > 0 && r.getTemp() > max_limit_temp) {
                                    Log.i("FA", "Temp limit exceeded");
                                    //Temperatur Notification
                                    nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_temp), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                }
                                if(max_limit_humidity > 0 && r.getHumidity() > max_limit_humidity) {
                                    Log.i("FA", "Humidity limit exceeded");
                                    //Luftfeuchtigkeit Notification
                                    nu.displayLimitExceededNotification(res.getString(R.string.limit_exceeded), s.getName() + " - " + res.getString(R.string.limit_exceeded_humidity), Integer.parseInt(s.getId()), r.getDateTime().getTime());
                                }
                            }
                        }
                    }
                }
            }).start();
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