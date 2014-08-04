package com.ilm.sandwich;

import java.text.DecimalFormat;

import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DirectedLocationOverlay;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.view.Window;
import com.ilm.sandwich.helferklassen.HttpRequests;
import com.ilm.sandwich.helferklassen.Locationer;
import com.ilm.sandwich.helferklassen.MyItemizedOverlay;
import com.ilm.sandwich.helferklassen.Rechnung;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.OnEditorActionListener;

@SuppressLint({ "NewApi" })
public class OsmMapActivity extends SherlockActivity implements SensorEventListener, MapEventsReceiver {

	private MapView myOpenMapView;
	private IMapController myMapController;
	private MyItemizedOverlay[] myItemizedOverlay = new MyItemizedOverlay[10];
	private SensorManager mSensorManager;
	private String magnetName;
	private String accelName;
	private String gyroName;
	private Rechnung mRechnung;
	private long kartenStartzeit;
	private String uid;
	private Locationer mLocationer;
	private boolean exportBenutzt;
	private boolean userSchaltetGPS;
	private boolean bekannterPauseGrund;
	private int einheiten;
	private int accelEinheiten;
	private int magnEinheiten;
	private long startTime;
	private int androidVersion;
	private String modelName;
	private String deviceName;
	private String productName;
	private long verstrZeit;
	private boolean hintergrundAn;
	public static Handler listHandler;
	private boolean followMe = true;
	private int compassStatus;
	private boolean egoPerspektive;
	private boolean touchHatFollowAbgeschaltet;
	protected DirectedLocationOverlay myLocationOverlay;
	private boolean listeIstVisible = false;
	static ListView liste;
	private long POS_UPDATE_FREQ = 750;
	public static boolean firstPositionFound;
	private static boolean setzePositionVonHand = false;
	View tutorialOverlay;
	public boolean metrischeAngaben = true;
	private int schrittSpeicher = 0;
	private static SubMenu subMenu1;
	private static Menu mainMenu;
	static DecimalFormat df = new DecimalFormat("0.0");
	static DecimalFormat df0 = new DecimalFormat("0");
	private static GeoPoint longPressedGeoPoint;
	private boolean autoCorrect = false;
	private int autoCorrectFaktor = 1;
	private boolean warteSchonAufAutoCorrect;
	private int zuWartendeSchritte = 0;
	private boolean hinterGrundSollWiederAn = false;
	OnlineTileSourceBase MAPBOXSATELLITELABELLED;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.ab_bg_black));

		Config.usingGoogleMaps = false;
		SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		setContentView(R.layout.osmmap_layout);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		androidVersion = android.os.Build.VERSION.SDK_INT;
		modelName = android.os.Build.MODEL;
		deviceName = android.os.Build.DEVICE;
		productName = android.os.Build.PRODUCT;

		myOpenMapView = (MapView) findViewById(R.id.openmapview);
		MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
		myOpenMapView.getOverlays().add(mapEventsOverlay);
		myOpenMapView.setBuiltInZoomControls(true);
		myOpenMapView.setKeepScreenOn(true);
		myOpenMapView.setMultiTouchControls(true);
		
		String tileProviderName = settings.getString("MapSource", "MapQuestOSM");
		if(tileProviderName.equalsIgnoreCase("MapQuestOSM")){
			// in the following line the Zoom-Level could be raised from 19 to 20, but tiles are currently not reloading when map is moving
			myOpenMapView.setTileSource(new XYTileSource("MapquestOSM", ResourceProxy.string.mapquest_osm, 0, 19, 256, ".jpg", new String[] {
					"http://otile1.mqcdn.com/tiles/1.0.0/map/", "http://otile2.mqcdn.com/tiles/1.0.0/map/", "http://otile3.mqcdn.com/tiles/1.0.0/map/",
			"http://otile4.mqcdn.com/tiles/1.0.0/map/" }));			
		}else{
			try {
				ITileSource tileSource = TileSourceFactory.getTileSource("Mapnik"); //do not change this string
				myOpenMapView.setTileSource(tileSource);
			} catch (IllegalArgumentException e) {
				myOpenMapView.setTileSource(TileSourceFactory.MAPNIK);
			}			
		}
		
		// http://otile1.mqcdn.com/tiles/1.0.0/sat for satelite pictures, but only zoom lovel 12+ für the U.S.

		// myOpenMapView.setTileSource(TileSourceFactory.MAPQUESTOSM);

		isOnline();
		firstPositionFound = false;

		myMapController = myOpenMapView.getController();
		double defaultLat = 50.000000D;
		double defaultLon = 10.000000D;
		double mittellat = defaultLat * 0.01745329252;
		double abstandLaengengrade = 111.3D * Math.cos(mittellat);
		Rechnung.initialize(defaultLat, defaultLon, abstandLaengengrade, 200, 3000);
		//myMapController.setCenter(new GeoPoint(defaultLat, defaultLon));
		myMapController.setZoom(3);

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			new schreibeSettings("follow", true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			new schreibeSettings("follow", true).execute();
		}

		uid = settings.getString("uid", "0");
		if (uid.equalsIgnoreCase("0")) {
			String neuUID = "" + (1 + (int) (Math.random() * ((10000000 - 1) + 1)));
			new schreibeSettings("uid", neuUID).execute();
			uid = settings.getString("uid", "0");
		}

		mLocationer = new Locationer(this);

		handlerAnwerfen();

		// setPosition(50.682715, 10.932317, R.drawable.ic_maps_indicator_current_position);

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

		// Rate App show for debugging
		// showRateDialog();
		// Rate App live
		appRateDialog();

		positionsUpdate();

		liste = (ListView) findViewById(R.id.listeOsm);
		liste.setVisibility(View.INVISIBLE);
		listeIstVisible = false;
		liste.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (arg2 == 0) {
					setzePositionVonHand = true;
					Rechnung.setLocation(longPressedGeoPoint.getLatitude(), longPressedGeoPoint.getLongitude());
					liste.setVisibility(View.INVISIBLE);
					listeIstVisible = false;
					setFollowOn();
				} else {
					// TODO fingerDestination(longpressLocation);
					liste.setVisibility(View.INVISIBLE);
					listeIstVisible = false;
				}
			}
		});
	}


	private void setFirstPosition(double lat, double lon, int drawable) {
		// Marker setzen
		if (firstPositionFound == false) {
			Log.d("Location-Status", "Set FIRST Position: " + Rechnung.startLat + " and " + Rechnung.startLon);
			//ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
			if (Rechnung.lastErrorGPS < 100) {
				myMapController.setZoom(19);
				// Log.d("Location-Status", "zoom auf:" + 18);
			} else if (Rechnung.lastErrorGPS < 231) {
				myMapController.setZoom(18);
				// Log.d("Location-Status", "zoom auf:" + 17);
			} else if (Rechnung.lastErrorGPS < 401) {
				myMapController.setZoom(17);
				// Log.d("Location-Status", "zoom auf:" + 16);
			} else if (Rechnung.lastErrorGPS < 801) {
				myMapController.setZoom(16);
				// Log.d("Location-Status", "zoom auf:" + 15);
			} else if (Rechnung.lastErrorGPS < 1501) {
				myMapController.setZoom(15);
				// Log.d("Location-Status", "zoom auf:" + 14);
			}
			// Drawable marker = getResources().getDrawable(drawable);
			// myItemizedOverlay[0] = new MyItemizedOverlay(marker, resourceProxy);
			// myOpenMapView.getOverlays().add(myItemizedOverlay[0]);
			// GeoPoint point0 = new GeoPoint(lat, lon);
			// myItemizedOverlay[0].addItem(point0, "point0", "point0");
			myLocationOverlay = new DirectedLocationOverlay(this);
			myOpenMapView.getOverlays().add(myLocationOverlay);
			firstPositionFound = true;
		}
	}

	public void setPosition(boolean follow) {
		int latE6 = (int) (Rechnung.startLat * 1E6);
		int lonE6 = (int) (Rechnung.startLon * 1E6);
		if (firstPositionFound == false) {
			Log.d("Location-Status", "Setze NOTFALL - ErstePosition: " + latE6 + " und " + lonE6);
			setFirstPosition(Rechnung.startLat, Rechnung.startLon, R.drawable.ic_maps_indicator_current_position_ohne);
		} else {
			Log.d("Location-Status", "Setze Position: " + latE6 + " und " + lonE6);
			// myItemizedOverlay[0].getItem(0).getPoint().setLatitudeE6(latE6);
			// myItemizedOverlay[0].getItem(0).getPoint().setLongitudeE6(lonE6);

			myLocationOverlay.setLocation(new GeoPoint(latE6, lonE6));

			if (follow == true) {
				if (Rechnung.lastErrorGPS < 100) {
					myMapController.setZoom(19);
					// Log.d("Location-Status", "zoom auf:" + 18);
				} else if (Rechnung.lastErrorGPS < 231) {
					myMapController.setZoom(18);
					// Log.d("Location-Status", "zoom auf:" + 17);
				} else if (Rechnung.lastErrorGPS < 401) {
					myMapController.setZoom(17);
					// Log.d("Location-Status", "zoom auf:" + 16);
				} else if (Rechnung.lastErrorGPS < 801) {
					myMapController.setZoom(16);
					// Log.d("Location-Status", "zoom auf:" + 15);
				} else if (Rechnung.lastErrorGPS < 1501) {
					myMapController.setZoom(15);
					// Log.d("Location-Status", "zoom auf:" + 14);
				}
				// myMapController.animateTo(new GeoPoint(Rechnung.startLat, Rechnung.startLon));
			}

		}
	}

	@SuppressLint("HandlerLeak")
	public void handlerAnwerfen() {
		listHandler = new Handler() {

			public void handleMessage(Message msg) {
				if (msg.what == 0) {
					setFirstPosition(Rechnung.startLat, Rechnung.startLon, R.drawable.ic_maps_indicator_current_position_ohne);
					positionsUpdate();
					// setErstePosition(); TODO
					restartListener();
					// fremdIntent(); TODO
					// starte Autocorrect falls gewünscht
					// listHandler.sendEmptyMessage(6);
					ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
					mProgressBar.setVisibility(View.VISIBLE);
				} else if (msg.what == 1) {
					// Compass passendes margin setzen, abhängig von Höhe Actionbar
					int height = getSherlock().getActionBar().getHeight();
					if (height > 0) {
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
					} else {
						listHandler.sendEmptyMessageDelayed(1, 100);
					}
				} else if (msg.what == 2) {
					if (egoPerspektive) {
						myOpenMapView.setMapOrientation((float) Rechnung.azimuth * (-1));
					}
					listHandler.sendEmptyMessageDelayed(2, 10);
				} else if (msg.what == 3) {
					finish(); // used by Settings to change to GoogleMapActivity
				} else if (msg.what == 4) {
					listHandler.removeMessages(0);
					positionsUpdate();
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
					// initialize Autocorrect oder starte neu durch settings-änderung falls nötig
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
				} else if (msg.what == 12) {
					// message from Locationer
					ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
					mProgressBar.setVisibility(View.GONE);
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

	private void positionsUpdate() {
		if (firstPositionFound) {
			int latE6 = (int) (Rechnung.startLat * 1E6);
			int lonE6 = (int) (Rechnung.startLon * 1E6);
			// Log.d("egal", "Setze Position: " + latE6 + " und " + lonE6);

			// myItemizedOverlay[0].getItem(0).getPoint().setLatitudeE6(latE6);
			// myItemizedOverlay[0].getItem(0).getPoint().setLongitudeE6(lonE6);

			myLocationOverlay.setLocation(new GeoPoint(latE6, lonE6));
			myLocationOverlay.setBearing((float) Rechnung.azimuth);
			myOpenMapView.invalidate();
			if (followMe) {
				myMapController.setCenter(new GeoPoint(latE6, lonE6));
			}
			// try {
			// if (hellerPunkt || zoomLevel != altesZoomLevel) {
			// altesZoomLevel = zoomLevel;
			// hellerPunkt = false;

			// aktuellerMarker[0].setVisible(false);

			// if (zoomLevel < 15) {
			// setPosition(Rechnung.startLat, Rechnung.startLon, R.drawable.ic_maps_indicator_current_position_ohne);
			// }else if (zoomLevel >= 16 && zoomLevel < 16) {
			// setPosition(Rechnung.startLat, Rechnung.startLon, R.drawable.ic_maps_indicator_current_position_k1);
			// } else if (zoomLevel >= 17 && zoomLevel < 18) {
			// setPosition(Rechnung.startLat, Rechnung.startLon, R.drawable.ic_maps_indicator_current_position_g1);
			// } else if (zoomLevel >= 18 && zoomLevel < 19) {
			// setPosition(Rechnung.startLat, Rechnung.startLon, R.drawable.ic_maps_indicator_current_position_g2);
			// } else if (zoomLevel >= 19) {
			// setPosition(Rechnung.startLat, Rechnung.startLon, R.drawable.ic_maps_indicator_current_position_ohne);
			// }
			// }
			//
			// if (Rechnung.schrittZaehler != schrittZaehlerAlt) {
			// schrittZaehlerAlt = Rechnung.schrittZaehler;
			//
			// aktuellerMarker[0].setVisible(false);
			//
			// if (zoomLevel < 17) {
			// if (jetzt % 2 != 0) {
			// current_position_ohne.setPosition(newPos);
			// current_position_ohne.setVisible(true);
			// aktuellerMarker[0] = current_position_ohne;
			// hellerPunkt = false;
			// } else {
			// current_position_anim_ohne.setPosition(newPos);
			// current_position_anim_ohne.setVisible(true);
			// aktuellerMarker[0] = current_position_anim_ohne;
			// hellerPunkt = true;
			// }
			// } else if (zoomLevel >= 17 && zoomLevel < 18) {
			// if (jetzt % 2 != 0) {
			// current_position_k1.setPosition(newPos);
			// current_position_k1.setVisible(true);
			// aktuellerMarker[0] = current_position_k1;
			// hellerPunkt = false;
			// } else {
			// current_position_anim_k1.setPosition(newPos);
			// current_position_anim_k1.setVisible(true);
			// aktuellerMarker[0] = current_position_anim_k1;
			// hellerPunkt = true;
			// }
			// } else if (zoomLevel >= 18 && zoomLevel < 19) {
			// if (jetzt % 2 != 0) {
			// current_position.setPosition(newPos);
			// current_position.setVisible(true);
			// aktuellerMarker[0] = current_position;
			// hellerPunkt = false;
			// } else {
			// current_position_anim.setPosition(newPos);
			// current_position_anim.setVisible(true);
			// aktuellerMarker[0] = current_position_anim;
			// hellerPunkt = true;
			// }
			// } else if (zoomLevel >= 19 && zoomLevel < 20) {
			// if (jetzt % 2 != 0) {
			// current_position_g1.setPosition(newPos);
			// current_position_g1.setVisible(true);
			// aktuellerMarker[0] = current_position_g1;
			// hellerPunkt = false;
			// } else {
			// current_position_anim_g1.setPosition(newPos);
			// current_position_anim_g1.setVisible(true);
			// aktuellerMarker[0] = current_position_anim_g1;
			// hellerPunkt = true;
			// }
			// } else if (zoomLevel >= 20) {
			// if (jetzt % 2 != 0) {
			// current_position_g2.setPosition(newPos);
			// current_position_g2.setVisible(true);
			// aktuellerMarker[0] = current_position_g2;
			// hellerPunkt = false;
			// } else {
			// current_position_anim_g2.setPosition(newPos);
			// current_position_anim_g2.setVisible(true);
			// aktuellerMarker[0] = current_position_anim_g2;
			// hellerPunkt = true;
			// }
			// }
			//
			// }
			// } catch (Exception e) {
			// if (Config.debugModus)
			// e.printStackTrace();
			// }

		}

		listHandler.sendEmptyMessageDelayed(4, POS_UPDATE_FREQ);
	}

	@Override
	protected void onResume() {
		SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		
		Config.usingGoogleMaps = false;
		try {
			egoPerspektive = false;
			compassStatus = 1;
			ImageView compass = (ImageView) findViewById(R.id.osmNadel);
			compass.setImageResource(R.drawable.needle);

		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}

		if (userSchaltetGPS == false) {
			// Log.d("Location-Status","Positionstask EINGESCHALTET weil onResume 1");
			restartListener();

			if (bekannterPauseGrund == true) {
				// User kommt gerade aus Einstellungen, Hintergrunddienst oder
				// Info-Seite
				bekannterPauseGrund = false;
			} else {
				// User ruft onResume auf, also wohl bildschirm abgeschaltet, also GPS-Position holen
				mLocationer.startLocationUpdates();
			}

			// if (Config.debugModus == true) {
			// TextView mapText = (TextView) findViewById(R.id.mapText);
			// mapText.setVisibility(0);
			// }

			Rechnung.schrittZaehler = Rechnung.schrittZaehler + schrittSpeicher;
			schrittSpeicher = 0;

			if (Rechnung.startLat != 0) {
				// TODO
				// startLatLng = new LatLng(Rechnung.startLat, Rechnung.startLon);
				listHandler.sendEmptyMessageDelayed(4, POS_UPDATE_FREQ);
			}
			if (Smartgeo.sGeoLat != 0) {
				// TODO
				// startLatLng = new LatLng(Smartgeo.sGeoLat, Smartgeo.sGeoLon);
			}

			// Export
			boolean export = settings.getBoolean("export", false);
			try {
				mRechnung.schreibeLog(export);
			} catch (Exception e) {
				if (Config.debugModus)
					e.printStackTrace();
			}
			if (export == true) {
				exportBenutzt = true;
			}

			// schrittZaehlerAlt = Rechnung.schrittZaehler - 1;
		} else if (userSchaltetGPS == true) {
			// User wurde vorher von SmartNavi in die Einstellungen geschickt,
			// hat also viell GPS eingeschaltet, also suchen :)
			mLocationer.startLocationUpdates();
		}

		// CompassNadel und ProgressBar nach unten setzen
		listHandler.sendEmptyMessageDelayed(1, 10);
		
		String tileProviderName = settings.getString("MapSource", "MapQuestOSM");
		if(tileProviderName.equalsIgnoreCase("MapQuestOSM")){
			// in the following line the Zoom-Level could be raised from 19 to 20, but tiles are currently not reloading when map is moving
			myOpenMapView.setTileSource(new XYTileSource("MapquestOSM", ResourceProxy.string.mapquest_osm, 0, 19, 256, ".jpg", new String[] {
					"http://otile1.mqcdn.com/tiles/1.0.0/map/", "http://otile2.mqcdn.com/tiles/1.0.0/map/", "http://otile3.mqcdn.com/tiles/1.0.0/map/",
			"http://otile4.mqcdn.com/tiles/1.0.0/map/" }));			
		}else{
			try {
				ITileSource tileSource = TileSourceFactory.getTileSource("Mapnik"); //do not change this string
				myOpenMapView.setTileSource(tileSource);
			} catch (IllegalArgumentException e) {
				myOpenMapView.setTileSource(TileSourceFactory.MAPNIK);
			}			
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
			ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
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

	public void setFollowOn() {
		ImageView compass = (ImageView) findViewById(R.id.osmNadel);
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
		// TODO schrittZaehlerAlt = schrittZaehlerAlt - 1;
	}

	public void setFollowOff() {
		ImageView compass = (ImageView) findViewById(R.id.osmNadel);
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

	public void compassNadelOsm(final View view) {

		// Falls off, wird das longPressMenu invisible gemacht TODO
		// try {
		// liste = (ListView) findViewById(R.id.liste);
		// liste.setVisibility(View.INVISIBLE);
		// } catch (Exception e) {
		// if(Config.debugModus)
		// e.printStackTrace();
		// }

		ImageView compass = (ImageView) findViewById(R.id.osmNadel);

		listHandler.removeMessages(0);
		listHandler.removeMessages(2);
		// Log.d("Location-Status",
		// "Positionstask AUS        compassNadel");

		// if (followMe == false && touchHatFollowAbgeschaltet == true) {
		// touchHatFollowAbgeschaltet = false;
		// if (compassStatus == 4) {
		// // status auf 3
		// compass.setImageResource(R.drawable.needle3);
		// compassStatus = 3;
		// } else if (compassStatus == 2) {
		// // status auf 1
		// compass.setImageResource(R.drawable.needle);
		// compassStatus = 1;
		// }
		// // Unbedingt erst die if abfrage und DANN setFollowOn
		// setFollowOn();
		// }
		//
		// else
		if (compassStatus == 1) {
			// status auf 3
			compass.setImageResource(R.drawable.needle3);
			compassStatus = 3;
			// Kamera in Kompass-Richtung drehen
			egoPerspektive = true;
			listHandler.sendEmptyMessage(2);
		} else if (compassStatus == 3) {
			// status auf 1
			egoPerspektive = false;
			compass.setImageResource(R.drawable.needle);
			compassStatus = 1;
			// Kamera in nach Norden ausrichten
			myOpenMapView.setMapOrientation(0);
		} else if (compassStatus == 4) {
			// status auf 3
			setFollowOn();
		} else if (compassStatus == 2) {
			// status auf 1
			setFollowOn();
		}
		// Positionstask wieder anwerfen
		listHandler.sendEmptyMessageDelayed(4, 50);
	}

	private void restartListener() {
		// // Log.d("Location-Status", "Sensoren gestartet.");
		einheiten = 0;
		accelEinheiten = 0;
		magnEinheiten = 0;
		startTime = System.nanoTime();
		try {
			// Man könnte auch direkt das delay angeben, aber dann läuft App
			// erst ab Android 2.3
			mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
			mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
	}

	private class anfrageTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {

			HttpRequests httpStats = new HttpRequests();
			httpStats.setURL(Config.VSERVER_API_URL);
			httpStats.setHeader(Config.VSERVER_API_HEADER);

			// TODO muss noch osm und googlemap tracken

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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onDestroy() {
		mSensorManager.unregisterListener(this);
		listHandler.removeMessages(0);
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		myOpenMapView.getTileProvider().clearTileCache();
		super.onStop();
	}

	private void restartListenerLight() {
		// // Log.d("Location-Status", "Sensoren wieder gestartet.");
		try {
			mSensorManager.unregisterListener(OsmMapActivity.this);
			mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
			mSensorManager.registerListener(OsmMapActivity.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
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
			verstrZeit = System.nanoTime() - startTime;
			accelEinheiten++;
			einheiten++;

			if (verstrZeit >= 2000000000) {
				mRechnung.changeDelay(accelEinheiten / 2, 0);
				mRechnung.changeDelay(magnEinheiten / 2, 1);
				// Log.d("egal", "verstrZeit = 2000; aclFreq = " +accelEinheiten/2 + " magnFreq = " + magnEinheiten/2);
				accelEinheiten = magnEinheiten = 0;
				startTime = System.nanoTime();
			}

			if (hintergrundAn == true && einheiten % 50 == 0) {
				Smartgeo.neueFakePosition();
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
			break;
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		listHandler.removeMessages(0);
		listHandler.removeMessages(2);

		if (setzePositionVonHand && event.getAction() == MotionEvent.ACTION_DOWN) {
			IGeoPoint g = (IGeoPoint) myOpenMapView.getProjection().fromPixels((int) event.getX(), (int) event.getY()); // TODO Hier hab ich (int) gemacht
		}

		if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
			touchHatFollowAbgeschaltet = true;
			if (followMe == true) {
				setFollowOff();
			}
		} else if (egoPerspektive) {
			listHandler.sendEmptyMessage(2);
		}

		if (listeIstVisible) {
			liste.setVisibility(View.GONE);
			listeIstVisible = false;
		}

		return super.onTouchEvent(event);
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
					userSchaltetGPS = true;
				} catch (android.content.ActivityNotFoundException ae) {
					startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
					userSchaltetGPS = true;
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

	// bei Touch auf ProgressBar
	public void abortGPS(final View view) {
		// GPS abbrechen
		// Abort GPS was pressed (ProgressBar was pressed)
		try {
			mLocationer.deactivateLocationer();
			ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBarOsm);
			mProgressBar.setVisibility(View.GONE);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		Toast.makeText(this, getResources().getString(R.string.tx_82), Toast.LENGTH_SHORT).show();
	}

	public void tutorialKram() {
		// einblenden und Kartenclicks und Longpress deaktivieren
		myOpenMapView.setClickable(false);
		tutorialOverlay = (View) findViewById(R.id.tutorialOverlayOsm);
		tutorialOverlay.setVisibility(View.VISIBLE);

		SharedPreferences settings = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
		String schrittLaengeString = settings.getString("step_length", null);
		Spinner spinner = (Spinner) findViewById(R.id.tutorialSpinnerOsm);
		// Create an ArrayAdapter using the string array and a default spinner
		// layout
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
					EditText editText = (EditText) findViewById(R.id.tutorialEditTextOsm);
					editText.setText("" + gespeicherteGroesse);
					spinner.setSelection(0);
				} else if (gespeicherteGroesse < 95 && gespeicherteGroesse > 45) {
					EditText editText = (EditText) findViewById(R.id.tutorialEditTextOsm);
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

		Button startButton = (Button) findViewById(R.id.startbuttonOsm);
		startButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				boolean tutorialAbgeschlossen = false;
				final EditText koerperFeld = (EditText) findViewById(R.id.tutorialEditTextOsm);
				int op = koerperFeld.length();
				float number;
				if (op != 0) {
					try {
						number = Float.valueOf(koerperFeld.getText().toString());
						if (number < 241 && number > 119 && metrischeAngaben == true) {

							String numberString = df0.format(number);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
								new schreibeSettings("step_length", numberString).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							} else {
								new schreibeSettings("step_length", numberString).execute();
							}
							Rechnung.schrittLaenge = (float) (number / 222);
							tutorialAbgeschlossen = true;
						} else if (number < 95 && number > 45 && metrischeAngaben == false) {

							String numberString = df0.format(number);
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
								new schreibeSettings("step_length", numberString).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
							} else {
								new schreibeSettings("step_length", numberString).execute();
							}
							Rechnung.schrittLaenge = (float) (number * 2.54 / 222);
							tutorialAbgeschlossen = true;
						} else {
							Toast.makeText(OsmMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
						}

					} catch (NumberFormatException e) {
						if (Config.debugModus) {
							e.printStackTrace();
						}
						Toast.makeText(OsmMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_32), Toast.LENGTH_LONG).show();
					}
				} else {
					Toast.makeText(OsmMapActivity.this, getApplicationContext().getResources().getString(R.string.tx_10), Toast.LENGTH_LONG).show();
				}

				if (tutorialAbgeschlossen) {
					// Tutorial ausblenden
					tutorialOverlay = (View) findViewById(R.id.tutorialOverlayOsm);
					tutorialOverlay.setVisibility(View.INVISIBLE);
					// Karte klickbar machen
					myOpenMapView.setClickable(true);
					// LongPressDialog
					showLongPressDialog();
				}
			}
		});

		EditText groessenFeld = (EditText) findViewById(R.id.tutorialEditTextOsm);
		groessenFeld.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_NEXT) {
					try {
						InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
						EditText groessenFeld = (EditText) findViewById(R.id.tutorialEditText);
						groessenFeld.setFocusableInTouchMode(false); // Workaround:
																		// Zeiger
																		// aus
																		// Textfeld
																		// nehmen
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

	private void showLongPressDialog() {
		try {
			myItemizedOverlay[0].getItem(0).getDrawable().setVisible(false, true);
		} catch (Exception e) {
			if (Config.debugModus)
				e.printStackTrace();
		}
		final View longPressDialog = (View) findViewById(R.id.longpPressDialogOsm);
		longPressDialog.setVisibility(View.VISIBLE);

		Button longPressButtonOsm = (Button) findViewById(R.id.longPressButtonOsm);
		longPressButtonOsm.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				longPressDialog.setVisibility(View.GONE);
				try {
					myItemizedOverlay[0].getItem(0).getDrawable().setVisible(true, true);
				} catch (Exception e) {
					if (Config.debugModus)
						e.printStackTrace();
				}
			}
		});
		// Merken dass Longpress gezeigt wurde
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			new schreibeSettings("longPressWasShown", true).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			new schreibeSettings("longPressWasShown", true).execute();
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
		final View appRateDialog = (View) findViewById(R.id.appRateDialogOsm);
		appRateDialog.setVisibility(View.VISIBLE);

		Button rateButton1 = (Button) findViewById(R.id.rateButtonOsm);
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

		// Button rateButton2 = (Button) findViewById(R.id.rateButtonOsm);
		// rateButton2.setOnClickListener(new OnClickListener() {
		// @Override
		// public void onClick(View v) {
		// appRateDialog.setVisibility(View.INVISIBLE);
		// new schreibeSettings("dontshowagain", true);
		// }
		// });

		Button rateButton3 = (Button) findViewById(R.id.rateButton2Osm);
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
		final View appRateDialog = (View) findViewById(R.id.appRateDialogOsm);
		appRateDialog.setVisibility(View.INVISIBLE);
		new schreibeSettings("dontshowagain", true).execute();
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + Config.APP_PNAME)));
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

		// menu.add(0, 1, 1, getApplicationContext().getResources().getString(R.string.tx_03)).setIcon(R.drawable.ic_menu_search_holo_dark)
		// .setActionView(searchView).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

		//menu.add(0, 1, 1, getApplicationContext().getResources().getString(R.string.tx_03)).setIcon(R.drawable.ic_menu_search_holo_dark);

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
				listeIstVisible = false;
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
			listeIstVisible = false;
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
			// schritte speichern um sie später wenn man wieder zu Karte
			// zurückkommt, mit
			// denen zusammenzurechnen, die im Hintergrunddienst gemacht wurden,
			// das ist nur für sendeDaten
			bekannterPauseGrund = true;
			schrittSpeicher = Rechnung.schrittZaehler;
			Intent myIntent = new Intent(this, Smartgeo.class);
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
			// zurück und finish
			finish();
			return (true);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean singleTapConfirmedHelper(GeoPoint p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean longPressHelper(GeoPoint p) {
		listeIstVisible = false;
		longPressedGeoPoint = p;
		Log.d("egal", "longPress");
		liste = (ListView) findViewById(R.id.listeOsm);
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
		return false;
	}

}
