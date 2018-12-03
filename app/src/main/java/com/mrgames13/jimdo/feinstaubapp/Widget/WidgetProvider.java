package com.mrgames13.jimdo.feinstaubapp.Widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.mrgames13.jimdo.feinstaubapp.HelpClasses.Constants;
import com.mrgames13.jimdo.feinstaubapp.R;

public class WidgetProvider extends AppWidgetProvider {
    //Konstanten

    //Variablen als Objekte

    //Variablen



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        if(intent.hasExtra(Constants.WIDGET_UPDATE_DATA)) {
            rv.setTextViewText(R.id.cv_p1, intent.getStringExtra(Constants.WIDGET_EXTRA_P1));
            rv.setTextViewText(R.id.cv_p2, intent.getStringExtra(Constants.WIDGET_EXTRA_P2));
            rv.setTextViewText(R.id.cv_temp, intent.getStringExtra(Constants.WIDGET_EXTRA_TEMP));
            rv.setTextViewText(R.id.cv_humidity, intent.getStringExtra(Constants.WIDGET_EXTRA_HUMIDITY));
            rv.setTextViewText(R.id.cv_pressure, intent.getStringExtra(Constants.WIDGET_EXTRA_PRESSURE));
            rv.setTextViewText(R.id.cv_time, intent.getStringExtra(Constants.WIDGET_EXTRA_TIME));
            update(context, rv);
        }
    }

    private void update(Context context, RemoteViews rv) {
        ComponentName widget_component = new ComponentName(context, WidgetProvider.class);
        AppWidgetManager.getInstance(context).updateAppWidget(widget_component, rv);
    }
}
