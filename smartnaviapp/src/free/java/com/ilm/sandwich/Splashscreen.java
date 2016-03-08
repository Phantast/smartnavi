package com.ilm.sandwich;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

/**
 * This class is for playStore Version important because it checks if user has GooglePlayServices installed
 * if yes: GoogleMap is opened
 * if not: OsmMapAcitivity is opened
 *
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class Splashscreen extends Activity {

    public static final boolean PLAYSTORE_VERSION = false; //Used to differ between free and playStoreVersion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splashscreen);

        Handler waitingTimer = new Handler();
        waitingTimer.postDelayed(new Runnable() {
            @Override
            public void run() {
                startMap();
            }
        }, 1000);
    }


    private void startMap() {
        startActivity(new Intent(Splashscreen.this, OsmMap.class));
        finish();
    }

}
