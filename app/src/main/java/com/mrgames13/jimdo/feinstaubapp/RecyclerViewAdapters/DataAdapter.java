package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import java.text.SimpleDateFormat;

public class DataAdapter extends RecyclerView.Adapter<DataAdapter.ViewHolderClass> {

    //Konstanten

    //Variablen als Objekte
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    //Variablen

    public class ViewHolderClass extends RecyclerView.ViewHolder {
        //Variablen als Objekte
        TextView item_time;
        TextView item_sdsp1;
        TextView item_sdsp2;
        TextView item_temp;
        TextView item_humidity;
        TextView item_pressure;

        public ViewHolderClass(View itemView) {
            super(itemView);
            //Oberflächenkomponenten initialisieren
            item_time = itemView.findViewById(R.id.item_time);
            item_sdsp1 = itemView.findViewById(R.id.item_sdsp1);
            item_sdsp2 = itemView.findViewById(R.id.item_sdsp2);
            item_temp = itemView.findViewById(R.id.item_temp);
            item_humidity = itemView.findViewById(R.id.item_humidity);
            item_pressure = itemView.findViewById(R.id.item_pressure);
        }
    }

    @Override
    public ViewHolderClass onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_data, null);
        return new ViewHolderClass(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolderClass holder, final int pos) {
        //Daten befüllen
        DataRecord record = SensorActivity.records.get(pos);
        holder.item_time.setText(sdf.format(record.getDateTime()));
        holder.item_sdsp1.setText(String.valueOf(Tools.round(record.getSdsp1(), 1)).replace(".", ",") + " µg/m³");
        holder.item_sdsp2.setText(String.valueOf(Tools.round(record.getSdsp2(), 1)).replace(".", ",") + " µg/m³");
        holder.item_temp.setText(String.valueOf(record.getTemp()).replace(".", ",") + " °C");
        holder.item_humidity.setText(String.valueOf(record.getHumidity()).replace(".", ",") + " %");
        holder.item_pressure.setText(String.valueOf((int) Math.round(record.getPressure())).replace(".", ",") + " Pa");
    }

    @Override
    public int getItemCount() {
        return SensorActivity.records == null ? 0 : SensorActivity.records.size();
    }
}