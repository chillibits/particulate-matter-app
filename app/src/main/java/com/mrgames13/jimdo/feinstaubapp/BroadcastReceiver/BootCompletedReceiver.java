package com.mrgames13.jimdo.feinstaubapp.BroadcastReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.Services.SyncService;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
	
	//Konstanten

    //Variablen als Objekte
    private StorageUtils su;

    //Variablen
    private int background_sync_frequency;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") || intent.getAction().equals("com.htc.intent.action.QUICKBOOT_POWERON")) {
		    //StorageUtils initialisieren
            su = new StorageUtils(context);
            background_sync_frequency = Integer.parseInt(su.getString("sync_cycle_background", String.valueOf(Constants.DEFAULT_SYNC_CYCLE_BACKGROUND))) * 1000 * 60;

            //Alarmmanager aufsetzen
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent start_service_intent = new Intent(context, SyncService.class);
            PendingIntent start_service_pending_intent = PendingIntent.getService(context, Constants.REQ_ALARM_MANAGER_BACKGROUND_SYNC, start_service_intent, 0);
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), background_sync_frequency, start_service_pending_intent);
		}
	}
}