/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.tool

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
import com.chillibits.pmapp.model.DataRecord
import com.chillibits.pmapp.model.ExternalSensor
import com.chillibits.pmapp.model.Sensor
import com.chillibits.pmapp.service.WebRealtimeSyncService
import com.mrgames13.jimdo.feinstaubapp.R
import kotlinx.io.IOException
import kotlinx.io.StringWriter
import org.xmlpull.v1.XmlPullParser
import java.io.FileInputStream
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class StorageUtils(private val context: Context) : SQLiteOpenHelper(context, "database.db", null, 3) {

    // Variables as objects
    private val prefs: SharedPreferences = context.getSharedPreferences("com.mrgames13.jimdo.feinstaubapp_preferences", Context.MODE_PRIVATE)

    val allOwnSensors: ArrayList<Sensor>
        get() {
            return try {
                val cursor = readableDatabase.rawQuery("SELECT sensor_id, sensor_name, sensor_color FROM $TABLE_SENSORS", null)
                val sensors = ArrayList<Sensor>()
                while (cursor.moveToNext()) {
                    sensors.add(Sensor(cursor.getString(0), cursor.getString(1), cursor.getInt(2)))
                }
                cursor.close()
                sensors.sort()
                sensors
            } catch (ignored: Exception) { ArrayList() }
        }

    val allFavourites: ArrayList<Sensor>
        get() {
            return try {
                val cursor = readableDatabase.rawQuery("SELECT sensor_id, sensor_name, sensor_color FROM $TABLE_FAVOURITES", null)
                val sensors = ArrayList<Sensor>()
                while (cursor.moveToNext()) {
                    sensors.add(Sensor(cursor.getString(0), cursor.getString(1), cursor.getInt(2)))
                }
                cursor.close()
                sensors.sort()
                sensors
            } catch (ignored: Exception) { ArrayList() }
        }

    val externalSensors: ArrayList<ExternalSensor>
        get() {
            return try {
                val cursor = readableDatabase.rawQuery("SELECT sensor_id, latitude, longitude FROM $TABLE_EXTERNAL_SENSORS", null)
                val sensors = ArrayList<ExternalSensor>()
                while (cursor.moveToNext()) {
                    sensors.add(ExternalSensor(chipId = cursor.getString(0), lat = cursor.getDouble(1), lng = cursor.getDouble(2)))
                }
                cursor.close()
                sensors
            } catch (ignored: Exception) { ArrayList() }
        }

    //-----------------------------------------File-system------------------------------------------

    fun clearSensorDataMetadata() {
        prefs.all.keys.filter { it.startsWith("LM_") }.forEach {
            prefs.edit().remove(it).apply()
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

    fun getString(name: String) = prefs.getString(name, DEFAULT_STRING_VALUE).toString()
    fun getBoolean(name: String) = prefs.getBoolean(name, DEFAULT_BOOLEAN_VALUE)
    fun getDouble(name: String) = prefs.getFloat(name, DEFAULT_DOUBLE_VALUE.toFloat()).toDouble()
    fun getString(name: String, default_value: String) = prefs.getString(name, default_value).toString()
    fun getInt(name: String, default_value: Int) = prefs.getInt(name, default_value)
    fun getBoolean(name: String, default_value: Boolean) = prefs.getBoolean(name, default_value)
    fun getLong(name: String, default_value: Long) = prefs.getLong(name, default_value)

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
        writableDatabase.insert(table, null, values)
    }

    private fun execSQL(command: String) {
        writableDatabase.execSQL(command)
    }

    //-----------------------------------------Own-sensors------------------------------------------

    fun addOwnSensor(sensor: Sensor, offline: Boolean, requestFromRealtimeSyncService: Boolean) {
        val values = ContentValues()
        values.put("sensor_id", sensor.chipID)
        values.put("sensor_name", sensor.name)
        values.put("sensor_color", sensor.color)
        addRecord(TABLE_SENSORS, values)
        putBoolean(sensor.chipID + "_offline", offline)
        // Refresh, if a web client is connected
        if (!requestFromRealtimeSyncService) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun getSensor(chipId: String): Sensor? {
        val sensors = allOwnSensors + allFavourites
        return sensors.find { it.chipID == chipId }
    }

    fun isSensorExisting(chipId: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT sensor_id FROM $TABLE_SENSORS WHERE sensor_id = '$chipId'", null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun updateOwnSensor(newSensor: Sensor, requestFromRealtimeSyncService: Boolean) {
        execSQL("UPDATE " + TABLE_SENSORS + " SET sensor_name = '" + newSensor.name + "', sensor_color = '" + newSensor.color + "' WHERE sensor_id = '" + newSensor.chipID + "';")
        // Refresh, if a web client is connected
        if (!requestFromRealtimeSyncService) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun removeOwnSensor(chipId: String, requestFromRealtimeSyncService: Boolean) {
        val db = writableDatabase
        db.delete(TABLE_SENSORS, "sensor_id = ?", arrayOf(chipId))
        // Refresh, if a web client is connected
        if (!requestFromRealtimeSyncService) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun isSensorInOfflineMode(chipId: String) = getBoolean("${chipId}_offline")

    //------------------------------------------Favourites------------------------------------------

    fun addFavourite(sensor: Sensor, requestFromRealtimeSyncService: Boolean) {
        val values = ContentValues()
        values.put("sensor_id", sensor.chipID)
        values.put("sensor_name", sensor.name)
        values.put("sensor_color", sensor.color)
        addRecord(TABLE_FAVOURITES, values)
        // Refresh, if a web client is connected
        if (!requestFromRealtimeSyncService) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun isFavouriteExisting(chipId: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT sensor_id FROM $TABLE_FAVOURITES WHERE sensor_id = '$chipId'", null)
        val count = cursor.count
        cursor.close()
        return count > 0
    }

    fun updateFavourite(newSensor: Sensor, requestFromRealtimeSyncService: Boolean) {
        execSQL("UPDATE " + TABLE_FAVOURITES + " SET sensor_name = '" + newSensor.name + "', sensor_color = '" + newSensor.color + "' WHERE sensor_id = '" + newSensor.chipID + "';")
        // Refresh, if a web client is connected
        if (!requestFromRealtimeSyncService) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    fun removeFavourite(chipId: String, requestFromRealtimeSyncService: Boolean) {
        val db = writableDatabase
        db.delete(TABLE_FAVOURITES, "sensor_id = ?", arrayOf(chipId))
        // Refresh, if a web client is connected
        if (!requestFromRealtimeSyncService) WebRealtimeSyncService.own_instance?.refresh(context)
    }

    //---------------------------------------Externe Sensoren---------------------------------------

    fun addAllExternalSensors(sensors: ArrayList<ExternalSensor>) {
        val db = writableDatabase
        db.beginTransaction()
        // Create table if it doesn't already exist
        db.compileStatement("CREATE TABLE IF NOT EXISTS $TABLE_EXTERNAL_SENSORS (sensor_id text PRIMARY KEY, latitude double, longitude double);").execute()
        // Write records into db
        val stmt = db.compileStatement("REPLACE INTO $TABLE_EXTERNAL_SENSORS (sensor_id, latitude, longitude) VALUES (?, ?, ?);")
        sensors.forEach {
            try {
                stmt.bindString(1, it.chipId)
                stmt.bindDouble(2, it.lat)
                stmt.bindDouble(3, it.lng)
                stmt.execute()
            } catch (ignored: SQLiteConstraintException) {ignored.printStackTrace()} finally {
                stmt.clearBindings()
            }
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()
    }

    fun clearExternalSensors() {
        execSQL("DELETE FROM $TABLE_EXTERNAL_SENSORS")
    }

    fun deleteExternalSensor(chip_id: String) {
        execSQL("DELETE FROM $TABLE_EXTERNAL_SENSORS WHERE sensor_id = '$chip_id'")
    }

    //------------------------------------------Messdaten-------------------------------------------

    internal fun saveRecords(chipId: String, records: ArrayList<DataRecord>) {
        val db = writableDatabase
        db.beginTransaction()
        // Create table if it doesn't already exist
        db.compileStatement("CREATE TABLE IF NOT EXISTS data_$chipId (time integer PRIMARY KEY, pm2_5 double, pm10 double, temp double, humidity double, pressure double, gps_lat double, gps_lng double, gps_alt double, note text);").execute()
        // Write records into db
        val stmt = db.compileStatement("INSERT INTO data_$chipId (time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")
        records.forEach {
            try {
                stmt.bindLong(1, it.dateTime.time / 1000)
                stmt.bindDouble(2, it.p2)
                stmt.bindDouble(3, it.p1)
                stmt.bindDouble(4, it.temp)
                stmt.bindDouble(5, it.humidity)
                stmt.bindDouble(6, it.pressure)
                stmt.bindDouble(7, it.lat)
                stmt.bindDouble(8, it.lng)
                stmt.bindDouble(9, it.alt)
                stmt.execute()
            } catch (ignored: SQLiteConstraintException) {} finally {
                stmt.clearBindings()
            }
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()
    }

    fun loadRecords(chipId: String, from: Long, to: Long): ArrayList<DataRecord> {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt FROM data_" + chipId + " WHERE time >= " + from / 1000 + " AND time < " + to / 1000, null)
            val records = ArrayList<DataRecord>()
            while (cursor.moveToNext()) {
                val time = Date()
                time.time = cursor.getLong(0) * 1000
                records.add(DataRecord(
                    time,
                    cursor.getDouble(2),
                    cursor.getDouble(1),
                    cursor.getDouble(3),
                    cursor.getDouble(4),
                    cursor.getDouble(5),
                    cursor.getDouble(6),
                    cursor.getDouble(7),
                    cursor.getDouble(8)
                ))
            }
            cursor.close()
            return records
        } catch (ignored: Exception) {}
        return ArrayList()
    }

    fun getLastRecord(chipId: String): DataRecord? {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT time, pm2_5, pm10, temp, humidity, pressure, gps_lat, gps_lng, gps_alt FROM data_$chipId ORDER BY time DESC LIMIT 1", null)
            cursor.moveToNext()
            val time = Date()
            time.time = cursor.getLong(0) * 1000
            val record = DataRecord(
                time,
                cursor.getDouble(2),
                cursor.getDouble(1),
                cursor.getDouble(3),
                cursor.getDouble(4),
                cursor.getDouble(5),
                cursor.getDouble(6),
                cursor.getDouble(7),
                cursor.getDouble(8)
            )
            cursor.close()
            return record
        } catch (ignored: Exception) {}
        return null
    }

    fun shareImage(image: Bitmap, shareMessage: String) {
        try {
            // Save
            val out = context.openFileOutput("export.png", Context.MODE_PRIVATE)
            image.compress(Bitmap.CompressFormat.PNG, 80, out)
            out.close()
            val uri = FileProvider.getUriForFile(context, "com.chillibits.pmapp", context.getFileStreamPath("export.png"))
            // Share
            val i = Intent(Intent.ACTION_SEND)
            i.type = URLConnection.guessContentTypeFromName(uri.path)
            i.putExtra(Intent.EXTRA_STREAM, uri)
            context.startActivity(Intent.createChooser(i, shareMessage))
        } catch (ignored: IOException) {}
    }

    fun exportDataRecords(records: ArrayList<DataRecord>): Uri? {
        try {
            val out = context.openFileOutput("export.csv", Context.MODE_PRIVATE)
            val sdfDatetime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            out.write("Time;PM10;PM2.5;Temperature;Humidity;Pressure;Latitude;Longitude;Altitude\n".toByteArray())
            records.forEach {
                val time = sdfDatetime.format(it.dateTime.time)
                val p1 = it.p1.toString()
                val p2 = it.p2.toString()
                val temp = it.temp.toString()
                val humidity = it.humidity.toString()
                val pressure = it.pressure.toString()
                val gpsLat = it.lat.toString()
                val gpsLng = it.lng.toString()
                val gpsAlt = it.alt.toString()
                out.write("$time;$p1;$p2;$temp;$humidity;$pressure;$gpsLat;$gpsLng;$gpsAlt\n".toByteArray())
            }
            out.close()
            return FileProvider.getUriForFile(context, "com.chillibits.pmapp", context.getFileStreamPath("export.csv"))
        } catch (ignored: Exception) {}
        return null
    }

    fun deleteDataDatabase(chipId: String) {
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS data_$chipId")
    }

    fun deleteAllDataDatabases() {
        val cursor = writableDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'data_%'", null)
        while (cursor.moveToNext()) {
            writableDatabase.execSQL("DROP TABLE " + cursor.getString(0))
            Log.i(Constants.TAG, "Deleted database: " + cursor.getString(0))
        }
        cursor.close()
    }

    fun importXMLFile(path: String): Boolean {
        return try {
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
                s.chipID = parser.getAttributeValue(null, "id")
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
                s.chipID = parser.getAttributeValue(null, "id")
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
            favourites.forEach {
                if (!isSensorExisting(it.chipID)) addFavourite(it, false)
            }
            ownSensors.forEach {
                if (!isSensorExisting(it.chipID)) addOwnSensor(it, offline = true, requestFromRealtimeSyncService = false)
            }
            Log.i(Constants.TAG, "Imported ${favourites.size} favourites and ${ownSensors.size} own sensors ")

            inputStream.close()
            true
        } catch (e: Exception) {
            Toast.makeText(context, R.string.error_try_again, Toast.LENGTH_SHORT).show()
            false
        }
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
            favourites.forEach {
                serializer.startTag("", "sensor")
                serializer.attribute("", "id", it.chipID)
                serializer.attribute("", "name", it.name)
                serializer.attribute("", "color", it.color.toString())
                serializer.endTag("", "sensor")
            }
            serializer.endTag("", "favourites")
            // Own sensors
            serializer.startTag("", "own-sensors")
            ownSensors.forEach {
                serializer.startTag("", "sensor")
                serializer.attribute("", "id", it.chipID)
                serializer.attribute("", "name", it.name)
                serializer.attribute("", "color", it.color.toString())
                serializer.endTag("", "sensor")
            }
            serializer.endTag("", "own-sensors")
            serializer.endTag("", "sensor-configuration")
            serializer.endDocument()
            // Write to file
            val out = context.openFileOutput("sensor_config.xml", Context.MODE_PRIVATE)
            out.write(writer.toString().toByteArray())
            out.close()
            val uri = FileProvider.getUriForFile(context, "com.chillibits.pmapp", context.getFileStreamPath("sensor_config.xml"))
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