package com.ilm.sandwich.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.util.Log;

import com.ilm.sandwich.Splashscreen;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class is created when the app is finished.
 * Anonymous usage statistics will be sent to the smartnavi server.
 * No locations and no personal data are transmitted.
 * The uid is a random number which is defined at each new installation.
 * Statistics are important to improve the app according to usage of the users.
 *
 * @author Christian Henke
 *         www.smartnavi-app.com
 */
public class Statistics {
    public Statistics() {

    }

    public void check(Context context) {
        SharedPreferences settings = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        if (Config.usingGoogleMaps) {
            if (settings.getBoolean("nutzdaten", true)) {
                sendStatistics(context, settings, true);
            }
        } else {
            if (settings.getBoolean("nutzdaten", false)) {
                sendStatistics(context, settings, true);
            } else {
                sendStatistics(context, settings, false);
            }
        }
    }

    public void sendStatistics(Context context, SharedPreferences settings, boolean usageDataAllowed) {
        JSONObject usageData = new JSONObject();
        String appVersion = "unknown";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = pInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (usageDataAllowed) {
            SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            String magnetName = "not existing";
            try {
                magnetName = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getName();
            } catch (Exception e) {
            }
            String aclName = "not existing";
            try {
                aclName = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).getName();
            } catch (Exception e) {
            }
            String gyroName = "not existing";
            try {
                gyroName = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).getName();
            } catch (Exception e) {
            }

            try {
                // status
                usageData.put("uid", settings.getString("uid", "0"));
                // device activity_info
                usageData.put("deviceName", android.os.Build.DEVICE);
                usageData.put("productName", android.os.Build.PRODUCT);
                usageData.put("modelName", android.os.Build.MODEL);
                usageData.put("androidVersion", android.os.Build.VERSION.SDK_INT);
                usageData.put("appVersion", appVersion);
                // sensor activity_info
                usageData.put("aclName", aclName);
                usageData.put("magnName", magnetName);
                usageData.put("gyroName", gyroName);

                usageData.put("meanAclFreq", (int) Config.meanAclFreq);
                usageData.put("meanMagnFreq", (int) Config.meanMagnFreq);

                if (Splashscreen.PLAYSTORE_VERSION) {
                    usageData.put("mapSource", settings.getString("MapSource", "GoogleMaps"));
                } else {
                    usageData.put("mapSource", settings.getString("MapSource", "MapQuestOSM"));
                }

                int serviceUsage = 0;
                if (Config.backgroundServiceUsed) {
                    serviceUsage = 1;
                }
                usageData.put("serviceUsage", serviceUsage);

                int autoCorrect = 0;
                if (settings.getBoolean("autocorrect", false)) {
                    autoCorrect = 1;
                }
                usageData.put("autocorrect", autoCorrect);
                usageData.put("gpstimer", settings.getInt("gpstimer", 1));

                int time = (int) ((System.currentTimeMillis() - Config.startTime) / 1000);
                usageData.put("time", time);

                //Number of steps
                usageData.put("stepCounter", Core.stepCounter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                // no device and user related data allowed
                // keep in mind that uid is a random number
                usageData.put("uid", settings.getString("uid", "0"));
                usageData.put("appVersion", appVersion);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (Config.debugMode) {
            Log.i("Usage Data", usageData.toString());
        }

        new connectAndSend(usageData).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class connectAndSend extends AsyncTask<Void, Void, Void> {
        private JSONObject usageData;

        private connectAndSend(JSONObject usageData) {
            this.usageData = usageData;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // send Data to Server
            try {
                URL url = new URL(Config.SMARTNAVI_API_URL);
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("PUT");
                httpCon.setRequestProperty("Content-Type", "application/json");
                OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
                out.write(usageData.toString());
                out.close();
                int respCode = httpCon.getResponseCode();
                if (Config.debugMode) {
                    Log.d("UsageData", "Server-Response: " + respCode);
                }
                httpCon.disconnect();
            } catch (Exception e) {
                    e.printStackTrace();
            }
            return null;
        }
    }


}
