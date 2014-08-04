package com.ilm.sandwich.helferklassen;

import java.util.Iterator;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.ilm.sandwich.Config;
import com.ilm.sandwich.GoogleMapActivity;
import com.ilm.sandwich.OsmMapActivity;
import com.ilm.sandwich.R;
import com.ilm.sandwich.Smartgeo;
import com.ilm.sandwich.TouchableWrapper.UpdateMapAfterUserInterection;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorEventListener;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Locationer implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, com.google.android.gms.location.LocationListener{

	private int sichtbareSatelliten = 0;
	private Handler mHandler = new Handler();
	private int erlaubterErrorGPS = 10;
	private boolean autoCorrectErfolgreich = true;
	private int zusatzSekundenAutocorrect = 0;
	private boolean goennGpsNochmalEtwasZeit = true;
	public static double startLat;
	public static double startLon;
	public static double altitude = 100;
	public static double errorGPS;
	public static float lastErrorGPS = 9999999999.0f;
	
	private LocationClient mLocationClient;
	private LocationRequest highRequest;
	private long lastLocationTime = 0L;
	private LocationManager mLocationManager;
	
	private Context mContext;

	public Locationer(Context context) {
		super();
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		mContext = context;
	}
	
	public void deactivateLocationer(){
		try {
			if (mLocationClient.isConnected()) {
				mLocationClient.removeLocationUpdates(this);
				mLocationClient.disconnect();
			}
		} catch (Exception e) {
			//nothing
		}
		mLocationManager.removeUpdates(this);
	}

	public void startLocationUpdates(){
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
		if (status != ConnectionResult.SUCCESS) {
			try {
				mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10, 0, this);
				mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10, 0, this);
				mLocationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 10, 0, this);
			} catch (Exception e) {
				if(Config.debugModus)
					e.printStackTrace();
			}
		}else{
			// holen der ersten Position
			mLocationClient = new LocationClient(mContext, this, this);
			if(mLocationClient.isConnected() == false && mLocationClient.isConnecting() == false){
				mLocationClient.connect();				
			}
		}	
	}
	
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

	private void updateSats() {
		final GpsStatus gs = this.mLocationManager.getGpsStatus(null);
		int i = 0;
		final Iterator<GpsSatellite> it = gs.getSatellites().iterator();
		while (it.hasNext()) {
			it.next();
			i += 1;
		}
		// Log.d("Location-Status", "Satelites in range: " + i);
		sichtbareSatelliten = i;
	}


	
	// LocationClient
	// **************

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		// Log.d("Location-Status","LocationClient: Connection FAILED" + arg0.getErrorCode());
		//TODO Google Play Services not available on this device
		//mapText.append("Connection Problem with Google Play Services. \nPlease make sure you have Google Play Services installed.\nErrorCode: "
		//		+ arg0.getErrorCode());
	}

	@Override
	public void onConnected(Bundle arg0) {
		// Log.d("Location-Status", "LocationClient: connected");
		try {
			Location lastLocation = mLocationClient.getLastLocation();
			double startLat = lastLocation.getLatitude();
			double startLon = lastLocation.getLongitude();
			lastErrorGPS = lastLocation.getAccuracy();
			double altitude = lastLocation.getAltitude();
			lastLocationTime = lastLocation.getTime();
			double mittellat = startLat * 0.01745329252;
			double abstandLaengengrade = 111.3D * Math.cos(mittellat);

			Rechnung.initialize(startLat, startLon, abstandLaengengrade, altitude, lastErrorGPS);
			highRequest = new LocationRequest();
			highRequest.setExpirationDuration(40000).setInterval(10).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			mLocationClient.requestLocationUpdates(highRequest, this);
			if(Config.usingGoogleMaps){
				GoogleMapActivity.listHandler.sendEmptyMessage(0);
				// nach 40sek automatisch location updates removen
				mHandler.postDelayed(deaktivateTask, 40000);			
			}else{
				OsmMapActivity.listHandler.sendEmptyMessage(0);
			}

		} catch (Exception e) {
			LocationManager locManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
			boolean locationEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			if (locationEnabled == false) {
				// es wurde noch nie eine Position abgerufen oder Location Services sind deaktiviert, also auffordern zum aktivieren
				if(Config.usingGoogleMaps){
					GoogleMapActivity.listHandler.sendEmptyMessage(5);			
				}else{
					OsmMapActivity.listHandler.sendEmptyMessage(5);
				}
			} else {
				// location services sind aktiviert aber es gab noch nie eine position
				// also mit 0,0 anfangen und hoffen das bald eine kommt
				double startLat = 0;
				double startLon = 0;
				lastErrorGPS = 1000000;
				double altitude = 0;
				lastLocationTime = System.currentTimeMillis() - 1000000;
				double mittellat = startLat * 0.01745329252;
				double abstandLaengengrade = 111.3 * Math.cos(mittellat);
				Rechnung.initialize(startLat, startLon, abstandLaengengrade, altitude, lastErrorGPS);
				
				if(Config.usingGoogleMaps){
					GoogleMapActivity.listHandler.sendEmptyMessage(0);
					// nach 40sek automatisch location updates removen
					mHandler.postDelayed(deaktivateTask, 40000);			
				}else{
					OsmMapActivity.listHandler.sendEmptyMessage(0);
				}
			}

		}

	}

	@Override
	public void onDisconnected() {
		// kommt nur bei einem Problem
		//TODO 
		//TextView mapText = (TextView) findViewById(R.id.mapText);
		//mapText.setVisibility(0);
		//mapText.append("Connection Problem with Google Play Services. \nPlease make sure you have Google Play Services installed.");
	}

	@Override
	public void onLocationChanged(Location location) {
		
		long differenzTime = location.getTime() - lastLocationTime;
		double differenzError = lastErrorGPS - location.getAccuracy();
		if (differenzTime > 30000 || differenzError > 7) {
			
			double startLat = location.getLatitude();
			double startLon = location.getLongitude();
			lastErrorGPS = location.getAccuracy();
			double altitude = location.getAltitude();
			lastLocationTime = location.getTime();
			double mittellat = startLat * 0.01745329252;
			double abstandLaengengrade = 111.3D * Math.cos(mittellat);

			Rechnung.initialize(startLat, startLon, abstandLaengengrade, altitude, lastErrorGPS);

			if(Config.usingGoogleMaps){
				GoogleMapActivity.setPosition(true);				
			}else{
				OsmMapActivity.listHandler.sendEmptyMessage(14);
			}

			if (location.getAccuracy() < 13) {
				try {
					deactivateLocationer();
					if(Config.usingGoogleMaps){
						GoogleMapActivity.listHandler.sendEmptyMessage(12);										
					}else{
						OsmMapActivity.listHandler.sendEmptyMessage(12);	
					}
				} catch (Exception e) {
					if(Config.debugModus)
						e.printStackTrace();
				}
			}

			lastLocationTime = location.getTime();
			lastErrorGPS = location.getAccuracy();
		} else {
			//for debug purposes
		}

	}

	@Override
	public void onProviderDisabled(String provider) {
		SharedPreferences settings = mContext.getSharedPreferences(mContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);

		if (settings.getBoolean("gpsDialogShown", false) == false) {

			new schreibeSettings("gpsDialogShown", true).execute();
			if(Config.usingGoogleMaps){
				//show GPS Dialog
				GoogleMapActivity.listHandler.sendEmptyMessage(13);
			}else{				
				OsmMapActivity.listHandler.sendEmptyMessage(13);
			}

		}
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
	
	
	private Runnable deaktivateTask = new Runnable() {
		public void run() {
			deactivateLocationer();
		}
	};
	
	
	
	// ******************************************************************
	// ******************** AutoCorrection with GPS ******************
	// ******************************************************************

	public void starteAutocorrect() {
		if (autoCorrectErfolgreich) {
			autoCorrectErfolgreich = false;
		} else if (zusatzSekundenAutocorrect <= 30) {
			zusatzSekundenAutocorrect = zusatzSekundenAutocorrect + 7;
			erlaubterErrorGPS = erlaubterErrorGPS + 8;
			// Log.d("Location-Status", "Zeit zum abrufen:" +
			// zusatzSekundenAutocorrect + " und erlaubter Fehler: " +
			// erlaubterErrorGPS);
		}
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				100, 0, gpsAutocorrectLocationListener);
		mHandler.postDelayed(autoStopTask,
				10000 + zusatzSekundenAutocorrect * 1000);
		mHandler.postDelayed(satellitenSichtPruefung, 10000);
		mLocationManager.addGpsStatusListener(mGpsStatusListener);
		goennGpsNochmalEtwasZeit = true;
	}

	private Runnable satellitenSichtPruefung = new Runnable() {
		public void run() {
			if (sichtbareSatelliten < 5) {
				beendeAutocorrect();
				// Log.d("Location-Status", "Nicht genügend Satelliten: " +
				// sichtbareSatelliten);
			}
		}
	};

	private Runnable autoStopTask = new Runnable() {
		public void run() {
			beendeAutocorrect();
		}
	};

	public void beendeAutocorrect() {
		mLocationManager.removeGpsStatusListener(mGpsStatusListener);
		mLocationManager.removeUpdates(gpsAutocorrectLocationListener);
		mHandler.removeCallbacks(autoStopTask);
		mHandler.removeCallbacks(satellitenSichtPruefung);
		// Log.d("Location-Status", "beende Autocorrect");

		if (GoogleMapActivity.hinterGrundSollWiederAn == true) {
			Config.hintergrundAn = true;
			Smartgeo.reaktiviereFakeProvider();
		}
	}

	private LocationListener gpsAutocorrectLocationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (location.getLatitude() != 0) {
				location.getProvider();
				startLat = location.getLatitude();
				startLon = location.getLongitude();
				errorGPS = location.getAccuracy();
				if (errorGPS <= erlaubterErrorGPS) {
					// Log.d("Location-Status", "Autocorrect GPS: " +
					// location.getProvider() + " "
					// + location.getAccuracy());

					if(Config.usingGoogleMaps){
						GoogleMapActivity.listHandler.sendEmptyMessage(8);
					}else{				
						OsmMapActivity.listHandler.sendEmptyMessage(8);
					}
					
					erlaubterErrorGPS = 10;
					autoCorrectErfolgreich = true;
					zusatzSekundenAutocorrect = 0;
				} else {
					if (goennGpsNochmalEtwasZeit) {
						// Es kommen also Positionen rein, die noch zu ungenau
						// sind
						// also gibts nochmal extra Zeit, denn es wäre zu schade
						// das holen zu stoppen, wenns gegen Ende erfolgreich
						// wird
						mHandler.removeCallbacks(autoStopTask);
						mHandler.postDelayed(autoStopTask,
								10000 + zusatzSekundenAutocorrect * 1000);
						goennGpsNochmalEtwasZeit = false;
					}
					// Log.d("Location-Status",
					// "VERWORFEN: Autocorrect GPS: " + location.getProvider() +
					// " "
					// + location.getAccuracy());
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
			SharedPreferences settings = mContext.getSharedPreferences(mContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);
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
	
}
