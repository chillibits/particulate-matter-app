package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationUtils {
    //Priorities
    public static final int PRIORITY_HIGH = 1;
    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_LOW = -1;
    //Vibrations
    public static final int VIBRATION_SHORT = 300;
    //Lights
    public static final int LIGHT_SHORT = 500;

    //Variablen als Objekte
    private Context context;
    private NotificationManager nm;
    private Resources res;

    //Variablen

    //Konstruktor
    public NotificationUtils(Context context) {
        this.context = context;
        res = context.getResources();
        nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

    public static void createNotificationChannels(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            //System-Channel
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel_system = new NotificationChannel(Constants.CHANNEL_SYSTEM, context.getString(R.string.nc_system_name), importance);
            channel_system.setDescription(context.getString(R.string.nc_system_description));
            notificationManager.createNotificationChannel(channel_system);
            //Limit-Channel
            importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel_limit = new NotificationChannel(Constants.CHANNEL_LIMIT, context.getString(R.string.nc_limit_name), importance);
            channel_limit.setDescription(context.getString(R.string.nc_limit_description));
            notificationManager.createNotificationChannel(channel_limit);
            //MissingMeasurements-Channel
            importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel_missing_measurements = new NotificationChannel(Constants.CHANNEL_MISSING_MEASUREMENTS, context.getString(R.string.nc_missing_measurements_name), importance);
            channel_missing_measurements.setDescription(context.getString(R.string.nc_missing_measurements_description));
            notificationManager.createNotificationChannel(channel_missing_measurements);
        }
    }

    public void displayLimitExceededNotification(String message, String chip_id, long time) {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra("ChipID", chip_id);
        displayNotification(Constants.CHANNEL_LIMIT, res.getString(R.string.limit_exceeded), message, Integer.parseInt(chip_id), i, PRIORITY_HIGH, LIGHT_SHORT, new long[]{0, VIBRATION_SHORT, VIBRATION_SHORT, VIBRATION_SHORT}, time);
    }

    public void displayMissingMeasurementsNotification(String chip_id, String sensor_name) {
        Intent i = new Intent(context, MainActivity.class);
        i.putExtra("ChipID", chip_id);
        displayNotification(Constants.CHANNEL_MISSING_MEASUREMENTS, res.getString(R.string.sensor_breakdown), sensor_name + " (" + chip_id + ")", Integer.parseInt(chip_id) * 10, i, PRIORITY_HIGH, LIGHT_SHORT, new long[]{0, VIBRATION_SHORT, VIBRATION_SHORT, VIBRATION_SHORT}, System.currentTimeMillis());
    }

    public void displayNotification(String channel_id, String title, String message, int id, Intent i, int priority, int light_lenght, long[] vibration, long time) {
        //Notification aufbauen
        NotificationCompat.Builder n = buildNotification(title, message);
        n.setAutoCancel(true);
        n.setSmallIcon(R.drawable.notification_icon);
        n.setWhen(time);
        if(i != null) {
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            n.setContentIntent(pi);
        }
        //ID ermitteln
        if(id == 0) id = (int) ((Math.random()) * Integer.MAX_VALUE);
        if(priority == PRIORITY_HIGH) {
            n.setPriority(NotificationCompat.PRIORITY_HIGH);
            n.setLights(res.getColor(R.color.colorPrimary), light_lenght, light_lenght);
            n.setVibrate(vibration);
        } else if(priority == PRIORITY_NORMAL) {
            n.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        } else if(priority == PRIORITY_LOW) {
            n.setPriority(NotificationCompat.PRIORITY_LOW);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) n.setChannelId(channel_id);
        nm.notify(id, n.build());
    }

    public NotificationCompat.Builder buildNotification(String title, String message) {
        return new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(res.getColor(R.color.colorPrimary));
    }

    public void cancelNotification(int id) {
        nm.cancel(id);
    }
}