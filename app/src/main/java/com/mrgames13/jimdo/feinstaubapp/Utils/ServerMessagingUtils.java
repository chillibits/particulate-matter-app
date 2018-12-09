package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerMessagingUtils {

    //Konstanten
    private final String SERVER_ADRESS_HTTP = "http://h2801469.stratoserver.net/";
    private final String SERVER_ADRESS_HTTPS = "https://h2801469.stratoserver.net/";
    private final String SERVER_MAIN_SCRIPT_HTTP = SERVER_ADRESS_HTTP + "ServerScript_v210.php";
    private final String SERVER_MAIN_SCRIPT_HTTPS = SERVER_ADRESS_HTTPS + "ServerScript_v210.php";
    private final String DATA_URL_HTTP = "http://h2801469.stratoserver.net/data";
    private final String DATA_URL_HTTPS = "https://h2801469.stratoserver.net/data";
    private final int MAX_REQUEST_REPEAT = 10;

    //Variablen als Objekte
    private Context context;
    private ConnectivityManager cm;
    private WifiManager wifiManager;
    private URL url;

    //Utils-Pakete
    private StorageUtils su;

    //Variablen
    private int repeat_count = 0;

    public ServerMessagingUtils(Context context, StorageUtils su) {
        this.context = context;
        this.su = su;
        this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //URL erstellen
        try { url = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? new URL(SERVER_MAIN_SCRIPT_HTTP) : new URL(SERVER_MAIN_SCRIPT_HTTPS); } catch (MalformedURLException e) {}
    }

    public String sendRequest(View v, final String param) {
        if(isInternetAvailable()) {
            try {
                //Connection aufbauen
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setFixedLengthStreamingMode(param.getBytes().length);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                //Anfrage senden
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                out.write(param);
                out.flush();
                out.close();
                //Antwort empfangen
                InputStream in = connection.getInputStream();
                String answer = getAnswerFromInputStream(in);
                //Connection schließen
                connection.disconnect();
                Log.i("FA", "Answer from Server: '" + answer + "'");
                //Antwort zurückgeben
                repeat_count = 0;
                return answer;
            } catch (IOException e) {
                e.printStackTrace();
                repeat_count++;
                return repeat_count <= MAX_REQUEST_REPEAT ? sendRequest(v, param) : "";
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if(v != null) checkConnection(v);
        }
        return "";
    }

    public void manageDownloads(Sensor sensor, String date_string, String date_yesterday) {
        //Die CSV-Dateien für Tag und Tag davor herunterladen
        //Eingestellter Tag
        if(isCSVFileExisting(date_string, sensor.getId())) {
            //CSV-Datei existiert und kann heruntergeladen werden
            if(!su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getCSVLastModified(sensor.getId(), date_string) > su.getCSVLastModified(sensor.getId(), date_string)) {
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
                if(!su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getZipLastModified(sensor.getId(), date_string) > su.getZipLastModified(sensor.getId(), date_string)) {
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
            if(!su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getCSVLastModified(sensor.getId(), date_yesterday) > su.getCSVLastModified(sensor.getId(), date_yesterday)) {
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
                if(!su.getBoolean("reduce_data_consumption", Constants.DEFAULT_REDUCE_DATA_CONSUMPTION) || getZipLastModified(sensor.getId(), date_yesterday) > su.getZipLastModified(sensor.getId(), date_yesterday)) {
                    Log.i("FA", "Downloading ZIP2 ...");
                    if(downloadZipFile(date_yesterday, sensor.getId())) su.unpackZipFile(sensor.getId(), date_yesterday);
                } else {
                    //Die Zip-Datei wurde bereits in dieser Version heruntergeladen
                    Log.i("FA", "No need to download ZIP2");
                }
            }
        }
    }

    public void downloadCSVFile(String date, String sensor_id) {
        try {
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            String new_date = format.format(newDate);

            String file_name = new_date + ".csv";

            URL url = new URL((Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? DATA_URL_HTTP : DATA_URL_HTTPS) + "/esp8266-" + sensor_id + "/data-" + file_name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            while((read = i.read(buffer)) != -1) {
                o.write(buffer, 0, read);
            }
            //Streams schließen
            o.flush();
            o.close();
            i.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean downloadZipFile(String date, String sensor_id) {
        try {
            String month = date.substring(3, 5);
            String year = date.substring(6);

            URL url = new URL((Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? DATA_URL_HTTP : DATA_URL_HTTPS) + "/esp8266-" + sensor_id + "/data-" + year + "-" + month + ".zip");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            while((read = i.read(buffer)) != -1) {
                o.write(buffer, 0, read);
            }
            //Streams schließen
            o.flush();
            o.close();
            i.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isCSVFileExisting(String date, String sensor_id) {
        try{
            //Datum umformatieren
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            Date newDate = format.parse(date);
            format = new SimpleDateFormat("yyyy-MM-dd");
            String new_date = format.format(newDate);

            String url = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? DATA_URL_HTTP : DATA_URL_HTTPS) + "/esp8266-" + sensor_id + "/data-" + new_date + ".csv";
            return isOnlineResourceExisting(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isZipFileExisting(String date, String sensor_id) {
        try{
            String month = date.substring(3, 5);
            String year = date.substring(6);
            String url = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? DATA_URL_HTTP : DATA_URL_HTTPS) + "/esp8266-" + sensor_id + "/data-" + year + "-" + month + ".zip";
            return isOnlineResourceExisting(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

            URL url = new URL((Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? DATA_URL_HTTP : DATA_URL_HTTPS) + "/esp8266-" + sensor_id + "/data-" + date + ".csv");
            URLConnection connection = url.openConnection();
            connection.connect();
            return connection.getLastModified();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public long getZipLastModified(String sensor_id, String date) {
        try {
            String month = date.substring(3, 5);
            String year = date.substring(6);

            URL url = new URL((Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? DATA_URL_HTTP : DATA_URL_HTTPS) + "/esp8266-" + sensor_id + "/data-" + year + "-" + month + ".zip");
            URLConnection connection = url.openConnection();
            connection.connect();
            return connection.getLastModified();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private String getAnswerFromInputStream(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();

        String currentLine;
        while((currentLine = reader.readLine()) != null) {
            sb.append(currentLine);
            sb.append("\n");
        }
        return sb.toString().replace("<br>", "").trim();
    }

    public boolean isInternetAvailable() {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    public boolean isConnectedWithWifi() {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting() && ni.getType() == 1;
    }
}