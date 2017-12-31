package com.mrgames13.jimdo.feinstaubapp.CommonObjects;

import android.support.annotation.NonNull;

import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;

public class DataRecord implements Comparable {

    //Konstanten

    //Variablen als Objekte

    //Variablen
    private String time;
    private Double sdsp1;
    private Double sdsp2;
    private Double temp;
    private Double humidity;

    public DataRecord() {}

    public DataRecord(String time, Double sdsp1, Double sdsp2, Double temp, Double humidity) {
        this.time = time;
        this.sdsp1 = sdsp1;
        this.sdsp2 = sdsp2;
        this.temp = temp;
        this.humidity = humidity;
    }

    public void setTime(String time) {
        this.time = time;
    }
    public String getTime() {
        return time;
    }

    public void setSdsp1(Double sdsp1) {
        this.sdsp1 = sdsp1;
    }
    public Double getSdsp1() {
        return sdsp1;
    }

    public void setSdsp2(Double sdsp2) {
        this.sdsp2 = sdsp2;
    }
    public Double getSdsp2() {
        return sdsp2;
    }

    public void setTemp(Double temp) {
        this.temp = temp;
    }
    public Double getTemp() {
        return temp;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }
    public Double getHumidity() {
        return humidity;
    }

    @Override
    public int compareTo(@NonNull Object another) {
        try{
            DataRecord other_record = (DataRecord) another;

            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_ASC) {
                int other_int = Integer.parseInt(other_record.getTime().replace(":", ""));
                int current_int = Integer.parseInt(getTime().replace(":", ""));
                return other_int < current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_DESC) {
                int other_int = Integer.parseInt(other_record.getTime().replace(":", ""));
                int current_int = Integer.parseInt(getTime().replace(":", ""));
                return other_int > current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_ASC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getSdsp1()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getSdsp1()).replace(".", ""));
                return other_int < current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_DESC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getSdsp1()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getSdsp1()).replace(".", ""));
                return other_int > current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_ASC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getSdsp2()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getSdsp2()).replace(".", ""));
                return other_int < current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_DESC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getSdsp2()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getSdsp2()).replace(".", ""));
                return other_int > current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_ASC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getTemp()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getTemp()).replace(".", ""));
                return other_int < current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_DESC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getTemp()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getTemp()).replace(".", ""));
                return other_int > current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_ASC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getHumidity()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getHumidity()).replace(".", ""));
                return other_int < current_int ? 1 : -1;
            } else if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_DESC) {
                int other_int = Integer.parseInt(String.valueOf(other_record.getHumidity()).replace(".", ""));
                int current_int = Integer.parseInt(String.valueOf(getHumidity()).replace(".", ""));
                return other_int > current_int ? 1 : -1;
            }
        } catch (Exception e) {}
        return 0;
    }
}