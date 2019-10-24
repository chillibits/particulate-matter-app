/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.App;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncJobService;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;

public class SettingsActivity extends PreferenceActivity {

    // Variables as objects
    private Resources res;
    private Toolbar toolbar;
    private ProgressDialog pd;

    // Utils packages
    private StorageUtils su;
    private ServerMessagingUtils smu;

    // Variables
    private String result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize StorageUtils
        su = new StorageUtils(this);

        // Initialize ServerMessagingUtils
        smu = new ServerMessagingUtils(this, su);

        // Initialize toolbar
        final LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
        toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.toolbar_settings, root, false);
        toolbar.setBackgroundColor(res.getColor(R.color.colorPrimary));
        toolbar.setTitleTextColor(res.getColor(R.color.white));
        toolbar.setTitle(res.getString(R.string.settings));
        Drawable upArrow = res.getDrawable(R.drawable.arrow_back);
        upArrow.setColorFilter(res.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        toolbar.setNavigationIcon(upArrow);
        root.addView(toolbar, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            getWindow().getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    getListView().setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
                    toolbar.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
                    return insets;
                }
            });
        } else {
            int state = Integer.parseInt(su.getString("app_theme", "0"));
            AppCompatDelegate.setDefaultNightMode(state == 0 ? AppCompatDelegate.MODE_NIGHT_AUTO_TIME : (state == 1 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES));
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupPreferencesScreen();
    }

    private void setupPreferencesScreen() {
        addPreferencesFromResource(R.xml.pref_main);

        EditTextPreference sync_cycle = (EditTextPreference) findPreference("sync_cycle");
        sync_cycle.setSummary(su.getString("sync_cycle", String.valueOf(Constants.DEFAULT_SYNC_CYCLE)) + " " + (Integer.parseInt(su.getString("sync_cycle", String.valueOf(Constants.DEFAULT_SYNC_CYCLE))) == 1 ? res.getString(R.string.second) : res.getString(R.string.seconds)));
        sync_cycle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) < Constants.MIN_SYNC_CYCLE) return false;
                preference.setSummary(o + " " + (Integer.parseInt(String.valueOf(o)) == 1 ? res.getString(R.string.second) : res.getString(R.string.seconds)));
                return true;
            }
        });

        EditTextPreference sync_cycle_background = (EditTextPreference) findPreference("sync_cycle_background");
        sync_cycle_background.setSummary(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND)) + " " + (Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) == 1 ? res.getString(R.string.minute) : res.getString(R.string.minutes)));
        sync_cycle_background.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) < Constants.MIN_SYNC_CYCLE_BACKGROUND) return false;
                preference.setSummary(o + " " + (Integer.parseInt(String.valueOf(o)) == 1 ? res.getString(R.string.minute) : res.getString(R.string.minutes)));

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    // Update JobScheduler
                    int background_sync_frequency = Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) * 1000 * 60;
                    ComponentName component = new ComponentName(SettingsActivity.this, SyncJobService.class);
                    JobInfo.Builder info = new JobInfo.Builder(Constants.JOB_SYNC_ID, component)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setPeriodic(background_sync_frequency)
                            .setPersisted(true);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) info.setRequiresBatteryNotLow(true);
                    JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                    Log.d("FA", scheduler.schedule(info.build()) == JobScheduler.RESULT_SUCCESS ? "Job scheduled successfully" : "Job schedule failed");
                } else {
                    // Update AlarmManager
                    int background_sync_frequency = Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) * 1000 * 60;
                    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent start_service_intent = new Intent(SettingsActivity.this, SyncService.class);
                    PendingIntent start_service_pending_intent = PendingIntent.getService(SettingsActivity.this, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, start_service_intent, 0);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(System.currentTimeMillis());
                    am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), background_sync_frequency, start_service_pending_intent);

                    startService(start_service_intent);
                }

                return true;
            }
        });

        ListPreference app_theme = (ListPreference) findPreference("app_theme");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PreferenceCategory category = (PreferenceCategory) findPreference("appearance_settings");
            getPreferenceScreen().removePreference(category);
        } else {
            app_theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    AlertDialog d = new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(R.string.app_restart_t)
                            .setMessage(R.string.app_restart_m)
                            .setCancelable(true)
                            .setNegativeButton(R.string.later, null)
                            .setPositiveButton(R.string.now, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                            })
                            .create();
                    d.show();
                    return true;
                }
            });
        }

        EditTextPreference limit_p1 = (EditTextPreference) findPreference("limit_p1");
        EditTextPreference limit_p2 = (EditTextPreference) findPreference("limit_p2");
        EditTextPreference limit_temp = (EditTextPreference) findPreference("limit_temp");
        EditTextPreference limit_humidity = (EditTextPreference) findPreference("limit_humidity");
        EditTextPreference limit_pressure = (EditTextPreference) findPreference("limit_pressure");

        limit_p1.setSummary(Integer.parseInt(su.getString("limit_p1", String.valueOf(Constants.DEFAULT_P1_LIMIT))) > 0 ? su.getString("limit_p1", String.valueOf(Constants.DEFAULT_P1_LIMIT)) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
        limit_p1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) {
                    preference.setSummary(res.getString(R.string.pref_limit_disabled));
                    su.putString("limit_p1", "0");
                    return false;
                } else {
                    preference.setSummary(o + " µg/m³");
                    return true;
                }
            }
        });

        limit_p2.setSummary(Integer.parseInt(su.getString("limit_p2", String.valueOf(Constants.DEFAULT_P2_LIMIT))) > 0 ? su.getString("limit_p2", String.valueOf(Constants.DEFAULT_P2_LIMIT)) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
        limit_p2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) {
                    preference.setSummary(res.getString(R.string.pref_limit_disabled));
                    su.putString("limit_p2", "0");
                    return false;
                } else {
                    preference.setSummary(o + " µg/m³");
                    return true;
                }
            }
        });


        limit_temp.setSummary(Integer.parseInt(su.getString("limit_temp", String.valueOf(Constants.DEFAULT_TEMP_LIMIT))) > 0 ? su.getString("limit_temp", String.valueOf(Constants.DEFAULT_TEMP_LIMIT)) + "°C" : res.getString(R.string.pref_limit_disabled));
        limit_temp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) {
                    preference.setSummary(res.getString(R.string.pref_limit_disabled));
                    su.putString("limit_temp", "0");
                    return false;
                } else {
                    preference.setSummary(o + " °C");
                    return true;
                }
            }
        });

        limit_humidity.setSummary(Integer.parseInt(su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT))) > 0 ? su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT)) + "%" : res.getString(R.string.pref_limit_disabled));
        limit_humidity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) {
                    preference.setSummary(res.getString(R.string.pref_limit_disabled));
                    su.putString("limit_temp", "0");
                    return false;
                } else {
                    preference.setSummary(o + "%");
                    return true;
                }
            }
        });

        limit_pressure.setSummary(Integer.parseInt(su.getString("limit_pressure", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT))) > 0 ? su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT)) + " hPa" : res.getString(R.string.pref_limit_disabled));
        limit_pressure.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) {
                    preference.setSummary(res.getString(R.string.pref_limit_disabled));
                    su.putString("limit_pressure", "0");
                    return false;
                } else {
                    preference.setSummary(o + "hPa");
                    return true;
                }
            }
        });

        final SwitchPreference enable_marker_clustering = (SwitchPreference) findPreference("enable_marker_clustering");
        enable_marker_clustering.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                AlertDialog d = new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(R.string.app_restart_t)
                        .setMessage(R.string.app_restart_m)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }
                        })
                        .create();
                d.show();
                return true;
            }
        });

        final Preference notification_breakdown = findPreference("notification_breakdown");
        final EditTextPreference notification_breakdown_number = (EditTextPreference) findPreference("notification_breakdown_number");
        notification_breakdown.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object enabled) {
                notification_breakdown_number.setEnabled((boolean) enabled);
                return true;
            }
        });
        notification_breakdown_number.setEnabled(su.getBoolean("notification_breakdown", true));
        notification_breakdown_number.setSummary(su.getString("notification_breakdown_number", String.valueOf(Constants.DEFAULT_MISSING_MEASUREMENT_NUMBER)) + " " + (Integer.parseInt(su.getString("notification_breakdown_number", String.valueOf(Constants.DEFAULT_MISSING_MEASUREMENT_NUMBER))) > 1 ? res.getString(R.string.measurements) : res.getString(R.string.measurement)));
        notification_breakdown_number.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                notification_breakdown_number.setSummary(o + " " + (Integer.parseInt(String.valueOf(o)) > 1 ? res.getString(R.string.measurements) : res.getString(R.string.measurement)));
                return true;
            }
        });

        final Preference clear_sensor_data = findPreference("clear_sensor_data");
        clear_sensor_data.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog d = new AlertDialog.Builder(SettingsActivity.this)
                        .setCancelable(true)
                        .setTitle(R.string.clear_sensor_data_t)
                        .setMessage(R.string.clear_sensor_data_m)
                        .setIcon(R.drawable.delete_red)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // Delete sensor data
                                final ProgressDialog pd = new ProgressDialog(SettingsActivity.this);
                                pd.setMessage(res.getString(R.string.please_wait_));
                                pd.show();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        su.deleteAllDataDatabases();
                                        su.clearSensorDataMetadata();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                pd.dismiss();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        })
                        .create();
                d.show();
                return true;
            }
        });

        final Preference about_serverinfo = findPreference("about_serverinfo");
        about_serverinfo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(smu.isInternetAvailable()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            getServerInfo(true, true);
                        }
                    }).start();
                } else {
                    Toast.makeText(SettingsActivity.this, res.getString(R.string.internet_is_not_available), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        if(smu.isInternetAvailable()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        // Get info from server
                        getServerInfo(false, false);
                        // Extract result
                        if(!result.isEmpty()) {
                            JSONArray array = new JSONArray(result);
                            JSONObject jsonobject = array.getJSONObject(0);
                            final int server_state_int = jsonobject.getInt("serverstate");

                            // Override server state
                            String server_state = "";
                            if(server_state_int == 1) server_state = res.getString(R.string.serverstate_1);
                            if(server_state_int == 2) server_state = res.getString(R.string.serverstate_2);
                            if(server_state_int == 3) server_state = res.getString(R.string.serverstate_3);
                            if(server_state_int == 4) server_state = res.getString(R.string.serverstate_4);
                            final String summary = server_state;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    about_serverinfo.setSummary(summary);
                                }
                            });
                        }
                    } catch(Exception ignored) {}
                }
            }).start();
        } else {
            about_serverinfo.setSummary(res.getString(R.string.internet_is_not_available));
        }

        final Preference about_opensouces = findPreference("about_opensourcelicenses");
        about_opensouces.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SpannableString s = new SpannableString(res.getString(R.string.openSourceLicense));
                Linkify.addLinks(s, Linkify.ALL);

                AlertDialog d = new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle(about_opensouces.getTitle())
                        .setMessage(Html.fromHtml(s.toString()))
                        .setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                d.show();
                ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                return false;
            }
        });

        Preference version = findPreference("about_appversion");
        PackageInfo pinfo;
        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version.setSummary("Version " + pinfo.versionName);
        } catch (PackageManager.NameNotFoundException ignored) {}
        version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                return false;
            }
        });

        Preference developers = findPreference("about_developers");
        developers.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(res.getString(R.string.link_homepage)));
                startActivity(i);
                return false;
            }
        });

        Preference more_apps = findPreference("about_moreapps");
        more_apps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(res.getString(R.string.link_playstore_developer_site_market))));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(res.getString(R.string.link_playstore_developer_site))));
                }
                return false;
            }
        });
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }

    private void getServerInfo(final boolean showProgressDialog, final boolean showResultDialog) {
        try {
            if(showProgressDialog) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Show ProgressDialog
                        pd = new ProgressDialog(SettingsActivity.this);
                        pd.setMessage(res.getString(R.string.pref_serverinfo_downloading_));
                        pd.setIndeterminate(true);
                        pd.setTitle(res.getString(R.string.pref_serverinfo_t));
                        pd.show();
                    }
                });
            }
            // Send request to server
            result = smu.sendRequest(null, new HashMap<String, String>() {{
                put("command", "getserverinfo");
            }});
            // Extract result
            if(!result.isEmpty()) {
                JSONArray array = new JSONArray(result);
                JSONObject jsonobject = array.getJSONObject(0);
                final String client_name = jsonobject.getString("clientname");
                final int server_state = jsonobject.getInt("serverstate");
                final String min_appversion = jsonobject.getString("min_appversion");
                final String newest_appversion = jsonobject.getString("newest_appversion");
                final String owners = jsonobject.getString("owner");

                // Show dialog to display the result
                if(showResultDialog) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(showProgressDialog) pd.dismiss();
                            // Override server info
                            String server_state_display = null;
                            if(server_state == 1) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_1_short);
                            if(server_state == 2) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_2_short);
                            if(server_state == 3) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_3_short);
                            if(server_state == 4) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_4_short);
                            // Concatenate strings
                            String client_name_display = res.getString(R.string.client_name) + ": " + client_name;
                            String min_app_version_display = res.getString(R.string.min_app_version) + ": " + min_appversion;
                            String newest_app_version_display = res.getString(R.string.newest_app_version) + ": " + newest_appversion;
                            String owners_display = res.getString(R.string.owners) + ": " + owners;
                            // Concatenate strings and display dialog
                            final SpannableString info = new SpannableString(client_name_display + "\n" + server_state_display + "\n" + min_app_version_display + "\n" + newest_app_version_display + "\n" + owners_display);
                            Linkify.addLinks(info, Linkify.WEB_URLS);
                            androidx.appcompat.app.AlertDialog.Builder d_Result;
                            d_Result = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this);
                            d_Result.setTitle(res.getString(R.string.pref_serverinfo_t))
                                    .setMessage(info)
                                    .setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .create();
                            androidx.appcompat.app.AlertDialog d = d_Result.show();
                            ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}