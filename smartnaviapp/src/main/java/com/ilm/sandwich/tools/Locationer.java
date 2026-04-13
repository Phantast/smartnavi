package com.ilm.sandwich.tools;

import android.content.Context;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.GoogleMap;
import com.ilm.sandwich.sensors.Core;


/**
 * This class is used to determine the start location
 * and to get location for the autocorrect feature
 *
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Locationer implements LocationListener {

    public static double startLat;
    public static double startLon;
    public static double errorGPS;
    public static float lastErrorGPS = 9999999999.0f;
    private onLocationUpdateListener locationListener;
    private int satellitesInRange = 0;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private int allowedErrorGps = 10;
    private boolean autoCorrectSuccess = true;
    private int additionalSecondsAutocorrect = 0;
    private boolean giveGpsMoreTime = true;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback fusedLocationCallback;
    private LocationRequest highRequest;
    private long lastLocationTime = 0L;
    private LocationManager mLocationManager;
    private Context mContext;
    private GnssStatus.Callback mGnssStatusCallback;
    private Runnable deaktivateTask = new Runnable() {
        public void run() {
            deactivateLocationer();
        }
    };
    private LocationListener gpsAutocorrectLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if (location.getLatitude() != 0) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mGnssStatusCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    satellitesInRange = status.getSatelliteCount();
                    if (BuildConfig.debug) {
                        Log.i("Location-Status", "Satellites in range: " + satellitesInRange);
                    }
                }
            };
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        fusedLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    onLocationChanged(location);
                }
            }
        };
    }

    public void deactivateLocationer() {
        //ProgressBar must be made invisible (GONE)
        locationListener.onLocationUpdate(12);
        try {
            fusedLocationClient.removeLocationUpdates(fusedLocationCallback);
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
            // Get last known location, then start updates
            try {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location lastLocation) {
                                if (BuildConfig.debug) {
                                    Log.i("Location-Status", "getLastLocation success");
                                }
                                if (lastLocation != null) {
                                    double lat = lastLocation.getLatitude();
                                    double lon = lastLocation.getLongitude();
                                    lastErrorGPS = lastLocation.getAccuracy();
                                    double altitude = lastLocation.getAltitude();
                                    lastLocationTime = lastLocation.getTime();
                                    if (BuildConfig.debug) {
                                        Log.i("Location-Status", "Last-Location: Error=" + lastErrorGPS + " Time:" + lastLocationTime);
                                    }
                                    double middleLat = Math.toRadians(lat);
                                    double distanceLongitude = 111.3D * Math.cos(middleLat);
                                    Core.initialize(lat, lon, distanceLongitude, altitude, lastErrorGPS);
                                }
                                startFusedUpdates();
                            }
                        })
                        .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                if (BuildConfig.debug) {
                                    Log.i("Location-Status", "getLastLocation failed: " + e.getMessage());
                                }
                                // Still try to start updates even if last location failed
                                startFusedUpdates();
                            }
                        });
            } catch (SecurityException e) {
                handleSecurityException();
            }
        }
    }

    private void startFusedUpdates() {
        try {
            highRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .build();

            fusedLocationClient.requestLocationUpdates(highRequest, fusedLocationCallback, Looper.getMainLooper());

            locationListener.onLocationUpdate(0);
            // remove location updates after 40s automatically
            mHandler.postDelayed(deaktivateTask, 40000);
        } catch (SecurityException e) {
            handleSecurityException();
        }
    }

    private void handleSecurityException() {
        boolean locationEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!locationEnabled) {
            if (BuildConfig.debug) {
                Log.i("Location-Status", "Security Exception");
            }
            locationListener.onLocationUpdate(5);
        } else {
            double lat = 0;
            double lon = 0;
            lastErrorGPS = 1000000;
            double altitude = 0;
            lastLocationTime = System.currentTimeMillis() - 1000000;
            double middleLat = Math.toRadians(lat);
            double distanceLongitude = 111.3 * Math.cos(middleLat);
            Core.initialize(lat, lon, distanceLongitude, altitude, lastErrorGPS);

            locationListener.onLocationUpdate(8);
            mHandler.postDelayed(deaktivateTask, 40000);
        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (BuildConfig.debug) {
            Log.i("Location-Status", "onLocationChanged Acc:" + location.getAccuracy());
        }
        long differenceTime = location.getTime() - lastLocationTime;
        double differenceError = lastErrorGPS - location.getAccuracy();
        if (differenceTime > 30000 || differenceError > 7) {

            double lat = location.getLatitude();
            double lon = location.getLongitude();
            lastErrorGPS = location.getAccuracy();
            double altitude = location.getAltitude();
            lastLocationTime = location.getTime();
            double middleLat = Math.toRadians(lat);
            double distanceLongitude = 111.3D * Math.cos(middleLat);

            Core.initialize(lat, lon, distanceLongitude, altitude, lastErrorGPS);

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
        if (BuildConfig.debug) {
            Log.i("Location-Status", "onProviderDisabled");
        }
        android.content.SharedPreferences settings = mContext.getSharedPreferences(mContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        if (!settings.getBoolean("gpsDialogShown", false)) {
            PreferencesHelper.putBoolean(mContext, "gpsDialogShown", true);
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mGnssStatusCallback != null) {
                mLocationManager.registerGnssStatusCallback(mGnssStatusCallback, mHandler);
            }
            giveGpsMoreTime = true;
        } catch (SecurityException e) {
            e.printStackTrace();
        }

    }

    public void stopAutocorrect() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mGnssStatusCallback != null) {
                mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
            }
            mLocationManager.removeUpdates(gpsAutocorrectLocationListener);
            mHandler.removeCallbacks(autoStopTask);
            mHandler.removeCallbacks(satelitesInRangeTest);
            if (BuildConfig.debug)
                Log.i("Location-Status", "shutdown Autocorrect");

            if (GoogleMap.backgroundServiceShallBeOnAgain) {
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
}
