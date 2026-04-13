package com.ilm.sandwich;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ilm.sandwich.sensors.Core;
import com.ilm.sandwich.tools.Config;
import com.ilm.sandwich.tools.ForegroundService;

/**
 * @author Christian Henke
 * https://smartnavi.app
 */
public class BackgroundService extends AppCompatActivity {

    // the following two are just that the following map (if user goes back) can get an actual position
    public static double sGeoLat;
    public static double sGeoLon;
    public static int steps = 0;
    static Location loc;
    static String mocLocationProvider;
    static String mocNetworkProvider;
    static LocationManager geoLocationManager;
    private FirebaseAnalytics mFirebaseAnalytics;
    Button serviceButton;
    private boolean shouldStart = true;
    private boolean pendingStart = false;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // Start regardless - the service works without the permission,
                // but the notification will only be visible if granted
                if (pendingStart) {
                    pendingStart = false;
                    starte();
                }
            });


    public static void pauseFakeProvider() {
        if (BuildConfig.debug)
            Log.i("Location-Status", "pause Fake Provider");
        try {
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
            geoLocationManager.removeTestProvider(mocLocationProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            geoLocationManager.setTestProviderEnabled(mocNetworkProvider, false);
            geoLocationManager.removeTestProvider(mocNetworkProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reactivateFakeProvider() {
        if (BuildConfig.debug)
            Log.i("Location-Status", "reactivate Fake Provider");
        try {
            mocLocationProvider = LocationManager.GPS_PROVIDER;
            geoLocationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 0, 5);
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mocNetworkProvider = LocationManager.NETWORK_PROVIDER;
            geoLocationManager.addTestProvider(mocNetworkProvider, false, false, false, false, false, false, false, 0, 5);
            geoLocationManager.setTestProviderEnabled(mocNetworkProvider, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    public static void newFakePosition() {
        // Write positions to variables so the map can pick them up when called again
        sGeoLat = Core.startLat;
        sGeoLon = Core.startLon;
        steps = Core.stepCounter;

        loc = new Location(mocLocationProvider);
        loc.setAccuracy(1);
        loc.setAltitude(Core.altitude);
        loc.setLatitude(Core.startLat);
        loc.setLongitude(Core.startLon);
        loc.setProvider(mocLocationProvider);
        loc.setSpeed(0.8f);
        loc.setBearing((float) Core.azimuth);
        loc.setTime(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            loc.setSpeedAccuracyMetersPerSecond(0.01f);
            loc.setBearingAccuracyDegrees(1);
            loc.setVerticalAccuracyMeters(1);
        }

        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        try {
            geoLocationManager.setTestProviderLocation(mocLocationProvider, loc);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Also feed the network provider so FusedLocationProviderClient
        // doesn't fall back to real network locations
        try {
            Location networkLoc = new Location(mocNetworkProvider);
            networkLoc.setLatitude(Core.startLat);
            networkLoc.setLongitude(Core.startLon);
            networkLoc.setAltitude(Core.altitude);
            networkLoc.setAccuracy(1);
            networkLoc.setTime(System.currentTimeMillis());
            networkLoc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            geoLocationManager.setTestProviderLocation(mocNetworkProvider, networkLoc);
        } catch (Exception e) {
            // Network provider mock may not be set up yet, ignore
        }

        if (BuildConfig.debug) {
            Log.i("Location-Status", "New Fake Position: " + loc.toString());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backgroundservice);

        // Set up custom top bar
        View topbarRoot = findViewById(R.id.topbar_root);
        TextView topbarTitle = findViewById(R.id.topbar_title);
        ImageButton topbarBack = findViewById(R.id.topbar_back);
        topbarTitle.setText(getResources().getString(R.string.tx_64));
        topbarBack.setOnClickListener(v -> {
            if (!shouldStart) {
                stop();
            } else {
                finish();
            }
        });

        // Handle edge-to-edge insets
        ViewCompat.setOnApplyWindowInsetsListener(topbarRoot, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            return windowInsets;
        });

        geoLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Sync state: if the service is already active (e.g. activity recreated), reflect that
        if (Config.backgroundServiceActive) {
            shouldStart = false;
        }

        // Reactivate sensors so step detection continues while this activity is in foreground
        try {
            GoogleMap.reactivateSensorsForBackgroundService();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unfortunately the background service is not supported on your device.", Toast.LENGTH_LONG).show();
            cleanupAndFinish();
            return;
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!shouldStart) {
                    stop();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        serviceButton = findViewById(R.id.button1);
        serviceButton.setOnClickListener(v -> {
            if (shouldStart) {
                startWithPermissionCheck();
            } else {
                stop();
            }
        });

        updateButtonText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sync state in case user returned from developer settings after enabling mock locations
        if (Config.backgroundServiceActive) {
            shouldStart = false;
        }
        updateButtonText();
    }

    private void updateButtonText() {
        if (shouldStart) {
            serviceButton.setText(getResources().getString(R.string.tx_74));
        } else {
            serviceButton.setText(getResources().getString(R.string.tx_69));
        }
    }

    private void startWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingStart = true;
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        starte();
    }

    public void starte() {
        try {
            mocLocationProvider = LocationManager.GPS_PROVIDER;
            mocNetworkProvider = LocationManager.NETWORK_PROVIDER;

            geoLocationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 1, 1);
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, true);

            // Also mock the network provider to prevent FusedLocationProviderClient
            // from falling back to real cell/WiFi locations
            try {
                geoLocationManager.addTestProvider(mocNetworkProvider, false, false, false, false, false, false, false, 1, 1);
                geoLocationManager.setTestProviderEnabled(mocNetworkProvider, true);
            } catch (Exception e) {
                if (BuildConfig.debug) Log.i("Location-Status", "Could not mock network provider: " + e.getMessage());
            }

            shouldStart = false;
            updateButtonText();
            Config.backgroundServiceActive = true;

            mFirebaseAnalytics.logEvent("BackgroundService_Start_Success", null);

            Intent intent = new Intent(this, ForegroundService.class);
            intent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);

        } catch (SecurityException sece) {
            int devSettingsEnabled = Settings.Secure.getInt(this.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
            if (devSettingsEnabled == 1) {
                mFirebaseAnalytics.logEvent("BackgroundService_Start_Err_DevEnabled", null);
                final Dialog dialog1 = new Dialog(BackgroundService.this);
                dialog1.setContentView(R.layout.dialog1);
                dialog1.setTitle(getResources().getString(R.string.tx_44));
                dialog1.setCancelable(true);
                dialog1.show();

                Button cancel2 = dialog1.findViewById(R.id.dialogCancelMock);
                cancel2.setOnClickListener(v -> dialog1.dismiss());

                Button settings2 = dialog1.findViewById(R.id.dialogSettingsMock);
                settings2.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    } catch (android.content.ActivityNotFoundException ae) {
                        ae.printStackTrace();
                    }
                    dialog1.dismiss();
                });
            } else {
                mFirebaseAnalytics.logEvent("BackgroundService_Start_Err_DevDisabled", null);
                final Dialog dialog2 = new Dialog(BackgroundService.this);
                dialog2.setContentView(R.layout.dialog2);
                dialog2.setTitle(getResources().getString(R.string.tx_104));
                dialog2.setCancelable(true);
                dialog2.show();

                Button cancel3 = dialog2.findViewById(R.id.dialogCancelDevSett);
                cancel3.setOnClickListener(v -> dialog2.dismiss());

                Button settings3 = dialog2.findViewById(R.id.dialogSettingsDevSett);
                settings3.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
                    } catch (android.content.ActivityNotFoundException ae2) {
                        ae2.printStackTrace();
                    }
                    dialog2.dismiss();
                });
            }
            updateButtonText();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        mFirebaseAnalytics.logEvent("BackgroundService_Stop", null);

        // Stop the foreground service
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);

        // Cancel any pending callbacks
        GoogleMap.cancelPendingCallbacks();

        Config.backgroundServiceActive = false;
        shouldStart = true;

        removeAllTestProviders();

        Toast.makeText(this, getResources().getString(R.string.tx_70), Toast.LENGTH_LONG).show();
        finish();
    }

    private void cleanupAndFinish() {
        removeAllTestProviders();
        finish();
    }

    private static void removeAllTestProviders() {
        try {
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            geoLocationManager.removeTestProvider(mocLocationProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            geoLocationManager.setTestProviderEnabled(mocNetworkProvider, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            geoLocationManager.removeTestProvider(mocNetworkProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
