package com.ilm.sandwich;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.actionbarsherlock.widget.SearchView.OnSuggestionListener;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
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
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.ilm.sandwich.TouchableWrapper.UpdateMapAfterUserInterection;
import com.ilm.sandwich.helferklassen.HttpRequests;
import com.ilm.sandwich.helferklassen.Locationer;
import com.ilm.sandwich.helferklassen.PlacesAutoComplete;
import com.ilm.sandwich.helferklassen.PlacesTextSearch;
import com.ilm.sandwich.helferklassen.Rechnung;
import com.ilm.sandwich.helferklassen.SuggestionsAdapter;

public class GoogleMapActivity extends SherlockFragmentActivity implements SensorEventListener, UpdateMapAfterUserInterection,
		GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener {

	public static GoogleMap map;
	private static long verstrZeit;
	public static double zielLat;
	public static double zielLon;
	private static int schrittZaehlerAlt = 1;
	public static boolean followMe;
	public static boolean vibration;
	public static boolean satelliteView;
	public double[] gp2Latk = new double[31];
	public double[] gp2Lonk = new double[31];
	public boolean anfangAbgewartet = false;
	public static boolean ersteRouteFertig = false;
	private static int jetzt = 0;
	private static long startTime;
	private static int geoVersuch = 0;
	Drawable ic_menu_mylocation;
	Drawable ic_menu_mylocation_off;
	static Bitmap drawableStart;
	public static Bitmap drawableZiel;
	public static Rechnung mRechnung;
	static String stepLengthVorher;
	String sprache;
	TextToSpeech mTts;
	public int counterRoutenKomplexitaet = 0;
	int phasen;
	int stationsCounter;
	static SensorManager mSensorManager;
	private String[] html_instructions = new String[31];
	private String[] polylineArray = new String[31];
	public boolean sprachAusgabe;
	static boolean uTaskIstAn;
	public static long kartenStartzeit;
	public static long kartenEndzeit;
	public static int maxAccelFreq = 0;
	public static int minAccelFreq = 1000;
	public static int maxMagnFreq = 0;
	public static int minMagnFreq = 1000;
	public static int durchAccelFreq = 0;
	public static int durchMagnFreq = 0;
	public static int durchlauf = 1;
	public static String kartenNutzung;
	public static String serviceNutzung;
	public static String backgroundGesehen = "false";
	public static String exportVerwendet = "false";
	private SubMenu subMenu1;
	private static Menu mainMenu;
	private int magnEinheiten;
	private int accelEinheiten;
	private static LatLng startLatLng;
	private static LatLng zielLatLng;
	private static float altesZoomLevel;
	public static int schrittSpeicher = 0;
	static DecimalFormat df = new DecimalFormat("0.0");
	static DecimalFormat df0 = new DecimalFormat("0");
	public static int einheiten = 0;
	private Locationer mLocationer;
	public static boolean hinterGrundSollWiederAn = false;
	public static String magnetName;
	public static String accelName;
	public static String gyroName;
	public static String deviceName;
	public static int androidVersion;
	private boolean bekannterPauseGrund = false;
	public static String modelName;
	public static String productName;
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
	private static Marker[] aktuellerMarker = new Marker[1];
	public static String uid;
	public static boolean infoGesehen = false;
	public static boolean gpsErfolgreich = false;
	public static boolean gpsAktiv = true;
	public static boolean fremdAufruf = false;
	public static boolean userHatManuellGesetzt = false;
	public static boolean tutorialAufgerufen = false;
	public static SearchView searchView;
	static LatLng longpressLocation;
	static ListView liste;
	public static Handler listHandler;
	View tutorialOverlay;
	public boolean metrischeAngaben = true;
	private boolean zuEndeGesprochen = false;
	private int autoCorrectFaktor = 1;
	private boolean autoCorrect = false;
	private boolean warteSchonAufAutoCorrect = false;
	private int zuWartendeSchritte = 0;
	private boolean listeIstVisible = false;
	public static SuggestionsAdapter mSuggestionsAdapter;
	public Context sbContext;
	public static boolean suggestionsInProgress = false;
	public static MatrixCursor cursor = new MatrixCursor(Config.COLUMNS);
	public static Handler changeSuggestionAdapter;
	private Polyline[] kompletteRoute = new Polyline[31];
	private static boolean hellerPunkt;
	private static boolean egoPerspektive = false;
	private static boolean touchHatFollowAbgeschaltet = false;
	private static int compassStatus = 1;
	private static Marker zielMarker;
	private static boolean routeIstGezeichnet = false;
	private static int routenStuecke = 0;
	private static Marker longPressMarker;
	private static boolean userSchaltetGPS = false;
	private final long POS_UPDATE_FREQ = 500;

	// ------------------------------------------------------------------------------------------------------------------
	// ------ON
	// CREATE---------------------------------------------------------------------------------------------------
	// ------------------------------------------------------------------------------------------------------------------

	@SuppressLint({ "HandlerLeak", "NewApi" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);

		Config.usingGoogleMaps = true;
		mLocationer = new Locationer(this);

		getSherlock().getActionBar().show();
		setContentView(R.layout.googlemap_layout);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		androidVersion = android.os.Build.VERSION.SDK_INT;
		modelName = android.os.Build.MODEL;
		deviceName = android.os.Build.DEVICE;
		productName = android.os.Build.PRODUCT;

		// Rate App show for debugging
		// showRateDialog();
		// Rate App live
		appRateDialog();

		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if (status != ConnectionResult.SUCCESS) {
			GooglePlayServicesUtil.getErrorDialog(status, this, 2, new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					startActivity(new Intent(GoogleMapActivity.this, OsmMapActivity.class));
					finish();
				}
			}).show();
		} else {

			map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
			map.setMyLocationEnabled(false);
			map.setIndoorEnabled(true);
			map.getUiSettings().setCompassEnabled(false);

			map.setOnMapLongClickListener(new OnMapLongClickListener() {
				@Override
				public void onMapLongClick(LatLng arg0) {
					longpressLocation = arg0;
					try {
						if (longPressMarker.isVisible()) {
							longPressMarker.remove();
						}
					} catch (Exception e) {
						if (Config.debugModus)
							e.printStackTrace();
					}
					longPressMarker = map.addMarker(new MarkerOptions().position(longpressLocation).icon(BitmapDescriptorFactory.fromBitmap(drawableZiel))
							.anchor(0.5F, 1.0F));
					listHandler.sendEmptyMessage(2);
				}
			});

			map.setOnMapClickListener(new OnMapClickListener() {
				@Override
				public void onMapClick(LatLng arg0) {
					if (listeIstVisible == true) {
						longpressLocation = arg0;
						longPressMarker.remove();
						liste.setVisibility(View.INVISIBLE);
						listeIstVisible = false;
					}
				}
			});

			drawableZiel = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);

			map.setIndoorEnabled(true);

			mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

			// Create the search view
			searchView = new SearchView(getSupportActionBar().getThemedContext());
			searchView.setQueryHint(getApplicationContext().getResources().getString(R.string.tx_02));
			// get static themed context for autocomplete update
			sbContext = getSupportActionBar().getThemedContext();

			// add adapter to search view
			searchView.setSuggestionsAdapter(mSuggestionsAdapter);

			// bei onClick wird das longPressMenu geschlossen falls offen
			searchView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// Falls off, wird das longPressMenu invisible gemacht
					try {
						liste = (ListView) findViewById(R.id.liste);
						liste.setVisibility(View.INVISIBLE);
					} catch (Exception e) {
						if (Config.debugModus)
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
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

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
						Toast.makeText(GoogleMapActivity.this, "You can not find Chuck Norris. Chuck Norris finds YOU!", Toast.LENGTH_LONG).show();
					else if (query.equalsIgnoreCase("cake") || query.equalsIgnoreCase("the cake") || query.equalsIgnoreCase("portal"))
						Toast.makeText(GoogleMapActivity.this, "The cake is a lie!", Toast.LENGTH_LONG).show();
					else if (query.equalsIgnoreCase("tomlernt")) {
						// start debug mode
						Toast.makeText(GoogleMapActivity.this, "Debug-Mode ON", Toast.LENGTH_SHORT).show();
						Config.debugModus = true;
					} else if (query.equalsIgnoreCase("rateme")) {
						// show app rate dialog
						showRateDialog();
					} else if (query.equalsIgnoreCase("smartnavihelp")) {
						// User ID anzeigen
						SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
						uid = settings.getString("uid", "0");
						View viewLine = (View) findViewById(R.id.view156);
						viewLine.setVisibility(0);
						TextView mapText = (TextView) findViewById(R.id.mapText);
						mapText.setVisibility(0);
						mapText.setText("Random User ID: " + uid);
					}
					// search coordinates for autocomplete result
					else if (isOnline()) {
						if (Config.PLACES_API_FALLBACK < 2)
							new PlacesTextSeachAsync().execute(query);
						else
							new geocodeTask().execute(query);
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
						searchView.setSuggestionsAdapter(GoogleMapActivity.mSuggestionsAdapter);
						// important to update suggestion list
						searchView.getSuggestionsAdapter().notifyDataSetChanged();
						suggestionsInProgress = false;
					}
					super.handleMessage(msg);
				}
			};

			handlerAnwerfen();

			try {
				magnetName = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getName();
			} catch (Exception e) {
				View viewLine = (View) findViewById(R.id.view156);
				viewLine.setVisibility(0);
				TextView mapText = (TextView) findViewById(R.id.mapText);
				mapText.setVisibility(0);
				mapText.setText(getResources().getString(R.string.tx_43));
			}
			try {
				accelName = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).getName();
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
			gyroName = "nicht vorhanden";
			try {
				gyroName = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).getName();
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}

			mRechnung = new Rechnung();

			kartenStartzeit = System.currentTimeMillis();

			// if offline, Toast Message will appear automatically
			isOnline();

			TextView mapText = (TextView) findViewById(R.id.mapText);
			mapText.setVisibility(4);
			mapText.setSingleLine(false);
			View viewLine = (View) findViewById(R.id.view156);
			viewLine.setVisibility(4);

			sprache = Locale.getDefault().getLanguage();

			new schreibeSettings("follow", true).execute();
			SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
			
			uid = settings.getString("uid", "0");
			if (uid.equalsIgnoreCase("0")) {
				String neuUID = "" + (1 + (int) (Math.random() * ((10000000 - 1) + 1)));
				new schreibeSettings("uid", neuUID).execute();
				uid = settings.getString("uid", "0");
			}
			// Log.d("Location-Status" , "UID = "+uid);
			satelliteView = settings.getBoolean("view", false);
			if (satelliteView == true) {
				map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			} else {
				map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			}

			try {
				new anfrageTask().execute();
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}

			alleMarkerErstellenInvisible();

			String schrittLaengeString = settings.getString("step_length", null);
			if (schrittLaengeString != null) {
				try {
					schrittLaengeString = schrittLaengeString.replace(",", ".");
					Float gespeicherteGroesse = (Float.parseFloat(schrittLaengeString));
					if (gespeicherteGroesse < 241 && gespeicherteGroesse > 119) {
						Rechnung.schrittLaenge = gespeicherteGroesse / 222;
					} else if (gespeicherteGroesse < 95 && gespeicherteGroesse > 45) {
						Rechnung.schrittLaenge = (float) (gespeicherteGroesse * 2.54 / 222);
					}
				} catch (NumberFormatException e) {
					if (Config.debugModus)
						e.printStackTrace();
				}

				// Tutorial wurde schon einmal abgeschlossen, jedoch
				// wurde longPressDialog nie angezeigt
				// also auch bitte einmal anzeigen
				boolean longPressWurdeGezeigt = settings.getBoolean("longPressWasShown", false);
				if (longPressWurdeGezeigt == false) {
					showLongPressDialog();
				}
			} else {
				tutorialKram();
			}
			// Compass nach unten setzen
			listHandler.sendEmptyMessageDelayed(1, 10);

			mTts = new TextToSpeech(GoogleMapActivity.this, null);
			mTts.setLanguage(Locale.getDefault());

			// onLongPress Auswahl-Liste
			liste = (ListView) findViewById(R.id.liste);
			liste.setVisibility(View.INVISIBLE);
			liste.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					if (arg2 == 0) {
						setHome();
						liste.setVisibility(View.INVISIBLE);
						listeIstVisible = false;
						longPressMarker.remove();
					} else {
						fingerDestination(longpressLocation);
						liste.setVisibility(View.INVISIBLE);
						listeIstVisible = false;
						longPressMarker.remove();
					}
				}
			});
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// ------ENDE
	// ONCREATE-----------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------------------

	@SuppressLint("HandlerLeak")
	public void handlerAnwerfen() {
		listHandler = new Handler() {
			public void handleMessage(Message msg) {
				if (msg.what == 0) {
					// First Position from the Locationer
					startLatLng = new LatLng(Rechnung.startLat, Rechnung.startLon);
					setErstePosition();
					restartListener();
					fremdIntent();
					// starte Autocorrect falls gewünscht
					listHandler.sendEmptyMessage(6);
					ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
					mProgressBar.setVisibility(View.VISIBLE);

				} else if (msg.what == 1) {
					// Compass passendes margin setzen, abhängig von Höhe Actionbar
					int height = getSherlock().getActionBar().getHeight();
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
					liste.setVisibility(View.VISIBLE);
					// Erst nach einiger Zeit die Variable setzen
					// damit die Liste nicht durch minimale Touch-Bewegung
					// verschwindet, sondern eine minimale Existenzzeit hat.
					Handler handler = new Handler();
					handler.postDelayed(new Runnable() {
						public void run() {
							listeIstVisible = true;
						}
					}, 1200);
				} else if (msg.what == 3) {
					finish(); // used by Settings to change to OsmMapActivity
				} else if (msg.what == 4) {
					listHandler.removeMessages(4);
					positionsUpdate();
					// Log.d("Location-Status","Positionstask EINGESCHALTET (listHandler 4)");
				} else if (msg.what == 5) {
					final Dialog dialog = new Dialog(GoogleMapActivity.this);
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
					// initialize Autocorrect oder starte neu durch
					// settings-änderung falls nötig
					SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
					autoCorrect = settings.getBoolean("autocorrect", false);
					// Erst gucken ob wirklich Autocorrect an soll wichtig, weil
					// beendeLocationer() sich darauf verlässt ;)
					if (autoCorrect) {
						int i = settings.getInt("gpstimer", 1);
						if (i == 0) { // am meisten Akku sparen, also hoher
										// Faktor
							autoCorrectFaktor = 4;
						} else if (i == 1) { // ausgewogen
							autoCorrectFaktor = 2;
						} else if (i == 2) { // hohe Genauigkeit
							autoCorrectFaktor = 1;
						}
						warteSchonAufAutoCorrect = false;
					}
				} else if (msg.what == 7) {
					autoCorrect = false;
					mLocationer.beendeAutocorrect();
				} else if (msg.what == 8) {
					Rechnung.setLocation(Locationer.startLat, Locationer.startLon);
					mLocationer.beendeAutocorrect();
					if (hinterGrundSollWiederAn == true) {
						Config.hintergrundAn = true;
						Smartgeo.reaktiviereFakeProvider();
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
						// "Positionstask AUS     listHandler 11");
					} catch (Exception e) {
						if (Config.debugModus)
							e.printStackTrace();
					}
					startLatLng = new LatLng(Rechnung.startLat, Rechnung.startLon);
					if (egoPerspektive) {
						// Kamera in Kompass-Richtung drehen wenn gewünscht
						CameraPosition currentPlace = new CameraPosition.Builder().target(startLatLng).bearing((float) Rechnung.azimuth).tilt(65.5f).zoom(19)
								.build();
						map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
					} else {
						// Kamera in nach Norden ausrichten
						CameraPosition currentPlace = new CameraPosition.Builder().target(startLatLng).bearing(0.0F).tilt(0.0F).zoom(19).build();
						map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
					}
					// Positionstask wieder anwerfen
					listHandler.sendEmptyMessageDelayed(4, 1500);
					// Log.d("Location-Status", "ListeHandler 11  ruft auf");
				} else if (msg.what == 12) {
					// message from Locationer
					ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
					mProgressBar.setVisibility(View.GONE);
				} else if (msg.what == 13) {
					showGPSDialog();
				}
				super.handleMessage(msg);
			}
		};
	}

	public void alleMarkerErstellenInvisible() {
		LatLng versteckAmNordpol = new LatLng(90.0D, 0.0D);

		zielMarker = map.addMarker(new MarkerOptions().position(versteckAmNordpol).icon(BitmapDescriptorFactory.fromBitmap(drawableZiel)));
		zielMarker.setVisible(false);

		current_position = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position)).anchor(0.5f, 0.5f));
		current_position.setVisible(false);

		current_position_anim = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim)).anchor(0.5f, 0.5f));
		current_position_anim.setVisible(false);

		current_position_ohne = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_ohne)).anchor(0.5f, 0.5f));
		current_position_ohne.setVisible(false);

		current_position_anim_ohne = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_ohne)).anchor(0.5f, 0.5f));
		current_position_anim_ohne.setVisible(false);

		current_position_k1 = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_k1)).anchor(0.5f, 0.5f));
		current_position_k1.setVisible(false);

		current_position_anim_k1 = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_k1)).anchor(0.5f, 0.5f));
		current_position_anim_k1.setVisible(false);

		current_position_g1 = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_g1)).anchor(0.5f, 0.5f));
		current_position_g1.setVisible(false);

		current_position_anim_g1 = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_anim_g1)).anchor(0.5f, 0.5f));
		current_position_anim_g1.setVisible(false);

		current_position_g2 = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_maps_indicator_current_position_g2)).anchor(0.5f, 0.5f));
		current_position_g2.setVisible(false);

		current_position_anim_g2 = map.addMarker(new MarkerOptions().position(versteckAmNordpol)
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

	public void fremdIntent() {
		try {
			String[] aufrufArray1 = { "", "", "" };
			String[] aufrufArray2 = { "", "", "" };

			String aufruf = this.getIntent().getDataString();
			if (aufruf != null) {
				if (aufruf.contains("google.navigation")) {
					aufrufArray1 = aufruf.split("&q=");
					aufrufArray1 = aufrufArray1[0].split("%2C");
					zielLon = Float.parseFloat(aufrufArray1[1]);
					aufrufArray2 = aufrufArray1[0].split("ll=");
					zielLat = Float.parseFloat(aufrufArray2[1]);
				} else if (aufruf.contains("http://maps.google")) {
					String[] aufrufArray3 = { "", "", "" };
					String[] aufrufArray4 = { "", "", "" };

					if (aufruf.contains("?saddr=")) {
						// Variante 1:
						// "http://maps.google.com/maps?saddr=50.685053,10.910772&daddr=50.689308,10.932552";
						aufrufArray1 = aufruf.split("saddr=");
						aufrufArray2 = aufrufArray1[1].split("&daddr=");

						aufrufArray3 = aufrufArray2[0].split(",");
						Rechnung.startLat = Float.parseFloat(aufrufArray3[0]);
						Rechnung.startLon = Float.parseFloat(aufrufArray3[1]);

						aufrufArray4 = aufrufArray2[1].split(",");
						zielLat = Float.parseFloat(aufrufArray4[0]);
						zielLon = Float.parseFloat(aufrufArray4[1]);
						// Locationer ausmachen, weil StartPosition ja mitgegeben wurde
						try {
							mLocationer.deactivateLocationer();
						} catch (Exception e) {
							if (Config.debugModus)
								e.printStackTrace();
						}
					} else if (aufruf.contains("?daddr=")) {
						// Variante 2 :
						// "http://maps.google.com/maps?daddr=50.685053,10.910772&saddr=50.689308,10.932552";
						aufrufArray1 = aufruf.split("daddr=");
						if (aufruf.contains("saddr=")) {
							aufrufArray2 = aufrufArray1[1].split("&saddr=");

							aufrufArray3 = aufrufArray2[0].split(",");
							zielLat = Float.parseFloat(aufrufArray3[0]);
							zielLon = Float.parseFloat(aufrufArray3[1]);

							aufrufArray4 = aufrufArray2[1].split(",");
							Rechnung.startLat = Float.parseFloat(aufrufArray4[0]);
							Rechnung.startLon = Float.parseFloat(aufrufArray4[1]);
							// Locationer ausmachen, weil StartPosition ja mitgegeben wurde
							try {
								mLocationer.deactivateLocationer();
							} catch (Exception e) {
								if (Config.debugModus)
									e.printStackTrace();
							}
						} else {
							// Angenommen es wurde ?daddr mitgesendet, aber keine
							// startadresse, dann gehts nur mit daddr weiter
							// Variante 3 :
							// "http://maps.google.com/maps?daddr=50.685053,10.910772"

							aufrufArray3 = aufrufArray1[1].split(",");
							zielLat = Float.parseFloat(aufrufArray3[0]);
							zielLon = Float.parseFloat(aufrufArray3[1]);
						}
					}
				} else {
					// String aufruf = "geo:50.6815558821102,10.932855606079102";
					aufrufArray1 = aufruf.split(",");
					aufrufArray2 = aufrufArray1[0].split(":");
					zielLat = Float.parseFloat(aufrufArray2[1]);
					zielLon = Float.parseFloat(aufrufArray1[1]);
				}
				zielLatLng = new LatLng(zielLat, zielLon);
				fremdAufruf = true; // Für Nutzungsstatistik
				listHandler.removeCallbacksAndMessages(null);
				listHandler.removeMessages(4);
				// Log.d("Location-Status","Positionstask AUS    weil fremdIntent");
				map.stopAnimation();
				setPosition(false);
				drawableZiel = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
				setZielPos(zielLatLng);
				getRouteInfo();
				new routenTask().execute("zielortSollRoutenTaskSelbstRausfinden");
			}
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
	}

	public void setFollowOn() {
		ImageView compass = (ImageView) findViewById(R.id.nadel);
		followMe = true;
		if (compassStatus == 2) {
			// setze auf Status 1
			compass.setImageResource(R.drawable.needle);
			compassStatus = 1;
		} else if (compassStatus == 4) {
			// setze Status auf 3
			compass.setImageResource(R.drawable.needle3);
			compassStatus = 3;
		}
		schrittZaehlerAlt = schrittZaehlerAlt - 1;
	}

	public void setFollowOff() {
		ImageView compass = (ImageView) findViewById(R.id.nadel);
		followMe = false;
		if (compassStatus == 1) {
			// setze auf Status 2
			compass.setImageResource(R.drawable.needle2);
			compassStatus = 2;
		} else if (compassStatus == 3) {
			// setze Status auf 4
			compass.setImageResource(R.drawable.needle4);
			compassStatus = 4;
		}
	}

	public void setHome() {
		Rechnung.startLat = longpressLocation.latitude;
		Rechnung.startLon = longpressLocation.longitude;
		Rechnung.schrittZaehler++;

		aktuellerMarker[0].setPosition(longpressLocation);

		ImageView compass = (ImageView) findViewById(R.id.nadel);
		LatLng newPos = new LatLng(Rechnung.startLat, Rechnung.startLon);
		listHandler.removeMessages(4);
		// Log.d("Location-Status", "Positionstask AUS      setHome");
		if (compassStatus == 4) {
			// status auf 3
			compass.setImageResource(R.drawable.needle3);
			compassStatus = 3;
			followMe = true;
			map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
		} else if (compassStatus == 2) {
			// status auf 1
			compass.setImageResource(R.drawable.needle);
			followMe = true;
			compassStatus = 1;
			map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
		}
		// Positionstask wieder anwerfen
		listHandler.sendEmptyMessageDelayed(4, 1500);
		// Wichtig für locationer, damit der das berücksichtigen kann
		userHatManuellGesetzt = true;
	}

	public static double neueDistanz(double lat, double lon) {
		// Entfernung bzw. Distanz zur eigenen aktuellen Position
		double mittellat2 = (Rechnung.startLat + lat) / 2 * 0.01745329252;
		double abstandLaengengrade = 111.3 * Math.cos(mittellat2);
		double dlat2 = 111.3 * (Rechnung.startLat - lat);
		double dlon2 = abstandLaengengrade * (Rechnung.startLon - lon);
		double entfernung = Math.sqrt(dlat2 * dlat2 + dlon2 * dlon2);
		return entfernung; // in km Luftlinie
	}

	private void positionsUpdate() {
		float zoomLevel = map.getCameraPosition().zoom;
		LatLng newPos = new LatLng(Rechnung.startLat, Rechnung.startLon);

		try {
			if (hellerPunkt || zoomLevel != altesZoomLevel) {
				altesZoomLevel = zoomLevel;
				hellerPunkt = false;

				aktuellerMarker[0].setVisible(false);

				if (zoomLevel < 17) {
					current_position_ohne.setPosition(newPos);
					current_position_ohne.setVisible(true);
					aktuellerMarker[0] = current_position_ohne;
				} else if (zoomLevel >= 17 && zoomLevel < 18) {
					current_position_k1.setPosition(newPos);
					current_position_k1.setVisible(true);
					aktuellerMarker[0] = current_position_k1;
				} else if (zoomLevel >= 18 && zoomLevel < 19) {
					current_position.setPosition(newPos);
					current_position.setVisible(true);
					aktuellerMarker[0] = current_position;
				} else if (zoomLevel >= 19 && zoomLevel < 20) {
					current_position_g1.setPosition(newPos);
					current_position_g1.setVisible(true);
					aktuellerMarker[0] = current_position_g1;
				} else if (zoomLevel >= 20) {
					current_position_g2.setPosition(newPos);
					current_position_g2.setVisible(true);
					aktuellerMarker[0] = current_position_g2;
				}
			}

			if (Rechnung.schrittZaehler != schrittZaehlerAlt) {
				schrittZaehlerAlt = Rechnung.schrittZaehler;

				aktuellerMarker[0].setVisible(false);

				if (zoomLevel < 17) {
					if (jetzt % 2 != 0) {
						current_position_ohne.setPosition(newPos);
						current_position_ohne.setVisible(true);
						aktuellerMarker[0] = current_position_ohne;
						hellerPunkt = false;
					} else {
						current_position_anim_ohne.setPosition(newPos);
						current_position_anim_ohne.setVisible(true);
						aktuellerMarker[0] = current_position_anim_ohne;
						hellerPunkt = true;
					}
				} else if (zoomLevel >= 17 && zoomLevel < 18) {
					if (jetzt % 2 != 0) {
						current_position_k1.setPosition(newPos);
						current_position_k1.setVisible(true);
						aktuellerMarker[0] = current_position_k1;
						hellerPunkt = false;
					} else {
						current_position_anim_k1.setPosition(newPos);
						current_position_anim_k1.setVisible(true);
						aktuellerMarker[0] = current_position_anim_k1;
						hellerPunkt = true;
					}
				} else if (zoomLevel >= 18 && zoomLevel < 19) {
					if (jetzt % 2 != 0) {
						current_position.setPosition(newPos);
						current_position.setVisible(true);
						aktuellerMarker[0] = current_position;
						hellerPunkt = false;
					} else {
						current_position_anim.setPosition(newPos);
						current_position_anim.setVisible(true);
						aktuellerMarker[0] = current_position_anim;
						hellerPunkt = true;
					}
				} else if (zoomLevel >= 19 && zoomLevel < 20) {
					if (jetzt % 2 != 0) {
						current_position_g1.setPosition(newPos);
						current_position_g1.setVisible(true);
						aktuellerMarker[0] = current_position_g1;
						hellerPunkt = false;
					} else {
						current_position_anim_g1.setPosition(newPos);
						current_position_anim_g1.setVisible(true);
						aktuellerMarker[0] = current_position_anim_g1;
						hellerPunkt = true;
					}
				} else if (zoomLevel >= 20) {
					if (jetzt % 2 != 0) {
						current_position_g2.setPosition(newPos);
						current_position_g2.setVisible(true);
						aktuellerMarker[0] = current_position_g2;
						hellerPunkt = false;
					} else {
						current_position_anim_g2.setPosition(newPos);
						current_position_anim_g2.setVisible(true);
						aktuellerMarker[0] = current_position_anim_g2;
						hellerPunkt = true;
					}
				}

			}
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		if (followMe) {
			if (jetzt % 2 != 0) {
				if (egoPerspektive) {
					// Kamera in Kompass-Richtung drehen wenn gewünscht
					CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing((float) Rechnung.azimuth).tilt(65.5f).zoom(zoomLevel)
							.build();
					map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
				} else {
					// Kamera in nach Norden ausrichten
					CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing(0.0F).tilt(0.0F).zoom(zoomLevel).build();
					map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
				}
			}
		}
		jetzt++;
		if (stationsCounter < (phasen - 1) && anfangAbgewartet) {
			abschnittKontrolle();
		}
		// Log.d("Location-Status"," - neuer Task - ");
		listHandler.sendEmptyMessageDelayed(4, 500);
	}

	public static void setPosition(boolean follow) {
		startLatLng = new LatLng(Rechnung.startLat, Rechnung.startLon);
		try {
			aktuellerMarker[0].setPosition(startLatLng);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		// Log.d("Location-Status", "setPosition:");
		if (follow == true) {
			if (Rechnung.lastErrorGPS < 100) {
				map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 18.0F)));
				// Log.d("Location-Status", "zoom auf:" + 18);
			} else if (Rechnung.lastErrorGPS < 231) {
				map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 17.0F)));
				// Log.d("Location-Status", "zoom auf:" + 17);
			} else if (Rechnung.lastErrorGPS < 401) {
				map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 16.0F)));
				// Log.d("Location-Status", "zoom auf:" + 16);
			} else if (Rechnung.lastErrorGPS < 801) {
				map.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 15.0F)));
				// Log.d("Location-Status", "zoom auf:" + 15);
			} else if (Rechnung.lastErrorGPS < 1501) {
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

	public void setErstePosition() {
		startLatLng = new LatLng(Rechnung.startLat, Rechnung.startLon);
		current_position.setPosition(startLatLng);
		current_position.setVisible(true);
		aktuellerMarker[0] = current_position;

		if (Rechnung.lastErrorGPS < 100) {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 18.0F)));
		} else if (Rechnung.lastErrorGPS < 231) {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 17.0F)));
		} else if (Rechnung.lastErrorGPS < 401) {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 16.0F)));
		} else if (Rechnung.lastErrorGPS < 801) {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 15.0F)));
		} else if (Rechnung.lastErrorGPS < 1501) {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 14.0F)));
		} else if (Rechnung.lastErrorGPS == 9999999) {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 4.0F)));
			View viewLine = (View) findViewById(R.id.view156);
			viewLine.setVisibility(0);
			TextView mapText = (TextView) findViewById(R.id.mapText);
			mapText.setVisibility(0);
			mapText.setText(getResources().getString(R.string.tx_06));
		} else {
			map.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(startLatLng, 13.0F)));
		}
		// den PositionsTask anwerfen
		listHandler.sendEmptyMessageDelayed(4, POS_UPDATE_FREQ);
	}

	// ****************************************************************
	// ************* ROUTEN-TASK **************************************

	private class routenTask extends AsyncTask<String, Void, Void> {

		private String endAddress;
		private JSONArray stepsArray;
		private LatLng northeastLatLng;
		private LatLng southwestLatLng;
		private boolean getPathErfolgreich;
		private String ersteEntfAngabe;

		protected void getPath(LatLng src, LatLng dest) {
			anfangAbgewartet = false;
			counterRoutenKomplexitaet = phasen = stationsCounter = 0;

			HttpRequests httpJSON = new HttpRequests();
			httpJSON.setURL("http://maps.googleapis.com/maps/api/directions/json");
			httpJSON.setMethod("GET");
			httpJSON.addValue("origin", src.latitude + "," + src.longitude);
			httpJSON.addValue("destination", dest.latitude + "," + dest.longitude);
			httpJSON.addValue("sensor", "true");
			httpJSON.addValue("mode", "walking");
			httpJSON.addValue("language", sprache);
			String response = httpJSON.doRequest();
			try {
				getPathErfolgreich = true;
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

				// //vollständige Polyline für erste zeichnung holen
				// JSONObject overview =
				// routesObject.getJSONObject("overview_polyline");
				// fullPolyline = overview.getString("points");

				// Ziel ist durch longPress bestimmt
				// endAddress soll dann aus der JSON bestimmt werden
				// wenn Ziel durch die Suche ausgewählt wurde, dann ist
				// endAddress schon mit richtiger Adresse bestückt
				if (endAddress.equalsIgnoreCase("zielortSollRoutenTaskSelbstRausfinden")) {
					endAddress = legsObject.getString("end_address");
				}
				String[] zielOrtArray = new String[5];
				zielOrtArray = endAddress.split(",", 3);
				try {
					endAddress = zielOrtArray[0];
					endAddress += "\n" + zielOrtArray[1];
				} catch (Exception e) {
					// kann gut sein, wenn Zielname nur 1 oder 1 Zeilen hat, ist
					if (Config.debugModus)
						e.printStackTrace();
				}
				if (sprache.equalsIgnoreCase("de")) {
					ersteEntfAngabe = "Ziel ist " + distance + "\n" + "oder " + duration + " entfernt.";
				} else if (sprache.equalsIgnoreCase("es")) {
					ersteEntfAngabe = "Destino es de " + distance + "\n" + "o " + duration + " de distancia.";
				} else if (sprache.equalsIgnoreCase("fr")) {
					ersteEntfAngabe = "Destination est de " + distance + "\n" + "ou " + duration + ".";
				} else if (sprache.equalsIgnoreCase("pl")) {
					ersteEntfAngabe = "Docelowy jest " + distance + "\n" + "lub " + duration + ".";
				} else if (sprache.equalsIgnoreCase("it")) {
					ersteEntfAngabe = "Destination si trova a " + distance + "\n" + "o " + duration + ".";
				} else if (sprache.equalsIgnoreCase("en")) {
					ersteEntfAngabe = "Destination is " + distance + "\n" + "or " + duration + " away.";
				} else {
					ersteEntfAngabe = "Distance: " + distance + "\n or" + duration + ".";
				}
				stepsArray = legsObject.getJSONArray("steps");
				phasen = stepsArray.length();

			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
				getPathErfolgreich = false;
			}
		}

		@Override
		protected Void doInBackground(String... query) {
			endAddress = query[0];
			try {
				getPath(GoogleMapActivity.startLatLng, GoogleMapActivity.zielLatLng);
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void void1) {
			if (getPathErfolgreich) {
				drawPath();
				listHandler.removeMessages(4);
				// Log.d("Location-Status",
				// "Positionstask AUS    weil routenTask");
				// Positionstask soll nich wieterlaufen, erst nach
				// routenStartAnimation im listHandler
				routenStartAnimation(northeastLatLng, southwestLatLng);
			}
			macheInfos(endAddress, ersteEntfAngabe);
			// setPosition(false);
		}

		public void drawPath() {
			if (routeIstGezeichnet) {
				// Log.d("Location-Status", "Route ist gezeichnet = " +
				// routeIstGezeichnet);
				for (int a = 0; a <= routenStuecke; a++) {
					kompletteRoute[a].remove();
					routeIstGezeichnet = false;
				}
			}
			try {
				int color = Color.argb(200, 25, 181, 224);
				for (int i = 0; i < phasen; i++) {
					if (counterRoutenKomplexitaet < 30) {
						counterRoutenKomplexitaet++;
						JSONObject stepObject = stepsArray.optJSONObject(i);
						html_instructions[i] = stepObject.getString("html_instructions");

						JSONObject endObject = stepObject.optJSONObject("end_location");
						int gp2Lon = (int) (Double.parseDouble(endObject.getString("lng")) * 1E6);
						int gp2Lat = (int) (Double.parseDouble(endObject.getString("lat")) * 1E6);
						gp2Lonk[i] = gp2Lon / 1E6;
						gp2Latk[i] = gp2Lat / 1E6;

						JSONObject polyObject = stepObject.optJSONObject("polyline");
						String polyline = polyObject.getString("points");

						// Polylines Sammeln in String Array um später das Array
						// auch verkürzt zeichnen zu können
						polylineArray[i] = polyline;
						kompletteRoute[i] = map.addPolyline(new PolylineOptions().addAll(decodePoly(polylineArray[i])).width(8).color(color));
						routeIstGezeichnet = true;
						routenStuecke = i;
					}
				}

			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
		}

	}

	// ****************** ENDE ROUTEN-TASK *******************************
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

	public void abschnittKontrolle() {
		double abstand = neueDistanz(gp2Latk[stationsCounter], gp2Lonk[stationsCounter]);
		if (abstand < 0.03 && zuEndeGesprochen) {
			zuEndeGesprochen = false;
			kompletteRoute[stationsCounter].remove();
			stationsCounter++;
			Spanned marked_up = Html.fromHtml(html_instructions[stationsCounter]);
			String textString = marked_up.toString();
			((TextView) findViewById(R.id.mapText)).setText(textString);
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			String textString2;
			if (sprache.equalsIgnoreCase("de")) {
				textString2 = "Als nächstes, " + textString;
			} else if (sprache.equalsIgnoreCase("es")) {
				textString2 = "A continuacion, " + textString;
			} else if (sprache.equalsIgnoreCase("fr")) {
				textString2 = "Ensuite, " + textString;
			} else if (sprache.equalsIgnoreCase("pl")) {
				textString2 = "Nastepnie, " + textString;
			} else if (sprache.equalsIgnoreCase("it")) {
				textString2 = "Successivamente,  " + textString;
			} else if (sprache.equalsIgnoreCase("en")) {
				textString2 = "Next, " + textString;
			} else {
				textString2 = textString;
			}
			if (sprachAusgabe) {
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
					zuEndeGesprochen = true;
				}
			}, 10000);
		}
	}

	public void getRouteInfo() {
		TextView mapText = (TextView) findViewById(R.id.mapText);
		View viewLine = (View) findViewById(R.id.view156);
		mapText.setText(getApplicationContext().getResources().getString(R.string.tx_04));
		viewLine.setVisibility(0);
		mapText.setVisibility(0);
	}

	public void macheInfos(String endAddress, String ersteEntfAngabe) {
		TextView mapText = (TextView) findViewById(R.id.mapText);
		View viewLine = (View) findViewById(R.id.view156);
		if (ersteEntfAngabe != null) {
			mapText.setText(endAddress + "\n\n" + ersteEntfAngabe);
			viewLine.setVisibility(0);
			mapText.setVisibility(0);
			if (sprachAusgabe) {
				ersteEntfAngabe = ersteEntfAngabe.replace("-", " ");
				ersteEntfAngabe = ersteEntfAngabe.replace("\n", " ");
				ersteEntfAngabe = ersteEntfAngabe.replace("/", " ");
				mTts.speak(ersteEntfAngabe, TextToSpeech.QUEUE_FLUSH, null);
			}
			Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				public void run() {
					zuEndeGesprochen = true;
					anfangAbgewartet = true;
				}
			}, 8500);
			uTaskIstAn = true;
		} else {
			Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_30), Toast.LENGTH_LONG).show();
			viewLine.setVisibility(4);
			mapText.setVisibility(4);
		}
	}

	@Override
	protected void onResume() {
		try {
			Config.usingGoogleMaps = true;
			egoPerspektive = false;
			compassStatus = 1;
			ImageView compass = (ImageView) findViewById(R.id.nadel);
			compass.setImageResource(R.drawable.needle);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if (status != ConnectionResult.SUCCESS) {
			try {
				TextView mapText = (TextView) findViewById(R.id.mapText);
				mapText.setVisibility(0);
				mapText.append("Google Play Services NOT installed.");
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
		} else if (userSchaltetGPS == false) {
			listHandler.sendEmptyMessageDelayed(4, POS_UPDATE_FREQ);
			// Log.d("Location-Status","Positionstask EINGESCHALTET weil onResume 1");
			restartListenerLight();

			if (bekannterPauseGrund == true) {
				// User kommt gerade aus Einstellungen, Hintergrunddienst oder Info-Seite
				bekannterPauseGrund = false;
			} else {
				// User ruft onResume auf, also wohl bildschirm abgeschaltet, also GPS-Position holen
				mLocationer.startLocationUpdates();
			}

			if (Config.debugModus == true) {
				TextView mapText = (TextView) findViewById(R.id.mapText);
				mapText.setVisibility(View.GONE);
			}

			Rechnung.schrittZaehler = Rechnung.schrittZaehler + schrittSpeicher;
			schrittSpeicher = 0;

			if (Rechnung.startLat != 0) {
				startLatLng = new LatLng(Rechnung.startLat, Rechnung.startLon);
			}

			if (Smartgeo.sGeoLat != 0) {
				startLatLng = new LatLng(Smartgeo.sGeoLat, Smartgeo.sGeoLon);
			}

			ImageView compass = (ImageView) findViewById(R.id.nadel);
			LatLng newPos = new LatLng(Rechnung.startLat, Rechnung.startLon);
			if (compassStatus == 4) {
				listHandler.removeMessages(4);
				// Log.d("Location-Status","Positionstask AUS        compassNadel");
				// status auf 3
				compass.setImageResource(R.drawable.needle3);
				compassStatus = 3;
				map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
				// Positionstask wieder anwerfen
				listHandler.sendEmptyMessageDelayed(4, 1500);
			} else if (compassStatus == 2) {
				listHandler.removeMessages(4);
				// Log.d("Location-Status","Positionstask AUS        compassNadel");
				// status auf 1
				compass.setImageResource(R.drawable.needle);
				compassStatus = 1;
				map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
				// Positionstask wieder anwerfen
				listHandler.sendEmptyMessageDelayed(4, 1500);
			}
			followMe = true;

			SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

			// Export
			boolean export = settings.getBoolean("export", false);
			try {
				mRechnung.schreibeLog(export);
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
			if (export == true) {
				Config.exportBenutzt = true;
			}

			// KartenAnsicht aendern
			satelliteView = settings.getBoolean("view", false);
			if (satelliteView == true) {
				map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
			} else {
				map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			}

			// Sprachausgabe
			sprachAusgabe = settings.getBoolean("sprache", true);

			// Vibration
			vibration = settings.getBoolean("vibration", true);

			schrittZaehlerAlt = Rechnung.schrittZaehler - 1;
		} else if (userSchaltetGPS == true) {
			// User wurde vorher von SmartNavi in die Einstellungen geschickt,
			// hat also viell GPS eingeschaltet, also suchen :)
			mLocationer.startLocationUpdates();
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		try {
			mLocationer.deactivateLocationer();
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		try {
			ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
			mProgressBar.setVisibility(View.GONE);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}

		try {
			listHandler.removeMessages(4);
			// Log.d("Location-Status", "Positionstask AUS    weil onPause");
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		try {
			mSensorManager.unregisterListener(this);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		// // Log.d("Location-Status", "Sensoren gestoppt.");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		routeIstGezeichnet = false;

		// // Log.d("Location-Status",
		// "SmartNavi beendet! Sensoren ausgeschaltet.");
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
			if (Config.debugModus)
				e.printStackTrace();
		}
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
		if (status != ConnectionResult.SUCCESS) {
			// nothing
		} else {
			map.clear();
			mSensorManager.unregisterListener(this);
			if (uTaskIstAn == true) {
				anfangAbgewartet = true;
				uTaskIstAn = false;
			}
			mRechnung.beende(this);
			SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
			new schreibeSettings("export", false).execute();
			if (settings.getBoolean("nutzdaten", true)) {
				sendeStatistik();
			}
			mTts.stop();
			mTts.shutdown();
			Config.serviceGesehen = false;
			Config.exportBenutzt = false;
			Config.hintergrundStartzeit = 0;

			try {
				mLocationer.beendeAutocorrect();
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
		}
		super.onDestroy();
	}

	private class schreibeSettings extends AsyncTask<Void, Void, Void> {

		private String key;
		private int datenTyp;
		private boolean einstellung;
		private String einstellung2;
		private int einstellung3;

		private schreibeSettings(String key, boolean einstellung) {
			this.key = key;
			this.einstellung = einstellung;
			datenTyp = 0;
		}

		private schreibeSettings(String key, String einstellung2) {
			this.key = key;
			this.einstellung2 = einstellung2;
			datenTyp = 1;
		}

		private schreibeSettings(String key, int einstellung3) {
			this.key = key;
			this.einstellung3 = einstellung3;
			datenTyp = 2;
		}

		@Override
		protected Void doInBackground(Void... params) {
			SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
			if (datenTyp == 0) {
				settings.edit().putBoolean(key, einstellung).commit();
			} else if (datenTyp == 1) {
				settings.edit().putString(key, einstellung2).commit();
			} else if (datenTyp == 2) {
				settings.edit().putInt(key, einstellung3).commit();
			}
			return null;
		}
	}

	public void sendeStatistik() {
		SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

		// nochmal UID holen weil die beim ersten Start noch nicht aktuell in
		// diesem Wert steckt
		uid = settings.getString("uid", "0");
		// //// Log.d("Location-Status" , "UID = "+uid);

		HttpRequests httpStats = new HttpRequests();
		httpStats.setURL(Config.VSERVER_API_URL);
		httpStats.setHeader(Config.VSERVER_API_HEADER);
		// status
		httpStats.addValue("uid", uid);
		httpStats.addValue("status", "ok");
		// device info
		httpStats.addValue("deviceName", deviceName);
		httpStats.addValue("productName", productName);
		httpStats.addValue("modelName", modelName);
		httpStats.addValue("androidVersion", "" + androidVersion);
		// sensor info
		httpStats.addValue("accelName", accelName);
		httpStats.addValue("magnetName", magnetName);
		httpStats.addValue("gyroName", gyroName);
		// sensor info quali
		// TODO raus
		httpStats.addValue("quali", "");
		httpStats.addValue("qualiAccelFilter1", "");
		httpStats.addValue("qualiAccelFilter2", "");
		httpStats.addValue("qualiAccelFilter3", "");
		httpStats.addValue("qualiMagnFilter1", "");
		httpStats.addValue("qualiMagnFilter2", "");
		httpStats.addValue("qualiMagnFilter3", "");
		// TODO bis hier
		httpStats.addValue("maxAccel", "" + maxAccelFreq);
		httpStats.addValue("minAccel", "" + minAccelFreq);
		httpStats.addValue("durchAccel", "" + durchAccelFreq);
		httpStats.addValue("maxMagn", "" + maxMagnFreq);
		httpStats.addValue("minMagn", "" + minMagnFreq);
		httpStats.addValue("durchMagn", "" + durchMagnFreq);
		// Nutzungsdauer
		kartenNutzung = "" + (System.currentTimeMillis() - kartenStartzeit) / 1000;
		serviceNutzung = "0"; // erstmal Null
		if (Config.hintergrundStartzeit != 0) {
			serviceNutzung = "" + (Config.hintergrundEndzeit - Config.hintergrundStartzeit) / 1000;
		}
		httpStats.addValue("kartenNutzung", kartenNutzung);
		httpStats.addValue("serviceNutzung", serviceNutzung);
		// Position bestimmt über:
		httpStats.addValue("gpsAktiv", "" + gpsAktiv);
		httpStats.addValue("gpsErfolgreich", "" + gpsErfolgreich);

		// Nutzungsverhalten
		int schrittZahl = Rechnung.schrittZaehler;
		httpStats.addValue("infoGesehen", "" + GoogleMapActivity.infoGesehen);

		httpStats.addValue("tutorialAufgerufen", "" + tutorialAufgerufen);
		httpStats.addValue("serviceGesehen", "" + Config.serviceGesehen);
		httpStats.addValue("exportVerwendet", "" + Config.exportBenutzt);
		httpStats.addValue("routeGenutzt", "" + ersteRouteFertig);
		httpStats.addValue("satelliteView", "" + settings.getBoolean("view", false));
		httpStats.addValue("vibration", "" + settings.getBoolean("vibration", true));
		httpStats.addValue("sprachAusgabe", "" + settings.getBoolean("sprache", true));
		httpStats.addValue("schrittZahl", "" + schrittZahl);
		httpStats.addValue("fremdAufruf", "" + fremdAufruf);

		httpStats.addValue("not_rated", "" + settings.getInt("not_rated", 0));
		httpStats.addValue("egoPerspektive", "" + egoPerspektive);
		if (Config.debugModus) {
			// Log.i("Nutzungsdaten", httpStats.toString());
		}

		// execute the async task: postData
		try {
			new postTask(httpStats).execute();
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {

		mainMenu = menu;

		subMenu1 = menu.addSubMenu(0, 3, 3, "").setIcon(R.drawable.ic_menu_moreoverflow_normal_holo_dark);
		subMenu1.add(0, 4, 4, getApplicationContext().getResources().getString(R.string.tx_64));
		subMenu1.add(0, 5, 5, getApplicationContext().getResources().getString(R.string.tx_15));
		subMenu1.add(0, 6, 6, getApplicationContext().getResources().getString(R.string.tx_50));
		subMenu1.add(0, 7, 7, getApplicationContext().getResources().getString(R.string.tx_65));

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

				// Falls off, wird das longPressMenu invisible gemachty
				liste = (ListView) findViewById(R.id.liste);
				liste.setVisibility(View.INVISIBLE);
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {

		// Falls off, wird das longPressMenu invisible gemacht
		try {
			liste = (ListView) findViewById(R.id.liste);
			liste.setVisibility(View.INVISIBLE);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}

		switch (item.getItemId()) {
		case 5:
			// Aufrufen der Settings
			bekannterPauseGrund = true;
			startActivity(new Intent(this, Settings.class));
			return true;
		case 4:
			// smartgeo
			// schritte speichern um sie sp+ter wenn man wieder zu Karte
			// zurückkommt, mit
			// denen zusammenzurechnen, die im Hintergrunddienst gemacht wurden,
			// das ist nur für sendeDaten
			bekannterPauseGrund = true;
			schrittSpeicher = Rechnung.schrittZaehler;
			Intent myIntent = new Intent(GoogleMapActivity.this, Smartgeo.class);
			startActivity(myIntent);
			return true;
		case 6:
			// zum Tutorial
			tutorialKram();
			return true;
		case 7:
			// zur About Seite
			bekannterPauseGrund = true;
			startActivity(new Intent(this, Info.class));
			return true;
		case android.R.id.home:
			// zurück zu Tuto und finish
			finish();
			return (true);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void restartListener() {
		// // Log.d("Location-Status", "Sensoren gestartet.");
		durchlauf = 1;
		durchAccelFreq = durchMagnFreq = 0;
		accelEinheiten = 0;
		magnEinheiten = 0;
		startTime = System.nanoTime();
		try {
			// Man könnte auch direkt das delay angeben, aber dann läuft App erst ab Android 2.3
			mSensorManager.registerListener(GoogleMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
			mSensorManager.registerListener(GoogleMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
	}

	private void restartListenerLight() {
		// // Log.d("Location-Status", "Sensoren wieder gestartet.");
		try {
			mSensorManager.unregisterListener(GoogleMapActivity.this);
			mSensorManager.registerListener(GoogleMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
			mSensorManager.registerListener(GoogleMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
		} catch (Exception e) {
			if (Config.debugModus)
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
			mRechnung.imbaMagnetic(event.values.clone());
			if (Config.debugModus) {
				Rechnung.origmagn = event.values.clone();
			}
			magnEinheiten++;
			break;

		case Sensor.TYPE_ACCELEROMETER:
			if (Config.debugModus) {
				Rechnung.origaccel = event.values.clone();
			}

			if (Config.hintergrundAn == true && einheiten % 50 == 0) {
				Smartgeo.neueFakePosition();
			}

			verstrZeit = System.nanoTime() - startTime;
			accelEinheiten++;
			einheiten++;

			if (verstrZeit >= 2000000000) { // every 2sek
				mRechnung.changeDelay(accelEinheiten / 2, 0);
				mRechnung.changeDelay(magnEinheiten / 2, 1);
				// Log.d("egal", "verstrZeit = 2000; aclFreq = " + accelEinheiten / 2 + " magnFreq = " + magnEinheiten / 2);

				// durchschnitts-werte ermitteln und speichern
				durchAccelFreq = (int) ((durchAccelFreq + accelEinheiten / 2) / durchlauf);
				durchMagnFreq = (int) ((durchMagnFreq + magnEinheiten / 2) / durchlauf);

				// maximal-werte ermitteln und speichern
				if (accelEinheiten / 2 > maxAccelFreq) {
					maxAccelFreq = accelEinheiten / 2;
				}
				if (accelEinheiten / 2 < minAccelFreq) {
					minAccelFreq = accelEinheiten / 2;
				}
				// minimal-werte ermitteln und speichern
				if (magnEinheiten / 2 > maxMagnFreq) {
					maxMagnFreq = magnEinheiten / 2;
				}
				if (magnEinheiten / 2 < minMagnFreq) {
					minMagnFreq = magnEinheiten / 2;
				}

				startTime = System.nanoTime();
				accelEinheiten = magnEinheiten = 0;
				durchlauf = 2;
			}

			mRechnung.imbaGravity(event.values.clone());
			mRechnung.imbaLinear(event.values.clone());
			mRechnung.rechneDaten();
			mRechnung.schrittMessung();

			// Autokorrektur je nach Stufe (also Schrittzahl)
			if (autoCorrect) {
				if (warteSchonAufAutoCorrect == false) {
					warteSchonAufAutoCorrect = true;
					zuWartendeSchritte = Rechnung.schrittZaehler + 75 * autoCorrectFaktor;
					// // Log.d("Location-Status", Rechnung.schrittZaehler +
					// " von " + zuWartendeSchritte);
				}
				if (Rechnung.schrittZaehler >= zuWartendeSchritte) {
					if (Config.hintergrundAn == true) {
						hinterGrundSollWiederAn = true;
						Smartgeo.pausiereFakeProvider();
					}
					mLocationer.starteAutocorrect();
					warteSchonAufAutoCorrect = false;
					// // Log.d("Location-Status",
					// "Mache jetzt Autokorrektur weil: " +
					// Rechnung.schrittZaehler + " von " +
					// zuWartendeSchritte);
				}
			}

			// if (Config.debugModus == true && Config.hintergrundAn == false) {
			// TextView mapText = (TextView) findViewById(R.id.mapText);
			// mapText.setText("Az " + df.format(Rechnung.azimuth)
			// + "\nDurch: "
			// + durchAccelFreq + " "
			// + durchMagnFreq + "\nAccel: "
			// + accelEinheiten / 2
			// +"\n Magnet: "
			// + magnEinheiten / 2
			// + "\n"
			// + Rechnung.schrittZaehler + " Schritte"
			// );
			// } else if (Config.debugModus == true && Config.hintergrundAn == true && einheiten % 50 == 0) {
			// Log.i("Frequenzen",
			// df.format(Rechnung.azimuth)
			// + "\nDurch: "
			// + durchAccelFreq + " "
			// + durchMagnFreq + "\nAccel: "
			// + accelEinheiten / 2
			// +"\n Magnet: "
			// + magnEinheiten / 2
			// + "\n"
			// + Rechnung.schrittZaehler + " Schritte"
			// );
			// }
			break;
		}
	}

	private class postTask extends AsyncTask<Void, Void, Void> {

		HttpRequests mRequest;

		public postTask(HttpRequests arg0) {
			mRequest = arg0;
		}

		@Override
		protected Void doInBackground(Void... params) {

			mRequest.doRequest();

			return null;
		}
	}

	private class anfrageTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			HttpRequests httpStats = new HttpRequests();
			httpStats.setURL(Config.VSERVER_API_URL);
			httpStats.setHeader(Config.VSERVER_API_HEADER);

			// Geräteinfo
			httpStats.addValue("uid", uid);
			httpStats.addValue("status", "anfrage");
			httpStats.addValue("deviceName", deviceName);
			httpStats.addValue("productName", productName);
			httpStats.addValue("modelName", modelName);
			httpStats.addValue("androidVersion", "" + androidVersion);
			httpStats.doRequest();

			return null;
		}
	}

	// private class saveLastPosition extends AsyncTask<Void, Void, Void> {
	//
	// @Override
	// protected Void doInBackground(Void... params) {
	// try {
	// SharedPreferences settings = getSharedPreferences(getPackageName() +
	// "_preferences", MODE_PRIVATE);
	//
	// settings.edit().putString("passiveLat", "" + Rechnung.startLat).commit();
	// settings.edit().putString("passiveLon", "" + Rechnung.startLon).commit();
	// settings.edit().putLong("passiveError", (long) errorGPS).commit();
	// settings.edit().putLong("passiveAltitude", (long) altitude).commit();
	// settings.edit().putString("passiveProvider", "SmartNavi").commit();
	// settings.edit().putLong("passiveTime",
	// System.currentTimeMillis()).commit();
	// // //// Log.d("Location-Status", "Zuletzt gespeichert: "+saveLast);
	// } catch (Exception e) {
	// if(debug)e.printStackTrace();
	// }
	// return null;
	// }
	// }

	public void fingerDestination(LatLng g) {
		zielLat = g.latitude;// getLatitudeE6() / 1E6;
		zielLon = g.longitude;// getLongitudeE6() / 1E6;
		zielLatLng = g;
		listHandler.removeCallbacksAndMessages(null);
		getRouteInfo();
		map.stopAnimation();
		setPosition(false);
		drawableZiel = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
		setZielPos(zielLatLng);
		new routenTask().execute("zielortSollRoutenTaskSelbstRausfinden");
	}

	public void setZielPos(LatLng z) {
		zielMarker.setPosition(z);
		zielMarker.setVisible(true);
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
				Toast.makeText(GoogleMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_77), Toast.LENGTH_LONG).show();
			} else {
				// set destination for the routing tasks
				try {
					zielLat = (Double) destination.get("lat");
					zielLon = (Double) destination.get("lng");
					zielLatLng = new LatLng(zielLat, zielLon);
					listHandler.removeCallbacksAndMessages(null);
					map.stopAnimation();
					listHandler.removeMessages(4);
					// Log.d("Location-Status","Positionstask AUS    weil placesTextSearch");
					setPosition(false);
					setZielPos(zielLatLng);
					getRouteInfo();
					new routenTask().execute(query);
				} catch (JSONException e) {
					if (Config.debugModus)
						e.printStackTrace();
				}
			}
		}
	}

	public void routenStartAnimation(LatLng northeast, LatLng southwest) {
		LatLngBounds grenzen = new LatLngBounds(southwest, northeast);
		map.animateCamera(CameraUpdateFactory.newLatLngBounds(grenzen, 100));
		listHandler.sendEmptyMessageDelayed(11, 3000);
	}

	public void tutorialKram() {
		// einblenden und Kartenclicks und Longpress deaktivieren
		map.getUiSettings().setAllGesturesEnabled(false);
		tutorialOverlay = (View) findViewById(R.id.tutorialOverlay);
		tutorialOverlay.setVisibility(View.VISIBLE);

		SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		String schrittLaengeString = settings.getString("step_length", null);
		Spinner spinner = (Spinner) findViewById(R.id.tutorialSpinner);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.dimension, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		if (schrittLaengeString != null) {
			try {
				schrittLaengeString = schrittLaengeString.replace(",", ".");
				int gespeicherteGroesse = Integer.parseInt(schrittLaengeString);
				if (gespeicherteGroesse < 241 && gespeicherteGroesse > 119) {
					EditText editText = (EditText) findViewById(R.id.tutorialEditText);
					editText.setText("" + gespeicherteGroesse);
					spinner.setSelection(0);
				} else if (gespeicherteGroesse < 95 && gespeicherteGroesse > 45) {
					EditText editText = (EditText) findViewById(R.id.tutorialEditText);
					editText.setText("" + gespeicherteGroesse);
					spinner.setSelection(1);
				}
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
		}
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 == 0) {
					metrischeAngaben = true;
				} else {
					metrischeAngaben = false;
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
				boolean tutorialAbgeschlossen = false;
				final EditText koerperFeld = (EditText) findViewById(R.id.tutorialEditText);
				int op = koerperFeld.length();
				float number;
				if (op != 0) {
					try {
						number = Float.valueOf(koerperFeld.getText().toString());
						if (number < 241 && number > 119 && metrischeAngaben == true) {

							String numberString = df0.format(number);
							new schreibeSettings("step_length", numberString).execute();
							Rechnung.schrittLaenge = (float) (number / 222);
							tutorialAbgeschlossen = true;
						} else if (number < 95 && number > 45 && metrischeAngaben == false) {

							String numberString = df0.format(number);
							new schreibeSettings("step_length", numberString).execute();
							Rechnung.schrittLaenge = (float) (number * 2.54 / 222);
							tutorialAbgeschlossen = true;
						} else {
							Toast.makeText(GoogleMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
						}

					} catch (NumberFormatException e) {
						if (Config.debugModus)
							e.printStackTrace();
						Toast.makeText(GoogleMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(GoogleMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
				}

				if (tutorialAbgeschlossen) {
					// Tutorial ausblenden
					tutorialOverlay = (View) findViewById(R.id.tutorialOverlay);
					tutorialOverlay.setVisibility(View.INVISIBLE);
					// Karte klickbar machen
					map.getUiSettings().setAllGesturesEnabled(true);
					// LongPressDialog
					showLongPressDialog();
				}
			}
		});

		EditText groessenFeld = (EditText) findViewById(R.id.tutorialEditText);
		groessenFeld.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
					try {
						InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
						EditText groessenFeld = (EditText) findViewById(R.id.tutorialEditText); //Workaround Zeiger aus Textfeld nehmen
						groessenFeld.setFocusable(false);
						groessenFeld.setFocusableInTouchMode(true);
						groessenFeld.setFocusable(true);
					} catch (Exception e) {
						if (Config.debugModus)
							e.printStackTrace();
					}
				}
				return false;
			}
		});
	}

	private class geocodeTask extends AsyncTask<String, Void, String> {

		private boolean geocodeSuccess = false;

		@Override
		protected String doInBackground(String... zielOrt) {

			final Geocoder gc = new Geocoder(GoogleMapActivity.this);
			try {
				double lowerLeftLatitude;
				double lowerLeftLongitude;
				double upperRightLatitude;
				double upperRightLongitude;
				// Geocode Suche starten
				// beim ersten Versuch nur in 5km Umkreis
				// beim 2. Versuch mit über 100km Umkreis
				// ganz entfernte Ergebnisse (Moskau) gehen trotzdem
				if (geoVersuch == 0) {
					if (Rechnung.startLat >= 0) {
						lowerLeftLatitude = Rechnung.startLat - 0.05;
						upperRightLatitude = Rechnung.startLat + 0.05;
					} else {
						lowerLeftLatitude = Rechnung.startLat + 0.05;
						upperRightLatitude = Rechnung.startLat - 0.05;
					}

					if (Rechnung.startLon >= 0) {
						lowerLeftLongitude = Rechnung.startLon - 0.07;
						upperRightLongitude = Rechnung.startLon + 0.07;
					} else {
						lowerLeftLongitude = Rechnung.startLon + 0.07;
						upperRightLongitude = Rechnung.startLon - 0.07;
					}
				} else {
					if (Rechnung.startLat >= 0) {
						lowerLeftLatitude = Rechnung.startLat - 0.77;
						upperRightLatitude = Rechnung.startLat + 0.77;
					} else {
						lowerLeftLatitude = Rechnung.startLat + 0.77;
						upperRightLatitude = Rechnung.startLat - 0.77;
					}

					if (Rechnung.startLon >= 0) {
						lowerLeftLongitude = Rechnung.startLon - 0.5;
						upperRightLongitude = Rechnung.startLon + 0.5;
					} else {
						lowerLeftLongitude = Rechnung.startLon + 0.5;
						upperRightLongitude = Rechnung.startLon - 0.5;
					}
				}

				List<Address> foundAdresses = gc.getFromLocationName(zielOrt[0], 1, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude,
						upperRightLongitude);
				for (int i = 0; i < foundAdresses.size(); ++i) {
					Address x = foundAdresses.get(i);
					zielLat = x.getLatitude();
					zielLon = x.getLongitude();
					geocodeSuccess = true;
				}
			} catch (Exception e) {
				geocodeSuccess = false;
				if (Config.debugModus)
					e.printStackTrace();
			}
			return zielOrt[0];
		}

		@Override
		protected void onPostExecute(String zielOrt) {

			if (geocodeSuccess) {
				listHandler.removeCallbacksAndMessages(null);
				map.stopAnimation();
				listHandler.removeMessages(4);
				// Log.d("Location-Status","Positionstask AUS    weil geoCodeTask");
				geoVersuch = 0;
				// zielLat und zielLon wurden schon in onDoInBackground gesetzt
				// bei List<Address>...
				zielLatLng = new LatLng(zielLat, zielLon);
				setPosition(false);
				setZielPos(zielLatLng);
				drawableZiel = BitmapFactory.decodeResource(getResources(), R.drawable.finish2);
				getRouteInfo();
				new routenTask().execute("zielortSollRoutenTaskSelbstRausfinden");

				Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					public void run() {
						mainMenu.getItem(0).collapseActionView();
					}
				}, 7000);
			} else if (geoVersuch < 1) {
				new geocodeTask().execute(zielOrt);
				geoVersuch++;
			} else {
				searchView.clearFocus();
				Toast.makeText(GoogleMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_24), Toast.LENGTH_LONG).show();
				geoVersuch = 0;
			}
		}
	}

	public void compassNadel(View view) {

		// Falls off, wird das longPressMenu invisible gemacht
		try {
			liste = (ListView) findViewById(R.id.liste);
			liste.setVisibility(View.INVISIBLE);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}

		ImageView compass = (ImageView) findViewById(R.id.nadel);
		float zoomLevel = map.getCameraPosition().zoom;
		LatLng newPos = new LatLng(Rechnung.startLat, Rechnung.startLon);

		listHandler.removeMessages(4);
		// Log.d("Location-Status",
		// "Positionstask AUS        compassNadel");

		if (followMe == false && touchHatFollowAbgeschaltet == true) {
			touchHatFollowAbgeschaltet = false;
			if (compassStatus == 4) {
				// status auf 3
				compass.setImageResource(R.drawable.needle3);
				compassStatus = 3;
				map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
			} else if (compassStatus == 2) {
				// status auf 1
				compass.setImageResource(R.drawable.needle);
				compassStatus = 1;
				map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
			}
			// Unbedingt erst die if abfrage und DANN setFollowOn
			setFollowOn();
		}

		else if (compassStatus == 1) {
			// status auf 3
			egoPerspektive = true;
			compass.setImageResource(R.drawable.needle3);
			compassStatus = 3;
			// Kamera in Kompass-Richtung drehen wenn gewünscht
			CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing((float) Rechnung.azimuth).tilt(65.5f).zoom(zoomLevel).build();
			map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
		} else if (compassStatus == 3) {
			// status auf 1
			egoPerspektive = false;
			compass.setImageResource(R.drawable.needle);
			compassStatus = 1;
			// Kamera in nach Norden ausrichten
			CameraPosition currentPlace = new CameraPosition.Builder().target(newPos).bearing(0.0F).tilt(0.0F).zoom(zoomLevel).build();
			map.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
		} else if (compassStatus == 4) {
			// status auf 3
			compass.setImageResource(R.drawable.needle3);
			compassStatus = 3;
			setFollowOn();
			map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
		} else if (compassStatus == 2) {
			// status auf 1
			compass.setImageResource(R.drawable.needle);
			setFollowOn();
			compassStatus = 1;
			map.animateCamera(CameraUpdateFactory.newLatLng(newPos));
		}
		// Positionstask wieder anwerfen
		listHandler.sendEmptyMessageDelayed(4, 1500);
	}

	@Override
	public void onUpdateMapAfterUserInterection() {
		touchHatFollowAbgeschaltet = true;
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
		final View appRateDialog = (View) findViewById(R.id.appRateDialog);
		appRateDialog.setVisibility(View.VISIBLE);

		Button rateButton1 = (Button) findViewById(R.id.rateButton);
		rateButton1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				appRateDialog.setVisibility(View.INVISIBLE);

				SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", 0);
				int notRated = prefs.getInt("not_rated", 0) + 1;

				new schreibeSettings("not_rated", notRated).execute();

				if (notRated == 1) {
					new schreibeSettings("launch_count", -6).execute();
				} else if (notRated == 2) {
					new schreibeSettings("launch_count", -8).execute();
				} else if (notRated == 3) {
					new schreibeSettings("launch_count", -10).execute();
				} else if (notRated == 4) {
					new schreibeSettings("dontshowagain", true).execute();
				}
			}
		});

		// Button rateButton2 = (Button) findViewById(R.id.rateButton);
		// rateButton2.setOnClickListener(new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// appRateDialog.setVisibility(View.INVISIBLE);
		// new schreibeSettings("dontshowagain", true);
		// }
		// });

		Button rateButton3 = (Button) findViewById(R.id.rateButton2);
		rateButton3.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new schreibeSettings("not_rated", 999).execute();
				appRateDialog.setVisibility(View.INVISIBLE);
				new schreibeSettings("dontshowagain", true).execute();
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
			}
		});
	}

	public void clickOnStars(final View view) {
		new schreibeSettings("not_rated", 999).execute();
		final View appRateDialog = (View) findViewById(R.id.appRateDialog);
		appRateDialog.setVisibility(View.INVISIBLE);
		new schreibeSettings("dontshowagain", true).execute();
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
	}

	private void showLongPressDialog() {
		try {
			aktuellerMarker[0].setVisible(false);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		final View longPressDialog = (View) findViewById(R.id.longpPressDialog);
		longPressDialog.setVisibility(View.VISIBLE);

		Button longPressButton = (Button) findViewById(R.id.longPressButton);
		longPressButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				longPressDialog.setVisibility(View.GONE);
				try {
					aktuellerMarker[0].setVisible(true);
				} catch (Exception e) {
					if (Config.debugModus)
						e.printStackTrace();
				}
			}
		});
		// Merken dass Longpress gezeigt wurde
		new schreibeSettings("longPressWasShown", true).execute();
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// Log.d("Location-Status","LocationClient: Connection FAILED" + arg0.getErrorCode());
		TextView mapText = (TextView) findViewById(R.id.mapText);
		mapText.setVisibility(0);
		mapText.append("Please make sure you have Google Play Services installed.\nErrorCode: "
				+ arg0.getErrorCode());
	}

	// bei Touch auf ProgressBar
	public void abortGPS(final View view) {
		// Abort GPS was pressed (ProgressBar was pressed)
		try {
			mLocationer.deactivateLocationer();
			ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
			mProgressBar.setVisibility(View.GONE);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		Toast.makeText(this, getResources().getString(R.string.tx_82), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onConnected(Bundle arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	};

	private void showGPSDialog() {
		final Dialog dialogGPS = new Dialog(GoogleMapActivity.this);
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
					userSchaltetGPS = true;
				} catch (android.content.ActivityNotFoundException ae) {
					startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
					userSchaltetGPS = true;
				}
				dialogGPS.dismiss();
			}
		});
	}

}