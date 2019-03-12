package com.mrgames13.jimdo.feinstaubapp.CommonObjects;

import androidx.annotation.NonNull;

import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;

import java.util.Date;

public class DataRecord implements Comparable {

    //Konstanten

    //Variablen als Objekte

    //Variablen
    private Date date_time;
    private Double p1;
    private Double p2;
    private Double temp;
    private Double humidity;
    private Double pressure;
    private Double lat;
    private Double lng;
    private Double alt;

    public DataRecord(Date time, Double p1, Double p2, Double temp, Double humidity, Double pressure, Double lat, Double lng, Double alt) {
        this.date_time = time;
        this.p1 = p1;
        this.p2 = p2;
        this.temp = temp;
        this.humidity = humidity;
        this.pressure = pressure;
        this.lat = lat;
        this.lng = lng;
        this.alt = alt;
    }

    public Date getDateTime() {
        return date_time;
    }

    public Double getP1() {
        return p1;
    }

    public Double getP2() {
        return p2;
    }

    public Double getTemp() {
        return temp;
    }

    public Double getHumidity() {
        return humidity;
    }

    public Double getPressure() {
        return pressure;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public Double getAlt() {
        return alt;
    }

    @Override
    public int compareTo(@NonNull Object another) {
        try{
            DataRecord other_record = (DataRecord) another;

            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_ASC) return getDateTime().compareTo(other_record.getDateTime());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_TIME_DESC) return other_record.getDateTime().compareTo(getDateTime());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_ASC) return getP1().compareTo(other_record.getP1());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE1_DESC) return other_record.getP1().compareTo(getP1());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_ASC) return getP2().compareTo(other_record.getP2());
            if(SensorActivity.sort_mode == SensorActivity.SORT_MODE_VALUE2_DESC) return other_record.getP2().compareTo(getP2());
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