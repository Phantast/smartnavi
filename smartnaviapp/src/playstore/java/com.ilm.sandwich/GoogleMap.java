package com.ilm.sandwich;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.ilm.sandwich.fragments.RatingFragment;
import com.ilm.sandwich.fragments.TutorialFragment;
import com.ilm.sandwich.sensors.Core;
import com.ilm.sandwich.tools.AnalyticsApplication;
import com.ilm.sandwich.tools.Config;
import com.ilm.sandwich.tools.HttpRequests;
import com.ilm.sandwich.tools.Locationer;
import com.ilm.sandwich.tools.PlacesAutoComplete;
import com.ilm.sandwich.tools.PlacesTextSearch;
import com.ilm.sandwich.tools.Statistics;
import com.ilm.sandwich.tools.SuggestionsAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MapActivitiy for Google Maps
 *
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class GoogleMap extends AppCompatActivity implements Locationer.onLocationUpdateListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, TutorialFragment.onTutorialFinishedListener, Core.onStepUpdateListener, RatingFragment.onRatingFinishedListener {

    public static double destLat;
    public static double destLon;
    public static boolean followMe;
    public static boolean vibration;
    public static boolean satelliteView;
    public static Bitmap drawableDest;
    public static Core mCore;
    public static boolean backgroundServiceShallBeOnAgain = false;
    public static String uid;
    public static boolean userHasSetByTouch = false;
    public static SearchView searchView;
    public static Handler listHandler;
    public static SuggestionsAdapter mSuggestionsAdapter;
    public static boolean suggestionsInProgress = false;
    public static MatrixCursor cursor = new MatrixCursor(Config.COLUMNS);
    public static Handler changeSuggestionAdapter;
    static boolean uTaskIsOn;
    static LatLng longpressLocation;
    static ListView list;
    private static int stepCounterOld = 1;
    private static int now = 0;
    private static int geoCodeTry = 0;
    private static LatLng startLatLng;
    private static LatLng destLatLng;
    private static float oldZoomLevel;
    private static Marker currentPosition;
    private static Marker current_position_anim_ohne;
    private static Marker[] actualMarker = new Marker[1];
    private static boolean brightPoint;
    private static Marker destMarker;
    private static boolean routeHasBeenDrawn = false;
    private static int routeParts = 0;
    private static Marker longPressMarker;
    private static boolean userSwitchedGps = false;
    public double[] gp2Latk = new double[31];
    public double[] gp2Lonk = new double[31];
    public boolean waitedAtStart = false;
    public int counterRouteComplexity = 0;
    public boolean speechOutput;
    public Context sbContext;
    Menu mainMenu;
    String language;
    TextToSpeech mTts;
    ProgressBar mProgressBar;
    int phases;
    int segmentCounter;
    TutorialFragment tutorialFragment;
    RatingFragment ratingFragment;
    private Tracker mTracker;
    private com.google.android.gms.maps.GoogleMap map;
    private String[] html_instructions = new String[31];
    private String[] polylineArray = new String[31];
    private Locationer mLocationer;
    private boolean knownReasonForBreak = false;
    private boolean finishedTalking = false;
    private boolean listVisible = false;
    private Polyline[] completeRoute = new Polyline[31];
    private Toolbar toolbar;
    private FloatingActionButton fab;

    public static double computeDistanz(double lat, double lon) {
        // Entfernung bzw. Distanz zur eigenen aktuellen Position
        double mittellat2 = (Core.startLat + lat) / 2 * 0.01745329252;
        double distanceLongitude = 111.3 * Math.cos(mittellat2);
        double dlat2 = 111.3 * (Core.startLat - lat);
        double dlon2 = distanceLongitude * (Core.startLon - lon);
        return Math.sqrt(dlat2 * dlat2 + dlon2 * dlon2); //in km air distance
    }

    public void setPosition(boolean follow) {
        startLatLng = new LatLng(Core.startLat, Core.startLon);
        try {
            actualMarker[0].setPosition(startLatLng);
        } catch (Exception e) {
            if (BuildConfig.debug)
                e.printStackTrace();
        }
        if (follow) {
            if (Core.lastErrorGPS < 100) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 18.0F)));
            } else if (Core.lastErrorGPS < 231) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 17.0F)));
            } else if (Core.lastErrorGPS < 401) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 16.0F)));
            } else if (Core.lastErrorGPS < 801) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 15.0F)));
            } else if (Core.lastErrorGPS < 1501) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 14.0F)));
            } else {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, map.getCameraPosition().zoom)));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config.usingGoogleMaps = true;
        setContentView(R.layout.activity_googlemap);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toolbar = (Toolbar) findViewById(R.id.toolbar_googlemap); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call

        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // longPressMenu will become invisible
                    try {
                        list = (ListView) findViewById(R.id.liste);
                        if (list != null && list.getVisibility() == View.VISIBLE)
                            list.setVisibility(View.INVISIBLE);
                    } catch (Exception e) {
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
            proceedOnCreate();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("Granted_Google_Location")
                    .build());
            proceedOnCreate();
        } else {
            Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_100), Toast.LENGTH_LONG).show();
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("Denied_Google_Location")
                    .build());
            finish();
        }
    }

    private void proceedOnCreate() {
        mLocationer = new Locationer(GoogleMap.this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googlemap_fragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(com.google.android.gms.maps.GoogleMap googleMap) {

        map = googleMap;
        try {
            map.setMyLocationEnabled(false);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        map.setIndoorEnabled(true);
        map.getUiSettings().setCompassEnabled(false);
        map.setBuildingsEnabled(true);
        map.setOnMarkerClickListener(new com.google.android.gms.maps.GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //do not react to clicks on Markers, as Google would show a small menu in the bottom right corner
                //showing options to open GoogleMaps
                return true;
            }
        });

        map.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng arg0) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Longpress_GoogleMap")
                        .build());
                longpressLocation = arg0;
                if (longPressMarker != null) {
                    if (longPressMarker.isVisible()) {
                        longPressMarker.remove();
                    }
                }
                longPressMarker = map.addMarker(new MarkerOptions().position(longpressLocation).icon(BitmapDescriptorFactory.fromBitmap(drawableDest))
                        .anchor(0.5F, 1.0F));
                listHandler.sendEmptyMessage(2);
            }
        });

        map.setOnMapClickListener(new OnMapClickListener() {
            @Override
            public void onMapClick(LatLng arg0) {
                if (listVisible) {
                    longpressLocation = arg0;
                    longPressMarker.remove();
                    list.setVisibility(View.INVISIBLE);
                    listVisible = false;
                }
            }
        });

        drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);

        map.setIndoorEnabled(true);


        startHandler();

        //Check if magnetic sensor is existing. If not: Warn user!
        try {
            SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getName();
        } catch (Exception e) {
            View viewLine = findViewById(R.id.view156);
            if (viewLine != null) {
                viewLine.setVisibility(View.VISIBLE);
            }
            TextView mapText = (TextView) findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setVisibility(View.VISIBLE);
            }
            if (mapText != null) {
                mapText.setText(getResources().getString(R.string.tx_43));
            }
        }

        // if offline, Toast Message will appear automatically
        isOnline();

        TextView mapText = (TextView) findViewById(R.id.mapText);
        if (mapText != null) {
            mapText.setVisibility(View.INVISIBLE);
            mapText.setSingleLine(false);
        }
        View viewLine = findViewById(R.id.view156);
        if (viewLine != null) {
            viewLine.setVisibility(View.INVISIBLE);
        }

        language = Locale.getDefault().getLanguage();

        new changeSettings("follow", true).execute();
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        uid = settings.getString("uid", "0");
        if (uid.equalsIgnoreCase("0")) {
            String neuUID = "" + (1 + (int) (Math.random() * ((10000000 - 1) + 1)));
            new changeSettings("uid", neuUID).execute();
            uid = settings.getString("uid", "0");
        }

        satelliteView = settings.getBoolean("view", false);
        if (satelliteView) {
            map.setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID);
        } else {
            map.setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL);
        }

        createAllMarkersInvisible();

        String stepLengthString = settings.getString("step_length", null);
        if (stepLengthString != null) {
            try {
                stepLengthString = stepLengthString.replace(",", ".");
                Float savedBodyHeight = (Float.parseFloat(stepLengthString));
                if (savedBodyHeight < 241 && savedBodyHeight > 119) {
                    Core.stepLength = savedBodyHeight / 222;
                } else if (savedBodyHeight < 95 && savedBodyHeight > 45) {
                    Core.stepLength = (float) (savedBodyHeight * 2.54 / 222);
                }
            } catch (NumberFormatException e) {
                if (BuildConfig.debug) {
                    e.printStackTrace();
                }
            }
        } else {
            map.getUiSettings().setAllGesturesEnabled(false);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            tutorialFragment = new TutorialFragment();
            fragmentTransaction.add(R.id.googlemap_actvity_layout, tutorialFragment);
            fragmentTransaction.commitAllowingStateLoss();
        }

        mTts = new TextToSpeech(GoogleMap.this, null);
        mTts.setLanguage(Locale.getDefault());

        // onLongPress Auswahl-Liste
        list = (ListView) findViewById(R.id.liste);
        if (list != null) {
            list.setVisibility(View.INVISIBLE);
        }
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    setHome();
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("SetPosition_after_Longpress")
                            .build());
                    list.setVisibility(View.INVISIBLE);
                    listVisible = false;
                    longPressMarker.remove();
                    positionUpdate();
                } else {
                    fingerDestination(longpressLocation);
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("SetDestination_after_Longpress")
                            .build());
                    list.setVisibility(View.INVISIBLE);
                    listVisible = false;
                    longPressMarker.remove();
                }
            }
        });
        // Rate App show for debugging
        //showRateDialog();
        // Rate App live
        appRateDialog();
    }

    public void onMapTouch() {
        if (followMe)
            setFollowOff();
    }

    @SuppressLint("HandlerLeak")
    public void startHandler() {
        listHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    followMe = true;
                } else if (msg.what == 2) {
                    list.setVisibility(View.VISIBLE);
                    //Set this variable after some time has passed
                    // so that the LongclickList will not be removed by random
                    // minimal Touch gestures. So the list has a guaranteed short life time
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            listVisible = true;
                        }
                    }, 1200);
                } else if (msg.what == 3) {
                    finish(); // used by Settings to change to OsmMap
                } else if (msg.what == 6) {
                    // initialize Autocorrect oder restart new
                    // after activity_settings changed if necessary
                    if (mCore != null)
                        mCore.enableAutocorrect();
                } else if (msg.what == 7) {
                    if (mCore != null)
                        mCore.disableAutocorrect();
                    mLocationer.stopAutocorrect();
                } else if (msg.what == 9) {
                    //Reactivate sensors regularly because app is in background mode
                    //and other apps might cause sensors to stop
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            if (mCore != null)
                                mCore.reactivateSensors();
                            listHandler.sendEmptyMessageDelayed(9, 5000);
                        }
                    }, 5000);
                } else if (msg.what == 10) {
                    //BackgroundService is created, so dont stop sensors
                    if (mCore != null)
                        mCore.reactivateSensors();
                } else if (msg.what == 11) {
                    setFollowOn();
                    startLatLng = new LatLng(Core.startLat, Core.startLon);
                    // Turn Camera towards North
                    CameraPosition currentPlace = new CameraPosition.Builder().target(startLatLng).bearing(0.0F).tilt(0.0F).zoom(19).build();
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
                    // Restart FollowMe
                    listHandler.sendEmptyMessageDelayed(1, 1500);
                } else if (msg.what == 15) {
                    onMapTouch();
                }
                super.handleMessage(msg);
            }
        };
    }


    @Override
    public void onLocationUpdate(int event) {
        switch (event) {
            case 0:
                // First Position from the Locationer
                startLatLng = new LatLng(Core.startLat, Core.startLon);
                setFirstPosition();
                foreignIntent();
                // start Autocorrect if user wants it
                listHandler.sendEmptyMessage(6);
                mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                break;
            case 5:
                showGPSDialog();
                break;
            case 8:
                Core.setLocation(Locationer.startLat, Locationer.startLon);
                mLocationer.stopAutocorrect();
                if (backgroundServiceShallBeOnAgain) {
                    Config.backgroundServiceActive = true;
                    BackgroundService.reactivateFakeProvider();
                }
                break;
            case 12:
                // message from Locationer
                mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
                if (mProgressBar != null) {
                    mProgressBar.setVisibility(View.GONE);
                }
                break;
            case 14:
                // next position from Locationer
                setPosition(true);
        }
    }

    public void createAllMarkersInvisible() {
        LatLng northPoleHideout = new LatLng(90.0D, 0.0D);

        destMarker = map.addMarker(new MarkerOptions().position(northPoleHideout).icon(BitmapDescriptorFactory.fromBitmap(drawableDest)));
        destMarker.setVisible(false);

        currentPosition = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_ohne)).anchor(0.5f, 0.5f));
        currentPosition.setVisible(false);

        current_position_anim_ohne = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_ohne)).anchor(0.5f, 0.5f));
        current_position_anim_ohne.setVisible(false);
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

    public void foreignIntent() {
        try {
            String[] requestArray1;
            String[] requestArray2;

            String requestString = this.getIntent().getDataString();
            if (requestString != null) {
                if (requestString.contains("google.navigation")) {
                    requestArray1 = requestString.split("&q=");
                    requestArray1 = requestArray1[0].split("%2C");
                    destLon = Float.parseFloat(requestArray1[1]);
                    requestArray2 = requestArray1[0].split("ll=");
                    destLat = Float.parseFloat(requestArray2[1]);
                } else if (requestString.contains("http://maps.google")) {
                    String[] requestArray3;
                    String[] requestArray4;

                    if (requestString.contains("?saddr=")) {
                        // Variante 1:
                        // "http://maps.google.com/maps?saddr=50.685053,10.910772&daddr=50.689308,10.932552";
                        requestArray1 = requestString.split("saddr=");
                        requestArray2 = requestArray1[1].split("&daddr=");

                        requestArray3 = requestArray2[0].split(",");
                        Core.startLat = Float.parseFloat(requestArray3[0]);
                        Core.startLon = Float.parseFloat(requestArray3[1]);

                        requestArray4 = requestArray2[1].split(",");
                        destLat = Float.parseFloat(requestArray4[0]);
                        destLon = Float.parseFloat(requestArray4[1]);
                        // Deactivate Locationer, because startPosition is within Request
                        try {
                            mLocationer.deactivateLocationer();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (requestString.contains("?daddr=")) {
                        // Variante 2 :
                        // "http://maps.google.com/maps?daddr=50.685053,10.910772&saddr=50.689308,10.932552";
                        requestArray1 = requestString.split("daddr=");
                        if (requestString.contains("saddr=")) {
                            requestArray2 = requestArray1[1].split("&saddr=");

                            requestArray3 = requestArray2[0].split(",");
                            destLat = Float.parseFloat(requestArray3[0]);
                            destLon = Float.parseFloat(requestArray3[1]);

                            requestArray4 = requestArray2[1].split(",");
                            Core.startLat = Float.parseFloat(requestArray4[0]);
                            Core.startLon = Float.parseFloat(requestArray4[1]);
                            // Locationer ausmachen, weil StartPosition ja mitgegeben wurde
                            try {
                                mLocationer.deactivateLocationer();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            //In case that ?daddr has been sent, but now startLocation
                            // Variante 3 :
                            // "http://maps.google.com/maps?daddr=50.685053,10.910772"
                            requestArray3 = requestArray1[1].split(",");
                            destLat = Float.parseFloat(requestArray3[0]);
                            destLon = Float.parseFloat(requestArray3[1]);
                        }
                    }
                } else {
                    // String requestString = "geo:50.6815558821102,10.932855606079102";
                    requestArray1 = requestString.split(",");
                    requestArray2 = requestArray1[0].split(":");
                    destLat = Float.parseFloat(requestArray2[1]);
                    destLon = Float.parseFloat(requestArray1[1]);
                }
                destLatLng = new LatLng(destLat, destLon);
                listHandler.removeCallbacksAndMessages(null);
                map.stopAnimation();
                setPosition(false);
                drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
                setDestPosition(destLatLng);
                showRouteInfo();
                new routeTask().execute("zielortSollRoutenTaskSelbstRausfinden");
            }
        } catch (Exception e) {
            if (BuildConfig.debug)
                e.printStackTrace();
        }
    }

    public void setFollowOn() {
        followMe = true;
        stepCounterOld = stepCounterOld - 1;
        if (map != null) {
            LatLng newPos = new LatLng(Core.startLat, Core.startLon);
            float zoomLevel;
            if (map.getCameraPosition().zoom < 15) {
                zoomLevel = 17.0F;
            } else {
                zoomLevel = map.getCameraPosition().zoom;
            }
            CameraPosition followPosition = new CameraPosition.Builder().target(newPos).bearing(0.0F).tilt(0.0F).zoom(zoomLevel).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(followPosition));
        }
        fab.hide();
    }

    public void setFollowOff() {
        followMe = false;
        fab.show();
    }

    public void setHome() {
        Core.startLat = longpressLocation.latitude;
        Core.startLon = longpressLocation.longitude;
        Core.stepCounter++;

        actualMarker[0].setPosition(longpressLocation);
        setFollowOn();
        // Wichtig für locationer, damit der das berücksichtigen kann
        userHasSetByTouch = true;
    }

    private void positionUpdate() {
        float zoomLevel = map.getCameraPosition().zoom;
        LatLng newPos = new LatLng(Core.startLat, Core.startLon);

        try {
            if (brightPoint || zoomLevel != oldZoomLevel) {
                oldZoomLevel = zoomLevel;
                brightPoint = false;
                actualMarker[0].setVisible(false);
                currentPosition.setPosition(newPos);
                currentPosition.setVisible(true);
                actualMarker[0] = currentPosition;
            }

            if (Core.stepCounter != stepCounterOld) {
                stepCounterOld = Core.stepCounter;

                actualMarker[0].setVisible(false);

                if (now % 2 != 0) {
                    currentPosition.setPosition(newPos);
                    currentPosition.setVisible(true);
                    actualMarker[0] = currentPosition;
                    brightPoint = false;
                } else {
                    current_position_anim_ohne.setPosition(newPos);
                    current_position_anim_ohne.setVisible(true);
                    actualMarker[0] = current_position_anim_ohne;
                    brightPoint = true;
                }
            }
            actualMarker[0].setRotation(actualMarker[0].getRotation() * (-1));
            float rotation = (float) Core.azimuth;
            actualMarker[0].setRotation(rotation);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (followMe) {
            if (now % 2 != 0) {
                // Camera points north
                CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing(0.0F).tilt(0.0F).zoom(zoomLevel).build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
            }
        }
        now++;
        if (segmentCounter < (phases - 1) && waitedAtStart) {
            segmentControl();
        }
    }

    public void setFirstPosition() {
        startLatLng = new LatLng(Core.startLat, Core.startLon);
        currentPosition.setPosition(startLatLng);
        currentPosition.setVisible(true);
        actualMarker[0] = currentPosition;

        if (Core.lastErrorGPS < 100) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 18.0F)));
        } else if (Core.lastErrorGPS < 231) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 17.0F)));
        } else if (Core.lastErrorGPS < 401) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 16.0F)));
        } else if (Core.lastErrorGPS < 801) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 15.0F)));
        } else if (Core.lastErrorGPS < 1501) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 14.0F)));
        } else if (Core.lastErrorGPS == 9999999) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 4.0F)));
            View viewLine = findViewById(R.id.view156);
            if (viewLine != null) {
                viewLine.setVisibility(View.VISIBLE);
            }
            TextView mapText = (TextView) findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setVisibility(View.VISIBLE);
                mapText.setText(getResources().getString(R.string.tx_06));
            }
        } else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 13.0F)));
        }
        followMe = true;
        mCore = new Core(GoogleMap.this);
        mCore.startSensors();
    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
            poly.add(p);
        }
        return poly;
    }

    public void segmentControl() {
        double abstand = computeDistanz(gp2Latk[segmentCounter], gp2Lonk[segmentCounter]);
        if (abstand < 0.03 && finishedTalking) {
            finishedTalking = false;
            completeRoute[segmentCounter].remove();
            segmentCounter++;
            Spanned marked_up = Html.fromHtml(html_instructions[segmentCounter]);
            String textString = marked_up.toString();
            TextView mapText = (TextView) findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setText(textString);
            }
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            String textString2;
            if (language.equalsIgnoreCase("de")) {
                textString2 = "Als nächstes, " + textString;
            } else if (language.equalsIgnoreCase("es")) {
                textString2 = "A continuacion, " + textString;
            } else if (language.equalsIgnoreCase("fr")) {
                textString2 = "Ensuite, " + textString;
            } else if (language.equalsIgnoreCase("pl")) {
                textString2 = "Nastepnie, " + textString;
            } else if (language.equalsIgnoreCase("it")) {
                textString2 = "Successivamente,  " + textString;
            } else if (language.equalsIgnoreCase("en")) {
                textString2 = "Next, " + textString;
            } else {
                textString2 = textString;
            }
            if (speechOutput) {
                textString2 = textString2.replace("-", " ");
                textString2 = textString2.replace("/", " ");
                mTts.speak(textString2, TextToSpeech.QUEUE_FLUSH, null);
            }
            if (vibration) {
                v.vibrate(300);
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    finishedTalking = true;
                }
            }, 10000);
        }
    }

    public void showRouteInfo() {
        TextView mapText = (TextView) findViewById(R.id.mapText);
        View viewLine = findViewById(R.id.view156);
        if (mapText != null && viewLine != null) {
            mapText.setText(getApplicationContext().getResources().getString(R.string.tx_04));
            mapText.setVisibility(View.VISIBLE);
            viewLine.setVisibility(View.VISIBLE);
        }
    }

    public void makeInfo(String endAddress, String firstDistance) {
        TextView mapText = (TextView) findViewById(R.id.mapText);
        View viewLine = findViewById(R.id.view156);
        if (firstDistance != null && mapText != null && viewLine != null) {
            mapText.setText(endAddress + "\n\n" + firstDistance);
            viewLine.setVisibility(View.VISIBLE);
            mapText.setVisibility(View.VISIBLE);
            if (speechOutput) {
                firstDistance = firstDistance.replace("-", " ");
                firstDistance = firstDistance.replace("\n", " ");
                firstDistance = firstDistance.replace("/", " ");
                mTts.speak(firstDistance, TextToSpeech.QUEUE_FLUSH, null);
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    finishedTalking = true;
                    waitedAtStart = true;
                }
            }, 8500);
            uTaskIsOn = true;
        } else {
            Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_30), Toast.LENGTH_LONG).show();
            if (viewLine != null && mapText != null) {
                viewLine.setVisibility(View.INVISIBLE);
                mapText.setVisibility(View.INVISIBLE);
            }
        }
    }


    @Override
    protected void onResume() {
        Config.usingGoogleMaps = true;
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int status = api.isGooglePlayServicesAvailable(this);

        mTracker.setScreenName("GoogleMap");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (status != ConnectionResult.SUCCESS) {
            new changeSettings("MapSource", "MapQuestOSM").execute();
        } else if (!userSwitchedGps) {
            if (mCore != null) {
                mCore.reactivateSensors();
            }
            if (knownReasonForBreak) {
                //User is coming from Settings, Background Service or About
                knownReasonForBreak = false;
            } else {
                //User calls onResume, probably because screen was deactiaved for a short time. Get GPS Position to go on!
                if (mLocationer != null) {
                    mLocationer.startLocationUpdates();
                }
            }
            if (Core.startLat != 0) {
                startLatLng = new LatLng(Core.startLat, Core.startLon);
            }
            if (BackgroundService.sGeoLat != 0) {
                startLatLng = new LatLng(BackgroundService.sGeoLat, BackgroundService.sGeoLon);
            }
            setFollowOn();
            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
            // Export
            if (mCore != null) {
                boolean export = settings.getBoolean("export", false);
                mCore.writeLog(export);
            }
            // MapView change
            satelliteView = settings.getBoolean("view", false);
            if (satelliteView && map != null) {
                map.setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID);
            } else if (map != null) {
                map.setMapType(com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL);
            }
            // Speech Output
            speechOutput = settings.getBoolean("language", false);
            // Vibration
            vibration = settings.getBoolean("vibration", true);
            stepCounterOld = Core.stepCounter - 1;
        } else {
            if (mLocationer != null) {
                mLocationer.startLocationUpdates();
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (mLocationer != null) {
            mLocationer.deactivateLocationer();
        }
        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
        if (mCore != null)
            mCore.pauseSensors();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        routeHasBeenDrawn = false;
        try {
            listHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int status = api.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            if (map != null) {
                map.clear();
            }
            if (uTaskIsOn) {
                waitedAtStart = true;
                uTaskIsOn = false;
            }
            if (mCore != null) {
                mCore.shutdown(this);
            }
            if (mTts != null) {
                mTts.stop();
                mTts.shutdown();
            }
            try {
                mLocationer.stopAutocorrect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Statistics mStatistics = new Statistics();
        mStatistics.check(this);
        super.onDestroy();
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
            // if off, longPressMenu will be made invisible
            list = (ListView) findViewById(R.id.liste);
            if (list != null) {
                list.setVisibility(View.INVISIBLE);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // if off, longPressMenu will be made invisible
        list = (ListView) findViewById(R.id.liste);
        if (list != null) {
            list.setVisibility(View.INVISIBLE);
        }

        switch (item.getItemId()) {
            case R.id.menu_bgservice:
                // activity_backgroundservice
                knownReasonForBreak = true;
                Intent myIntent = new Intent(GoogleMap.this, BackgroundService.class);
                startActivity(myIntent);
                return true;
            case R.id.menu_settings:
                // Go to Settings
                knownReasonForBreak = true;
                startActivity(new Intent(this, Settings.class));
                return true;
            case R.id.menu_offlinemaps:
                /*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    String url = "http://smartnavi-app.com/offline/";
                    Intent chromeTabIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
                    Bundle extras = new Bundle();
                    extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null );
                    chromeTabIntent.putExtras(extras);
                    final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";
                    chromeTabIntent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.BLACK);
                    startActivity(chromeTabIntent);
                }else{}*/
                startActivity(new Intent(GoogleMap.this, Webview.class));
                return true;
            case R.id.menu_tutorial:
                // TutorialFragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                map.getUiSettings().setAllGesturesEnabled(false);
                tutorialFragment = new TutorialFragment();
                fragmentTransaction.add(R.id.googlemap_actvity_layout, tutorialFragment).commit();
                return true;
            case R.id.menu_info:
                // About Page
                knownReasonForBreak = true;
                startActivity(new Intent(this, Info.class));
                return true;
            case android.R.id.home:
                finish();
                return (true);
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void fingerDestination(LatLng g) {
        destLat = g.latitude;// getLatitudeE6() / 1E6;
        destLon = g.longitude;// getLongitudeE6() / 1E6;
        destLatLng = g;
        listHandler.removeCallbacksAndMessages(null);
        showRouteInfo();
        map.stopAnimation();
        setPosition(false);
        drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
        setDestPosition(destLatLng);
        new routeTask().execute("zielortSollRoutenTaskSelbstRausfinden"); //magic String :)
    }

    public void setDestPosition(LatLng z) {
        destMarker.setPosition(z);
        destMarker.setVisible(true);
    }

    public void routeStartAnimation(LatLng northeast, LatLng southwest) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("RouteCreated_on_GoogleMap")
                .build());
        LatLngBounds grenzen = new LatLngBounds(southwest, northeast);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(grenzen, 100));
        listHandler.sendEmptyMessageDelayed(11, 3000);
    }


  /*  private void showLongPressDialog() {
        try {
            actualMarker[0].setVisible(false);
        } catch (Exception e) {
            if (BuildConfig.debug)
                e.printStackTrace();
        }
        final View longPressDialog = findViewById(R.id.longpPressDialog);
        longPressDialog.setVisibility(View.VISIBLE);

        Button longPressButton = (Button) findViewById(R.id.longPressButton);
        longPressButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                longPressDialog.setVisibility(View.GONE);
                try {
                    actualMarker[0].setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    */


    private void appRateDialog() {
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", 0);
        if (prefs.getBoolean("dontshowagain", false)) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        // Increment launch counter
        int launch_count = prefs.getInt("launch_count", 0) + 1;
        editor.putInt("launch_count", launch_count);
        if (BuildConfig.debug)
            Log.i("RateDialog", "Launch-Count: " + launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }
        // Wait at least n days before opening
        if (launch_count >= Config.LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch + (Config.DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                // RatingFragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                try {
                    map.getUiSettings().setAllGesturesEnabled(false);
                } catch (Exception e) {
                }
                ratingFragment = new RatingFragment();
                fragmentTransaction.add(R.id.googlemap_actvity_layout, ratingFragment).commitAllowingStateLoss();
            }
        }
        editor.apply();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult arg0) {
        if (BuildConfig.debug)
            Log.i("Location-Status", "LocationClient: Connection FAILED" + arg0.getErrorCode());
        startActivity(new Intent(GoogleMap.this, OsmMap.class));
        finish();
    }

    // at Touch on ProgressBar
    public void abortGPS(final View view) {
        // Abort GPS was pressed (ProgressBar was pressed)
        try {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("GPS_canceled_GoogleMap")
                    .build());
            mLocationer.deactivateLocationer();
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            if (BuildConfig.debug)
                e.printStackTrace();
        }
        Toast.makeText(this, getResources().getString(R.string.tx_82), Toast.LENGTH_SHORT).show();
    }

    private void showGPSDialog() {
        final Dialog dialogGPS = new Dialog(GoogleMap.this);
        if (!dialogGPS.isShowing()) {
            dialogGPS.setContentView(R.layout.dialog3);
            dialogGPS.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
            dialogGPS.setCanceledOnTouchOutside(false);
            dialogGPS.show();
            mTracker.setScreenName("Dialog-GPS-enable");
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());

            Button cancel = (Button) dialogGPS.findViewById(R.id.dialogCancelgps);
            cancel.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("GPS_dialog_canceled_GoogleMap")
                            .build());
                    dialogGPS.dismiss();
                }
            });

            Button settingsGPS = (Button) dialogGPS.findViewById(R.id.dialogSettingsgps);
            settingsGPS.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        userSwitchedGps = true;
                    } catch (android.content.ActivityNotFoundException ae) {
                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                        userSwitchedGps = true;
                    }
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("GPS_dialog_jumpToSettings_GoogleMap")
                            .build());
                    dialogGPS.dismiss();
                }
            });
        }
    }


    @SuppressLint("HandlerLeak")
    private void prepareSearchView() {
        searchView.setSuggestionsAdapter(mSuggestionsAdapter);

        // onClick closes the longPressMenu if it is shown
        searchView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // if off, longPressMenu will be made invisible
                list = (ListView) findViewById(R.id.liste);
                if (list != null) {
                    list.setVisibility(View.INVISIBLE);
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
                    Toast.makeText(GoogleMap.this, "You can not find Chuck Norris. Chuck Norris finds YOU!", Toast.LENGTH_LONG).show();
                else if (query.equalsIgnoreCase("cake") || query.equalsIgnoreCase("the cake") || query.equalsIgnoreCase("portal")) {
                    Toast.makeText(GoogleMap.this, "The cake is a lie!", Toast.LENGTH_LONG).show();
                } else if (query.equalsIgnoreCase("gyrooff")) {
                    mCore.gyroExists = false;
                    mCore.reactivateSensors();
                }
                else if (query.equalsIgnoreCase("rateme")) {
                    // show RatingFragment
                    Log.i("Rating", "Showing Rating Fragment");
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    map.getUiSettings().setAllGesturesEnabled(false);
                    ratingFragment = new RatingFragment();
                    fragmentTransaction.add(R.id.googlemap_actvity_layout, ratingFragment).commit();
                } else if (query.equalsIgnoreCase("smartnavihelp")) {
                    // User ID anzeigen
                    SharedPreferences activity_settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                    uid = activity_settings.getString("uid", "0");
                    View viewLine = findViewById(R.id.view156);
                    TextView mapText = (TextView) findViewById(R.id.mapText);
                    if (viewLine != null && mapText != null) {
                        viewLine.setVisibility(View.VISIBLE);
                        mapText.setVisibility(View.VISIBLE);
                        mapText.setText("Random User ID: " + uid);
                    }
                }
                // search coordinates for autocomplete result
                else if (isOnline()) {
                    if (Config.PLACES_API_UNDER_LIMIT) {
                        new PlacesTextSeachAsync().execute(query);
                    } else {
                        new geocodeTask().execute(query);
                    }
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
                    searchView.setSuggestionsAdapter(GoogleMap.mSuggestionsAdapter);
                    // important to update suggestion list
                    searchView.getSuggestionsAdapter().notifyDataSetChanged();
                    suggestionsInProgress = false;
                }
                super.handleMessage(msg);
            }
        };
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onBackPressed() {
        if (routeHasBeenDrawn) {
            //Remove Route if drawn
            if (destMarker != null) {
                if (destMarker.isVisible()) {
                    destMarker.setVisible(false);
                }
            }
            for (int a = 0; a <= routeParts; a++) {
                completeRoute[a].remove();
            }
            routeHasBeenDrawn = false;
            setFollowOn();
            View viewLine = findViewById(R.id.view156);
            if (viewLine != null) {
                viewLine.setVisibility(View.INVISIBLE);
            }
            TextView mapText = (TextView) findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setVisibility(View.INVISIBLE);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onTutorialFinished() {
        map.getUiSettings().setAllGesturesEnabled(true);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(tutorialFragment).commit();
    }

    @Override
    public void onStepUpdate(int event) {
        if (event == 0) {
            //New step detected, change position
            positionUpdate();
        } else {
            //Threshold reached for Autocorrection
            mLocationer.starteAutocorrect();
        }
    }

    @Override
    public void onRatingFinished() {
        map.getUiSettings().setAllGesturesEnabled(true);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(ratingFragment).commit();
    }


    private class routeTask extends AsyncTask<String, Void, Void> {

        private String endAddress;
        private JSONArray stepsArray;
        private LatLng northeastLatLng;
        private LatLng southwestLatLng;
        private boolean getPathSuccess;
        private String firstDistance;

        protected void getPath(LatLng src, LatLng dest) {
            waitedAtStart = false;
            counterRouteComplexity = phases = segmentCounter = 0;

            HttpRequests httpJSON = new HttpRequests();
            httpJSON.setURL("http://maps.googleapis.com/maps/api/directions/json");
            httpJSON.setMethod("GET");
            httpJSON.addValue("origin", src.latitude + "," + src.longitude);
            httpJSON.addValue("destination", dest.latitude + "," + dest.longitude);
            httpJSON.addValue("sensor", "true");
            httpJSON.addValue("mode", "walking");
            httpJSON.addValue("language", language);
            String response = httpJSON.doRequest();
            try {
                getPathSuccess = true;
                JSONObject json = new JSONObject(response);
                JSONArray routesArray = json.getJSONArray("routes");
                JSONObject routesObject = routesArray.optJSONObject(0);
                JSONArray legsArray = routesObject.getJSONArray("legs");
                JSONObject legsObject = legsArray.optJSONObject(0);

                JSONObject durationObject = legsObject.optJSONObject("duration");
                String duration = durationObject.getString("text");
                JSONObject distanceObject = legsObject.optJSONObject("distance");
                String distance = distanceObject.getString("text");

                JSONObject bounds = routesObject.getJSONObject("bounds");
                JSONObject northeast = bounds.getJSONObject("northeast");
                northeastLatLng = new LatLng(northeast.optDouble("lat"), northeast.optDouble("lng"));

                JSONObject southwest = bounds.getJSONObject("southwest");
                southwestLatLng = new LatLng(southwest.optDouble("lat"), southwest.optDouble("lng"));

                // get Polyline for first Draw
                // JSONObject overview =
                // routesObject.getJSONObject("overview_polyline");
                // fullPolyline = overview.getString("points");

                // Destination set by longPress
                // endAddress shall be estimated through the JSON
                // if destination has been chosen by Search, then endAdress is already
                // set with correct Location
                if (endAddress.equalsIgnoreCase("zielortSollRoutenTaskSelbstRausfinden")) {
                    endAddress = legsObject.getString("end_address");
                }
                String[] zielOrtArray;
                zielOrtArray = endAddress.split(",", 3);
                try {
                    endAddress = zielOrtArray[0];
                    endAddress += "\n" + zielOrtArray[1];
                } catch (Exception e) {
                    // thats possible if Destination Name is only 1 line
                    if (BuildConfig.debug)
                        e.printStackTrace();
                }
                if (language.equalsIgnoreCase("de")) {
                    firstDistance = "Ziel ist " + distance + "\n" + "oder " + duration + " entfernt.";
                } else if (language.equalsIgnoreCase("es")) {
                    firstDistance = "Destino es de " + distance + "\n" + "o " + duration + " de distancia.";
                } else if (language.equalsIgnoreCase("fr")) {
                    firstDistance = "Destination est de " + distance + "\n" + "ou " + duration + ".";
                } else if (language.equalsIgnoreCase("pl")) {
                    firstDistance = "Docelowy jest " + distance + "\n" + "lub " + duration + ".";
                } else if (language.equalsIgnoreCase("it")) {
                    firstDistance = "Destination si trova a " + distance + "\n" + "o " + duration + ".";
                } else if (language.equalsIgnoreCase("en")) {
                    firstDistance = "Destination is " + distance + "\n" + "or " + duration + " away.";
                } else {
                    firstDistance = "Distance: " + distance + "\n or" + duration + ".";
                }
                stepsArray = legsObject.getJSONArray("steps");
                phases = stepsArray.length();

            } catch (Exception e) {
                if (BuildConfig.debug)
                    e.printStackTrace();
                getPathSuccess = false;
            }
        }

        @Override
        protected Void doInBackground(String... query) {
            endAddress = query[0];
            try {
                getPath(GoogleMap.startLatLng, GoogleMap.destLatLng);
            } catch (Exception e) {
                if (BuildConfig.debug)
                    e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void void1) {
            if (getPathSuccess) {
                drawPath();
                followMe = false;
                routeStartAnimation(northeastLatLng, southwestLatLng);
            }
            makeInfo(endAddress, firstDistance);
        }

        public void drawPath() {
            if (routeHasBeenDrawn) {
                for (int a = 0; a <= routeParts; a++) {
                    completeRoute[a].remove();
                    routeHasBeenDrawn = false;
                }
            }
            try {
                int color = Color.argb(200, 25, 181, 224);
                for (int i = 0; i < phases; i++) {
                    if (counterRouteComplexity < 30) {
                        counterRouteComplexity++;
                        JSONObject stepObject = stepsArray.optJSONObject(i);
                        html_instructions[i] = stepObject.getString("html_instructions");

                        JSONObject endObject = stepObject.optJSONObject("end_location");
                        int gp2Lon = (int) (Double.parseDouble(endObject.getString("lng")) * 1E6);
                        int gp2Lat = (int) (Double.parseDouble(endObject.getString("lat")) * 1E6);
                        gp2Lonk[i] = gp2Lon / 1E6;
                        gp2Latk[i] = gp2Lat / 1E6;

                        JSONObject polyObject = stepObject.optJSONObject("polyline");
                        String polyline = polyObject.getString("points");

                        //Collect Polylines in String Array to draw them seperately
                        polylineArray[i] = polyline;
                        completeRoute[i] = map.addPolyline(new PolylineOptions().addAll(decodePoly(polylineArray[i])).width(8).color(color));
                        routeHasBeenDrawn = true;
                        routeParts = i;
                    }
                }

            } catch (Exception e) {
                if (BuildConfig.debug)
                    e.printStackTrace();
            }
        }

    }

    private class PlacesTextSeachAsync extends AsyncTask<String, Void, JSONObject> {

        private String query;

        @Override
        protected JSONObject doInBackground(String... input) {
            query = input[0];
            PlacesTextSearch textSearch = new PlacesTextSearch(getBaseContext());
            return textSearch.getDestinationCoordinates(input[0]);
        }

        @Override
        protected void onPostExecute(JSONObject destination) {
            super.onPostExecute(destination);
            // no results from api
            if (destination == null) {
                Toast.makeText(GoogleMap.this, getApplicationContext().getResources().getString(R.string.tx_77), Toast.LENGTH_LONG).show();
            } else {
                // set destination for the routing tasks
                try {
                    destLat = (Double) destination.get("lat");
                    destLon = (Double) destination.get("lng");
                    destLatLng = new LatLng(destLat, destLon);
                    listHandler.removeCallbacksAndMessages(null);
                    map.stopAnimation();
                    setFollowOff();
                    setPosition(false);
                    setDestPosition(destLatLng);
                    showRouteInfo();
                    new routeTask().execute(query);
                } catch (JSONException e) {
                    if (BuildConfig.debug)
                        e.printStackTrace();
                }
            }
        }
    }

    private class geocodeTask extends AsyncTask<String, Void, String> {

        private boolean geocodeSuccess = false;

        @Override
        protected String doInBackground(String... destination) {

            final Geocoder gc = new Geocoder(GoogleMap.this);
            try {
                double lowerLeftLatitude;
                double lowerLeftLongitude;
                double upperRightLatitude;
                double upperRightLongitude;
                // start Geocode Search
                // first try: 5km perimeter
                // second try: over 100km perimeter
                // very far results are still working (e.g. Moskow)
                if (geoCodeTry == 0) {
                    if (Core.startLat >= 0) {
                        lowerLeftLatitude = Core.startLat - 0.05;
                        upperRightLatitude = Core.startLat + 0.05;
                    } else {
                        lowerLeftLatitude = Core.startLat + 0.05;
                        upperRightLatitude = Core.startLat - 0.05;
                    }

                    if (Core.startLon >= 0) {
                        lowerLeftLongitude = Core.startLon - 0.07;
                        upperRightLongitude = Core.startLon + 0.07;
                    } else {
                        lowerLeftLongitude = Core.startLon + 0.07;
                        upperRightLongitude = Core.startLon - 0.07;
                    }
                } else {
                    if (Core.startLat >= 0) {
                        lowerLeftLatitude = Core.startLat - 0.77;
                        upperRightLatitude = Core.startLat + 0.77;
                    } else {
                        lowerLeftLatitude = Core.startLat + 0.77;
                        upperRightLatitude = Core.startLat - 0.77;
                    }

                    if (Core.startLon >= 0) {
                        lowerLeftLongitude = Core.startLon - 0.5;
                        upperRightLongitude = Core.startLon + 0.5;
                    } else {
                        lowerLeftLongitude = Core.startLon + 0.5;
                        upperRightLongitude = Core.startLon - 0.5;
                    }
                }

                List<Address> foundAdresses = gc.getFromLocationName(destination[0], 1, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
                        upperRightLongitude);
                for (int i = 0; i < foundAdresses.size(); ++i) {
                    Address x = foundAdresses.get(i);
                    destLat = x.getLatitude();
                    destLon = x.getLongitude();
                    geocodeSuccess = true;
                }
            } catch (Exception e) {
                geocodeSuccess = false;
                if (BuildConfig.debug)
                    e.printStackTrace();
            }
            return destination[0];
        }

        @Override
        protected void onPostExecute(String destination) {

            if (geocodeSuccess) {
                listHandler.removeCallbacksAndMessages(null);
                map.stopAnimation();
                followMe = false;
                geoCodeTry = 0;
                // destLat and destLon are already set in onDoInBackground
                // at List<Address>...
                destLatLng = new LatLng(destLat, destLon);
                setPosition(false);
                setDestPosition(destLatLng);
                drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
                showRouteInfo();
                new routeTask().execute("zielortSollRoutenTaskSelbstRausfinden");

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        mainMenu.getItem(0).collapseActionView();
                    }
                }, 7000);
            } else if (geoCodeTry < 1) {
                new geocodeTask().execute(destination);
                geoCodeTry++;
            } else {
                searchView.clearFocus();
                Toast.makeText(GoogleMap.this, getApplicationContext().getResources().getString(R.string.tx_24), Toast.LENGTH_LONG).show();
                geoCodeTry = 0;
            }
        }
    }


    private class changeSettings extends AsyncTask<Void, Void, Void> {

        private String key;
        private int dataType;
        private boolean setting1;
        private String setting2;
        private int setting3;

        private changeSettings(String key, boolean setting1) {
            this.key = key;
            this.setting1 = setting1;
            dataType = 0;
        }

        private changeSettings(String key, String setting2) {
            this.key = key;
            this.setting2 = setting2;
            dataType = 1;
        }

        private changeSettings(String key, int setting3) {
            this.key = key;
            this.setting3 = setting3;
            dataType = 2;
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
            if (dataType == 0) {
                settings.edit().putBoolean(key, setting1).apply();
            } else if (dataType == 1) {
                settings.edit().putString(key, setting2).apply();
            } else if (dataType == 2) {
                settings.edit().putInt(key, setting3).apply();
            }
            return null;
        }
    }
}