package com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.ExternalSensor;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SensorAdapter;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ViewPagerAdapterMain extends FragmentPagerAdapter {

    //Konstanten
    private static final int REQ_LOCATION_PERMISSION = 10001;

    //Variablen als Objekte
    private static MainActivity activity;
    private static Random random;

    //Utils-Pakete
    private static StorageUtils su;
    private static ServerMessagingUtils smu;

    //Variablen

    public ViewPagerAdapterMain(FragmentManager manager, MainActivity activity, StorageUtils su, ServerMessagingUtils smu) {
        super(manager);
        ViewPagerAdapterMain.activity = activity;
        random = new Random();
        ViewPagerAdapterMain.su = su;
        ViewPagerAdapterMain.smu = smu;
    }

    @Override
    public Fragment getItem(int pos) {
        if (pos == 0) return new MyFavouritesFragment();
        if (pos == 1) return new AllSensorsFragment();
        if (pos == 2) return new MySensorsFragment();
        return null;
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return "";
    }

    public void refresh() {
        MyFavouritesFragment.refresh();
        AllSensorsFragment.refresh();
        MySensorsFragment.refresh();
    }

    public ArrayList<Sensor> getSelectedSensors() {
        ArrayList<Sensor> selected_sensors = new ArrayList<>();
        selected_sensors.addAll(MyFavouritesFragment.getSelectedSensors());
        selected_sensors.addAll(MySensorsFragment.getSelectedSensors());
        return Tools.removeDuplicateSensors(selected_sensors);
    }

    public void deselectAllSensors() {
        MyFavouritesFragment.deselectAllSensors();
        MySensorsFragment.deselectAllSensors();
    }

    public void search(String query, int mode) {
        if(mode == SensorAdapter.MODE_FAVOURITES) MyFavouritesFragment.search(query);
        if(mode == SensorAdapter.MODE_OWN_SENSORS) MySensorsFragment.search(query);
    }

    //-------------------------------------------Fragmente------------------------------------------

    public static class MyFavouritesFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        private static View contentView;
        private static RecyclerView sensor_view;
        private static SensorAdapter sensor_view_adapter;
        private static ArrayList<Sensor> sensors;

        //Variablen

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            contentView = inflater.inflate(R.layout.tab_my_favourites, null);

            sensor_view = contentView.findViewById(R.id.sensor_view);
            sensor_view.setItemViewCacheSize(100);
            sensor_view.setLayoutManager(new LinearLayoutManager(activity));

            refresh();

            return contentView;
        }

        public static void refresh() {
            sensors = su.getAllFavourites();
            sensors.addAll(su.getAllOwnSensors());

            sensor_view_adapter = new SensorAdapter(activity, sensors, su, smu, SensorAdapter.MODE_FAVOURITES);
            sensor_view.setAdapter(sensor_view_adapter);
            sensor_view.setHasFixedSize(true);
            sensor_view.setVisibility(sensors.size() == 0 ? View.GONE : View.VISIBLE);
            contentView.findViewById(R.id.no_data).setVisibility(sensors.size() == 0 ? View.VISIBLE : View.GONE);
        }

        public static ArrayList<Sensor> getSelectedSensors() {
            return sensor_view_adapter.getSelectedSensors();
        }

        public static void deselectAllSensors() {
            sensor_view_adapter.deselectAllSensors();
        }

        public static void search(String query) {
            ArrayList<Sensor> search_values;
            if(query.isEmpty()) {
                search_values = sensors;
            } else {
                search_values = new ArrayList<>();
                for(Sensor s : sensors) {
                    if(s.getName().toLowerCase().contains(query.toLowerCase()) || s.getId().toLowerCase().contains(query.toLowerCase())) search_values.add(s);
                }
            }
            sensor_view_adapter = new SensorAdapter(activity, search_values, su, smu, SensorAdapter.MODE_FAVOURITES);
            sensor_view.setAdapter(sensor_view_adapter);
        }
    }

    public static class AllSensorsFragment extends Fragment implements OnMapReadyCallback {
        //Konstanten

        //Variablen als Objekte
        private static View contentView;
        private SupportMapFragment map_fragment;
        private static GoogleMap map;
        private AlertDialog info_window;
        private static ArrayList<ExternalSensor> sensors;

        //Variablen
        private int current_color;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            contentView = inflater.inflate(R.layout.tab_all_sensors, null);

            map_fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            map_fragment.getMapAsync(this);

            LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            final boolean isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            final boolean isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(!isGpsProviderEnabled && !isNetworkProviderEnabled) {
                Snackbar.make(contentView, getString(R.string.enable_location_m), Snackbar.LENGTH_LONG)
                        .setAction(R.string.enable_location, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if(activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                    } else {
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION_PERMISSION);
                                        return;
                                    }
                                } else {
                                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                }

                                AlertDialog d = new AlertDialog.Builder(activity)
                                        .setCancelable(true)
                                        .setTitle(R.string.enabled_gps_t)
                                        .setMessage(R.string.enabled_gps_m)
                                        .setIcon(R.drawable.info_outline)
                                        .setPositiveButton(R.string.ok, null)
                                        .create();
                                d.show();
                            }
                        })
                        .setDuration(6000)
                        .setCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                activity.showFab(true);
                            }

                            @Override
                            public void onShown(Snackbar snackbar) {
                                activity.showFab(false);
                            }
                        }).show();
            }

            return contentView;
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            map = googleMap;
            map.getUiSettings().setRotateGesturesEnabled(false);
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.map_style_silver));
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
                map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(Location location) {
                        moveCamera(new LatLng(location.getLatitude(), location.getLongitude()));
                        map.setOnMyLocationChangeListener(null);
                    }
                });
            }

            //Zoom zum aktuellen Land
            try{
                TelephonyManager teleMgr = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                if (teleMgr != null) {
                    String iso = teleMgr.getSimCountryIso();
                    LatLng location = Tools.getLocationFromAddress(activity, iso);
                    if(location != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 5));
                }
            } catch (Exception e) {}

            googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(final Marker marker) {
                    View v = getLayoutInflater().inflate(R.layout.infowindow_sensor, null);
                    TextView sensor_chip_id = v.findViewById(R.id.sensor_chip_id);
                    TextView sensor_coordinates = v.findViewById(R.id.sensor_coordinates);
                    TextView sensor_location = v.findViewById(R.id.sensor_location);
                    Button sensor_show_data = v.findViewById(R.id.sensor_show_data);
                    Button sensor_link = v.findViewById(R.id.sensor_link);

                    try{
                        Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(marker.getPosition().latitude, marker.getPosition().longitude, 1);
                        String city = addresses.get(0).getLocality();
                        marker.setTag(city);
                        String country = addresses.get(0).getCountryName();
                        sensor_location.setText(country.equals("null") || city.equals("null") ? getString(R.string.unknown_location) : country + " - " + city);
                    } catch (Exception e) {
                        sensor_location.setText(R.string.unknown_location);
                        marker.setTag(marker.getTitle());
                    }

                    sensor_chip_id.setText(marker.getTitle());
                    sensor_coordinates.setText(marker.getSnippet());
                    sensor_show_data.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            random = new Random();
                            current_color = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));

                            Intent i = new Intent(activity, SensorActivity.class);
                            i.putExtra("Name", marker.getTitle());
                            i.putExtra("ID", marker.getTitle());
                            i.putExtra("Color", current_color);
                            activity.startActivity(i);

                            info_window.dismiss();
                        }
                    });
                    sensor_link.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!su.isFavouriteExisting(marker.getTitle()) && !su.isSensorExistingLocally(marker.getTitle())) {
                                View v = getLayoutInflater().inflate(R.layout.dialog_add_sensor, null);
                                final EditText name = v.findViewById(R.id.sensor_name_value);
                                EditText chip_id = v.findViewById(R.id.sensor_chip_id_value);
                                Button choose_color = v.findViewById(R.id.choose_sensor_color);
                                final ImageView sensor_color = v.findViewById(R.id.sensor_color);

                                name.setHint(marker.getTag().toString());
                                chip_id.setText(marker.getTitle());

                                //Zufallsgenerator initialisieren und zufällige Farbe ermitteln
                                random = new Random();
                                current_color = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));
                                sensor_color.setColorFilter(current_color, PorterDuff.Mode.SRC);

                                sensor_color.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        chooseColor(sensor_color);
                                    }
                                });
                                choose_color.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        chooseColor(sensor_color);
                                    }
                                });

                                AlertDialog d = new AlertDialog.Builder(activity)
                                        .setCancelable(true)
                                        .setTitle(R.string.add_favourite)
                                        .setView(v)
                                        .setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                //Neuen Sensor speichern
                                                String name_string = name.getText().toString().trim();
                                                if(name_string.isEmpty()) name_string = marker.getTag().toString();
                                                su.addFavourite(new Sensor(marker.getTitle(), name_string, current_color));
                                                MyFavouritesFragment.refresh();
                                                refresh();
                                                info_window.dismiss();
                                                Toast.makeText(activity, getString(R.string.favourite_added), Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .create();
                                d.show();
                            } else {
                                //Sensor ist bereits verknüpft
                                Toast.makeText(activity, getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    info_window = new AlertDialog.Builder(activity)
                            .setCancelable(true)
                            .setView(v)
                            .create();
                    info_window.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

                    Projection p = map.getProjection();
                    Point screen_pos = p.toScreenLocation(marker.getPosition());

                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                    lp.copyFrom(info_window.getWindow().getAttributes());
                    lp.gravity = Gravity.TOP | Gravity.LEFT;
                    lp.x = screen_pos.x - 275;
                    lp.y = screen_pos.y - 530;
                    lp.width = 550;
                    lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    info_window.show();
                    info_window.getWindow().setAttributes(lp);
                    return true;
                }
            });

            //Sensoren laden
            loadAllSensors();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if(requestCode == REQ_LOCATION_PERMISSION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                final boolean isGpsProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                final boolean isNetworkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                if(!isGpsProviderEnabled && !isNetworkProviderEnabled) startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                enableOwnLocation();
            }
        }

        public static void moveCamera(LatLng coords) {
            if (map != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 11));
        }

        public static void enableOwnLocation() {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
                map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(Location location) {
                        moveCamera(new LatLng(location.getLatitude(), location.getLongitude()));
                        map.setOnMyLocationChangeListener(null);
                    }
                });
            }
        }

        public static void refresh() {
            if(map != null) {
                map.clear();
                loadAllSensors();
            }
        }

        private void chooseColor(final ImageView sensor_color) {
            //Farb-Auswahl-Dialog anzeigen
            ColorPickerDialog color_picker = new ColorPickerDialog(activity, current_color);
            color_picker.setAlphaSliderVisible(false);
            color_picker.setHexValueEnabled(true);
            color_picker.setTitle(getString(R.string.choose_color));
            color_picker.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
                @Override
                public void onColorChanged(int color) {
                    current_color = color;
                    sensor_color.setColorFilter(color, PorterDuff.Mode.SRC);
                }
            });
            color_picker.show();
        }

        private static void loadAllSensors() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        String result = smu.sendRequest(contentView.findViewById(R.id.container), "command=getall");
                        if(!result.isEmpty()) {
                            JSONArray array = new JSONArray(result);
                            sensors = new ArrayList<>();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject jsonobject = array.getJSONObject(i);
                                ExternalSensor sensor = new ExternalSensor();
                                sensor.setChipID(jsonobject.getString("chip_id"));
                                sensor.setLat(jsonobject.getDouble("lat"));
                                sensor.setLng(jsonobject.getDouble("lng"));
                                sensors.add(sensor);
                            }

                            //Sensoren auf der Karte einzeichnen
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(map != null) {
                                        for(ExternalSensor sensor : sensors) {
                                            map.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(su.isFavouriteExisting(sensor.getChipID()) ? BitmapDescriptorFactory.HUE_RED : su.isSensorExistingLocally(sensor.getChipID()) ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_BLUE)).position(new LatLng(sensor.getLat(), sensor.getLng())).title(sensor.getChipID()).snippet(String.valueOf(sensor.getLat()) + ", " + String.valueOf(sensor.getLng())));
                                        }
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public static class MySensorsFragment extends Fragment {
        //Konstanten

        //Variablen als Objekte
        private static View contentView;
        private TextView no_data_text;
        private static RecyclerView sensor_view;
        private static SensorAdapter sensor_view_adapter;
        private static ArrayList<Sensor> sensors;

        //Variablen

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            contentView = inflater.inflate(R.layout.tab_my_sensors, null);

            sensor_view = contentView.findViewById(R.id.sensor_view);
            sensor_view.setItemViewCacheSize(100);
            sensor_view.setLayoutManager(new LinearLayoutManager(activity));

            no_data_text = contentView.findViewById(R.id.no_data_text);
            no_data_text.setMovementMethod(LinkMovementMethod.getInstance());

            refresh();

            return contentView;
        }

        public static void refresh() {
            sensors = su.getAllOwnSensors();
            sensor_view_adapter = new SensorAdapter(activity, sensors, su, smu, SensorAdapter.MODE_OWN_SENSORS);
            sensor_view.setAdapter(sensor_view_adapter);
            sensor_view.setVisibility(sensors.size() == 0 ? View.GONE : View.VISIBLE);
            contentView.findViewById(R.id.no_data).setVisibility(sensors.size() == 0 ? View.VISIBLE : View.GONE);
        }

        public static ArrayList<Sensor> getSelectedSensors() {
            return sensor_view_adapter.getSelectedSensors();
        }

        public static void deselectAllSensors() {
            sensor_view_adapter.deselectAllSensors();
        }

        public static void search(String query) {
            ArrayList<Sensor> search_values;
            if(query.isEmpty()) {
                search_values = sensors;
            } else {
                search_values = new ArrayList<>();
                for(Sensor s : sensors) {
                    if(s.getName().toLowerCase().contains(query.toLowerCase()) || s.getId().toLowerCase().contains(query.toLowerCase())) search_values.add(s);
                }
            }
            sensor_view_adapter = new SensorAdapter(activity, search_values, su, smu, SensorAdapter.MODE_OWN_SENSORS);
            sensor_view.setAdapter(sensor_view_adapter);
        }
    }
}