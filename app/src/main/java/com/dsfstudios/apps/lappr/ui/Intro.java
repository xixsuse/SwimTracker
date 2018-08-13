package com.dsfstudios.apps.lappr.ui;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;

import com.dsfstudios.apps.lappr.R;

public class Intro extends Activity {
    protected int _splashTime = 3000;

    private Thread splashTread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        splashTread = new Thread() {
            @Override
            public void run() {
                try {
                    synchronized (this) {
                        wait(_splashTime);
                    }

                } catch (InterruptedException e) {
                } finally {
                    finish();

                    Intent i = new Intent();
                    i.setClass(Intro.this, RecordWorkout.class);
                    startActivity(i);

                    //stop();
                }
            }
        };

        splashTread.start();
    }
}