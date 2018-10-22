package com.mrgames13.jimdo.feinstaubapp.Utils;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;

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
                if(s_old.getId().equals(s_new.getId())) continue outerloop;
            }
            sensors_new.add(s_new);
        }
        return sensors_new;
    }
}