package com.mrgames13.jimdo.feinstaubapp.App;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
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

import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.util.Calendar;
import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    //Konstanten
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    //Variablen als Objekte
    private Resources res;
    private StorageUtils su;
    private Toolbar toolbar;

    //Variablen

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Resourcen initialisieren
        res = getResources();

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //Toolbar initialisieren
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
            toolbar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
            toolbar.setBackgroundColor(res.getColor(R.color.colorPrimary));
            toolbar.setTitleTextColor(res.getColor(R.color.white));
            toolbar.setTitle(res.getString(R.string.settings));
            Drawable upArrow = res.getDrawable(R.drawable.ic_arrow_back);
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
            Drawable upArrow = res.getDrawable(R.drawable.ic_arrow_back);
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

        EditTextPreference limit_sdsp1 = (EditTextPreference) findPreference("limit_sdsp1");
        EditTextPreference limit_sdsp2 = (EditTextPreference) findPreference("limit_sdsp2");
        EditTextPreference limit_temp = (EditTextPreference) findPreference("limit_temp");
        EditTextPreference limit_humidity = (EditTextPreference) findPreference("limit_humidity");
        EditTextPreference limit_pressure = (EditTextPreference) findPreference("limit_pressure");

        limit_sdsp1.setSummary(Integer.parseInt(su.getString("limit_sdsp1", String.valueOf(Constants.DEFAULT_SDSP1_LIMIT))) > 0 ? su.getString("limit_sdsp1", String.valueOf(Constants.DEFAULT_SDSP1_LIMIT)) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
        limit_sdsp1.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
                return true;
            }
        });

        limit_sdsp2.setSummary(Integer.parseInt(su.getString("limit_sdsp2", String.valueOf(Constants.DEFAULT_SDSP2_LIMIT))) > 0 ? su.getString("limit_sdsp2", String.valueOf(Constants.DEFAULT_SDSP2_LIMIT)) + " µg/m³" : res.getString(R.string.pref_limit_disabled));
        limit_sdsp2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
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

        limit_humidity.setSummary(Integer.parseInt(su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT))) > 0 ? su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_HUMIDITY_LIMIT)) + "%" : res.getString(R.string.pref_limit_disabled));
        limit_humidity.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + "%" : res.getString(R.string.pref_limit_disabled));
                return true;
            }
        });

        limit_pressure.setSummary(Integer.parseInt(su.getString("limit_pressure", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT))) > 0 ? su.getString("limit_humidity", String.valueOf(Constants.DEFAULT_PRESSURE_LIMIT)) + " Pa" : res.getString(R.string.pref_limit_disabled));
        limit_pressure.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                if(String.valueOf(o).equals("") || Integer.parseInt(String.valueOf(o)) <= 0) o = "0";
                preference.setSummary(Integer.parseInt(String.valueOf(o)) > 0 ? String.valueOf(o) + " Pa" : res.getString(R.string.pref_limit_disabled));
                return true;
            }
        });

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
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
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
}