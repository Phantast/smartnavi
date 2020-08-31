package com.ilm.sandwich.sensors;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;

import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.GoogleMap;
import com.ilm.sandwich.tools.Config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This is the core of SmartNavis stepdetection and direction calculation
 * The MapActivities just give the Core all sensordata.
 * Core recognizes steps and computes direction, location, etc.
 *
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Core implements SensorEventListener {

    public static float[] gravity = new float[3];
    public static float[] linear = new float[4];
    public static float[] linearRemapped = new float[4];
    public static float[] origMagn = new float[3];
    public static float[] magn = new float[3];
    public static float[] origAcl = new float[3]; //only needed for logging/debug purposes
    public static double startLat;
    public static double startLon;
    public static int stepCounter = 0;
    public static double azimuth;
    public static int altitude = 150;
    public static double distanceLongitude;
    public static float stepLength;
    public static boolean export;
    public static String version;
    public static float lastErrorGPS;
    public static int units = 0;
    static File posFile;
    static File sensorFile;
    private static double oldAzimuth = 0;
    private static float frequency;
    private static boolean stepBegin = false;
    private static float[] iMatrix = new float[9];
    private static float[] RMatrix = new float[16];
    private static float[] RMatrixRemapped = new float[16];
    private static float[] RMatrixTranspose = new float[16];
    private static float[] orientation = new float[3];
    private static double deltaLat;
    private static double deltaLon;
    private static float iStep = 1;
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
    private static float stepThreshold = 2.0f;
    private static boolean sensorFileNotExisting = true;
    private static boolean positionsFileNotExisting = true;
    private static float decl = 0;
    private static boolean initialStep;
    private static boolean newStepDetected = false;
    private static boolean startedToExport = false;
    private static long startTime;
    public boolean gyroExists = false;
    private ImprovedOrientationSensor2Provider mOrientationProvider;
    private SensorManager mSensorManager;
    private boolean alreadyWaitingForAutoCorrect = false;
    private int stepsToWait = 0;
    private int autoCorrectFactor = 1;
    private int magnUnits;
    private int aclUnits;
    private boolean autoCorrect;
    private SharedPreferences settings;
    private onStepUpdateListener stepUpdateListener;


    public Core(Context context) {

        if (context instanceof onStepUpdateListener) {
            stepUpdateListener = (onStepUpdateListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        settings = context.getApplicationContext().getSharedPreferences(context.getApplicationContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        autoCorrect = settings.getBoolean("autocorrect", false);

        positionsFileNotExisting = true;
        sensorFileNotExisting = true;

        stepCounter = 0;
        initialStep = true;

        magn[0] = magn[1] = magn[2] = gravity[0] = gravity[1] = 0;
        gravity[2] = 9.81f;
        ugainM = ugainA = 154994.3249f;
        tpA[0] = tpM[0] = 0.9273699683f;
        tpA[1] = tpM[1] = -2.8520278186f;
        tpA[2] = tpM[2] = 2.9246062355f;

        version = BuildConfig.VERSION_NAME;

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);


        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroExists = true;
            mOrientationProvider = new ImprovedOrientationSensor2Provider((SensorManager) context.getSystemService(Context.SENSOR_SERVICE));
        }
    }

    /**
     * Initializing
     *
     * @param startLat
     * @param startLon
     * @param distanceLongitude
     */
    public static void initialize(double startLat, double startLon, double distanceLongitude, double altitude, float lastErrorGPS) {
        Core.startLat = startLat;
        Core.startLon = startLon;
        Core.distanceLongitude = distanceLongitude;
        Core.altitude = (int) altitude;
        Core.lastErrorGPS = lastErrorGPS;
        trueNorth();
    }

    public static void setLocation(double lat, double lon) {
        startLat = lat;
        startLon = lon;
    }

    private static void trueNorth() {
        long time = System.currentTimeMillis();
        GeomagneticField geo = new GeomagneticField((float) startLat, (float) startLon, altitude, time);
        decl = geo.getDeclination();
    }

    private static void positionOutput() {
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/smartnavi/");
            folder.mkdir();
            if (folder.canWrite()) {
                if (positionsFileNotExisting) {
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
                    positionsFileNotExisting = false;
                } else {
                    FileWriter posWriter = new FileWriter(posFile, true);
                    BufferedWriter out = new BufferedWriter(posWriter);

                    if (newStepDetected) {
                        out.newLine();

                        TimeZone tz = TimeZone.getTimeZone("UTC");
                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ", Locale.GERMAN);
                        df.setTimeZone(tz);
                        String nowAsISO = df.format(new Date());

                        out.write("<trkpt lat=\"" + startLat + "\" lon=\"" + startLon + "\"><time>" + nowAsISO + "</time></trkpt>");

                        newStepDetected = false;
                    }

                    out.close();
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public static void closeLogFile() {
        if (export && positionsFileNotExisting == false) {
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
        positionsFileNotExisting = true;
        sensorFileNotExisting = true;
    }

    private static void dataOutput() {
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/smartnavi/");
            folder.mkdir();
            if (folder.canWrite()) {
                if (sensorFileNotExisting) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMAN);
                    String curentDateandTime = sdf.format(new Date());
                    String textname = "sensoren_" + curentDateandTime + ".csv";
                    sensorFile = new File(folder, textname);
                    FileWriter sensorWriter = new FileWriter(sensorFile);
                    BufferedWriter outs = new BufferedWriter(sensorWriter);
                    outs.write(startLat + "; " + startLon + "; " + stepLength + ";" + version + "; ");
                    outs.newLine();
                    outs.write("origmagn0; origmagn1; origmagn2; origaccel0; origaccel1; origaccel2; "
                            + "azimuthNeu;");
                    outs.close();
                    sensorFileNotExisting = false;
                } else {
                    FileWriter sensorWriter = new FileWriter(sensorFile, true);
                    BufferedWriter outs = new BufferedWriter(sensorWriter);

                    outs.newLine();

                    outs.write(origMagn[0] + ";" + origMagn[1] + ";" + origMagn[2] + ";" + origAcl[0] + ";" + origAcl[1] + ";" + origAcl[2] + ";"
                            + azimuth + ";");
                    outs.close();
                }
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    public void startSensors() {
        aclUnits = 0;
        magnUnits = 0;
        startTime = System.nanoTime();
        try {
            mSensorManager.unregisterListener(Core.this);
            mSensorManager.registerListener(Core.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(Core.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
            if (BuildConfig.debug)
                Log.i("Sensors", "Sensors activated");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (gyroExists) {
            //use gyroscope with impovedOrientationProvider
            mOrientationProvider.start();
        }
    }

    public void reactivateSensors() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(Core.this);
            mSensorManager.registerListener(Core.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1);
            mSensorManager.registerListener(Core.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1);
            if (BuildConfig.debug)
                Log.i("Sensors", "Sensors reactivated!");
            if (gyroExists) {
                //use gyroscope with impovedOrientationProvider
                mOrientationProvider.start();
            }
        }
    }

    public void pauseSensors() {
        try {
            mSensorManager.unregisterListener(this);
            //new orientation provider
            mOrientationProvider.stop();
            if (BuildConfig.debug)
                Log.i("Sensors", "Sensors deactivated!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void enableAutocorrect() {
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
    }

    public void disableAutocorrect() {
        autoCorrect = settings.getBoolean("autocorrect", false);
    }

    public void writeLog(boolean sollich) {
        if (sollich) {
            export = true;
            startedToExport = true;
        } else if (startedToExport == true && sollich == false) {
            closeLogFile();
        }
    }

    public void imbaMagnetic(float[] magnetic) {
        // LowPass 0.5Hz for alpha0
        xm0[0] = xm0[1];
        xm0[1] = xm0[2];
        xm0[2] = xm0[3];
        xm0[3] = magnetic[0] / ugainM;
        ym0[0] = ym0[1];
        ym0[1] = ym0[2];
        ym0[2] = ym0[3];
        ym0[3] = (xm0[0] + xm0[3]) + 3 * (xm0[1] + xm0[2]) + (tpM[0] * ym0[0]) + (tpM[1] * ym0[1]) + (tpM[2] * ym0[2]);
        magn[0] = (float) ym0[3];

        // LowPass 0.5Hz for alpha1
        xm1[0] = xm1[1];
        xm1[1] = xm1[2];
        xm1[2] = xm1[3];
        xm1[3] = magnetic[1] / ugainM;
        ym1[0] = ym1[1];
        ym1[1] = ym1[2];
        ym1[2] = ym1[3];
        ym1[3] = (xm1[0] + xm1[3]) + 3 * (xm1[1] + xm1[2]) + (tpM[0] * ym1[0]) + (tpM[1] * ym1[1]) + (tpM[2] * ym1[2]);
        magn[1] = (float) ym1[3];

        // LowPass 0.5Hz for alpha2
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
        // LowPass 0.5Hz for alpha0
        xa0[0] = xa0[1];
        xa0[1] = xa0[2];
        xa0[2] = xa0[3];
        xa0[3] = accel[0] / ugainA;
        ya0[0] = ya0[1];
        ya0[1] = ya0[2];
        ya0[2] = ya0[3];
        ya0[3] = (xa0[0] + xa0[3]) + 3 * (xa0[1] + xa0[2]) + (tpA[0] * ya0[0]) + (tpA[1] * ya0[1]) + (tpA[2] * ya0[2]);
        gravity[0] = (float) ya0[3];

        // LowPass 0.5Hz for alpha1
        xa1[0] = xa1[1];
        xa1[1] = xa1[2];
        xa1[2] = xa1[3];
        xa1[3] = accel[1] / ugainA;
        ya1[0] = ya1[1];
        ya1[1] = ya1[2];
        ya1[2] = ya1[3];
        ya1[3] = (xa1[0] + xa1[3]) + 3 * (xa1[1] + xa1[2]) + (tpA[0] * ya1[0]) + (tpA[1] * ya1[1]) + (tpA[2] * ya1[2]);
        gravity[1] = (float) ya1[3];

        // LowPass 0.5Hz for alpha2
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

    public void calculateAzimuth() {
        SensorManager.getRotationMatrix(RMatrix, iMatrix, gravity, magn);
        SensorManager.remapCoordinateSystem(RMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, RMatrixRemapped);
        SensorManager.getOrientation(RMatrixRemapped, orientation);
        Matrix.transposeM(RMatrixTranspose, 0, RMatrix, 0);
        Matrix.multiplyMV(linearRemapped, 0, RMatrixTranspose, 0, linear, 0);

        //If Gyroscope exists, use ImprovedOrientationProvider, else use accelerometer and magentic field
        if (gyroExists) {
            azimuth = mOrientationProvider.getAzimuth(decl);
        } else {
            if (orientation[0] >= 0) {
                // Azimuth-Calculation (rad in degree)
                azimuth = (orientation[0] * 57.29577951f + decl);
            } else {
                // Azimuth-Calculation (rad in degree) +360
                azimuth = (orientation[0] * 57.29577951f + 360 + decl);
            }

            if (azimuth >= 360) {
                azimuth -= 360;
            }
        }
    }

    public void stepDetection() {
        float value = linearRemapped[2];
        if (initialStep && value >= stepThreshold) {
            // Introduction of a step
            initialStep = false;
            stepBegin = true;
        } else if (!stepBegin) {
            if (oldAzimuth - azimuth > 5 || oldAzimuth - azimuth < -5) {
                //invoke step (only interface, not a real step), because orientation of user has changed more than X degree
                //so a step is necessary to update users position marker and respective orientation
                //at this position in code it means: no step is being awaited and therefore check orientation change
                stepUpdateListener.onStepUpdate(0);
                oldAzimuth = azimuth;
            }
        }
        if (stepBegin && iStep / frequency >= 0.24f && iStep / frequency <= 0.8f) {
            // Timeframe for step between minTime and maxTime
            // Check for negative peak
            if (value < -stepThreshold) {
                // TimeFrame correct AND Threshold of reverse side reached
                stepCounter++;
                stepBegin = false;
                iStep = 1;
                initialStep = true;
                newStep();
                newStepDetected = true;
                //save old azimith for possibly necessary orientation change, in case no steps are detected and users orientation changes strong enough
                oldAzimuth = azimuth;
                if (export) {
                    positionOutput();
                }
            } else {
                // TimeFrame correct but negative Threshold is too low
                iStep++;
            }
        } else if (stepBegin && iStep / frequency < 0.24f) {
            // TimeFrame for step too small, so wait and iStep++
            iStep++;
        } else if (stepBegin && iStep / frequency > 0.8f) {
            // TimeFrame for step too long
            stepBegin = false;
            initialStep = true;
            iStep = 1;
        }
    }

    private void newStep() {
        double winkel = azimuth;
        double winkel2 = winkel * 0.01745329252;
        if (BuildConfig.debug) {
            Log.i("Location-Status", "Step: " + Core.startLon);
        }
        deltaLat = Math.cos(winkel2) * 0.000008984725966 * stepLength;
        // 100cm for a step will be calculated according to angle on lat
        deltaLon = Math.sin(winkel2) / (distanceLongitude * 1000) * stepLength;
        // 100cm for a step will be calculated according to angle on lon

        deltaLat = Math.abs(deltaLat);
        deltaLon = Math.abs(deltaLon);
        // made by Christian Henke
        if (startLat > 0) {
            // User is on northern hemisphere, Latitude bigger than 0
            if (winkel > 270 || winkel < 90) { // Movement towards north
                startLat += deltaLat;
            } else {
                // Movement towards south
                startLat -= deltaLat;
            }
        } else if (startLat < 0) {
            // User is on southern hemisphere, Latitude smaller than 0
            if (winkel > 270 || winkel < 90) {
                // Movement towards north
                startLat += deltaLat;
            } else {
                // Movement towards south
                startLat -= deltaLat;
            }
        }
        if (winkel < 180) {
            // Movement towards east
            startLon += deltaLon;
        } else {
            // Movement towards west
            startLon -= deltaLon;
        }
        stepUpdateListener.onStepUpdate(0);
    }

    public void changeDelay(int freq, int sensor) {
        // LowPassFilter 3. Order - Corner frequency all at 0.3 Hz

        //Initializing on 50Hz
        float ugain = 154994.3249f;
        float tp0 = 0.9273699683f;
        float tp1 = -2.8520278186f;
        float tp2 = 2.9246062355f;

        // Values according to actual frequency
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

        // Set values for specific sensor
        if (sensor == 0) {
            //  Accelerometer
            frequency = freq;
            ugainA = ugain;
            tpA[0] = tp0;
            tpA[1] = tp1;
            tpA[2] = tp2;
        } else if (sensor == 1) {
            // Magnetic Field
            // here not: frequency = freq; otherwise value is wrong for step detection
            //that value has to be specified by accelerometer
            ugainM = ugain;
            tpM[0] = tp0;
            tpM[1] = tp1;
            tpM[2] = tp2;
        }
    }

    public void shutdown(Context mContext) {
        pauseSensors();
        if (BuildConfig.debug)
            Log.i("Sensors", "Sensors deactivated");
        try {
            //Show new files with MTP for Windows immediatly
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(sensorFile)));
        } catch (Exception e) {
            // is always the case
        }
        try {
            //Show new files with MTP for Windows immediatly
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(posFile)));
        } catch (Exception e) {
            // is always the case
        }
        closeLogFile();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {

            case Sensor.TYPE_MAGNETIC_FIELD:
                imbaMagnetic(event.values.clone());
                if (BuildConfig.debug) {
                    Core.origMagn = event.values.clone();
                }
                magnUnits++;
                break;

            case Sensor.TYPE_ACCELEROMETER:
                if (BuildConfig.debug) {
                    Core.origAcl = event.values.clone();
                }

                if (Config.backgroundServiceActive && units % 10 == 0) {
                    BackgroundService.newFakePosition();
                }

                long timePassed = System.nanoTime() - startTime;
                aclUnits++;
                units++;

                if (timePassed >= 2000000000) { // every 2sek
                    changeDelay(aclUnits / 2, 0);
                    changeDelay(magnUnits / 2, 1);

                    startTime = System.nanoTime();
                    aclUnits = magnUnits = 0;
                }

                imbaGravity(event.values.clone());
                imbaLinear(event.values.clone());

                calculateAzimuth();

                if (export && BuildConfig.debug) {
                    dataOutput();
                }

                stepDetection();
                // AutoCorrect (dependent on Factor, i.e. number of steps)
                if (autoCorrect) {
                    if (!alreadyWaitingForAutoCorrect) {
                        alreadyWaitingForAutoCorrect = true;
                        stepsToWait = stepCounter + 75 * autoCorrectFactor;
                        if (BuildConfig.debug) {
                            stepsToWait = stepCounter + 10;
                            Log.i("Location-Status", Core.stepCounter + " von " + stepsToWait);
                        }
                    }
                    if (stepCounter >= stepsToWait) {
                        if (Config.backgroundServiceActive) {
                            GoogleMap.backgroundServiceShallBeOnAgain = true;
                            BackgroundService.pauseFakeProvider();
                        }
                        stepUpdateListener.onStepUpdate(1); //start Autocorrect
                        alreadyWaitingForAutoCorrect = false;
                        if (BuildConfig.debug)
                            Log.i("Location-Status", "Steps reached for Autocorrect!");
                    }
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface onStepUpdateListener {
        void onStepUpdate(int event);
    }
}
