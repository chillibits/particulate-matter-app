package com.mrgames13.jimdo.feinstaubapp.Utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;

import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationUtils {
    //Konstanten
        //Priorities
        public final int PRIORITY_MAX = 2;
        public final int PRIORITY_HIGH = 1;
        public final int PRIORITY_NORMAL = 0;
        public final int PRIORITY_LOW = -1;
        public final int PRIORITY_MIN = -2;
        //Vibrations
        public final int VIBRATION_SHORT = 300;
        public final int VIBRATION_LONG = 600;
        //Lights
        public final int LIGHT_SHORT = 500;
        public final int LIGHT_LONG = 1000;


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

    public void displayLimitExceededNotification(String title, String message, int id, long time) {
        displayNotification(title, message, id, new Intent(context, MainActivity.class), PRIORITY_NORMAL, LIGHT_SHORT, new long[]{0, VIBRATION_SHORT, VIBRATION_SHORT, VIBRATION_SHORT}, time);
    }

    public void displayNotification(String title, String message, int id, Intent i, int priority, int light_lenght, long[] vibration, long time) {
        //Notification aufbauen
        NotificationCompat.Builder n = buildNotification(title, message);
        n.setAutoCancel(true);
        n.setWhen(time);
        if(i != null) {
            PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
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
        nm.notify(id, n.build());
    }

    public void clearNotification(int id) {
        nm.cancel(id);
    }

    public void clearNotifications() {
        nm.cancelAll();
    }

    private NotificationCompat.Builder buildNotification(String title, String message) {
        return new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(res.getColor(R.color.colorPrimary));
    }
}