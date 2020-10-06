package com.ilm.sandwich;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.ilm.sandwich.fragments.RatingFragment;
import com.ilm.sandwich.fragments.TutorialFragment;
import com.ilm.sandwich.sensors.Core;
import com.ilm.sandwich.tools.Config;
import com.ilm.sandwich.tools.HttpRequests;
import com.ilm.sandwich.tools.Locationer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * MapActivitiy for Google Maps
 *
 * @author Christian Henke
 * https://smartnavi.app
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
    public static Handler listHandler;
    static boolean uTaskIsOn;
    static LatLng longpressLocation;
    private static int stepCounterOld = 1;
    private static int now = 0;
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
    ListView list;
    Menu mainMenu;
    String language;
    TextToSpeech mTts;
    ProgressBar mProgressBar;
    int phases;
    int segmentCounter;
    TutorialFragment tutorialFragment;
    RatingFragment ratingFragment;
    private FirebaseAnalytics mFirebaseAnalytics;
    private com.google.android.gms.maps.GoogleMap map;
    private String[] html_instructions = new String[31];
    private String[] polylineArray = new String[31];
    private Locationer mLocationer;
    private boolean knownReasonForBreak = false;
    private boolean finishedTalking = false;
    private boolean listVisible = false;
    private Polyline[] completeRoute = new Polyline[31];
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
        setContentView(R.layout.activity_googlemap);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Toolbar toolbar = findViewById(R.id.toolbar_googlemap); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                   // Setting toolbar as the ActionBar with setSupportActionBar() call

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        if (!BuildConfig.DEBUG) {
            mFirebaseRemoteConfig.fetch(1);
            mFirebaseRemoteConfig.activate();
        }
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .setMinimumFetchIntervalInSeconds(1)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(configSettings);
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful()) {
                    boolean updated = task.getResult();
                    Log.d("Firebase", "Config params updated: " + updated);
                } else {
                    Log.d("Firebase", "Fetch failed");
                }
            }
        });

        fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // longPressMenu will become invisible
                    try {
                        list = findViewById(R.id.liste);
                        if (list != null && list.getVisibility() == View.VISIBLE)
                            list.setVisibility(View.INVISIBLE);
                    } catch (Exception e) {
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
            mFirebaseAnalytics.logEvent("Granted_Google_Location", null);
            proceedOnCreate();
        } else {
            Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_100), Toast.LENGTH_LONG).show();
            mFirebaseAnalytics.logEvent("Denied_Google_Location", null);
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
                mFirebaseAnalytics.logEvent("Longpress_Map", null);
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
            TextView mapText = findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setVisibility(View.VISIBLE);
            }
            if (mapText != null) {
                mapText.setText(getResources().getString(R.string.tx_43));
            }
        }

        TextView mapText = findViewById(R.id.mapText);
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
                if (stepLengthString.contains("'")) {
                    String[] feetInchString = stepLengthString.split("'");
                    String feetString = feetInchString[0];
                    float feet = Float.valueOf(feetString);

                    //Check if user provided inch, if so set that. If not assume 0
                    float inch = 0;
                    if (feetInchString.length > 1) {
                        String inchString = feetInchString[1];
                        inch = Float.valueOf(inchString);
                    } else {
                        inch = 0;
                    }

                    float totalInch = 12 * feet + inch;
                    Core.stepLength = (float) (totalInch * 2.54 / 222);
                } else {
                    stepLengthString = stepLengthString.replace(",", ".");
                    Float savedBodyHeight = (Float.parseFloat(stepLengthString));
                    Core.stepLength = savedBodyHeight / 222;
                }
                if (BuildConfig.DEBUG) Log.i("Step length", "Step length = " + Core.stepLength);
            } catch (NumberFormatException e) {
                e.printStackTrace();
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
        list = findViewById(R.id.liste);
        if (list != null) {
            list.setVisibility(View.INVISIBLE);
        }
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (arg2 == 0) {
                    setHome();
                    mFirebaseAnalytics.logEvent("SetPosition_after_Longpress", null);
                    list.setVisibility(View.INVISIBLE);
                    listVisible = false;
                    longPressMarker.remove();
                    positionUpdate();
                } else {
                    fingerDestination(longpressLocation);
                    mFirebaseAnalytics.logEvent("SetDestination_after_Longpress", null);
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
                    //not used anymore
                } else if (msg.what == 10) {
                    //BackgroundService is created, so dont stop sensors
                    if (mCore != null)
                        mCore.reactivateSensors();
                    if (BuildConfig.DEBUG)
                        Log.i("SmartNavi", "Reactivate Sensors because Background service is running.");
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
                mProgressBar = findViewById(R.id.progressBar1);
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
                mProgressBar = findViewById(R.id.progressBar1);
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
                new routeTask().execute();
            }
        } catch (Exception e) {
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
            TextView mapText = findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setVisibility(View.VISIBLE);
                mapText.setText(getResources().getString(R.string.tx_06));
            }
        } else {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 13.0F)));
        }
        followMe = true;
        if (mCore == null) {
            mCore = new Core(GoogleMap.this);
            mCore.startSensors();
        }
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
            TextView mapText = findViewById(R.id.mapText);
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
                mTts.speak(textString2, TextToSpeech.QUEUE_FLUSH, null, null);
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
        TextView mapText = findViewById(R.id.mapText);
        View viewLine = findViewById(R.id.view156);
        if (mapText != null && viewLine != null) {
            mapText.setText(getApplicationContext().getResources().getString(R.string.tx_04));
            mapText.setVisibility(View.VISIBLE);
            viewLine.setVisibility(View.VISIBLE);
        }
    }

    public void makeInfo(String endAddress, String firstDistance) {
        TextView mapText = findViewById(R.id.mapText);
        View viewLine = findViewById(R.id.view156);
        if (firstDistance != null && mapText != null && viewLine != null) {
            mapText.setText(endAddress + "\n\n" + firstDistance);
            viewLine.setVisibility(View.VISIBLE);
            mapText.setVisibility(View.VISIBLE);
            if (speechOutput) {
                firstDistance = firstDistance.replace("-", " ").replace("\n", " ").replace("/", " ");
                mTts.speak(firstDistance, TextToSpeech.QUEUE_FLUSH, null, null);
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
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int status = api.isGooglePlayServicesAvailable(this);

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
        ProgressBar mProgressBar = findViewById(R.id.progressBar1);
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
            try {
                if (mTts != null) {
                    mTts.stop();
                    mTts.shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mLocationer.stopAutocorrect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        mainMenu = menu;
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // if off, longPressMenu will be made invisible
            list = findViewById(R.id.liste);
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
        list = findViewById(R.id.liste);
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
            case R.id.menu_search:
                autoCompleteSearch();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void autoCompleteSearch() {
        //Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), Config.PLACES_SDK_API_KEY);
        }
        // Set the fields to specify which types of place data to return.
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME);
        //Define Reactangular Search Box for 3km
        double lowerLeftLatitude;
        double lowerLeftLongitude;
        double upperRightLatitude;
        double upperRightLongitude;

        lowerLeftLatitude = Core.startLat - 0.03;
        upperRightLatitude = Core.startLat + 0.03;

        lowerLeftLongitude = Core.startLon - 0.04;
        upperRightLongitude = Core.startLon + 0.04;

        //SoutWest , NothEast
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(lowerLeftLatitude, lowerLeftLongitude),
                new LatLng(upperRightLatitude, upperRightLongitude));
        Log.i("AutoComplete Bounds", "SouthWest: " + bounds.getSouthwest().latitude + " " + bounds.getSouthwest().longitude + "  " + " NorthEast: " + bounds.getNortheast().latitude + " " + bounds.getNortheast().longitude);
        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields).setLocationBias(bounds)
                .build(this);

        startActivityForResult(intent, 6767);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 6767) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i("SmartNavi Autocomplete", "Place: " + place.getName() + ", " + place.getLatLng());
                destLatLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                setDestPosition(destLatLng);
                new routeTask().execute();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i("SmartNavi Autocomplete", status.getStatusMessage());
                Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_103), Toast.LENGTH_LONG).show();
            }
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
        new routeTask().execute();
    }

    public void setDestPosition(LatLng z) {
        destMarker.setPosition(z);
        destMarker.setVisible(true);
    }

    public void routeStartAnimation(LatLng northeast, LatLng southwest) {
        mFirebaseAnalytics.logEvent("Route_Created_Successfully", null);
        LatLngBounds grenzen = new LatLngBounds(southwest, northeast);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(grenzen, 100));
        listHandler.sendEmptyMessageDelayed(11, 3000);
    }

    private void appRateDialog() {
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", 0);
        if (BuildConfig.debug)
            Log.i("RateDialog", "User denied to show again: " + prefs.getBoolean("neverShowRatingAgain", false));
        if (prefs.getBoolean("neverShowRatingAgain", false)) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit();
        // Increment launch counter
        int appLaunchCounter = prefs.getInt("appLaunchCounter", 0) + 1;
        editor.putInt("appLaunchCounter", appLaunchCounter);
        if (BuildConfig.debug)
            Log.i("RateDialog", "Launch-Count: " + appLaunchCounter);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }
        // Wait at least n days before opening
        if (appLaunchCounter >= Config.LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch + (Config.DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                // RatingFragment
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                try {
                    map.getUiSettings().setAllGesturesEnabled(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ratingFragment = new RatingFragment();
                fragmentTransaction.add(R.id.googlemap_actvity_layout, ratingFragment).commitAllowingStateLoss();
            }
        }
        editor.apply();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult arg0) {
    }

    // at Touch on ProgressBar
    public void abortGPS(final View view) {
        // Abort GPS was pressed (ProgressBar was pressed)
        try {
            mFirebaseAnalytics.logEvent("User_Canceled_GPS", null);
            mLocationer.deactivateLocationer();
            ProgressBar mProgressBar = findViewById(R.id.progressBar1);
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.GONE);
            }
        } catch (Exception e) {
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
            mFirebaseAnalytics.logEvent("GPS_Dialog_shown", null);
            Button cancel = dialogGPS.findViewById(R.id.dialogCancelgps);
            cancel.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    mFirebaseAnalytics.logEvent("GPS_Dialog_canceled", null);
                    dialogGPS.dismiss();
                }
            });

            Button settingsGPS = dialogGPS.findViewById(R.id.dialogSettingsgps);
            settingsGPS.setOnClickListener(new OnClickListener() {
                public void onClick(View arg0) {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        userSwitchedGps = true;
                    } catch (android.content.ActivityNotFoundException ae) {
                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                        userSwitchedGps = true;
                    }
                    mFirebaseAnalytics.logEvent("GPS_Dialog_JumpToSettings", null);
                    dialogGPS.dismiss();
                }
            });
        }
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
            TextView mapText = findViewById(R.id.mapText);
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

    private class routeTask extends AsyncTask<Void, Void, Void> {

        private String endAddress;
        private JSONArray stepsArray;
        private LatLng northeastLatLng;
        private LatLng southwestLatLng;
        private boolean getPathSuccess;
        private String firstDistance;


        private void getPath(LatLng src, LatLng dest) {
            waitedAtStart = false;
            counterRouteComplexity = phases = segmentCounter = 0;

            HttpRequests httpJSON = new HttpRequests();
            httpJSON.setURL("https://maps.googleapis.com/maps/api/directions/json");
            httpJSON.setMethod("GET");
            httpJSON.addValue("origin", src.latitude + "," + src.longitude);
            httpJSON.addValue("destination", dest.latitude + "," + dest.longitude);
            httpJSON.addValue("sensor", "true");
            httpJSON.addValue("key", Config.DIRECTIONS_API_KEY);
            httpJSON.addValue("mode", "walking");
            httpJSON.addValue("language", language);
            String response = httpJSON.doRequest();
            Log.i("routeTask Response", response);
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

                endAddress = legsObject.getString("end_address");

                String[] zielOrtArray;
                zielOrtArray = endAddress.split(",", 3);
                try {
                    endAddress = zielOrtArray[0];
                    endAddress += "\n" + zielOrtArray[1];
                } catch (Exception e) {
                    // thats possible if Destination Name is only 1 line
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
                e.printStackTrace();
                getPathSuccess = false;
            }

        }

        @Override
        protected Void doInBackground(Void... void2) {
            try {
                getPath(GoogleMap.startLatLng, GoogleMap.destLatLng);
            } catch (Exception e) {
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
                e.printStackTrace();
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