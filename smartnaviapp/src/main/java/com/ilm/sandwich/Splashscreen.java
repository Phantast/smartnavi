package com.ilm.sandwich;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;


/**
 * This class is important because it checks if user has GooglePlayServices installed
 *
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Splashscreen extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1000;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        com.ilm.sandwich.tools.DebugLogHelper.installCrashHandler(getApplicationContext());
        setContentView(R.layout.activity_splashscreen);

        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            View decorView = getWindow().getDecorView();
            decorView.postDelayed(() -> startMap(), SPLASH_DELAY_MS);
        } else {
            // Prompts user to install/update Google Play Services if possible
            api.makeGooglePlayServicesAvailable(this).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startMap();
                } else {
                    Toast.makeText(this, api.getErrorString(code), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void startMap() {
        startActivity(new Intent(Splashscreen.this, GoogleMap.class));
        finish();
    }

}
