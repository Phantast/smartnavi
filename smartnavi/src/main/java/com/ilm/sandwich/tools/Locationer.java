package com.ilm.sandwich.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.Config;
import com.ilm.sandwich.GoogleMapActivity;
import com.ilm.sandwich.OsmMapActivity;

import java.util.Iterator;

/**
 * This class is used to determine the start location
 * and to get location for the autocorrect feature
 *
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class Locationer implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, com.google.android.gms.location.LocationListener {

    public static double startLat;
    public static double startLon;
    public static double errorGPS;
    public static float lastErrorGPS = 9999999999.0f;
    private int satellitesInRange = 0;
    private Runnable satelitesInRangeTest = new Runnable() {
        public void run() {
            if (satellitesInRange < 5) {
                stopAutocorrect();
                // Log.d("Location-Status", "Not enough satelites in range: " +
                // satellitesInRange);
            }
        }
    };
    private Handler mHandler = new Handler();
    private int erlaubterErrorGPS = 10;
    private boolean autoCorrectSuccess = true;
    private int additionalSecondsAutocorrect = 0;
    private boolean giveGpsMoreTime = true;
    private LocationClient mLocationClient;
    private LocationRequest highRequest;
    private long lastLocationTime = 0L;
    private LocationManager mLocationManager;
    private Context mContext;
    private Listener mGpsStatusListener = new Listener() {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    updateSats();
                    break;
            }
        }
    };
    private Runnable deaktivateTask = new Runnable() {
        public void run() {
            deactivateLocationer();
        }
    };
    private Runnable autoStopTask = new Runnable() {
        public void run() {
            stopAutocorrect();
        }
    };
    private LocationListener gpsAutocorrectLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (location.getLatitude() != 0) {
                location.getProvider();
                startLat = location.getLatitude();
                startLon = location.getLongitude();
                errorGPS = location.getAccuracy();
                if (errorGPS <= erlaubterErrorGPS) {
                    // Log.d("Location-Status", "Autocorrect GPS: " +
                    // location.getProvider() + " "
                    // + location.getAccuracy());

                    if (Config.usingGoogleMaps) {
                        GoogleMapActivity.listHandler.sendEmptyMessage(8);
                    } else {
                        OsmMapActivity.listHandler.sendEmptyMessage(8);
                    }

                    erlaubterErrorGPS = 10;
                    autoCorrectSuccess = true;
                    additionalSecondsAutocorrect = 0;
                } else {
                    if (giveGpsMoreTime) {
                        //Positions are coming in, but they are to inaccurate
                        //so give some extra time
                        mHandler.removeCallbacks(autoStopTask);
                        mHandler.postDelayed(autoStopTask,
                                10000 + additionalSecondsAutocorrect * 1000);
                        giveGpsMoreTime = false;
                    }
                    // Log.d("Location-Status",
                    // "DISCARDED: Autocorrect GPS: " + location.getProvider() +
                    // " "
                    // + location.getAccuracy());
                }
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }

    };


    // LocationClient
    // **************

    public Locationer(Context context) {
        super();
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mContext = context;
    }

    public void deactivateLocationer() {
        //ProgressBar must be made invisible (GONE)
        try {
            if (Config.usingGoogleMaps) {
                GoogleMapActivity.listHandler.sendEmptyMessage(12);
                //Log.i("Location-Status", "Progress aus GOOGLE");
            } else {
                OsmMapActivity.listHandler.sendEmptyMessage(12);
                //Log.i("Location-Status", "Progress aus OSM");
            }
        } catch (Exception e) {
            //nothing, may happen sometimes if the views of the activity are already destroyed
        }


        try {
            if (mLocationClient.isConnected()) {
                mLocationClient.removeLocationUpdates(this);
                mLocationClient.disconnect();
            }
        } catch (Exception e) {
            //nothing
        }
        mLocationManager.removeUpdates(this);

    }

    public void startLocationUpdates() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
        if (status != ConnectionResult.SUCCESS) {
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 0, this);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, this);
                mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10, 0, this);
            } catch (Exception e) {
                if (Config.debugMode)
                    e.printStackTrace();
            }
        } else {
            // get first position
            mLocationClient = new LocationClient(mContext, this, this);
            if (mLocationClient.isConnected() == false && mLocationClient.isConnecting() == false) {
                mLocationClient.connect();
            }
        }
    }

    private void updateSats() {
        final GpsStatus gs = this.mLocationManager.getGpsStatus(null);
        int i = 0;
        final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
        while (it.hasNext()) {
            it.next();
            i += 1;
        }
        // Log.d("Location-Status", "Satelites in range: " + i);
        satellitesInRange = i;
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        if (Config.debugMode) {
            Log.i("Location-Status", "LocationClient: Connection FAILED" + arg0.getErrorCode());
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        if (Config.debugMode) {
            Log.i("Location-Status", "onConnected");
        }
        try {
            Location lastLocation = mLocationClient.getLastLocation();
            double startLat = lastLocation.getLatitude();
            double startLon = lastLocation.getLongitude();
            lastErrorGPS = lastLocation.getAccuracy();
            double altitude = lastLocation.getAltitude();
            lastLocationTime = lastLocation.getTime();
            double middleLat = startLat * 0.01745329252;
            double distanceLongitude = 111.3D * Math.cos(middleLat);

            Core.initialize(startLat, startLon, distanceLongitude, altitude, lastErrorGPS);
            highRequest = new LocationRequest();
            highRequest.setExpirationDuration(40000).setInterval(10).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            mLocationClient.requestLocationUpdates(highRequest, this);
            if (Config.usingGoogleMaps) {
                GoogleMapActivity.listHandler.sendEmptyMessage(0);
            } else {
                OsmMapActivity.listHandler.sendEmptyMessage(0);
            }
            // remove location updates after 40s automatically
            mHandler.postDelayed(deaktivateTask, 40000);

        } catch (Exception e) {
            LocationManager locManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            boolean locationEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (locationEnabled == false) {
                //no position has ever been requested or Location Services are deactivated, so tell the user to activate them
                if (Config.usingGoogleMaps) {
                    GoogleMapActivity.listHandler.sendEmptyMessage(5);
                } else {
                    OsmMapActivity.listHandler.sendEmptyMessage(5);
                }
            } else {
                // location services are activated but no location has ever been requested
                // so, start with 0,0 and hope the best
                double startLat = 0;
                double startLon = 0;
                lastErrorGPS = 1000000;
                double altitude = 0;
                lastLocationTime = System.currentTimeMillis() - 1000000;
                double middleLat = startLat * 0.01745329252;
                double distanceLongitude = 111.3 * Math.cos(middleLat);
                Core.initialize(startLat, startLon, distanceLongitude, altitude, lastErrorGPS);

                if (Config.usingGoogleMaps) {
                    GoogleMapActivity.listHandler.sendEmptyMessage(0);
                } else {
                    OsmMapActivity.listHandler.sendEmptyMessage(0);
                }
                // nach 40sek automatisch location updates removen
                mHandler.postDelayed(deaktivateTask, 40000);
            }

        }

    }

    @Override
    public void onDisconnected() {
        // only when Problem occurs
        if (Config.debugMode) {
            Log.i("Location-Status", "onDisconnected");
        }
        //TextView mapText = (TextView) findViewById(R.id.mapText);
        //mapText.setVisibility(0);
        //mapText.append("Connection Problem with Google Play Services. \nPlease make sure you have Google Play Services installed.");
    }

    @Override
    public void onLocationChanged(Location location) {
        if (Config.debugMode) {
            Log.i("Location-Status", "onLocationChanged Acc:" + location.getAccuracy());
        }
        long differenceTime = location.getTime() - lastLocationTime;
        double differenceError = lastErrorGPS - location.getAccuracy();
        if (differenceTime > 30000 || differenceError > 7) {

            double startLat = location.getLatitude();
            double startLon = location.getLongitude();
            lastErrorGPS = location.getAccuracy();
            double altitude = location.getAltitude();
            lastLocationTime = location.getTime();
            double middleLat = startLat * 0.01745329252;
            double distanceLongitude = 111.3D * Math.cos(middleLat);

            Core.initialize(startLat, startLon, distanceLongitude, altitude, lastErrorGPS);

            if (Config.usingGoogleMaps) {
                GoogleMapActivity.setPosition(true);
            } else {
                OsmMapActivity.listHandler.sendEmptyMessage(14);
            }

            if (location.getAccuracy() < 16) {
                try {
                    deactivateLocationer();
                } catch (Exception e) {
                    if (Config.debugMode)
                        e.printStackTrace();
                }
            }

            lastLocationTime = location.getTime();
            lastErrorGPS = location.getAccuracy();
        } else {
            //for debug purposes
        }

    }


    // ******************************************************************
    // ******************** AutoCorrection with GPS ******************
    // ******************************************************************

    @Override
    public void onProviderDisabled(String provider) {
        SharedPreferences settings = mContext.getSharedPreferences(mContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        if (Config.debugMode) {
            Log.i("Location-Status", "onProviderDisabled");
        }
        if (settings.getBoolean("gpsDialogShown", false) == false) {

            new writeSettings("gpsDialogShown", true).execute();
            if (Config.usingGoogleMaps) {
                //show GPS Dialog
                GoogleMapActivity.listHandler.sendEmptyMessage(13);
            } else {
                OsmMapActivity.listHandler.sendEmptyMessage(13);
            }

        }
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void starteAutocorrect() {
        if (autoCorrectSuccess) {
            autoCorrectSuccess = false;
        } else if (additionalSecondsAutocorrect <= 30) {
            additionalSecondsAutocorrect = additionalSecondsAutocorrect + 7;
            erlaubterErrorGPS = erlaubterErrorGPS + 8;
            // Log.d("Location-Status", "Time for request:" +
            // additionalTimeForGPS + " and allowed Error: " +
            // allowedErrorGps);
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                100, 0, gpsAutocorrectLocationListener);
        mHandler.postDelayed(autoStopTask,
                10000 + additionalSecondsAutocorrect * 1000);
        mHandler.postDelayed(satelitesInRangeTest, 10000);
        mLocationManager.addGpsStatusListener(mGpsStatusListener);
        giveGpsMoreTime = true;
    }

    public void stopAutocorrect() {
        mLocationManager.removeGpsStatusListener(mGpsStatusListener);
        mLocationManager.removeUpdates(gpsAutocorrectLocationListener);
        mHandler.removeCallbacks(autoStopTask);
        mHandler.removeCallbacks(satelitesInRangeTest);
        // Log.d("Location-Status", "shutdown Autocorrect");

        if (GoogleMapActivity.backgroundServiceShallBeOnAgain == true) {
            Config.backgroundServiceActive = true;
            BackgroundService.reactivateFakeProvider();
        }
    }

    private class writeSettings extends AsyncTask<Void, Void, Void> {

        private String key;
        private int dataType;
        private boolean setting1;
        private String setting2;
        private int einstellung3;

        private writeSettings(String key, boolean setting1) {
            this.key = key;
            this.setting1 = setting1;
            dataType = 0;
        }

        private writeSettings(String key, String setting2) {
            this.key = key;
            this.setting2 = setting2;
            dataType = 1;
        }

        private writeSettings(String key, int einstellung3) {
            this.key = key;
            this.einstellung3 = einstellung3;
            dataType = 2;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences settings = mContext.getSharedPreferences(mContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            if (dataType == 0) {
                settings.edit().putBoolean(key, setting1).commit();
            } else if (dataType == 1) {
                settings.edit().putString(key, setting2).commit();
            } else if (dataType == 2) {
                settings.edit().putInt(key, einstellung3).commit();
            }
            return null;
        }
    }

}
