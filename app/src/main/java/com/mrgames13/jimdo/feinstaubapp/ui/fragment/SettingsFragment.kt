/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.mrgames13.jimdo.feinstaubapp.ui.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mrgames13.jimdo.feinstaubapp.R
import com.mrgames13.jimdo.feinstaubapp.shared.Constants
import com.mrgames13.jimdo.feinstaubapp.shared.openGooglePlayAppSite
import com.mrgames13.jimdo.feinstaubapp.shared.openGooglePlayDeveloperSite
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showClearSensorDataDialog
import com.mrgames13.jimdo.feinstaubapp.ui.dialog.showRestartAppDialog
import kotlinx.serialization.UnstableDefault

class SettingsFragment : PreferenceFragmentCompat() {

    @UnstableDefault
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref, rootKey)
        val activity = requireActivity()

        // SyncCycle
        val syncCycle = findPreference<EditTextPreference>(Constants.PREF_SYNC_CYCLE)
        syncCycle?.setOnBindEditTextListener { editText -> applyEditTextAttributes(editText, 5) }
        syncCycle?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            String.format(getString(R.string.seconds), preference.text)
        }
        syncCycle?.setOnPreferenceChangeListener { _, newValue ->
            newValue.toString().isNotEmpty() && newValue.toString().toInt() >= Constants.MIN_SYNC_CYCLE
        }

        // SyncCycleBackground
        val syncCycleBackground = findPreference<EditTextPreference>(Constants.PREF_SYNC_CYCLE_BACKGROUND)
        syncCycleBackground?.setOnBindEditTextListener { editText -> applyEditTextAttributes(editText, 5) }
        syncCycleBackground?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            String.format(getString(R.string.minutes), preference.text)
        }
        syncCycleBackground?.setOnPreferenceChangeListener { _, newValue ->
            rescheduleSyncService(newValue, activity)
        }

        // EnableMarkerClustering
        val enableMarkerClustering = findPreference<SwitchPreferenceCompat>(Constants.PREF_ENABLE_MARKER_CLUSTERING)
        enableMarkerClustering?.setOnPreferenceChangeListener { _, _ ->
            context?.showRestartAppDialog()
            true
        }

        // ClearSensorData
        val clearSensorData = findPreference<Preference>(Constants.PREF_CLEAR_SENSOR_DATA)
        clearSensorData?.setOnPreferenceClickListener {
            context?.showClearSensorDataDialog()
            true
        }

        val openSource = findPreference<Preference>(Constants.PREF_OPEN_SOURCE)
        openSource?.setOnPreferenceClickListener {
            openGitHubPage()
            false
        }

        val openSourceLicenses = findPreference<Preference>(Constants.PREF_OPEN_SOURCE_LICENSES)
        openSourceLicenses?.setOnPreferenceClickListener {
            openLicensesDialog()
            false
        }

        val appVersion = findPreference<Preference>(Constants.PREF_APP_VERSION)
        appVersion?.summaryProvider = Preference.SummaryProvider<Preference> {
            try {
                val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                pInfo.versionName
            } catch (ignored: PackageManager.NameNotFoundException) { "" }
        }
        appVersion?.setOnPreferenceClickListener {
            context?.openGooglePlayAppSite()
            false
        }

        val developers = findPreference<Preference>(Constants.PREF_DEVELOPERS)
        developers?.setOnPreferenceClickListener {
            Intent(Intent.ACTION_VIEW).run {
                data = Uri.parse(getString(R.string.url_homepage))
                startActivity(this)
            }
            false
        }

        val moreApps = findPreference<Preference>(Constants.PREF_MORE_APPS)
        moreApps?.setOnPreferenceClickListener {
            context?.openGooglePlayDeveloperSite()
            false
        }
    }

    private fun rescheduleSyncService(newValue: Any, activity: FragmentActivity): Boolean {
        return newValue.toString().isNotEmpty() && Integer.parseInt(newValue.toString()) >= Constants.MIN_SYNC_CYCLE_BACKGROUND
    }

    private fun openLicensesDialog() {
        LibsBuilder()
            .withActivityTitle(getString(R.string.pref_open_source_t))
            .withAboutAppName(getString(R.string.app_name))
            .withAboutDescription(getString(R.string.app_description))
            .withAboutVersionShownCode(false)
            .withEdgeToEdge(true)
            .withLicenseDialog(true)
            .withLicenseShown(true)
            .start(requireContext())
    }

    private fun openGitHubPage() {
        Intent(Intent.ACTION_VIEW).run {
            data = Uri.parse(getString(R.string.url_github))
            startActivity(this)
        }
    }

    private fun applyEditTextAttributes(editText: EditText, length: Int) {
        editText.inputType = InputType.TYPE_CLASS_NUMBER
        editText.filters += InputFilter.LengthFilter(length)
        editText.selectAll()
    }
}