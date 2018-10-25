package com.mrgames13.jimdo.feinstaubapp.App;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends PreferenceActivity {

    //Konstanten
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    //Variablen als Objekte
    private Resources res;
    private Toolbar toolbar;
    private ProgressDialog pd;

    //Utils-Pakete
    private StorageUtils su;
    private ServerMessagingUtils smu;

    //Variablen
    private String result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Resourcen initialisieren
        res = getResources();

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(this, su);

        //Toolbar initialisieren
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
            toolbar.setBackgroundColor(res.getColor(R.color.colorPrimary));
            toolbar.setTitleTextColor(res.getColor(R.color.white));
            toolbar.setTitle(res.getString(R.string.settings));
            Drawable upArrow = res.getDrawable(R.drawable.arrow_back);
            upArrow.setColorFilter(res.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
            toolbar.setNavigationIcon(upArrow);
            root.addView(toolbar, 0);
        } else {
            ViewGroup root = findViewById(android.R.id.content);
            ListView content = (ListView) root.getChildAt(0);
            root.removeAllViews();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
            toolbar.setBackgroundColor(res.getColor(R.color.colorPrimary));
            toolbar.setTitleTextColor(res.getColor(R.color.white));
            toolbar.setTitle(res.getString(R.string.settings));
            Drawable upArrow = res.getDrawable(R.drawable.arrow_back);
            upArrow.setColorFilter(res.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
            toolbar.setNavigationIcon(upArrow);
            int height;
            TypedValue tv = new TypedValue();
            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                height = TypedValue.complexToDimensionPixelSize(tv.data, res.getDisplayMetrics());
            } else{
                height = toolbar.getHeight();
            }
            content.setPadding(0, height, 0, 0);
            root.addView(content);
            root.addView(toolbar);
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
        if (isSimplePreferences(this)) return;
        addPreferencesFromResource(R.xml.pref_main);

        EditTextPreference sync_cycle = (EditTextPreference) findPreference("sync_cycle");
        sync_cycle.setSummary(su.getString("sync_cycle", String.valueOf(Constants.DEFAULT_SYNC_CYCLE)) + " " + (Integer.parseInt(su.getString("sync_cycle", String.valueOf(Constants.DEFAULT_SYNC_CYCLE))) == 1 ? res.getString(R.string.second) : res.getString(R.string.seconds)));
        sync_cycle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) < 20) return false;
                preference.setSummary(String.valueOf(o) + " " + (Integer.parseInt(String.valueOf(o)) == 1 ? res.getString(R.string.second) : res.getString(R.string.seconds)));
                return true;
            }
        });

        EditTextPreference sync_cycle_background = (EditTextPreference) findPreference("sync_cycle_background");
        sync_cycle_background.setSummary(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND)) + " " + (Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) == 1 ? res.getString(R.string.minute) : res.getString(R.string.minutes)));
        sync_cycle_background.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) < 10) return false;
                preference.setSummary(String.valueOf(o) + " " + (Integer.parseInt(String.valueOf(o)) == 1 ? res.getString(R.string.minute) : res.getString(R.string.minutes)));

                //AlarmManager updaten
                int background_sync_frequency = Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) * 1000 * 60;
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent start_service_intent = new Intent(SettingsActivity.this, SyncService.class);
                PendingIntent start_service_pending_intent = PendingIntent.getService(SettingsActivity.this, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, start_service_intent, 0);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), background_sync_frequency, start_service_pending_intent);

                startService(start_service_intent);

                return true;
            }
        });

        EditTextPreference limit_p1 = (EditTextPreference) findPreference("limit_p1");
        EditTextPreference limit_p2 = (EditTextPreference) findPreference("limit_p2");
        EditTextPreference limit_temp = (EditTextPreference) findPreference("limit_temp");
        EditTextPreference limit_humidity = (EditTextPreference) findPreference("limit_humidity");
        EditTextPreference limit_pressure = (EditTextPreference) findPreference("limit_pressure");

        limit_p1.setSummary(Integer.parseInt(su.getString("limit_p1", String.valueOf(Constants.DEFAULT_P1_LIMIT))) > 0 ? su.getString("limit_p1", String.valueOf(Constants.DEFAULT_P1_LIMIT)) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
        limit_p1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
                return true;
            }
        });

        limit_p2.setSummary(Integer.parseInt(su.getString("limit_p2", String.valueOf(Constants.DEFAULT_P2_LIMIT))) > 0 ? su.getString("limit_p2", String.valueOf(Constants.DEFAULT_P2_LIMIT)) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
        limit_p2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
                return true;
            }
        });


        limit_temp.setSummary(Integer.parseInt(su.getString("limit_temp", String.valueOf(Constants.DEFAULT_TEMP_LIMIT))) > 0 ? su.getString("limit_temp", String.valueOf(Constants.DEFAULT_TEMP_LIMIT)) + "°C" : res.getString(R.string.pref_limit_disabled));
        limit_temp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + "°C" : res.getString(R.string.pref_limit_disabled));
                return true;
            }
        });

        try{
            Integer.parseInt(su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT))); // TODO: Bei nächstem Update entfernen
        } catch (Exception e) {
            su.putString("limit_humidity", "0");
        }
        limit_humidity.setSummary(Integer.parseInt(su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT))) > 0 ? su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT)) + "%" : res.getString(R.string.pref_limit_disabled));
        limit_humidity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + "%" : res.getString(R.string.pref_limit_disabled));
                return true;
            }
        });

        try{
            Integer.parseInt(su.getString("limit_pressure", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT))); // TODO: Bei nächstem Update entfernen
        } catch (Exception e) {
            su.putString("limit_pressure", "0");
        }
        limit_pressure.setSummary(Integer.parseInt(su.getString("limit_pressure", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT))) > 0 ? su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT)) + " kPa" : res.getString(R.string.pref_limit_disabled));
        limit_pressure.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + " kPa" : res.getString(R.string.pref_limit_disabled));
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
                        //Info vom Server holen
                        getServerInfo(false, false);
                        //Result auseinandernehmen
                        if(!result.isEmpty()) {
                            JSONArray array = new JSONArray(result);
                            JSONObject jsonobject = array.getJSONObject(0);
                            final int server_state_int = jsonobject.getInt("serverstate");

                            //ServerState überschreiben
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
                    } catch(Exception e) {}
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
        } catch (PackageManager.NameNotFoundException e) {}
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
        return isXLargeTablet(this) && isSimplePreferences(this);
    }

    private static boolean isXLargeTablet(Context context) {
        //return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
        return false;
    }

    private static boolean isSimplePreferences(Context context) {
        return !ALWAYS_SIMPLE_PREFS && isXLargeTablet(context);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (isSimplePreferences(this)) loadHeadersFromResource(R.xml.prefs_headers, target);
    }

    private static Preference.OnPreferenceChangeListener value_listener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(value_listener);
        value_listener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return MainPreferenceFragment.class.getName().equals(fragmentName);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            bindPreferenceSummaryToValue(findPreference("sensor_id"));
            bindPreferenceSummaryToValue(findPreference("sync_cycle"));
        }
    }

    private String getServerInfo(final boolean showProgressDialog, final boolean showResultDialog) {
        try {
            if(showProgressDialog) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Dialog für den Fortschritt anzeigen
                        pd = new ProgressDialog(SettingsActivity.this);
                        pd.setMessage(res.getString(R.string.pref_serverinfo_downloading_));
                        pd.setIndeterminate(true);
                        pd.setTitle(res.getString(R.string.pref_serverinfo_t));
                        pd.show();
                    }
                });
            }
            //Abfrage an den Server senden
            result = smu.sendRequest(null, "name="+ URLEncoder.encode(su.getString("Username"), "UTF-8")+"&command=getserverinfo");
            //Result auseinandernehmen
            if(!result.isEmpty()) {
                JSONArray array = new JSONArray(result);
                JSONObject jsonobject = array.getJSONObject(0);
                final String client_name = jsonobject.getString("clientname");
                final int server_state = jsonobject.getInt("serverstate");
                final String min_appversion = jsonobject.getString("min_appversion");
                final String newest_appversion = jsonobject.getString("newest_appversion");
                final String owners = jsonobject.getString("owner");

                //Dialog für das Ergebnis anzeigen
                if(showResultDialog) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(showProgressDialog) pd.dismiss();
                            //Serverinfo überschreiben
                            String server_state_display = null;
                            if(server_state == 1) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_1_short);
                            if(server_state == 2) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_2_short);
                            if(server_state == 3) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_3_short);
                            if(server_state == 4) server_state_display = res.getString(R.string.server_state) + ": " + res.getString(R.string.serverstate_4_short);
                            //String einzeln zusammensetzen
                            String client_name_display = res.getString(R.string.client_name) + ": " + client_name;
                            String min_app_version_display = res.getString(R.string.min_app_version) + ": " + min_appversion;
                            String newest_app_version_display = res.getString(R.string.newest_app_version) + ": " + newest_appversion;
                            String owners_display = res.getString(R.string.owners) + ": " + owners;
                            //String zusammensetzen und Dialog anzeigen
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
        return result;
    }
}