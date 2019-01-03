package com.mrgames13.jimdo.feinstaubapp.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WebRealtimeSyncService extends Service {

    //Konstanten

    //Variablen als Objekte

    //Utils-Pakete
    private StorageUtils su;

    //Variablen

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String sync_key = intent.getStringExtra("sync_key");

        //StorageUtils initialisieren
        su = new StorageUtils(getApplicationContext());

        ArrayList<Sensor> favourites = su.getAllFavourites();
        ArrayList<Sensor> own_sensors = su.getAllOwnSensors();

        //Firebase initialisieren
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        final DatabaseReference ref = db.getReference("sync/" + sync_key);

        //Favoriten und eigene Sensoren in die DB schreiben
        final long timestamp = System.currentTimeMillis();

        //Daten zusammensetzen
        HashMap<String, Object> data = new HashMap<>();
        int object_id = 0;
        for(Sensor s : favourites) {
            HashMap<String, Object> sensor_map = new HashMap<>();
            sensor_map.put("name", s.getName());
            sensor_map.put("chip_id", s.getId());
            sensor_map.put("color", String.format("#%06X", (0xFFFFFF & s.getColor())));
            sensor_map.put("fav", true);
            data.put(String.valueOf(object_id), sensor_map);
            object_id++;
        }
        for(Sensor s : own_sensors) {
            HashMap<String, Object> sensor_map = new HashMap<>();
            sensor_map.put("name", s.getName());
            sensor_map.put("chip_id", s.getId());
            sensor_map.put("color", String.format("#%06X", (0xFFFFFF & s.getColor())));
            sensor_map.put("fav", false);
            data.put(String.valueOf(object_id), sensor_map);
            object_id++;
        }

        //Connection bauen
        HashMap<String, Object> connection = new HashMap<>();
        connection.put("time", timestamp);
        connection.put("data", data);
        ref.setValue(connection);

        //DataChange-Listener setzen
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if(snap.exists()) {
                    if(snap.child("time").getValue(Long.class) > timestamp) {
                        //Die Daten haben sich geändert
                        Toast.makeText(getApplicationContext(), "Data changed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    ref.removeEventListener(this);
                    //Toast anzeigen
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    Toast t = new Toast(getApplicationContext());
                    t.setGravity(Gravity.CENTER, 0, 0);
                    t.setDuration(Toast.LENGTH_LONG);
                    t.setView(inflater.inflate(R.layout.sync_ended, null));
                    t.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), "Sync failed", Toast.LENGTH_SHORT).show();
            }
        });

        stopSelf();
        return super.onStartCommand(intent, flags, startId);
    }


}
