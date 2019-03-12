package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.ExternalSensor;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Services.WebRealtimeSyncService;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StorageUtils extends SQLiteOpenHelper {

    //Konstanten
    private static final String DEFAULT_STRING_VALUE = "";
    private static final int DEFAULT_INT_VALUE = 0;
    private static final boolean DEFAULT_BOOLEAN_VALUE = false;
    private static final long DEFAULT_LONG_VALUE = -1;
    private static final double DEFAULT_DOUBLE_VALUE = 0.0d;
    public static final String TABLE_SENSORS = "Sensors";
    public static final String TABLE_EXTERNAL_SENSORS = "ExternalSensors";
    public static final String TABLE_FAVOURITES = "Favourites";

    //Variablen als Objekte
    private Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor e;
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");

    //Variablen

    public StorageUtils(Context context) {
        super(context, "database.db", null, 3);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.context = context;
    }

    //-----------------------------------------Dateisystem------------------------------------------

    public String getCSVFromFile(String date, String sensor_id) {
        try{
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            date = format.format(newDate);

            //Datei ermitteln
            String file_name = sensor_id + "-" + date + ".csv";
            File dir = new File(context.getFilesDir(), "/SensorData");
            File file = new File(dir, file_name);

            if(!file.exists()) return "";

            //Text auslesen
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            return text.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean isCSVFileExisting(String date, String sensor_id) {
        try{
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            date = format.format(newDate);

            String file_name = sensor_id + "-" + date + ".csv";
            File dir = new File(context.getFilesDir(), "/SensorData");
            return new File(dir, file_name).exists();
        } catch (Exception e) {}
        return false;
    }

    public void shareCSVFile(String date, String sensor_id) {
        try{
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            date = format.format(newDate);

            String file_name = sensor_id + "-" + date + ".csv";
            File dir = new File(context.getFilesDir(), "/SensorData");
            File file = new File(dir, file_name);

            Uri uri = FileProvider.getUriForFile(context, "com.mrgames13.jimdo.feinstaubapp", file);

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(URLConnection.guessContentTypeFromName(file.getName()));
            i.putExtra(Intent.EXTRA_STREAM, uri);
            context.startActivity(Intent.createChooser(i, context.getString(R.string.export_data_records)));
        } catch (Exception e) {}
    }

    public boolean clearSensorData() {
        try{
            File dir = new File(context.getFilesDir(), "/SensorData");
            String[] children = dir.list();
            for (int j = 0; j < children.length; j++) new File(dir, children[j]).delete();
            return true;
        } catch (Exception e) {}
        return false;
    }

    public void clearSensorDataMetadata() {
        e = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("LM_")) e.remove(key);
        }
        e.apply();
    }

    public ArrayList<DataRecord> getDataRecordsFromCSV(String csv_string) {
        if(csv_string.equals("")) return new ArrayList<>();
        ArrayList<DataRecord> records = new ArrayList<>();
        //In Zeilen aufspalten
        String[] lines = csv_string.split("\\r?\\n");
        for(int i = 1; i < lines.length; i ++) {
            try{
                Date time = new Date();
                double p1 = 0.0;
                double p2 = 0.0;
                double temp = 0.0;
                double humidity = 0.0;
                double pressure = 0.0;
                double gps_lat = -200.0;
                double gps_lng = -200.0;
                double gps_alt = -1000;
                //SimpleDateFormat initialisieren
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

                //Zeile aufspalten
                String[] line_contents = lines[i].split(";");
                try { if(!line_contents[0].equals("")) time = sdf.parse(line_contents[0]); } catch (Exception e) {}
                try { if(!line_contents[1].equals("")) p1 = Double.parseDouble(line_contents[1]); } catch (Exception e) {}
                try { if(!line_contents[2].equals("")) p1 = Double.parseDouble(line_contents[2]); } catch (Exception e) {}
                try { if(!line_contents[3].equals("")) p1 = Double.parseDouble(line_contents[3]); } catch (Exception e) {}
                try { if(!line_contents[4].equals("")) p2 = Double.parseDouble(line_contents[4]); } catch (Exception e) {}
                try { if(!line_contents[5].equals("")) p2 = Double.parseDouble(line_contents[5]); } catch (Exception e) {}
                try { if(!line_contents[6].equals("")) p2 = Double.parseDouble(line_contents[6]); } catch (Exception e) {}
                try { if(!line_contents[7].equals("")) p1 = Double.parseDouble(line_contents[7]); } catch (Exception e) {}
                try { if(!line_contents[8].equals("")) p2 = Double.parseDouble(line_contents[8]); } catch (Exception e) {}
                try { if(!line_contents[9].equals("")) temp = Double.parseDouble(line_contents[9]); } catch (Exception e) {}
                try { if(!line_contents[10].equals("")) humidity = Double.parseDouble(line_contents[10]); } catch (Exception e) {}
                try { if(!line_contents[11].equals("")) temp = Double.parseDouble(line_contents[11]); } catch (Exception e) {}
                try { if(!line_contents[12].equals("")) pressure = Double.parseDouble(line_contents[12]); } catch (Exception e) {}
                try { if(!line_contents[13].equals("")) temp = Double.parseDouble(line_contents[13]); } catch (Exception e) {}
                try { if(!line_contents[14].equals("")) humidity = Double.parseDouble(line_contents[14]); } catch (Exception e) {}
                try { if(!line_contents[15].equals("")) pressure = Double.parseDouble(line_contents[15]); } catch (Exception e) {}
                try { if(!line_contents[20].equals("")) p1 = Double.parseDouble(line_contents[20]); } catch (Exception e) {}
                try { if(!line_contents[21].equals("")) p2 = Double.parseDouble(line_contents[21]); } catch (Exception e) {}
                try { if(!line_contents[22].equals("")) p1 = Double.parseDouble(line_contents[22]); } catch (Exception e) {}
                try { if(!line_contents[23].equals("")) p2 = Double.parseDouble(line_contents[23]); } catch (Exception e) {}
                try { if(!line_contents[24].equals("")) gps_lat = Double.parseDouble(line_contents[24]); } catch (Exception e) {}
                try { if(!line_contents[25].equals("")) gps_lng = Double.parseDouble(line_contents[25]); } catch (Exception e) {}
                try { if(!line_contents[26].equals("")) gps_alt = Double.parseDouble(line_contents[26]); } catch (Exception e) {}

                records.add(new DataRecord(time, p1, p2, temp, humidity, pressure / ((double) 100), gps_lat, gps_lng, gps_alt));
            } catch (Exception e) {}
        }
        return records;
    }

    public ArrayList<DataRecord> trimDataRecords(ArrayList<DataRecord> records, String current_date_string) {
        ArrayList<DataRecord> new_records = new ArrayList<>();
        for(DataRecord r : records) {
            if(sdf_date.format(r.getDateTime()).equals(current_date_string)) new_records.add(r);
        }
        return new_records;
    }

    public void unpackZipFile(String sensor_id, String date) {
        try {
            String month = date.substring(3, 5);
            String year = date.substring(6);

            File dir = new File(context.getFilesDir(), "/SensorData");
            if(!dir.exists()) dir.mkdirs();
            String path = new File(dir, sensor_id + "_" + year + "_" + month + ".zip").getAbsolutePath();

            InputStream is;
            ZipInputStream zis;

            String filename;
            is = new FileInputStream(path);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while((ze = zis.getNextEntry()) != null) {
                filename = ze.getName();

                if (ze.isDirectory()) {
                    File file = new File(path + filename);
                    file.mkdirs();
                } else {
                    FileOutputStream fout = new FileOutputStream(dir.getAbsolutePath() + "/" + sensor_id + filename.substring(4));

                    while((count = zis.read(buffer)) != -1) fout.write(buffer, 0, count);

                    fout.close();
                    zis.closeEntry();
                }
            }
            zis.close();

            //Zip-Datei löschen
            new File(path).delete();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public long getCSVLastModified(String sensor_id, String date_string) {
        return getLong("LM_" + date_string + "_" + sensor_id);
    }

    public long getZipLastModified(String sensor_id, String date_string) {
        String month = date_string.substring(3, 5);
        String year = date_string.substring(6);
        return getLong("LM_" + year + "_" + month + "_" + sensor_id + "_zip");
    }

    //---------------------------------------SharedPreferences--------------------------------------

    public void putString(String name, String value) {
        e = prefs.edit();
        e.putString(name, value);
        e.apply();
    }

    public void putInt(String name, int value) {
        e = prefs.edit();
        e.putInt(name, value);
        e.apply();
    }

    public void putBoolean(String name, boolean value) {
        e = prefs.edit();
        e.putBoolean(name, value);
        e.apply();
    }

    public void putLong(String name, long value) {
        e = prefs.edit();
        e.putLong(name, value);
        e.apply();
    }

    public void putDouble(String name, double value) {
        e = prefs.edit();
        e.putFloat(name, (float) value);
        e.apply();
    }

    public String getString(String name) {
        return prefs.getString(name, DEFAULT_STRING_VALUE);
    }

    public int getInt(String name) {
        return prefs.getInt(name, DEFAULT_INT_VALUE);
    }

    public boolean getBoolean(String name) {
        return prefs.getBoolean(name, DEFAULT_BOOLEAN_VALUE);
    }

    public long getLong(String name) { return prefs.getLong(name, DEFAULT_LONG_VALUE); }

    public double getDouble(String name) {
        return prefs.getFloat(name, (float) DEFAULT_DOUBLE_VALUE);
    }

    public String getString(String name, String default_value) {
        return prefs.getString(name, default_value);
    }

    public int getInt(String name, int default_value) {
        return prefs.getInt(name, default_value);
    }

    public boolean getBoolean(String name, boolean default_value) {
        return prefs.getBoolean(name, default_value);
    }

    public long getLong(String name, long default_value) {
        return  prefs.getLong(name, default_value);
    }

    public double getDouble(String name, double default_value) {
        return prefs.getFloat(name, (float) default_value);
    }

    public void removeKey(String name) {
        e = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key.equals(name)) e.remove(key);
        }
        e.apply();
    }

    //------------------------------------------Datenbank-------------------------------------------

    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            //Tabellen erstellen
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SENSORS + " (sensor_id text PRIMARY KEY, sensor_name text, sensor_color integer);");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXTERNAL_SENSORS + " (sensor_id text PRIMARY KEY, latitude double, longitude double);");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAVOURITES + " (sensor_id text PRIMARY KEY, sensor_name text, sensor_color integer);");
        } catch (Exception e) {
            Log.e("ChatLet", "Database creation error: ", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion && newVersion == 3) {
            //Datenbank-Update
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_EXTERNAL_SENSORS + " (sensor_id text PRIMARY KEY, latitude double, longitude double);");
        }
    }

    public void addRecord(String table, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        db.insert(table, null, values);
    }

    public void execSQL(String command) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(command);
    }

    //---------------------------------------Eigene Sensoren----------------------------------------

    public void addOwnSensor(Sensor sensor, boolean offline, boolean request_from_realtime_sync_service) {
        ContentValues values = new ContentValues();
        values.put("sensor_id", sensor.getChipID());
        values.put("sensor_name", sensor.getName());
        values.put("sensor_color", sensor.getColor());
        addRecord(TABLE_SENSORS, values);
        putBoolean(sensor.getChipID() + "_offline", offline);
        //Falls ein Web-Client verbunden ist, refreshen
        if(WebRealtimeSyncService.own_instance != null && !request_from_realtime_sync_service) WebRealtimeSyncService.own_instance.refresh(context);
    }

    public Sensor getSensor(String chip_id) {
        Log.d("FA", "GetSensor: " + chip_id);
        ArrayList<Sensor> sensors = getAllOwnSensors();
        sensors.addAll(getAllFavourites());
        for(Sensor s : sensors) {
            if(s.getChipID().equals(chip_id)) return s;
        }
        return null;
    }

    public boolean isSensorExistingLocally(String chip_id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT sensor_id FROM " + TABLE_SENSORS + " WHERE sensor_id = '" + chip_id + "'", null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public void updateOwnSensor(Sensor new_sensor, boolean request_from_realtime_sync_service) {
        execSQL("UPDATE " + TABLE_SENSORS + " SET sensor_name = '" + new_sensor.getName() + "', sensor_color = '" + new_sensor.getColor() + "' WHERE sensor_id = '" + new_sensor.getChipID() + "';");
        //Falls ein Web-Client verbunden ist, refreshen
        if(WebRealtimeSyncService.own_instance != null && !request_from_realtime_sync_service) WebRealtimeSyncService.own_instance.refresh(context);
    }

    public void removeOwnSensor(String chip_id, boolean request_from_realtime_sync_service) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SENSORS, "sensor_id = ?", new String[]{chip_id});
        //Falls ein Web-Client verbunden ist, refreshen
        if(WebRealtimeSyncService.own_instance != null && !request_from_realtime_sync_service) WebRealtimeSyncService.own_instance.refresh(context);
    }

    public ArrayList<Sensor> getAllOwnSensors() {
        try{
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT sensor_id, sensor_name, sensor_color FROM " + TABLE_SENSORS, null);
            ArrayList<Sensor> sensors = new ArrayList<>();
            while(cursor.moveToNext()) {
                sensors.add(new Sensor(cursor.getString(0), cursor.getString(1), cursor.getInt(2)));
            }
            cursor.close();
            Collections.sort(sensors);
            return sensors;
        } catch (Exception e) {}
        return new ArrayList<>();
    }

    public boolean isSensorInOfflineMode(String chip_id) {
        return getBoolean(chip_id + "_offline");
    }

    //------------------------------------------Favoriten-------------------------------------------

    public void addFavourite(Sensor sensor, boolean request_from_realtime_sync_service) {
        ContentValues values = new ContentValues();
        values.put("sensor_id", sensor.getChipID());
        values.put("sensor_name", sensor.getName());
        values.put("sensor_color", sensor.getColor());
        addRecord(TABLE_FAVOURITES, values);
        //Falls ein Web-Client verbunden ist, refreshen
        if(WebRealtimeSyncService.own_instance != null && !request_from_realtime_sync_service) WebRealtimeSyncService.own_instance.refresh(context);
    }

    public boolean isFavouriteExisting(String chip_id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT sensor_id FROM " + TABLE_FAVOURITES + " WHERE sensor_id = '" + chip_id + "'", null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public void updateFavourite(Sensor new_sensor, boolean request_from_realtime_sync_service) {
        execSQL("UPDATE " + TABLE_FAVOURITES + " SET sensor_name = '" + new_sensor.getName() + "', sensor_color = '" + new_sensor.getColor() + "' WHERE sensor_id = '" + new_sensor.getChipID() + "';");
        //Falls ein Web-Client verbunden ist, refreshen
        if(WebRealtimeSyncService.own_instance != null && !request_from_realtime_sync_service) WebRealtimeSyncService.own_instance.refresh(context);
    }

    public void removeFavourite(String chip_id, boolean request_from_realtime_sync_service) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_FAVOURITES, "sensor_id = ?", new String[]{chip_id});
        //Falls ein Web-Client verbunden ist, refreshen
        if(WebRealtimeSyncService.own_instance != null && !request_from_realtime_sync_service) WebRealtimeSyncService.own_instance.refresh(context);
    }

    public ArrayList<Sensor> getAllFavourites() {
        try{
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT sensor_id, sensor_name, sensor_color FROM " + TABLE_FAVOURITES, null);
            ArrayList<Sensor> sensors = new ArrayList<>();
            while(cursor.moveToNext()) {
                sensors.add(new Sensor(cursor.getString(0), cursor.getString(1), cursor.getInt(2)));
            }
            cursor.close();
            Collections.sort(sensors);
            return sensors;
        } catch (Exception e) {}
        return new ArrayList<>();
    }

    //---------------------------------------Externe Sensoren---------------------------------------

    public void addExternalSensor(ExternalSensor sensor) {
        if(!isExternalSensorExisting(sensor.getChipID())) {
            ContentValues values = new ContentValues();
            values.put("sensor_id", sensor.getChipID());
            values.put("latitude", sensor.getLat());
            values.put("longitude", sensor.getLng());
            addRecord(TABLE_EXTERNAL_SENSORS, values);
        } else {
            execSQL("UPDATE " + TABLE_EXTERNAL_SENSORS + " SET latitude = " + sensor.getLat() + ", longitude = " + sensor.getLng() + " WHERE sensor_id = '" + sensor.getChipID() + "';");
        }
    }

    public boolean isExternalSensorExisting(String chip_id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT sensor_id FROM " + TABLE_EXTERNAL_SENSORS + " WHERE sensor_id = '" + chip_id + "'", null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public void clearExternalSensors() {
        execSQL("DELETE FROM " + TABLE_EXTERNAL_SENSORS);
    }

    public void deleteExternalSensor(String chip_id) {
        execSQL("DELETE FROM " + TABLE_EXTERNAL_SENSORS + " WHERE sensor_id = '" + chip_id + "'");
    }

    public ArrayList<ExternalSensor> getExternalSensors() {
        try{
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT sensor_id, latitude, longitude FROM " + TABLE_EXTERNAL_SENSORS, null);
            ArrayList<ExternalSensor> sensors = new ArrayList<>();
            while(cursor.moveToNext()) {
                sensors.add(new ExternalSensor(cursor.getString(0), cursor.getDouble(1), cursor.getDouble(2)));
            }
            cursor.close();
            return sensors;
        } catch (Exception e) {}
        return new ArrayList<>();
    }

    //------------------------------------------Messdaten-------------------------------------------

    public void saveRecords(String chip_id, ArrayList<DataRecord> records) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        //Tabelle erstellen, falls sie noch nicht existiert
        db.compileStatement("CREATE TABLE IF NOT EXISTS data_" + chip_id + " (time integer PRIMARY KEY, pm2_5 double, pm10 double, temp double, humidity double, pressure double, gps_lat double, gps_lng double, gps_alt double, note text);").execute();
        //Datensätze in Tabelle schreiben
        SQLiteStatement stmt = db.compileStatement("INSERT INTO data_" + chip_id + " (time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);");
        for(DataRecord r : records) {
            try{
                stmt.bindLong(1, r.getDateTime().getTime() / 1000);
                stmt.bindDouble(2, r.getP2());
                stmt.bindDouble(3, r.getP1());
                stmt.bindDouble(4, r.getTemp());
                stmt.bindDouble(5, r.getHumidity());
                stmt.bindDouble(6, r.getPressure());
                stmt.bindDouble(7, r.getLat());
                stmt.bindDouble(8, r.getLng());
                stmt.bindDouble(9, r.getAlt());
                stmt.execute();
            } catch (SQLiteConstraintException e) {} finally {
                stmt.clearBindings();
            }
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    public ArrayList<DataRecord> loadRecords(String chip_id, long from, long to) {
        try{
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt FROM data_" + chip_id + " WHERE time >= " + from / 1000 + " AND time < " + to / 1000 , null);
            ArrayList<DataRecord> records = new ArrayList<>();
            while(cursor.moveToNext()) {
                Date time = new Date();
                time.setTime(cursor.getLong(0) * 1000);
                records.add(new DataRecord(time, cursor.getDouble(2), cursor.getDouble(1), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8)));
            }
            cursor.close();
            return records;
        } catch (Exception e) {}
        return new ArrayList<>();
    }

    public void deleteDataDatabase(String chip_id) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS data_" + chip_id);
    }

    public void deleteAllDataDatabases() {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'data_%'", null);
        while(cursor.moveToNext()) {
            db.execSQL("DROP TABLE " + cursor.getString(0));
            Log.i("FA", "Deleted Database: " + cursor.getString(0));
        }
    }
}