package com.mrgames13.jimdo.feinstaubapp.CommonObjects;

import android.support.annotation.NonNull;

import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;

import java.util.Date;

public class DataRecord implements Comparable {

    //Konstanten

    //Variablen als Objekte

    //Variablen
    private Date date_time;
    private Double sdsp1;
    private Double sdsp2;
    private Double temp;
    private Double humidity;
    private Double pressure;

    public DataRecord(Date time, Double sdsp1, Double sdsp2, Double temp, Double humidity, Double pressure) {
        this.date_time = time;
        this.sdsp1 = sdsp1;
        this.sdsp2 = sdsp2;
        this.temp = temp;
        this.humidity = humidity;
        this.pressure = pressure;
    }

    public void setDateTime(Date date_time) {
        this.date_time = date_time;
    }
    public Date getDateTime() {
        return date_time;
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

    public void setPressure(Double pressure) {
        this.pressure = pressure;
    }
    public Double getPressure() {
        return pressure;
    }

    @Override
    public int compareTo(@NonNull Object another) {
        try{
            DataRecord other_record = (DataRecord) another;

            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_ASC) return getDateTime().compareTo(other_record.getDateTime());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_DESC) return other_record.getDateTime().compareTo(getDateTime());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_ASC) return getSdsp1().compareTo(other_record.getSdsp1());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_DESC) return other_record.getSdsp1().compareTo(getSdsp1());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_ASC) return getSdsp2().compareTo(other_record.getSdsp2());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_DESC) return other_record.getSdsp2().compareTo(getSdsp2());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_ASC) return getTemp().compareTo(other_record.getTemp());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TEMP_DESC) return other_record.getTemp().compareTo(getTemp());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_ASC) return getHumidity().compareTo(other_record.getHumidity());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_HUMIDITY_DESC) return other_record.getHumidity().compareTo(getHumidity());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_PRESSURE_ASC) return getPressure().compareTo(other_record.getPressure());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_PRESSURE_DESC) return other_record.getPressure().compareTo(getPressure());
        } catch (Exception e) {}
        return 0;
    }
}