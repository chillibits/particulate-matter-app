package com.mrgames13.jimdo.feinstaubapp.App;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.mrgames13.jimdo.feinstaubapp.R;
import com.mrgames13.jimdo.splashscreen.App.SplashScreenBuilder;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //SplashScreen anzeigen
        SplashScreenBuilder.getInstance(this)
                .setVideo(R.raw.splash_animation)
                .setImage(R.drawable.app_icon)
                .show();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == SplashScreenBuilder.SPLASH_SCREEN_FINISHED) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
