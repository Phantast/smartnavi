package com.ilm.sandwich;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.actionbarsherlock.widget.SearchView.OnSuggestionListener;
import com.ilm.sandwich.tools.Core;
import com.ilm.sandwich.tools.Locationer;
import com.ilm.sandwich.tools.MyItemizedOverlay;
import com.ilm.sandwich.tools.PlacesAutoComplete;
import com.ilm.sandwich.tools.PlacesTextSearch;
import com.ilm.sandwich.tools.Statistics;
import com.ilm.sandwich.tools.SuggestionsAdapter;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.ResourceProxy;
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
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayManager;

import java.text.DecimalFormat;
import java.util.AbstractList;
import java.util.ArrayList;

/**
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
@SuppressLint({"NewApi"})
public class OsmMapActivity extends SherlockActivity implements SensorEventListener, MapEventsReceiver {

    public static Handler listHandler;
    public static SearchView searchView;
    public static boolean firstPositionFound;
    public static SuggestionsAdapter mSuggestionsAdapter;
    public static boolean suggestionsInProgress = false;
    public static MatrixCursor cursor = new MatrixCursor(Config.COLUMNS);
    public static Handler changeSuggestionAdapter;
    static ListView list;
    static DecimalFormat df0 = new DecimalFormat("0");
    private static SubMenu subMenu1;
    private static Menu mainMenu;
    private static GeoPoint longPressedGeoPoint;
    public boolean metricUnits = true;
    public Context sbContext;
    protected DirectedLocationOverlay myLocationOverlay;
    View tutorialOverlay;
    private MapView map;
    private IMapController mapController;
    private MyItemizedOverlay[] myItemizedOverlay = new MyItemizedOverlay[10];
    private SensorManager mSensorManager;
    private Core mCore;
    private String uid;
    private int iteration = 1;
    private Locationer mLocationer;
    private boolean userSwitchesGPS;
    private boolean knownReasonForBreak;
    private int units;
    private int aclUnits;
    private int magnUnits;
    private long startTime;
    private long timePassed;
    private boolean followMe = true;
    private int compassStatus;
    private boolean egoPerspective;
    private boolean listIsVisible = false;
    private long POS_UPDATE_FREQ = 2200;
    private boolean autoCorrect = false;
    private int autoCorrectFaktor = 1;
    private boolean alreadyWaitingForAutoCorrect;
    private int stepsToWait = 0;
    private boolean backgroundServiceShallBeOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Config.usingGoogleMaps = false;
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));


        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        setContentView(R.layout.osmmap_layout);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        map = (MapView) findViewById(R.id.openmapview);
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(mapEventsOverlay);
        map.setBuiltInZoomControls(true);
        map.setKeepScreenOn(true);
        map.setMultiTouchControls(true);

        prepareSearchView();

        String tileProviderName = settings.getString("MapSource", "MapQuestOSM");
        if (tileProviderName.equalsIgnoreCase("MapQuestOSM")) {
            // in the following line the Zoom-Level could be raised from 19 to 20, but tiles are currently not reloading when map is moving
            map.setTileSource(new XYTileSource("MapquestOSM", ResourceProxy.string.mapquest_osm, 0, 19, 256, ".jpg", new String[]{
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
        //mapController.setCenter(new GeoPoint(defaultLat, defaultLon));
        mapController.setZoom(3);
        map.setMultiTouchControls(true);

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new writeSettings("follow", true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new writeSettings("follow", true).execute();
        }

        uid = settings.getString("uid", "0");
        if (uid.equalsIgnoreCase("0")) {
            String neuUID = "" + (1 + (int) (Math.random() * ((10000000 - 1) + 1)));
            new writeSettings("uid", neuUID).execute();
            uid = settings.getString("uid", "0");
        }

        mLocationer = new Locationer(this);

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
                if (Config.debugMode)
                    e.printStackTrace();
            }

            // Tutorial has been shown but longPressDialog has not
            //so please show it
            boolean longPressHasBeenShown = settings.getBoolean("longPressWasShown", false);
            if (longPressHasBeenShown == false) {
                showLongPressDialog();
            }
        } else {
            tutorialStuff();
        }

        // Rate App show for debugging
        // showRateDialog();
        // Rate App live
        appRateDialog();

        positionUpdate();

        list = (ListView) findViewById(R.id.listeOsm);
        list.setVisibility(View.INVISIBLE);
        listIsVisible = false;
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    Core.setLocation(longPressedGeoPoint.getLatitude(), longPressedGeoPoint.getLongitude());
                    list.setVisibility(View.INVISIBLE);
                    listIsVisible = false;
                    setFollowOn();
                    map.invalidate();
                    mapController.animateTo(longPressedGeoPoint);
                    // Positionstask reactivate
                    listHandler.sendEmptyMessageDelayed(4, 50);
                } else {
                    showRouteInfo(true);
                    new PlacesTextSeachAsync().execute(longPressedGeoPoint.getLatitude() + ", " + longPressedGeoPoint.getLongitude());
                    list.setVisibility(View.INVISIBLE);
                    listIsVisible = false;
                }
            }
        });
    }


    private void setOwnLocationMarker() {
        // Set Marker
        if (firstPositionFound == false) {
            if (Config.debugMode) {
                Log.d("Location-Status", "Set FIRST Position: " + Core.startLat + " and " + Core.startLon + " Error: " + Core.lastErrorGPS);
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
            if (Config.debugMode) {
                Log.d("Location-Status", "FirstPosition: " + latE6 + " and " + lonE6);
            }
            setOwnLocationMarker();
        } else {
            if (Config.debugMode) {
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
                if (msg.what == 0) {
                    setOwnLocationMarker();
                    setPosition(true);
                    positionUpdate();
                    restartListener();
                    // foreignIntent();
                    // starte Autocorrect if wanted
                    listHandler.sendEmptyMessage(6);
                } else if (msg.what == 1) {
                    // set margin for compass, dependent on the height of the actionbar
                    int height = getSherlock().getActionBar().getHeight();
                    if (height > 0) {
                        try {
                            ImageView compass = (ImageView) findViewById(R.id.osmNadel);
                            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(10, height + 10, 0, 0);
                            compass.setLayoutParams(lp);
                            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
                            RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                    RelativeLayout.LayoutParams.WRAP_CONTENT);
                            lp2.setMargins(10, height + 10, 0, 0);
                            lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                            mProgressBar.setLayoutParams(lp2);
                        } catch (Exception e) {
                            if (Config.debugMode)
                                e.printStackTrace();
                        }
                    } else {
                        listHandler.sendEmptyMessageDelayed(1, 100);
                    }
                } else if (msg.what == 2) {
                    if (egoPerspective) {
                        map.setMapOrientation((float) Core.azimuth * (-1));
                    }
                    listHandler.sendEmptyMessageDelayed(2, 5);
                } else if (msg.what == 3) {
                    finish(); // used by Settings to change to GoogleMapActivity
                } else if (msg.what == 4) {
                    listHandler.removeMessages(0);
                    positionUpdate();
                } else if (msg.what == 5) {
                    // Dialog if user has LocationSettings disabled
                    final Dialog dialog = new Dialog(OsmMapActivity.this);
                    dialog.setContentView(R.layout.dialog2);
                    dialog.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();

                    Button cancel = (Button) dialog.findViewById(R.id.dialogCancelLoc);
                    cancel.setOnClickListener(new OnClickListener() {
                        public void onClick(View arg0) {
                            dialog.dismiss();
                        }
                    });

                    Button settings = (Button) dialog.findViewById(R.id.dialogSettingsLoc);
                    settings.setOnClickListener(new OnClickListener() {
                        public void onClick(View arg0) {
                            try {
                                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            } catch (android.content.ActivityNotFoundException ae) {
                                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                            }
                            dialog.dismiss();
                            finish();
                        }
                    });
                } else if (msg.what == 6) {
                    // initialize Autocorrect or start new because of settings change
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
                } else if (msg.what == 8) {
                    Core.setLocation(Locationer.startLat, Locationer.startLon);
                    mLocationer.stopAutocorrect();
                    if (backgroundServiceShallBeOn == true) {
                        Config.backgroundServiceActive = true;
                        BackgroundService.reactivateFakeProvider();
                    }
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
                    if (listIsVisible) {
                        list.setVisibility(View.GONE);
                        listIsVisible = false;
                    }
                } else if (msg.what == 12) {
                    try {
                        // message from Locationer
                        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
                        mProgressBar.setVisibility(View.GONE);
                    } catch (Exception e) {
                        //nothing, may happen sometimes if the views of the activity are already destroyed
                    }

                } else if (msg.what == 13) {
                    showGPSDialog();
                } else if (msg.what == 14) {
                    // next position from Locationer
                    setPosition(true);
                }
                super.handleMessage(msg);
            }
        };
    }

    private void positionUpdate() {
        if (firstPositionFound) {
            int latE6 = (int) (Core.startLat * 1E6);
            int lonE6 = (int) (Core.startLon * 1E6);

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

    @Override
    protected void onResume() {
        firstPositionFound = false;

        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        autoCorrect = settings.getBoolean("autocorrect", false);

        Config.usingGoogleMaps = false;
        try {
            egoPerspective = false;
            compassStatus = 1;
            ImageView compass = (ImageView) findViewById(R.id.osmNadel);
            compass.setImageResource(R.drawable.needle);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }

        if (userSwitchesGPS == false) {
            restartListener();

            if (knownReasonForBreak == true) {
                // User is coming from Settings, Background Service or About
                knownReasonForBreak = false;
            } else {
                // User calls onResume, probably because Screen was off and turned on again
                //get Position and go on
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
                if (Config.debugMode)
                    e.printStackTrace();
            }

        } else if (userSwitchesGPS == true) {
            // User has been sent by SmartNavi into his settings to check GPS etc.
            //so start getting a new location
            mLocationer.startLocationUpdates();
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        // CompassNeedle and ProgressBar position down with margin
        listHandler.sendEmptyMessageDelayed(1, 10);

        String tileProviderName = settings.getString("MapSource", "MapQuestOSM");
        if (tileProviderName.equalsIgnoreCase("MapQuestOSM")) {
            // in the following line the Zoom-Level could be raised from 19 to 20, but tiles are currently not reloading when map is moving
            map.setTileSource(new XYTileSource("MapquestOSM", ResourceProxy.string.mapquest_osm, 0, 19, 256, ".jpg", new String[]{
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
        try {
            if (mLocationer != null) {
                mLocationer.deactivateLocationer();
            }
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        try {
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
            mProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            //nothing
        }

        try {
            listHandler.removeMessages(4);
            // Log.d("Location-Status", "Positiontask OFF    onPause");
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        try {
            mSensorManager.unregisterListener(this);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }

        super.onPause();
    }

    public void setFollowOn() {
        ImageView compass = (ImageView) findViewById(R.id.osmNadel);
        followMe = true;
        if (compassStatus == 2) {
            // setze Status 1
            compass.setImageResource(R.drawable.needle);
            compassStatus = 1;
        } else if (compassStatus == 4) {
            // setze Status 3
            compass.setImageResource(R.drawable.needle3);
            compassStatus = 3;
        }
    }

    public void setFollowOff() {
        ImageView compass = (ImageView) findViewById(R.id.osmNadel);
        followMe = false;
        if (compassStatus == 1) {
            // setze Status 2
            compass.setImageResource(R.drawable.needle2);
            compassStatus = 2;
        } else if (compassStatus == 3) {
            // setze Status 4
            compass.setImageResource(R.drawable.needle4);
            compassStatus = 4;
        }
    }

    public void compassNeedleOsm(final View view) {
        ImageView compass = (ImageView) findViewById(R.id.osmNadel);

        listHandler.removeMessages(0);
        listHandler.removeMessages(2);

        if (compassStatus == 1) {
            // status 3
            compass.setImageResource(R.drawable.needle3);
            compassStatus = 3;
            // Camera in Compass Direction
            egoPerspective = true;
            listHandler.sendEmptyMessage(2);
        } else if (compassStatus == 3) {
            // status 1
            egoPerspective = false;
            compass.setImageResource(R.drawable.needle);
            compassStatus = 1;
            // camera points north
            map.setMapOrientation(0);
        } else if (compassStatus == 4) {
            // status 3
            setFollowOn();
            compass.setImageResource(R.drawable.needle3);
            compassStatus = 3;
        } else if (compassStatus == 2) {
            // status 1
            setFollowOn();
            compass.setImageResource(R.drawable.needle);
            compassStatus = 1;
        }
        // Positionstask reactivate
        listHandler.sendEmptyMessageDelayed(4, 50);
    }

    public void setCompassNeedleOsmOff() {
        ImageView compass = (ImageView) findViewById(R.id.osmNadel);
        listHandler.removeMessages(0);
        listHandler.removeMessages(2);
        // status 2
        egoPerspective = false;
        compass.setImageResource(R.drawable.needle2);
        compassStatus = 2;
        map.setMapOrientation(0);
        // Positionstask reactivate
        listHandler.sendEmptyMessageDelayed(4, 50);
    }

    private void restartListener() {
        iteration = 1;
        Config.meanAclFreq = Config.meanMagnFreq = 0;

        units = 0;
        aclUnits = 0;
        magnUnits = 0;
        startTime = System.nanoTime();
        try {
            mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
            mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
        } catch (Exception e) {
            if (Config.debugMode)
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
        // // Log.d("Location-Status", "Sensoren reactivated.");
        try {
            mSensorManager.unregisterListener(OsmMapActivity.this);
            mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
            mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
        } catch (Exception e) {
            if (Config.debugMode)
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
//                SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
//                boolean longPressHasBeenShown = settings.getBoolean("longPressWasShown", false);
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
                if (Config.debugMode) {
                    Core.origMagn = event.values.clone();
                }
                magnUnits++;
                break;

            case Sensor.TYPE_ACCELEROMETER:
                if (Config.debugMode) {
                    Core.origAcl = event.values.clone();
                }
                timePassed = System.nanoTime() - startTime;
                aclUnits++;
                units++;

                if (timePassed >= 2000000000) {
                    mCore.changeDelay(aclUnits / 2, 0);
                    mCore.changeDelay(magnUnits / 2, 1);
                    // Log.d("egal", "timePassed = 2000; aclFreq = " +aclUnits/2 + " magnFreq = " + magnUnits/2);

                    // calculate mean values and save
                    Config.meanAclFreq = (Config.meanAclFreq + aclUnits / 2) / iteration;
                    Config.meanMagnFreq = (Config.meanMagnFreq + magnUnits / 2) / iteration;
                    iteration = 2;

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
        listHandler.removeMessages(0);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            listHandler.removeMessages(4);
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (followMe == true) {
                setFollowOff();
                setCompassNeedleOsmOff();
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
        final Dialog dialogGPS = new Dialog(OsmMapActivity.this);
        dialogGPS.setContentView(R.layout.dialog3);
        dialogGPS.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
        dialogGPS.setCanceledOnTouchOutside(false);
        dialogGPS.show();

        Button cancel = (Button) dialogGPS.findViewById(R.id.dialogCancelgps);
        cancel.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                dialogGPS.dismiss();
            }
        });

        Button settingsGPS = (Button) dialogGPS.findViewById(R.id.dialogSettingsgps);
        settingsGPS.setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    userSwitchesGPS = true;
                } catch (android.content.ActivityNotFoundException ae) {
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
            mLocationer.deactivateLocationer();
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
            mProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        Toast.makeText(this, getResources().getString(R.string.tx_82), Toast.LENGTH_SHORT).show();
    }

    public void tutorialStuff() {
        // show Tutorial and deactivate Clicks on Map and Longpresses
        map.setClickable(false);
        tutorialOverlay = findViewById(R.id.tutorialOverlayOsm);
        tutorialOverlay.setVisibility(View.VISIBLE);

        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String stepLengthString = settings.getString("step_length", null);
        Spinner spinner = (Spinner) findViewById(R.id.tutorialSpinnerOsm);
        // Create an ArrayAdapter using the string array and a default spinner
        // layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.dimension, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        if (stepLengthString != null) {
            try {
                stepLengthString = stepLengthString.replace(",", ".");
                int savedHeight = Integer.parseInt(stepLengthString);
                if (savedHeight < 241 && savedHeight > 119) {
                    EditText editText = (EditText) findViewById(R.id.tutorialEditTextOsm);
                    editText.setText("" + savedHeight);
                    spinner.setSelection(0);
                } else if (savedHeight < 95 && savedHeight > 45) {
                    EditText editText = (EditText) findViewById(R.id.tutorialEditTextOsm);
                    editText.setText("" + savedHeight);
                    spinner.setSelection(1);
                }
            } catch (Exception e) {
                if (Config.debugMode)
                    e.printStackTrace();
            }
        }
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    metricUnits = true;
                } else {
                    metricUnits = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        Button startButton = (Button) findViewById(R.id.startbuttonOsm);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean tutorialAbgeschlossen = false;
                final EditText heightField = (EditText) findViewById(R.id.tutorialEditTextOsm);
                int op = heightField.length();
                float number;
                if (op != 0) {
                    try {
                        number = Float.valueOf(heightField.getText().toString());
                        if (number < 241 && number > 119 && metricUnits == true) {

                            String numberString = df0.format(number);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                new writeSettings("step_length", numberString).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            } else {
                                new writeSettings("step_length", numberString).execute();
                            }
                            Core.stepLength = (float) (number / 222);
                            tutorialAbgeschlossen = true;
                        } else if (number < 95 && number > 45 && metricUnits == false) {

                            String numberString = df0.format(number);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                                new writeSettings("step_length", numberString).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            } else {
                                new writeSettings("step_length", numberString).execute();
                            }
                            Core.stepLength = (float) (number * 2.54 / 222);
                            tutorialAbgeschlossen = true;
                        } else {
                            Toast.makeText(OsmMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                        }

                    } catch (NumberFormatException e) {
                        if (Config.debugMode) {
                            e.printStackTrace();
                        }
                        Toast.makeText(OsmMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(OsmMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                }

                if (tutorialAbgeschlossen) {
                    // hide Tutorial
                    tutorialOverlay = (View) findViewById(R.id.tutorialOverlayOsm);
                    tutorialOverlay.setVisibility(View.INVISIBLE);
                    // make Map clickable again
                    map.setClickable(true);
                    // LongPressDialog
                    showLongPressDialog();
                }
            }
        });

        EditText heightField = (EditText) findViewById(R.id.tutorialEditTextOsm);
        heightField.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
                    try {
                        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        EditText heightField = (EditText) findViewById(R.id.tutorialEditText);
                        heightField.setFocusableInTouchMode(false); //Workaround: Coursor out of textfield
                        heightField.setFocusable(false);
                        heightField.setFocusableInTouchMode(true);
                        heightField.setFocusable(true);
                    } catch (Exception e) {
                        if (Config.debugMode)
                            e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    private void showLongPressDialog() {
        try {
            myItemizedOverlay[0].getItem(0).getDrawable().setVisible(false, true);
        } catch (Exception e) {
            if (Config.debugMode)
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
                    if (Config.debugMode)
                        e.printStackTrace();
                }
            }
        });
        // Remember that longPress was shown
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new writeSettings("longPressWasShown", true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new writeSettings("longPressWasShown", true).execute();
        }
    }

    private void appRateDialog() {
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", 0);
        if (prefs.getBoolean("dontshowagain", false)) {
            return;
        }

        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        int launch_count = prefs.getInt("launch_count", 0) + 1;
        editor.putInt("launch_count", launch_count);
        // Log.d("Location-Status", "Launch-Count: " + launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }

        // Wait at least n days before opening
        if (launch_count >= Config.LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch + (Config.DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog();
            }
        }
        editor.commit();
    }

    private void showRateDialog() {
        final View appRateDialog = findViewById(R.id.appRateDialogOsm);
        appRateDialog.setVisibility(View.VISIBLE);

        Button rateButton1 = (Button) findViewById(R.id.rateButtonOsm);
        rateButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                appRateDialog.setVisibility(View.INVISIBLE);

                SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", 0);
                int notRated = prefs.getInt("not_rated", 0) + 1;

                new writeSettings("not_rated", notRated).execute();

                if (notRated == 1) {
                    new writeSettings("launch_count", -6).execute();
                } else if (notRated == 2) {
                    new writeSettings("launch_count", -8).execute();
                } else if (notRated == 3) {
                    new writeSettings("launch_count", -10).execute();
                } else if (notRated == 4) {
                    new writeSettings("dontshowagain", true).execute();
                }
            }
        });

        Button rateButton3 = (Button) findViewById(R.id.rateButton2Osm);
        rateButton3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new writeSettings("not_rated", 999).execute();
                appRateDialog.setVisibility(View.INVISIBLE);
                new writeSettings("dontshowagain", true).execute();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
            }
        });
    }

    public void clickOnStars(final View view) {
        new writeSettings("not_rated", 999).execute();
        final View appRateDialog = findViewById(R.id.appRateDialogOsm);
        appRateDialog.setVisibility(View.INVISIBLE);
        new writeSettings("dontshowagain", true).execute();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
    }

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {

        mainMenu = menu;

        subMenu1 = menu.addSubMenu(0, 3, 3, "").setIcon(R.drawable.ic_menu_moreoverflow_normal_holo_dark);
        subMenu1.add(0, 4, 4, getApplicationContext().getResources().getString(R.string.tx_64));
        subMenu1.add(0, 5, 5, getApplicationContext().getResources().getString(R.string.tx_90));
        subMenu1.add(0, 6, 6, getApplicationContext().getResources().getString(R.string.tx_15));
        subMenu1.add(0, 7, 7, getApplicationContext().getResources().getString(R.string.tx_50));
        subMenu1.add(0, 8, 8, getApplicationContext().getResources().getString(R.string.tx_65));

        MenuItem subMenu1Item = subMenu1.getItem();
        subMenu1Item.setIcon(R.drawable.ic_menu_moreoverflow_normal_holo_dark);
        subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, 1, 1, getApplicationContext().getResources().getString(R.string.tx_03)).setIcon(R.drawable.ic_menu_search_holo_dark)
                .setActionView(searchView).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            try {
                mainMenu.performIdentifierAction(subMenu1.getItem().getItemId(), 0);

                // if off, longPressMenu will be made invisible
                list = (ListView) findViewById(R.id.listeOsm);
                list.setVisibility(View.INVISIBLE);
                listIsVisible = false;
            } catch (Exception e) {
                if (Config.debugMode)
                    e.printStackTrace();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {

        // if off, longPressMenu will be made invisible
        try {
            list = (ListView) findViewById(R.id.listeOsm);
            list.setVisibility(View.INVISIBLE);
            listIsVisible = false;
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }

        switch (item.getItemId()) {
            case 4:
                // smartgeo / background service
                knownReasonForBreak = true;
                Intent myIntent = new Intent(this, BackgroundService.class);
                startActivity(myIntent);
                return true;
            case 5:
                View offlineMapsDialog = findViewById(R.id.offlineMapsOsm);
                offlineMapsDialog.setVisibility(View.VISIBLE);
                Button offlineMapsButtonOk = (Button) findViewById(R.id.offlineMapsButtonOsmOk);
                offlineMapsButtonOk.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        View offlineMapsDialog = findViewById(R.id.offlineMapsOsm);
                        offlineMapsDialog.setVisibility(View.GONE);
                    }
                });

                Button offlineMapsButtonWebsite = (Button) findViewById(R.id.offlineMapsButtonOsmWebsite);
                offlineMapsButtonWebsite.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://smartnavi-app.com/offline"));
                        startActivity(browserIntent);
                        finish();
                    }
                });

                return true;
            case 6:
                // Go to Settings
                knownReasonForBreak = true;
                startActivity(new Intent(this, Settings.class));
                return true;

            case 7:
                // open Tutorial
                tutorialStuff();
                return true;
            case 8:
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
        listIsVisible = false;
        longPressedGeoPoint = p;
        list = (ListView) findViewById(R.id.listeOsm);
        list.setVisibility(View.VISIBLE);
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
        // Create the search view
        searchView = new SearchView(getSupportActionBar().getThemedContext());
        searchView.setQueryHint(getApplicationContext().getResources().getString(R.string.tx_02));
        // get static themed context for autocomplete update
        sbContext = getSupportActionBar().getThemedContext();

        // add adapter to search view
        searchView.setSuggestionsAdapter(mSuggestionsAdapter);

        // onClick closes the longPressMenu if it is shown
        searchView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // if off, longPressMenu will be made invisible
                try {
                    list = (ListView) findViewById(R.id.listeOsm);
                    list.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    if (Config.debugMode)
                        e.printStackTrace();
                }
            }
        });

        // autocomplete suggestions
        searchView.setOnQueryTextListener(new OnQueryTextListener() {

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
                    Toast.makeText(OsmMapActivity.this, "You can not find Chuck Norris. Chuck Norris finds YOU!", Toast.LENGTH_LONG).show();
                else if (query.equalsIgnoreCase("cake") || query.equalsIgnoreCase("the cake") || query.equalsIgnoreCase("portal"))
                    Toast.makeText(OsmMapActivity.this, "The cake is a lie!", Toast.LENGTH_LONG).show();
                else if (query.equalsIgnoreCase("tomlernt")) {
                    // start debug mode
                    Toast.makeText(OsmMapActivity.this, "Debug-Mode ON", Toast.LENGTH_SHORT).show();
                    Config.debugMode = true;
                } else if (query.equalsIgnoreCase("rateme")) {
                    // show app rate dialog
                    showRateDialog();
                } else if (query.equalsIgnoreCase("smartnavihelp")) {
                    // User ID anzeigen
                    SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                    uid = settings.getString("uid", "0");
                    View viewLine = findViewById(R.id.view156);
                    viewLine.setVisibility(View.VISIBLE);
                    TextView mapText = (TextView) findViewById(R.id.mapText);
                    mapText.setVisibility(View.VISIBLE);
                    mapText.setText("Random User ID: " + uid);
                }
                // search coordinates for autocomplete result
                else if (isOnline()) {
                    if (Config.PLACES_API_FALLBACK < 2) {
                        showRouteInfo(true);
                        new PlacesTextSeachAsync().execute(query);
                    }
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {

                // min 3 chars before autocomplete
                if (query.length() >= 3 && Config.PLACES_API_FALLBACK < 2) {
                    // prevent hammering
                    if (!suggestionsInProgress) {
                        // get suggestions
                        new PlacesAutoComplete().execute(query);
                        suggestionsInProgress = true;
                        // Log.e("OnQueryTextChange", "request was sent");
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

        searchView.setOnSuggestionListener(new OnSuggestionListener() {

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
                    mSuggestionsAdapter = new SuggestionsAdapter(getSupportActionBar().getThemedContext(), cursor);
                    searchView.setSuggestionsAdapter(OsmMapActivity.mSuggestionsAdapter);
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

        private String query;

        @Override
        protected JSONObject doInBackground(String... input) {
            query = input[0];
            PlacesTextSearch textSearch = new PlacesTextSearch(getBaseContext());
            JSONObject destination = textSearch.getDestinationCoordinates(input[0]);
            return destination;
        }

        @Override
        protected void onPostExecute(JSONObject destination) {

            super.onPostExecute(destination);

            // no results from api
            if (destination == null) {
                Toast.makeText(OsmMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_77), Toast.LENGTH_LONG).show();
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
                    ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
                    waypoints.add(new GeoPoint(Core.startLat, Core.startLon));
                    GeoPoint endPoint = new GeoPoint(destLat, destLon);
                    waypoints.add(endPoint);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        new RoadManagerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, waypoints);
                    } else {
                        new RoadManagerTask().execute(waypoints);
                    }

                } catch (JSONException e) {
                    if (Config.debugMode)
                        e.printStackTrace();
                }
            }
        }
    }

    private class RoadManagerTask extends AsyncTask<ArrayList<GeoPoint>, Void, Polyline> {
        private Road road;

        @Override
        protected Polyline doInBackground(ArrayList<GeoPoint>... waypoints) {
            // old without pedestrian support
            // RoadManager roadManager = new OSRMRoadManager();
            RoadManager roadManager = new MapQuestRoadManager(Config.MAPQUEST_API_KEY);
            roadManager.addRequestOption("routeType=pedestrian");
            // retreive the road between those points:
            road = roadManager.getRoad(waypoints[0]);
            //build a Polyline with the route shape
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road, OsmMapActivity.this);
            return roadOverlay;
        }

        @Override
        protected void onPostExecute(Polyline roadOverlay) {
            FolderOverlay myMarkersFolder = new FolderOverlay(OsmMapActivity.this);
            AbstractList<Overlay> list = myMarkersFolder.getItems();
            list.add(roadOverlay);

            showRouteInfo(false);

            //Show Route Steps on the map
            Drawable nodeIcon = getResources().getDrawable(R.drawable.finish2);
            for (int i = 0; i < road.mNodes.size(); i++) {
                RoadNode node = road.mNodes.get(i);
                Marker nodeMarker = new Marker(map);
                nodeMarker.setPosition(node.mLocation);
                nodeMarker.setIcon(nodeIcon);
                nodeMarker.setTitle("Step " + i);
                nodeMarker.setSnippet(node.mInstructions);
                nodeMarker.setSubDescription(Road.getLengthDurationText(node.mLength, node.mDuration));
                Drawable icon = getResources().getDrawable(R.drawable.ic_continue);
                if (node.mManeuverType == 1) {
                    icon = getResources().getDrawable(R.drawable.ic_continue);
                } else if (node.mManeuverType == 3) {
                    icon = getResources().getDrawable(R.drawable.ic_slight_left);
                } else if (node.mManeuverType == 4 || node.mManeuverType == 5) {
                    icon = getResources().getDrawable(R.drawable.ic_turn_left);
                } else if (node.mManeuverType == 6) {
                    icon = getResources().getDrawable(R.drawable.ic_slight_right);
                } else if (node.mManeuverType == 7 || node.mManeuverType == 8) {
                    icon = getResources().getDrawable(R.drawable.ic_turn_right);
                } else if (node.mManeuverType == 24 || node.mManeuverType == 25 || node.mManeuverType == 26) {
                    icon = getResources().getDrawable(R.drawable.ic_arrived);
                }
                nodeMarker.setImage(icon);
                nodeMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker, MapView mapView) {
                        marker.showInfoWindow();
                        return true;
                    }
                });

                //put into FolderOverlay list
                list.add(nodeMarker);
            }

            OverlayManager om = map.getOverlayManager();

            if (om.size() > 2) {
                om.remove(1);
                om.remove(1);
            } else {
                om.remove(1);
            }

            map.getOverlays().add(myMarkersFolder);
            setNewPositionMarker();
            map.invalidate();
        }
    }

}
