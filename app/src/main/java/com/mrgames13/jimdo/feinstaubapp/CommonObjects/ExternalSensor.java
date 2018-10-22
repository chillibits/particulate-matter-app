package com.mrgames13.jimdo.feinstaubapp.CommonObjects;

public class ExternalSensor {

    //Konstanten

    //Variablen als Objekte

    //Variablen
    private String chip_id = "no_id";
    private double lat = 0;
    private double lng = 0;

    public ExternalSensor() {}

    public ExternalSensor(String chip_id, double lat, double lng) {
        this.chip_id = chip_id;
        this.lat = lat;
        this.lng = lng;
    }

    public String getChipID() {
        return chip_id;
    }
    public void setChipID(String chip_id) {
        this.chip_id = chip_id;
    }

    public double getLat() {
        return lat;
    }
    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }
    public void setLng(double lng) {
        this.lng = lng;
    }
}
