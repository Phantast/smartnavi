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
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.cardview.widget.CardView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
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
import com.ilm.sandwich.tools.PolylineDecoder;
import com.ilm.sandwich.tools.RouteManager;
import com.ilm.sandwich.tools.Locationer;
import com.ilm.sandwich.tools.MySupportMapFragment;
import com.ilm.sandwich.tools.TouchableWrapper;
import com.ilm.sandwich.ui.MapViewModel;

import androidx.lifecycle.ViewModelProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MapActivitiy for Google Maps
 *
 * @author Christian Henke
 * https://smartnavi.app
 */
public class GoogleMap extends AppCompatActivity implements Locationer.onLocationUpdateListener,
        OnMapReadyCallback, TutorialFragment.onTutorialFinishedListener, Core.onStepUpdateListener, RatingFragment.onRatingFinishedListener {

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
    private static Handler mainHandler = new Handler(android.os.Looper.getMainLooper());
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
    String language;
    TextToSpeech mTts;
    ProgressBar mProgressBar;
    int phases;
    int segmentCounter;
    TutorialFragment tutorialFragment;
    RatingFragment ratingFragment;
    private MapViewModel viewModel;
    private FirebaseAnalytics mFirebaseAnalytics;
    private com.google.android.gms.maps.GoogleMap map;
    private String[] html_instructions = new String[31];
    private String[] polylineArray = new String[31];
    private final ExecutorService routeExecutor = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<Intent> searchLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    if (place.getLatLng() != null) {
                        Log.i("SmartNavi Autocomplete", "Place: " + place.getName() + ", " + place.getLatLng());
                        destLatLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                        setDestPosition(destLatLng);
                        showRouteInfo();
                        fetchRouteAsync();
                        // Show selected destination in the search bar
                        TextView searchText = findViewById(R.id.searchBarText);
                        if (searchText != null && place.getName() != null) {
                            searchText.setText(place.getName());
                            searchText.setTextColor(0xFF333333);
                        }
                    } else {
                        Toast.makeText(this, getResources().getString(R.string.tx_103), Toast.LENGTH_LONG).show();
                    }
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR && result.getData() != null) {
                    Status status = Autocomplete.getStatusFromIntent(result.getData());
                    Log.i("SmartNavi Autocomplete", status.getStatusMessage());
                    Toast.makeText(this, getResources().getString(R.string.tx_103), Toast.LENGTH_LONG).show();
                }
            });
    private Locationer mLocationer;
    private boolean knownReasonForBreak = false;
    private boolean finishedTalking = false;
    private boolean listVisible = false;
    private Polyline[] completeRoute = new Polyline[31];
    private FloatingActionButton fab;

    public static double computeDistanz(double lat, double lon) {
        // Entfernung bzw. Distanz zur eigenen aktuellen Position
        double mittellat2 = Math.toRadians((Core.startLat + lat) / 2);
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

        // Set up floating search bar
        CardView searchBarCard = findViewById(R.id.searchBarCard);
        TextView searchBarText = findViewById(R.id.searchBarText);
        ImageButton searchBarOverflow = findViewById(R.id.searchBarOverflow);

        // Clicking the search bar or its text opens Places Autocomplete
        View.OnClickListener searchClickListener = v -> {
            knownReasonForBreak = true;
            autoCompleteSearch();
        };
        searchBarCard.setOnClickListener(searchClickListener);
        searchBarText.setOnClickListener(searchClickListener);

        // Overflow menu (3-dot icon) shows popup with all menu items
        searchBarOverflow.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(GoogleMap.this, v);
            popup.getMenu().add(0, 1, 0, getResources().getString(R.string.tx_64));  // Background Service
            popup.getMenu().add(0, 2, 1, getResources().getString(R.string.tx_90));  // Offline Maps
            popup.getMenu().add(0, 3, 2, getResources().getString(R.string.tx_15));  // Settings
            popup.getMenu().add(0, 4, 3, getResources().getString(R.string.tx_50));  // Tutorial
            popup.getMenu().add(0, 5, 4, getResources().getString(R.string.tx_65));  // About
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1:
                        knownReasonForBreak = true;
                        startActivity(new Intent(GoogleMap.this, BackgroundService.class));
                        return true;
                    case 2:
                        startActivity(new Intent(GoogleMap.this, Webview.class));
                        return true;
                    case 3:
                        knownReasonForBreak = true;
                        startActivity(new Intent(GoogleMap.this, Settings.class));
                        return true;
                    case 4:
                        FragmentManager fm = getSupportFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        map.getUiSettings().setAllGesturesEnabled(false);
                        tutorialFragment = new TutorialFragment();
                        ft.add(R.id.coordinatorRoot, tutorialFragment).commit();
                        // Hide search bar while tutorial is showing
                        CardView sb = findViewById(R.id.searchBarCard);
                        if (sb != null) sb.setVisibility(View.GONE);
                        return true;
                    case 5:
                        knownReasonForBreak = true;
                        startActivity(new Intent(GoogleMap.this, Info.class));
                        return true;
                }
                return false;
            });
            popup.show();
        });

        // Handle edge-to-edge insets: push search bar below the status bar
        ViewCompat.setOnApplyWindowInsetsListener(searchBarCard, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            int extraTopDp = 20;
            float density = getResources().getDisplayMetrics().density;
            lp.topMargin = insets.top + (int) (extraTopDp * density);
            v.setLayoutParams(lp);
            return windowInsets;
        });

        // Handle navigation bar insets for the FAB
        FloatingActionButton fabView = findViewById(R.id.fab);
        if (fabView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(fabView, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                lp.bottomMargin = insets.bottom + 16;
                v.setLayoutParams(lp);
                return windowInsets;
            });
        }

        // Handle navigation bar insets for the bottom info bar spacer
        View navBarSpacer = findViewById(R.id.mapTextNavBarSpacer);
        if (navBarSpacer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(navBarSpacer, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.height = insets.bottom;
                v.setLayoutParams(lp);
                return windowInsets;
            });
        }

        viewModel = new ViewModelProvider(this).get(MapViewModel.class);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (routeHasBeenDrawn) {
                    clearRoute();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        if (!BuildConfig.DEBUG) {
            mFirebaseRemoteConfig.fetch(1);
            mFirebaseRemoteConfig.activate();
        }
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.DEBUG ? 0 : 3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d("Firebase", "Config params updated: " + task.getResult());
            } else {
                Log.d("Firebase", "Fetch failed");
            }
        });

        fab = findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                try {
                    list = findViewById(R.id.liste);
                    if (list != null && list.getVisibility() == View.VISIBLE)
                        list.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setFollowOn();
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

        MySupportMapFragment mapFragment = (MySupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.googlemap_fragment);
        mapFragment.getMapAsync(this);
        if (mapFragment.mTouchView != null) {
            mapFragment.mTouchView.setOnMapTouchListener(() -> onMapTouch());
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
        map.setBuildingsEnabled(true);
        map.setOnMarkerClickListener(marker -> true); // suppress default marker info window

        map.setOnMapLongClickListener(arg0 -> {
            mFirebaseAnalytics.logEvent("Longpress_Map", null);
            longpressLocation = arg0;
            if (longPressMarker != null && longPressMarker.isVisible()) {
                longPressMarker.remove();
            }
            longPressMarker = map.addMarker(new MarkerOptions().position(longpressLocation).icon(BitmapDescriptorFactory.fromBitmap(drawableDest))
                    .anchor(0.5F, 1.0F));
            showLongPressList();
        });

        map.setOnMapClickListener(arg0 -> {
            if (listVisible) {
                longpressLocation = arg0;
                longPressMarker.remove();
                list.setVisibility(View.INVISIBLE);
                listVisible = false;
            }
        });

        drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);

        map.setIndoorEnabled(true);


        //Check if magnetic sensor is existing. If not: Warn user!
        try {
            SensorManager mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getName();
        } catch (Exception e) {
            TextView mapText = findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setText(getResources().getString(R.string.tx_43));
            }
            View container = findViewById(R.id.mapTextContainer);
            if (container != null) {
                container.setVisibility(View.VISIBLE);
            }
        }

        TextView mapText = findViewById(R.id.mapText);
        if (mapText != null) {
            mapText.setSingleLine(false);
        }
        View container = findViewById(R.id.mapTextContainer);
        if (container != null) {
            container.setVisibility(View.INVISIBLE);
        }

        language = Locale.getDefault().getLanguage();

        com.ilm.sandwich.tools.PreferencesHelper.putBoolean(this, "follow", true);
        SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        uid = settings.getString("uid", "0");
        if (uid.equalsIgnoreCase("0")) {
            String neuUID = "" + (1 + (int) (Math.random() * ((10000000 - 1) + 1)));
            com.ilm.sandwich.tools.PreferencesHelper.putString(this, "uid", neuUID);
            uid = settings.getString("uid", "0");
        }

        // Initialize ViewModel UI state from preferences
        viewModel.setSatelliteView(settings.getBoolean("view", false));
        viewModel.setSpeechOutput(settings.getBoolean("language", false));
        viewModel.setVibration(settings.getBoolean("vibration", true));
        viewModel.setAutocorrectEnabled(settings.getBoolean("autocorrect", false));

        // Observe satellite view changes
        viewModel.getSatelliteView().observe(this, isSatellite -> {
            satelliteView = isSatellite;
            if (map != null) {
                map.setMapType(isSatellite
                        ? com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID
                        : com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL);
            }
        });

        // Observe speech/vibration so instance fields stay in sync
        viewModel.getSpeechOutput().observe(this, value -> speechOutput = value);
        viewModel.getVibration().observe(this, value -> vibration = value);

        createAllMarkersInvisible();

        String stepLengthString = settings.getString("step_length", null);
        if (stepLengthString != null) {
            float parsed = com.ilm.sandwich.tools.StepLengthCalculator.calculateStepLength(stepLengthString);
            if (parsed > 0) {
                Core.stepLength = parsed;
                if (BuildConfig.DEBUG) Log.i("Step length", "Step length = " + Core.stepLength);
            }
        } else {
            map.getUiSettings().setAllGesturesEnabled(false);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            tutorialFragment = new TutorialFragment();
            fragmentTransaction.add(R.id.coordinatorRoot, tutorialFragment);
            fragmentTransaction.commitAllowingStateLoss();
            // Hide search bar while tutorial is showing
            CardView sb = findViewById(R.id.searchBarCard);
            if (sb != null) sb.setVisibility(View.GONE);
        }

        mTts = new TextToSpeech(GoogleMap.this, null);
        mTts.setLanguage(Locale.getDefault());

        // onLongPress Auswahl-Liste
        list = findViewById(R.id.liste);
        if (list != null) {
            list.setVisibility(View.INVISIBLE);
        }
        list.setOnItemClickListener((arg0, arg1, arg2, arg3) -> {
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

    // --- Public methods replacing the old listHandler message bus ---

    /** Show the long-press context list (was msg 2) */
    public void showLongPressList() {
        list.setVisibility(View.VISIBLE);
        mainHandler.postDelayed(() -> listVisible = true, 1200);
    }

    /** Enable autocorrect (was msg 6). Safe to call from other activities with a delay. */
    public static void enableAutocorrectDelayed(long delayMs) {
        mainHandler.postDelayed(() -> {
            if (mCore != null) mCore.enableAutocorrect();
        }, delayMs);
    }

    /** Disable autocorrect (was msg 7) */
    public static void disableAutocorrect() {
        mainHandler.post(() -> {
            if (mCore != null) mCore.disableAutocorrect();
        });
    }

    /** Reactivate sensors for background service (was msg 10) */
    public static void reactivateSensorsForBackgroundService() {
        if (mCore != null) {
            mCore.reactivateSensors();
            if (BuildConfig.DEBUG)
                Log.i("SmartNavi", "Reactivate Sensors because Background service is running.");
        }
    }

    /** Reset camera north and restart follow mode after route animation (was msg 11) */
    private void resetCameraAndFollow() {
        setFollowOn();
        startLatLng = new LatLng(Core.startLat, Core.startLon);
        CameraPosition currentPlace = new CameraPosition.Builder().target(startLatLng).bearing(0.0F).tilt(0.0F).zoom(19).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
        mainHandler.postDelayed(() -> followMe = true, 1500);
    }

    /** Cancel all pending handler callbacks */
    public static void cancelPendingCallbacks() {
        mainHandler.removeCallbacksAndMessages(null);
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
                enableAutocorrectDelayed(0);
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
                cancelPendingCallbacks();
                map.stopAnimation();
                setPosition(false);
                drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
                setDestPosition(destLatLng);
                showRouteInfo();
                fetchRouteAsync();
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
        // Hide the previous marker before showing the new one to avoid duplicates
        if (actualMarker[0] != null && actualMarker[0] != currentPosition) {
            actualMarker[0].setVisible(false);
        }
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
            TextView mapText = findViewById(R.id.mapText);
            if (mapText != null) {
                mapText.setText(getResources().getString(R.string.tx_06));
            }
            View mapContainer = findViewById(R.id.mapTextContainer);
            if (mapContainer != null) {
                mapContainer.setVisibility(View.VISIBLE);
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
        List<double[]> decoded = PolylineDecoder.decode(encoded);
        List<LatLng> poly = new ArrayList<>(decoded.size());
        for (double[] coord : decoded) {
            poly.add(new LatLng(coord[0], coord[1]));
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
            mainHandler.postDelayed(() -> finishedTalking = true, 10000);
        }
    }

    public void showRouteInfo() {
        TextView mapText = findViewById(R.id.mapText);
        View mapContainer = findViewById(R.id.mapTextContainer);
        if (mapText != null && mapContainer != null) {
            mapText.setText(getApplicationContext().getResources().getString(R.string.tx_04));
            mapContainer.setVisibility(View.VISIBLE);
        }
    }

    public void makeInfo(String endAddress, String firstDistance) {
        TextView mapText = findViewById(R.id.mapText);
        View mapContainer = findViewById(R.id.mapTextContainer);
        if (firstDistance != null && mapText != null && mapContainer != null) {
            mapText.setText(endAddress + "\n\n" + firstDistance);
            mapContainer.setVisibility(View.VISIBLE);
            if (speechOutput) {
                firstDistance = firstDistance.replace("-", " ").replace("\n", " ").replace("/", " ");
                mTts.speak(firstDistance, TextToSpeech.QUEUE_FLUSH, null, null);
            }
            mainHandler.postDelayed(() -> {
                finishedTalking = true;
                waitedAtStart = true;
            }, 8500);
            uTaskIsOn = true;
        } else {
            Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_30), Toast.LENGTH_LONG).show();
            if (mapContainer != null) {
                mapContainer.setVisibility(View.INVISIBLE);
            }
        }
    }


    @Override
    protected void onResume() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int status = api.isGooglePlayServicesAvailable(this);

        if (status != ConnectionResult.SUCCESS) {
            com.ilm.sandwich.tools.PreferencesHelper.putString(this, "MapSource", "MapQuestOSM");
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
            // Reload preferences into ViewModel — observers handle the rest
            viewModel.setSatelliteView(settings.getBoolean("view", false));
            viewModel.setSpeechOutput(settings.getBoolean("language", false));
            viewModel.setVibration(settings.getBoolean("vibration", true));
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
        // Keep sensors alive when background service is active — it needs step detection
        if (mCore != null && !Config.backgroundServiceActive) {
            mCore.pauseSensors();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        routeHasBeenDrawn = false;
        try {
            cancelPendingCallbacks();
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
            // Only shutdown Core if background service is NOT active.
            // If it is active, ForegroundService.onDestroy handles cleanup.
            if (mCore != null && !Config.backgroundServiceActive) {
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
        routeExecutor.shutdownNow();
        com.ilm.sandwich.tools.DebugLogHelper.dumpAndNotify(this);
        super.onDestroy();
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

    private void autoCompleteSearch() {
        //Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }
        // Set the fields to specify which types of place data to return.
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS);
        // Bias results towards ~50km around current position, but allow results anywhere
        double latOffset = 0.45; // ~50km
        double lonOffset = 0.6;  // ~50km (adjusted for longitude convergence)
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(Core.startLat - latOffset, Core.startLon - lonOffset),
                new LatLng(Core.startLat + latOffset, Core.startLon + lonOffset));
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields)
                .setLocationBias(bounds)
                .build(this);

        searchLauncher.launch(intent);
    }

    public void fingerDestination(LatLng g) {
        destLat = g.latitude;// getLatitudeE6() / 1E6;
        destLon = g.longitude;// getLongitudeE6() / 1E6;
        destLatLng = g;
        cancelPendingCallbacks();
        showRouteInfo();
        map.stopAnimation();
        setPosition(false);
        drawableDest = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
        setDestPosition(destLatLng);
        fetchRouteAsync();
    }

    public void setDestPosition(LatLng z) {
        destMarker.setPosition(z);
        destMarker.setVisible(true);
    }

    public void routeStartAnimation(LatLng northeast, LatLng southwest) {
        mFirebaseAnalytics.logEvent("Route_Created_Successfully", null);
        LatLngBounds grenzen = new LatLngBounds(southwest, northeast);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(grenzen, 100));
        mainHandler.postDelayed(() -> resetCameraAndFollow(), 3000);
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
                fragmentTransaction.add(R.id.coordinatorRoot, ratingFragment).commitAllowingStateLoss();
            }
        }
        editor.apply();
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
            cancel.setOnClickListener(v -> {
                mFirebaseAnalytics.logEvent("GPS_Dialog_canceled", null);
                dialogGPS.dismiss();
            });

            Button settingsGPS = dialogGPS.findViewById(R.id.dialogSettingsgps);
            settingsGPS.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    userSwitchedGps = true;
                } catch (android.content.ActivityNotFoundException ae) {
                    startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    userSwitchedGps = true;
                }
                mFirebaseAnalytics.logEvent("GPS_Dialog_JumpToSettings", null);
                dialogGPS.dismiss();
            });
        }
    }

    private void clearRoute() {
        if (destMarker != null && destMarker.isVisible()) {
            destMarker.setVisible(false);
        }
        for (int a = 0; a <= routeParts; a++) {
            completeRoute[a].remove();
        }
        routeHasBeenDrawn = false;
        setFollowOn();
        View mapContainer = findViewById(R.id.mapTextContainer);
        if (mapContainer != null) {
            mapContainer.setVisibility(View.INVISIBLE);
        }
        // Reset search bar text back to placeholder
        TextView searchText = findViewById(R.id.searchBarText);
        if (searchText != null) {
            searchText.setText(getResources().getString(R.string.tx_search_here));
            searchText.setTextColor(0xFF666666);
        }
    }

    @Override
    public void onTutorialFinished() {
        map.getUiSettings().setAllGesturesEnabled(true);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(tutorialFragment).commit();
        // Show search bar again
        CardView sb = findViewById(R.id.searchBarCard);
        if (sb != null) sb.setVisibility(View.VISIBLE);
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

    private void fetchRouteAsync() {
        final LatLng src = GoogleMap.startLatLng;
        final LatLng dest = GoogleMap.destLatLng;
        final String lang = language;
        routeExecutor.execute(() -> {
            RouteManager.RouteResult result;
            try {
                result = RouteManager.fetchRoute(src, dest, lang);
            } catch (Exception e) {
                e.printStackTrace();
                result = RouteManager.RouteResult.failure();
            }
            final RouteManager.RouteResult finalResult = result;
            runOnUiThread(() -> onRouteFetched(finalResult));
        });
    }

    private void onRouteFetched(RouteManager.RouteResult result) {
        waitedAtStart = false;
        counterRouteComplexity = phases = segmentCounter = 0;

        if (result.success) {
            phases = result.phases;
            drawPath(result.stepsArray);
            followMe = false;
            routeStartAnimation(result.northeastBound, result.southwestBound);
        }
        makeInfo(result.endAddress, result.distanceInfo);
    }

    private void drawPath(JSONArray stepsArray) {
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