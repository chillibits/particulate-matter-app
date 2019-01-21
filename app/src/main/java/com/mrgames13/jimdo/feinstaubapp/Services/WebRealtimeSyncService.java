package com.mrgames13.jimdo.feinstaubapp.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WebRealtimeSyncService extends Service {

    //Konstanten

    //Variablen als Objekte
    private static DatabaseReference ref;
    ArrayList<Sensor> favourites;
    ArrayList<Sensor> own_sensors;

    //Utils-Pakete
    private StorageUtils su;

    //Variablen
    public static WebRealtimeSyncService own_instance;
    private long timestamp;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String sync_key = intent.getStringExtra("sync_key");

        //Eigene Instanz initialisieren
        own_instance = this;

        //StorageUtils initialisieren
        su = new StorageUtils(getApplicationContext());

        //Firebase initialisieren
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        ref = db.getReference("sync/" + sync_key);

        refresh(getApplicationContext());

        return START_NOT_STICKY;
    }

    public void refresh(final Context context) {
        timestamp = System.currentTimeMillis();

        //Favoriten und eigene Sensoren aus der DB holen
        favourites = su.getAllFavourites();
        own_sensors = su.getAllOwnSensors();

        //Daten zusammensetzen
        final HashMap<String, Object> data = new HashMap<>();
        int object_id = 0;
        for(Sensor s : favourites) {
            HashMap<String, Object> sensor_map = new HashMap<>();
            sensor_map.put("name", s.getName());
            sensor_map.put("chip_id", s.getChipID());
            sensor_map.put("color", String.format("#%06X", (0xFFFFFF & s.getColor())));
            sensor_map.put("fav", true);
            data.put(String.valueOf(object_id), sensor_map);
            object_id++;
        }
        for(Sensor s : own_sensors) {
            HashMap<String, Object> sensor_map = new HashMap<>();
            sensor_map.put("name", s.getName());
            sensor_map.put("chip_id", s.getChipID());
            sensor_map.put("color", String.format("#%06X", (0xFFFFFF & s.getColor())));
            sensor_map.put("fav", false);
            data.put(String.valueOf(object_id), sensor_map);
            object_id++;
        }

        //Connection bauen
        HashMap<String, Object> connection = new HashMap<>();
        connection.put("time", timestamp);
        connection.put("device", "app");
        connection.put("data", data);
        ref.setValue(connection);

        //DataChange-Listener setzen
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if(snap.exists()) {
                    if(snap.child("device").getValue() != null && !snap.child("device").getValue().equals("app")) {
                        ArrayList new_sensors = (ArrayList) snap.child("data").getValue();
                        if(new_sensors != null && new_sensors.size() > 0) {
                            favourites = su.getAllFavourites();
                            own_sensors = su.getAllOwnSensors();
                            for(Sensor s : favourites) su.removeFavourite(s.getChipID(), true);
                            for(Sensor s : own_sensors) su.removeOwnSensor(s.getChipID(), true);
                            for(int i = 0; i < new_sensors.size(); i++) {
                                Map<String, Object> sensor = (Map<String, Object>) new_sensors.get(i);
                                //Daten extrahieren
                                String chip_id = String.valueOf(sensor.get("chip_id"));
                                String name = String.valueOf(sensor.get("name"));
                                boolean favorized = Boolean.parseBoolean(sensor.get("fav").toString());
                                String color = String.valueOf(sensor.get("color"));
                                if(!su.isFavouriteExisting(chip_id) && !su.isSensorExistingLocally(chip_id)) {
                                    if(favorized) {
                                        su.addFavourite(new Sensor(chip_id, name, Color.parseColor(color)), true);
                                    } else {
                                        su.addOwnSensor(new Sensor(chip_id, name, Color.parseColor(color)), true, true);
                                    }
                                }
                            }
                            MainActivity.own_instance.pager_adapter.refreshFavourites();
                            MainActivity.own_instance.pager_adapter.refreshMySensors();
                        }
                    }
                } else {
                    //ref.removeEventListener(childEventListener);
                    ref.removeEventListener(this);
                    //Toast anzeigen
                    LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                    Toast t = new Toast(context);
                    t.setGravity(Gravity.CENTER, 0, 0);
                    t.setDuration(Toast.LENGTH_LONG);
                    t.setView(inflater.inflate(R.layout.sync_ended, null));
                    t.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(context, "Sync failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void stop() {
        if(ref != null) ref.removeValue();
        stopSelf();
    }
}
