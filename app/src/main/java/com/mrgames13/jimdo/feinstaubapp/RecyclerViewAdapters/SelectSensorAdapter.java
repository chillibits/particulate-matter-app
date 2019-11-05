/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.util.ArrayList;

import eu.davidea.flipview.FlipView;

public class SelectSensorAdapter extends RecyclerView.Adapter<SelectSensorAdapter.ViewHolder> {

    // Constants
    public static final int MODE_SELECTION_SINGLE = 10001;
    private static final int MODE_SELECTION_MULTI = 10002;

    // Utils packages
    private StorageUtils su;

    // Variables as objects
    private Resources res;
    private Handler h;
    private ArrayList<Sensor> sensors;
    private Sensor selected_sensor = null;
    private ArrayList<Sensor> selected_sensors = new ArrayList<>();
    private ViewHolder selected_sensor_holder;

    // Variables
    private int selection_mode;

    public SelectSensorAdapter(Context context, StorageUtils su, ArrayList<Sensor> sensors, int selection_mode) {
        this.su = su;
        this.res = context.getResources();
        this.h = new Handler();
        this.sensors = sensors;
        this.selection_mode = selection_mode;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        // Variables as objects
        private FlipView item_icon;
        private TextView item_name;
        private TextView item_id;

        ViewHolder(View itemView) {
            super(itemView);
            // Initialize UI components
            item_icon = itemView.findViewById(R.id.item_icon);
            item_name = itemView.findViewById(R.id.item_name);
            item_id = itemView.findViewById(R.id.item_id);
            itemView.findViewById(R.id.item_more).setVisibility(View.GONE);
        }

        private void deselect() {
            item_icon.flip(false);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor, parent, false);
        return new SelectSensorAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder h, int pos) {
        // Fill in data
        final Sensor sensor = sensors.get(pos);

        h.item_icon.getFrontLayout().getBackground().setColorFilter(sensor.getColor(), android.graphics.PorterDuff.Mode.SRC_IN);
        h.item_name.setText(sensor.getName());
        h.item_id.setText(res.getString(R.string.chip_id) + " " + sensor.getChipID());

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                h.item_icon.flip(!h.item_icon.isFlipped());
            }
        });
        h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                h.item_icon.flip(!h.item_icon.isFlipped());
                return true;
            }
        });
        h.item_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                h.item_icon.flip(!h.item_icon.isFlipped());
            }
        });
        h.item_icon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                h.item_icon.flip(!h.item_icon.isFlipped());
                return true;
            }
        });
        h.item_icon.setOnFlippingListener(new FlipView.OnFlippingListener() {
            @Override
            public void onFlipped(FlipView flipView, boolean checked) {
                if(checked) {
                    if(selection_mode == MODE_SELECTION_SINGLE) {
                        SelectSensorAdapter.this.h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(selected_sensor_holder != null) selected_sensor_holder.deselect();
                                selected_sensor_holder = h;
                            }
                        }, 50);
                        selected_sensor = sensor;
                    } else if(selection_mode == MODE_SELECTION_MULTI) {
                        selected_sensors.add(sensor);
                    }
                } else {
                    if(selection_mode == MODE_SELECTION_SINGLE) {
                        if(selected_sensor.getChipID().equals(sensor.getChipID())) {
                            selected_sensor = null;
                            selected_sensor_holder = null;
                        }
                    } else if(selection_mode == MODE_SELECTION_MULTI) {
                        selected_sensors.remove(sensor);
                    }
                }
                h.itemView.setBackgroundColor(res.getColor(checked ? R.color.color_selection : R.color.transparent));
            }
        });

        h.itemView.findViewById(R.id.item_own_sensor).setVisibility(su.isSensorExisting(sensor.getChipID()) ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    public Sensor getSelectedSensor() {
        return selected_sensor;
    }
}
