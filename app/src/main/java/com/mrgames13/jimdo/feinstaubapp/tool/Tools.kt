/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.tool

import android.content.Context
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import com.mrgames13.jimdo.feinstaubapp.model.DataRecord
import com.mrgames13.jimdo.feinstaubapp.model.Sensor
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.NoSuchAlgorithmException
import kotlin.math.abs

object Tools {

    fun round(value: Double, places: Int): Double {
        try {
            require(places >= 0)
            var bd = BigDecimal(value)
            bd = bd.setScale(places, RoundingMode.HALF_UP)
            return bd.toDouble()
        } catch (ignored: Exception) {}
        return value
    }

    fun md5(s: String): String {
        try {
            // Create hash
            val digest = java.security.MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create hex string
            val hexString = StringBuilder()
            for (aMessageDigest in messageDigest) {
                var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (ignored: NoSuchAlgorithmException) {}
        return ""
    }

    fun calculateMedian(records: ArrayList<Double>): Double {
        if (records.size == 0) return 0.0
        records.sort()
        return records[records.size / 2]
    }

    fun fitArrayList(su: StorageUtils, records: ArrayList<DataRecord>): ArrayList<DataRecord> {
        if (!su.getBoolean("increase_diagram_performance", Constants.DEFAULT_FIT_ARRAY_LIST_ENABLED)) return records
        val divider = records.size / Constants.DEFAULT_FIT_ARRAY_LIST_CONSTANT
        if (divider == 0) return records
        val newRecords = ArrayList<DataRecord>()
        var i = 0
        while (i < records.size) {
            newRecords.add(records[i])
            i += divider + 1
        }
        return newRecords
    }

    fun removeDuplicateSensors(sensors: ArrayList<Sensor>): ArrayList<Sensor> {
        val sensorsNew = ArrayList<Sensor>()
        outerloop@ for (s_new in sensors) {
            for (s_old in sensorsNew) {
                if (s_old.chipID == s_new.chipID) continue@outerloop
            }
            sensorsNew.add(s_new)
        }
        return sensorsNew
    }

    fun getLocationFromAddress(context: Context, strAddress: String): LatLng? {
        val coder = Geocoder(context)
        val address: List<Address>?

        try {
            address = coder.getFromLocationName(strAddress, 5)
            if (address == null) return null
            val location = address[0]
            return LatLng(location.latitude, location.longitude)
        } catch (ignored: Exception) {}
        return null
    }

    fun measurementCorrection1(records: ArrayList<DataRecord>): ArrayList<DataRecord> {
        for (i in 1 until records.size) {
            // Get current and previous record
            val currentRecord = records[i]
            val recordBefore = records[i - 1]
            // Detect zero-values of particulate matter
            if (currentRecord.p1 == 0.0 && currentRecord.p2 == 0.0) {
                // Get next non-zero record
                var recordAfter = recordBefore
                for (j in i + 1 until records.size) {
                    if (!(records[j].temp == 0.0 && records[j].humidity == 0.0 || records[j].temp == 0.0 && records[j].pressure == 0.0 || records[j].humidity == 0.0 && records[j].pressure == 0.0)) {
                        recordAfter = records[j]
                        break
                    }
                }
                // Calculate average values
                // PM10
                var m = abs(recordBefore.p1 - recordAfter.p1) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                var b = recordBefore.p1 - m * recordBefore.dateTime.time
                val avgP1 = round(m * currentRecord.dateTime.time + b, 2)
                // PM2.5
                m = abs(recordBefore.p2 - recordAfter.p2) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                b = recordBefore.p2 - m * recordBefore.dateTime.time
                val avgP2 = round(m * currentRecord.dateTime.time + b, 2)

                // Alter record according to results
                val newRecord = DataRecord(currentRecord.dateTime, avgP1, avgP2, currentRecord.temp, currentRecord.humidity, currentRecord.pressure, 0.0, 0.0, 0.0)
                records[i] = newRecord
            }
            // Detect zero-values of temperature, humidity or pressure
            if (currentRecord.temp == 0.0 && currentRecord.humidity == 0.0 || currentRecord.temp == 0.0 && currentRecord.pressure == 0.0 || currentRecord.humidity == 0.0 && currentRecord.pressure == 0.0) {
                // Get next non-zero record
                var recordAfter = recordBefore
                for (j in i + 1 until records.size) {
                    if (!(records[j].temp == 0.0 && records[j].humidity == 0.0 || records[j].temp == 0.0 && records[j].pressure == 0.0 || records[j].humidity == 0.0 && records[j].pressure == 0.0)) {
                        recordAfter = records[j]
                        break
                    }
                }
                // Calculate average values
                // Temperature
                var m = abs(recordBefore.temp - recordAfter.temp) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                var b = recordBefore.temp - m * recordBefore.dateTime.time
                val avgTemp = round(m * currentRecord.dateTime.time + b, 2)
                // Humidity
                m = abs(recordBefore.humidity - recordAfter.humidity) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                b = recordBefore.humidity - m * recordBefore.dateTime.time
                val avgHumidity = round(m * currentRecord.dateTime.time + b, 2)
                // Pressure
                m = abs(recordBefore.pressure - recordAfter.pressure) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                b = recordBefore.pressure - m * recordBefore.dateTime.time
                val avgPressure = round(m * currentRecord.dateTime.time + b, 2)
                // Alter record according to results
                val newRecord = DataRecord(currentRecord.dateTime, currentRecord.p1, currentRecord.p2, avgTemp, avgHumidity, avgPressure, 0.0, 0.0, 0.0)
                records[i] = newRecord
            }
        }
        return records
    }

    fun measurementCorrection2(records: ArrayList<DataRecord>): ArrayList<DataRecord> {
        /*for(int i = 2; i < records.size(); i++) {
            // Get current and previous record
            DataRecord current_record = records.get(i);
            DataRecord record_before = records.get(i -1);
            DataRecord record_before2 = records.get(i -2);
            // PM10
            double deltaY1 = current_record.getP1() - record_before.getP1();
            double deltaY2 = record_before.getP1() - record_before2.getP1();
            double new_p1 = current_record.getP1();
            double new_p2 = current_record.getP2();
            // Detect measurement errors
            if(current_record.getP1() > 30 && current_record.getP1() > record_before.getP1() * 3 && deltaY1 > deltaY2 * 3) {
                double threshold = (current_record.getP1() + record_before.getP1()) / 2;
                DataRecord record_after = record_before;
                for(int j = i +1; j < records.size(); j++) {
                    if(records.get(j).getP1() < threshold) {
                        record_after = records.get(j);
                        break;
                    }
                }
                // Form linear function
                double m = Math.abs(record_before.getP1() - record_after.getP1()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                double b = record_before.getP1() - m * record_before.getDateTime().getTime();
                new_p1 = round(m * current_record.getDateTime().getTime() + b, 2);
            }
            // PM2.5
            deltaY1 = current_record.getP2() - record_before.getP2();
            deltaY2 = record_before.getP2() - record_before2.getP2();
            if(current_record.getP2() > 20 && current_record.getP2() > current_record.getP2() * 3 && deltaY1 > deltaY2 * 3) {
                double threshold = (current_record.getP2() + record_before.getP2()) / 2;
                DataRecord record_after = record_before;
                for(int j = i +1; j < records.size(); j++) {
                    if(records.get(j).getP2() < threshold) {
                        record_after = records.get(j);
                        break;
                    }
                }
                // Form linear function
                double m = Math.abs(record_before.getP2() - record_after.getP2()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                double b = record_before.getP2() - m * record_before.getDateTime().getTime();
                new_p2 = round(m * current_record.getDateTime().getTime() + b, 2);
            }

            // Alter record according to results
            DataRecord new_record = new DataRecord(current_record.getDateTime(), new_p1, new_p2, current_record.getTemp(), current_record.getHumidity(), current_record.getPressure(), 0.0, 0.0, 0.0);
            records.set(i, new_record);
        }*/
        return records
    }

    fun isMeasurementBreakdown(su: StorageUtils, records: ArrayList<DataRecord>): Boolean {
        val measurementInterval = if (records.size > 2) getMeasurementInteval(records) else 0
        return if (measurementInterval <= 0) false else System.currentTimeMillis() > records[records.size - 1].dateTime.time + measurementInterval * (Integer.parseInt(su.getString("notification_breakdown_number", Constants.DEFAULT_MISSING_MEASUREMENT_NUMBER.toString())) + 1)
    }

    private fun getMeasurementInteval(records: ArrayList<DataRecord>): Long {
        val distances = ArrayList<Long>()
        for (i in 1 until records.size) distances.add(records[i].dateTime.time - records[i - 1].dateTime.time)
        distances.sort()
        return distances[distances.size / 2]
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) result = context.resources.getDimensionPixelSize(resourceId)
        return result
    }

    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) result = context.resources.getDimensionPixelSize(resourceId)
        return result
    }
}