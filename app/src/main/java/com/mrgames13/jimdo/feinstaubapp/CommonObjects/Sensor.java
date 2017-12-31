package com.mrgames13.jimdo.feinstaubapp.CommonObjects;

public class Sensor {

    //Konstanten

    //Variablen als Objekte

    //Variablen
    private String id = "no_id";
    private String name = "unknown";
    private int color = 0;

    public Sensor() {}

    public Sensor(String id, String name, int color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public void setColor(int color) {
        this.color = color;
    }
    public int getColor() {
        return color;
    }
}