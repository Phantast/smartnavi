package com.ilm.sandwich;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.ilm.sandwich.tools.Config;

/**
 * This class is important because it checks if user has GooglePlayServices installed
 * if yes: GoogleMap is opened
 * if not: OsmMapAcitivity is opened
 *
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class Splashscreen extends Activity {

    public static final boolean PLAYSTORE_VERSION = true; //Used to differ between free and playStoreVersion
    private boolean playServicesAvaliable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splashscreen);
        Config.startTime = System.currentTimeMillis();

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        if (status != ConnectionResult.SUCCESS) {
            playServicesAvaliable = false;
        } else {
            playServicesAvaliable = true;
        }

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
                startActivity(new Intent(Splashscreen.this, GoogleMap.class));
            } else {
                startActivity(new Intent(Splashscreen.this, OsmMap.class));
            }
        } else {
            startActivity(new Intent(Splashscreen.this, OsmMap.class));
        }
        finish();
    }

}
