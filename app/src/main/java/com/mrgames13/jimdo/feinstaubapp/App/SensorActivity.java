/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.App;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.NotificationUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;
import com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters.ViewPagerAdapterSensor;
import com.mrgames13.jimdo.feinstaubapp.WidgetComponents.WidgetProvider;

import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorActivity extends AppCompatActivity implements ViewPagerAdapterSensor.OnFragmentsLoadedListener {

    // Constants
    public static final int SORT_MODE_TIME_ASC = 101;
    public static final int SORT_MODE_TIME_DESC = 102;
    public static final int SORT_MODE_VALUE1_ASC = 103;
    public static final int SORT_MODE_VALUE1_DESC = 104;
    public static final int SORT_MODE_VALUE2_ASC = 105;
    public static final int SORT_MODE_VALUE2_DESC = 106;
    public static final int SORT_MODE_TEMP_ASC = 107;
    public static final int SORT_MODE_TEMP_DESC = 108;
    public static final int SORT_MODE_HUMIDITY_ASC = 109;
    public static final int SORT_MODE_HUMIDITY_DESC = 110;
    public static final int SORT_MODE_PRESSURE_ASC = 111;
    public static final int SORT_MODE_PRESSURE_DESC = 112;
    private static final int REQ_WRITE_EXTERNAL_STORAGE = 1;

    // Variables as objects
    private ViewPager view_pager;
    private ViewPagerAdapterSensor view_pager_adapter;
    private Calendar calendar;
    private ImageView card_date_next;
    private ImageView card_date_today;
    private MenuItem progress_menu_item;
    private ScheduledExecutorService service;
    public static ArrayList<DataRecord> records = new ArrayList<>();
    private Sensor sensor;
    private SimpleDateFormat sdf_date = new SimpleDateFormat("dd.MM.yyyy");

    // Utils packages
    private ServerMessagingUtils smu;
    private StorageUtils su;
    private NotificationUtils nu;

    // Variables
    public static long selected_day_timestamp;
    public static long current_day_timestamp;
    public static int sort_mode = SORT_MODE_TIME_ASC; // Attention!! When you alter that attribute, the ViewPagerAdapterSensor does not work correctly any more
    public static boolean custom_p1 = true;
    public static boolean custom_p2 = true;
    public static boolean custom_temp = false;
    public static boolean custom_humidity = false;
    public static boolean custom_pressure = false;
    private int export_mode;
    private int bottomInsets = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        // Initialize toolbar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            getWindow().getDecorView().setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    toolbar.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
                    bottomInsets = insets.getSystemWindowInsetBottom();
                    return insets;
                }
            });
        }

        // Initialize StorageUtils
        su = new StorageUtils(this);

        // Initialize ServerMessagingUtils
        smu = new ServerMessagingUtils(SensorActivity.this, su);

        // Initialize NotificationUtils
        nu = new NotificationUtils(this);

        // Get intent extras
        sensor = new Sensor();
        if (getIntent().hasExtra("Name")) {
            sensor.setName(getIntent().getStringExtra("Name"));
            getSupportActionBar().setTitle(getIntent().getStringExtra("Name"));
        }
        if (getIntent().hasExtra("ID")) sensor.setId(getIntent().getStringExtra("ID"));
        if (getIntent().hasExtra("Color")) sensor.setColor(getIntent().getIntExtra("Color", getResources().getColor(R.color.colorPrimary)));

        // Initialize ViewPager
        view_pager = findViewById(R.id.view_pager);
        view_pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | (position == 0 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0));
                }
            }
        });
        view_pager_adapter = new ViewPagerAdapterSensor(getSupportFragmentManager(), SensorActivity.this, su, su.getBoolean("ShowGPS_" + sensor.getChipID()));
        view_pager.setAdapter(view_pager_adapter);

        // Setup TabLayout
        TabLayout tab_layout = findViewById(R.id.tablayout);
        tab_layout.setTabGravity(TabLayout.GRAVITY_FILL);
        tab_layout.setupWithViewPager(view_pager);
        tab_layout.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        tab_layout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                view_pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Initialize calendar
        if (selected_day_timestamp == 0 || calendar == null) {
            calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            current_day_timestamp = calendar.getTime().getTime();
            selected_day_timestamp = current_day_timestamp;
        }

        // Initialize card component
        final TextView card_date_value = findViewById(R.id.card_date_value);
        ImageView card_date_edit = findViewById(R.id.card_date_edit);
        card_date_today = findViewById(R.id.card_date_today);
        ImageView card_date_back = findViewById(R.id.card_date_back);
        card_date_next = findViewById(R.id.card_date_next);

        card_date_value.setText(sdf_date.format(calendar.getTime()));
        card_date_value.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Select date
                chooseDate(card_date_value);
            }
        });
        card_date_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Select date
                chooseDate(card_date_value);
            }
        });
        card_date_today.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set date to the current day
                calendar.setTime(new Date());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                selected_day_timestamp = calendar.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar.getTime()));

                card_date_next.setEnabled(false);
                card_date_today.setEnabled(false);

                // Load data for selected date
                loadData();
            }
        });
        card_date_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to previous day
                calendar.add(Calendar.DATE, -1);

                selected_day_timestamp = calendar.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar.getTime()));

                Calendar current_calendar = Calendar.getInstance();
                current_calendar.set(Calendar.HOUR_OF_DAY, 0);
                current_calendar.set(Calendar.MINUTE, 0);
                current_calendar.set(Calendar.SECOND, 0);
                current_calendar.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar.before(current_calendar));
                card_date_today.setEnabled(calendar.before(current_calendar));

                // Load data for selected date
                loadData();
            }
        });
        card_date_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to next day
                calendar.add(Calendar.DATE, 1);

                selected_day_timestamp = calendar.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar.getTime()));

                Calendar current_calendar = Calendar.getInstance();
                current_calendar.set(Calendar.HOUR_OF_DAY, 0);
                current_calendar.set(Calendar.MINUTE, 0);
                current_calendar.set(Calendar.SECOND, 0);
                current_calendar.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar.before(current_calendar));
                card_date_today.setEnabled(calendar.before(current_calendar));

                // Load data for selected date
                loadData();
            }
        });
        card_date_next.setEnabled(false);
        card_date_today.setEnabled(false);

        // Set refresh period
        int period = Integer.parseInt(su.getString("sync_cycle", String.valueOf(Constants.INSTANCE.getDEFAULT_SYNC_CYCLE())));

        // Setup ScheduledExecutorService
        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (selected_day_timestamp == current_day_timestamp) {
                    Log.i("FA", "Auto refreshing ...");
                    loadData();
                }
            }
        }, period, period, TimeUnit.SECONDS);

        if (!sensor.getChipID().equals("no_id")) loadData();

        //Check if sensor is existing on the server
        checkSensorAvailability();
    }

    private void chooseDate(final TextView card_date_value) {
        // Select date
        DatePickerDialog date_picker_dialog = new DatePickerDialog(SensorActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Calendar calendar_new = Calendar.getInstance();
                calendar_new.set(Calendar.YEAR, year);
                calendar_new.set(Calendar.MONTH, month);
                calendar_new.set(Calendar.DAY_OF_MONTH, day);
                calendar_new.set(Calendar.HOUR_OF_DAY, 0);
                calendar_new.set(Calendar.MINUTE, 0);
                calendar_new.set(Calendar.SECOND, 0);
                calendar_new.set(Calendar.MILLISECOND, 0);
                card_date_next.setEnabled(calendar_new.before(calendar));
                card_date_today.setEnabled(calendar_new.before(calendar));

                selected_day_timestamp = calendar_new.getTime().getTime();
                card_date_value.setText(sdf_date.format(calendar_new.getTime()));

                calendar = calendar_new;

                // Load data for selected date
                loadData();
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        date_picker_dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        date_picker_dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_sensor, menu);
        menu.findItem(R.id.action_show_gps).setChecked(su.getBoolean("ShowGPS_" + sensor.getChipID()));
        progress_menu_item = menu.findItem(R.id.action_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        } else if(id == R.id.action_export) {
            exportData();
        } else if(id == R.id.action_show_gps) {
            item.setChecked(!item.isChecked());
            view_pager_adapter.showGPSData(item.isChecked());
            su.putBoolean("ShowGPS_" + sensor.getChipID(), item.isChecked());
        } else if(id == R.id.action_refresh) {
            // Reload data
            Log.i("FA", "User refreshing ...");
            loadData();
        } else if(id == R.id.action_settings) {
            //Launch SettingsActivity
            startActivity(new Intent(SensorActivity.this, SettingsActivity.class));
        } else if(id == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(service != null) service.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQ_WRITE_EXTERNAL_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if(export_mode == 1) {
                exportDiagram();
            } else if(export_mode == 2) {
                exportDataRecords();
            }
        }
    }

    //-----------------------------------Private Methoden-------------------------------------------

    private void loadData() {
        // Set ProgressMenuItem
        if (progress_menu_item != null) progress_menu_item.setActionView(R.layout.menu_item_loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Clear records
                records.clear();

                // Get timestamps for 'from' and'to'
                long from = selected_day_timestamp;
                long to = selected_day_timestamp + TimeUnit.DAYS.toMillis(1);

                if(su.getBoolean("reduce_data_consumption", true) && records.size() > 0 && selected_day_timestamp == current_day_timestamp) {
                    // Load existing records from local database
                    records = su.loadRecords(sensor.getChipID(), from, to);
                    // Sort by time
                    resortData();
                    from = records.get(records.size() -1).getDateTime().getTime() +1000;
                }

                // If previous record was more than 30 secs ago
                if((records.size() > 0 ? records.get(records.size() -1).getDateTime().getTime() : from) < System.currentTimeMillis() - 30000) {
                    // Check if internet is available
                    if(smu.isInternetAvailable()) {
                        // Internet is available
                        records.addAll(smu.manageDownloadsRecords(sensor.getChipID(), from, to));
                    } else {
                        // Internet is not available
                        smu.checkConnection(findViewById(R.id.container));
                    }
                }

                // Sort by time
                resortData();
                // Possibly execute error correction
                if (su.getBoolean("enable_auto_correction", true)) {
                    records = Tools.measurementCorrection1(records);
                    records = Tools.measurementCorrection2(records);
                }
                // Detect sensor breakdown
                if (smu.isInternetAvailable()) {
                    if (su.getBoolean("notification_breakdown", true) && su.isSensorExisting(sensor.getChipID()) && selected_day_timestamp == current_day_timestamp && Tools.isMeasurementBreakdown(su, records)) {
                        if (!su.getBoolean("BD_" + sensor.getChipID())) {
                            nu.displayMissingMeasurementsNotification(sensor.getChipID(), sensor.getName());
                            su.putBoolean("BD_" + sensor.getChipID(), true);
                        }
                    } else {
                        nu.cancelNotification(Integer.parseInt(sensor.getChipID()) * 10);
                        su.removeKey("BD_" + sensor.getChipID());
                    }
                }
                // Push data records into adapter
                ViewPagerAdapterSensor.records = records;
                // If there is a widget for this sensor, refresh it
                Intent update_intent = new Intent(getApplicationContext(), WidgetProvider.class);
                update_intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                update_intent.putExtra(Constants.INSTANCE.getWIDGET_EXTRA_SENSOR_ID(), sensor.getChipID());
                sendBroadcast(update_intent);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Refresh ViewpagerAdapter
                        view_pager_adapter.refreshFragments();
                        // Reset ProgressMenuItem
                        if (progress_menu_item != null) progress_menu_item.setActionView(null);
                    }
                });
            }
        }).start();
    }

    public static void resortData() {
        try{ Collections.sort(records); } catch (Exception ignored) {}
    }

    private void checkSensorAvailability() {
        if(!su.getBoolean("DontShowAgain_" + sensor.getChipID()) && smu.isInternetAvailable()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = smu.sendRequest(findViewById(R.id.container), new HashMap<String, String>() {{
                        put("command", "issensordataexisting");
                        put("chip_id", sensor.getChipID());
                    }});
                    if(!Boolean.parseBoolean(result)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog d = new AlertDialog.Builder(SensorActivity.this)
                                        .setCancelable(true)
                                        .setTitle(R.string.app_name)
                                        .setMessage(R.string.add_sensor_tick_not_set_message)
                                        .setPositiveButton(R.string.ok, null)
                                        .setNegativeButton(R.string.dont_show_again, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                su.putBoolean("DontShowAgain_" + sensor.getChipID(), true);
                                            }
                                        })
                                        .create();
                                d.show();
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void exportData() {
        View v = getLayoutInflater().inflate(R.layout.dialog_share, null);
        final AlertDialog d = new AlertDialog.Builder(this)
                .setView(v)
                .create();
        d.show();

        RelativeLayout share_sensor = v.findViewById(R.id.share_sensor);
        share_sensor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        shareSensor();
                        d.dismiss();
                    }
                }, 200);
            }
        });
        RelativeLayout export_diagram = v.findViewById(R.id.share_diagram);
        export_diagram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(records.size() > 0) {
                    if(ContextCompat.checkSelfPermission(SensorActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                exportDiagram();
                                d.dismiss();
                            }
                        }, 200);
                    } else {
                        export_mode = 1;
                        d.dismiss();
                        ActivityCompat.requestPermissions(SensorActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    Toast.makeText(SensorActivity.this, R.string.no_data_date, Toast.LENGTH_SHORT).show();
                }
            }
        });
        RelativeLayout export_data_records = v.findViewById(R.id.share_data_records);
        export_data_records.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(records.size() > 0) {
                    if(ContextCompat.checkSelfPermission(SensorActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                exportDataRecords();
                                d.dismiss();
                            }
                        }, 200);
                    } else {
                        export_mode = 2;
                        d.dismiss();
                        ActivityCompat.requestPermissions(SensorActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    Toast.makeText(SensorActivity.this, R.string.no_data_date, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void shareSensor() {
        // Share sensor
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_sensor));
        i.putExtra(Intent.EXTRA_TEXT, "https://pm.mrgames-server.de/s/" + sensor.getChipID());
        startActivity(Intent.createChooser(i, getString(R.string.share_sensor)));
    }

    private void exportDiagram() {
        // Eyport Diagram
        view_pager_adapter.exportDiagram();
    }

    private void exportDataRecords() {
        // Export data records
        Uri export_uri = su.exportDataRecords(records);
        if(export_uri != null) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(URLConnection.guessContentTypeFromName(export_uri.getPath()));
            i.putExtra(Intent.EXTRA_STREAM, export_uri);
            startActivity(Intent.createChooser(i, getString(R.string.export_data_records)));
        }
    }

    @Override
    public void onDiagramFragmentLoaded(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) view.setPadding(0, 0, 0, bottomInsets + Tools.dpToPx(3));
    }

    @Override
    public void onDataFragmentLoaded(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int paddingInPixels = Tools.dpToPx(3);
            view.setPadding(paddingInPixels, paddingInPixels, paddingInPixels, bottomInsets + paddingInPixels);
        }
    }
}