package com.ilm.sandwich.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

import com.ilm.sandwich.BackgroundService;
import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.GoogleMap;
import com.ilm.sandwich.data.NavigationRepository;
import com.ilm.sandwich.tools.Config;

/**
 * This is the core of SmartNavis stepdetection and direction calculation
 * The MapActivities just give the Core all sensordata.
 * Core recognizes steps and computes direction, location, etc.
 *
 * @author Christian Henke
 *         https://smartnavi.app
 */
public class Core implements SensorEventListener {

    // Conversion constants
    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);   // 57.29577951
    private static final double DEG_TO_RAD = Math.PI / 180.0;            // 0.01745329252
    private static final double DEG_LAT_PER_METER = 0.000008984725966;   // 1 meter in latitude degrees

    // Step timing thresholds (seconds)
    private static final float MIN_STEP_TIME = 0.24f;
    private static final float MAX_STEP_TIME = 0.8f;

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
    // posFile and sensorFile moved to GpxExporter
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
    // sensorFileNotExisting and positionsFileNotExisting moved to GpxExporter
    private static float decl = 0;
    private static boolean initialStep;
    // newStepDetected moved to GpxExporter
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
    private final GpxExporter gpxExporter;


    public Core(Context context) {

        if (context instanceof onStepUpdateListener) {
            stepUpdateListener = (onStepUpdateListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        gpxExporter = new GpxExporter(context);
        settings = context.getApplicationContext().getSharedPreferences(context.getApplicationContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
        autoCorrect = settings.getBoolean("autocorrect", false);

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
        NavigationRepository repo = NavigationRepository.getInstance();
        repo.updatePosition(startLat, startLon);
        repo.updateAltitude((int) altitude);
        repo.updateGpsError(lastErrorGPS);
    }

    public static void setLocation(double lat, double lon) {
        startLat = lat;
        startLon = lon;
        NavigationRepository.getInstance().updatePosition(lat, lon);
    }

    private static void trueNorth() {
        long time = System.currentTimeMillis();
        GeomagneticField geo = new GeomagneticField((float) startLat, (float) startLon, altitude, time);
        decl = geo.getDeclination();
    }

    // File I/O has been extracted to GpxExporter (accessed via gpxExporter field)

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
            mSensorManager.registerListener(Core.this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(Core.this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
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
            if (mOrientationProvider != null) {
                mOrientationProvider.stop();
            }
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
        } else if (startedToExport && !sollich) {
            gpxExporter.closeLogFile();
            export = false;
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
                azimuth = (orientation[0] * RAD_TO_DEG + decl);
            } else {
                azimuth = (orientation[0] * RAD_TO_DEG + 360 + decl);
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
        if (stepBegin && iStep / frequency >= MIN_STEP_TIME && iStep / frequency <= MAX_STEP_TIME) {
            // Timeframe for step between minTime and maxTime
            // Check for negative peak
            if (value < -stepThreshold) {
                // TimeFrame correct AND Threshold of reverse side reached
                stepCounter++;
                stepBegin = false;
                iStep = 1;
                initialStep = true;
                newStep();
                //save old azimith for possibly necessary orientation change, in case no steps are detected and users orientation changes strong enough
                oldAzimuth = azimuth;
                if (export) {
                    gpxExporter.writePosition(startLat, startLon, true);
                }
            } else {
                // TimeFrame correct but negative Threshold is too low
                iStep++;
            }
        } else if (stepBegin && iStep / frequency < MIN_STEP_TIME) {
            // TimeFrame for step too small, so wait and iStep++
            iStep++;
        } else if (stepBegin && iStep / frequency > MAX_STEP_TIME) {
            // TimeFrame for step too long
            stepBegin = false;
            initialStep = true;
            iStep = 1;
        }
    }

    private void newStep() {
        if (BuildConfig.debug) {
            Log.i("Location-Status", "Step: " + Core.startLon);
        }
        double[] result = PositionCalculator.computeNewPosition(startLat, startLon, azimuth, stepLength, distanceLongitude);
        startLat = result[0];
        startLon = result[1];
        NavigationRepository.getInstance().updateFromStep(startLat, startLon, azimuth, stepCounter);
        stepUpdateListener.onStepUpdate(0);
    }

    public void changeDelay(int freq, int sensor) {
        FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(freq);

        // Set values for specific sensor
        if (sensor == 0) {
            //  Accelerometer
            frequency = freq;
            ugainA = c.ugain;
            tpA[0] = c.tp0;
            tpA[1] = c.tp1;
            tpA[2] = c.tp2;
        } else if (sensor == 1) {
            // Magnetic Field
            // here not: frequency = freq; otherwise value is wrong for step detection
            //that value has to be specified by accelerometer
            ugainM = c.ugain;
            tpM[0] = c.tp0;
            tpM[1] = c.tp1;
            tpM[2] = c.tp2;
        }
    }

    public void shutdown(Context mContext) {
        pauseSensors();
        if (BuildConfig.debug)
            Log.i("Sensors", "Sensors deactivated");
        gpxExporter.closeLogFile();
        export = false;
        NavigationRepository.getInstance().reset();
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
