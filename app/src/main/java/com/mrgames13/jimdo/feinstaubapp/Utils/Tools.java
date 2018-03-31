package com.mrgames13.jimdo.feinstaubapp.Utils;

import com.mrgames13.jimdo.feinstaubapp.App.Constants;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

public class Tools {

    //Konstanten

    //Variablen als Objekte

    //Variablen

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static ArrayList<DataRecord> fitArrayList(StorageUtils su, ArrayList<DataRecord> records) {
        if(!su.getBoolean("increase_diagram_performance", Constants.DEFAULT_FIT_ARRAY_LIST_ENABLED)) return records;
        int divider = records.size() / Constants.DEFAULT_FIT_ARRAY_LIST_CONSTANT;
        if(divider == 0) return records;
        ArrayList<DataRecord> new_records = new ArrayList<>();
        for(int i = 0; i < records.size(); i+=divider+1) new_records.add(records.get(i));
        return new_records;
    }
}