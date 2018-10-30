package com.mrgames13.jimdo.feinstaubapp.App;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.github.fabtransitionactivity.SheetLayout;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.SimpleAnimationListener;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SensorAdapter;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.NotificationUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters.ViewPagerAdapterMain;
import com.mrgames13.jimdo.splashscreen.App.SplashScreenBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity {

    //Konstanten
    public static final int REQ_ADD_OWN_SENSOR = 10002;
    public static final int REQ_SEARCH_LOCATION = 10003;
    private final int REQ_COMPARE = 10004;

    //Variablen als Objekte
    public static MainActivity own_instance;
    private Resources res;
    private Toolbar toolbar;
    private ViewPager pager;
    private ViewPagerAdapterMain pager_adapter;
    private BottomNavigationView bottom_nav;
    private MenuItem prevMenuItem;
    private FloatingActionButton fab;
    private SheetLayout sheet_fab;
    private FloatingActionButton fab_compare;
    private FloatingActionButton fab_compare_dismiss;
    private SheetLayout sheet_fab_compare;
    private MaterialSearchView searchView;

    //Utils-Pakete
    private StorageUtils su;
    private ServerMessagingUtils smu;

    //Variablen
    private boolean pressedOnce;
    private int selected_page;
    private boolean show_update_snackbar;
    private boolean selection_running;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //StorageUtils initialisieren
        su = new StorageUtils(this);

        int state = Integer.parseInt(su.getString("app_theme", "0"));
        AppCompatDelegate.setDefaultNightMode(state == 0 ? AppCompatDelegate.MODE_NIGHT_AUTO : (state == 1 ?  AppCompatDelegate.MODE_NIGHT_NO :  AppCompatDelegate.MODE_NIGHT_YES));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //SplashScreen anzeigen
        SplashScreenBuilder.getInstance(this)
                .setVideo(R.raw.splash_animation)
                .setImage(R.drawable.app_icon)
                .show();

        //Eigene Intanz initialisieren
        own_instance = this;

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(res.getString(R.string.app_name));
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
                if(searchView.isSearchOpen()) searchView.closeSearch();
                if(pos == 0) {
                    if(fab.getVisibility() == View.VISIBLE) {
                        Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_out);
                        a.setAnimationListener(new SimpleAnimationListener() {
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                fab.setVisibility(View.GONE);
                            }
                        });
                        fab.startAnimation(a);
                    }
                } else if(pos == 1) {
                    if(fab.getVisibility() == View.GONE) {
                        Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_in);
                        fab.startAnimation(a);
                        fab.setVisibility(View.VISIBLE);
                    }
                    if(selected_page == 2) {
                        fab.setImageResource(R.drawable.fab_anim_add_to_search);
                        Drawable drawable = fab.getDrawable();
                        if (drawable instanceof Animatable) ((Animatable) drawable).start();
                    }
                    selected_page = 1;
                } else if(pos == 2) {
                    if(fab.getVisibility() == View.GONE) {
                        Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_in);
                        fab.startAnimation(a);
                        fab.setVisibility(View.VISIBLE);
                    }
                    if(selected_page == 1) {
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
                invalidateOptionsMenu();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        bottom_nav = findViewById(R.id.bottom_navigation);
        bottom_nav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if(id == R.id.action_my_favorites) pager.setCurrentItem(0);
                if(id == R.id.action_all_sensors) pager.setCurrentItem(1);
                if(id == R.id.action_my_sensors) pager.setCurrentItem(2);
                return true;
            }
        });

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(pager.getCurrentItem() == 1) {
                    try {
                        Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(MainActivity.this);
                        startActivityForResult(intent, REQ_SEARCH_LOCATION);
                        overridePendingTransition(0, 0);
                    } catch (GooglePlayServicesRepairableException e) {
                        e.printStackTrace();
                    } catch (GooglePlayServicesNotAvailableException e) {
                        e.printStackTrace();
                    }
                } else if(pager.getCurrentItem() == 2) {
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

        //Start-Position auf der Karte
        pager.setCurrentItem(1);

        NotificationUtils.createNotificationChannels(this);
        getServerInfo();
    }

    @Override
    protected void onStart() {
        super.onStart();

        int background_sync_frequency = Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) * 1000 * 60;

        //Alarmmanager aufsetzen
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent start_service_intent = new Intent(this, SyncService.class);
        PendingIntent start_service_pending_intent = PendingIntent.getService(this, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, start_service_intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), background_sync_frequency, start_service_pending_intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(bottom_nav.getSelectedItemId() == R.id.action_my_favorites) {
            getMenuInflater().inflate(R.menu.menu_activity_main_my_favourites, menu);

            MenuItem item = menu.findItem(R.id.action_search);
            searchView.setMenuItem(item);
        } else if(bottom_nav.getSelectedItemId() == R.id.action_all_sensors) {
            getMenuInflater().inflate(R.menu.menu_activity_main_all_sensors, menu);
        } else if(bottom_nav.getSelectedItemId() == R.id.action_my_sensors) {
            getMenuInflater().inflate(R.menu.menu_activity_main_my_sensors, menu);

            MenuItem item = menu.findItem(R.id.action_search);
            searchView.setMenuItem(item);
        }
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
            } else {
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

    public void refresh() {
        pager_adapter.refresh();
        updateSelectionMode();
    }

    private void rateApp() {
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.rate))
                .setMessage(res.getString(R.string.rate_m))
                .setIcon(R.mipmap.ic_launcher)
                .setCancelable(true)
                .setPositiveButton(res.getString(R.string.rate), new DialogInterface.OnClickListener() {
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
                .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        d.show();
    }

    private void recommendApp() {
        AlertDialog d = new AlertDialog.Builder(this)
                .setTitle(res.getString(R.string.share))
                .setMessage(res.getString(R.string.share_m))
                .setIcon(R.mipmap.ic_launcher)
                .setCancelable(true)
                .setPositiveButton(res.getString(R.string.share), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent i = new Intent();
                        i.setAction(Intent.ACTION_SEND);
                        i.putExtra(Intent.EXTRA_TEXT, res.getString(R.string.recommend_string));
                        i.setType("text/plain");
                        startActivity(i);
                    }
                })
                .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        d.show();
    }

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
        if(requestCode == SplashScreenBuilder.SPLASH_SCREEN_FINISHED) {
            if(show_update_snackbar) {
                Snackbar.make(findViewById(R.id.container), res.getString(R.string.update_available), Snackbar.LENGTH_LONG)
                        .setAction(res.getString(R.string.download), new View.OnClickListener() {
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

            //Nutzer auf Aktualisierung der Sensoren aufmerksam machen
            if(!su.getBoolean("ShownSensorCompletion", false) && su.getAllOwnSensors().size() > 0) {
                AlertDialog d = new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setIcon(R.drawable.warning)
                        .setTitle(R.string.sensor_position_completion_t)
                        .setMessage(R.string.sensor_position_completion_m)
                        .setPositiveButton(R.string.ok, null)
                        .create();
                d.show();
            }
            su.putBoolean("ShownSensorCompletion", true);
        } else if(requestCode == REQ_ADD_OWN_SENSOR) {
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
        }
    }

    private void getServerInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = smu.sendRequest(null, "command=getserverinfo");
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
                    .setTitle(res.getString(R.string.offline_t))
                    .setMessage(user_msg.equals("") ? res.getString(R.string.offline_m) : user_msg)
                    .setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
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
                    .setTitle(res.getString(R.string.maintenance_t))
                    .setMessage(user_msg.equals("") ? res.getString(R.string.maintenance_m) : user_msg)
                    .setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
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
                    .setTitle(res.getString(R.string.support_end_t))
                    .setMessage(user_msg.equals("") ? res.getString(R.string.support_end_m) : user_msg)
                    .setPositiveButton(res.getString(R.string.ok), new DialogInterface.OnClickListener() {
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
                        .setTitle(res.getString(R.string.update_necessary_t))
                        .setMessage(res.getString(R.string.update_necessary_m))
                        .setPositiveButton(res.getString(R.string.download_update), new DialogInterface.OnClickListener() {
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
                        .setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .create();
                d.show();
            } else if(app_version_code < newest_app_version) {
                show_update_snackbar = true;
            }
        }
    }

    public void showFab(boolean show) {
        if(show && pager.getCurrentItem() != 0) {
            if(fab.getVisibility() == View.GONE) {
                Animation a = AnimationUtils.loadAnimation(MainActivity.this, R.anim.scale_in);
                fab.startAnimation(a);
                fab.setVisibility(View.VISIBLE);
            }
        } else {
            if(fab.getVisibility() == View.VISIBLE) fab.setVisibility(View.GONE);
        }
    }
}