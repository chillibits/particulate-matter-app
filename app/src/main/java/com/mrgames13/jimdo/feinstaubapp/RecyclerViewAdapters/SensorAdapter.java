package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.lguipeng.library.animcheckbox.AnimCheckBox;
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;

import java.util.ArrayList;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.ViewHolderClass> {

    //Konstanten

    //Variablen als Objekte
    private MainActivity activity;
    private Resources res;
    private ArrayList<Sensor> sensors;
    private ArrayList<Sensor> selected_sensors = new ArrayList<>();

    //Variablen

    public SensorAdapter(MainActivity activity, ArrayList<Sensor> sensors) {
        this.activity = activity;
        this.res = activity.getResources();
        this.sensors = sensors;
    }

    public class ViewHolderClass extends RecyclerView.ViewHolder {
        //Variablen als Objekte
        private ImageView item_icon;
        private TextView item_name;
        private TextView item_id;
        private AnimCheckBox animCheckBox;


        public ViewHolderClass(View itemView) {
            super(itemView);
            //Oberflächenkomponenten initialisieren
            item_icon = itemView.findViewById(R.id.item_icon);
            item_name = itemView.findViewById(R.id.item_name);
            item_id = itemView.findViewById(R.id.item_id);
            animCheckBox = itemView.findViewById(R.id.item_select);
        }
    }

    @Override
    public SensorAdapter.ViewHolderClass onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor, null);
        return new SensorAdapter.ViewHolderClass(itemView);
    }

    @Override
    public void onBindViewHolder(final SensorAdapter.ViewHolderClass holder, final int pos) {
        //Daten befüllen
        final Sensor sensor = sensors.get(pos);

        holder.item_icon.setColorFilter(sensor.getColor(), android.graphics.PorterDuff.Mode.SRC_IN);
        holder.item_name.setText(sensor.getName());
        holder.item_id.setText(res.getString(R.string.sensor_id_) + " " + sensor.getId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.animCheckBox.setChecked(!holder.animCheckBox.isChecked(), true);
            }
        });
        holder.animCheckBox.setOnCheckedChangeListener(new AnimCheckBox.OnCheckedChangeListener() {
            @Override
            public void onChange(AnimCheckBox animCheckBox, boolean checked) {
                if(checked && selected_sensors.size() < 5) {
                    holder.itemView.setBackgroundColor(res.getColor(R.color.white_dark));
                    selected_sensors.add(sensor);
                    activity.updateToolbar(selected_sensors);
                } else {
                    holder.itemView.setBackgroundColor(res.getColor(R.color.transparent));
                    selected_sensors.remove(sensor);
                    activity.updateToolbar(selected_sensors);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }
}