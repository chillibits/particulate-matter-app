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
        return try {
            if(places >= 0) {
                var bd = BigDecimal(value)
                bd = bd.setScale(places, RoundingMode.HALF_UP)
                bd.toDouble()
            } else
                value
        } catch (ignored: Exception) {
            value
        }
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
                var avgTemp = round(m * currentRecord.dateTime.time + b, 2)
                avgTemp = if(avgTemp.isNaN()) 0.0 else avgTemp
                // Humidity
                m = abs(recordBefore.humidity - recordAfter.humidity) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                b = recordBefore.humidity - m * recordBefore.dateTime.time
                var avgHumidity = round(m * currentRecord.dateTime.time + b, 2)
                avgHumidity = if(avgHumidity.isNaN()) 0.0 else avgHumidity
                // Pressure
                m = abs(recordBefore.pressure - recordAfter.pressure) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                b = recordBefore.pressure - m * recordBefore.dateTime.time
                var avgPressure = round(m * currentRecord.dateTime.time + b, 2)
                avgPressure = if(avgPressure.isNaN()) 0.0 else avgPressure
                // Alter record according to results
                val newRecord = DataRecord(currentRecord.dateTime, currentRecord.p1, currentRecord.p2, avgTemp, avgHumidity, avgPressure, 0.0, 0.0, 0.0)
                records[i] = newRecord
            }
        }
        return records
    }

    fun measurementCorrection2(records: ArrayList<DataRecord>): ArrayList<DataRecord> {
        /*for (i in 2 until records.size) {
            // Get current and previous record
            val currentRecord = records[i]
            val recordBefore = records[i - 1]
            val recordBefore2 = records[i - 2]
            // PM10
            var deltaY1 = currentRecord.p1 - recordBefore.p1
            var deltaY2 = recordBefore.p1 - recordBefore2.p1
            var newP1 = currentRecord.p1
            var newP2 = currentRecord.p2
            // Detect measurement errors
            if (currentRecord.p1 > 30 && currentRecord.p1 > recordBefore.p1 * 3 && deltaY1 > deltaY2 * 3) {
                val threshold = (currentRecord.p1 + recordBefore.p1) / 2
                var recordAfter = recordBefore
                for (j in i + 1 until records.size) {
                    if (records[j].p1 < threshold) {
                        recordAfter = records[j]
                        break
                    }
                }
                // Form linear function
                val m = abs(recordBefore.p1 - recordAfter.p1) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                val b = recordBefore.p1 - m * recordBefore.dateTime.time
                newP1 = round(m * currentRecord.dateTime.time + b, 2)
            }
            // PM2.5
            deltaY1 = currentRecord.p2 - recordBefore.p2
            deltaY2 = recordBefore.p2 - recordBefore2.p2
            if (currentRecord.p2 > 20 && currentRecord.p2 > currentRecord.p2 * 3 && deltaY1 > deltaY2 * 3) {
                val threshold = (currentRecord.p2 + recordBefore.p2) / 2
                var recordAfter = recordBefore
                for (j in i + 1 until records.size) {
                    if (records[j].p2 < threshold) {
                        recordAfter = records[j]
                        break
                    }
                }
                // Form linear function
                val m = abs(recordBefore.p2 - recordAfter.p2) / (recordAfter.dateTime.time - recordBefore.dateTime.time)
                val b = recordBefore.p2 - m * recordBefore.dateTime.time
                newP2 = round(m * currentRecord.dateTime.time + b, 2)
            }

            // Alter record according to results
            val newRecord = DataRecord(
                currentRecord.dateTime,
                newP1,
                newP2,
                currentRecord.temp,
                currentRecord.humidity,
                currentRecord.pressure,
                0.0,
                0.0,
                0.0
            )
            records[i] = newRecord
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
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    fun findMaxMeasurement(records: ArrayList<DataRecord>?, mode: Int): Double {
        return try {
            when(mode) {
                1 -> records?.maxBy { it.p1 }!!.p1
                2 -> records?.maxBy { it.p2 }!!.p2
                3 -> records?.maxBy { it.temp }!!.temp
                4 -> records?.maxBy { it.humidity }!!.humidity
                5 -> records?.maxBy { it.pressure }!!.pressure
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    fun findMinMeasurement(records: ArrayList<DataRecord>?, mode: Int): Double {
        return try {
            when(mode) {
                1 -> records?.minBy { it.p1 }!!.p1
                2 -> records?.minBy { it.p2 }!!.p2
                3 -> records?.minBy { it.temp }!!.temp
                4 -> records?.minBy { it.humidity }!!.humidity
                5 -> records?.minBy { it.pressure }!!.pressure
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }
}