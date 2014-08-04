package com.ilm.sandwich.helferklassen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.ilm.sandwich.Config;
import com.ilm.sandwich.GoogleMapActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.GeomagneticField;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.util.Log;

public class Rechnung {

	private static float frequenz;
	private static boolean schrittBeginn = false;
	private static float[] iMatrix = new float[9];
	private static float[] RMatrix = new float[9];
	private static float[] RMatrixRemapped = new float[9];
	private static float[] orientation = new float[3];
	public static float[] gravity = new float[3];
	public static float[] linear = new float[3];
	public static float[] origmagn = new float[3];
	public static float[] magn = new float[3];
	public static float[] origaccel = new float[3]; // werden nur gebraucht um
													// in Log Datei zu schreiben
													// zum debuggen
	public static double startLat;
	public static double startLon;
	public static int schrittZaehler = 0;
	private static double deltaLat;
	private static double deltaLon;
	private static float iSchritt = 1; // auch beim StartButton �ndern und auch
										// da lassen ;) !!!
	public static double azimuth;
	// private static double[] xv = new double[4];
	// private static double[] yv = new double[4];
	protected static double entfernung = 0;
	private static float ugainA;
	private static float ugainM;
	private static double[] xa0 = new double[4];
	private static double[] ya0 = new double[4];
	private static double[] xa1 = new double[4];
	private static double[] ya1 = new double[4];
	private static double[] xa2 = new double[4];
	private static double[] ya2 = new double[4];
	private static float[] tpA = new float[3];
	private static float[] tpM = new float[3];
	private static double[] xm0 = new double[4];
	private static double[] ym0 = new double[4];
	private static double[] xm1 = new double[4];
	private static double[] ym1 = new double[4];
	private static double[] xm2 = new double[4];
	private static double[] ym2 = new double[4];
	// private static long[] schrittx = new long[5];
	public static double schrittFrequenz = 0;
	public static double schrittFrequenzGesamt = 0;
	static int schrittFrequenzTeiler = 1;
	public static int altitude = 150;
	private static float schrittSchwelle = 2.0f;
	public static double abstandLaengengrade;
	private static boolean sensorenFileNichtVorhanden = true;
	private static boolean positionsFileNichtVorhanden = true;
	public static float schrittLaenge;
	public static boolean export;
	GeomagneticField geo;
	private static float decl = 0;
	ProgressDialog dialog;
	private static boolean initialSchritt;
	static int schrittFrequenzCounter = 0;
	protected String zielOrtErgebnis;
	static File posFile;
	static File sensorFile;
	static String zielOrt;
	private static boolean neuerSchrittDa = false;
	private static boolean habeAngefangenZuExportieren = false;
	// private static float posSinX;
	private static double azimuthUngefiltertUnKorr;
	public static double korr;
	private static float deltaSollGravity;
	// private static float sollYBeschl;
	static DecimalFormat df = new DecimalFormat("0.00");
	public static String schrittFreq = "0";
	public static int version;
	public static double kartenSichtAzimuth;
	public static float lastErrorGPS;

	// public static String schrittFrequenzGesamtString = "0";

	// -----------------------------------------------------------------------------------------------------------------
	// ------ON CREATE-------------------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------

	public Rechnung() {

		// schrittFrequenzGesamtString = "0";

		positionsFileNichtVorhanden = true;
		sensorenFileNichtVorhanden = true;

		schrittZaehler = 0;
		initialSchritt = true;

		magn[0] = magn[1] = magn[2] = gravity[0] = gravity[1] = 0;
		gravity[2] = 9.81f;
		ugainM = ugainA = 154994.3249f;
		tpA[0] = tpM[0] = 0.9273699683f;
		tpA[1] = tpM[1] = -2.8520278186f;
		tpA[2] = tpM[2] = 2.9246062355f;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// ------ENDE ON CREATE ENDE----------------------------------------------------------------------------------------
	// -----------------------------------------------------------------------------------------------------------------

	/**
	 * Initialisiert die Rechnung mit Startwerten
	 * 
	 * @param startLat
	 * @param startLon
	 * @param abstandLaengengrade
	 */
	public static void initialize(double startLat, double startLon, double abstandLaengengrade, double altitude, float lastError) {
		Rechnung.startLat = startLat;
		Rechnung.startLon = startLon;
		Rechnung.abstandLaengengrade = abstandLaengengrade;
		Rechnung.altitude = (int) altitude;
		Rechnung.lastErrorGPS = lastError;
		trueNorth();

	}

	/**
	 * Setzt die aktuelle Position entsprechend
	 * 
	 * @param lat
	 * @param lon
	 */
	public static void setLocation(double lat, double lon) {
		startLat = lat;
		startLon = lon;
	}

	public void schreibeLog(boolean sollich) {
		if (sollich) {
			export = true;
			habeAngefangenZuExportieren = true;
		} else if (habeAngefangenZuExportieren == true && sollich == false) {
			beendeLogFile();
		}
	}

	private static void trueNorth() {
		long zeeeit = System.currentTimeMillis();
		GeomagneticField geo = new GeomagneticField((float) startLat, (float) startLon, altitude, zeeeit);
		decl = geo.getDeclination();
	}

	public void imbaMagnetic(float[] magnetic) {
		// Tiefpass 0.5Hz f�r alpha0
		xm0[0] = xm0[1];
		xm0[1] = xm0[2];
		xm0[2] = xm0[3];
		xm0[3] = magnetic[0] / ugainM;
		ym0[0] = ym0[1];
		ym0[1] = ym0[2];
		ym0[2] = ym0[3];
		ym0[3] = (xm0[0] + xm0[3]) + 3 * (xm0[1] + xm0[2]) + (tpM[0] * ym0[0]) + (tpM[1] * ym0[1]) + (tpM[2] * ym0[2]);
		magn[0] = (float) ym0[3];

		// Tiefpass 0.5Hz f�r alpha1
		xm1[0] = xm1[1];
		xm1[1] = xm1[2];
		xm1[2] = xm1[3];
		xm1[3] = magnetic[1] / ugainM;
		ym1[0] = ym1[1];
		ym1[1] = ym1[2];
		ym1[2] = ym1[3];
		ym1[3] = (xm1[0] + xm1[3]) + 3 * (xm1[1] + xm1[2]) + (tpM[0] * ym1[0]) + (tpM[1] * ym1[1]) + (tpM[2] * ym1[2]);
		magn[1] = (float) ym1[3];

		// Tiefpass 0.5Hz f�r alpha2
		xm2[0] = xm2[1];
		xm2[1] = xm2[2];
		xm2[2] = xm2[3];
		xm2[3] = magnetic[2] / ugainM;
		ym2[0] = ym2[1];
		ym2[1] = ym2[2];
		ym2[2] = ym2[3];
		ym2[3] = (xm2[0] + xm2[3]) + 3 * (xm2[1] + xm2[2]) + (tpM[0] * ym2[0]) + (tpM[1] * ym2[1]) + (tpM[2] * ym2[2]);
		magn[2] = (float) ym2[3];
	}

	public void imbaGravity(float[] accel) {
		// Tiefpass 0.5Hz für alpha0
		xa0[0] = xa0[1];
		xa0[1] = xa0[2];
		xa0[2] = xa0[3];
		xa0[3] = accel[0] / ugainA;
		ya0[0] = ya0[1];
		ya0[1] = ya0[2];
		ya0[2] = ya0[3];
		ya0[3] = (xa0[0] + xa0[3]) + 3 * (xa0[1] + xa0[2]) + (tpA[0] * ya0[0]) + (tpA[1] * ya0[1]) + (tpA[2] * ya0[2]);
		gravity[0] = (float) ya0[3];

		// Tiefpass 0.5Hz f�r alpha1
		xa1[0] = xa1[1];
		xa1[1] = xa1[2];
		xa1[2] = xa1[3];
		xa1[3] = accel[1] / ugainA;
		ya1[0] = ya1[1];
		ya1[1] = ya1[2];
		ya1[2] = ya1[3];
		ya1[3] = (xa1[0] + xa1[3]) + 3 * (xa1[1] + xa1[2]) + (tpA[0] * ya1[0]) + (tpA[1] * ya1[1]) + (tpA[2] * ya1[2]);
		gravity[1] = (float) ya1[3];

		// Tiefpass 0.5Hz f�r alpha2
		xa2[0] = xa2[1];
		xa2[1] = xa2[2];
		xa2[2] = xa2[3];
		xa2[3] = accel[2] / ugainA;
		ya2[0] = ya2[1];
		ya2[1] = ya2[2];
		ya2[2] = ya2[3];
		ya2[3] = (xa2[0] + xa2[3]) + 3 * (xa2[1] + xa2[2]) + (tpA[0] * ya2[0]) + (tpA[1] * ya2[1]) + (tpA[2] * ya2[2]);
		gravity[2] = (float) ya2[3];
	}

	public void imbaLinear(float[] accel) {
		linear[0] = accel[0] - gravity[0];
		linear[1] = accel[1] - gravity[1];
		linear[2] = accel[2] - gravity[2];
	}

	public void rechneDaten() {

		SensorManager.getRotationMatrix(RMatrix, iMatrix, gravity, magn);
		SensorManager.remapCoordinateSystem(RMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, RMatrixRemapped);
		SensorManager.getOrientation(RMatrixRemapped, orientation);

		// rechneSollYBeschl();

		if (orientation[0] >= 0) {
			// Azimuth-Berechnung (von radiant in Grad)
			azimuthUngefiltertUnKorr = (orientation[0] * 57.29577951f + decl);
		} else {
			// Azimuth-Berechnung (von radiant in Grad) +360
			azimuthUngefiltertUnKorr = (orientation[0] * 57.29577951f + 360 + decl);
		}

		if (azimuthUngefiltertUnKorr >= 360) {
			azimuthUngefiltertUnKorr -= 360;
		}

		// if (android4 == false && (Karte.gravityExistiert == true ||
		// Smartgeo.gravityExistiert == true )){ //Ger�te mit 2.3.3 bekommen
		// korrekturAz
		// azimuthUngefiltert =
		// korrekturAz(azimuthUngefiltertUnKorr)/57.29577951;
		// }
		// else {
		// azimuthUngefiltert = azimuthUngefiltertUnKorr /57.29577951; //war nur
		// f�r Az Filterung, ist also nicht mehr n�tig, hat sich als ineffizient
		// herausgestellt
		// }

		azimuth = azimuthUngefiltertUnKorr;

		if (export && Config.debugModus) {
			datenausgabe();
		}
	}

	// private static void berechneSchrittfrequenz(long now) {
	//
	// schrittx[0] = schrittx[1];
	// schrittx[1] = schrittx[2];
	// schrittx[2] = schrittx[3];
	// schrittx[3] = schrittx[4];
	// schrittx[4] = now;
	//
	// if (schrittx[0] != 0) {
	// schrittFrequenzCounter++;
	// double dauerFuenfSchritte = (now - schrittx[0]) / 1000.00; // in
	// // sekunden
	// if (schrittFrequenz != 0 && schrittFrequenzCounter == 5) {
	// schrittFrequenzGesamt = (schrittFrequenzGesamt + schrittFrequenz)
	// / schrittFrequenzTeiler;
	// schrittFrequenzGesamtString = df.format(schrittFrequenzGesamt);
	// schrittFrequenzTeiler = 2;
	// schrittFrequenzCounter = 0;
	// } else {
	// schrittFrequenz = (5 / dauerFuenfSchritte);
	// }
	// schrittFreq = df.format(schrittFrequenz);
	// }
	// }

	// private double korrekturAz(double x) { // auf alle hab ich +0.6 nach Test
	// 11 und nochmal +0.5 auf die positive Werte
	// // 3. Juni: wieder 0.3 weniger auf die +werte und posSinX auch 0.1
	// weniger
	// // made by Christian Henke
	// if (azimuthUngefiltert < 180 ){
	// posSinX = 0.5f;
	// } else {
	// posSinX = 0;
	// }
	//
	// if (deltaSollGravity > 0.15 && deltaSollGravity <= 0.3){
	// korr = ((posSinX+3.8)*Math.sin((double)(3.141592653589793*x/180)));
	// }
	// else if (deltaSollGravity > 0.3 && deltaSollGravity <= 0.45){
	// korr = ((posSinX+7.0)*Math.sin((double)(3.141592653589793*x/180)));
	// } // made by Christian Henke
	// else if (deltaSollGravity > 0.45 && deltaSollGravity <= 0.55){
	// korr = ((posSinX+8.6)*Math.sin((double)(3.141592653589793*x/180)));
	// }
	// else if (deltaSollGravity > 0.55 && deltaSollGravity <= 0.75){
	// korr = ((posSinX+10.3)*Math.sin((double)(3.141592653589793*x/180)));
	// }
	// else if (deltaSollGravity > 0.75 && deltaSollGravity <= 0.95){
	// korr = ((posSinX+13.5)*Math.sin((double)(3.141592653589793*x/180)));
	// }
	// else if (deltaSollGravity > 0.95 && deltaSollGravity <= 1.25){
	// korr = ((posSinX+16.5)*Math.sin((double)(3.141592653589793*x/180)));
	// }// made by Christian Henke
	// else if (deltaSollGravity > 1.25){
	// korr = ((posSinX+20.8)*Math.sin((double)(3.141592653589793*x/180)));
	// }else{
	// korr = 0;
	// }
	// double azKorrigiert = x + korr;
	// return azKorrigiert;
	// }

	// private void rechneSollYBeschl() {
	// //Tiefpass 0.5Hz
	// xv[0] = xv[1]; xv[1] = xv[2]; xv[2] = xv[3];
	// xv[3] = orientation[1] / ugainA;
	// yv[0] = yv[1]; yv[1] = yv[2]; yv[2] = yv[3];
	// yv[3] = (xv[0] + xv[3]) + 3 * (xv[1] + xv[2]) + ( tpA[0] * yv[0]) + (
	// tpA[1] * yv[1]) + ( tpA[2] * yv[2]);
	// // made by Christian Henke
	// double bubu = (yv[3]*(-1));
	// sollYBeschl = (float)(Math.sin(bubu)*9.08665);
	// deltaSollGravity = Math.abs((sollYBeschl-gravity[1]));
	// }

	public void schrittMessung() {
		float wert = linear[2]; // Achtung linear oder henkylinear
		if (initialSchritt && wert >= schrittSchwelle) {
			// Einleitung eines zu messenden Schrittes
			initialSchritt = false;
			schrittBeginn = true;
		}
		if (schrittBeginn && iSchritt / frequenz >= 0.24f && iSchritt / frequenz <= 0.8f) {
			// Zeitfenster für Schritt zw. minZeit und maxZeit
			// Messung bis negativer Peak
			if (wert < -schrittSchwelle) {
				// Zeitfenster korrekt UND Schwelle des Gegenausschlages erreicht
				schrittZaehler++;
				schrittBeginn = false;
				iSchritt = 1;
				initialSchritt = true;
				neuerSchritt(azimuth);
				neuerSchrittDa = true;
				// berechneSchrittfrequenz(System.currentTimeMillis());
				if (export) {
					positionsAusgabe();
				}
			} else {
				// Zeitfenster korrekt aber NegativAusschlag zu gering
				iSchritt++;
			}
		} else if (schrittBeginn && iSchritt / frequenz < 0.24f) {
			// Zeitfenster für Schritt noch zu klein, also warten und iSchritt++
			iSchritt++;
		} else if (schrittBeginn && iSchritt / frequenz > 0.8f) {
			// Zeitfenster für einen Schritt abgelaufen
			schrittBeginn = false;
			initialSchritt = true;
			iSchritt = 1;
		}
	}

	private void neuerSchritt(double winkel) {
		double winkel2 = winkel * 0.01745329252;
		Log.d("Location-Status", "Schritt: " + Rechnung.startLon);
		deltaLat = Math.cos(winkel2) * 0.000008984725966 * schrittLaenge;
		// 100cm für einen Schritt wird verrechnet abhängig von Winkel auf Lat
		deltaLon = Math.sin(winkel2) / (abstandLaengengrade * 1000) * schrittLaenge;
		// 100cm für einen Schritt wird verrechnet abhängig von Winkel auf Lon

		deltaLat = Math.abs(deltaLat);
		deltaLon = Math.abs(deltaLon);
		// made by Christian Henke
		if (startLat > 0) {
			// Nutzer befindet sich auf NORDhalbkugel Latitude größer 0
			if (winkel > 270 || winkel < 90) { // Bewegung n�rdlich
				startLat += deltaLat;
			} else {
				// Bewegung südlich
				startLat -= deltaLat;
			}
		} else if (startLat < 0) {
			// Nutzer befindet sich auf SüDhalbkugel Latitude kleiner 0
			if (winkel > 270 || winkel < 90) {
				// Bewegung nördlich
				startLat += deltaLat;
			} else {
				// Bewegung südlich
				startLat -= deltaLat;
			}
		}
		if (winkel < 180) {
			// Bewegung östlich
			startLon += deltaLon;
		} else {
			// Bewegung westlich
			startLon -= deltaLon;
		}
		Log.d("Location-Status", "Schritt: " + Rechnung.startLon);
	}

	private static void positionsAusgabe() {
		try {
			File folder = new File(Environment.getExternalStorageDirectory() + "/smartnavi/");
			folder.mkdir();
			if (folder.canWrite()) {
				if (positionsFileNichtVorhanden) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.GERMAN);
					String curentDateandTime = sdf.format(new Date());
					String textname = "track_" + curentDateandTime + ".gpx";
					posFile = new File(folder, textname);
					FileWriter posWriter = new FileWriter(posFile);
					BufferedWriter out = new BufferedWriter(posWriter);

					TimeZone tz = TimeZone.getTimeZone("UTC");
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.GERMAN);
					df.setTimeZone(tz);
					String nowAsISO = df.format(new Date());

					out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx> <trk><name>SmartNavi " + nowAsISO
							+ "</name><number>1</number><trkseg>");
					out.close();
					positionsFileNichtVorhanden = false;
				} else {
					FileWriter posWriter = new FileWriter(posFile, true);
					BufferedWriter out = new BufferedWriter(posWriter);

					if (neuerSchrittDa) {
						out.newLine();

						TimeZone tz = TimeZone.getTimeZone("UTC");
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.GERMAN);
						df.setTimeZone(tz);
						String nowAsISO = df.format(new Date());

						out.write("<trkpt lat=\"" + startLat + "\" lon=\"" + startLon + "\"><time>" + nowAsISO + "</time></trkpt>");

						neuerSchrittDa = false;
					}

					out.close();
				}
			}
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}

	public static void beendeLogFile() {
		if (export && positionsFileNichtVorhanden == false) {
			try {
				FileWriter posWriter;
				posWriter = new FileWriter(posFile, true);
				BufferedWriter out = new BufferedWriter(posWriter);
				out.newLine();
				out.write("</trkseg></trk></gpx>");
				out.close();
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
		export = false;
		positionsFileNichtVorhanden = true;
		sensorenFileNichtVorhanden = true;
	}

	private static void datenausgabe() {
		try {
			File folder = new File(Environment.getExternalStorageDirectory() + "/smartnavi/");
			folder.mkdir();
			if (folder.canWrite()) {
				if (sensorenFileNichtVorhanden) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN);
					String curentDateandTime = sdf.format(new Date());
					String textname = "sensoren_" + curentDateandTime + ".csv";
					sensorFile = new File(folder, textname);
					FileWriter sensorWriter = new FileWriter(sensorFile);
					BufferedWriter outs = new BufferedWriter(sensorWriter);
					outs.write(startLat + "; " + startLon + "; " + schrittLaenge + ";" + version + "; ");
					outs.newLine();
					outs.write("origmagn0; origmagn1; origmagn2; origaccel0; origaccel1; origaccel2; "
							+ "imbamagn0; imbamagn1; imbamagn2; gravity0; gravity1; gravity2; "
							+ "azimuthUngefiltertUnKorr; korr; deltaSollGrav; azimuth; schrittFrequenz;");
					outs.close();
					sensorenFileNichtVorhanden = false;
				} else {
					FileWriter sensorWriter = new FileWriter(sensorFile, true);
					BufferedWriter outs = new BufferedWriter(sensorWriter);

					outs.newLine();

					outs.write(origmagn[0] + ";" + origmagn[1] + ";" + origmagn[2] + ";" + origaccel[0] + ";" + origaccel[1] + ";" + origaccel[2] + ";"
							+ magn[0] + ";" + magn[1] + ";" + magn[2] + ";" + gravity[0] + ";" + gravity[1] + ";" + gravity[2] + ";" + azimuthUngefiltertUnKorr
							+ ";" + korr + ";" + deltaSollGravity + ";" + azimuth + ";" + schrittFrequenz + ";");
					outs.close();
				}
			}
		} catch (IOException e) {
			// e.printStackTrace();
		}
	}

	public void changeDelay(int freq, int sensor) {
		// Tiefpassfilter 3. Ordnung - Eckfrequenz jeweils 0.3 Hz

		// Müssen auf irgendwas erstmal initialisiert werden, also einfach 50Hz
		// erstmal
		float ugain = 154994.3249f;
		float tp0 = 0.9273699683f;
		float tp1 = -2.8520278186f;
		float tp2 = 2.9246062355f;

		// Einstellen der Werte je nach Frequenz
		if (freq >= 125) {    //130
			ugain = 2662508.633f;
			tp0 = 0.9714168814f;
			tp1 = -2.9424208232f;
			tp2 = 2.9710009372f;
		} else if (freq <= 124 && freq >= 115) { //120
			ugain = 2096647.970f;
			tp0 = 0.9690721133f;
			tp1 = -2.9376603253f;
			tp2 = 2.9685843964f;
		} else if (freq <= 114 && freq >= 105) { //110
			ugain = 1617241.715f;
			tp0 = 0.9663083052f;
			tp1 = -2.9320417512f;
			tp2 = 2.9657284993f;
		} else if (freq <= 104 && freq >= 95) { //100
			ugain = 1217122.860f;
			tp0 = 0.9630021159f;
			tp1 = -2.9253101348f;
			tp2 = 2.9623014461f;
		} else if (freq <= 94 && freq >= 85) { //90
			ugain = 889124.3983f;
			tp0 = 0.9589765397f;
			tp1 = -2.9170984005f;
			tp2 = 2.9581128632f;
		} else if (freq <= 84 && freq >= 75) { //80
			ugain = 626079.3215f;
			tp0 = 0.9539681632f;
			tp1 = -2.9068581408f;
			tp2 = 2.9528771997f;
		} else if (freq <= 74 && freq >= 65) { //70
			ugain = 420820.6222f;
			tp0 = 0.9475671238f;
			tp1 = -2.8937318862f;
			tp2 = 2.9461457520f;
		} else if (freq <= 64 && freq >= 55) { //60
			ugain = 266181.2926f;
			tp0 = 0.9390989403f;
			tp1 = -2.8762997235f;
			tp2 = 2.9371707284f;
		} else if (freq <= 54 && freq >= 45) {  //50
			ugain = 154994.3249f;
			tp0 = 0.9273699683f;
			tp1 = -2.8520278186f;
			tp2 = 2.9246062355f;
		} else if (freq <= 44 && freq >= 35) { //40
			ugain = 80092.71123f;
			tp0 = 0.9100493001f;
			tp1 = -2.8159101079f;
			tp2 = 2.9057609235f;
		} else if (freq <= 34 && freq >= 28) { //30
			ugain = 34309.44333f;
			tp0 = 0.8818931306f;
			tp1 = -2.7564831952f;
			tp2 = 2.8743568927f;
		} else if (freq <= 27 && freq >= 23) { //25
			ugain = 20097.49869f;
			tp0 = 0.8599919781f;
			tp1 = -2.7096291328f;
			tp2 = 2.8492390952f;
		} else if (freq <= 22 && freq >= 15) { //20
			ugain = 10477.51171f;
			tp0 = 0.8281462754f;
			tp1 = -2.6404834928f;
			tp2 = 2.8115736773f;
		} else if (freq <= 14) { //10
			ugain = 1429.899908f;
			tp0 = 0.6855359773f;
			tp1 = -2.3146825811f;
			tp2 = 2.6235518066f;
		}

		// Festsetzen der Werte für den jeweiligen Sensor
		if (sensor == 0) {
			// also Accelerometer
			frequenz = freq;
			ugainA = ugain;
			tpA[0] = tp0;
			tpA[1] = tp1;
			tpA[2] = tp2;
		} else if (sensor == 1) {
			// also Magnetic Field
			// hier nicht frequenz = freq; da sonst der Wert für die
			// Schritterkennung falsch ist. Muss vom Accelerometer bestimmt
			// werden.
			ugainM = ugain;
			tpM[0] = tp0;
			tpM[1] = tp1;
			tpM[2] = tp2;
		}
	}

	public void beende(Context mContext) {
		try {
			// Dient dazu, dass die neuen Dateien/Ordner über MTP sofort
			// angezeigt werden unter windows
			mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(sensorFile)));
		} catch (Exception e) {
			// tritt immer auf
		}
		try {
			// Dient dazu, dass die neuen Dateien/Ordner über MTP sofort
			// angezeigt werden unter windows
			mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(posFile)));
		} catch (Exception e) {
			// tritt immer auf
		}
		beendeLogFile();
		// finish();
	}

}
