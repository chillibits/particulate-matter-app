package com.mrgames13.jimdo.feinstaubapp.App;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.DataRecord;
import com.mrgames13.jimdo.feinstaubapp.CommonObjects.Sensor;
import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.feinstaubapp.Utils.ServerMessagingUtils;
import com.mrgames13.jimdo.feinstaubapp.Utils.StorageUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CompareActivity extends AppCompatActivity {

    //Konstanten

    //Variablen als Objekte
    private Resources res;
    private Toolbar toolbar;
    private Calendar calendar;
    private ArrayList<Sensor> sensors;
    private ArrayList<ArrayList<DataRecord>> records = new ArrayList<>();
    private LineGraphSeries<DataPoint> series_sdsp1;
    private LineGraphSeries<DataPoint> series_sdsp2;
    private LineGraphSeries<DataPoint> series_temp;
    private LineGraphSeries<DataPoint> series_humidity;
    private MenuItem progress_menu_item;

    //Utils-Pakete
    private StorageUtils su;
    private ServerMessagingUtils smu;

    //Komponenten
    private GraphView diagram_sdsp1;
    private GraphView diagram_sdsp2;
    private GraphView diagram_temp;
    private GraphView diagram_humidity;
    private DatePickerDialog date_picker_dialog;

    //Variablen
    private String current_date_string;
    private String date_string;
    private boolean no_data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        //Resourcen initialisieren
        res = getResources();

        //Toolbar initialisieren
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getIntent().hasExtra("Title")) getSupportActionBar().setTitle(getIntent().getStringExtra("Title"));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Kalender initialisieren
        calendar = Calendar.getInstance();
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        current_date_string = sdf.format(calendar.getTime());
        date_string = current_date_string;

        //StorageUtils initialisieren
        su = new StorageUtils(this);

        //ServerMessagingUtils initialisieren
        smu = new ServerMessagingUtils(this, su);

        //Sensoren laden
        sensors = MainActivity.own_instance.selected_sensors;

        //Komponenten initialisieren
        CardView card_date = findViewById(R.id.card_date);
        final TextView card_date_value = findViewById(R.id.card_date_value);
        ImageView card_date_edit = findViewById(R.id.card_date_edit);

        card_date_value.setText(date_string);
        card_date_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Datum auswählen
                date_picker_dialog = new DatePickerDialog(CompareActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);

                        date_string = sdf.format(calendar.getTime());
                        card_date_value.setText(date_string);

                        //Daten für ausgewähltes Datum laden
                        reloadData(true);
                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                date_picker_dialog.show();
            }
        });

        diagram_sdsp1 = findViewById(R.id.diagram_sdsp1);
        diagram_sdsp2 = findViewById(R.id.diagram_sdsp2);
        diagram_temp = findViewById(R.id.diagram_temp);
        diagram_humidity = findViewById(R.id.diagram_humidity);

        reloadData(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_sensor, menu);
        progress_menu_item = menu.findItem(R.id.action_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            finish();
        } else if(id == R.id.action_refresh) {
            Log.i("FA", "User refreshing ...");
            //Daten neu laden
            reloadData(true);
        } else if(id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private static ArrayList<DataRecord> fitArrayList(ArrayList<DataRecord> records) {
        int divider = records.size() / 200;
        if(divider == 0) return records;
        ArrayList<DataRecord> new_records = new ArrayList<>();
        for(int i = 0; i < records.size(); i+=divider+1) new_records.add(records.get(i));
        return new_records;
    }

    private void reloadData(final boolean from_server) {
        //ProgressMenuItem setzen
        if(progress_menu_item != null) progress_menu_item.setActionView(R.layout.menu_item_loading);

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setMessage(res.getString(R.string.loading_data));
        pd.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Diagramme leeren
                diagram_sdsp1.removeAllSeries();
                diagram_sdsp2.removeAllSeries();
                diagram_humidity.removeAllSeries();
                diagram_temp.removeAllSeries();

                no_data = true;
                for(Sensor s : sensors) {
                    if(from_server && smu.isInternetAvailable()) smu.downloadCSVFile(date_string, s.getId());

                    ArrayList<DataRecord> current_records = getDataRecordsFromCSV(su.getCSVFromFile(date_string, s.getId()));
                    if(current_records.size() > 0) {
                        no_data = false;
                        try{
                            SimpleDateFormat sdf_data = new SimpleDateFormat("HH:mm:ss");
                            long first_time = sdf_data.parse(current_records.get(0).getTime()).getTime() / 1000;

                            series_sdsp1 = new LineGraphSeries<>();
                            series_sdsp1.setColor(s.getColor());
                            for(DataRecord record : fitArrayList(current_records)) {
                                Date time = sdf_data.parse(record.getTime());
                                try{
                                    series_sdsp1.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getSdsp1()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_sdsp1.addSeries(series_sdsp1);

                            series_sdsp2 = new LineGraphSeries<>();
                            series_sdsp2.setColor(s.getColor());
                            for(DataRecord record : fitArrayList(current_records)) {
                                Date time = sdf_data.parse(record.getTime());
                                try{
                                    series_sdsp2.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getSdsp2()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_sdsp2.addSeries(series_sdsp2);

                            series_temp = new LineGraphSeries<>();
                            series_temp.setColor(s.getColor());
                            for(DataRecord record : fitArrayList(current_records)) {
                                Date time = sdf_data.parse(record.getTime());
                                try{
                                    series_temp.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getTemp()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_temp.addSeries(series_temp);

                            series_humidity = new LineGraphSeries<>();
                            series_humidity.setColor(s.getColor());
                            for(DataRecord record : fitArrayList(current_records)) {
                                Date time = sdf_data.parse(record.getTime());
                                try{
                                    series_humidity.appendData(new DataPoint(time.getTime() / 1000 - first_time, record.getHumidity()), false, 1000000);
                                } catch (Exception e) {}
                            }
                            diagram_humidity.addSeries(series_humidity);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.no_data).setVisibility(no_data ? View.VISIBLE : View.GONE);
                        findViewById(R.id.container).setVisibility(no_data ? View.GONE : View.VISIBLE);
                        //ProgressMenuItem zurücksetzen
                        if(progress_menu_item != null) progress_menu_item.setActionView(null);
                        pd.dismiss();
                    }
                });
            }
        }).start();
    }

    private ArrayList<DataRecord> getDataRecordsFromCSV(String csv_string) {
        try{
            ArrayList<DataRecord> records = new ArrayList<>();
            //In Zeilen aufspalten
            String[] lines = csv_string.split("\\r?\\n");
            for(int i = 1; i < lines.length; i++) {
                String time = "00:00";
                Double sdsp1 = 0.0;
                Double sdsp2 = 0.0;
                Double temp = 0.0;
                Double humidity = 0.0;
                //Zeile aufspalten
                String[] line_contents = lines[i].split(";");
                if(!line_contents[0].equals("")) time = line_contents[0].substring(line_contents[0].indexOf(" ") +1);
                if(!line_contents[7].equals("")) sdsp1 = Double.parseDouble(line_contents[7]);
                if(!line_contents[8].equals("")) sdsp2 = Double.parseDouble(line_contents[8]);
                if(!line_contents[9].equals("")) temp = Double.parseDouble(line_contents[9]);
                if(!line_contents[10].equals("")) humidity = Double.parseDouble(line_contents[10]);
                if(!line_contents[11].equals("")) temp = Double.parseDouble(line_contents[11]);
                if(!line_contents[12].equals("")) humidity = Double.parseDouble(line_contents[12]);

                //Unsere Zeitzone einstellen
                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
                SimpleDateFormat format_out = new SimpleDateFormat("HH:mm:ss");
                try { time = format_out.format(format.parse(time)); } catch (ParseException e) {}
                records.add(new DataRecord(time, sdsp1, sdsp2, temp, humidity));
            }
            return records;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}