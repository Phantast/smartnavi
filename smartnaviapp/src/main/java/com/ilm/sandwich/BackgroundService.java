package com.ilm.sandwich;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.ilm.sandwich.tools.Config;
import com.ilm.sandwich.tools.Core;

/**
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class BackgroundService extends Activity {

    // the following two are just that the following map (if user goes back) can get an actual position
    public static double sGeoLat;
    public static double sGeoLon;
    public static int steps = 0;
    static Location loc;
    static Location loc2;
    static String mocLocationProvider;
    static String mocLocationNetworkProvider;
    static LocationManager geoLocationManager;
    Notification notification;
    Button serviceButton;
    NotificationManager notificationManager;
    private boolean shouldStart = true;

    public static void pauseFakeProvider() {
        //Log.d("Location-Status","pause Fake Provider");
        try {
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
            geoLocationManager.removeTestProvider(mocLocationProvider);
            geoLocationManager.clearTestProviderEnabled(mocLocationProvider);

            geoLocationManager.setTestProviderEnabled(mocLocationNetworkProvider, false);
            geoLocationManager.removeTestProvider(mocLocationNetworkProvider);
            geoLocationManager.clearTestProviderEnabled(mocLocationNetworkProvider);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static void reactivateFakeProvider() {
        //Log.d("Location-Status","reactivate Fake Provider");
        try {
            mocLocationProvider = LocationManager.GPS_PROVIDER;
            geoLocationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 0, 5);
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, true);

            mocLocationNetworkProvider = LocationManager.NETWORK_PROVIDER;
            geoLocationManager.addTestProvider(mocLocationNetworkProvider, false, false, false, false, true, true, true, 1, 5);
            geoLocationManager.setTestProviderEnabled(mocLocationNetworkProvider, true);
        } catch (Exception e) {
            //	e.printStackTrace();
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
        loc.setAccuracy(12);
        loc.setAltitude(Core.altitude);
        loc.setLatitude(Core.startLat);
        loc.setLongitude(Core.startLon);
        loc.setProvider(mocLocationProvider);
        loc.setSpeed(0.8f);
        loc.setBearing((float) Core.azimuth);
        loc.setTime(System.currentTimeMillis());
        try {
            try {
                loc.setElapsedRealtimeNanos(System.currentTimeMillis());
            } catch (NoSuchMethodError e) {
                //e.printStackTrace();
            }
        } catch (Exception e) {
            //egal
        }
        try {
            geoLocationManager.setTestProviderLocation(mocLocationProvider, loc);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        //Network
        loc2 = new Location(mocLocationNetworkProvider);
        loc2.setAccuracy(12.0f);
        loc2.setAltitude(Core.altitude);
        loc2.setLatitude(Core.startLat);
        loc2.setLongitude(Core.startLon);
        loc2.setProvider(mocLocationNetworkProvider);
        loc2.setSpeed(0.8f);
        loc2.setBearing((float) Core.azimuth);
        loc2.setTime(System.currentTimeMillis());
        try {
            try {
                loc2.setElapsedRealtimeNanos(System.currentTimeMillis());
            } catch (NoSuchMethodError e) {
                //e.printStackTrace();
            }
        } catch (Exception e) {
            //egal
        }
        try {
            geoLocationManager.setTestProviderLocation(mocLocationNetworkProvider, loc2);
        } catch (Exception e) {
            //e.printStackTrace();
        }

        if (Config.debugMode) {
            //Log.i("Frequenzen", loc.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backgroundservice);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(getResources().getString(R.string.tx_64));
        geoLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //restart Sensors
        try {
            if (Config.usingGoogleMaps) {
                GoogleMap.listHandler.sendEmptyMessage(10);
            } else {
                OsmMap.listHandler.sendEmptyMessage(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //Bug for some devices, in that case: Abort and go back.
            Toast.makeText(this, "Unfortunately the background service is not supported on your device.", Toast.LENGTH_LONG).show();
            try {
                geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
            } catch (Exception e2) {
                // e.printStackTrace();
            }
            try {
                geoLocationManager.clearTestProviderEnabled(mocLocationProvider);
            } catch (Exception e3) {
                // e.printStackTrace();
            }
            try {
                geoLocationManager.removeTestProvider(mocLocationProvider);
            } catch (Exception e4) {
                // e.printStackTrace();
            }
            try {
                notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
            } catch (Exception e5) {
                // e.printStackTrace();
            }
            finish();
        }
        serviceButton = (Button) findViewById(R.id.button1);
        serviceButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (shouldStart == true) {
                    starte();
                } else {
                    stop();
                    shouldStart = true;
                    serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
                }
            }
        });

        if (shouldStart == true) {
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
        } else {
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));
        }
    }

    @SuppressWarnings("deprecation")
    public void starte() {

        mocLocationProvider = LocationManager.GPS_PROVIDER;
        mocLocationNetworkProvider = LocationManager.NETWORK_PROVIDER;
        try {
            geoLocationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 1, 5);
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, true);

            geoLocationManager.addTestProvider(mocLocationNetworkProvider, false, false, false, false, true, true, true, 1, 5);
            geoLocationManager.setTestProviderEnabled(mocLocationNetworkProvider, true);

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


            Intent intent = new Intent(this, BackgroundService.class);
            PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    this);
            notification = builder.setContentIntent(activity)
                    .setContentTitle(getApplicationContext().getResources().getString(R.string.tx_72))
                    .setContentText(getApplicationContext().getResources().getString(R.string.tx_73))
                    .setSmallIcon(R.drawable.stats)
                    .setOngoing(true)
                    .build();

            notificationManager.notify(0, notification);
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));

            shouldStart = false;
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));
            Config.backgroundServiceActive = true;
            Config.backgroundServiceUsed = true;


            //tell the Maps to restart the sensor listeners after 10s because
            //other foreign third party apps may stop them
            if (Config.usingGoogleMaps) {
                GoogleMap.listHandler.sendEmptyMessage(9);
            } else {
                OsmMap.listHandler.sendEmptyMessage(9);
            }

            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        } catch (SecurityException sece) {
            final Dialog dialog1 = new Dialog(BackgroundService.this);
            dialog1.setContentView(R.layout.dialog1);
            dialog1.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
            dialog1.setCancelable(true);
            dialog1.show();

            Button cancel2 = (Button) dialog1.findViewById(R.id.dialogCancelMock);
            cancel2.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    dialog1.dismiss();
                }
            });

            Button settings2 = (Button) dialog1.findViewById(R.id.dialogSettingsMock);
            settings2.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                    } catch (android.content.ActivityNotFoundException ae) {
                        try {
                            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS));
                        } catch (android.content.ActivityNotFoundException ae2) {
                            try {
                                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                            } catch (android.content.ActivityNotFoundException e) {
                                // e.printStackTrace();
                            }

                        }
                    }

                    dialog1.dismiss();
                }
            });
            serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
        } catch (IllegalArgumentException e) {
            // e.printStackTrace();
        }

    }

    public void stop() {
        //stop the Handlers who are responsible for restarting the sensor-listeners
        if (Config.usingGoogleMaps) {
            GoogleMap.listHandler.removeMessages(10);
        } else {
            OsmMap.listHandler.removeMessages(10);
        }

        Config.backgroundServiceActive = false;

        try {
            geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        try {
            geoLocationManager.clearTestProviderEnabled(mocLocationProvider);
        } catch (Exception e) {
            // e.printStackTrace();
        }
        try {
            geoLocationManager.removeTestProvider(mocLocationProvider);
        } catch (Exception e) {
            // e.printStackTrace();
        }

        notificationManager.cancelAll();
        Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_70), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {

        if (shouldStart == false) {
            stop();
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (shouldStart == false) {
                    try {
                        geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
                        geoLocationManager.removeTestProvider(mocLocationProvider);
                        geoLocationManager.clearTestProviderEnabled(mocLocationProvider);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    notificationManager.cancelAll();
                    Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_70), Toast.LENGTH_LONG).show();
                }
                finish();
                return (true);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
