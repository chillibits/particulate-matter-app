package com.mrgames13.jimdo.feinstaubapp.App;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.Tools;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

import java.util.HashMap;
import java.util.Random;

public class AddSensorActivity extends AppCompatActivity {

    //Konstanten
    private static final int REQ_SELECT_PLACE = 10001;
    public static final int MODE_NEW = 10001;
    public static final int MODE_EDIT = 10002;
    public static final int MODE_COMPLETE = 10003;
    public static final int TARGET_FAVOURITE = 10003;
    public static final int TARGET_OWN_SENSOR = 10004;

    //Variablen als Objekte
    private Resources res;
    private Toolbar toolbar;
    private ColorPickerDialog color_picker;
    private Random random;
    private View reveal_view;
    private View reveal_background_view;
    private ImageView iv_color;
    private EditText sensor_name;
    private EditText chip_id;
    private SwitchCompat sensor_public;
    private Button choose_location;
    private EditText lat;
    private EditText lng;
    private ImageView coordinates_info;
    private EditText alt;

    //Utils-Pakete
    private StorageUtils su;
    private ServerMessagingUtils smu;

    //Variablen
    private int current_color;
    private int mode = MODE_NEW;
    private int target = TARGET_OWN_SENSOR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sensor);

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(res.getString(R.string.add_own_sensor));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(this, su);

        //RevealView initialisieren
        reveal_view = findViewById(R.id.reveal);
        reveal_background_view = findViewById(R.id.reveal_background);

        //Komponenten initialisieren
        iv_color = findViewById(R.id.sensor_color);
        iv_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectNewColor();
            }
        });

        Button choose_color = findViewById(R.id.choose_sensor_color);
        choose_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectNewColor();
            }
        });

        //Zufallsgenerator initialisieren und zufällige Farbe ermitteln
        random = new Random();
        current_color = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        iv_color.setColorFilter(current_color, PorterDuff.Mode.SRC);

        sensor_name = findViewById(R.id.sensor_name_value);
        chip_id = findViewById(R.id.chip_id_value);

        ImageView info = findViewById(R.id.chip_id_info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(res.getString(R.string.link_id_info))));
            }
        });

        sensor_public = findViewById(R.id.sensor_public);
        sensor_public.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                choose_location.setEnabled(b);
                lat.setEnabled(b);
                lng.setEnabled(b);
                alt.setEnabled(b);
                coordinates_info.setEnabled(b);
            }
        });

        choose_location = findViewById(R.id.choose_location);
        choose_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                    startActivityForResult(builder.build(AddSensorActivity.this), REQ_SELECT_PLACE);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        lat = findViewById(R.id.lat);
        lng = findViewById(R.id.lng);
        alt = findViewById(R.id.height_value);

        coordinates_info = findViewById(R.id.coordinates_info);
        coordinates_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog d = new AlertDialog.Builder(AddSensorActivity.this)
                        .setCancelable(true)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.coordinates_info)
                        .setPositiveButton(R.string.ok, null)
                        .create();
                d.show();
            }
        });

        //Intent-Extras auslesen
        Intent i = getIntent();
        if(i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_EDIT) {
            mode = MODE_EDIT;
            sensor_name.setText(i.getStringExtra("Name"));
            chip_id.setText(i.getStringExtra("ID"));
            chip_id.setEnabled(false);
            current_color = i.getIntExtra("Color", current_color);
            iv_color.setColorFilter(current_color, PorterDuff.Mode.SRC);
            toolbar.setTitle(R.string.edit_sensor);
            sensor_public.setChecked(false);
            findViewById(R.id.additional_info).setVisibility(View.GONE);

            if(i.hasExtra("Target")) target = i.getIntExtra("Target", TARGET_OWN_SENSOR);

            findViewById(R.id.edit_position_info).setVisibility(View.VISIBLE);
            TextView info_text = findViewById(R.id.edit_position_info_text);
            info_text.setMovementMethod(LinkMovementMethod.getInstance());
        } else if(i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_COMPLETE) {
            mode = MODE_COMPLETE;
            sensor_name.setText(i.getStringExtra("Name"));
            chip_id.setText(i.getStringExtra("ID"));
            chip_id.setEnabled(false);
            current_color = i.getIntExtra("Color", current_color);
            iv_color.setColorFilter(current_color, PorterDuff.Mode.SRC);
            toolbar.setTitle(R.string.complete_sensor);
            choose_location.requestFocus();

            AlertDialog d = new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.complete_sensor)
                    .setMessage(R.string.sensor_position_completion_m_short)
                    .setPositiveButton(R.string.ok, null)
                    .create();
            d.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_add_own_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        } else if(id == R.id.action_done) {
            addSensor(item);
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectNewColor() {
        //Farb-Auswahl-Dialog anzeigen
        color_picker = new ColorPickerDialog(AddSensorActivity.this, current_color);
        color_picker.setAlphaSliderVisible(false);
        color_picker.setHexValueEnabled(true);
        color_picker.setTitle(res.getString(R.string.choose_color));
        color_picker.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                current_color = color;
                animateToolAndStatusBar(color);
                iv_color.setColorFilter(color, PorterDuff.Mode.SRC);
            }
        });
        color_picker.show();
    }

    private void animateToolAndStatusBar(final int toColor) {
        //Animation für die Toolbar und die Statusleiste anzeigen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Animator animator = ViewAnimationUtils.createCircularReveal(reveal_view, toolbar.getWidth() / 2, toolbar.getHeight() / 2, 0, toolbar.getWidth() / 2 + 50);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    reveal_view.setBackgroundColor(toColor);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    reveal_background_view.setBackgroundColor(toColor);
                }
            });

            animator.setDuration(480);
            animator.start();
            reveal_view.setVisibility(View.VISIBLE);
        } else {
            reveal_view.setBackgroundColor(toColor);
            reveal_background_view.setBackgroundColor(toColor);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SELECT_PLACE && resultCode == RESULT_OK) {
            Place place = PlacePicker.getPlace(this, data);
            lat.setText(String.valueOf(Tools.round(place.getLatLng().latitude, 5)));
            lng.setText(String.valueOf(Tools.round(place.getLatLng().longitude, 5)));
            choose_location.setText(place.getName());
        }
    }

    private void addSensor(MenuItem item) {
        final String chip_id = this.chip_id.getText().toString().trim();
        final String sensor_name = this.sensor_name.getText().toString().trim();
        final String lat = this.lat.getText().toString();
        final String lng = this.lng.getText().toString();
        final String alt = this.alt.getText().toString();

        if(!chip_id.isEmpty() && !sensor_name.isEmpty() && (!sensor_public.isChecked() || (!lat.isEmpty() && !lng.isEmpty() && !alt.isEmpty()))) {
            if(mode == MODE_NEW) {
                if(!su.isSensorExistingLocally(chip_id)) {
                    final ProgressDialog pd = new ProgressDialog(this);
                    pd.setMessage(res.getString(R.string.please_wait_));
                    pd.setCancelable(false);
                    pd.show();

                    if(smu.isInternetAvailable()) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //Prüfen, ob schon Daten des Sensors auf dem Server verfügbar sind
                                String result = smu.sendRequest(findViewById(R.id.container), new HashMap<String, String>() {{
                                    put("command", "issensordataexisting");
                                    put("chip_id", chip_id);
                                }});
                                pd.dismiss();
                                if(Boolean.parseBoolean(result)) {
                                    //ggf. Sensor auf dem Server hinzufügen
                                    if(sensor_public.isChecked()) {
                                        result = smu.sendRequest(null, new HashMap<String, String>() {{
                                            put("command", "addsensor");
                                            put("chip_id", chip_id);
                                            put("lat", String.valueOf(Tools.round(Double.parseDouble(lat), 3)));
                                            put("lng", String.valueOf(Tools.round(Double.parseDouble(lng), 3)));
                                            put("alt", alt);
                                        }});
                                        if(result.equals("1")) {
                                            //Neuen Sensor speichern
                                            if(su.isFavouriteExisting(chip_id)) su.removeFavourite(chip_id, false);
                                            su.addOwnSensor(new Sensor(chip_id, sensor_name, current_color), false, false);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try { MainActivity.own_instance.refresh(); } catch (Exception e) {}
                                                    finish();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    pd.dismiss();
                                                    Toast.makeText(AddSensorActivity.this, getString(R.string.error_try_again), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    } else {
                                        //Neuen Sensor speichern
                                        if(su.isFavouriteExisting(chip_id)) su.removeFavourite(chip_id, false);
                                        su.addOwnSensor(new Sensor(chip_id, sensor_name, current_color), true, false);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try { MainActivity.own_instance.refresh(); } catch (Exception e) {}
                                                finish();
                                            }
                                        });
                                    }
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AlertDialog d = new AlertDialog.Builder(AddSensorActivity.this)
                                                    .setCancelable(true)
                                                    .setTitle(R.string.app_name)
                                                    .setMessage(R.string.add_sensor_tick_not_set_message_duty)
                                                    .setPositiveButton(R.string.ok, null)
                                                    .create();
                                            d.show();
                                        }
                                    });
                                }
                            }
                        }).start();
                    } else {
                        pd.dismiss();
                        Toast.makeText(AddSensorActivity.this, getString(R.string.internet_is_not_available), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    //Sensor ist bereits verknüpft
                    Toast.makeText(this, res.getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show();
                }
            } else if(mode == MODE_EDIT) {
                //Sensor aktualisieren
                if(target == TARGET_FAVOURITE) {
                    su.updateFavourite(new Sensor(chip_id, sensor_name, current_color), false);
                } else {
                    su.updateOwnSensor(new Sensor(chip_id, sensor_name, current_color), false);
                }
                try{ MainActivity.own_instance.refresh(); } catch (Exception e) {}
                finish();
            } else if(mode == MODE_COMPLETE) {
                su.removeOwnSensor(chip_id, false);
                mode = MODE_NEW;
                onOptionsItemSelected(item);
            }
        } else {
            //Es sind nicht alle Felder ausgefüllt
            Toast.makeText(this, res.getString(R.string.not_all_filled), Toast.LENGTH_SHORT).show();
        }
    }
}