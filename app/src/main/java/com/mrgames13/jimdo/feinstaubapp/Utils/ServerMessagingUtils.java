package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.mrgames13.jimdo.feinstaubapp.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerMessagingUtils {

    //Konstanten
    private final String REPOSITORY_URL = "https://www.madavi.de/sensor/csvfiles.php";
    private final String DATA_URL = "https://www.madavi.de/sensor/data";

    //Variablen als Objekte
    private Resources res;
    private Context context;
    private ConnectivityManager cm;
    private WifiManager wifiManager;

    //Utils-Pakete
    private StorageUtils su;

    //Variablen
    int byteCounter;

    public ServerMessagingUtils(Context context, StorageUtils su) {
        this.context = context;
        this.res = context.getResources();
        this.su = su;
        this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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
            File file = new File(dir, sensor_id + file_name);
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

    public long getLastModified(String date, String sensor_id) {
        try {
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            date = format.format(newDate);

            String file_name = date + ".csv";

            URL url = new URL(DATA_URL + "/data-esp8266-" + sensor_id + "-" + file_name);
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