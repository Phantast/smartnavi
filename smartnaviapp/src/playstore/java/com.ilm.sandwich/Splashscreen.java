package com.ilm.sandwich;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import io.fabric.sdk.android.Fabric;

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
    public static final int REQUEST_GOOGLE_PLAY_SERVICES = 1972;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(BuildConfig.debug){
            Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());
        }else{
            final Fabric fabric = new Fabric.Builder(this)
                    .kits(new Crashlytics())
                    .debuggable(true)
                    .build();
            Fabric.with(fabric);
            Fabric.with(this, new Answers());
        }
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
        } else if (api.isUserResolvableError(code) &&
                api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES)) {
            // wait for onActivityResult call (see below)
        } else {
            String str = GoogleApiAvailability.getInstance().getErrorString(code);
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
        }
    }

    private void startMap() {
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        if (settings.getString("MapSource", "GoogleMaps").equalsIgnoreCase("GoogleMaps")) {
            startActivity(new Intent(Splashscreen.this, GoogleMap.class));
        } else {
            startActivity(new Intent(Splashscreen.this, OsmMap.class));
        }
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
