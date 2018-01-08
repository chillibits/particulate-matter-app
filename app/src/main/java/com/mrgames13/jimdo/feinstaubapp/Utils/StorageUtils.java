package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;

public class StorageUtils extends SQLiteOpenHelper {

    //Konstanten
    private final String DEFAULT_STRING_VALUE = "";
    private final int DEFAULT_INT_VALUE = -1;
    private final int DEFAULT_LONG_VALUE = -1;
    private final boolean DEFAULT_BOOLEAN_VALUE = false;
    public static final String TABLE_SENSORS = "Sensors";

    //Variablen als Objekte
    private Context context;
    private SharedPreferences prefs;
    private SharedPreferences.Editor e;
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");

    //Variablen

    public StorageUtils(Context context) {
        super(context, "database.db", null, 1);
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
        } catch (Exception e) {}
        return "";
    }

    public boolean isCSVFileExisting(String date, String sensor_id) {
        try{
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            date = format.format(newDate);

            String file_name = sensor_id + date + ".csv";
            File dir = new File(context.getFilesDir(), "/SensorData");
            return new File(dir, file_name).exists();
        } catch (Exception e) {}
        return false;
    }

    public ArrayList<DataRecord> getDataRecordsFromCSV(String csv_string) {
        if(csv_string.equals("")) return new ArrayList<>();
        try{
            ArrayList<DataRecord> records = new ArrayList<>();
            //In Zeilen aufspalten
            String[] lines = csv_string.split("\\r?\\n");
            for(int i = 1; i < lines.length; i++) {
                Date time = new Date();
                Double sdsp1 = 0.0;
                Double sdsp2 = 0.0;
                Double temp = 0.0;
                Double humidity = 0.0;
                //SimpleDateFormat initialisieren
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));

                //Zeile aufspalten
                String[] line_contents = lines[i].split(";");
                if(!line_contents[0].equals("")) time = sdf.parse(line_contents[0]);
                if(!line_contents[7].equals("")) sdsp1 = Double.parseDouble(line_contents[7]);
                if(!line_contents[8].equals("")) sdsp2 = Double.parseDouble(line_contents[8]);
                if(!line_contents[9].equals("")) temp = Double.parseDouble(line_contents[9]);
                if(!line_contents[10].equals("")) humidity = Double.parseDouble(line_contents[10]);
                if(!line_contents[11].equals("")) temp = Double.parseDouble(line_contents[11]);
                if(!line_contents[12].equals("")) humidity = Double.parseDouble(line_contents[12]);

                records.add(new DataRecord(time, sdsp1, sdsp2, temp, humidity));
            }
            return records;
        } catch (Exception e) {}
        return null;
    }

    public ArrayList<DataRecord> trimDataRecords(ArrayList<DataRecord> records, String current_date_string) {
        ArrayList<DataRecord> new_records = new ArrayList<>();
        for(DataRecord r : records) {
            if(sdf_date.format(r.getDateTime()).equals(current_date_string)) new_records.add(r);
        }
        return new_records;
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

    public void putLong(String name, long value) {
        e = prefs.edit();
        e.putLong(name, value);
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

    public long getLong(String name) { return prefs.getLong(name, DEFAULT_LONG_VALUE); }

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
        return id;
    }

    public void removeRecord(String table, String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(table, "ROWID", new String[] {id});
    }

    public void execSQL(String command) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(command);
    }

    //-----------------------------------------Sensoren---------------------------------------------

    public void addSensor(Sensor sensor) {
        ContentValues values = new ContentValues();
        values.put("sensor_id", sensor.getId());
        values.put("sensor_name", sensor.getName());
        values.put("sensor_color", sensor.getColor());
        addRecord(TABLE_SENSORS, values);
    }

    public boolean isSensorExisting(String sensor_id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SENSORS + " WHERE sensor_id = " + sensor_id, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public void updateSensor(Sensor sensor) {
        execSQL("UPDATE " + TABLE_SENSORS + " SET sensor_name = " + sensor.getName() + ", sensor_color = " + String.valueOf(sensor.getColor()) + " WHERE sensor_id = " + sensor.getId() + ";");
    }

    public void deleteSensor(String sensor_id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SENSORS, "sensor_id = ?", new String[]{sensor_id});
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