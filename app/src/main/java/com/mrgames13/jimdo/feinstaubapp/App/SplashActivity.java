package com.mrgames13.jimdo.feinstaubapp.App;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.mrgames13.jimdo.feinstaubapp.HelpClasses.SimpleAnimationListener;
import com.mrgames13.jimdo.feinstaubapp.R;

public class SplashActivity extends AppCompatActivity {

    //Konstanten

    //Variablen als Objekte
    private RelativeLayout container;
    private VideoView animation;
    private ImageView app_icon;
    private TextView app_name;
    private TextView powered_by;
    private Handler handler;
    private Animation fade_in;

    //Variablen

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //Handler initialisieren
        handler = new Handler();

        //Komponenten initialisieren
        container = findViewById(R.id.container);
        container.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                animation.stopPlayback();
                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(i);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
                return true;
            }
        });
        animation = findViewById(R.id.app_icon_animation);
        app_icon = findViewById(R.id.app_icon);
        app_name = findViewById(R.id.app_title);
        powered_by = findViewById(R.id.app_powered);

        //FadeIn-Animation initialisieren
        fade_in = AnimationUtils.loadAnimation(SplashActivity.this, android.R.anim.fade_in);

        //VideoView initialisieren
        final Uri video_uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro);
        animation.setVideoURI(video_uri);
        animation.setDrawingCacheEnabled(true);
        animation.setZOrderOnTop(true);
        animation.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                animation.seekTo(0);
                animation.start();
                mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {
                        if(what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) app_icon.setVisibility(View.VISIBLE);
                        return false;
                    }
                });
            }
        });
        animation.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                animation.setVisibility(View.GONE);
                animation.stopPlayback();
                //Schriftzug langsam einblenden
                fade_in.setAnimationListener(new SimpleAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent i = new Intent(SplashActivity.this, MainActivity.class);
                                startActivity(i);
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                finish();
                            }
                        }, 1000);
                    }
                });
                app_name.startAnimation(fade_in);
                powered_by.startAnimation(fade_in);
                app_name.setVisibility(View.VISIBLE);
                powered_by.setVisibility(View.VISIBLE);

            }
        });
        animation.requestFocus();
    }
}
