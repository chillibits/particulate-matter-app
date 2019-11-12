/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.Utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.Xml
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.ExternalSensor
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.Services.WebRealtimeSyncService
import org.xmlpull.v1.XmlPullParser
import java.io.FileInputStream
import java.io.IOException
import java.io.StringWriter
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*

class StorageUtils(private val context: Context) : SQLiteOpenHelper(context, "database.db", null, 3) {

    // Variables as objects
    private val prefs: SharedPreferences = context.getSharedPreferences("com.mrgames13.jimdo.feinstaubapp_preferences", Context.MODE_PRIVATE)

    val allOwnSensors: ArrayList<Sensor>
        get() {
            try {
                val db = readableDatabase
                val cursor = db.rawQuery("SELECT sensor_id, sensor_name, sensor_color FROM $TABLE_SENSORS", null)
                val sensors = ArrayList<Sensor>()
                while (cursor.moveToNext()) {
                    sensors.add(Sensor(cursor.getString(0), cursor.getString(1), cursor.getInt(2)))
                }
                cursor.close()
                sensors.sort()
                return sensors
            } catch (ignored: Exception) {}
            return ArrayList()
        }

    val allFavourites: ArrayList<Sensor>
        get() {
            try {
                val db = readableDatabase
                val cursor = db.rawQuery("SELECT sensor_id, sensor_name, sensor_color FROM $TABLE_FAVOURITES", null)
                val sensors = ArrayList<Sensor>()
                while (cursor.moveToNext()) {
                    sensors.add(Sensor(cursor.getString(0), cursor.getString(1), cursor.getInt(2)))
                }
                cursor.close()
                sensors.sort()
                return sensors
            } catch (ignored: Exception) {}
            return ArrayList()
        }

    val externalSensors: ArrayList<ExternalSensor>
        get() {
            try {
                val db = readableDatabase
                val cursor = db.rawQuery("SELECT sensor_id, latitude, longitude FROM $TABLE_EXTERNAL_SENSORS", null)
                val sensors = ArrayList<ExternalSensor>()
                while (cursor.moveToNext()) {
                    sensors.add(ExternalSensor(cursor.getString(0), cursor.getDouble(1), cursor.getDouble(2)))
                }
                cursor.close()
                return sensors
            } catch (ignored: Exception) {}
            return ArrayList()
        }

    //-----------------------------------------File-system------------------------------------------

    fun clearSensorDataMetadata() {
        for (key in prefs.all.keys) {
            if (key.startsWith("LM_")) prefs.edit().remove(key).apply()
        }
    }

    //---------------------------------------SharedPreferences--------------------------------------

    fun putString(name: String, value: String) {
        prefs.edit().putString(name, value).apply()
    }

    fun putInt(name: String, value: Int) {
        prefs.edit().putInt(name, value).apply()
    }

    fun putBoolean(name: String, value: Boolean) {
        prefs.edit().putBoolean(name, value).apply()
    }

    fun putLong(name: String, value: Long) {
        prefs.edit().putLong(name, value).apply()
    }

    fun putDouble(name: String, value: Double) {
        prefs.edit().putFloat(name, value.toFloat()).apply()
    }

    fun getString(name: String): String {
        return prefs.getString(name, DEFAULT_STRING_VALUE).toString()
    }

    fun getBoolean(name: String): Boolean {
        return prefs.getBoolean(name, DEFAULT_BOOLEAN_VALUE)
    }

    fun getDouble(name: String): Double {
        return prefs.getFloat(name, DEFAULT_DOUBLE_VALUE.toFloat()).toDouble()
    }

    fun getString(name: String, default_value: String): String {
        return prefs.getString(name, default_value).toString()
    }

    fun getInt(name: String, default_value: Int): Int {
        return prefs.getInt(name, default_value)
    }

    fun getBoolean(name: String, default_value: Boolean): Boolean {
        return prefs.getBoolean(name, default_value)
    }

    fun getLong(name: String, default_value: Long): Long {
        return prefs.getLong(name, default_value)
    }

    fun removeKey(name: String) {
        prefs.edit().remove(name).apply()
    }

    //------------------------------------------Database--------------------------------------------

    override fun onCreate(db: SQLiteDatabase) {
        try {
            // Create tables
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_SENSORS (sensor_id text PRIMARY KEY, sensor_name text, sensor_color integer);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_EXTERNAL_SENSORS (sensor_id text PRIMARY KEY, latitude double, longitude double);")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_FAVOURITES (sensor_id text PRIMARY KEY, sensor_name text, sensor_color integer);")
        } catch (e: Exception) {
            Log.e("ChatLet", "Database creation error: ", e)
        }

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (newVersion > oldVersion && newVersion == 3) {
            // Update database
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_EXTERNAL_SENSORS (sensor_id text PRIMARY KEY, latitude double, longitude double);")
        }
    }

    private fun addRecord(table: String, values: ContentValues) {
        val db = writableDatabase
        db.insert(table, null, values)
    }

    private fun execSQL(command: String) {
        val db = writableDatabase
        db.execSQL(command)
    }

    //-----------------------------------------Own-sensors------------------------------------------

    fun addOwnSensor(sensor: Sensor, offline: Boolean, request_from_realtime_sync_service: Boolean) {
        val values = ContentValues()
        values.put("sensor_id", sensor.chipID)
        values.put("sensor_name", sensor.name)
        values.put("sensor_color", sensor.color)
        addRecord(TABLE_SENSORS, values)
        putBoolean(sensor.chipID + "_offline", offline)
        // Refresh, if a web client is connected
        if (!request_from_realtime_sync_service) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun getSensor(chip_id: String): Sensor? {
        val sensors = allOwnSensors
        sensors.addAll(allFavourites)
        for (s in sensors) {
            if (s.chipID == chip_id) return s
        }
        return null
    }

    fun isSensorExisting(chip_id: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT sensor_id FROM $TABLE_SENSORS WHERE sensor_id = '$chip_id'", null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun updateOwnSensor(new_sensor: Sensor, request_from_realtime_sync_service: Boolean) {
        execSQL("UPDATE " + TABLE_SENSORS + " SET sensor_name = '" + new_sensor.name + "', sensor_color = '" + new_sensor.color + "' WHERE sensor_id = '" + new_sensor.chipID + "';")
        // Refresh, if a web client is connected
        if (!request_from_realtime_sync_service) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun removeOwnSensor(chip_id: String, request_from_realtime_sync_service: Boolean) {
        val db = writableDatabase
        db.delete(TABLE_SENSORS, "sensor_id = ?", arrayOf(chip_id))
        // Refresh, if a web client is connected
        if (!request_from_realtime_sync_service) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun isSensorInOfflineMode(chip_id: String): Boolean {
        return getBoolean(chip_id + "_offline")
    }

    //------------------------------------------Favourites------------------------------------------

    fun addFavourite(sensor: Sensor, request_from_realtime_sync_service: Boolean) {
        val values = ContentValues()
        values.put("sensor_id", sensor.chipID)
        values.put("sensor_name", sensor.name)
        values.put("sensor_color", sensor.color)
        addRecord(TABLE_FAVOURITES, values)
        // Refresh, if a web client is connected
        if (!request_from_realtime_sync_service) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun isFavouriteExisting(chip_id: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT sensor_id FROM $TABLE_FAVOURITES WHERE sensor_id = '$chip_id'", null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun updateFavourite(new_sensor: Sensor, request_from_realtime_sync_service: Boolean) {
        execSQL("UPDATE " + TABLE_FAVOURITES + " SET sensor_name = '" + new_sensor.name + "', sensor_color = '" + new_sensor.color + "' WHERE sensor_id = '" + new_sensor.chipID + "';")
        // Refresh, if a web client is connected
        if (!request_from_realtime_sync_service) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun removeFavourite(chip_id: String, request_from_realtime_sync_service: Boolean) {
        val db = writableDatabase
        db.delete(TABLE_FAVOURITES, "sensor_id = ?", arrayOf(chip_id))
        // Refresh, if a web client is connected
        if (!request_from_realtime_sync_service) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    //---------------------------------------Externe Sensoren---------------------------------------

    fun addExternalSensor(sensor: ExternalSensor) {
        if (!isExternalSensorExisting(sensor.chipID)) {
            val values = ContentValues()
            values.put("sensor_id", sensor.chipID)
            values.put("latitude", sensor.lat)
            values.put("longitude", sensor.lng)
            addRecord(TABLE_EXTERNAL_SENSORS, values)
        } else {
            execSQL("UPDATE " + TABLE_EXTERNAL_SENSORS + " SET latitude = " + sensor.lat + ", longitude = " + sensor.lng + " WHERE sensor_id = '" + sensor.chipID + "';")
        }
    }

    private fun isExternalSensorExisting(chip_id: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT sensor_id FROM $TABLE_EXTERNAL_SENSORS WHERE sensor_id = '$chip_id'", null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun clearExternalSensors() {
        execSQL("DELETE FROM $TABLE_EXTERNAL_SENSORS")
    }

    fun deleteExternalSensor(chip_id: String) {
        execSQL("DELETE FROM $TABLE_EXTERNAL_SENSORS WHERE sensor_id = '$chip_id'")
    }

    //------------------------------------------Messdaten-------------------------------------------

    internal fun saveRecords(chip_id: String, records: ArrayList<DataRecord>) {
        val db = writableDatabase
        db.beginTransaction()
        // Create table if it doesn't already exist
        db.compileStatement("CREATE TABLE IF NOT EXISTS data_$chip_id (time integer PRIMARY KEY, pm2_5 double, pm10 double, temp double, humidity double, pressure double, gps_lat double, gps_lng double, gps_alt double, note text);").execute()
        // Write records into db
        val stmt = db.compileStatement("INSERT INTO data_$chip_id (time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")
        for (r in records) {
            try {
                stmt.bindLong(1, r.dateTime.time / 1000)
                stmt.bindDouble(2, r.p2)
                stmt.bindDouble(3, r.p1)
                stmt.bindDouble(4, r.temp)
                stmt.bindDouble(5, r.humidity)
                stmt.bindDouble(6, r.pressure)
                stmt.bindDouble(7, r.lat)
                stmt.bindDouble(8, r.lng)
                stmt.bindDouble(9, r.alt)
                stmt.execute()
            } catch (ignored: SQLiteConstraintException) {} finally {
                stmt.clearBindings()
            }
        }
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    fun loadRecords(chip_id: String, from: Long, to: Long): ArrayList<DataRecord> {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt FROM data_" + chip_id + " WHERE time >= " + from / 1000 + " AND time < " + to / 1000, null)
            val records = ArrayList<DataRecord>()
            while (cursor.moveToNext()) {
                val time = Date()
                time.time = cursor.getLong(0) * 1000
                records.add(DataRecord(time, cursor.getDouble(2), cursor.getDouble(1), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8)))
            }
            cursor.close()
            return records
        } catch (ignored: Exception) {}
        return ArrayList()
    }

    fun getLastRecord(chip_id: String): DataRecord? {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt FROM data_$chip_id ORDER BY time DESC LIMIT 1", null)
            cursor.moveToNext()
            val time = Date()
            time.time = cursor.getLong(0) * 1000
            val record = DataRecord(time, cursor.getDouble(2), cursor.getDouble(1), cursor.getDouble(3), cursor.getDouble(4), cursor.getDouble(5), cursor.getDouble(6), cursor.getDouble(7), cursor.getDouble(8))
            cursor.close()
            return record
        } catch (ignored: Exception) {}
        return null
    }

    fun shareImage(image: Bitmap, share_message: String) {
        try {
            // Save
            val out = context.openFileOutput("export.png", Context.MODE_PRIVATE)
            image.compress(Bitmap.CompressFormat.PNG, 80, out)
            out.close()
            val uri = FileProvider.getUriForFile(context, "com.mrgames13.jimdo.feinstaubapp", context.getFileStreamPath("export.png"))
            // Share
            val i = Intent(Intent.ACTION_SEND)
            i.type = URLConnection.guessContentTypeFromName(uri.path)
            i.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(i, share_message))
        } catch (ignored: IOException) {}
    }

    fun exportDataRecords(records: ArrayList<DataRecord>): Uri? {
        try {
            val out = context.openFileOutput("export.csv", Context.MODE_PRIVATE)
            val sdfDatetime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            out.write("Time;PM10;PM2.5;Temperature;Humidity;Pressure;Latitude;Longitude;Altitude\n".toByteArray())
            for (record in records) {
                val time = sdfDatetime.format(record.dateTime.time)
                val p1 = record.p1.toString()
                val p2 = record.p2.toString()
                val temp = record.temp.toString()
                val humidity = record.humidity.toString()
                val pressure = record.pressure.toString()
                val gpsLat = record.lat.toString()
                val gpsLng = record.lng.toString()
                val gpsAlt = record.alt.toString()
                out.write("$time;$p1;$p2;$temp;$humidity;$pressure;$gpsLat;$gpsLng;$gpsAlt\n".toByteArray())
            }
            out.close()
            return FileProvider.getUriForFile(context, "com.mrgames13.jimdo.feinstaubapp", context.getFileStreamPath("export.csv"))
        } catch (ignored: Exception) {}
        return null
    }

    fun deleteDataDatabase(chip_id: String) {
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS data_$chip_id")
    }

    fun deleteAllDataDatabases() {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'data_%'", null)
        while (cursor.moveToNext()) {
            db.execSQL("DROP TABLE " + cursor.getString(0))
            Log.i("FA", "Deleted database: " + cursor.getString(0))
        }
        cursor.close()
    }

    fun importXMLFile(path: String): Boolean {
        try {
            val inputStream = FileInputStream(path)
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()

            val favourites = ArrayList<Sensor>()
            val ownSensors = ArrayList<Sensor>()
            // Favourites
            parser.require(XmlPullParser.START_TAG, null, "sensor-configuration")
            parser.nextTag()
            parser.require(XmlPullParser.START_TAG, null, "favourites")
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue

                parser.require(XmlPullParser.START_TAG, null, "sensor")

                val s = Sensor()
                s.setId(parser.getAttributeValue(null, "id"))
                s.name = parser.getAttributeValue(null, "name")
                s.color = Integer.parseInt(parser.getAttributeValue(null, "color"))
                favourites.add(s)

                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "sensor")
            }
            parser.require(XmlPullParser.END_TAG, null, "favourites")
            parser.nextTag()
            // Own sensors
            parser.require(XmlPullParser.START_TAG, null, "own-sensors")
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) continue

                parser.require(XmlPullParser.START_TAG, null, "sensor")

                val s = Sensor()
                s.setId(parser.getAttributeValue(null, "id"))
                s.name = parser.getAttributeValue(null, "name")
                s.color = Integer.parseInt(parser.getAttributeValue(null, "color"))
                ownSensors.add(s)

                parser.nextTag()
                parser.require(XmlPullParser.END_TAG, null, "sensor")
            }
            parser.require(XmlPullParser.END_TAG, null, "own-sensors")
            parser.nextTag()
            parser.require(XmlPullParser.END_TAG, null, "sensor-configuration")

            // Import
            for (s in favourites) {
                if (!isSensorExisting(s.chipID)) addFavourite(s, false)
            }
            for (s in ownSensors) {
                if (!isSensorExisting(s.chipID)) addOwnSensor(s, offline = true, request_from_realtime_sync_service = false)
            }
            Log.i("FA", "Imported favourites: " + favourites.size)
            Log.i("FA", "Imported own sensors: " + ownSensors.size)

            inputStream.close()
            return true
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_try_again, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        return false
    }

    fun exportXMLFile() {
        val favourites = allFavourites
        val ownSensors = allOwnSensors

        val serializer = Xml.newSerializer()
        val writer = StringWriter()
        try {
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", "sensor-configuration")
            // Favourites
            serializer.startTag("", "favourites")
            for (s in favourites) {
                serializer.startTag("", "sensor")
                serializer.attribute("", "id", s.chipID)
                serializer.attribute("", "name", s.name)
                serializer.attribute("", "color", s.color.toString())
                serializer.endTag("", "sensor")
            }
            serializer.endTag("", "favourites")
            // Own sensors
            serializer.startTag("", "own-sensors")
            for (s in ownSensors) {
                serializer.startTag("", "sensor")
                serializer.attribute("", "id", s.chipID)
                serializer.attribute("", "name", s.name)
                serializer.attribute("", "color", s.color.toString())
                serializer.endTag("", "sensor")
            }
            serializer.endTag("", "own-sensors")
            serializer.endTag("", "sensor-configuration")
            serializer.endDocument()
            // Write to file
            val out = context.openFileOutput("sensor_config.xml", Context.MODE_PRIVATE)
            out.write(writer.toString().toByteArray())
            out.close()
            val uri = FileProvider.getUriForFile(context, "com.mrgames13.jimdo.feinstaubapp", context.getFileStreamPath("sensor_config.xml"))
            // Share
            val i = Intent(Intent.ACTION_SEND)
            i.type = URLConnection.guessContentTypeFromName(uri.path)
            i.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(i, context.getString(R.string.export_xml_file)))
        } catch (ignored: Exception) {}
    }

    companion object {
        // Constants
        private const val DEFAULT_STRING_VALUE = ""
        private const val DEFAULT_BOOLEAN_VALUE = false
        private const val DEFAULT_DOUBLE_VALUE = 0.0
        private const val TABLE_SENSORS = "Sensors"
        private const val TABLE_EXTERNAL_SENSORS = "ExternalSensors"
        private const val TABLE_FAVOURITES = "Favourites"
    }
}