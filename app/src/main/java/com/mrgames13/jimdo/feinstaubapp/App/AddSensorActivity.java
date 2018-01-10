package com.mrgames13.jimdo.feinstaubapp.App;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
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
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import net.margaritov.preference.colorpicker.ColorPickerDialog;

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

        random = new Random();
        current_color = Color.rgb(random.nextInt(255), random.nextInt(255), random.nextInt(255));
        iv_color.setColorFilter(current_color, PorterDuff.Mode.SRC);

        name = findViewById(R.id.sensor_name_value);
        id = findViewById(R.id.sensor_id_value);

        //Intent-Extras auslesen
        Intent i = getIntent();
        if(i.hasExtra("Mode") && i.getIntExtra("Mode", MODE_NEW) == MODE_EDIT) {
            mode = MODE_EDIT;
            name.setText(i.getStringExtra("Name"));
            id.setText(i.getStringExtra("ID"));
            id.setEnabled(false);
            current_color = i.getIntExtra("Color", current_color);
            iv_color.setColorFilter(current_color, PorterDuff.Mode.SRC);
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
            String sensor_id = this.id.getText().toString().trim();
            String sensor_name = name.getText().toString().trim();
            if(!sensor_id.equals("") && !sensor_name.equals("")) {
                if(sensor_id.length() == 7) {
                    if(mode == MODE_NEW) {
                        if(!su.isSensorExisting(sensor_id)) {
                            //Neuen Sensor speichern
                            su.addSensor(new Sensor(sensor_id, sensor_name.toString(), current_color));
                        } else {
                            //Sensor ist bereits verknüpft
                            Toast.makeText(this, res.getString(R.string.sensor_existing), Toast.LENGTH_SHORT).show();
                        }
                    } else if(mode == MODE_EDIT) {
                        //Sensor aktualisieren
                        su.updateSensor(new Sensor(sensor_id, sensor_name.toString(), current_color));
                    }
                    MainActivity.own_instance.refresh();
                    finish();
                } else {
                    //Die Sensor-ID hat keine 7 Zeichen
                    Toast.makeText(this, res.getString(R.string.id_7_chars), Toast.LENGTH_SHORT).show();
                }
            } else {
                //Es sind nicht alle Felder ausgefüllt
                Toast.makeText(this, res.getString(R.string.not_all_filled), Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectNewColor() {
        color_picker = new ColorPickerDialog(AddSensorActivity.this, current_color);
        color_picker.setAlphaSliderVisible(false);
        color_picker.setHexValueEnabled(true);
        color_picker.setTitle(res.getString(R.string.choose_color));
        color_picker.setOnColorChangedListener(new ColorPickerDialog.OnColorChangedListener() {
            @Override
            public void onColorChanged(int color) {
                current_color = color;
                animateAppAndStatusBar(color);
                iv_color.setColorFilter(color, PorterDuff.Mode.SRC);
            }
        });
        color_picker.show();
    }

    private void animateAppAndStatusBar(final int toColor) {
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