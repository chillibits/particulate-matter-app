package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Tools {

    //Konstanten
    private int CORRECTION_FACTOR = 5;

    //Variablen als Objekte

    //Variablen

    public static double round(double value, int places) {
        try{
            if (places < 0) throw new IllegalArgumentException();

            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } catch (Exception e) {}
        return value;
    }

    public static double calculateMedian(ArrayList<Double> records) {
        if(records.size() == 0) return 0;
        Collections.sort(records);
        return records.get(records.size() / 2);
    }

    public static ArrayList<DataRecord> fitArrayList(StorageUtils su, ArrayList<DataRecord> records) {
        if(!su.getBoolean("increase_diagram_performance", Constants.DEFAULT_FIT_ARRAY_LIST_ENABLED)) return records;
        int divider = records.size() / Constants.DEFAULT_FIT_ARRAY_LIST_CONSTANT;
        if(divider == 0) return records;
        ArrayList<DataRecord> new_records = new ArrayList<>();
        for(int i = 0; i < records.size(); i+=divider+1) new_records.add(records.get(i));
        return new_records;
    }

    public static ArrayList<Point> convertDataRecordsToPointsP1(ArrayList<DataRecord> records) {
        ArrayList<Point> result = new ArrayList<>();
        for(DataRecord r : records) result.add(new Point(r.getDateTime().getTime(), r.getP1()));
        return result;
    }
    public static ArrayList<DataRecord> convertPointsToDataRecordsP1(ArrayList<Point> points) {
        ArrayList<DataRecord> result = new ArrayList<>();
        for(Point p : points) {
            Date date = new Date();
            date.setTime(p.getX());
            result.add(new DataRecord(date, p.getY(), 0.0, 0.0, 0.0, 0.0));
        }
        return result;
    }

    public static ArrayList<Point> convertDataRecordsToPointsP2(ArrayList<DataRecord> records) {
        ArrayList<Point> result = new ArrayList<>();
        for(DataRecord r : records) result.add(new Point(r.getDateTime().getTime(), r.getP2()));
        return result;
    }
    public static ArrayList<DataRecord> convertPointsToDataRecordsP2(ArrayList<Point> points) {
        ArrayList<DataRecord> result = new ArrayList<>();
        for(Point p : points) {
            Date date = new Date();
            date.setTime(p.getX());
            result.add(new DataRecord(date, 0.0, p.getY(), 0.0, 0.0, 0.0));
        }
        return result;
    }

    public static ArrayList<Point> convertDataRecordsToPointsTemp(ArrayList<DataRecord> records) {
        ArrayList<Point> result = new ArrayList<>();
        for(DataRecord r : records) result.add(new Point(r.getDateTime().getTime(), r.getTemp()));
        return result;
    }
    public static ArrayList<DataRecord> convertPointsToDataRecordsTemp(ArrayList<Point> points) {
        ArrayList<DataRecord> result = new ArrayList<>();
        for(Point p : points) {
            Date date = new Date();
            date.setTime(p.getX());
            result.add(new DataRecord(date, 0.0, 0.0, p.getY(), 0.0, 0.0));
        }
        return result;
    }

    public static ArrayList<Point> convertDataRecordsToPointsHumidity(ArrayList<DataRecord> records) {
        ArrayList<Point> result = new ArrayList<>();
        for(DataRecord r : records) result.add(new Point(r.getDateTime().getTime(), r.getHumidity()));
        return result;
    }
    public static ArrayList<DataRecord> convertPointsToDataRecordsHumidity(ArrayList<Point> points) {
        ArrayList<DataRecord> result = new ArrayList<>();
        for(Point p : points) {
            Date date = new Date();
            date.setTime(p.getX());
            result.add(new DataRecord(date, 0.0, 0.0, 0.0, p.getY(), 0.0));
        }
        return result;
    }

    public static ArrayList<Point> convertDataRecordsToPointsPressure(ArrayList<DataRecord> records) {
        ArrayList<Point> result = new ArrayList<>();
        for(DataRecord r : records) result.add(new Point(r.getDateTime().getTime(), r.getPressure()));
        return result;
    }
    public static ArrayList<DataRecord> convertPointsToDataRecordsPressure(ArrayList<Point> points) {
        ArrayList<DataRecord> result = new ArrayList<>();
        for(Point p : points) {
            Date date = new Date();
            date.setTime(p.getX());
            result.add(new DataRecord(date, 0.0, 0.0, 0.0, 0.0, p.getY()));
        }
        return result;
    }

    public static ArrayList<Sensor> removeDuplicateSensors(ArrayList<Sensor> sensors) {
        ArrayList<Sensor> sensors_new = new ArrayList<>();
        outerloop:
        for(Sensor s_new : sensors) {
            for(Sensor s_old : sensors_new) {
                if(s_old.getChipID().equals(s_new.getChipID())) continue outerloop;
            }
            sensors_new.add(s_new);
        }
        return sensors_new;
    }

    public static LatLng getLocationFromAddress(Context context, String strAddress){
        Geocoder coder = new Geocoder(context);
        List<Address> address;

        try {
            address = coder.getFromLocationName(strAddress,5);
            if(address == null) return null;
            Address location = address.get(0);
            return new LatLng(location.getLatitude(), location.getLongitude());
        } catch (Exception e) {}
        return null;
    }

    public static ArrayList<DataRecord> measurementCorrection1(ArrayList<DataRecord> records) {
        for(int i = 1; i < records.size(); i++) {
            DataRecord current_record = records.get(i);
            //Auf Nullwerte überprüfen
            if((current_record.getTemp() == 0 && current_record.getHumidity() == 0) || (current_record.getTemp() == 0 && current_record.getPressure() == 0) || (current_record.getHumidity() == 0 && current_record.getPressure() == 0)) {
                //Vorherigen Datensatz beschaffen
                DataRecord record_before = records.get(i -1);
                //Nächsten Datensatz beschaffen, der keine Nullwerte hat
                DataRecord record_after = record_before;
                for(int j = i +1; j < records.size(); j++) {
                    if(!((records.get(j).getTemp() == 0 && records.get(j).getHumidity() == 0) || (records.get(j).getTemp() == 0 && records.get(j).getPressure() == 0) || (records.get(j).getHumidity() == 0 && records.get(j).getPressure() == 0))) {
                        record_after = records.get(j);
                        break;
                    }
                }
                //Mittelwerte berechnen
                //Temperatur
                double m = Math.abs(record_before.getTemp() - record_after.getTemp()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                double b = record_before.getTemp() - m * record_before.getDateTime().getTime();
                double avg_temp = round(m * current_record.getDateTime().getTime() + b, 2);
                //Luftfeuchtigkeit
                m = Math.abs(record_before.getHumidity() - record_after.getHumidity()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                b = record_before.getHumidity() - m * record_before.getDateTime().getTime();
                double avg_humidity = round(m * current_record.getDateTime().getTime() + b, 2);
                //Luftdruck
                m = Math.abs(record_before.getPressure() - record_after.getPressure()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                b = record_before.getPressure() - m * record_before.getDateTime().getTime();
                double avg_pressure = round(m * current_record.getDateTime().getTime() + b, 2);
                //Datensatz entsprechen ändern
                DataRecord new_record = new DataRecord(current_record.getDateTime(), current_record.getP1(), current_record.getP2(), avg_temp, avg_humidity, avg_pressure);
                records.set(i, new_record);
            }
        }
        return records;
    }

    public static int getMeasurementInteval(ArrayList<DataRecord> records) {
        int interval = 0;
        if(records.size() >= 3) {
            long interval1 = records.get(1).getDateTime().getTime() - records.get(0).getDateTime().getTime();
            long interval2 = records.get(2).getDateTime().getTime() - records.get(1).getDateTime().getTime();
            if(interval1 > interval2 - interval2 * (Constants.PERCENT_OF_VARIANCE_OF_MEASURING_INTERVAL / 100) && interval < interval2 + interval2 * (Constants.PERCENT_OF_VARIANCE_OF_MEASURING_INTERVAL / 100)) {
                //Intervall zwischen dem ersten Paar und dem zweiten Paar diferiert schwach oder ist gleich
                Log.d("FA", "Gleich");
            } else {
                //Intervall zwischen dem ersten Paar und dem zweiten Paar diferiert stark
                Log.d("FA", "Verschieden");
            }
        }
        return interval;
    }
}