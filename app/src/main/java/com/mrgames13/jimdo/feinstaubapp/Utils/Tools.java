/*
 * Copyright © 2019 Marc Auberer. All rights reserved.
 */

package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tools {

    //Konstanten

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

    public static String md5(final String s) {
        try {
            //Hash erstellen
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte[] messageDigest = digest.digest();

            //Hex-String erstellen
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2) h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {}
        return "";
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
            //Aktuellen und vorherigen Datensatz beschaffen
            DataRecord current_record = records.get(i);
            DataRecord record_before = records.get(i -1);
            //Auf Nullwerte überprüfen im Feinstaub überprüfen
            if(current_record.getP1() == 0 && current_record.getP2() == 0) {
                //Nächsten Datensatz beschaffen, der keine Nullwerte hat
                DataRecord record_after = record_before;
                for(int j = i +1; j < records.size(); j++) {
                    if(!((records.get(j).getTemp() == 0 && records.get(j).getHumidity() == 0) || (records.get(j).getTemp() == 0 && records.get(j).getPressure() == 0) || (records.get(j).getHumidity() == 0 && records.get(j).getPressure() == 0))) {
                        record_after = records.get(j);
                        break;
                    }
                }
                //Mittelwerte berechnen
                //PM10
                double m = Math.abs(record_before.getP1() - record_after.getP1()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                double b = record_before.getP1() - m * record_before.getDateTime().getTime();
                double avg_p1 = round(m * current_record.getDateTime().getTime() + b, 2);
                //PM2.5
                m = Math.abs(record_before.getP2() - record_after.getP2()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                b = record_before.getP2() - m * record_before.getDateTime().getTime();
                double avg_p2 = round(m * current_record.getDateTime().getTime() + b, 2);

                //Datensatz entsprechen ändern
                DataRecord new_record = new DataRecord(current_record.getDateTime(), avg_p1, avg_p2, current_record.getTemp(), current_record.getHumidity(), current_record.getPressure(), 0.0, 0.0, 0.0);
                records.set(i, new_record);
            }
            //Auf Nullwerte bei Temperatur, Luftfeuchtigkeit oder Luftdruck überprüfen
            if((current_record.getTemp() == 0 && current_record.getHumidity() == 0) || (current_record.getTemp() == 0 && current_record.getPressure() == 0) || (current_record.getHumidity() == 0 && current_record.getPressure() == 0)) {
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
                DataRecord new_record = new DataRecord(current_record.getDateTime(), current_record.getP1(), current_record.getP2(), avg_temp, avg_humidity, avg_pressure, 0.0, 0.0, 0.0);
                records.set(i, new_record);
            }
        }
        return records;
    }

    public static ArrayList<DataRecord> measurementCorrection2(final ArrayList<DataRecord> records) {
        /*for(int i = 2; i < records.size(); i++) {
            //Aktuellen und vorherigen Datensatz beschaffen
            DataRecord current_record = records.get(i);
            DataRecord record_before = records.get(i -1);
            DataRecord record_before2 = records.get(i -2);
            //PM10
            double deltaY1 = current_record.getP1() - record_before.getP1();
            double deltaY2 = record_before.getP1() - record_before2.getP1();
            double new_p1 = current_record.getP1();
            double new_p2 = current_record.getP2();
            //Auf Messfehler überprüfen
            if(current_record.getP1() > 30 && current_record.getP1() > record_before.getP1() * 3 && deltaY1 > deltaY2 * 3) {
                double threshold = (current_record.getP1() + record_before.getP1()) / 2;
                DataRecord record_after = record_before;
                for(int j = i +1; j < records.size(); j++) {
                    if(records.get(j).getP1() < threshold) {
                        record_after = records.get(j);
                        break;
                    }
                }
                //Gerade bilden
                double m = Math.abs(record_before.getP1() - record_after.getP1()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                double b = record_before.getP1() - m * record_before.getDateTime().getTime();
                new_p1 = round(m * current_record.getDateTime().getTime() + b, 2);
            }
            //PM2,5
            deltaY1 = current_record.getP2() - record_before.getP2();
            deltaY2 = record_before.getP2() - record_before2.getP2();
            if(current_record.getP2() > 20 && current_record.getP2() > current_record.getP2() * 3 && deltaY1 > deltaY2 * 3) {
                double threshold = (current_record.getP2() + record_before.getP2()) / 2;
                DataRecord record_after = record_before;
                for(int j = i +1; j < records.size(); j++) {
                    if(records.get(j).getP2() < threshold) {
                        record_after = records.get(j);
                        break;
                    }
                }
                //Gerade bilden
                double m = Math.abs(record_before.getP2() - record_after.getP2()) / (record_after.getDateTime().getTime() - record_before.getDateTime().getTime());
                double b = record_before.getP2() - m * record_before.getDateTime().getTime();
                new_p2 = round(m * current_record.getDateTime().getTime() + b, 2);
            }

            //Datensatz entsprechen ändern
            DataRecord new_record = new DataRecord(current_record.getDateTime(), new_p1, new_p2, current_record.getTemp(), current_record.getHumidity(), current_record.getPressure(), 0.0, 0.0, 0.0);
            records.set(i, new_record);
        }*/
        return records;
    }

    public static boolean isMeasurementBreakdown(StorageUtils su, ArrayList<DataRecord> records) {
        long measurement_interval = records.size() > 2 ? getMeasurementInteval(records) : 0;
        Log.d("FA", "Measurement interval: " + measurement_interval);
        if(measurement_interval <= 0) return false;
        return System.currentTimeMillis() > records.get(records.size() - 1).getDateTime().getTime() + measurement_interval * (Integer.parseInt(su.getString("notification_breakdown_number", String.valueOf(Constants.DEFAULT_MISSING_MEASUREMENT_NUMBER))) + 1);
    }

    private static long getMeasurementInteval(ArrayList<DataRecord> records) {
        ArrayList<Long> distances = new ArrayList<>();
        for(int i = 1; i < records.size(); i++) distances.add(records.get(i).getDateTime().getTime() - records.get(i -1).getDateTime().getTime());
        Collections.sort(distances);
        return distances.get(distances.size() / 2);
    }

    public static double findHighestMeasurement(ArrayList<DataRecord> records, int mode) {
        double highest = 0;
        if(mode == 1) {
            for(DataRecord r : records) highest = r.getP1() > highest ? r.getP1() : highest;
        } else if(mode == 2) {
            for(DataRecord r : records) highest = r.getP2() > highest ? r.getP2() : highest;
        } else if(mode == 3) {
            for(DataRecord r : records) highest = r.getTemp() > highest ? r.getTemp() : highest;
        } else if(mode == 4) {
            for(DataRecord r : records) highest = r.getHumidity() > highest ? r.getHumidity() : highest;
        } else if(mode == 5) {
            for(DataRecord r : records) highest = r.getPressure() > highest ? r.getPressure() : highest;
        }
        Log.d("FA", "Highest: " + highest);
        return highest;
    }

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static int pxToDp(int px) {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) result = context.getResources().getDimensionPixelSize(resourceId);
        return result;
    }

    public static int getNavigationBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) result = context.getResources().getDimensionPixelSize(resourceId);
        return result;
    }
}