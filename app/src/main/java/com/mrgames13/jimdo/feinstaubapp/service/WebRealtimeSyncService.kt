/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.google.firebase.database.*
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.Sensor
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.ui.activity.MainActivity
import java.util.*

class WebRealtimeSyncService : Service() {
    internal lateinit var favourites: ArrayList<Sensor>
    internal lateinit var ownSensors: ArrayList<Sensor>

    // Utils packages
    private lateinit var su: StorageUtils
    private var timestamp: Long = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val syncKey = intent.getStringExtra("sync_key")

        // Initialize own instance
        own_instance = this

        // Initialize StorageUtils
        su = StorageUtils(applicationContext)

        // Initialize Firebase
        val db = FirebaseDatabase.getInstance()
        ref = db.getReference("sync/" + syncKey!!)

        refresh(applicationContext)

        return START_NOT_STICKY
    }

    fun refresh(context: Context) {
        timestamp = System.currentTimeMillis()

        // Get favourites and own sensors from local db
        favourites = su.allFavourites
        ownSensors = su.allOwnSensors

        // Assemble data
        val data = HashMap<String, Any>()
        var objectId = 0
        for (s in favourites) {
            val sensorMap = HashMap<String, Any>()
            sensorMap["name"] = s.name
            sensorMap["chip_id"] = s.chipID
            sensorMap["color"] = String.format("#%06X", 0xFFFFFF and s.color)
            sensorMap["fav"] = true
            data[objectId.toString()] = sensorMap
            objectId++
        }
        for (s in ownSensors) {
            val sensorMap = HashMap<String, Any>()
            sensorMap["name"] = s.name
            sensorMap["chip_id"] = s.chipID
            sensorMap["color"] = String.format("#%06X", 0xFFFFFF and s.color)
            sensorMap["fav"] = false
            data[objectId.toString()] = sensorMap
            objectId++
        }

        // Build connection
        val connection = HashMap<String, Any>()
        connection["time"] = timestamp
        connection["device"] = "app"
        connection["data"] = data
        ref.setValue(connection)

        // Set DateChangeListener
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                if (snap.exists()) {
                    if (snap.child("device").value != null && snap.child("device").value != "app") {
                        val newSensors = snap.child("data").value as ArrayList<*>?
                        if (newSensors != null && newSensors.size > 0) {
                            favourites = su.allFavourites
                            ownSensors = su.allOwnSensors
                            for (s in favourites) su.removeFavourite(s.chipID, true)
                            for (s in ownSensors) su.removeOwnSensor(s.chipID, true)
                            for (i in newSensors.indices) {
                                val sensor = newSensors[i] as Map<*, *>
                                // Extract data
                                val chipId = sensor["chip_id"].toString()
                                val name = sensor["name"].toString()
                                val favorized = java.lang.Boolean.parseBoolean(sensor["fav"]!!.toString())
                                val color = sensor["color"].toString()
                                if (!su.isFavouriteExisting(chipId) && !su.isSensorExisting(chipId)) {
                                    if (favorized) {
                                        su.addFavourite(Sensor(chipId, name, Color.parseColor(color)), true)
                                    } else {
                                        su.addOwnSensor(Sensor(chipId, name, Color.parseColor(color)), offline = true, request_from_realtime_sync_service = true)
                                    }
                                }
                            }
                            MainActivity.own_instance?.pagerAdapter?.refreshFavourites()
                            MainActivity.own_instance?.pagerAdapter?.refreshMySensors()
                        }
                    }
                } else {
                    ref.removeEventListener(this)
                    // Show toast
                    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val t = Toast(context)
                    t.setGravity(Gravity.CENTER, 0, 0)
                    t.duration = Toast.LENGTH_LONG
                    t.view = inflater.inflate(R.layout.sync_ended, null)
                    t.show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(context, "Sync failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun stop() {
        ref.removeValue()
        stopSelf()
    }

    companion object {
        // Variables as objects
        private lateinit var ref: DatabaseReference

        // Variables
        var own_instance: WebRealtimeSyncService? = null
    }
}
