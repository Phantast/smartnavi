package com.ilm.sandwich;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;


/**
 * This class is important because it checks if user has GooglePlayServices installed
 *
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Splashscreen extends Activity {

    public static final boolean PLAYSTORE_VERSION = true; //Used to differ between free and playStoreVersion
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1972;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splashscreen);


        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            Handler waitingTimer = new Handler();
            waitingTimer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startMap();
                }
            }, 1000);
        } else if (api.isUserResolvableError(code) && api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES)) {
            // wait for onActivityResult call (see below)
        } else {
            String str = GoogleApiAvailability.getInstance().getErrorString(code);
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
        }
    }

    private void startMap() {
        startActivity(new Intent(Splashscreen.this, GoogleMap.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    startMap();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
