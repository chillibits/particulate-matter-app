package com.mrgames13.jimdo.feinstaubapp.CommonObjects;

import java.io.Serializable;

import androidx.annotation.NonNull;

public class Sensor implements Comparable, Serializable {

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
    public String getChipID() {
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

    @Override
    public int compareTo(@NonNull Object o) {
        Sensor other = (Sensor) o;
        return getName().compareTo(other.getName());
    }
}