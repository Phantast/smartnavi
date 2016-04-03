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

import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.GoogleMap;

import java.util.Iterator;

/**
 * This class is used to determine the start location
 * and to get location for the autocorrect feature
 *
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class Locationer implements LocationListener {

    public static double startLat;
    public static double startLon;
    public static double errorGPS;
    public static float lastErrorGPS = 9999999999.0f;
    private int satellitesInRange = 0;
    private Handler mHandler = new Handler();
    ;
    private int allowedErrorGps = 10;
    private boolean autoCorrectSuccess = true;
    private int additionalSecondsAutocorrect = 0;
    private boolean giveGpsMoreTime = true;
    private long lastLocationTime = 0L;
    private LocationManager mLocationManager;
    private onLocationUpdateListener locationListener;
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
                    if(BuildConfig.debug)
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
                    if(BuildConfig.debug)
                     Log.i("Location-Status",
                     "DISCARDED: Autocorrect GPS: " + location.getProvider() + " "
                     + location.getAccuracy());
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
                if(BuildConfig.debug)
                 Log.i("Location-Status", "Not enough satelites in range: " +
                 satellitesInRange);
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
    }

    public void deactivateLocationer() {
        //ProgressBar must be made invisible (GONE)
        locationListener.onLocationUpdate(12);
        try {
            mLocationManager.removeUpdates(this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    public void startLocationUpdates() {
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 0, this);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, this);
            mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
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
        if(BuildConfig.debug)
         Log.i("Location-Status", "Satelites in range: " + i);
        satellitesInRange = i;
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
                    if (BuildConfig.debug)
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
            allowedErrorGps = allowedErrorGps + 8;
            if(BuildConfig.debug)
            Log.i("Location-Status", "Time for request:" + additionalSecondsAutocorrect + " and allowed Error: " +
             allowedErrorGps);
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
            if(BuildConfig.debug)
             Log.i("Location-Status", "shutdown Autocorrect");

            if (GoogleMap.backgroundServiceShallBeOnAgain == true) {
                Config.backgroundServiceActive = true;
                BackgroundService.reactivateFakeProvider();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public interface onLocationUpdateListener {
        void onLocationUpdate(int event);
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
