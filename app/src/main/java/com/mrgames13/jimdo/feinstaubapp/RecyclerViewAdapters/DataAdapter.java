package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import java.text.SimpleDateFormat;

import androidx.recyclerview.widget.RecyclerView;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolder> {

    //Konstanten

    //Variablen als Objekte
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    //Variablen

    public class ViewHolder extends RecyclerView.ViewHolder {
        //Variablen als Objekte
        TextView item_time;
        TextView item_p1;
        TextView item_p2;
        TextView item_temp;
        TextView item_humidity;
        TextView item_pressure;

        public ViewHolder(View itemView) {
            super(itemView);
            //Oberflächenkomponenten initialisieren
            item_time = itemView.findViewById(R.id.item_time);
            item_p1 = itemView.findViewById(R.id.item_p1);
            item_p2 = itemView.findViewById(R.id.item_p2);
            item_temp = itemView.findViewById(R.id.item_temp);
            item_humidity = itemView.findViewById(R.id.item_humidity);
            item_pressure = itemView.findViewById(R.id.item_pressure);
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
        DataRecord record = SensorActivity.records.get(pos);
        holder.item_time.setText(sdf.format(record.getDateTime()));
        holder.item_p1.setText(String.valueOf(Tools.round(record.getP1(), 1)).replace(".", ",").concat(" µg/m³"));
        holder.item_p2.setText(String.valueOf(Tools.round(record.getP2(), 1)).replace(".", ",").concat(" µg/m³"));
        holder.item_temp.setText(String.valueOf(record.getTemp()).replace(".", ",").concat(" °C"));
        holder.item_humidity.setText(String.valueOf(record.getHumidity()).replace(".", ",").concat(" %"));
        holder.item_pressure.setText(String.valueOf(Tools.round(record.getPressure(), 2)).replace(".", ",").concat(" kPa"));
    }

    @Override
    public int getItemCount() {
        return SensorActivity.records == null ? 0 : SensorActivity.records.size();
    }
}