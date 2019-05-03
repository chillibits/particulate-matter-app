package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

    //Konstanten

    //Variablen als Objekte
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    private ArrayList<ViewHolder> holders = new ArrayList<>();

    //Variablen
    private boolean show_gps_data;

    public class ViewHolder extends RecyclerView.ViewHolder {
        //Variablen als Objekte
        TextView item_time;
        TextView item_p1;
        TextView item_p2;
        TextView item_temp;
        TextView item_humidity;
        TextView item_pressure;
        TextView item_lat;
        TextView item_lng;
        TextView item_alt;
        LinearLayout item_gps_container;

        public ViewHolder(View itemView) {
            super(itemView);
            //Oberflächenkomponenten initialisieren
            item_time = itemView.findViewById(R.id.item_time);
            item_p1 = itemView.findViewById(R.id.item_p1);
            item_p2 = itemView.findViewById(R.id.item_p2);
            item_temp = itemView.findViewById(R.id.item_temp);
            item_humidity = itemView.findViewById(R.id.item_humidity);
            item_pressure = itemView.findViewById(R.id.item_pressure);
            item_lat = itemView.findViewById(R.id.item_lat);
            item_lng = itemView.findViewById(R.id.item_lng);
            item_alt = itemView.findViewById(R.id.item_alt);
            item_gps_container = itemView.findViewById(R.id.item_gps_container);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_data, null);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int pos) {
        //Daten befüllen
        try{
            DataRecord record = SensorActivity.records.get(pos);
            holder.item_time.setText(sdf.format(record.getDateTime()));
            holder.item_p1.setText(String.valueOf(Tools.round(record.getP1(), 1)).replace(".", ",").concat(" µg/m³"));
            holder.item_p2.setText(String.valueOf(Tools.round(record.getP2(), 1)).replace(".", ",").concat(" µg/m³"));
            holder.item_temp.setText(String.valueOf(record.getTemp()).replace(".", ",").concat(" °C"));
            holder.item_humidity.setText(String.valueOf(record.getHumidity()).replace(".", ",").concat(" %"));
            holder.item_pressure.setText(String.valueOf(Tools.round(record.getPressure(), 2)).replace(".", ",").concat(" hPa"));
            holder.item_lat.setText(String.valueOf(Tools.round(record.getLat(), 3)).concat(" °"));
            holder.item_lng.setText(String.valueOf(Tools.round(record.getLng(), 3)).concat(" °"));
            holder.item_alt.setText(String.valueOf(Tools.round(record.getAlt(), 1)).concat(" m"));
            holder.item_gps_container.setVisibility(show_gps_data ? View.VISIBLE : View.GONE);
            this.holders.add(holder);
        } catch (Exception e) {}
    }

    public void showGPSData(boolean show) {
        show_gps_data = show;
        for(ViewHolder h : holders) h.item_gps_container.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return SensorActivity.records == null ? 0 : SensorActivity.records.size();
    }
}