package com.mrgames13.jimdo.feinstaubapp.RecyclerViewAdapters;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.mrgames13.jimdo.feinstaubapp.App.AddSensorActivity;
import com.mrgames13.jimdo.feinstaubapp.App.MainActivity;
import com.mrgames13.jimdo.feinstaubapp.App.SensorActivity;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import eu.davidea.flipview.FlipView;

public class SensorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    //Konstanten
    public static final int MODE_FAVOURITES = 10001;
    public static final int MODE_OWN_SENSORS = 10002;
    private final int TYPE_ITEM = 10003;
    private final int TYPE_HEADER = 10004;

    //Variablen als Objekte
    private MainActivity activity;
    private Resources res;
    private ArrayList<Sensor> sensors;
    private ArrayList<Sensor> selected_sensors = new ArrayList<>();
    private ArrayList<ViewHolder> view_holders = new ArrayList<>();

    //Utils-Pakete
    private StorageUtils su;
    private ServerMessagingUtils smu;

    //Variablen
    private int mode;

    public SensorAdapter(MainActivity activity, ArrayList<Sensor> sensors, StorageUtils su, ServerMessagingUtils smu, int mode) {
        this.activity = activity;
        this.res = activity.getResources();
        this.sensors = sensors;
        this.su = su;
        this.smu = smu;
        this.mode = mode;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //Variablen als Objekte
        private FlipView item_icon;
        private TextView item_name;
        private TextView item_id;
        private ImageView item_warning;
        private ImageView item_more;

        //Variablen

        public ViewHolder(View itemView) {
            super(itemView);
            //Oberflächenkomponenten initialisieren
            item_icon = itemView.findViewById(R.id.item_icon);
            item_name = itemView.findViewById(R.id.item_name);
            item_id = itemView.findViewById(R.id.item_id);
            item_warning = itemView.findViewById(R.id.item_warning);
            item_more = itemView.findViewById(R.id.item_more);
        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {
        //Variablen als Objekte
        private TextView header_text;
        private ImageView header_close;

        //Variablen

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            //Oberflächenkomponenten initialisieren
            header_text = itemView.findViewById(R.id.header_text);
            header_close = itemView.findViewById(R.id.header_close);
        }
    }

    @Override
    public int getItemViewType(int pos) {
        return shallShowHeader() ? (pos == 0 ? TYPE_HEADER : TYPE_ITEM) : TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_HEADER) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_view_header, null);
            return new HeaderViewHolder(itemView);
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sensor, null);
            return new ViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int pos) {
        if(holder instanceof ViewHolder) {
            final ViewHolder h = (ViewHolder) holder;
            view_holders.add(h);
            //Daten befüllen
            final Sensor sensor = sensors.get(shallShowHeader() ? pos -1 : pos);

            h.item_icon.getFrontLayout().getBackground().setColorFilter(sensor.getColor(), android.graphics.PorterDuff.Mode.SRC_IN);
            h.item_name.setText(sensor.getName());
            h.item_id.setText(res.getString(R.string.chip_id) + " " + sensor.getId());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(selected_sensors.size() > 0) {
                        h.item_icon.flip(!h.item_icon.isFlipped());
                    } else {
                        Intent i = new Intent(activity, SensorActivity.class);
                        i.putExtra("Name", sensor.getName());
                        i.putExtra("ID", sensor.getId());
                        i.putExtra("Color", sensor.getColor());
                        activity.startActivity(i);
                    }
                }
            });
            h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    h.item_icon.flip(!h.item_icon.isFlipped());
                    return true;
                }
            });
            h.item_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    h.item_icon.flip(!h.item_icon.isFlipped());
                }
            });
            h.item_icon.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    h.item_icon.flip(!h.item_icon.isFlipped());
                    return true;
                }
            });
            h.item_icon.setOnFlippingListener(new FlipView.OnFlippingListener() {
                @Override
                public void onFlipped(FlipView flipView, boolean checked) {
                    if(checked) selected_sensors.add(sensor);
                    if(!checked) selected_sensors.remove(sensor);
                    h.itemView.setBackgroundColor(res.getColor(checked ? R.color.color_selection : R.color.transparent));
                    activity.updateSelectionMode();
                }
            });

            h.item_more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popup = new PopupMenu(activity, h.item_more);
                    popup.inflate(R.menu.menu_sensor_more);
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            int id = menuItem.getItemId();
                            if(id == R.id.action_sensor_data) {
                                Intent i = new Intent(activity, SensorActivity.class);
                                i.putExtra("Name", sensor.getName());
                                i.putExtra("ID", sensor.getId());
                                i.putExtra("Color", sensor.getColor());
                                activity.startActivity(i);
                            } else if(id == R.id.action_sensor_edit) {
                                Intent i = new Intent(activity, AddSensorActivity.class);
                                i.putExtra("Mode", AddSensorActivity.MODE_EDIT);
                                i.putExtra("Name", sensor.getName());
                                i.putExtra("ID", sensor.getId());
                                i.putExtra("Color", sensor.getColor());
                                if(mode == MODE_FAVOURITES) i.putExtra("Target", AddSensorActivity.TARGET_FAVOURITE);
                                activity.startActivity(i);
                            } else if(id == R.id.action_sensor_unlink) {
                                AlertDialog d = new AlertDialog.Builder(activity)
                                        .setCancelable(true)
                                        .setIcon(R.drawable.delete_red)
                                        .setTitle(R.string.unlink_sensor)
                                        .setMessage(R.string.really_unlink_sensor)
                                        .setNegativeButton(R.string.cancel,null)
                                        .setPositiveButton(R.string.unlink_sensor, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                //Sensor aus der Datenbank löschen
                                                if(mode == MODE_FAVOURITES) {
                                                    su.removeFavourite(sensor.getId());
                                                } else {
                                                    su.removeOwnSensor(sensor.getId());
                                                }
                                                activity.refresh();
                                            }
                                        })
                                        .create();
                                d.show();
                            } else if(id == R.id.action_sensor_properties) {
                                View v = activity.getLayoutInflater().inflate(R.layout.dialog_sensor_properties, null);
                                TextView sensor_name = v.findViewById(R.id.sensor_name_value);
                                TextView sensor_chip_id = v.findViewById(R.id.sensor_chip_id_value);
                                final TextView sensor_public = v.findViewById(R.id.sensor_public_value);
                                final TextView sensor_creation = v.findViewById(R.id.sensor_creation_value);
                                final TextView sensor_lat = v.findViewById(R.id.sensor_lat_value);
                                final TextView sensor_lng = v.findViewById(R.id.sensor_lng_value);
                                final TextView sensor_alt = v.findViewById(R.id.sensor_alt_value);

                                sensor_public.setSelected(true);
                                sensor_creation.setSelected(true);
                                sensor_lat.setSelected(true);
                                sensor_lng.setSelected(true);
                                sensor_alt.setSelected(true);

                                sensor_name.setText(sensor.getName());
                                sensor_chip_id.setText(sensor.getId());

                                AlertDialog d = new AlertDialog.Builder(activity)
                                        .setIcon(R.drawable.info_outline)
                                        .setTitle(R.string.properties)
                                        .setCancelable(true)
                                        .setView(v)
                                        .setPositiveButton(R.string.ok, null)
                                        .create();
                                d.show();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(smu.isInternetAvailable()) {
                                            try {
                                                String result = smu.sendRequest(null, "command=getsensorinfo&chip_id=" + URLEncoder.encode(sensor.getId(), "UTF-8"));
                                                if(!result.isEmpty()) {
                                                    JSONArray array = new JSONArray(result);
                                                    final JSONObject jsonobject = array.getJSONObject(0);

                                                    final DateFormat df = DateFormat.getDateInstance();
                                                    final Calendar c = Calendar.getInstance();
                                                    c.setTimeInMillis(jsonobject.getLong("creation_date") * 1000);
                                                    activity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            try{
                                                                sensor_public.setText(res.getString(R.string.yes));
                                                                sensor_creation.setText(df.format(c.getTime()));
                                                                sensor_lat.setText(String.valueOf(jsonobject.getDouble("lat")).replace(".", ","));
                                                                sensor_lng.setText(String.valueOf(jsonobject.getDouble("lng")).replace(".", ","));
                                                                sensor_alt.setText(String.valueOf(jsonobject.getDouble("alt")).replace(".", ",").concat(" m"));
                                                            } catch (Exception e) {
                                                                sensor_public.setText(res.getString(R.string.no));
                                                                sensor_creation.setText("-");
                                                                sensor_lat.setText("-");
                                                                sensor_lng.setText("-");
                                                                sensor_alt.setText("-");
                                                            }
                                                        }
                                                    });
                                                } else {
                                                    activity.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            sensor_public.setText(res.getString(R.string.no));
                                                            sensor_creation.setText("-");
                                                            sensor_lat.setText("-");
                                                            sensor_lng.setText("-");
                                                            sensor_alt.setText("-");
                                                        }
                                                    });
                                                }
                                            } catch (Exception e) {
                                                activity.runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        sensor_public.setText(res.getString(R.string.no));
                                                        sensor_creation.setText("-");
                                                        sensor_lat.setText("-");
                                                        sensor_lng.setText("-");
                                                        sensor_alt.setText("-");
                                                    }
                                                });
                                            }
                                        } else {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    sensor_public.setText("-");
                                                    sensor_creation.setText("-");
                                                    sensor_lat.setText("-");
                                                    sensor_lng.setText("-");
                                                    sensor_alt.setText("-");
                                                }
                                            });
                                        }
                                    }
                                }).start();
                            }
                            return true;
                        }
                    });
                    popup.show();
                }
            });

            h.item_more.setVisibility(su.isSensorExistingLocally(sensor.getId()) && mode == MODE_FAVOURITES ? View.GONE : View.VISIBLE);
            h.itemView.findViewById(R.id.item_own_sensor).setVisibility(su.isSensorExistingLocally(sensor.getId()) && mode == MODE_FAVOURITES ? View.VISIBLE : View.GONE);

            if(mode == MODE_OWN_SENSORS && !su.isSensorInOfflineMode(sensor.getId())) { // TODO: Diesen Teil beim nächsten Update entfernen
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String result = smu.sendRequest(null, "command=issensorexisting&chip_id=" + URLEncoder.encode(sensor.getId()));
                        if(result.equals("0")) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    h.item_warning.setVisibility(View.VISIBLE);
                                    h.item_warning.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent i = new Intent(activity, AddSensorActivity.class);
                                            i.putExtra("Mode", AddSensorActivity.MODE_COMPLETE);
                                            i.putExtra("Name", sensor.getName());
                                            i.putExtra("ID", sensor.getId());
                                            i.putExtra("Color", sensor.getColor());
                                            activity.startActivity(i);
                                        }
                                    });
                                }
                            });
                        }
                    }
                }).start();
            }
        } else if(holder instanceof HeaderViewHolder) {
            if(shallShowHeader()) {
                final HeaderViewHolder h = (HeaderViewHolder) holder;
                h.header_text.setText(res.getString(R.string.compare_instruction));
                h.header_close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        su.putBoolean("SensorViewHeader", false);
                        activity.refresh();
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return shallShowHeader() ? sensors.size() +1 : sensors.size();
    }

    private boolean shallShowHeader() {
        return su.getBoolean("SensorViewHeader", true);
    }

    public ArrayList<Sensor> getSelectedSensors() {
        return selected_sensors;
    }

    public void deselectAllSensors() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(final ViewHolder h : view_holders) {
                    try{
                        if(h.item_icon.isFlipped()) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    h.item_icon.flip(false);
                                }
                            });
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {}
                }
            }
        }).start();
    }
}