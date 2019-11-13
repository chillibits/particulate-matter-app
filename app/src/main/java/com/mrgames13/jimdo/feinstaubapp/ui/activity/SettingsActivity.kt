/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.activity

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.*
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.service.SyncJobService
import com.mrgames13.jimdo.feinstaubapp.service.SyncService
import com.mrgames13.jimdo.feinstaubapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.tool.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.ui.view.ProgressDialog
import org.json.JSONArray
import java.util.*

class SettingsActivity : PreferenceActivity() {

    // Utils packages
    private lateinit var su: StorageUtils
    private lateinit var smu: ServerMessagingUtils

    // Variables
    private var result: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize StorageUtils
        su = StorageUtils(this)

        // Initialize ServerMessagingUtils
        smu = ServerMessagingUtils(this, su)

        // Initialize toolbar
        val root = findViewById<View>(android.R.id.list).parent.parent.parent as LinearLayout
        val toolbar = LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false) as Toolbar
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        toolbar.title = getString(R.string.settings)
        val upArrow = resources.getDrawable(R.drawable.arrow_back)
        upArrow.setColorFilter(ContextCompat.getColor(this, R.color.white), PorterDuff.Mode.SRC_ATOP)
        toolbar.navigationIcon = upArrow
        root.addView(toolbar, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.setOnApplyWindowInsetsListener { v, insets ->
                listView.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
                toolbar.setPadding(0, insets.systemWindowInsetTop, 0, 0)
                insets
            }
        } else {
            val state = Integer.parseInt(su.getString("app_theme", "0"))
            AppCompatDelegate.setDefaultNightMode(if (state == 0) AppCompatDelegate.MODE_NIGHT_AUTO_TIME else if (state == 1) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES)
        }

        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        setupPreferencesScreen()
    }

    private fun setupPreferencesScreen() {
        addPreferencesFromResource(R.xml.pref_main)

        val syncCycle = findPreference("sync_cycle") as EditTextPreference
        syncCycle.summary = su.getString("sync_cycle", Constants.DEFAULT_SYNC_CYCLE.toString()) + " " + if (Integer.parseInt(su.getString("sync_cycle", Constants.DEFAULT_SYNC_CYCLE.toString())) == 1) getString(R.string.second) else getString(R.string.seconds)
        syncCycle.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            if (o.toString() == "" || Integer.parseInt(o.toString()) < Constants.MIN_SYNC_CYCLE) return@OnPreferenceChangeListener false
            preference.summary = o.toString() + " " + if (Integer.parseInt(o.toString()) == 1) getString(R.string.second) else getString(R.string.seconds)
            true
        }

        val syncCycleBackground = findPreference("sync_cycle_background") as EditTextPreference
        syncCycleBackground.summary = su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString()) + " " + if (Integer.parseInt(su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) == 1) getString(R.string.minute) else getString(R.string.minutes)
        syncCycleBackground.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            if (o.toString() == "" || Integer.parseInt(o.toString()) < Constants.MIN_SYNC_CYCLE_BACKGROUND) return@OnPreferenceChangeListener false
            preference.summary = o.toString() + " " + if (Integer.parseInt(o.toString()) == 1) getString(R.string.minute) else getString(R.string.minutes)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Update JobScheduler
                val backgroundSyncFrequency = Integer.parseInt(su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) * 1000 * 60
                val component = ComponentName(this@SettingsActivity, SyncJobService::class.java)
                val info = JobInfo.Builder(Constants.JOB_SYNC_ID, component)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPeriodic(backgroundSyncFrequency.toLong())
                        .setPersisted(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.setRequiresBatteryNotLow(true)
                val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                Log.d("FA", if (scheduler.schedule(info.build()) == JobScheduler.RESULT_SUCCESS) "Job scheduled successfully" else "Job schedule failed")
            } else {
                // Update AlarmManager
                val backgroundSyncFrequency = Integer.parseInt(su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) * 1000 * 60
                val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val startServiceIntent = Intent(this@SettingsActivity, SyncService::class.java)
                val startServicePendingIntent = PendingIntent.getService(this@SettingsActivity, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, startServiceIntent, 0)
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, backgroundSyncFrequency.toLong(), startServicePendingIntent)

                startService(startServiceIntent)
            }

            true
        }

        val appTheme = findPreference("app_theme") as ListPreference
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val category = findPreference("appearance_settings") as PreferenceCategory
            preferenceScreen.removePreference(category)
        } else {
            appTheme.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
                val d = AlertDialog.Builder(this@SettingsActivity)
                        .setTitle(R.string.app_restart_t)
                        .setMessage(R.string.app_restart_m)
                        .setCancelable(true)
                        .setNegativeButton(R.string.later, null)
                        .setPositiveButton(R.string.now) { _, _ ->
                            val intent = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
                            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                        }
                        .create()
                d.show()
                true
            }
        }

        val limitP1 = findPreference("limit_p1") as EditTextPreference
        val limitP2 = findPreference("limit_p2") as EditTextPreference
        val limitTemp = findPreference("limit_temp") as EditTextPreference
        val limitHumidity = findPreference("limit_humidity") as EditTextPreference
        val limitPressure = findPreference("limit_pressure") as EditTextPreference

        limitP1.summary = if (Integer.parseInt(su.getString("limit_p1", Constants.DEFAULT_P1_LIMIT.toString())) > 0) su.getString("limit_p1", Constants.DEFAULT_P1_LIMIT.toString()) + " µg/m³" else getString(R.string.pref_limit_disabled)
        limitP1.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            if (o.toString() == "" || Integer.parseInt(o.toString()) <= 0) {
                preference.summary = getString(R.string.pref_limit_disabled)
                su.putString("limit_p1", "0")
                false
            } else {
                preference.summary = "$o µg/m³"
                true
            }
        }

        limitP2.summary = if (Integer.parseInt(su.getString("limit_p2", Constants.DEFAULT_P2_LIMIT.toString())) > 0) su.getString("limit_p2", Constants.DEFAULT_P2_LIMIT.toString()) + " µg/m³" else getString(R.string.pref_limit_disabled)
        limitP2.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            if (o.toString() == "" || Integer.parseInt(o.toString()) <= 0) {
                preference.summary = getString(R.string.pref_limit_disabled)
                su.putString("limit_p2", "0")
                false
            } else {
                preference.summary = "$o µg/m³"
                true
            }
        }


        limitTemp.summary = if (Integer.parseInt(su.getString("limit_temp", Constants.DEFAULT_TEMP_LIMIT.toString())) > 0) su.getString("limit_temp", Constants.DEFAULT_TEMP_LIMIT.toString()) + "°C" else getString(R.string.pref_limit_disabled)
        limitTemp.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            if (o.toString() == "" || Integer.parseInt(o.toString()) <= 0) {
                preference.summary = getString(R.string.pref_limit_disabled)
                su.putString("limit_temp", "0")
                false
            } else {
                preference.summary = "$o °C"
                true
            }
        }

        limitHumidity.summary = if (Integer.parseInt(su.getString("limit_humidity", Constants.DEFAULT_HUMIDITY_LIMIT.toString())) > 0) su.getString("limit_humidity", Constants.DEFAULT_HUMIDITY_LIMIT.toString()) + "%" else getString(R.string.pref_limit_disabled)
        limitHumidity.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            if (o.toString() == "" || Integer.parseInt(o.toString()) <= 0) {
                preference.summary = getString(R.string.pref_limit_disabled)
                su.putString("limit_temp", "0")
                false
            } else {
                preference.summary = "$o%"
                true
            }
        }

        limitPressure.summary = if (Integer.parseInt(su.getString("limit_pressure", Constants.DEFAULT_PRESSURE_LIMIT.toString())) > 0) su.getString("limit_humidity", Constants.DEFAULT_PRESSURE_LIMIT.toString()) + " hPa" else getString(R.string.pref_limit_disabled)
        limitPressure.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            if (o.toString() == "" || Integer.parseInt(o.toString()) <= 0) {
                preference.summary = getString(R.string.pref_limit_disabled)
                su.putString("limit_pressure", "0")
                false
            } else {
                preference.summary = o.toString() + "hPa"
                true
            }
        }

        val enableMarkerClustering = findPreference("enable_marker_clustering") as SwitchPreference
        enableMarkerClustering.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            val d = AlertDialog.Builder(this@SettingsActivity)
                    .setTitle(R.string.app_restart_t)
                    .setMessage(R.string.app_restart_m)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialogInterface, i ->
                        val intent = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
                        intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                    }
                    .create()
            d.show()
            true
        }

        val notificationBreakdown = findPreference("notification_breakdown")
        val notificationBreakdownNumber = findPreference("notification_breakdown_number") as EditTextPreference
        notificationBreakdown.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, enabled ->
            notificationBreakdownNumber.isEnabled = enabled as Boolean
            true
        }
        notificationBreakdownNumber.isEnabled = su.getBoolean("notification_breakdown", true)
        notificationBreakdownNumber.summary = su.getString("notification_breakdown_number", Constants.DEFAULT_MISSING_MEASUREMENT_NUMBER.toString() + " " + if (Integer.parseInt(su.getString("notification_breakdown_number", Constants.DEFAULT_MISSING_MEASUREMENT_NUMBER.toString())) > 1) getString(R.string.measurements) else getString(R.string.measurement))
        notificationBreakdownNumber.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, o ->
            notificationBreakdownNumber.summary = o.toString() + " " + if (Integer.parseInt(o.toString()) > 1) getString(R.string.measurements) else getString(R.string.measurement)
            true
        }

        val clearSensorData = findPreference("clear_sensor_data")
        clearSensorData.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val d = AlertDialog.Builder(this@SettingsActivity)
                    .setCancelable(true)
                    .setTitle(R.string.clear_sensor_data_t)
                    .setMessage(R.string.clear_sensor_data_m)
                    .setIcon(R.drawable.delete_red)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.yes) { dialogInterface, i ->
                        // Delete sensor data
                        /*val pd = ProgressDialog(this@SettingsActivity)
                        pd.setMessage(getString(R.string.please_wait_))
                        pd.show()*/
                        val pd = ProgressDialog(this@SettingsActivity).setMessage(R.string.please_wait_).show()
                        Thread(Runnable {
                            su.deleteAllDataDatabases()
                            su.clearSensorDataMetadata()
                            runOnUiThread { pd.dismiss() }
                        }).start()
                    }
                    .create()
            d.show()
            true
        }

        val aboutServerInfo = findPreference("about_serverinfo")
        aboutServerInfo.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (smu.isInternetAvailable) {
                Thread(Runnable { getServerInfo(showProgressDialog = true, showResultDialog = true) }).start()
            } else {
                Toast.makeText(this@SettingsActivity, getString(R.string.internet_is_not_available), Toast.LENGTH_SHORT).show()
            }
            true
        }
        if (smu.isInternetAvailable) {
            Thread(Runnable {
                try {
                    // Get info from server
                    getServerInfo(showProgressDialog = false, showResultDialog = false)
                    // Extract result
                    if (result!!.isNotEmpty()) {
                        val array = JSONArray(result)
                        val jsonobject = array.getJSONObject(0)
                        val serverStateInt = jsonobject.getInt("serverstate")

                        // Override server state
                        var serverState = ""
                        if (serverStateInt == 1) serverState = getString(R.string.serverstate_1)
                        if (serverStateInt == 2) serverState = getString(R.string.serverstate_2)
                        if (serverStateInt == 3) serverState = getString(R.string.serverstate_3)
                        if (serverStateInt == 4) serverState = getString(R.string.serverstate_4)
                        val summary = serverState
                        runOnUiThread { aboutServerInfo.summary = summary }
                    }
                } catch (ignored: Exception) {
                }
            }).start()
        } else {
            aboutServerInfo.summary = getString(R.string.internet_is_not_available)
        }

        val aboutOpensouces = findPreference("about_opensourcelicenses")
        aboutOpensouces.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val s = SpannableString(getString(R.string.openSourceLicense))
            Linkify.addLinks(s, Linkify.ALL)

            val d = AlertDialog.Builder(this@SettingsActivity)
                    .setTitle(aboutOpensouces.title)
                    .setMessage(Html.fromHtml(s.toString()))
                    .setPositiveButton(getString(R.string.ok)) { dialog, which -> dialog.dismiss() }
                    .create()
            d.show()
            (d.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
            false
        }

        val version = findPreference("about_appversion")
        val pInfo: PackageInfo
        try {
            pInfo = packageManager.getPackageInfo(packageName, 0)
            version.summary = "Version " + pInfo.versionName
        } catch (ignored: PackageManager.NameNotFoundException) {}

        version.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val appPackageName = packageName
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            }

            false
        }

        val developers = findPreference("about_developers")
        developers.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(getString(R.string.link_homepage))
            startActivity(i)
            false
        }

        val moreApps = findPreference("about_moreapps")
        moreApps.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_playstore_developer_site_market))))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.link_playstore_developer_site))))
            }

            false
        }
    }

    override fun onIsMultiPane(): Boolean {
        return false
    }

    private fun getServerInfo(showProgressDialog: Boolean, showResultDialog: Boolean) {
        try {
            // Create ProgressDialog
            val pd = ProgressDialog(this)
                    .setTitle(R.string.pref_serverinfo_t)
                    .setMessage(R.string.pref_serverinfo_downloading_)
            if (showProgressDialog) {
                runOnUiThread {
                    // Show ProgressDialog
                    pd.show()
                }
            }
            // Send request to server
            result = smu.sendRequest(null, object : HashMap<String, String>() {
                init {
                    put("command", "getserverinfo")
                }
            })
            // Extract result
            if (result!!.isNotEmpty()) {
                val array = JSONArray(result)
                val jsonObject = array.getJSONObject(0)
                val clientName = jsonObject.getString("clientname")
                val serverState = jsonObject.getInt("serverstate")
                val minAppVersion = jsonObject.getString("min_appversion")
                val newestAppVersion = jsonObject.getString("newest_appversion")
                val owners = jsonObject.getString("owner")

                // Show dialog to display the result
                if (showResultDialog) {
                    runOnUiThread {
                        if (pd.isShowing()) pd.dismiss()
                        // Override server info
                        var serverStateDisplay: String? = null
                        if (serverState == 1) serverStateDisplay = getString(R.string.server_state) + ": " + getString(R.string.serverstate_1_short)
                        if (serverState == 2) serverStateDisplay = getString(R.string.server_state) + ": " + getString(R.string.serverstate_2_short)
                        if (serverState == 3) serverStateDisplay = getString(R.string.server_state) + ": " + getString(R.string.serverstate_3_short)
                        if (serverState == 4) serverStateDisplay = getString(R.string.server_state) + ": " + getString(R.string.serverstate_4_short)
                        // Concatenate strings
                        val clientNameDisplay = getString(R.string.client_name) + ": " + clientName
                        val minAppVersionDisplay = getString(R.string.min_app_version) + ": " + minAppVersion
                        val newestAppVersionDisplay = getString(R.string.newest_app_version) + ": " + newestAppVersion
                        val ownersDisplay = getString(R.string.owners) + ": " + owners
                        // Concatenate strings and display dialog
                        val info = SpannableString(clientNameDisplay + "\n" + serverStateDisplay + "\n" + minAppVersionDisplay + "\n" + newestAppVersionDisplay + "\n" + ownersDisplay)
                        Linkify.addLinks(info, Linkify.WEB_URLS)
                        val dResult: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity)
                        dResult.setTitle(getString(R.string.pref_serverinfo_t))
                                .setMessage(info)
                                .setPositiveButton(getString(R.string.ok)) { dialog, which -> dialog.dismiss() }
                                .create()
                        val d = dResult.show()
                        (d.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            }
        } catch (ignored: Exception) {}
    }
}