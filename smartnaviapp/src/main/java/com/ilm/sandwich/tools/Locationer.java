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
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.GoogleMap;
import com.ilm.sandwich.sensors.Core;

import java.util.Iterator;

/**
 * This class is used to determine the start location
 * and to get location for the autocorrect feature
 *
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Locationer implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, com.google.android.gms.location.LocationListener {

    public static double startLat;
    public static double startLon;
    public static double errorGPS;
    public static float lastErrorGPS = 9999999999.0f;
    private onLocationUpdateListener locationListener;
    private int satellitesInRange = 0;
    private Handler mHandler = new Handler();
    private int allowedErrorGps = 10;
    private boolean autoCorrectSuccess = true;
    private int additionalSecondsAutocorrect = 0;
    private boolean giveGpsMoreTime = true;
    private GoogleApiClient mGoogleApiClient;
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
    private LocationListener gpsAutocorrectLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (location.getLatitude() != 0) {
                location.getProvider();
                startLat = location.getLatitude();
                startLon = location.getLongitude();
                errorGPS = location.getAccuracy();
                if (errorGPS <= allowedErrorGps) {
                    if (BuildConfig.debug)
                        Log.i("Location-Status", "Autocorrect GPS: " +
                                location.getProvider() + " "
                                + location.getAccuracy());

                    locationListener.onLocationUpdate(8);

                    allowedErrorGps = 10;
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
                    if (BuildConfig.debug)
                        Log.i("Location-Status", "DISCARDED: Autocorrect GPS: " + location.getProvider() + " " + location.getAccuracy());
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
    private Runnable autoStopTask = new Runnable() {
        public void run() {

            stopAutocorrect();
        }
    };
    private Runnable satelitesInRangeTest = new Runnable() {
        public void run() {
            if (satellitesInRange < 5) {
                stopAutocorrect();
                if (BuildConfig.debug)
                    Log.i("Location-Status", "Not enough satelites in range: " + satellitesInRange);
            }
        }
    };


    // LocationClient
    // **************

    public Locationer(Context context) {
        super();
        if (context instanceof onLocationUpdateListener) {
            locationListener = (onLocationUpdateListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mContext = context;

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void deactivateLocationer() {
        //ProgressBar must be made invisible (GONE)
        locationListener.onLocationUpdate(12);
        try {
            if (mGoogleApiClient.isConnected()) {
                try {
                    mLocationManager.removeUpdates(this);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                mGoogleApiClient.disconnect();
            }
        } catch (Exception e) {
            //nothing
        }
        try {
            mLocationManager.removeUpdates(this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    public void startLocationUpdates() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int status = api.isGooglePlayServicesAvailable(mContext);
        if (status != ConnectionResult.SUCCESS) {
            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 0, this);
                if (mLocationManager.getAllProviders().contains("network")) {
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, this);
                }
                mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10, 0, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        } else {
            // get first position
            mGoogleApiClient.connect();
        }
    }

    private void updateSats() {
        try {
            final GpsStatus gs = this.mLocationManager.getGpsStatus(null);
            int i = 0;
            final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
            while (it.hasNext()) {
                it.next();
                i += 1;
            }
            if (BuildConfig.debug) {
                Log.i("Location-Status", "Satelites in range: " + i);
            }
            satellitesInRange = i;
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        if (BuildConfig.debug) {
            Log.i("Location-Status", "Connection FAILED" + arg0.getErrorCode());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onConnected(Bundle arg0) {
        if (BuildConfig.debug) {
            Log.i("Location-Status", "onConnected");
        }
        try {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null) {
                double startLat = lastLocation.getLatitude();
                double startLon = lastLocation.getLongitude();
                lastErrorGPS = lastLocation.getAccuracy();
                double altitude = lastLocation.getAltitude();
                lastLocationTime = lastLocation.getTime();
                if (BuildConfig.debug) {
                    Log.i("Location-Status", "Last-Location: Error=" + lastErrorGPS + " Time:" + lastLocationTime);
                }
                double middleLat = startLat * 0.01745329252;
                double distanceLongitude = 111.3D * Math.cos(middleLat);
                Core.initialize(startLat, startLon, distanceLongitude, altitude, lastErrorGPS);
            }
            highRequest = LocationRequest.create();
            highRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            highRequest.setInterval(1000); // Update location every second

            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, highRequest, this);

            locationListener.onLocationUpdate(0);
            // remove location updates after 40s automatically
            mHandler.postDelayed(deaktivateTask, 40000);

        } catch (SecurityException e) {
            LocationManager locManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            boolean locationEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (locationEnabled == false) {
                //no position has ever been requested or Location Services are deactivated, so tell the user to activate them
                if(BuildConfig.debug){
                    Log.i("Location-Status", "Security Exception");
                }
                locationListener.onLocationUpdate(5);
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

                locationListener.onLocationUpdate(8);
                // nach 40sek automatisch location updates removen
                mHandler.postDelayed(deaktivateTask, 40000);
            }

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (BuildConfig.debug) {
            Log.i("Location-Status", "Connection suspended!");
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        if (BuildConfig.debug) {
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

            locationListener.onLocationUpdate(14);

            if (location.getAccuracy() < 16) {
                try {
                    deactivateLocationer();
                } catch (Exception e) {
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
        if (BuildConfig.debug) {
            Log.i("Location-Status", "onProviderDisabled");
        }
        if (settings.getBoolean("gpsDialogShown", false) == false) {
            new writeSettings("gpsDialogShown", true).execute();
            locationListener.onLocationUpdate(5);
        }
    }

    public void starteAutocorrect() {
        if (autoCorrectSuccess) {
            autoCorrectSuccess = false;
        } else if (additionalSecondsAutocorrect <= 30) {
            additionalSecondsAutocorrect = additionalSecondsAutocorrect + 7;
            allowedErrorGps = allowedErrorGps + 8;
            if (BuildConfig.debug)
                Log.i("Location-Status", "Time for request:" + additionalSecondsAutocorrect + " and allowed Error: " + allowedErrorGps);
        }
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    100, 0, gpsAutocorrectLocationListener);
            mHandler.postDelayed(autoStopTask,
                    10000 + additionalSecondsAutocorrect * 1000);
            mHandler.postDelayed(satelitesInRangeTest, 10000);
            mLocationManager.addGpsStatusListener(mGpsStatusListener);
            giveGpsMoreTime = true;
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    public void stopAutocorrect() {
        try {
            mLocationManager.removeGpsStatusListener(mGpsStatusListener);
            mLocationManager.removeUpdates(gpsAutocorrectLocationListener);
            mHandler.removeCallbacks(autoStopTask);
            mHandler.removeCallbacks(satelitesInRangeTest);
            if (BuildConfig.debug)
                Log.i("Location-Status", "shutdown Autocorrect");

            if (GoogleMap.backgroundServiceShallBeOnAgain == true) {
                Config.backgroundServiceActive = true;
                BackgroundService.reactivateFakeProvider();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    public interface onLocationUpdateListener {
        void onLocationUpdate(int event);
    }

    private class writeSettings extends AsyncTask<Void, Void, Void> {

        private String key;
        private int dataType;
        private boolean setting1;

        private writeSettings(String key, boolean setting1) {
            this.key = key;
            this.setting1 = setting1;
            dataType = 0;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences settings = mContext.getSharedPreferences(mContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            if (dataType == 0) {
                settings.edit().putBoolean(key, setting1).commit();
            }
            return null;
        }
    }
}
