package com.ilm.sandwich;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.ilm.sandwich.helferklassen.Rechnung;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class Smartgeo extends SherlockActivity {

	static Location			loc;
	static Location			loc2;
	// die beiden sind nur dazu da, das Karte aktuelle Positionen bekommt wenn man hier rausgeht.
	public static double	sGeoLat;				
	public static double	sGeoLon;
	static String			mocLocationProvider;
	static String			mocLocationNetworkProvider;
	static LocationManager	geoLocationManager;
	public static int		schritte	= 0;
	Notification			notification;
	private boolean			sollStarten	= true;
	Button					serviceButton;
	NotificationManager		notificationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.smartgeo);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Config.serviceGesehen = true;

		geoLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		//Sensoren sofort wieder starten
		try {
			if(Config.usingGoogleMaps){
				GoogleMapActivity.listHandler.sendEmptyMessage(10);
			}else{
				OsmMapActivity.listHandler.sendEmptyMessage(10);
			}
		}catch(Exception e){
			e.printStackTrace();
			//Bug der bei manchen Ger�ten auftritt
			//dann wird abgebrochen und zurück zur Karte
			Toast.makeText(this, "Unfortunately the background service is not supported on your device.", Toast.LENGTH_LONG).show();
			try {
				geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
			} catch (Exception e2) {
				// e.printStackTrace();
			}
			try {
				geoLocationManager.clearTestProviderEnabled(mocLocationProvider);
			} catch (Exception e3) {
				// e.printStackTrace();
			}
			try {
				geoLocationManager.removeTestProvider(mocLocationProvider);
			} catch (Exception e4) {
				// e.printStackTrace();
			}
			try{
				notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.cancelAll();				
			}catch(Exception e5){
				// e.printStackTrace();
			}
			finish();
		}
		serviceButton = (Button) findViewById(R.id.button1);
		serviceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (sollStarten == true) {
					starte();
				} else {
					stop();
					sollStarten = true;
					serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
				}
			}
		});

		if (sollStarten == true) {
			serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
		} else {
			serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));
		}
	}

	public static void pausiereFakeProvider(){
		//Log.d("Location-Status","pausiere Fake Provider");
		try {
			geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
			geoLocationManager.removeTestProvider(mocLocationProvider);
			geoLocationManager.clearTestProviderEnabled(mocLocationProvider);
			
			geoLocationManager.setTestProviderEnabled(mocLocationNetworkProvider, false);
			geoLocationManager.removeTestProvider(mocLocationNetworkProvider);
			geoLocationManager.clearTestProviderEnabled(mocLocationNetworkProvider);
		} catch (Exception e) {
			//e.printStackTrace();
		}
	}

	public static void reaktiviereFakeProvider() {
		//Log.d("Location-Status","reaktiviere Fake Provider");
		try {
			mocLocationProvider = LocationManager.GPS_PROVIDER;
			geoLocationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 0, 5);
			geoLocationManager.setTestProviderEnabled(mocLocationProvider, true);
			
			mocLocationNetworkProvider = LocationManager.NETWORK_PROVIDER;
			geoLocationManager.addTestProvider(mocLocationNetworkProvider, false, false, false, false, true, true, true, 1, 5);
			geoLocationManager.setTestProviderEnabled(mocLocationNetworkProvider, true);
		}catch(Exception e){
		//	e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	public void starte() {
		
		Config.hintergrundStartzeit = System.currentTimeMillis();

		mocLocationProvider = LocationManager.GPS_PROVIDER;
		mocLocationNetworkProvider = LocationManager.NETWORK_PROVIDER;
		try {
			geoLocationManager.addTestProvider(mocLocationProvider, false, false, false, false, true, true, true, 1, 5);
			geoLocationManager.setTestProviderEnabled(mocLocationProvider, true);
			
			geoLocationManager.addTestProvider(mocLocationNetworkProvider, false, false, false, false, true, true, true, 1, 5);
			geoLocationManager.setTestProviderEnabled(mocLocationNetworkProvider, true);

			notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			notification = new Notification(R.drawable.stats, getApplicationContext().getResources().getString(R.string.tx_72), System.currentTimeMillis());
			Intent intent = new Intent(this, Smartgeo.class);
			PendingIntent activity = PendingIntent.getActivity(this, 0, intent, 0);
			notification.flags = Notification.FLAG_ONGOING_EVENT;
			notification.setLatestEventInfo(this, "SmartNavi", getApplicationContext().getResources().getString(R.string.tx_73), activity);
			notificationManager.notify(0, notification);
			serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));

			sollStarten = false;
			serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_69));
			Config.hintergrundAn = true;
			
			
			// der Karte bescheid sagen, dass sie nach 10sek restartlistener etc. machen soll
			// weil andere Anwendungen die Sensorenrate sonst reduzieren
			if(Config.usingGoogleMaps){
				GoogleMapActivity.listHandler.sendEmptyMessage(9);
			}else{
				OsmMapActivity.listHandler.sendEmptyMessage(9);
			}

			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
		} catch (SecurityException sece) {
			final Dialog dialog1 = new Dialog(Smartgeo.this);
			dialog1.setContentView(R.layout.dialog1);
			dialog1.setTitle(getApplicationContext().getResources().getString(R.string.tx_44));
			dialog1.setCancelable(true);
			dialog1.show();

			Button cancel2 = (Button) dialog1.findViewById(R.id.dialogCancelMock);
			cancel2.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					dialog1.dismiss();
				}
			});

			Button settings2 = (Button) dialog1.findViewById(R.id.dialogSettingsMock);
			settings2.setOnClickListener(new OnClickListener() {
				public void onClick(View arg0) {
					try {
						startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
					} catch (android.content.ActivityNotFoundException ae) {
						try {
							startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS));
						} catch (android.content.ActivityNotFoundException ae2) {
							try {
								startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
							} catch (android.content.ActivityNotFoundException e) {
								// e.printStackTrace();
							}

						}
					}

					dialog1.dismiss();
				}
			});
			serviceButton.setText(getApplicationContext().getResources().getString(R.string.tx_74));
		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
		}

	}

	public void stop() {
		//die Handler für das resetten der Sensoren sollen ausgemacht werden bzw. zurückgerufen werden
		if(Config.usingGoogleMaps){
			GoogleMapActivity.listHandler.removeMessages(10);
		}else{
			OsmMapActivity.listHandler.removeMessages(10);
		}

		Config.hintergrundEndzeit = System.currentTimeMillis();
		Config.hintergrundAn = false;

		try {
			geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		try {
			geoLocationManager.clearTestProviderEnabled(mocLocationProvider);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		try {
			geoLocationManager.removeTestProvider(mocLocationProvider);
		} catch (Exception e) {
			// e.printStackTrace();
		}

		notificationManager.cancelAll();
		Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_70), Toast.LENGTH_LONG).show();
		finish();
	}

	@Override
	public void onBackPressed() {

		Config.hintergrundEndzeit = System.currentTimeMillis();

		if (sollStarten == false) {
			stop();
		}
		super.onBackPressed();
	}

	@SuppressLint("NewApi") 
	public static void neueFakePosition() {
		// Positionen in Variablen schreiben, damit Karte die aufnehmen kann wenn es wieder aufgerufen wird
		sGeoLat = Rechnung.startLat;
		sGeoLon = Rechnung.startLon;

		schritte = Rechnung.schrittZaehler;
		//GPS
		loc = new Location(mocLocationProvider);
		loc.setAccuracy(12);
		loc.setAltitude(Rechnung.altitude);
		loc.setLatitude(Rechnung.startLat);
		loc.setLongitude(Rechnung.startLon);
		loc.setProvider(mocLocationProvider);
		loc.setSpeed(0.8f);
		loc.setBearing((float) Rechnung.azimuth);
		loc.setTime(System.currentTimeMillis());
		try{
			try{
				loc.setElapsedRealtimeNanos(System.currentTimeMillis());			
			}catch(NoSuchMethodError e){
				//e.printStackTrace();
			}
		}catch(Exception e){
			//egal
		}
		try {
			geoLocationManager.setTestProviderLocation(mocLocationProvider, loc);
		} catch (Exception e) {
			//e.printStackTrace();
		}

		//Network
		loc2 = new Location(mocLocationNetworkProvider);
		loc2.setAccuracy(12.0f);
		loc2.setAltitude(Rechnung.altitude);
		loc2.setLatitude(Rechnung.startLat);
		loc2.setLongitude(Rechnung.startLon);
		loc2.setProvider(mocLocationNetworkProvider);
		loc2.setSpeed(0.8f);
		loc2.setBearing((float) Rechnung.azimuth);
		loc2.setTime(System.currentTimeMillis());
		try{
			try{
				loc2.setElapsedRealtimeNanos(System.currentTimeMillis());			
			}catch(NoSuchMethodError e){
				//e.printStackTrace();
			}
		}catch(Exception e){
			//egal
		}
		try {
			geoLocationManager.setTestProviderLocation(mocLocationNetworkProvider, loc2);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		
		if (Config.debugModus) {
			//Log.i("Frequenzen", loc.toString());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Config.hintergrundEndzeit = System.currentTimeMillis();
			if (sollStarten == false) {
				try {
					geoLocationManager.setTestProviderEnabled(mocLocationProvider, false);
					geoLocationManager.removeTestProvider(mocLocationProvider);
					geoLocationManager.clearTestProviderEnabled(mocLocationProvider);
				} catch (Exception e) {
					e.printStackTrace();
				}
				notificationManager.cancelAll();
				Toast.makeText(this, getApplicationContext().getResources().getString(R.string.tx_70), Toast.LENGTH_LONG).show();
			}
			finish();
			return (true);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

}
