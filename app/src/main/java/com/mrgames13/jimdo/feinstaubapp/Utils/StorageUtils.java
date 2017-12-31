package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class StorageUtils extends SQLiteOpenHelper {

    //Konstanten
    private final String DEFAULT_STRING_VALUE = "";
    private final int DEFAULT_INT_VALUE = -1;
    private final boolean DEFAULT_BOOLEAN_VALUE = false;
    public static final String TABLE_SENSORS = "Sensors";

    //Variablen als Objekte
    private Resources res;
    private Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor e;

    //Variablen

    public StorageUtils(Context context) {
        super(context, "database.db", null, 1);
        this.res = context.getResources();
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
            String file_name = sensor_id + date + ".csv";
            File dir = new File(context.getFilesDir(), "/SensorData");
            File file = new File(dir, file_name);

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
        } catch (Exception e) {}
        return "";
    }

    public boolean isFileExisting(String path) {
        File file = new File(path);
        return file.exists();
    }

    //---------------------------------------SharedPreferences--------------------------------------

    public void putString(String name, String value) {
        e = prefs.edit();
        e.putString(name, value);
        e.commit();
    }

    public void putInt(String name, int value) {
        e = prefs.edit();
        e.putInt(name, value);
        e.commit();
    }

    public void putBoolean(String name, boolean value) {
        e = prefs.edit();
        e.putBoolean(name, value);
        e.commit();
    }

    public void putStringSet(String name, Set<String> value) {
        e = prefs.edit();
        e.putStringSet(name, value);
        e.commit();
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

    public Set<String> getStringSet(String name) {
        return prefs.getStringSet(name, null);
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

    public void removePair(String name) {
        e = prefs.edit();
        e.remove(name);
        e.commit();
    }

    public void clearData(String name) {
        prefs.edit().remove(name).commit();
    }

    public void clear() {
        prefs.edit().clear().commit();
    }

    //------------------------------------------------Datenbank---------------------------------------------

    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            //Tabellen erstellen
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SENSORS + " (sensor_id text, sensor_name text, sensor_color integer);");
        } catch (Exception e) {
            Log.e("ChatLet", "Database creation error: ", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {

        }
    }

    public long addRecord(String table, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(table, null, values);
        //db.close();
        return id;
    }

    public void removeRecord(String table, String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, "ROWID", new String[] {id});
        //db.close();
    }

    public void execSQL(String command) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(command);
        //db.close();
    }

    //-----------------------------------------Sensoren---------------------------------------------

    public void addSensor(Sensor sensor) {
        ContentValues values = new ContentValues();
        values.put("sensor_id", sensor.getId());
        values.put("sensor_name", sensor.getName());
        values.put("sensor_color", sensor.getColor());
        addRecord(TABLE_SENSORS, values);
    }

    public void clearSensors() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SENSORS, "", null);
    }

    public ArrayList<Sensor> getAllSensors() {
        try{
            SQLiteDatabase db = getWritableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SENSORS, null);
            ArrayList<Sensor> sensors = new ArrayList<>();
            while(cursor.moveToNext()) {
                sensors.add(new Sensor(cursor.getString(0), cursor.getString(1), cursor.getInt(2)));
            }
            cursor.close();
            return sensors;
        } catch (Exception e) {
            Log.e("ChatLet", "Error loading message", e);
        }
        return new ArrayList<>();
    }

    public Sensor getSensor(String sensor_id) {
        for(Sensor s : getAllSensors()) {
            if(s.getId().equals(sensor_id)) return s;
        }
        return null;
    }
}