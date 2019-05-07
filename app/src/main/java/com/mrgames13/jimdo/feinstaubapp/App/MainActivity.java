package com.mrgames13.jimdo.feinstaubapp.App;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.github.fabtransitionactivity.SheetLayout;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SensorAdapter;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncJobService;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Services.WebRealtimeSyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.NotificationUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters.ViewPagerAdapterMain;
import com.mrgames13.jimdo.splashscreen.HelpClasses.SimpleAnimationListener;
import com.taskail.googleplacessearchdialog.SimplePlacesSearchDialog;
import com.taskail.googleplacessearchdialog.SimplePlacesSearchDialogBuilder;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

public class MainActivity extends AppCompatActivity {

    //Konstanten
    private static final String QR_PREFIX_SUFFIX = "01010";
    public static final int REQ_ADD_OWN_SENSOR = 10002;
    public static final int REQ_SEARCH_LOCATION = 10003;
    private static final int REQ_COMPARE = 10004;
    private static final int REQ_SCAN_WEB = 10005;
    private static final int REQ_SCAN_SENSOR = 10006;
    private static final String REQ_IMPORT_XML = "Import_XML";
    private static final int REQ_SELECT_SENSORS = 10007;

    //Variablen als Objekte
    public static MainActivity own_instance;
    private Resources res;
    private ViewPager pager;
    public ViewPagerAdapterMain pager_adapter;
    private BottomNavigationView bottom_nav;
    private MenuItem prevMenuItem;
    private FloatingActionButton fab;
    private SheetLayout sheet_fab;
    private FloatingActionButton fab_compare;
    private FloatingActionButton fab_compare_dismiss;
    private SheetLayout sheet_fab_compare;
    private MaterialSearchView searchView;
    private MenuItem search_item;

    //Utils-Pakete
    private StorageUtils su;
    private ServerMessagingUtils smu;

    //Variablen
    private boolean pressedOnce;
    private int selected_page;
    private boolean selection_running;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //StorageUtils initialisieren
        su = new StorageUtils(this);

        int state = Integer.parseInt(su.getString("app_theme", "0"));
        AppCompatDelegate.setDefaultNightMode(state == 0 ? AppCompatDelegate.MODE_NIGHT_AUTO : (state == 1 ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Eigene Intanz initialisieren
        own_instance = this;

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(this, su);

        //Komponenten initialisieren
        pager = findViewById(R.id.view_pager);
        pager.setOffscreenPageLimit(3);
        pager_adapter = new ViewPagerAdapterMain(getSupportFragmentManager(), this, su, smu);
        pager.setAdapter(pager_adapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int pos) {
                if (searchView.isSearchOpen()) searchView.closeSearch();
                if (pos == 0) {
                    if (fab.getVisibility() == View.VISIBLE) {
                        Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_out);
                        a.setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                fab.setVisibility(View.GONE);
                            }
                        });
                        fab.startAnimation(a);
                    }
                } else if (pos == 1) {
                    if (fab.getVisibility() == View.GONE) {
                        Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_in);
                        a.setAnimationListener(new SimpleAnimationListener() {
                            @SuppressLint("RestrictedApi")
                            @Override
                            public void onAnimationStart(Animation animation) {
                                fab.setVisibility(View.VISIBLE);
                            }
                        });
                        fab.startAnimation(a);
                    }
                    if (selected_page == 2) {
                        fab.setImageResource(R.drawable.fab_anim_add_to_search);
                        Drawable drawable = fab.getDrawable();
                        if (drawable instanceof Animatable) ((Animatable) drawable).start();
                    }
                    selected_page = 1;
                } else if (pos == 2) {
                    if (fab.getVisibility() == View.GONE) {
                        Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_in);
                        a.setAnimationListener(new SimpleAnimationListener() {
                            @SuppressLint("RestrictedApi")
                            @Override
                            public void onAnimationStart(Animation animation) {
                                fab.setVisibility(View.VISIBLE);
                            }
                        });
                        fab.startAnimation(a);
                    }
                    if (selected_page == 1) {
                        fab.setImageResource(R.drawable.fab_anim_search_to_add);
                        Drawable drawable = fab.getDrawable();
                        if (drawable instanceof Animatable) ((Animatable) drawable).start();
                    }
                    selected_page = 2;
                }

                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                } else {
                    bottom_nav.getMenu().getItem(0).setChecked(false);
                }
                bottom_nav.getMenu().getItem(pos).setChecked(true);
                prevMenuItem = bottom_nav.getMenu().getItem(pos);
                if(search_item != null) search_item.setVisible(pos != 1);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        bottom_nav = findViewById(R.id.bottom_navigation);
        bottom_nav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_my_favourites) pager.setCurrentItem(0);
                if (id == R.id.action_all_sensors) pager.setCurrentItem(1);
                if (id == R.id.action_my_sensors) pager.setCurrentItem(2);
                return true;
            }
        });

        final int nightModeFlags = res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pager.getCurrentItem() == 1) {
                    SimplePlacesSearchDialog d = new SimplePlacesSearchDialogBuilder(MainActivity.this)
                            .setSearchHint(getString(R.string.search_places))
                            .setLocationListener(new SimplePlacesSearchDialog.PlaceSelectedCallback() {
                                @Override
                                public void onPlaceSelected(@NotNull Place place) {
                                    ViewPagerAdapterMain.AllSensorsFragment.moveCamera(place.getLatLng());
                                }
                            }).build();
                    if(nightModeFlags == UI_MODE_NIGHT_YES) {
                        ((View)d.findViewById(R.id.search_edit_text).getParent()).setBackgroundColor(res.getColor(R.color.bg_dark));
                        d.findViewById(R.id.recyclerFrame).setBackgroundColor(res.getColor(R.color.bg_dark));
                    }
                    d.show();
                } else if (pager.getCurrentItem() == 2) {
                    sheet_fab.expandFab();
                }
            }
        });

        sheet_fab = findViewById(R.id.sheet_fab);
        sheet_fab.setFabAnimationEndListener(new SheetLayout.OnFabAnimationEndListener() {
            @Override
            public void onFabAnimationEnd() {
                startActivityForResult(new Intent(MainActivity.this, AddSensorActivity.class), REQ_ADD_OWN_SENSOR);
            }
        });
        sheet_fab.setFab(fab);

        fab_compare = findViewById(R.id.fab_compare);
        fab_compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sheet_fab_compare.expandFab();
            }
        });
        sheet_fab_compare = findViewById(R.id.sheet_fab_compare);
        sheet_fab_compare.setFabAnimationEndListener(new SheetLayout.OnFabAnimationEndListener() {
            @Override
            public void onFabAnimationEnd() {
                //CompareActivity starten
                Intent i = new Intent(MainActivity.this, CompareActivity.class);
                i.putExtra("Sensors", pager_adapter.getSelectedSensors());
                startActivityForResult(i, REQ_COMPARE);
            }
        });
        sheet_fab_compare.setFab(fab_compare);

        fab_compare_dismiss = findViewById(R.id.fab_compare_dismiss);
        fab_compare_dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Alle Sensoren deselektieren
                pager_adapter.deselectAllSensors();
                updateSelectionMode();
            }
        });

        searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                pager_adapter.search(query, pager.getCurrentItem() == 0 ? SensorAdapter.MODE_FAVOURITES : SensorAdapter.MODE_OWN_SENSORS);
                updateSelectionMode();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                pager_adapter.search(newText, pager.getCurrentItem() == 0 ? SensorAdapter.MODE_FAVOURITES : SensorAdapter.MODE_OWN_SENSORS);
                updateSelectionMode();
                return true;
            }
        });
        if(nightModeFlags == UI_MODE_NIGHT_YES) searchView.setBackgroundColor(res.getColor(R.color.gray_light));

        //Start-Position auf der Karte
        pager.setCurrentItem(1);

        initializeApp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { WebRealtimeSyncService.own_instance.stop(); } catch (Exception e) {}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        search_item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(search_item);
        search_item.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if(id == R.id.action_rate) {
            rateApp();
        } else if(id == R.id.action_share) {
            recommendApp();
        } else if(id == R.id.action_search) {
            item.expandActionView();
        } else if(id == R.id.action_import_export) {
            importExportConfiguration();
        } else if(id == R.id.action_help) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://mrgames13.jimdo.com/feinstaub-app/faq"));
            startActivity(i);
        } else if(id == R.id.action_web) {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setRequestCode(REQ_SCAN_WEB);
            integrator.setOrientationLocked(true);
            integrator.setBeepEnabled(false);
            integrator.setPrompt(getString(R.string.scan_prompt));
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.initiateScan();
        } else if(id == R.id.action_exit) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(searchView.isSearchOpen()) {
                searchView.closeSearch();
            } else if(!pager_adapter.closeInfoWindow()) {
                if(!pressedOnce) {
                    pressedOnce = true;
                    Toast.makeText(MainActivity.this, R.string.tap_again_to_exit_app, Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pressedOnce = false;
                        }
                    }, 2500);
                } else {
                    pressedOnce = false;
                    onBackPressed();
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initializeApp() {
        //Notification Channels erstellen
        NotificationUtils.createNotificationChannels(this);

        //ServerInfo abfragen
        getServerInfo();

        //Hintergrundservices starten
        int background_sync_frequency = Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) * 1000 * 60;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(!isJobServiceOn(this)) {
                //JobScheduler starten
                ComponentName component = new ComponentName(this, SyncJobService.class);
                JobInfo.Builder info = new JobInfo.Builder(Constants.JOB_SYNC_ID, component)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NOT_ROAMING)
                        .setPeriodic(background_sync_frequency)
                        .setPersisted(true);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) info.setRequiresBatteryNotLow(true);
                JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                Log.i("FA", scheduler.schedule(info.build()) == JobScheduler.RESULT_SUCCESS ? "Job scheduled successfully" : "Job schedule failed");
            }
        } else {
            //Alarmmanager aufsetzen
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent start_service_intent = new Intent(this, SyncService.class);
            PendingIntent start_service_pending_intent = PendingIntent.getService(this, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, start_service_intent, 0);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), background_sync_frequency, start_service_pending_intent);
        }

        //Intent-Abfragen vornehmen
        Intent intent = getIntent();
        Uri appLinkData = intent.getData();
        if(appLinkData != null && appLinkData.toString().startsWith("https://feinstaub.mrgames-server.de/s/")) {
            String chip_id = appLinkData.toString().substring(appLinkData.toString().lastIndexOf("/") + 1);
            Random random = new Random();
            int color = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));

            Intent i = new Intent(this, SensorActivity.class);
            i.putExtra("Name", chip_id);
            i.putExtra("ID", chip_id);
            i.putExtra("Color", color);
            startActivity(i);
        } else if(intent.hasExtra("ChipID")) {
            Sensor s = su.getSensor(intent.getStringExtra("ChipID"));
            Intent i = new Intent(this, SensorActivity.class);
            i.putExtra("Name", s.getName());
            i.putExtra("ID", s.getChipID());
            i.putExtra("Color", s.getColor());
            startActivity(i);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isJobServiceOn(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE) ;
        boolean hasBeenScheduled = false ;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == Constants.JOB_SYNC_ID) {
                hasBeenScheduled = true ;
                break;
            }
        }
        return hasBeenScheduled ;
    }

    public void refresh() {
        pager_adapter.refresh();
        updateSelectionMode();
    }

    private void rateApp() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.rate))
                .setMessage(getString(R.string.rate_m))
                .setIcon(R.mipmap.ic_launcher)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.rate), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final String app_package_name = getPackageName();
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app_package_name)));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app_package_name)));
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    private void recommendApp() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.recommend))
                .setMessage(getString(R.string.recommend_m))
                .setIcon(R.mipmap.ic_launcher)
                .setCancelable(true)
                .setPositiveButton(getString(R.string.recommend), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_SEND);
                        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.recommend_string));
                        i.setType("text/plain");
                        startActivity(i);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
    }
    
    private void importExportConfiguration() {
        View v = getLayoutInflater().inflate(R.layout.dialog_import_export, null);
        final android.app.AlertDialog d = new android.app.AlertDialog.Builder(this)
                .setView(v)
                .create();
        d.show();

        RelativeLayout import_qr = v.findViewById(R.id.import_qr);
        import_qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                        IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                        integrator.setRequestCode(REQ_SCAN_SENSOR);
                        integrator.setOrientationLocked(true);
                        integrator.setBeepEnabled(false);
                        integrator.setPrompt(getString(R.string.scan_qr_code_prompt));
                        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
                        integrator.initiateScan();
                    }
                }, 200);
            }
        });
        RelativeLayout export_qr = v.findViewById(R.id.export_qr);
        export_qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();

                        try{
                            ArrayList<Sensor> sensors = pager_adapter.getSelectedSensors();
                            if(sensors.size() > 0) {
                                String qr_string = QR_PREFIX_SUFFIX;
                                for(int i = 0; i < sensors.size(); i++) {
                                    Sensor s = sensors.get(i);
                                    if(i > 0) qr_string = qr_string.concat(";");
                                    qr_string = qr_string.concat(s.getChipID());
                                    qr_string = qr_string.concat(",");
                                    qr_string = qr_string.concat(Base64.encodeToString(s.getName().getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
                                    qr_string = qr_string.concat(",");
                                    qr_string = qr_string.concat(String.valueOf(s.getColor()));
                                }
                                qr_string += QR_PREFIX_SUFFIX;

                                ImageView qr_view = new ImageView(MainActivity.this);
                                qr_view.setAdjustViewBounds(true);
                                MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
                                BitMatrix bitMatrix = multiFormatWriter.encode(qr_string, BarcodeFormat.QR_CODE,500,500);
                                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                                final Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
                                qr_view.setImageBitmap(bitmap);

                                new AlertDialog.Builder(MainActivity.this)
                                        .setView(qr_view)
                                        .setPositiveButton(getString(R.string.ok), null)
                                        .setNeutralButton(getString(R.string.share_sensor), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                su.shareImage(bitmap, getString(R.string.share_qr_code));
                                            }
                                        })
                                        .create().show();
                            } else {
                                Toast.makeText(MainActivity.this, getString(R.string.please_select_at_least_one_sensor), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, getString(R.string.error_try_again), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, 200);
            }
        });
        RelativeLayout import_xml = v.findViewById(R.id.import_xml);
        import_xml.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                        DialogProperties properties = new DialogProperties();
                        properties.selection_mode = DialogConfigs.SINGLE_MODE;
                        properties.selection_type = DialogConfigs.FILE_SELECT;
                        properties.root = Environment.getExternalStorageDirectory();
                        properties.error_dir = Environment.getExternalStorageDirectory();
                        properties.offset = Environment.getExternalStorageDirectory();
                        properties.extensions = new String[]{"xml"};
                        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
                        dialog.setTitle(R.string.import_xml_file);
                        dialog.setDialogSelectionListener(new DialogSelectionListener() {
                            @Override
                            public void onSelectedFilePaths(String[] files) {
                                su.importXMLFile(files[0]);
                                refresh();
                            }
                        });
                        dialog.show();
                    }
                }, 200);
            }
        });
        RelativeLayout export_xml = v.findViewById(R.id.export_xml);
        export_xml.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                        su.exportXMLFile();
                    }
                }, 200);
            }
        });
    }

    @SuppressLint("RestrictedApi")
    public void updateSelectionMode() {
        if(pager_adapter.getSelectedSensors().size() >= 2) {
            if(!selection_running) {
                Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_in);
                fab_compare.startAnimation(a);
                fab_compare_dismiss.startAnimation(a);
                fab_compare.setVisibility(View.VISIBLE);
                fab_compare_dismiss.setVisibility(View.VISIBLE);
                selection_running = true;
            }
        } else {
            if(selection_running) {
                Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_out);
                a.setAnimationListener(new SimpleAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        fab_compare.setVisibility(View.GONE);
                    }
                });
                fab_compare.startAnimation(a);

                a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_out);
                a.setAnimationListener(new SimpleAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        fab_compare_dismiss.setVisibility(View.GONE);
                    }
                });
                fab_compare_dismiss.startAnimation(a);
                selection_running = false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_ADD_OWN_SENSOR) {
            sheet_fab.contractFab();
        } else if(requestCode == REQ_SEARCH_LOCATION && resultCode == RESULT_OK) {
            Place place = PlaceAutocomplete.getPlace(this, data);
            ViewPagerAdapterMain.AllSensorsFragment.moveCamera(place.getLatLng());
        } else if(requestCode == REQ_COMPARE) {
            sheet_fab_compare.contractFab();
        } else if (requestCode == MaterialSearchView.REQUEST_VOICE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && matches.size() > 0) {
                String searchWrd = matches.get(0);
                if (!TextUtils.isEmpty(searchWrd)) searchView.setQuery(searchWrd, false);
            }
        } else if(requestCode == REQ_SCAN_WEB && resultCode == RESULT_OK) {
            try{
                IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);
                String sync_key = result.getContents();
                if(sync_key.length() == 25 && !sync_key.startsWith("http")) {
                    Intent i = new Intent(MainActivity.this, WebRealtimeSyncService.class);
                    i.putExtra("sync_key", sync_key);
                    startService(i);
                    //Toast anzeigen
                    Toast t = new Toast(MainActivity.this);
                    t.setGravity(Gravity.CENTER, 0, 0);
                    t.setDuration(Toast.LENGTH_LONG);
                    t.setView(getLayoutInflater().inflate(R.layout.sync_success, null));
                    t.show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.error_try_again, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, R.string.error_try_again, Toast.LENGTH_SHORT).show();
            }
        } else if(requestCode == REQ_SCAN_SENSOR && resultCode == RESULT_OK) {
            try{
                IntentResult result = IntentIntegrator.parseActivityResult(resultCode, data);
                String configuration_string = result.getContents();
                if(configuration_string.startsWith(QR_PREFIX_SUFFIX) && configuration_string.endsWith(QR_PREFIX_SUFFIX)) {
                    configuration_string = configuration_string.substring(0, configuration_string.length() - QR_PREFIX_SUFFIX.length()).substring(QR_PREFIX_SUFFIX.length());
                    String[] configs = configuration_string.split(";");
                    for(String config : configs) {
                        String chip_id = config.split(",")[0];
                        String name = new String(Base64.decode(config.split(",")[1], Base64.DEFAULT), StandardCharsets.UTF_8);
                        int color = Integer.parseInt(config.split(",")[2]);
                        if(!su.isSensorExisting(chip_id)) {
                            su.addFavourite(new Sensor(chip_id, name, color), false);
                            Toast.makeText(MainActivity.this, getString(R.string.favourite_added), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show();
                        }
                    }
                    refresh();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, R.string.error_try_again, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getServerInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = smu.sendRequest(null, new HashMap<String, String>() {{
                        put("command", "getserverinfo");
                    }});
                    if(!result.isEmpty()) {
                        JSONArray array = new JSONArray(result);
                        JSONObject jsonobject = array.getJSONObject(0);
                        final int server_state = jsonobject.getInt("serverstate");
                        final int min_appversion = Integer.parseInt(jsonobject.getString("min_appversion"));
                        final int newest_appversion = Integer.parseInt(jsonobject.getString("newest_appversion"));
                        final String user_msg = jsonobject.getString("user_message");
                        //Parameter abspeichern
                        su.putInt("ServerState", server_state);
                        su.putInt("MinAppVersion", min_appversion);
                        su.putInt("NewestAppVersion", newest_appversion);
                        su.putString("UserMsg", user_msg);
                        //ServerInfo verarbeiten
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                parseServerInfo(server_state, min_appversion, newest_appversion, user_msg);
                            }
                        });
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    private void parseServerInfo(int server_state, int min_app_version, int newest_app_version, String user_msg) {
        int app_version_code = 0;
        try { app_version_code = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; } catch (PackageManager.NameNotFoundException e1) {}
        //ServerState verarbeiten
        if(server_state == 2) {
            AlertDialog d = new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle(getString(R.string.offline_t))
                    .setMessage(user_msg.equals("") ? getString(R.string.offline_m) : user_msg)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .create();
            d.show();
        } else if(server_state == 3) {
            AlertDialog d = new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle(getString(R.string.maintenance_t))
                    .setMessage(user_msg.equals("") ? getString(R.string.maintenance_m) : user_msg)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .create();
            d.show();
        } else if(server_state == 4) {
            AlertDialog d = new AlertDialog.Builder(MainActivity.this)
                    .setCancelable(false)
                    .setTitle(getString(R.string.support_end_t))
                    .setMessage(user_msg.equals("") ? getString(R.string.support_end_m) : user_msg)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .create();
            d.show();
        } else {
            //AppVersion überprüfen
            if(app_version_code < min_app_version) {
                AlertDialog d = new AlertDialog.Builder(MainActivity.this)
                        .setCancelable(false)
                        .setTitle(getString(R.string.update_necessary_t))
                        .setMessage(getString(R.string.update_necessary_m))
                        .setPositiveButton(getString(R.string.download_update), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                                }
                                finish();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .create();
                d.show();
            } else if(app_version_code < newest_app_version) {
                Snackbar.make(findViewById(R.id.container), getString(R.string.update_available), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.download), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                                }
                            }
                        })
                        .show();
            }
        }
    }
}