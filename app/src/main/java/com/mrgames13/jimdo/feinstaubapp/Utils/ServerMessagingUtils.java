package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerMessagingUtils {

    //Konstanten
    private final String REPOSITORY_URL = "https://www.madavi.de/sensor/csvfiles.php";
    private final String DATA_URL = "https://www.madavi.de/sensor/data";

    //Variablen als Objekte
    private Context context;
    private ConnectivityManager cm;
    private WifiManager wifiManager;

    //Utils-Pakete
    private StorageUtils su;

    //Variablen
    int byteCounter;

    public ServerMessagingUtils(Context context, StorageUtils su) {
        this.context = context;
        this.su = su;
        this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    public void manageDownloads(Sensor sensor, String date_string, String date_yesterday) {
        //Die CSV-Dateien für Tag und Tag davor herunterladen
        //Eingestellter Tag
        if(isCSVFileExisting(date_string, sensor.getId())) {
            //CSV-Datei existiert und kann heruntergeladen werden
            if(su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getCSVLastModified(sensor.getId(), date_string) > su.getCSVLastModified(sensor.getId(), date_string)) {
                //Lade CSV-Datei herunter
                Log.i("FA", "Downloading CSV1 ...");
                downloadCSVFile(date_string, sensor.getId());
            } else {
                //Die CSV-Datei wurde bereits in dieser Version heruntergeladen
                Log.i("FA", "No need to download CSV1");
            }
        } else {
            //CSV-Datei existiert nicht
            if(isZipFileExisting(date_string, sensor.getId())) {
                //Zip-Datei existiert und kann heruntergeladen werden
                if(su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getZipLastModified(sensor.getId(), date_string) > su.getZipLastModified(sensor.getId(), date_string)) {
                    Log.i("FA", "Downloading ZIP1 ...");
                    if(downloadZipFile(date_string, sensor.getId())) su.unpackZipFile(sensor.getId(), date_string);
                } else {
                    //Die Zip-Datei wurde bereits in dieser Version heruntergeladen
                    Log.i("FA", "No need to download ZIP1");
                }
            }
        }
        //Tag davor
        if(isCSVFileExisting(date_yesterday, sensor.getId())) {
            //CSV-Datei existiert und kann heruntergeladen werden
            if(su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getCSVLastModified(sensor.getId(), date_yesterday) > su.getCSVLastModified(sensor.getId(), date_yesterday)) {
                //Lade CSV-Datei herunter
                Log.i("FA", "Downloading CSV2 ...");
                downloadCSVFile(date_yesterday, sensor.getId());
            } else {
                //Die CSV-Datei wurde bereits in dieser Version heruntergeladen
                Log.i("FA", "No need to download CSV2");
            }
        } else {
            //CSV-Datei existiert nicht
            if(isZipFileExisting(date_yesterday, sensor.getId())) {
                //Zip-Datei existiert und kann heruntergeladen werden
                if(su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getZipLastModified(sensor.getId(), date_yesterday) > su.getZipLastModified(sensor.getId(), date_yesterday)) {
                    Log.i("FA", "Downloading ZIP2 ...");
                    if(downloadZipFile(date_yesterday, sensor.getId())) su.unpackZipFile(sensor.getId(), date_yesterday);
                } else {
                    //Die Zip-Datei wurde bereits in dieser Version heruntergeladen
                    Log.i("FA", "No need to download ZIP2");
                }
            }
        }
    }

    public boolean downloadCSVFile(String date, String sensor_id) {
        try {
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            String new_date = format.format(newDate);

            String file_name = new_date + ".csv";

            URL url = new URL(DATA_URL + "/data-esp8266-" + sensor_id + "-" + file_name);
            URLConnection connection = url.openConnection();
            connection.connect();
            //LastModified speichern
            su.putLong("LM_" + date + "_" + sensor_id, connection.getLastModified());
            //InputStream erstellen
            InputStream i = new BufferedInputStream(connection.getInputStream(), 1024);
            //Dateien initialisieren
            File dir = new File(context.getFilesDir(), "/SensorData");
            if(!dir.exists()) dir.mkdirs();
            File file = new File(dir, sensor_id + "-" + file_name);
            //FileOutputStreams erstellen
            OutputStream o = new FileOutputStream(file);
            //In Datei hineinschreiben
            byte[] buffer = new byte[1024];
            int read;
            byteCounter = 0;
            while((read = i.read(buffer)) != -1) {
                byteCounter+=read;
                o.write(buffer, 0, read);
            }
            //Streams schließen
            o.flush();
            o.close();
            i.close();

            return true;
        } catch (Exception e) {}
        return false;
    }

    public boolean downloadZipFile(String date, String sensor_id) {
        try {
            String month = date.substring(3, 5);
            String year = date.substring(6);

            URL url = new URL(DATA_URL + "/" + year + "/data-esp8266-" + sensor_id + "-" + year + "-" + month + ".zip");
            URLConnection connection = url.openConnection();
            connection.connect();
            //LastModified speichern
            su.putLong("LM_" + year + "_" + month + "_" + sensor_id + "_zip", connection.getLastModified());
            //InputStream erstellen
            InputStream i = new BufferedInputStream(connection.getInputStream(), 1024);
            //Dateien initialisieren
            File dir = new File(context.getFilesDir(), "/SensorData");
            if(!dir.exists()) dir.mkdirs();
            File file = new File(dir, sensor_id + "_" + year + "_" + month + ".zip");
            //FileOutputStreams erstellen
            OutputStream o = new FileOutputStream(file);
            //In Datei hineinschreiben
            byte[] buffer = new byte[1024];
            int read;
            byteCounter = 0;
            while((read = i.read(buffer)) != -1) {
                byteCounter+=read;
                o.write(buffer, 0, read);
            }
            //Streams schließen
            o.flush();
            o.close();
            i.close();

            return true;
        } catch (Exception e) {}
        return false;
    }

    public boolean isCSVFileExisting(String date, String sensor_id) {
        try{
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            String new_date = format.format(newDate);

            String url = DATA_URL + "/data-esp8266-" + sensor_id + "-" + new_date + ".csv";
            return isOnlineResourceExisting(url);
        } catch (Exception e) {}
        return false;
    }

    public boolean isZipFileExisting(String date, String sensor_id) {
        try{
            String month = date.substring(3, 5);
            String year = date.substring(6);
            String url = DATA_URL + "/" + year + "/data-esp8266-" + sensor_id + "-" + year + "-" + month + ".zip";
            return isOnlineResourceExisting(url);
        } catch (Exception e) {}
        return false;
    }

    public boolean isOnlineResourceExisting(String url) {
        try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public long getCSVLastModified(String sensor_id, String date) {
        try {
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            date = format.format(newDate);

            URL url = new URL(DATA_URL + "/data-esp8266-" + sensor_id + "-" + date + ".csv");
            URLConnection connection = url.openConnection();
            connection.connect();
            return connection.getLastModified();
        } catch (Exception e) {}
        return -1;
    }

    public long getZipLastModified(String sensor_id, String date) {
        try {
            String month = date.substring(3, 5);
            String year = date.substring(6);

            URL url = new URL(DATA_URL + "/" + year + "/data-esp8266-" + sensor_id + "-" + year + "-" + month + ".zip");
            URLConnection connection = url.openConnection();
            connection.connect();
            return connection.getLastModified();
        } catch (Exception e) {}
        return -1;
    }

    public boolean checkConnection(View v) {
        if(isInternetAvailable()) {
            return true;
        } else {
            Snackbar.make(v, context.getResources().getString(R.string.internet_is_not_available), Snackbar.LENGTH_LONG)
                    .setAction(R.string.activate_wlan, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            wifiManager.setWifiEnabled(true);
                        }
                    })
                    .show();
            return false;
        }
    }

    public boolean isInternetAvailable() {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }
}