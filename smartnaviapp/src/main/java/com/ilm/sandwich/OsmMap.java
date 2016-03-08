package com.ilm.sandwich;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ilm.sandwich.fragments.TutorialFragment;
import com.ilm.sandwich.tools.Analytics;
import com.ilm.sandwich.tools.Config;
import com.ilm.sandwich.tools.Core;
import com.ilm.sandwich.tools.Locationer;
import com.ilm.sandwich.tools.MyItemizedOverlay;
import com.ilm.sandwich.tools.PlacesAutoComplete;
import com.ilm.sandwich.tools.PlacesTextSearch;
import com.ilm.sandwich.tools.Statistics;
import com.ilm.sandwich.tools.SuggestionsAdapter;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.FolderOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.bonuspack.routing.MapQuestRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.RoadNode;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.OverlayManager;

import java.io.File;
import java.util.ArrayList;

/**
 * MapActvitiy for OpenStreetMaps
 * @author Christian Henke
 *         www.smartnavi-app.com
 */

public class OsmMap extends AppCompatActivity implements Locationer.onLocationUpdateListener, SensorEventListener, MapEventsReceiver, TutorialFragment.onTutorialFinishedListener {

    public static Handler listHandler;
    public static SearchView searchView;
    public static boolean firstPositionFound;
    public static SuggestionsAdapter mSuggestionsAdapter;
    public static boolean suggestionsInProgress = false;
    public static MatrixCursor cursor = new MatrixCursor(Config.COLUMNS);
    public static Handler changeSuggestionAdapter;
    static ListView list;
    private static Menu mainMenu;
    private static GeoPoint longPressedGeoPoint;
    public Context sbContext;
    protected DirectedLocationOverlay myLocationOverlay;
    TutorialFragment tutorialFragment;
    private MapView map;
    private IMapController mapController;
    private MyItemizedOverlay[] myItemizedOverlay = new MyItemizedOverlay[10];
    private SensorManager mSensorManager;
    private Core mCore;
    private String uid;
    private Locationer mLocationer;
    private boolean userSwitchesGPS;
    private boolean knownReasonForBreak;
    private int units;
    private int aclUnits;
    private int magnUnits;
    private long startTime;
    private long timePassed;
    private boolean followMe = true;
    private boolean listIsVisible = false;
    private long POS_UPDATE_FREQ = 2200;
    private boolean autoCorrect = false;
    private int autoCorrectFaktor = 1;
    private boolean alreadyWaitingForAutoCorrect;
    private int stepsToWait = 0;
    private boolean backgroundServiceShallBeOn = false;
    private Toolbar toolbar;
    private Analytics mAnalytics;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config.usingGoogleMaps = false;
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        setContentView(R.layout.activity_osmmap);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toolbar = (Toolbar) findViewById(R.id.toolbar_osm); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call

        boolean trackingAllowed = settings.getBoolean("nutzdaten", true);
        mAnalytics = new Analytics(trackingAllowed);

        fab = (FloatingActionButton) findViewById(R.id.fabosm);
        if (fab != null) {
            fab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // longPressMenu will become invisible
                    try {
                        list = (ListView) findViewById(R.id.listeOsm);
                        if (list.getVisibility() == View.VISIBLE)
                            list.setVisibility(View.INVISIBLE);
                    } catch (NullPointerException e) {
                        if (BuildConfig.debug)
                            e.printStackTrace();
                    }
                    setFollowOn();
                }
            });
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Config.PERMISSION_REQUEST_FINE_LOCATION);
        } else {
            checkOfflineMapsDirectory();
            proceedOnCreate();
        }
    }

    private void checkWriteStoragePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Config.PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
            proceedOnCreate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Config.PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mAnalytics.trackEvent("Location_Permission", "Granted_OSM");
                    checkWriteStoragePermission();
                } else {
                    Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_100), Toast.LENGTH_LONG).show();
                    mAnalytics.trackEvent("Location_Permission", "Denied_OSM");
                    finish();
                }
            }
            case Config.PERMISSION_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mAnalytics.trackEvent("Storage_Permission", "Granted_OSM");
                    proceedOnCreate();
                } else {
                    Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_101), Toast.LENGTH_LONG).show();
                    mAnalytics.trackEvent("Storage_Permission", "Denied_OSM");
                    finish();
                }
            }
        }
    }

    private void proceedOnCreate() {
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        map = (MapView) findViewById(R.id.openmapview);
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(mapEventsOverlay);
        map.setBuiltInZoomControls(true);
        map.setKeepScreenOn(true);
        map.setMultiTouchControls(true);

        String tileProviderName = settings.getString("MapSource", "MapQuestOSM");
        if (tileProviderName.equalsIgnoreCase("MapQuestOSM")) {
            final float scale = getBaseContext().getResources().getDisplayMetrics().density;
            final int newScale = (int) (256 * scale);
            map.setTileSource(new XYTileSource("MapquestOSM", 0, 22, newScale, ".jpg", new String[]{
                    "http://otile1.mqcdn.com/tiles/1.0.0/map/", "http://otile2.mqcdn.com/tiles/1.0.0/map/", "http://otile3.mqcdn.com/tiles/1.0.0/map/",
                    "http://otile4.mqcdn.com/tiles/1.0.0/map/"}));
        } else {
            try {
                ITileSource tileSource = TileSourceFactory.getTileSource("Mapnik"); //do not change this string
                map.setTileSource(tileSource);
            } catch (IllegalArgumentException e) {
                map.setTileSource(TileSourceFactory.MAPNIK);
            }
        }

        // http://otile1.mqcdn.com/tiles/1.0.0/sat for satelite pictures, but only zoom lovel 12+ for the U.S.

        // map.setTileSource(TileSourceFactory.MAPQUESTOSM);

        isOnline();
        firstPositionFound = false;

        mapController = map.getController();
        double defaultLat = 50.000000D;
        double defaultLon = 10.000000D;
        double mittellat = defaultLat * 0.01745329252;
        double abstandLaengengrade = 111.3D * Math.cos(mittellat);
        Core.initialize(defaultLat, defaultLon, abstandLaengengrade, 200, 3000);
        mapController.setZoom(3);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        try {
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getName();
        } catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.tx_43), Toast.LENGTH_LONG).show();
        }

        //Core of SmartNavi
        //does all the step-detection and orientation estimations
        //as well as export feature
        mCore = new Core();

        new writeSettings("follow", true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        uid = settings.getString("uid", "0");
        if (uid.equalsIgnoreCase("0")) {
            String neuUID = "" + (1 + (int) (Math.random() * ((10000000 - 1) + 1)));
            new writeSettings("uid", neuUID).execute();
            uid = settings.getString("uid", "0");
        }

        mLocationer = new Locationer(OsmMap.this);

        startHandler();

        String stepLengthString = settings.getString("step_length", null);
        if (stepLengthString != null) {
            try {
                stepLengthString = stepLengthString.replace(",", ".");
                Float savedHeight = (Float.parseFloat(stepLengthString));
                if (savedHeight < 241 && savedHeight > 119) {
                    Core.stepLength = savedHeight / 222;
                } else if (savedHeight < 95 && savedHeight > 45) {
                    Core.stepLength = (float) (savedHeight * 2.54 / 222);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            tutorialFragment = new TutorialFragment();
            fragmentTransaction.add(R.id.osmmap_actvity_layout, tutorialFragment);
            fragmentTransaction.commit();
        }

        positionUpdate();

        list = (ListView) findViewById(R.id.listeOsm);
        if (list.getVisibility() == View.VISIBLE)
        list.setVisibility(View.INVISIBLE);
        listIsVisible = false;
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    Core.setLocation(longPressedGeoPoint.getLatitude(), longPressedGeoPoint.getLongitude());
                    mAnalytics.trackEvent("OSM_LongPress_Action", "Set_Position");
                    list.setVisibility(View.INVISIBLE);
                    setFollowOn();
                    map.invalidate();
                    mapController.animateTo(longPressedGeoPoint);
                    // Positionstask reactivate
                    listHandler.sendEmptyMessageDelayed(4, 50);
                } else {
                    showRouteInfo(true);
                    mAnalytics.trackEvent("OSM_LongPress_Action", "Set_Destination");
                    new PlacesTextSeachAsync().execute(longPressedGeoPoint.getLatitude() + ", " + longPressedGeoPoint.getLongitude());
                    list.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void setOwnLocationMarker() {
        // Set Marker
        if (firstPositionFound == false) {
            {
                if (BuildConfig.debug) {
                    Log.d("Location-Status", "Set FIRST Position: " + Core.startLat + " and " + Core.startLon + " Error: " + Core.lastErrorGPS);
                }
            }
            if (Core.lastErrorGPS < 100) {
                mapController.setZoom(19);
                // Log.d("Location-Status", "zoom:" + 18);
            } else if (Core.lastErrorGPS < 231) {
                mapController.setZoom(18);
                // Log.d("Location-Status", "zoom:" + 17);
            } else if (Core.lastErrorGPS < 401) {
                mapController.setZoom(17);
                // Log.d("Location-Status", "zoom:" + 16);
            } else if (Core.lastErrorGPS < 801) {
                mapController.setZoom(16);
                // Log.d("Location-Status", "zoom:" + 15);
            } else if (Core.lastErrorGPS < 1501) {
                mapController.setZoom(15);
                // Log.d("Location-Status", "zoom:" + 14);
            }

            OverlayManager om = map.getOverlayManager();
            if (om.size() == 1) {
                myLocationOverlay = new DirectedLocationOverlay(this);
                map.getOverlays().add(myLocationOverlay);
            }
            int latE6 = (int) (Core.startLat * 1E6);
            int lonE6 = (int) (Core.startLon * 1E6);
            myLocationOverlay.setLocation(new GeoPoint(latE6, lonE6));
            firstPositionFound = true;
        }
    }

    public void setPosition(boolean zoom) {
        int latE6 = (int) (Core.startLat * 1E6);
        int lonE6 = (int) (Core.startLon * 1E6);
        if (firstPositionFound == false) {
            {
                Log.d("Location-Status", "FirstPosition: " + latE6 + " and " + lonE6);
            }
            setOwnLocationMarker();
        } else {
            {
                Log.d("Location-Status", "Set Position: " + latE6 + " and " + lonE6);
            }

            myLocationOverlay.setLocation(new GeoPoint(latE6, lonE6));

            if (zoom == true) {
                if (Core.lastErrorGPS < 100) {
                    mapController.setZoom(19);
                    // Log.d("Location-Status", "zoom:" + 18);
                } else if (Core.lastErrorGPS < 231) {
                    mapController.setZoom(18);
                    // Log.d("Location-Status", "zoom:" + 17);
                } else if (Core.lastErrorGPS < 401) {
                    mapController.setZoom(17);
                    // Log.d("Location-Status", "zoom:" + 16);
                } else if (Core.lastErrorGPS < 801) {
                    mapController.setZoom(16);
                    // Log.d("Location-Status", "zoom:" + 15);
                } else if (Core.lastErrorGPS < 1501) {
                    mapController.setZoom(15);
                    // Log.d("Location-Status", "zoom:" + 14);
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    public void startHandler() {
        listHandler = new Handler() {

            public void handleMessage(Message msg) {
                if (msg.what == 3) {
                    finish(); // used by Settings to change to GoogleMap
                } else if (msg.what == 4) {
                    positionUpdate();
                } else if (msg.what == 6) {
                    // initialize Autocorrect or start new because of activity_settings change
                    SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                    autoCorrect = settings.getBoolean("autocorrect", false);
                    //First look if AutoCorrect should really be activated - important because
                    //stopLocationer relies on that
                    if (autoCorrect) {
                        int i = settings.getInt("gpstimer", 1);
                        if (i == 0) { //priority on energy saving
                            autoCorrectFaktor = 4;
                        } else if (i == 1) { // balanced
                            autoCorrectFaktor = 2;
                        } else if (i == 2) { // high accuracy
                            autoCorrectFaktor = 1;
                        }
                        alreadyWaitingForAutoCorrect = false;
                    }
                } else if (msg.what == 7) {
                    autoCorrect = false;
                    mLocationer.stopAutocorrect();
                } else if (msg.what == 9) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            restartListenerLight();
                            listHandler.sendEmptyMessageDelayed(9, 5000);
                        }
                    }, 5000);
                } else if (msg.what == 10) {
                    restartListenerLight();
                } else if (msg.what == 11) {
                    try {
                        list = (ListView) findViewById(R.id.listeOsm);
                        if (listIsVisible) {
                            list.setVisibility(View.INVISIBLE);
                            listIsVisible = false;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                super.handleMessage(msg);
            }
        };
    }

    @Override
    public void onLocationUpdate(int event) {
        switch (event) {
            case 0:
                setOwnLocationMarker();
                setPosition(true);
                positionUpdate();
                restartListener();
                // foreignIntent();
                // starte Autocorrect if wanted
                listHandler.sendEmptyMessage(6);
                return;
            case 5:
                showGPSDialog();
            case 8:
                Core.setLocation(Locationer.startLat, Locationer.startLon);
                mLocationer.stopAutocorrect();
                if (backgroundServiceShallBeOn == true) {
                    Config.backgroundServiceActive = true;
                    BackgroundService.reactivateFakeProvider();
                }
            case 12:
                // message from Locationer
                ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
                if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
            case 14:
                // next position from Locationer
                setPosition(true);
        }
    }

    private void positionUpdate() {
        int latE6 = (int) (Core.startLat * 1E6);
        int lonE6 = (int) (Core.startLon * 1E6);
        if (myLocationOverlay != null) {
            myLocationOverlay.setLocation(new GeoPoint(latE6, lonE6));
            myLocationOverlay.setBearing((float) Core.azimuth);
            map.invalidate();
            if (followMe) {
                mapController.animateTo(new GeoPoint(latE6, lonE6));
            }
        }
        listHandler.removeMessages(4);
        listHandler.sendEmptyMessageDelayed(4, POS_UPDATE_FREQ);
    }


    private void checkOfflineMapsDirectory() {
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String offlineMapsPath = settings.getString("offlinemapspath", null);
        File osmdroidDirectory = new File(Environment.getExternalStorageDirectory(), "/osmdroid/tiles");
        if (offlineMapsPath != null) {
            //Use saved custom tile storage path
            OpenStreetMapTileProviderConstants.setOfflineMapsPath(offlineMapsPath);
            OpenStreetMapTileProviderConstants.setCachePath(offlineMapsPath);
        } else if (offlineMapsPath == null && !osmdroidDirectory.exists()) {
            //Initialize default tile storage path
            File mapsDirectory = new File(Environment.getExternalStorageDirectory(), Config.DEFAULT_OFFLINE_MAPS_FOLDER);
            if (!mapsDirectory.exists()) {
                boolean geschafft = mapsDirectory.mkdirs();
                Log.i("ordner", "OSMMap erstellen: " + geschafft);
            }
            OpenStreetMapTileProviderConstants.setOfflineMapsPath(Config.DEFAULT_OFFLINE_MAPS_PATH);
            OpenStreetMapTileProviderConstants.setCachePath(Config.DEFAULT_OFFLINE_MAPS_PATH);
            new writeSettings("offlinemapspath", Config.DEFAULT_OFFLINE_MAPS_PATH).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (offlineMapsPath == null && osmdroidDirectory.exists()) {
            //Already existing directory but not saved yet in SharePreferences
            OpenStreetMapTileProviderConstants.setOfflineMapsPath("/sdcard/osmdroid/tiles");
            OpenStreetMapTileProviderConstants.setCachePath("/sdcard/osmdroid/tiles");
            new writeSettings("offlinemapspath", "/sdcard/osmdroid/tiles").executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    protected void onResume() {
        firstPositionFound = false;
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        autoCorrect = settings.getBoolean("autocorrect", false);
        Config.usingGoogleMaps = false;
        setFollowOn();
        positionUpdate();
        if (userSwitchesGPS == false) {
            restartListener();

            if (knownReasonForBreak == true) {
                checkOfflineMapsDirectory();
                // User is coming from Settings, Background Service or About
                knownReasonForBreak = false;
            } else {
                // User calls onResume, probably because Screen was off and turned on again
                // get Position and go on
                mLocationer.startLocationUpdates();
                ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
                mProgressBar.setVisibility(View.VISIBLE);
            }
            if (Core.startLat != 0) {
                listHandler.sendEmptyMessage(0);
            }

            // Export
            boolean export = settings.getBoolean("export", false);
            try {
                mCore.writeLog(export);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (userSwitchesGPS == true) {
            // User has been sent by SmartNavi into his activity_settings to check GPS etc.
            //so start getting a new location
            mLocationer.startLocationUpdates();
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        String tileProviderName = settings.getString("MapSource", "MapQuestOSM");
        if (tileProviderName.equalsIgnoreCase("MapQuestOSM")) {
            // in the following line the Zoom-Level could be raised from 19 to 20, but tiles are currently not reloading when map is moving
            map.setTileSource(new XYTileSource("MapquestOSM", 0, 19, 256, ".jpg", new String[]{
                    "http://otile1.mqcdn.com/tiles/1.0.0/map/", "http://otile2.mqcdn.com/tiles/1.0.0/map/", "http://otile3.mqcdn.com/tiles/1.0.0/map/",
                    "http://otile4.mqcdn.com/tiles/1.0.0/map/"}));
        } else {
            try {
                ITileSource tileSource = TileSourceFactory.getTileSource("Mapnik"); //do not change this string
                map.setTileSource(tileSource);
            } catch (IllegalArgumentException e) {
                map.setTileSource(TileSourceFactory.MAPNIK);
            }
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLocationer != null) {
            mLocationer.deactivateLocationer();
        }
        try {
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
            mProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            //nothing
        }
        if (listHandler != null) {
            listHandler.removeMessages(4);
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    public void setFollowOn() {
        followMe = true;
        map.setMapOrientation(0);
        int latE6 = (int) (Core.startLat * 1E6);
        int lonE6 = (int) (Core.startLon * 1E6);
        mapController.animateTo(new GeoPoint(latE6, lonE6));
        fab.hide();
    }

    public void setFollowOff() {
        followMe = false;
        fab.show();
    }

    private void restartListener() {
        units = 0;
        aclUnits = 0;
        magnUnits = 0;
        startTime = System.nanoTime();
        try {
            mSensorManager.registerListener(OsmMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
            mSensorManager.registerListener(OsmMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(this);
        listHandler.removeMessages(0);
        Statistics mStatistics = new Statistics();
        mStatistics.check(this);
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        map.getTileProvider().clearTileCache();
        super.onStop();
    }

    private void restartListenerLight() {
        try {
            mSensorManager.unregisterListener(OsmMap.this);
            mSensorManager.registerListener(OsmMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
            mSensorManager.registerListener(OsmMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //sensor warning
        //If sensors are inaccurate, tell the user to rotate his device
        //Commented out, because the accuracy data is useless on some devices :(
//        if (event.accuracy < 1){
//            sensorUnreliabilityCounter++;
//            if(sensorUnreliabilityCounter > 200 && sensorWarningShown == false){
//                SharedPreferences activity_settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
//                boolean longPressHasBeenShown = activity_settings.getBoolean("longPressWasShown", false);
//                if(longPressHasBeenShown){
//                    sensorWarningShown = true;
//                    View sensorWarning = findViewById(R.id.sensorWarningOsm);
//                    sensorWarning.setVisibility(View.VISIBLE);
//                    Button sensorWarningButton = (Button) findViewById(R.id.sensorWarningButtonOsm);
//                    sensorWarningButton.setOnClickListener(new OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//                            View sensorWarning = findViewById(R.id.sensorWarningOsm);
//                            sensorWarning.setVisibility(View.GONE);
//                        }
//                    });
//                }
//            }
//        }
        //end of sensor warning

        switch (event.sensor.getType()) {

            case Sensor.TYPE_MAGNETIC_FIELD:
                mCore.imbaMagnetic(event.values.clone());
            {
                Core.origMagn = event.values.clone();
            }
            magnUnits++;
            break;

            case Sensor.TYPE_ACCELEROMETER: {
                Core.origAcl = event.values.clone();
            }
            timePassed = System.nanoTime() - startTime;
            aclUnits++;
            units++;

            if (timePassed >= 2000000000) {
                mCore.changeDelay(aclUnits / 2, 0);
                mCore.changeDelay(magnUnits / 2, 1);

                aclUnits = magnUnits = 0;
                startTime = System.nanoTime();
            }

            if (Config.backgroundServiceActive == true && units % 50 == 0) {
                BackgroundService.newFakePosition();
            }

            mCore.imbaGravity(event.values.clone());
            mCore.imbaLinear(event.values.clone());
            mCore.calculate();
            mCore.stepDetection();

            // Autokorrektur dependent on stepCounter
            if (autoCorrect) {
                if (alreadyWaitingForAutoCorrect == false) {
                    alreadyWaitingForAutoCorrect = true;
                    stepsToWait = Core.stepCounter + 75 * autoCorrectFaktor;
                }
                if (Core.stepCounter >= stepsToWait) {
                    if (Config.backgroundServiceActive == true) {
                        backgroundServiceShallBeOn = true;
                        BackgroundService.pauseFakeProvider();
                    }
                    mLocationer.starteAutocorrect();
                    alreadyWaitingForAutoCorrect = false;
                }
            }
            break;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            listHandler.removeMessages(4);
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (followMe == true) {
                setFollowOff();
            }
        }
        //Make longPressList invisible, but it is important to wait some time
        //if not, it my happen, that the list will be made invisible, BEFORE the buttonListener of the list
        //can react
        listHandler.sendEmptyMessageDelayed(11, 250);

        if (event.getAction() == MotionEvent.ACTION_UP) {
            listHandler.sendEmptyMessage(4);
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public void finish() {
        //this prevents an exception with ZoomButtonControllers of osmDroid
        ViewGroup view = (ViewGroup) getWindow().getDecorView();
        view.removeAllViews();
        super.finish();
    }

    private void showGPSDialog() {
        final Dialog dialogGPS = new Dialog(OsmMap.this);
        dialogGPS.setContentView(R.layout.dialog3);
        dialogGPS.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
        dialogGPS.setCanceledOnTouchOutside(false);
        dialogGPS.show();
        mAnalytics.trackEvent("GPS_Dialog_View", "OSM_View");

        Button cancel = (Button) dialogGPS.findViewById(R.id.dialogCancelgps);
        cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mAnalytics.trackEvent("OSM_GPS_Dialog_Action", "Cancel");
                dialogGPS.dismiss();
            }
        });

        Button settingsGPS = (Button) dialogGPS.findViewById(R.id.dialogSettingsgps);
        settingsGPS.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                mAnalytics.trackEvent("OSM_GPS_Dialog_Action", "Go_To_Settings");
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    userSwitchesGPS = true;
                } catch (ActivityNotFoundException ae) {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    userSwitchesGPS = true;
                }
                dialogGPS.dismiss();
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_47), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    // Touch on ProgressBar
    public void abortGPS(final View view) {
        // Abort GPS was pressed (ProgressBar was pressed)
        try {
            mAnalytics.trackEvent("GPS_Cancel_Pressed", "pressed_on_OSM");
            mLocationer.deactivateLocationer();
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
            mProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, getResources().getString(R.string.tx_82), Toast.LENGTH_SHORT).show();
    }

    private void showLongPressDialog() {
        try {
            myItemizedOverlay[0].getItem(0).getDrawable().setVisible(false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final View longPressDialog = findViewById(R.id.longpPressDialogOsm);
        longPressDialog.setVisibility(View.VISIBLE);

        Button longPressButtonOsm = (Button) findViewById(R.id.longPressButtonOsm);
        longPressButtonOsm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                longPressDialog.setVisibility(View.GONE);
                try {
                    myItemizedOverlay[0].getItem(0).getDrawable().setVisible(true, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // Remember that longPress was shown
        new writeSettings("longPressWasShown", true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void clickOnStars(final View view) {
        new writeSettings("not_rated", 999).execute();
        final View appRateDialog = findViewById(R.id.appRateDialogOsm);
        appRateDialog.setVisibility(View.INVISIBLE);
        new writeSettings("dontshowagain", true).execute();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        mainMenu = menu;
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        searchView = (SearchView) searchItem.getActionView();
        prepareSearchView();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            try {
                // if off, longPressMenu will be made invisible
                list = (ListView) findViewById(R.id.listeOsm);
                if (list.getVisibility() == View.VISIBLE)
                list.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // if off, longPressMenu will be made invisible
        try {
            list = (ListView) findViewById(R.id.listeOsm);
            if (list.getVisibility() == View.VISIBLE)
            list.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            if (BuildConfig.debug)
            e.printStackTrace();
        }

        switch (item.getItemId()) {
            case R.id.menu_bgservice:
                // activity_backgroundservice / background service
                mAnalytics.trackEvent("OSM_Menu", "background_service");
                knownReasonForBreak = true;
                Intent myIntent = new Intent(this, BackgroundService.class);
                startActivity(myIntent);
                return true;
            case R.id.menu_offlinemaps:
                mAnalytics.trackEvent("OSM_Menu", "offline_maps");
                startActivity(new Intent(OsmMap.this, Webview.class));
                return true;
            case R.id.menu_settings:
                mAnalytics.trackEvent("OSM_Menu", "settings");
                // Go to Settings
                knownReasonForBreak = true;
                startActivity(new Intent(this, Settings.class));
                return true;
            case R.id.menu_tutorial:
                mAnalytics.trackEvent("OSM_Menu", "tutorial");
                // open TutorialFragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                if (tutorialFragment != null) {
                    tutorialFragment = new TutorialFragment();
                    fragmentTransaction.add(R.id.osmmap_actvity_layout, tutorialFragment);
                    fragmentTransaction.commit();
                } else {
                    tutorialFragment = new TutorialFragment();
                    fragmentTransaction.add(R.id.osmmap_actvity_layout, tutorialFragment);
                    fragmentTransaction.commit();
                }
                return true;
            case R.id.menu_info:
                mAnalytics.trackEvent("OSM_Menu", "info");
                // go to About Page
                knownReasonForBreak = true;
                startActivity(new Intent(this, Info.class));
                return true;
            case android.R.id.home:
                // back und finish
                finish();
                return (true);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        longPressedGeoPoint = p;
        list = (ListView) findViewById(R.id.listeOsm);
        list.setVisibility(View.VISIBLE);
        mAnalytics.trackEvent("LongPress_View", "OSM_View");
        //Set this variable after quite some time, so that the dialog
        //has a guaranteed mininum lifetime and is not discarded after a short
        //random touch on the screen
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                listIsVisible = true;
            }
        }, 1200);
        return false;
    }

    private void prepareSearchView() {
        searchView.setSuggestionsAdapter(mSuggestionsAdapter);

        // onClick closes the longPressMenu if it is shown
        searchView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // if off, longPressMenu will be made invisible
                try {
                    list = (ListView) findViewById(R.id.listeOsm);
                    if (list.getVisibility() == View.VISIBLE)
                    list.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    if (BuildConfig.debug)
                    e.printStackTrace();
                }
            }
        });

        // autocomplete suggestions
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                // close virtual keyboard
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                try {
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // display query as hint
                searchView.setQueryHint(query);
                searchView.setQuery(query, false);
                searchView.clearFocus();
                // close search input field after 10 sec
                searchView.clearFocus();
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        mainMenu.getItem(0).collapseActionView();
                    }
                }, 10000);

                // easter eggs
                if (query.equalsIgnoreCase("Chuck Norris"))
                    Toast.makeText(OsmMap.this, "You can not find Chuck Norris. Chuck Norris finds YOU!", Toast.LENGTH_LONG).show();
                else if (query.equalsIgnoreCase("cake") || query.equalsIgnoreCase("the cake") || query.equalsIgnoreCase("portal"))
                    Toast.makeText(OsmMap.this, "The cake is a lie!", Toast.LENGTH_LONG).show();
                else if (query.equalsIgnoreCase("smartnavihelp")) {
                    // User ID anzeigen
                    SharedPreferences activity_settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                    uid = activity_settings.getString("uid", "0");
                    View viewLine = findViewById(R.id.view156);
                    viewLine.setVisibility(View.VISIBLE);
                    TextView mapText = (TextView) findViewById(R.id.mapText);
                    mapText.setVisibility(View.VISIBLE);
                    mapText.setText("Random User ID: " + uid);
                }
                // search coordinates for autocomplete result
                else if (isOnline()) {
                    showRouteInfo(true);
                    if (Config.PLACES_API_UNDER_LIMIT) {
                        new PlacesTextSeachAsync().execute(query);
                    }
                    //react to exceeded places api limit. Use GeocodeTask from GoogleMap
                    //but should not happen due to high limit
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // min 3 chars before autocomplete
                if (query.length() >= Config.PLACES_SEARCH_QUERY_CHARACTER_LIMIT) {
                    // prevent hammering
                    if (!suggestionsInProgress) {
                        // get suggestions
                        new PlacesAutoComplete().execute(query);
                        suggestionsInProgress = true;
                    }
                } else {
                    // clear suggestion list
                    mSuggestionsAdapter = new SuggestionsAdapter(sbContext, new MatrixCursor(Config.COLUMNS));
                    searchView.setSuggestionsAdapter(mSuggestionsAdapter);
                    searchView.getSuggestionsAdapter().notifyDataSetChanged();
                }
                return true;
            }
        });

        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                Cursor c = (Cursor) mSuggestionsAdapter.getItem(position);
                String query = c.getString(c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));

                searchView.setQuery(query, true);

                return true;
            }
        });

        // remote update of suggestion adapter
        changeSuggestionAdapter = new Handler() {

            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    mSuggestionsAdapter = new SuggestionsAdapter(toolbar.getContext(), cursor);
                    searchView.setSuggestionsAdapter(OsmMap.mSuggestionsAdapter);
                    // important to update suggestion list
                    searchView.getSuggestionsAdapter().notifyDataSetChanged();
                    suggestionsInProgress = false;
                }
                super.handleMessage(msg);
            }
        };
    }


    private void showRouteInfo(boolean show) {
        TextView mapText = (TextView) findViewById(R.id.mapTextOsm);
        View viewLine = findViewById(R.id.viewLineOsm);
        mapText.setText(getApplicationContext().getResources().getString(R.string.tx_04));
        if (show) {
            viewLine.setVisibility(View.VISIBLE);
            mapText.setVisibility(View.VISIBLE);
        } else {
            viewLine.setVisibility(View.GONE);
            mapText.setVisibility(View.GONE);
        }
    }

    private void setNewPositionMarker() {
        myLocationOverlay = new DirectedLocationOverlay(this);
        map.getOverlays().add(myLocationOverlay);
        int latE6 = (int) (Core.startLat * 1E6);
        int lonE6 = (int) (Core.startLon * 1E6);
        myLocationOverlay.setLocation(new GeoPoint(latE6, lonE6));
    }

    @Override
    public void onTutorialFinished() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(tutorialFragment).commit();
    }

    private class writeSettings extends AsyncTask<Void, Void, Void> {

        private String key;
        private int dataType;
        private boolean setting1;
        private String setting2;
        private int setting3;

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

        private writeSettings(String key, int setting3) {
            this.key = key;
            this.setting3 = setting3;
            dataType = 2;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
            if (dataType == 0) {
                settings.edit().putBoolean(key, setting1).commit();
            } else if (dataType == 1) {
                settings.edit().putString(key, setting2).commit();
            } else if (dataType == 2) {
                settings.edit().putInt(key, setting3).commit();
            }
            return null;
        }
    }

    private class PlacesTextSeachAsync extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... input) {
            PlacesTextSearch textSearch = new PlacesTextSearch(getBaseContext());
            JSONObject destination = textSearch.getDestinationCoordinates(input[0]);
            return destination;
        }

        @Override
        protected void onPostExecute(JSONObject destination) {

            super.onPostExecute(destination);

            // no results from api
            if (destination == null) {
                Toast.makeText(OsmMap.this, getApplicationContext().getResources().getString(R.string.tx_77), Toast.LENGTH_LONG).show();
                showRouteInfo(false);
            } else {
                // set destination for the routing tasks
                try {
                    double destLat = (Double) destination.get("lat");
                    double destLon = (Double) destination.get("lng");
                    listHandler.removeCallbacksAndMessages(null);
                    //dont follow, but of course still update position
                    setFollowOff();
                    listHandler.sendEmptyMessage(4);

                    setPosition(false);

                    //Set-up your start and end points:
                    ArrayList<GeoPoint> waypoints = new ArrayList<>();
                    waypoints.add(new GeoPoint(Core.startLat, Core.startLon));
                    GeoPoint endPoint = new GeoPoint(destLat, destLon);
                    waypoints.add(endPoint);

                    RoadManagerTask rm = new RoadManagerTask();
                    rm.setContext(getApplicationContext());
                    rm.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, waypoints);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class RoadManagerTask extends AsyncTask<ArrayList<GeoPoint>, Void, Polyline> {
        private Road road;
        private Context mContext;

        public void setContext(Context context) {
            mContext = context;
        }

        @Override
        protected Polyline doInBackground(ArrayList<GeoPoint>... waypoints) {
            // old without pedestrian support
            // RoadManager roadManager = new OSRMRoadManager();
            RoadManager roadManager = new MapQuestRoadManager(Config.MAPQUEST_API_KEY, mContext);
            roadManager.addRequestOption("routeType=pedestrian");
            // retreive the road between those points:
            road = roadManager.getRoad(waypoints[0]);
            //build a Polyline with the route shape
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road, OsmMap.this);
            roadOverlay.setColor(Color.BLUE);
            return roadOverlay;
        }

        @Override
        protected void onPostExecute(Polyline roadOverlay) {
            OverlayManager om = map.getOverlayManager();
            //remove all current polylines or road"step"markers
            do {
                om.remove(1);
            } while (om.size() > 1);

            //Show Route Polyline
            FolderOverlay roadMarkers = new FolderOverlay(mContext);
            map.getOverlays().add(roadOverlay);

            showRouteInfo(false);

            //Show Route Steps on the map
            Drawable nodeIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.finish2, null);
            for (int i = 0; i < road.mNodes.size(); i++) {
                RoadNode node = road.mNodes.get(i);
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(node.mLocation);
                nodeMarker.setIcon(nodeIcon);
                nodeMarker.setTitle("Step " + i);
                nodeMarker.setSnippet(node.mInstructions);
                nodeMarker.setSubDescription(Road.getLengthDurationText(node.mLength, node.mDuration));
                Drawable icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_continue, null);
                if (node.mManeuverType == 1) {
                    icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_continue, null);
                } else if (node.mManeuverType == 3) {
                    icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_slight_left, null);
                } else if (node.mManeuverType == 4 || node.mManeuverType == 5) {
                    icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_turn_left, null);
                } else if (node.mManeuverType == 6) {
                    icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_slight_right, null);
                } else if (node.mManeuverType == 7 || node.mManeuverType == 8) {
                    icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_turn_right, null);
                } else if (node.mManeuverType == 24 || node.mManeuverType == 25 || node.mManeuverType == 26) {
                    icon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_arrived, null);
                }
                nodeMarker.setImage(icon);
                nodeMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker, MapView mapView) {
                        marker.showInfoWindow();
                        return true;
                    }
                });
                roadMarkers.add(nodeMarker);
            }
            map.getOverlays().add(roadMarkers);
            setNewPositionMarker();
            map.invalidate();
        }
    }

}
