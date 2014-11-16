package com.ilm.sandwich;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * This class is important because it checks if user has GooglePlayServices installed
 * if yes: GoogleMapActivity is opened
 * if not: OsmMapAcitivity is opened
 *
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class StartChooser extends Activity {

    private boolean playServicesAvaliable = true;
    private int surveyPopup = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.startchoose);

        Config.startTime = System.currentTimeMillis();

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        if (status != ConnectionResult.SUCCESS) {
            playServicesAvaliable = false;
        } else {
            playServicesAvaliable = true;
        }

        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        surveyPopup = settings.getInt("surveyPopup1", 0);

        Handler waitingTimer = new Handler();
        waitingTimer.postDelayed(new Runnable() {
            @Override
            public void run() {
                startMap();
            }
        }, 1000);
    }


    private void startMap() {
        if (playServicesAvaliable) {
            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
            if (settings.getString("MapSource", "GoogleMaps").equalsIgnoreCase("GoogleMaps")) {
                startActivity(new Intent(StartChooser.this, GoogleMapActivity.class));
            } else {
                startActivity(new Intent(StartChooser.this, OsmMapActivity.class));
            }
        } else {
            startActivity(new Intent(StartChooser.this, OsmMapActivity.class));
        }
        finish();
    }

}
