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
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
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
import com.ilm.sandwich.tools.Config;
import com.ilm.sandwich.tools.Core;
import com.ilm.sandwich.tools.HttpRequests;
import com.ilm.sandwich.tools.Locationer;
import com.ilm.sandwich.tools.PlacesAutoComplete;
import com.ilm.sandwich.tools.PlacesTextSearch;
import com.ilm.sandwich.tools.Statistics;
import com.ilm.sandwich.tools.SuggestionsAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class GoogleMap extends AppCompatActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {


    public static double destLat;
    public static double destLon;
    public static boolean followMe;
    public static boolean vibration;
    public static boolean satelliteView;
    public static Bitmap drawableDest;
    public static Core mCore;
    public static int units = 0;
    public static boolean backgroundServiceShallBeOnAgain = false;
    public static String uid;
    public static boolean userHasSetByTouch = false;
    public static SearchView searchView;
    public static Handler listHandler;
    public static SuggestionsAdapter mSuggestionsAdapter;
    public static boolean suggestionsInProgress = false;
    public static MatrixCursor cursor = new MatrixCursor(Config.COLUMNS);
    public static Handler changeSuggestionAdapter;
    static SensorManager mSensorManager;
    static boolean uTaskIsOn;
    static DecimalFormat df0 = new DecimalFormat("0");
    static LatLng longpressLocation;
    static ListView list;
    private static int stepCounterOld = 1;
    private static int now = 0;
    private static long startTime;
    private static int geoCodeTry = 0;
    private static LatLng startLatLng;
    private static LatLng destLatLng;
    private static float oldZoomLevel;
    private static Marker current_position;
    private static Marker current_position_anim;
    private static Marker current_position_ohne;
    private static Marker current_position_anim_ohne;
    private static Marker current_position_k1;
    private static Marker current_position_anim_k1;
    private static Marker current_position_g1;
    private static Marker current_position_anim_g1;
    private static Marker current_position_g2;
    private static Marker current_position_anim_g2;
    private static Marker[] actualMarker = new Marker[1];
    private static boolean brightPoint;
    private static boolean egoPerspective = false;
    private static boolean touchDeactivatedFollow = false;
    private static int compassStatus = 1;
    private static Marker destMarker;
    private static boolean routeHasBeenDrawn = false;
    private static int routeParts = 0;
    private static Marker longPressMarker;
    private static boolean userSwitchedGps = false;
    private final long POS_UPDATE_FREQ = 500;
    public double[] gp2Latk = new double[31];
    public double[] gp2Lonk = new double[31];
    public boolean waitedAtStart = false;
    public int counterRouteComplexity = 0;
    public boolean speechOutput;
    public boolean metricUnits = true;
    public Context sbContext;
    Menu mainMenu;
    String language;
    TextToSpeech mTts;
    int phases;
    int segmentCounter;
    View tutorialOverlay;
    View welcomeView;
    private com.google.android.gms.maps.GoogleMap map;
    private String[] html_instructions = new String[31];
    private String[] polylineArray = new String[31];
    private int iteration = 1;
    private SubMenu subMenu1;
    private int magnUnits;
    private int aclUnits;
    private Locationer mLocationer;
    private boolean knownReasonForBreak = false;
    private boolean finishedTalking = false;
    private int autoCorrectFactor = 1;
    private boolean autoCorrect = false;
    private boolean alreadyWaitingForAutoCorrect = false;
    private int stepsToWait = 0;
    private boolean listVisible = false;
    private Polyline[] completeRoute = new Polyline[31];
    private int viaOptions = 0;
    private Toolbar toolbar;

    public static double neueDistanz(double lat, double lon) {
        // Entfernung bzw. Distanz zur eigenen aktuellen Position
        double mittellat2 = (Core.startLat + lat) / 2 * 0.01745329252;
        double distanceLongitude = 111.3 * Math.cos(mittellat2);
        double dlat2 = 111.3 * (Core.startLat - lat);
        double dlon2 = distanceLongitude * (Core.startLon - lon);
        double distance = Math.sqrt(dlat2 * dlat2 + dlon2 * dlon2);
        return distance; // in km Luftlinie
    }

    // ------------------------------------------------------------------------------------------------------------------
    // ------ONCREATE---------------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------------------------------

    public void setPosition(boolean follow) {
        startLatLng = new LatLng(Core.startLat, Core.startLon);
        try {
            actualMarker[0].setPosition(startLatLng);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        // Log.d("Location-Status", "setPosition:");
        if (follow == true) {
            if (Core.lastErrorGPS < 100) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 18.0F)));
                // Log.d("Location-Status", "zoom auf:" + 18);
            } else if (Core.lastErrorGPS < 231) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 17.0F)));
                // Log.d("Location-Status", "zoom auf:" + 17);
            } else if (Core.lastErrorGPS < 401) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 16.0F)));
                // Log.d("Location-Status", "zoom auf:" + 16);
            } else if (Core.lastErrorGPS < 801) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 15.0F)));
                // Log.d("Location-Status", "zoom auf:" + 15);
            } else if (Core.lastErrorGPS < 1501) {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 14.0F)));
                // Log.d("Location-Status", "zoom auf:" + 14);
            } else {
                map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, map.getCameraPosition().zoom)));
                // Log.d("Location-Status", "zoom auf:" +
                // map.getCameraPosition().zoom);
            }
        }
        // Log.d("Location-Status", "Fazit zoom auf:" +
        // map.getCameraPosition().zoom);
    }

    // ------------------------------------------------------------------------------------------------------------------
    // ------END OF ONCREATE-----------------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Config.usingGoogleMaps = true;
        mLocationer = new Locationer(this);
        setContentView(R.layout.activity_googlemap);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        toolbar = (Toolbar) findViewById(R.id.toolbar_googlemap); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call

        // Rate App show for debugging
        //showRateDialog();
        // Rate App live
        appRateDialog();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googlemap_fragment);
        mapFragment.getMapAsync(this);


        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Config.PERMISSION_REQUEST_FINE_LOCATION);
            //TODO: react to granted permission
        }


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

        map.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng arg0) {
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

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        startHandler();

        //Check if magnetic sensor is existing. If not: Warn user!
        try {
            String magnName = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getName();
        } catch (Exception e) {
            View viewLine = findViewById(R.id.view156);
            viewLine.setVisibility(View.VISIBLE);
            TextView mapText = (TextView) findViewById(R.id.mapText);
            mapText.setVisibility(View.VISIBLE);
            mapText.setText(getResources().getString(R.string.tx_43));
        }

        //Core of SmartNavi
        //does all the step-detection and orientation estimations
        //as well as export feature
        mCore = new Core();

        // if offline, Toast Message will appear automatically
        isOnline();

        TextView mapText = (TextView) findViewById(R.id.mapText);
        mapText.setVisibility(View.INVISIBLE);
        mapText.setSingleLine(false);
        View viewLine = findViewById(R.id.view156);
        viewLine.setVisibility(View.INVISIBLE);

        language = Locale.getDefault().getLanguage();

        new changeSettings("follow", true).execute();
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        uid = settings.getString("uid", "0");
        if (uid.equalsIgnoreCase("0")) {
            String neuUID = "" + (1 + (int) (Math.random() * ((10000000 - 1) + 1)));
            new changeSettings("uid", neuUID).execute();
            uid = settings.getString("uid", "0");
        }
        // Log.d("Location-Status" , "UID = "+uid);
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
                if (Config.debugMode) {
                    e.printStackTrace();
                }
            }
        } else {
            tutorialStuff(0);
        }
        // Compass nach unten setzen
        listHandler.sendEmptyMessageDelayed(1, 10);

        mTts = new TextToSpeech(GoogleMap.this, null);
        mTts.setLanguage(Locale.getDefault());

        // onLongPress Auswahl-Liste
        list = (ListView) findViewById(R.id.liste);
        list.setVisibility(View.INVISIBLE);
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    setHome();
                    list.setVisibility(View.INVISIBLE);
                    listVisible = false;
                    longPressMarker.remove();
                } else {
                    fingerDestination(longpressLocation);
                    list.setVisibility(View.INVISIBLE);
                    listVisible = false;
                    longPressMarker.remove();
                }
            }
        });
    }

    public void onMapTouch() {
        touchDeactivatedFollow = true;
        if (followMe == true) {
            setFollowOff();
            if (compassStatus == 1) {
                ImageView compass = (ImageView) findViewById(R.id.nadel);
                compass.setImageResource(R.drawable.needle2);
                compassStatus = 2;
            } else if (compassStatus == 3) {
                ImageView compass = (ImageView) findViewById(R.id.nadel);
                compass.setImageResource(R.drawable.needle4);
                compassStatus = 4;
            }
        }
    }

    @SuppressLint("HandlerLeak")
    public void startHandler() {
        listHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    // First Position from the Locationer
                    startLatLng = new LatLng(Core.startLat, Core.startLon);
                    setFirstPosition();
                    restartListener();
                    foreignIntent();
                    // start Autocorrect if user wants it
                    listHandler.sendEmptyMessage(6);
                    ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
                    mProgressBar.setVisibility(View.VISIBLE);

                } else if (msg.what == 1) {
                    //Set margin for compass, dependent from height of ActionBar
                    int height = toolbar.getHeight();
                    if (height > 0) {
                        ImageView compass = (ImageView) findViewById(R.id.nadel);
                        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        lp.setMargins(10, height + 10, 0, 0);
                        compass.setLayoutParams(lp);
                        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
                        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);
                        lp2.setMargins(10, height + 10, 0, 0);
                        lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                        mProgressBar.setLayoutParams(lp2);
                    } else {
                        listHandler.sendEmptyMessageDelayed(1, 100);
                    }
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
                } else if (msg.what == 4) {
                    listHandler.removeMessages(4);
                    positionUpdate();
                    // Log.d("Location-Status","Positionstask ACTIVATED (listHandler 4)");
                } else if (msg.what == 5) {
                    //Dialog is location services are disabled
                    final Dialog dialog = new Dialog(GoogleMap.this);
                    dialog.setContentView(R.layout.dialog2);
                    dialog.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();

                    Button cancel = (Button) dialog.findViewById(R.id.dialogCancelLoc);
                    cancel.setOnClickListener(new OnClickListener() {
                        public void onClick(View arg0) {
                            dialog.dismiss();
                            // finish();
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
                    // initialize Autocorrect oder restart new
                    // after activity_settings changed if necessary
                    SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                    autoCorrect = settings.getBoolean("autocorrect", false);
                    //First look if AutoCorrect should be activated, because closeLocationer relies on that
                    if (autoCorrect) {
                        int i = settings.getInt("gpstimer", 1);
                        if (i == 0) { //save as much battery as possible
                            autoCorrectFactor = 4;
                        } else if (i == 1) { // balanced
                            autoCorrectFactor = 2;
                        } else if (i == 2) { // high accuracy
                            autoCorrectFactor = 1;
                        }
                        alreadyWaitingForAutoCorrect = false;
                    }
                } else if (msg.what == 7) {
                    autoCorrect = false;
                    mLocationer.stopAutocorrect();
                } else if (msg.what == 8) {
                    Core.setLocation(Locationer.startLat, Locationer.startLon);
                    mLocationer.stopAutocorrect();
                    if (backgroundServiceShallBeOnAgain == true) {
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
                    setFollowOn();
                    try {
                        listHandler.removeMessages(4);
                        // Log.d("Location-Status",
                        // "Positionstask OFF     listHandler 11");
                    } catch (Exception e) {
                        if (Config.debugMode)
                            e.printStackTrace();
                    }
                    startLatLng = new LatLng(Core.startLat, Core.startLon);
                    if (egoPerspective) {
                        // Kamera in Kompass-Richtung drehen wenn gewünscht
                        //Turn Camera in Compass Direction if wanted
                        CameraPosition currentPlace = new CameraPosition.Builder().target(startLatLng).bearing((float) Core.azimuth).tilt(65.5f).zoom(19)
                                .build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
                    } else {
                        // Turn Camera towards North
                        CameraPosition currentPlace = new CameraPosition.Builder().target(startLatLng).bearing(0.0F).tilt(0.0F).zoom(19).build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
                    }
                    // Restart PositionTask
                    listHandler.sendEmptyMessageDelayed(4, 1500);
                    // Log.d("Location-Status", "ListeHandler 11  ruft auf");
                } else if (msg.what == 12) {
                    // message from Locationer
                    ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
                    mProgressBar.setVisibility(View.GONE);
                } else if (msg.what == 13) {
                    showGPSDialog();
                } else if (msg.what == 14) {
                    // next position from Locationer
                    setPosition(true);
                } else if (msg.what == 15) {
                    onMapTouch();
                }

                super.handleMessage(msg);
            }
        };
    }

    public void createAllMarkersInvisible() {
        LatLng northPoleHideout = new LatLng(90.0D, 0.0D);

        destMarker = map.addMarker(new MarkerOptions().position(northPoleHideout).icon(BitmapDescriptorFactory.fromBitmap(drawableDest)));
        destMarker.setVisible(false);

        current_position = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position)).anchor(0.5f, 0.5f));
        current_position.setVisible(false);

        current_position_anim = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim)).anchor(0.5f, 0.5f));
        current_position_anim.setVisible(false);

        current_position_ohne = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_ohne)).anchor(0.5f, 0.5f));
        current_position_ohne.setVisible(false);

        current_position_anim_ohne = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_ohne)).anchor(0.5f, 0.5f));
        current_position_anim_ohne.setVisible(false);

        current_position_k1 = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_k1)).anchor(0.5f, 0.5f));
        current_position_k1.setVisible(false);

        current_position_anim_k1 = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_k1)).anchor(0.5f, 0.5f));
        current_position_anim_k1.setVisible(false);

        current_position_g1 = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_g1)).anchor(0.5f, 0.5f));
        current_position_g1.setVisible(false);

        current_position_anim_g1 = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_g1)).anchor(0.5f, 0.5f));
        current_position_anim_g1.setVisible(false);

        current_position_g2 = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_g2)).anchor(0.5f, 0.5f));
        current_position_g2.setVisible(false);

        current_position_anim_g2 = map.addMarker(new MarkerOptions().position(northPoleHideout)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_g2)).anchor(0.5f, 0.5f));
        current_position_anim_g2.setVisible(false);
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
            String[] requestArray1 = {"", "", ""};
            String[] requestArray2 = {"", "", ""};

            String requestString = this.getIntent().getDataString();
            if (requestString != null) {
                if (requestString.contains("google.navigation")) {
                    requestArray1 = requestString.split("&q=");
                    requestArray1 = requestArray1[0].split("%2C");
                    destLon = Float.parseFloat(requestArray1[1]);
                    requestArray2 = requestArray1[0].split("ll=");
                    destLat = Float.parseFloat(requestArray2[1]);
                } else if (requestString.contains("http://maps.google")) {
                    String[] requestArray3 = {"", "", ""};
                    String[] requestArray4 = {"", "", ""};

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
                                if (Config.debugMode)
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
                listHandler.removeMessages(4);
                // Log.d("Location-Status","Positionstask AUS    weil foreignIntent");
                map.stopAnimation();
                setPosition(false);
                drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
                setDestPosition(destLatLng);
                showRouteInfo();
                new routeTask().execute("zielortSollRoutenTaskSelbstRausfinden");
            }
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
    }

    public void setFollowOn() {
        ImageView compass = (ImageView) findViewById(R.id.nadel);
        followMe = true;
        if (compassStatus == 2) {
            // set to status 1
            compass.setImageResource(R.drawable.needle);
            compassStatus = 1;
        } else if (compassStatus == 4) {
            // set to status 3
            compass.setImageResource(R.drawable.needle3);
            compassStatus = 3;
        }
        stepCounterOld = stepCounterOld - 1;
    }

    public void setFollowOff() {
        ImageView compass = (ImageView) findViewById(R.id.nadel);
        followMe = false;
        if (compassStatus == 1) {
            // set to status 2
            compass.setImageResource(R.drawable.needle2);
            compassStatus = 2;
        } else if (compassStatus == 3) {
            // set to status 4
            compass.setImageResource(R.drawable.needle4);
            compassStatus = 4;
        }
    }

    public void setHome() {
        Core.startLat = longpressLocation.latitude;
        Core.startLon = longpressLocation.longitude;
        Core.stepCounter++;

        actualMarker[0].setPosition(longpressLocation);

        ImageView compass = (ImageView) findViewById(R.id.nadel);
        LatLng newPos = new LatLng(Core.startLat, Core.startLon);
        listHandler.removeMessages(4);
        // Log.d("Location-Status", "Positionstask AUS      setHome");
        if (compassStatus == 4) {
            // status to 3
            compass.setImageResource(R.drawable.needle3);
            compassStatus = 3;
            followMe = true;
            map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
        } else if (compassStatus == 2) {
            // status to 1
            compass.setImageResource(R.drawable.needle);
            followMe = true;
            compassStatus = 1;
            map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
        }
        // Positionstask wieder anwerfen
        listHandler.sendEmptyMessageDelayed(4, 1500);
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

                if (zoomLevel < 17) {
                    current_position_ohne.setPosition(newPos);
                    current_position_ohne.setVisible(true);
                    actualMarker[0] = current_position_ohne;
                } else if (zoomLevel >= 17 && zoomLevel < 18) {
                    current_position_k1.setPosition(newPos);
                    current_position_k1.setVisible(true);
                    actualMarker[0] = current_position_k1;
                } else if (zoomLevel >= 18 && zoomLevel < 19) {
                    current_position.setPosition(newPos);
                    current_position.setVisible(true);
                    actualMarker[0] = current_position;
                } else if (zoomLevel >= 19 && zoomLevel < 20) {
                    current_position_g1.setPosition(newPos);
                    current_position_g1.setVisible(true);
                    actualMarker[0] = current_position_g1;
                } else if (zoomLevel >= 20) {
                    current_position_g2.setPosition(newPos);
                    current_position_g2.setVisible(true);
                    actualMarker[0] = current_position_g2;
                }
            }

            if (Core.stepCounter != stepCounterOld) {
                stepCounterOld = Core.stepCounter;

                actualMarker[0].setVisible(false);

                if (zoomLevel < 17) {
                    if (now % 2 != 0) {
                        current_position_ohne.setPosition(newPos);
                        current_position_ohne.setVisible(true);
                        actualMarker[0] = current_position_ohne;
                        brightPoint = false;
                    } else {
                        current_position_anim_ohne.setPosition(newPos);
                        current_position_anim_ohne.setVisible(true);
                        actualMarker[0] = current_position_anim_ohne;
                        brightPoint = true;
                    }
                } else if (zoomLevel >= 17 && zoomLevel < 18) {
                    if (now % 2 != 0) {
                        current_position_k1.setPosition(newPos);
                        current_position_k1.setVisible(true);
                        actualMarker[0] = current_position_k1;
                        brightPoint = false;
                    } else {
                        current_position_anim_k1.setPosition(newPos);
                        current_position_anim_k1.setVisible(true);
                        actualMarker[0] = current_position_anim_k1;
                        brightPoint = true;
                    }
                } else if (zoomLevel >= 18 && zoomLevel < 19) {
                    if (now % 2 != 0) {
                        current_position.setPosition(newPos);
                        current_position.setVisible(true);
                        actualMarker[0] = current_position;
                        brightPoint = false;
                    } else {
                        current_position_anim.setPosition(newPos);
                        current_position_anim.setVisible(true);
                        actualMarker[0] = current_position_anim;
                        brightPoint = true;
                    }
                } else if (zoomLevel >= 19 && zoomLevel < 20) {
                    if (now % 2 != 0) {
                        current_position_g1.setPosition(newPos);
                        current_position_g1.setVisible(true);
                        actualMarker[0] = current_position_g1;
                        brightPoint = false;
                    } else {
                        current_position_anim_g1.setPosition(newPos);
                        current_position_anim_g1.setVisible(true);
                        actualMarker[0] = current_position_anim_g1;
                        brightPoint = true;
                    }
                } else if (zoomLevel >= 20) {
                    if (now % 2 != 0) {
                        current_position_g2.setPosition(newPos);
                        current_position_g2.setVisible(true);
                        actualMarker[0] = current_position_g2;
                        brightPoint = false;
                    } else {
                        current_position_anim_g2.setPosition(newPos);
                        current_position_anim_g2.setVisible(true);
                        actualMarker[0] = current_position_anim_g2;
                        brightPoint = true;
                    }
                }

            }
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        if (followMe) {
            if (now % 2 != 0) {
                if (egoPerspective) {
                    // Kamera in Kompass-Richtung drehen wenn gewünscht
                    CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing((float) Core.azimuth).tilt(65.5f).zoom(zoomLevel)
                            .build();
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
                } else {
                    // Kamera in nach Norden ausrichten
                    CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing(0.0F).tilt(0.0F).zoom(zoomLevel).build();
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
                }
            }
        }
        now++;
        if (segmentCounter < (phases - 1) && waitedAtStart) {
            segmentControl();
        }
        // Log.d("Location-Status"," - neuer Task - ");
        listHandler.sendEmptyMessageDelayed(4, 500);
    }

    // ****************************************************************
    // ************* ROUTE-TASK **************************************

    public void setFirstPosition() {
        startLatLng = new LatLng(Core.startLat, Core.startLon);
        current_position.setPosition(startLatLng);
        current_position.setVisible(true);
        actualMarker[0] = current_position;

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
            viewLine.setVisibility(View.VISIBLE);
            TextView mapText = (TextView) findViewById(R.id.mapText);
            mapText.setVisibility(View.VISIBLE);
            mapText.setText(getResources().getString(R.string.tx_06));
        } else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 13.0F)));
        }
        // start positionTask
        listHandler.sendEmptyMessageDelayed(4, POS_UPDATE_FREQ);
    }

    // ****************** END ROUTE-TASK *******************************
    // ********************************************************************

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
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
        double abstand = neueDistanz(gp2Latk[segmentCounter], gp2Lonk[segmentCounter]);
        if (abstand < 0.03 && finishedTalking) {
            finishedTalking = false;
            completeRoute[segmentCounter].remove();
            segmentCounter++;
            Spanned marked_up = Html.fromHtml(html_instructions[segmentCounter]);
            String textString = marked_up.toString();
            ((TextView) findViewById(R.id.mapText)).setText(textString);
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
        mapText.setText(getApplicationContext().getResources().getString(R.string.tx_04));
        viewLine.setVisibility(View.VISIBLE);
        mapText.setVisibility(View.VISIBLE);
    }

    public void makeInfo(String endAddress, String firstDistance) {
        TextView mapText = (TextView) findViewById(R.id.mapText);
        View viewLine = findViewById(R.id.view156);
        if (firstDistance != null) {
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
            viewLine.setVisibility(View.INVISIBLE);
            mapText.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        try {
            Config.usingGoogleMaps = true;
            egoPerspective = false;
            compassStatus = 1;
            ImageView compass = (ImageView) findViewById(R.id.nadel);
            compass.setImageResource(R.drawable.needle);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        if (status != ConnectionResult.SUCCESS) {
            try {
                new changeSettings("MapSource", "MapQuestOSM").execute();
            } catch (Exception e) {
                if (Config.debugMode)
                    e.printStackTrace();
            }
        } else if (!userSwitchedGps) {
            if (listHandler != null) {
                listHandler.sendEmptyMessageDelayed(4, POS_UPDATE_FREQ);
            }
            // Log.d("Location-Status","Positionstask ACTIVATED because onResume 1");
            restartListenerLight();

            if (knownReasonForBreak) {
                //User is coming from Settings, Background Service or About
                knownReasonForBreak = false;
            } else {
                //User calls onResume, probably because screen was deactiaved for a short time. Get GPS Position to go on!
                mLocationer.startLocationUpdates();
            }

            if (Config.debugMode) {
                TextView mapText = (TextView) findViewById(R.id.mapText);
                mapText.setVisibility(View.GONE);
            }

            if (Core.startLat != 0) {
                startLatLng = new LatLng(Core.startLat, Core.startLon);
            }

            if (BackgroundService.sGeoLat != 0) {
                startLatLng = new LatLng(BackgroundService.sGeoLat, BackgroundService.sGeoLon);
            }

            ImageView compass = (ImageView) findViewById(R.id.nadel);
            LatLng newPos = new LatLng(Core.startLat, Core.startLon);
            if (compassStatus == 4) {
                listHandler.removeMessages(4);
                // Log.d("Location-Status","Positionstask OFF        compassNadel");
                // status auf 3
                compass.setImageResource(R.drawable.needle3);
                compassStatus = 3;
                map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
                // Positionstask reactivate
                listHandler.sendEmptyMessageDelayed(4, 1500);
            } else if (compassStatus == 2) {
                listHandler.removeMessages(4);
                // Log.d("Location-Status","Positionstask OFF        compassNadel");
                // status to 1
                compass.setImageResource(R.drawable.needle);
                compassStatus = 1;
                map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
                // Positionstask reactivate
                listHandler.sendEmptyMessageDelayed(4, 1500);
            }
            followMe = true;

            SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
            autoCorrect = settings.getBoolean("autocorrect", false);

            // Export
            boolean export = settings.getBoolean("export", false);
            try {
                mCore.writeLog(export);
            } catch (Exception e) {
                if (Config.debugMode)
                    e.printStackTrace();
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
        } else if (userSwitchedGps) {
            //SmartNavi sent user into his settings (e.g. to activate GPS)
            //so, search for new position
            mLocationer.startLocationUpdates();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        try {
            mLocationer.deactivateLocationer();
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        try {
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
            mProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }

        try {
            listHandler.removeMessages(4);
            // Log.d("Location-Status", "Positionstask OFF    onPause");
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
        // // Log.d("Location-Status", "Sensors stopped.");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        routeHasBeenDrawn = false;

        // // Log.d("Location-Status",
        // "SmartNavi closed! Sensors off.");
        try {
            listHandler.removeMessages(1);
            listHandler.removeMessages(2);
            listHandler.removeMessages(3);
            listHandler.removeMessages(4);
            listHandler.removeMessages(5);
            listHandler.removeMessages(6);
            listHandler.removeMessages(7);
            listHandler.removeMessages(8);
            listHandler.removeMessages(9);
            listHandler.removeMessages(10);
            listHandler.removeMessages(11);

        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        if (status != ConnectionResult.SUCCESS) {
            // nothing
        } else {
            map.clear();
            mSensorManager.unregisterListener(this);
            if (uTaskIsOn) {
                waitedAtStart = true;
                uTaskIsOn = false;
            }
            mCore.shutdown(this);

            mTts.stop();
            mTts.shutdown();

            try {
                mLocationer.stopAutocorrect();
            } catch (Exception e) {
                if (Config.debugMode)
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
            try {
                mainMenu.performIdentifierAction(subMenu1.getItem().getItemId(), 0);

                // if off, longPressMenu will be made invisible
                list = (ListView) findViewById(R.id.liste);
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
            list = (ListView) findViewById(R.id.liste);
            list.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
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
                // Tutorial
                tutorialStuff(1);
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

    private void restartListener() {
        // // Log.d("Location-Status", "Sensors started.");
        iteration = 1;
        Config.meanAclFreq = Config.meanMagnFreq = 0;
        aclUnits = 0;
        magnUnits = 0;
        startTime = System.nanoTime();
        try {
            mSensorManager.registerListener(GoogleMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
            mSensorManager.registerListener(GoogleMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
    }

    private void restartListenerLight() {
        // // Log.d("Location-Status", "Sensoren reactivated.");
        try {
            mSensorManager.unregisterListener(GoogleMap.this);
            mSensorManager.registerListener(GoogleMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
            mSensorManager.registerListener(GoogleMap.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

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

                if (Config.backgroundServiceActive && units % 50 == 0) {
                    BackgroundService.newFakePosition();
                }

                long timePassed = System.nanoTime() - startTime;
                aclUnits++;
                units++;

                if (timePassed >= 2000000000) { // every 2sek
                    mCore.changeDelay(aclUnits / 2, 0);
                    mCore.changeDelay(magnUnits / 2, 1);
                    // Log.d("egal", "timePassed = 2000; aclFreq = " + aclUnits / 2 + " magnFreq = " + magnUnits / 2);

                    // calculate mean values and save
                    Config.meanAclFreq = (Config.meanAclFreq + aclUnits / 2) / iteration;
                    Config.meanMagnFreq = (Config.meanMagnFreq + magnUnits / 2) / iteration;

                    startTime = System.nanoTime();
                    aclUnits = magnUnits = 0;
                    iteration = 2;
                }

                mCore.imbaGravity(event.values.clone());
                mCore.imbaLinear(event.values.clone());
                mCore.calculate();
                mCore.stepDetection();

                // AutoCorrect (dependent on Factor, i.e. number of steps)
                if (autoCorrect) {
                    if (alreadyWaitingForAutoCorrect == false) {
                        alreadyWaitingForAutoCorrect = true;
                        stepsToWait = Core.stepCounter + 75 * autoCorrectFactor;
                        // // Log.d("Location-Status", Core.stepCounter +
                        // " von " + stepsToWait);
                    }
                    if (Core.stepCounter >= stepsToWait) {
                        if (Config.backgroundServiceActive == true) {
                            backgroundServiceShallBeOnAgain = true;
                            BackgroundService.pauseFakeProvider();
                        }
                        mLocationer.starteAutocorrect();
                        alreadyWaitingForAutoCorrect = false;
                    }
                }
                break;
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
        LatLngBounds grenzen = new LatLngBounds(southwest, northeast);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(grenzen, 100));
        listHandler.sendEmptyMessageDelayed(11, 3000);
    }

    public void tutorialStuff(int i) {
        viaOptions = i;
        //show Tutorial, and decativate touching the Map
        map.getUiSettings().setAllGesturesEnabled(false);

        welcomeView = findViewById(R.id.welcomeView);
        welcomeView.setVisibility(View.VISIBLE);

        Button welcomeButton = (Button) findViewById(R.id.welcomeButton);
        welcomeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                welcomeView.setVisibility(View.INVISIBLE);
                tutorialOverlay = findViewById(R.id.tutorialOverlay);
                tutorialOverlay.setVisibility(View.VISIBLE);
            }
        });

        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String stepLengthString = settings.getString("step_length", null);
        Spinner spinner = (Spinner) findViewById(R.id.tutorialSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.dimension, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        if (stepLengthString != null) {
            try {
                stepLengthString = stepLengthString.replace(",", ".");
                int savedBodyHeight = Integer.parseInt(stepLengthString);
                if (savedBodyHeight < 241 && savedBodyHeight > 119) {
                    EditText editText = (EditText) findViewById(R.id.tutorialEditText);
                    editText.setText("" + savedBodyHeight);
                    spinner.setSelection(0);
                } else if (savedBodyHeight < 95 && savedBodyHeight > 45) {
                    EditText editText = (EditText) findViewById(R.id.tutorialEditText);
                    editText.setText("" + savedBodyHeight);
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

        Button startButton = (Button) findViewById(R.id.startbutton);
        startButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                boolean tutorialDone = false;
                final EditText heightField = (EditText) findViewById(R.id.tutorialEditText);
                int op = heightField.length();
                float number;
                if (op != 0) {
                    try {
                        number = Float.valueOf(heightField.getText().toString());
                        if (number < 241 && number > 119 && metricUnits == true) {

                            String numberString = df0.format(number);
                            new changeSettings("step_length", numberString).execute();
                            Core.stepLength = (float) (number / 222);
                            tutorialDone = true;
                        } else if (number < 95 && number > 45 && metricUnits == false) {

                            String numberString = df0.format(number);
                            new changeSettings("step_length", numberString).execute();
                            Core.stepLength = (float) (number * 2.54 / 222);
                            tutorialDone = true;
                        } else {
                            Toast.makeText(GoogleMap.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                        }

                    } catch (NumberFormatException e) {
                        if (Config.debugMode)
                            e.printStackTrace();
                        Toast.makeText(GoogleMap.this, getApplicationContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(GoogleMap.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
                }

                if (tutorialDone) {
                    // Tutorial hide
                    tutorialOverlay = findViewById(R.id.tutorialOverlay);
                    tutorialOverlay.setVisibility(View.INVISIBLE);
                    // make Map touchable again
                    map.getUiSettings().setAllGesturesEnabled(true);
                    // show LongPressDialog
                    if (viaOptions == 1) {
                        showLongPressDialog();
                    }
                }
            }
        });

        EditText bodyHeightField = (EditText) findViewById(R.id.tutorialEditText);
        bodyHeightField.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
                    try {
                        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        try {
                            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        EditText bodyHeightField = (EditText) findViewById(R.id.tutorialEditText); //Workaround Coursor out off textfield
                        bodyHeightField.setFocusable(false);
                        bodyHeightField.setFocusableInTouchMode(true);
                        bodyHeightField.setFocusable(true);
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
            actualMarker[0].setVisible(false);
        } catch (Exception e) {
            if (Config.debugMode)
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

    public void compassNadel(View view) {

        // If off, longPressMenu will become invisible
        try {
            list = (ListView) findViewById(R.id.liste);
            list.setVisibility(View.INVISIBLE);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }

        ImageView compass = (ImageView) findViewById(R.id.nadel);
        float zoomLevel = map.getCameraPosition().zoom;
        LatLng newPos = new LatLng(Core.startLat, Core.startLon);

        listHandler.removeMessages(4);
        // Log.d("Location-Status",
        // "Positionstask OFF        compassNeedle");

        if (followMe == false && touchDeactivatedFollow == true) {
            touchDeactivatedFollow = false;
            if (compassStatus == 4) {
                // status to 3
                compass.setImageResource(R.drawable.needle3);
                compassStatus = 3;
                map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
            } else if (compassStatus == 2) {
                // status to 1
                compass.setImageResource(R.drawable.needle);
                compassStatus = 1;
                map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
            }
            //Important: First "if" and THEN setFollowOn
            setFollowOn();
        } else if (compassStatus == 1) {
            // status to 3
            egoPerspective = true;
            compass.setImageResource(R.drawable.needle3);
            compassStatus = 3;
            // Camera in Compass Direction
            CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing((float) Core.azimuth).tilt(65.5f).zoom(zoomLevel).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        } else if (compassStatus == 3) {
            // status to 1
            egoPerspective = false;
            compass.setImageResource(R.drawable.needle);
            compassStatus = 1;
            // Camera points North
            CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing(0.0F).tilt(0.0F).zoom(zoomLevel).build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        } else if (compassStatus == 4) {
            // status to 3
            compass.setImageResource(R.drawable.needle3);
            compassStatus = 3;
            setFollowOn();
            map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
        } else if (compassStatus == 2) {
            // status to 1
            compass.setImageResource(R.drawable.needle);
            setFollowOn();
            compassStatus = 1;
            map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
        }
        // Positionstask reactivate
        listHandler.sendEmptyMessageDelayed(4, 1500);
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
        final View appRateDialog = findViewById(R.id.appRateDialog);
        appRateDialog.setVisibility(View.VISIBLE);

        Button rateButton1 = (Button) findViewById(R.id.rateButton);
        rateButton1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                appRateDialog.setVisibility(View.INVISIBLE);

                SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", 0);
                int notRated = prefs.getInt("not_rated", 0) + 1;

                new changeSettings("not_rated", notRated).execute();

                if (notRated == 1) {
                    new changeSettings("launch_count", -6).execute();
                } else if (notRated == 2) {
                    new changeSettings("launch_count", -8).execute();
                } else if (notRated == 3) {
                    new changeSettings("launch_count", -10).execute();
                } else if (notRated == 4) {
                    new changeSettings("dontshowagain", true).execute();
                }
            }
        });


        Button rateButton3 = (Button) findViewById(R.id.rateButton2);
        rateButton3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new changeSettings("not_rated", 999).execute();
                appRateDialog.setVisibility(View.INVISIBLE);
                new changeSettings("dontshowagain", true).execute();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
            }
        });
    }

    public void clickOnStars(final View view) {
        new changeSettings("not_rated", 999).execute();
        final View appRateDialog = findViewById(R.id.appRateDialog);
        appRateDialog.setVisibility(View.INVISIBLE);
        new changeSettings("dontshowagain", true).execute();
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        // Log.d("Location-Status","LocationClient: Connection FAILED" + arg0.getErrorCode());
        startActivity(new Intent(GoogleMap.this, OsmMap.class));
        finish();
    }

    // at Touch on ProgressBar
    public void abortGPS(final View view) {
        // Abort GPS was pressed (ProgressBar was pressed)
        try {
            mLocationer.deactivateLocationer();
            ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
            mProgressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            if (Config.debugMode)
                e.printStackTrace();
        }
        Toast.makeText(this, getResources().getString(R.string.tx_82), Toast.LENGTH_SHORT).show();
    }

    private void showGPSDialog() {
        final Dialog dialogGPS = new Dialog(GoogleMap.this);
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
                    userSwitchedGps = true;
                } catch (android.content.ActivityNotFoundException ae) {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    userSwitchedGps = true;
                }
                dialogGPS.dismiss();
            }
        });
    }


    private void prepareSearchView() {
        searchView.setSuggestionsAdapter(mSuggestionsAdapter);

        // onClick closes the longPressMenu if it is shown
        searchView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // if off, longPressMenu will be made invisible
                try {
                    list = (ListView) findViewById(R.id.liste);
                    list.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
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
                    Toast.makeText(GoogleMap.this, "You can not find Chuck Norris. Chuck Norris finds YOU!", Toast.LENGTH_LONG).show();
                else if (query.equalsIgnoreCase("cake") || query.equalsIgnoreCase("the cake") || query.equalsIgnoreCase("portal"))
                    Toast.makeText(GoogleMap.this, "The cake is a lie!", Toast.LENGTH_LONG).show();
                else if (query.equalsIgnoreCase("tomlernt")) {
                    // start debug mode
                    Toast.makeText(GoogleMap.this, "Debug-Mode ON", Toast.LENGTH_SHORT).show();
                    Config.debugMode = true;
                } else if (query.equalsIgnoreCase("rateme")) {
                    // show app rate dialog
                    showRateDialog();
                } else if (query.equalsIgnoreCase("smartnavihelp")) {
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
                if (query.length() >= 5) {
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
                String[] zielOrtArray = new String[5];
                zielOrtArray = endAddress.split(",", 3);
                try {
                    endAddress = zielOrtArray[0];
                    endAddress += "\n" + zielOrtArray[1];
                } catch (Exception e) {
                    // thats possible if Destination Name is only 1 line
                    if (Config.debugMode)
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
                if (Config.debugMode)
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
                if (Config.debugMode)
                    e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void void1) {
            if (getPathSuccess) {
                drawPath();
                listHandler.removeMessages(4);
                // Log.d("Location-Status",
                // "Positionstask OFF    because routeTask");
                //PositionTask shall NOT run. After routeStartAnimation in listHandler it shall start again.
                routeStartAnimation(northeastLatLng, southwestLatLng);
            }
            makeInfo(endAddress, firstDistance);
            // setPosition(false);
        }

        public void drawPath() {
            if (routeHasBeenDrawn) {
                // Log.d("Location-Status", "Route has been drawn = " +
                // routeHasBeenDrawn);
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
                if (Config.debugMode)
                    e.printStackTrace();
            }
        }

    }

    private class changeSettings extends AsyncTask<Void, Void, Void> {

        private String key;
        private int dataType;
        private boolean setting1;
        private String setting2;
        private int einstellung3;

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

        private changeSettings(String key, int einstellung3) {
            this.key = key;
            this.einstellung3 = einstellung3;
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
                settings.edit().putInt(key, einstellung3).commit();
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
                Toast.makeText(GoogleMap.this, getApplicationContext().getResources().getString(R.string.tx_77), Toast.LENGTH_LONG).show();
            } else {
                // set destination for the routing tasks
                try {
                    destLat = (Double) destination.get("lat");
                    destLon = (Double) destination.get("lng");
                    destLatLng = new LatLng(destLat, destLon);
                    listHandler.removeCallbacksAndMessages(null);
                    map.stopAnimation();
                    listHandler.removeMessages(4);
                    // Log.d("Location-Status","Positionstask OFF    placesTextSearch");
                    setPosition(false);
                    setDestPosition(destLatLng);
                    showRouteInfo();
                    new routeTask().execute(query);
                } catch (JSONException e) {
                    if (Config.debugMode)
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
                if (Config.debugMode)
                    e.printStackTrace();
            }
            return destination[0];
        }

        @Override
        protected void onPostExecute(String destination) {

            if (geocodeSuccess) {
                listHandler.removeCallbacksAndMessages(null);
                map.stopAnimation();
                listHandler.removeMessages(4);
                // Log.d("Location-Status","Positionstask OFF    geoCodeTask");
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

}