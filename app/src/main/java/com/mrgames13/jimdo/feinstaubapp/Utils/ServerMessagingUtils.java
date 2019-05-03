package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ServerMessagingUtils {

    //Konstanten
    private static final String SERVER_ADRESS_HTTP = "http://h2801469.stratoserver.net/";
    private static final String SERVER_ADRESS_HTTPS = "https://h2801469.stratoserver.net/";
    private static final String SERVER_MAIN_SCRIPT_HTTP = SERVER_ADRESS_HTTP + "ServerScript_v310.php";
    private static final String SERVER_MAIN_SCRIPT_HTTPS = SERVER_ADRESS_HTTPS + "ServerScript_v310.php";
    private static final String SERVER_GET_SCRIPT_HTTP = SERVER_ADRESS_HTTP + "get.php";
    private static final String SERVER_GET_SCRIPT_HTTPS = SERVER_ADRESS_HTTPS + "get.php";
    private static final int MAX_REQUEST_REPEAT = 10;

    //Variablen als Objekte
    private Context context;
    private ConnectivityManager cm;
    private WifiManager wifiManager;
    private OkHttpClient client;
    private URL main_url;
    private URL get_url;

    //Utils-Pakete
    private StorageUtils su;

    //Variablen
    private int repeat_count = 0;

    public ServerMessagingUtils(Context context, StorageUtils su) {
        this.context = context;
        this.su = su;
        this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.client = new OkHttpClient();
        //URL erstellen
        try { main_url = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? new URL(SERVER_MAIN_SCRIPT_HTTP) : new URL(SERVER_MAIN_SCRIPT_HTTPS); } catch (MalformedURLException e) {}
        try { get_url = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT ? new URL(SERVER_GET_SCRIPT_HTTP) : new URL(SERVER_GET_SCRIPT_HTTPS); } catch (MalformedURLException e) {}
    }

    public String sendRequest(View v, final HashMap<String, String> params) {
        if(isInternetAvailable()) {
            try {
                MultipartBody.Builder body = new MultipartBody.Builder().setType(MultipartBody.FORM);
                for(String key : params.keySet()) body.addFormDataPart(key, params.get(key));
                Request request = new Request.Builder()
                        .url(main_url)
                        .post(body.build())
                        .build();
                try(Response response = client.newCall(request).execute()) {
                    return response.body().string();
                }
            } catch (IOException e) {
                e.printStackTrace();
                repeat_count++;
                return repeat_count <= MAX_REQUEST_REPEAT ? sendRequest(v, params) : "";
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if(v != null) checkConnection(v);
        }
        return "";
    }

    public ArrayList<DataRecord> manageDownloadsRecords(final String chip_id, final long from, final long to) {
        //Datensätze herunterladen
        if(isInternetAvailable()) {
            try {
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("id", chip_id)
                        .addFormDataPart("from", String.valueOf(from / 1000))
                        .addFormDataPart("to", String.valueOf(to / 1000))
                        .addFormDataPart("minimize", "true")
                        .addFormDataPart("gps", "true")
                        .build();
                Request request = new Request.Builder()
                        .url(get_url)
                        .post(body)
                        .build();
                String response = client.newCall(request).execute().body().string();
                //Datensätze parsen
                ArrayList<DataRecord> records = new ArrayList<>();
                if(!response.isEmpty() && response.startsWith("[") && response.endsWith("]")) {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject obj = array.getJSONObject(i);
                        Date time = new Date();
                        time.setTime(obj.getLong("time") * 1000);
                        DataRecord record = new DataRecord(time, obj.getDouble("p1"), obj.getDouble("p2"), obj.getDouble("t"), obj.getDouble("h"), obj.getDouble("p") / 100, obj.getDouble("la"), obj.getDouble("ln"), obj.getDouble("a"));
                        records.add(record);
                    }
                    su.saveRecords(chip_id, records);
                }
                return records;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
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