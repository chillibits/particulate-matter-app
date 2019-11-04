/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.ViewPagerAdapters;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.mrgames13.jimdo.feinstaubapp.App.CompareActivity;
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.ExternalSensor;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.ClusterRederer;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.MarkerItem;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.SensorClusterItem;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters.SensorAdapter;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ViewPagerAdapterMain extends FragmentPagerAdapter {

    // Constants
    private static final int REQ_LOCATION_PERMISSION = 10001;

    // Variables as objects
    private static MainActivity activity;
    private static Random random;

    // Utils packages
    private static StorageUtils su;
    private static ServerMessagingUtils smu;

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

    public void refreshFavourites() {
        MyFavouritesFragment.refresh();
    }

    public void refreshMySensors() {
        MySensorsFragment.refresh();
    }

    public ArrayList<Sensor> getSelectedSensors() {
        ArrayList<Sensor> selected_sensors = new ArrayList<>();
        selected_sensors.addAll(MyFavouritesFragment.getSelectedSensors());
        selected_sensors.addAll(MySensorsFragment.getSelectedSensors());
        return Tools.INSTANCE.removeDuplicateSensors(selected_sensors);
    }

    public void deselectAllSensors() {
        MyFavouritesFragment.deselectAllSensors();
        MySensorsFragment.deselectAllSensors();
    }

    public void search(String query, int mode) {
        if(mode == SensorAdapter.MODE_FAVOURITES) MyFavouritesFragment.search(query);
        if(mode == SensorAdapter.MODE_OWN_SENSORS) MySensorsFragment.search(query);
    }

    public boolean closeInfoWindow() {
        return AllSensorsFragment.closeInfoWindow();
    }

    //-------------------------------------------Fragmente------------------------------------------

    public static class MyFavouritesFragment extends Fragment {

        // Variables as objects
        private static View contentView;
        private static RecyclerView sensor_view;
        private static SensorAdapter sensor_view_adapter;
        private static ArrayList<Sensor> sensors;

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
                    if(s.getName().toLowerCase().contains(query.toLowerCase()) || s.getChipID().toLowerCase().contains(query.toLowerCase())) search_values.add(s);
                }
            }
            sensor_view_adapter = new SensorAdapter(activity, search_values, su, smu, SensorAdapter.MODE_FAVOURITES);
            sensor_view.setAdapter(sensor_view_adapter);
        }
    }

    public static class AllSensorsFragment extends Fragment implements OnMapReadyCallback {

        // Variables as objects
        private static View contentView;
        private static SupportMapFragment map_fragment;
        private Spinner map_type;
        private Spinner map_traffic;
        private static TextView map_sensor_count;
        private ImageView map_sensor_refresh;
        private static GoogleMap map;
        private static ClusterManager<SensorClusterItem> clusterManager;
        private static ArrayList<ExternalSensor> sensors;
        private static LatLng current_country;
        private static ProgressDialog pd;
        // Sensor info window
        private static RelativeLayout sensor_container;
        private TextView sensor_chip_id;
        private TextView sensor_coordinates;
        private TextView sensor_location;
        private Button sensor_show_data;
        private Button sensor_link;
        private static LatLng selected_marker_position;
        // Sensor cluster info window
        private static RelativeLayout sensor_cluster_container;
        private TextView info_sensor_count;
        private TextView info_average_value;
        private Button info_compare_sensors;
        private Button info_zoom_in;
        private static LatLng selected_cluster_position;

        // Variables
        private int current_color;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            contentView = inflater.inflate(R.layout.tab_all_sensors, null);

            map_fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

            map_type = contentView.findViewById(R.id.map_type);
            List<String> map_types = new ArrayList<>();
            map_types.add(getString(R.string.normal));
            map_types.add(getString(R.string.terrain));
            map_types.add(getString(R.string.satellite));
            map_types.add(getString(R.string.hybrid));
            final ArrayAdapter<String> adapter_type = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, map_types);
            adapter_type.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            map_type.setAdapter(adapter_type);
            map_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int type, long l) {
                    if(map != null) {
                        if(type == 0) {
                            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        } else if(type == 1) {
                            map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                        } else if(type == 2) {
                            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        } else if(type == 3) {
                            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            map_traffic = contentView.findViewById(R.id.map_traffic);
            List<String> map_traffic_items = new ArrayList<>();
            map_traffic_items.add(getString(R.string.traffic_hide));
            map_traffic_items.add(getString(R.string.traffic_show));
            final ArrayAdapter<String> adapter_traffic = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, map_traffic_items);
            adapter_traffic.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            map_traffic.setAdapter(adapter_traffic);
            map_traffic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int type, long l) {
                    if(map != null) {
                        if(type == 0) {
                            map.setTrafficEnabled(false);
                        } else {
                            map.setTrafficEnabled(true);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            map_sensor_count = contentView.findViewById(R.id.map_sensor_count);
            map_sensor_count.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://h2801469.stratoserver.net/stats.php"));
                    startActivity(i);
                }
            });

            map_sensor_refresh = contentView.findViewById(R.id.map_sensor_refresh);
            map_sensor_refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    pd = new ProgressDialog(activity);
                    pd.setMessage(getResources().getString(R.string.loading_data));
                    pd.setCancelable(false);
                    pd.show();
                    loadAllSensorsNonSync();
                }
            });

            map_fragment.getMapAsync(this);

            // Initialize sensor info window
            sensor_container = contentView.findViewById(R.id.sensor_container);
            sensor_chip_id = contentView.findViewById(R.id.sensor_chip_id);
            sensor_coordinates = contentView.findViewById(R.id.sensor_coordinates);
            sensor_location = contentView.findViewById(R.id.sensor_location);
            sensor_show_data = contentView.findViewById(R.id.sensor_show_data);
            sensor_link = contentView.findViewById(R.id.sensor_link);

            // Initialize sensor cluster info window
            sensor_cluster_container = contentView.findViewById(R.id.sensor_cluster_container);
            info_sensor_count = contentView.findViewById(R.id.info_sensor_count);
            info_average_value = contentView.findViewById(R.id.info_average_value);
            info_compare_sensors = contentView.findViewById(R.id.info_sensors_compare);
            info_zoom_in = contentView.findViewById(R.id.info_sensors_zoom);

            return contentView;
        }

        @Override
        public void onMapReady(final GoogleMap googleMap) {
            map = googleMap;
            map.getUiSettings().setRotateGesturesEnabled(false);
            map.getUiSettings().setZoomControlsEnabled(true);

            // relocate MyLocationButton
            View locationButton = ((View) map_fragment.getView().findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            rlp.setMargins(0, 0, 27, 430);

            @SuppressLint("ResourceType") View zoomControls = map_fragment.getView().findViewById(0x1);
            if (zoomControls != null && zoomControls.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) zoomControls.getLayoutParams();

                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

                final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, getResources().getDisplayMetrics());
                params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, margin);
            }

            enableOwnLocation();

            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, getResources().getColor(R.color.colorPrimary) == getResources().getColor(R.color.dark_mode_indicator) ? R.raw.map_style_dark : R.raw.map_style_silver));

            // Initialize ClusterManager
            clusterManager = new ClusterManager<>(activity, map);
            clusterManager.setRenderer(new ClusterRederer(activity, map, clusterManager, su));
            if(su.getBoolean("enable_marker_clustering", true)) {
                map.setOnMarkerClickListener(clusterManager);
                clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<SensorClusterItem>() {
                    @Override
                    public boolean onClusterItemClick(SensorClusterItem sensorClusterItem) {
                        showInfoWindow(sensorClusterItem.getMarker());
                        return true;
                    }
                });
                clusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<SensorClusterItem>() {
                    @Override
                    public boolean onClusterClick(Cluster<SensorClusterItem> cluster) {
                        showClusterWindow(cluster);
                        return true;
                    }
                });
                map.setOnCameraIdleListener(clusterManager);
            } else {
                map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(final Marker marker) {
                        MarkerItem m = new MarkerItem(marker.getTitle(), marker.getSnippet(), marker.getPosition());
                        showInfoWindow(m);
                        return true;
                    }
                });
            }

            // Zoom to current country
            try{
                TelephonyManager teleMgr = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                if (teleMgr != null) {
                    String iso = teleMgr.getSimCountryIso();
                    current_country = Tools.INSTANCE.getLocationFromAddress(activity, iso);
                }
            } catch (Exception ignored) {}

            map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                @Override
                public void onCameraMove() {
                    if(selected_marker_position != null) {
                        Projection p = map.getProjection();
                        Point screen_pos = p.toScreenLocation(selected_marker_position);
                        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT);
                        int x = Math.max(0, screen_pos.x - 275);
                        int y = Math.max(0, screen_pos.y - 680);
                        x = x + 550 > map_fragment.getView().getWidth() ? map_fragment.getView().getWidth() - 550 : x;
                        y = y + sensor_container.getHeight() > map_fragment.getView().getHeight() ? map_fragment.getView().getHeight() - sensor_container.getHeight() : y;
                        lp.setMargins(x, y, 0, 0);
                        sensor_container.setLayoutParams(lp);
                    }
                    if(selected_cluster_position != null) {
                        Projection p = map.getProjection();
                        Point screen_pos = p.toScreenLocation(selected_cluster_position);
                        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT);
                        int x = Math.max(0, screen_pos.x - 275);
                        int y = Math.max(0, screen_pos.y - 650);
                        x = x + 550 > map_fragment.getView().getWidth() ? map_fragment.getView().getWidth() - 550 : x;
                        y = y + sensor_cluster_container.getHeight() > map_fragment.getView().getHeight() ? map_fragment.getView().getHeight() - sensor_cluster_container.getHeight() : y;
                        lp.setMargins(x, y, 0, 0);
                        sensor_cluster_container.setLayoutParams(lp);
                    }
                }
            });

            map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    if(selected_marker_position != null) {
                        exitReveal(sensor_container);
                    } else if(selected_cluster_position != null) {
                        exitReveal(sensor_cluster_container);
                    } else {
                        // Show/Hide toolbar und BottomNavigationBar
                        activity.toggleToolbar();
                    }
                }
            });

            // Load sensors
            loadAllSensors();
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if(isGPSPermissionGranted()) {
                if(!isGPSEnabled(activity)) startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                enableOwnLocation();
            }
        }

        public static void moveCamera(LatLng coords) {
            if (map != null) map.animateCamera(CameraUpdateFactory.newLatLngZoom(coords, 11));
        }

        @SuppressLint("MissingPermission")
        private void enableOwnLocation() {
            if(isGPSPermissionGranted() && isGPSEnabled(activity)) {
                map.setMyLocationEnabled(true);
                map.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                    @Override
                    public void onMyLocationChange(Location location) {
                        moveCamera(new LatLng(location.getLatitude(), location.getLongitude()));
                        map.setOnMyLocationChangeListener(null);
                    }
                });
            } else if(isGPSEnabled(activity)) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION_PERMISSION);
            }
        }

        public static void refresh() {
            if(map != null) {
                CameraPosition pos = map.getCameraPosition();
                map.clear();
                loadAllSensors();
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos.target, pos.zoom));
            }
        }

        private void chooseColor(final ImageView sensor_color) {
            // Show color picker dialog
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
            if(smu.checkConnection(contentView)) {
                // The device is online. We're able to load data from the server
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            // Load old sensors from the local db
                            sensors = su.getExternalSensors();
                            // Create hash
                            double chip_sum = 0;
                            for(ExternalSensor s : sensors) chip_sum+=Long.parseLong(s.getChipID()) / 1000d;
                            final String sensor_hash = Tools.INSTANCE.md5(String.valueOf((int) Tools.INSTANCE.round(chip_sum, 0)));
                            // Load new sensors from server
                            long last_request = su.getLong("LastRequest", 0);
                            final String last_request_string = String.valueOf(last_request).length() > 10 ? String.valueOf(last_request).substring(0, 10) : String.valueOf(last_request);
                            long new_last_request = System.currentTimeMillis();
                            String result = smu.sendRequest(contentView.findViewById(R.id.container), new HashMap<String, String>() {{
                                put("command", "getall");
                                put("last_request", last_request_string);
                                put("cs", sensor_hash);
                            }});
                            if(!result.isEmpty()) {
                                JSONObject array = new JSONObject(result);
                                JSONArray array_update = array.getJSONArray("update");
                                JSONArray array_ids = array.getJSONArray("ids");
                                if(array_ids.length() > 0) {
                                    for(ExternalSensor s : sensors) {
                                        boolean found = false;
                                        for (int i = 0; i < array_ids.length(); i++) {
                                            String chip_id = array_ids.get(i).toString();
                                            if(chip_id.equals(s.getChipID())) {
                                                found = true;
                                                break;
                                            }
                                        }
                                        if(!found) su.deleteExternalSensor(s.getChipID());
                                    }
                                }
                                // Process update
                                for (int i = 0; i < array_update.length(); i++) {
                                    JSONObject jsonobject = array_update.getJSONObject(i);
                                    ExternalSensor sensor = new ExternalSensor();
                                    sensor.setChipID(jsonobject.getString("i"));
                                    sensor.setLat(jsonobject.getDouble("l"));
                                    sensor.setLng(jsonobject.getDouble("b"));
                                    su.addExternalSensor(sensor);
                                }
                                su.putLong("LastRequest", new_last_request);
                                sensors = su.getExternalSensors();
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        drawSensorsToMap();
                                    }
                                });
                            } else {
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(activity, R.string.error_try_again, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                // We're offline, load the old data from the local db
                sensors = su.getExternalSensors();
                drawSensorsToMap();
            }
        }

        private static void drawSensorsToMap() {
            // Draw the sensors on the map
            map_sensor_count.setText(String.valueOf(sensors.size()));
            if(map != null) {
                boolean is_marker_clustering_enabled = su.getBoolean("enable_marker_clustering", true);
                if(is_marker_clustering_enabled) {
                    clusterManager.clearItems();
                } else {
                    map.clear();
                }
                for(ExternalSensor sensor : sensors) {
                    if(is_marker_clustering_enabled) {
                        MarkerItem m = new MarkerItem(sensor.getChipID(), sensor.getLat() + ", " + sensor.getLng(), new LatLng(sensor.getLat(), sensor.getLng()));
                        clusterManager.addItem(new SensorClusterItem(sensor.getLat(), sensor.getLng(), sensor.getChipID(), sensor.getLat() + ", " + sensor.getLng(), m));
                    } else {
                        map.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.defaultMarker(su.isFavouriteExisting(sensor.getChipID()) ? BitmapDescriptorFactory.HUE_RED : su.isSensorExisting(sensor.getChipID()) ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_BLUE))
                                .position(new LatLng(sensor.getLat(), sensor.getLng()))
                                .title(sensor.getChipID())
                                .snippet(sensor.getLat() + ", " + sensor.getLng())
                        );
                    }
                }
                if(current_country != null) map.moveCamera(CameraUpdateFactory.newLatLngZoom(current_country, 5));
                if(su.getBoolean("enable_marker_clustering", true)) clusterManager.cluster();
            }
        }

        private static void loadAllSensorsNonSync() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        long start = System.currentTimeMillis();
                        // Load new sensors from server
                        long new_last_request = System.currentTimeMillis();
                        String result = smu.sendRequest(contentView.findViewById(R.id.container), new HashMap<String, String>() {{
                            put("command", "getallnonsync");
                        }});
                        Log.i("FA", "Loading time: " + (System.currentTimeMillis() - start));
                        if(!result.isEmpty()) {
                            su.clearExternalSensors();
                            JSONArray array = new JSONArray(result);
                            sensors = new ArrayList<>();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject jsonobject = array.getJSONObject(i);
                                ExternalSensor sensor = new ExternalSensor();
                                sensor.setChipID(jsonobject.getString("i"));
                                sensor.setLat(jsonobject.getDouble("l"));
                                sensor.setLng(jsonobject.getDouble("b"));
                                su.addExternalSensor(sensor);
                            }
                            su.putLong("LastRequest", new_last_request);
                            sensors = su.getExternalSensors();

                            // Draw sensors on the map
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    drawSensorsToMap();
                                    if(pd != null && pd.isShowing()) pd.dismiss();
                                }
                            });
                        } else {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pd.dismiss();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        private boolean isGPSEnabled(Context context){
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }

        private boolean isGPSPermissionGranted() {
            return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        private void showInfoWindow(final MarkerItem marker) {
            if(selected_cluster_position != null) exitReveal(sensor_cluster_container);
            if(selected_marker_position != null && selected_marker_position.latitude == marker.getPosition().latitude && selected_marker_position.longitude == marker.getPosition().longitude) {
                exitReveal(sensor_container);
            } else {
                sensor_chip_id.setText(marker.getTitle());
                sensor_coordinates.setText(marker.getSnippet());
                marker.setTag(marker.getTitle());
                sensor_show_data.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        exitReveal(sensor_container);

                        random = new Random();
                        current_color = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));

                        Intent i = new Intent(activity, SensorActivity.class);
                        i.putExtra("Name", marker.getTag());
                        i.putExtra("ID", marker.getTitle());
                        i.putExtra("Color", current_color);
                        activity.startActivity(i);
                    }
                });
                sensor_link.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!su.isFavouriteExisting(marker.getTitle()) && !su.isSensorExisting(marker.getTitle())) {
                            View v = getLayoutInflater().inflate(R.layout.dialog_add_sensor, null);
                            final EditText name = v.findViewById(R.id.sensor_name_value);
                            EditText chip_id = v.findViewById(R.id.sensor_chip_id_value);
                            Button choose_color = v.findViewById(R.id.choose_sensor_color);
                            final ImageView sensor_color = v.findViewById(R.id.sensor_color);

                            name.setHint(marker.getTag());
                            chip_id.setText(marker.getTitle());

                            // Initialize randomizer and generate random color
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
                                            if(name_string.isEmpty()) name_string = marker.getTag();
                                            su.addFavourite(new Sensor(marker.getTitle(), name_string, current_color), false);
                                            MyFavouritesFragment.refresh();
                                            AllSensorsFragment.refresh();
                                            selected_marker_position = null;
                                            sensor_container.setVisibility(View.GONE);
                                            Toast.makeText(activity, getString(R.string.favourite_added), Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .create();
                            d.show();
                        } else {
                            // Sensor is already linked
                            Toast.makeText(activity, getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                Projection p = map.getProjection();
                Point screen_pos = p.toScreenLocation(selected_marker_position = marker.getPosition());

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT);
                int x = Math.max(0, screen_pos.x - 275);
                int y = Math.max(0, screen_pos.y - 680);
                x = x + 550 > map_fragment.getView().getWidth() ? map_fragment.getView().getWidth() - 550 : x;
                y = y + sensor_container.getHeight() > map_fragment.getView().getHeight() ? map_fragment.getView().getHeight() - sensor_container.getHeight() : y;
                lp.setMargins(x, y, 0, 0);
                sensor_container.setLayoutParams(lp);
                enterReveal(sensor_container);

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
            }
        }

        private void showClusterWindow(final Cluster cluster) {
            if(sensor_container != null) exitReveal(sensor_container);
            if(selected_cluster_position != null && selected_cluster_position.latitude == cluster.getPosition().latitude && selected_cluster_position.longitude == cluster.getPosition().longitude) {
                exitReveal(sensor_cluster_container);
            } else {
                info_sensor_count.setText(cluster.getItems().size() + " " + getString(R.string.sensors));
                info_average_value.setText(R.string.loading);
                info_compare_sensors.setEnabled(cluster.getItems().size() <= 15);

                Projection p = map.getProjection();
                Point screen_pos = p.toScreenLocation(selected_cluster_position = cluster.getPosition());

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(550, ViewGroup.LayoutParams.WRAP_CONTENT);
                int x = Math.max(0, screen_pos.x - 275);
                int y = Math.max(0, screen_pos.y - 650);
                x = x + 550 > map_fragment.getView().getWidth() ? map_fragment.getView().getWidth() - 550 : x;
                y = y + sensor_cluster_container.getHeight() > map_fragment.getView().getHeight() ? map_fragment.getView().getHeight() - sensor_cluster_container.getHeight() : y;
                lp.setMargins(x, y, 0, 0);
                sensor_cluster_container.setLayoutParams(lp);
                enterReveal(sensor_cluster_container);

                final Collection<SensorClusterItem> items = cluster.getItems();
                String param = "";
                final ArrayList<Sensor> sensors = new ArrayList<>();
                Random rand = new Random();
                for(SensorClusterItem s : items) {
                    param+=";"+s.getTitle();
                    sensors.add(new Sensor(s.getTitle(), s.getSnippet(), Color.argb(255, rand.nextInt(256), rand.nextInt(256), rand.nextInt(256))));
                }
                final String param_string = param.substring(1);

                info_compare_sensors.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        exitReveal(sensor_cluster_container);
                        // Launch CompareActivity
                        Intent i = new Intent(activity, CompareActivity.class);
                        i.putExtra("Sensors", sensors);
                        startActivity(i);
                    }
                });
                info_zoom_in.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        exitReveal(sensor_cluster_container);
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), Math.min(map.getMaxZoomLevel(), map.getCameraPosition().zoom +3)));
                    }
                });

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // Get information from the server
                        final String result = smu.sendRequest(null, new HashMap<String, String>() {{
                            put("command", "getclusterinfo");
                            put("ids", param_string);
                        }});
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    info_average_value.setText("Ø " + Tools.INSTANCE.round(Double.parseDouble(result), 2) + " µg/m³");
                                } catch (Exception e) {
                                    info_average_value.setText(R.string.error_try_again);
                                }
                            }
                        });
                    }
                }).start();
            }
        }

        private void enterReveal(View v) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                int finalRadius = Math.max(v.getWidth(), v.getHeight()) +40;
                Animator anim = ViewAnimationUtils.createCircularReveal(v, 275, v.getMeasuredHeight(), 0, finalRadius);
                anim.setDuration(350);
                anim.start();
            }
            v.setVisibility(View.VISIBLE);
        }

        static void exitReveal(final View v) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                int initialRadius = Math.max(v.getWidth(), v.getHeight()) +40;
                Animator anim = ViewAnimationUtils.createCircularReveal(v, 275, v.getMeasuredHeight(), initialRadius, 0);
                anim.setDuration(350);
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v.setVisibility(View.INVISIBLE);
                    }
                });
                anim.start();
            } else {
                v.setVisibility(View.INVISIBLE);
            }
            selected_marker_position = null;
            selected_cluster_position = null;
        }

        public static boolean closeInfoWindow() {
            if(selected_marker_position != null) {
                exitReveal(sensor_container);
                selected_marker_position = null;
                return true;
            } else if(selected_cluster_position != null) {
                exitReveal(sensor_cluster_container);
                selected_cluster_position = null;
                return true;
            }
            return false;
        }
    }

    public static class MySensorsFragment extends Fragment {

        // Variables as objects
        private static View contentView;
        private TextView no_data_text;
        private static RecyclerView sensor_view;
        private static SensorAdapter sensor_view_adapter;
        private static ArrayList<Sensor> sensors;

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
                    if(s.getName().toLowerCase().contains(query.toLowerCase()) || s.getChipID().toLowerCase().contains(query.toLowerCase())) search_values.add(s);
                }
            }
            sensor_view_adapter = new SensorAdapter(activity, search_values, su, smu, SensorAdapter.MODE_OWN_SENSORS);
            sensor_view.setAdapter(sensor_view_adapter);
        }
    }
}