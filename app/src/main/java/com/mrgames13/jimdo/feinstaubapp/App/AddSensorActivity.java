package com.mrgames13.jimdo.feinstaubapp.App;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

import java.net.URLEncoder;
import java.util.Random;

public class AddSensorActivity extends AppCompatActivity {

    //Konstanten
    public static final int MODE_NEW = 10001;
    public static final int MODE_EDIT = 10002;

    //Variablen als Objekte
    private Resources res;
    private Toolbar toolbar;
    private ColorPickerDialog color_picker;
    private Random random;
    private View reveal_view;
    private View reveal_background_view;
    private ImageView iv_color;
    private EditText name;
    private EditText id;

    //Utils-Pakete
    private StorageUtils su;
    private ServerMessagingUtils smu;

    //Variablen
    int current_color;
    int mode = MODE_NEW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_sensor);

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(res.getString(R.string.add_sensor));
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

        name = findViewById(R.id.sensor_name_value);
        id = findViewById(R.id.sensor_id_value);

        ImageView info = findViewById(R.id.sensor_id_info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(res.getString(R.string.link_id_info))));
            }
        });

        //Intent-Extras auslesen
        Intent i = getIntent();
        if(i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_EDIT) {
            mode = MODE_EDIT;
            name.setText(i.getStringExtra("Name"));
            id.setText(i.getStringExtra("ID"));
            id.setEnabled(false);
            current_color = i.getIntExtra("Color", current_color);
            iv_color.setColorFilter(current_color, PorterDuff.Mode.SRC);
            toolbar.setTitle(R.string.edit_sensor);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_add_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        } else if(id == R.id.action_done) {
            final String sensor_id = this.id.getText().toString().trim();
            final String sensor_name = name.getText().toString().trim();

            if(!sensor_id.equals("") && !sensor_name.equals("")) {
                if(mode == MODE_NEW) {
                    if(!su.isSensorExisting(sensor_id)) {
                        final ProgressDialog pd = new ProgressDialog(this);
                        pd.setMessage(res.getString(R.string.please_wait_));
                        pd.setCancelable(false);
                        pd.show();

                        //Neuen Sensor speichern
                        su.addSensor(new Sensor(sensor_id, sensor_name, current_color));
                        if(smu.isInternetAvailable()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String result = smu.sendRequest(findViewById(R.id.container), "command=issensorexisting&sensor_id=" + URLEncoder.encode(sensor_id));
                                    pd.dismiss();
                                    if(Boolean.parseBoolean(result)) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try{ MainActivity.own_instance.refresh(); } catch (Exception e) {}
                                                finish();
                                            }
                                        });
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                AlertDialog d = new AlertDialog.Builder(AddSensorActivity.this)
                                                        .setCancelable(false)
                                                        .setTitle(R.string.add_sensor)
                                                        .setMessage(R.string.add_sensor_tick_not_set_message)
                                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                                try{ MainActivity.own_instance.refresh(); } catch (Exception e) {}
                                                                finish();
                                                            }
                                                        })
                                                        .create();
                                                d.show();
                                            }
                                        });
                                    }
                                }
                            }).start();
                        } else {
                            try{ MainActivity.own_instance.refresh(); } catch (Exception e) {}
                            finish();
                        }
                    } else {
                        //Sensor ist bereits verknüpft
                        Toast.makeText(this, res.getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show();
                        try{ MainActivity.own_instance.refresh(); } catch (Exception e) {}
                        finish();
                    }
                } else if(mode == MODE_EDIT) {
                    //Sensor aktualisieren
                    su.updateSensor(new Sensor(sensor_id, sensor_name, current_color));
                    try{ MainActivity.own_instance.refresh(); } catch (Exception e) {}
                    finish();
                }
            } else {
                //Es sind nicht alle Felder ausgefüllt
                Toast.makeText(this, res.getString(R.string.not_all_filled), Toast.LENGTH_SHORT).show();
            }
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
}