/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.preference.*
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.model.ServerInfo
import com.mrgames13.jimdo.feinstaubapp.network.ServerMessagingUtils
import com.mrgames13.jimdo.feinstaubapp.network.loadServerInfo
import com.mrgames13.jimdo.feinstaubapp.service.SyncJobService
import com.mrgames13.jimdo.feinstaubapp.service.SyncService
import com.mrgames13.jimdo.feinstaubapp.tool.Constants
import com.mrgames13.jimdo.feinstaubapp.tool.StorageUtils
import com.mrgames13.jimdo.feinstaubapp.ui.view.ProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {

    // Variables as objects
    private lateinit var smu: ServerMessagingUtils

    @UnstableDefault
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref, rootKey)
        val activity = requireActivity()

        // Initialiize StorageUtils
        val su = StorageUtils(activity)

        smu = ServerMessagingUtils(activity)

        // SyncCycle
        val syncCycle = findPreference<EditTextPreference>("sync_cycle")
        syncCycle?.setOnBindEditTextListener { editText -> applyEditTextAttributes(editText, 5) }
        syncCycle?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            String.format(getString(R.string._seconds), preference.text)
        }
        syncCycle?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty() && newValue.toString().toInt() >= Constants.MIN_SYNC_CYCLE
        }

        // SyncCycleBackground
        val syncCycleBackground = findPreference<EditTextPreference>("sync_cycle_background")
        syncCycleBackground?.setOnBindEditTextListener { editText -> applyEditTextAttributes(editText, 5) }
        syncCycleBackground?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            String.format(getString(R.string._minutes), preference.text)
        }
        syncCycleBackground?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue.toString().isNotEmpty() && Integer.parseInt(newValue.toString()) >= Constants.MIN_SYNC_CYCLE_BACKGROUND) {
                // Restart Background service with new configuration
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Update JobScheduler
                    val backgroundSyncFrequency = Integer.parseInt(su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) * 1000 * 60
                    val component = ComponentName(activity, SyncJobService::class.java)
                    val info = JobInfo.Builder(Constants.JOB_SYNC_ID, component)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setPeriodic(backgroundSyncFrequency.toLong())
                            .setPersisted(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.setRequiresBatteryNotLow(true)
                    val scheduler = activity.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    Log.i("FA", if (scheduler.schedule(info.build()) == JobScheduler.RESULT_SUCCESS) "Job scheduled successfully" else "Job schedule failed")
                } else {
                    // Update AlarmManager
                    val backgroundSyncFrequency = Integer.parseInt(su.getString("sync_cycle_background", Constants.DEFAULT_SYNC_CYCLE_BACKGROUND.toString())) * 1000 * 60
                    val am = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val startServiceIntent = Intent(activity, SyncService::class.java)
                    val startServicePendingIntent = PendingIntent.getService(activity, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, startServiceIntent, 0)
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = System.currentTimeMillis()
                    am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, backgroundSyncFrequency.toLong(), startServicePendingIntent)
                    activity.startService(startServiceIntent)
                }
                true
            } else {
                false
            }
        }

        // AppTheme
        val appTheme = findPreference<ListPreference>("app_theme")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            preferenceScreen.removePreference(appTheme)
        } else {
            appTheme?.setOnPreferenceChangeListener { _, _ ->
                restartApp(activity)
                true
            }
        }

        // LimitP1
        val limitP1 = findPreference<EditTextPreference>("limit_p1")
        limitP1?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 4)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitP1?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            "${preference.text} µg/m³"
        }
        limitP1?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitP2
        val limitP2 = findPreference<EditTextPreference>("limit_p2")
        limitP2?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 4)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitP2?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            "${preference.text} µg/m³"
        }
        limitP2?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitTemperature
        val limitTemp = findPreference<EditTextPreference>("limit_temp")
        limitTemp?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 2)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitTemp?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            "${preference.text} °C"
        }
        limitTemp?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitHumidity
        val limitHumidity = findPreference<EditTextPreference>("limit_humidity")
        limitHumidity?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 2)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitHumidity?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            "${preference.text} %"
        }
        limitHumidity?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // LimitPressure
        val limitPressure = findPreference<EditTextPreference>("limit_pressure")
        limitPressure?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 6)
            editText.hint = getString(R.string.zero_to_disable)
        }
        limitPressure?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            "${preference.text} hPa"
        }
        limitPressure?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty()
        }

        // NotificationBreakdownNumber
        val notificationBreakdownNumber = findPreference<EditTextPreference>("notification_breakdown_number")
        notificationBreakdownNumber?.setOnBindEditTextListener { editText ->
            applyEditTextAttributes(editText, 3)
            editText.hint = getString(R.string.zero_to_disable)
        }
        notificationBreakdownNumber?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            String.format(getString(if(preference.text.toString().toInt() == 1) R.string.measurement else R.string._measurements), preference.text)
        }

        // EnableMarkerClustering
        val enableMarkerClustering = findPreference<SwitchPreferenceCompat>("enable_marker_clustering")
        enableMarkerClustering?.setOnPreferenceChangeListener { _, _ ->
            restartApp(activity)
            true
        }

        // ClearSensorData
        val clearSensorData = findPreference<Preference>("clear_sensor_data")
        clearSensorData?.setOnPreferenceClickListener {
            AlertDialog.Builder(activity)
                    .setCancelable(true)
                    .setTitle(R.string.clear_sensor_data_t)
                    .setMessage(R.string.clear_sensor_data_m)
                    .setIcon(R.drawable.delete_red)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        val pd = ProgressDialog(activity).setMessage(R.string.please_wait_).show()
                        CoroutineScope(Dispatchers.IO).launch {
                            su.deleteAllDataDatabases()
                            su.clearSensorDataMetadata()
                            activity.runOnUiThread { pd.dismiss() }
                        }
                    }
                    .show()
            true
        }

        // ServerInfo
        val serverInfo = findPreference<Preference>("server_info")
        serverInfo?.summaryProvider = Preference.SummaryProvider<Preference> { preference ->
            if(smu.isInternetAvailable) preference.extras.getString("summary") else getString(R.string.internet_is_not_available)
        }
        serverInfo?.setOnPreferenceClickListener {
            if (smu.isInternetAvailable) {
                // Create ProgressDialog
                val pd = ProgressDialog(requireContext())
                        .setTitle(R.string.pref_server_info_t)
                        .setMessage(R.string.pref_server_info_downloading_)
                        .show()
                CoroutineScope(Dispatchers.IO).launch {
                    val result = loadServerInfo(activity)

                    val status = when(result?.serverStatus) {
                        ServerInfo.SERVER_STATUS_ONLINE -> getString(R.string.server_status_online_short)
                        ServerInfo.SERVER_STATUS_OFFLINE -> getString(R.string.server_status_offline_short)
                        ServerInfo.SERVER_STATUS_MAINTENANCE -> getString(R.string.server_status_maintenance_short)
                        ServerInfo.SERVER_STATUS_SUPPORT_ENDED -> getString(R.string.server_status_support_ended_short)
                        else -> ""
                    }

                    val clientName = activity.getString(R.string.client_name_) + " " + result?.clientName
                    val serverStatus = activity.getString(R.string.server_status_) + " " + status
                    val minAppVersion = activity.getString(R.string.min_app_version_) + " " + result?.minAppVersionName
                    val latestAppVersion = activity.getString(R.string.latest_app_version_) + " " + result?.latestAppVersionName
                    val owner = activity.getString(R.string.server_owner_) + " " + result?.serverOwner
                    // Concatenate strings and display dialog
                    val info = SpannableString(clientName+ "\n" + serverStatus + "\n" + minAppVersion + "\n" + latestAppVersion + "\n" + owner)
                    Linkify.addLinks(info, Linkify.WEB_URLS)
                    activity.runOnUiThread {
                        pd.dismiss()
                        val d = AlertDialog.Builder(requireContext())
                                .setTitle(getString(R.string.pref_server_info_t))
                                .setMessage(info)
                                .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                                .show()
                        (d.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
                    }
                }
            } else {
                Toast.makeText(activity, getString(R.string.internet_is_not_available), Toast.LENGTH_SHORT).show()
            }
            true
        }
        if(smu.isInternetAvailable) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = loadServerInfo(requireActivity())
                requireActivity().runOnUiThread {
                    val summary = when(result?.serverStatus) {
                        ServerInfo.SERVER_STATUS_ONLINE -> getString(R.string.server_status_online)
                        ServerInfo.SERVER_STATUS_OFFLINE -> getString(R.string.server_status_offline)
                        ServerInfo.SERVER_STATUS_MAINTENANCE -> getString(R.string.server_status_maintenance)
                        ServerInfo.SERVER_STATUS_SUPPORT_ENDED -> getString(R.string.server_status_support_ended)
                        else -> ""
                    }
                    serverInfo?.extras?.putString("summary", summary)
                }
            }
        }

        val openSourceLicenses = findPreference<Preference>("open_source_licenses")
        openSourceLicenses?.setOnPreferenceClickListener {
            val s = SpannableString(getString(R.string.openSourceLicense))
            Linkify.addLinks(s, Linkify.ALL)

            val d = AlertDialog.Builder(activity)
                    .setTitle(openSourceLicenses.title)
                    .setMessage(HtmlCompat.fromHtml(s.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY))
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ -> dialog.dismiss() }
                    .show()
            (d.findViewById<View>(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
            false
        }

        val openSource = findPreference<Preference>("open_source")
        openSource?.setOnPreferenceClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(getString(R.string.github_url))
            startActivity(i)
            false
        }

        val appVersion = findPreference<Preference>("app_version")
        appVersion?.summaryProvider = Preference.SummaryProvider<Preference> {
            try {
                val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                pInfo.versionName
            } catch (ignored: PackageManager.NameNotFoundException) { "" }
        }
        appVersion?.setOnPreferenceClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${activity.packageName}")))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${activity.packageName}")))
            }
            false
        }

        val developers = findPreference<Preference>("developers")
        developers?.setOnPreferenceClickListener {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(getString(R.string.url_homepage))
            startActivity(i)
            false
        }

        val moreApps = findPreference<Preference>("more_apps")
        moreApps?.setOnPreferenceClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_store_developer_site_market))))
            } catch (anfe: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_store_developer_site))))
            }
            false
        }
    }

    private fun applyEditTextAttributes(editText: EditText, length: Int) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.filters += InputFilter.LengthFilter(length)
        editText.selectAll()
    }
}

fun restartApp(context: Context) {
    AlertDialog.Builder(context)
        .setTitle(R.string.app_restart_t)
        .setMessage(R.string.app_restart_m)
        .setCancelable(false)
        .setPositiveButton(R.string.ok) { _, _ ->
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
        .show()
}