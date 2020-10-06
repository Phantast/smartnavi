package com.ilm.sandwich;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
    private FirebaseAnalytics mFirebaseAnalytics;
    static String mocLocationProvider;
    static LocationManager geoLocationManager;
    Button serviceButton;
    NotificationManagerCompat notificationManager;
    private boolean shouldStart = true;


    public static void pauseFakeProvider() {
        if (BuildConfig.debug)
            Log.i("Location-Status", "pause Fake Provider");
        try {
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
            geoLocationManager.removeTestProvider(mocLocationProvider);
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
    }

    @SuppressLint("NewApi")
    public static void newFakePosition() {
        // Positionen in Variablen schreiben, damit Karte die aufnehmen kann wenn es wieder aufgerufen wird
        sGeoLat = Core.startLat;
        sGeoLon = Core.startLon;
        steps = Core.stepCounter;
        //GPS
        loc = new Location(mocLocationProvider);
        loc.setAccuracy(1);
        loc.setAltitude(Core.altitude);
        loc.setLatitude(Core.startLat);
        loc.setLongitude(Core.startLon);
        loc.setProvider(mocLocationProvider);
        loc.setSpeed(0.8f);
        loc.setSpeedAccuracyMetersPerSecond(0.01f);
        loc.setBearing((float) Core.azimuth);
        loc.setBearingAccuracyDegrees(1);
        loc.setTime(System.currentTimeMillis());
        loc.setVerticalAccuracyMeters(1);
        try {
            try {
                loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            } catch (NoSuchMethodError e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            geoLocationManager.setTestProviderLocation(mocLocationProvider, loc);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (BuildConfig.debug) {
            Log.i("Location-Status", "New Fake Position: " + loc.toString());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backgroundservice);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getResources().getString(R.string.tx_64));
        geoLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //restart Sensors
        try {
            GoogleMap.listHandler.sendEmptyMessage(10);
        } catch (Exception e) {
            e.printStackTrace();
            //Bug for some devices, in that case: Abort and go back.
            Toast.makeText(this, "Unfortunately the background service is not supported on your device.", Toast.LENGTH_LONG).show();
            try {
                geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
            } catch (Exception e2) {
                    e2.printStackTrace();
            }
            try {
                geoLocationManager.removeTestProvider(mocLocationProvider);
            } catch (Exception e4) {
                e4.printStackTrace();
            }
            try {
                notificationManager = NotificationManagerCompat.from(this);
                notificationManager.cancelAll();
            } catch (Exception e5) {
                e5.printStackTrace();
            }
            finish();
        }
        serviceButton = findViewById(R.id.button1);
        serviceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shouldStart) {
                    starte();
                } else {
                    stop();
                    shouldStart = true;
                    serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
                }
            }
        });

        if (shouldStart) {
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
        } else {
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));
        }
    }


    public void starte() {
        try {
            mocLocationProvider = LocationManager.GPS_PROVIDER;

            geoLocationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 1, 1);
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, true);
            geoLocationManager.setTestProviderStatus(mocLocationProvider, 2, null, System.currentTimeMillis());

            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));

            shouldStart = false;
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));
            Config.backgroundServiceActive = true;

            mFirebaseAnalytics.logEvent("BackgroundService_Start_Success", null);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(this, ForegroundService.class);
                intent.setAction(ForegroundService.ACTION_START_FOREGROUND_SERVICE);
                this.startForegroundService(intent);
            } else {
                Intent intent = new Intent(this, BackgroundService.class);
                PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);

                notificationManager = NotificationManagerCompat.from(this);
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "21986938")
                        .setContentIntent(activity)
                        .setContentTitle(getApplicationContext().getResources().getString(R.string.tx_72))
                        .setContentText(getApplicationContext().getResources().getString(R.string.tx_73))
                        .setSmallIcon(R.drawable.ic_stat_maps_directions_walk)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setOngoing(true)
                        .setAutoCancel(false);

                /* Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = "SmartNavi";
                    String description = "SmartNavi Background Service Notifications";
                    int importance = NotificationManager.IMPORTANCE_DEFAULT;
                    NotificationChannel channel = new NotificationChannel("21986938", name, importance);
                    channel.setDescription(description);
                    // Register the channel with the system; you can't change the importance
                    // or other notification behaviors after this
                    NotificationManager notificationManager = getSystemService(NotificationManager.class);
                    notificationManager.createNotificationChannel(channel);
                }
                */
                notificationManager.notify(0, notificationBuilder.build());
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
                dialog1.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
                dialog1.setCancelable(true);
                dialog1.show();

                Button cancel2 = dialog1.findViewById(R.id.dialogCancelMock);
                cancel2.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        dialog1.dismiss();
                    }
                });

                Button settings2 = dialog1.findViewById(R.id.dialogSettingsMock);
                settings2.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        try {
                            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                        } catch (android.content.ActivityNotFoundException ae) {

                        }
                        dialog1.dismiss();
                    }
                });
            } else {
                mFirebaseAnalytics.logEvent("BackgroundService_Start_Err_DevDisabled", null);
                final Dialog dialog2 = new Dialog(BackgroundService.this);
                dialog2.setContentView(R.layout.dialog2);
                dialog2.setTitle(getApplicationContext().getResources().getString(R.string.tx_104));
                dialog2.setCancelable(true);
                dialog2.show();

                Button cancel3 = dialog2.findViewById(R.id.dialogCancelDevSett);
                cancel3.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        dialog2.dismiss();
                    }
                });

                Button settings3 = dialog2.findViewById(R.id.dialogSettingsDevSett);
                settings3.setOnClickListener(new OnClickListener() {
                    public void onClick(View arg0) {
                        try {
                            startActivity(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
                        } catch (android.content.ActivityNotFoundException ae2) {

                        }
                        dialog2.dismiss();
                    }
                });
            }
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
        } catch (IllegalArgumentException e) {
                e.printStackTrace();
        }
    }

    public void stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent serviceIntent = new Intent(this, ForegroundService.class);
            stopService(serviceIntent);
        }
        //stop the Handlers who are responsible for restarting the sensor-listeners
        mFirebaseAnalytics.logEvent("BackgroundService_Stop", null);
        //Stop sensors from beeing reactivated, 9 is for reactivating every 5 sec
        GoogleMap.listHandler.removeMessages(10);

        Config.backgroundServiceActive = false;

        try {
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
        } catch (Exception e) {
                e.printStackTrace();
        }
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
            notificationManager = NotificationManagerCompat.from(this);
            notificationManager.cancelAll();
        } catch (Exception e) {
                e.printStackTrace();
        }

        Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_70), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (!shouldStart) {
            stop();
        } else {
            finish();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!shouldStart) {
                stop();
            } else {
                finish();
            }
        }
        return (true);
    }

}
