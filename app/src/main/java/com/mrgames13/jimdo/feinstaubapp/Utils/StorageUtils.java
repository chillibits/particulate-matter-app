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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StorageUtils extends SQLiteOpenHelper {

    //Konstanten
    private final String DEFAULT_STRING_VALUE = "";
    private final int DEFAULT_LONG_VALUE = -1;
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

    public ArrayList<DataRecord> getDataRecordsFromCSV(String csv_string) {
        if(csv_string.equals("")) return new ArrayList<>();
        ArrayList<DataRecord> records = new ArrayList<>();
        //In Zeilen aufspalten
        String[] lines = csv_string.split("\\r?\\n");
        for(int i = 1; i < lines.length; i ++) {
            try{
                Date time = new Date();
                double sdsp1 = 0.0;
                double sdsp2 = 0.0;
                double temp = 0.0;
                double humidity = 0.0;
                double pressure = 0.0;
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
                if(!line_contents[13+2].equals("")) temp = Double.parseDouble(line_contents[13+2]); // Luftdaten.info Bug. Zwei Spalten zu viel
                if(!line_contents[14+2].equals("")) humidity = Double.parseDouble(line_contents[14+2]); // Luftdaten.info Bug. Zwei Spalten zu viel
                if(!line_contents[15+2].equals("")) pressure = Double.parseDouble(line_contents[15+2]); // Luftdaten.info Bug. Zwei Spalten zu viel

                records.add(new DataRecord(time, sdsp1, sdsp2, temp, humidity, pressure));
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
                    FileOutputStream fout = new FileOutputStream(dir.getAbsolutePath() + "/" + filename.substring(13));

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

    public void putLong(String name, long value) {
        e = prefs.edit();
        e.putLong(name, value);
        e.apply();
    }

    public String getString(String name) {
        return prefs.getString(name, DEFAULT_STRING_VALUE);
    }

    public long getLong(String name) { return prefs.getLong(name, DEFAULT_LONG_VALUE); }

    public String getString(String name, String default_value) {
        return prefs.getString(name, default_value);
    }

    public long getLong(String name, long default_value) {
        return  prefs.getLong(name, default_value);
    }

    public boolean getBoolean(String name, boolean default_value) {
        return prefs.getBoolean(name, default_value);
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
            //Datenbank-Update
        }
    }

    public void addRecord(String table, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(table, null, values);
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
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SENSORS + " WHERE sensor_id = '" + sensor_id + "'", null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public void updateSensor(Sensor sensor) {
        execSQL("UPDATE " + TABLE_SENSORS + " SET sensor_name = '" + sensor.getName() + "', sensor_color = '" + String.valueOf(sensor.getColor()) + "' WHERE sensor_id = '" + sensor.getId() + "';");
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
}