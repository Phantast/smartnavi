package com.ilm.sandwich.tools;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.ilm.sandwich.Config;

import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class is fired when the app is finished.
 * Anonymous usage statistics will be sent to the backend.
 * No locations and no personal data are transmitted.
 * The uid is a random number which is set at each new installation.
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
        if (settings.getBoolean("nutzdaten", false)) {
            sendStatistics(context, settings);
        }
    }

    public void sendStatistics(Context context, SharedPreferences settings) {

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

        JSONObject usageData = new JSONObject();
        try {
            // status
            String uid = settings.getString("uid", "0");
            usageData.put("uid", uid + "_free");
            // device info
            usageData.put("deviceName", Build.DEVICE);
            usageData.put("productName", Build.PRODUCT);
            usageData.put("modelName", Build.MODEL);
            usageData.put("androidVersion", Build.VERSION.SDK_INT);
            // sensor info
            usageData.put("aclName", aclName);
            usageData.put("magnName", magnetName);
            usageData.put("gyroName", gyroName);

            usageData.put("meanAclFreq", (int) Config.meanAclFreq);
            usageData.put("meanMagnFreq", (int) Config.meanMagnFreq);

            usageData.put("mapSource", settings.getString("MapSource", "-"));
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
            if (Config.debugMode) {
                e.printStackTrace();
            }
        }
        if (Config.debugMode) {
            Log.i("Usage Data", usageData.toString());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new connectAndSend(usageData).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new connectAndSend(usageData).execute();
        }
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
                URL url = new URL(Config.API_URL);
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
                if (Config.debugMode) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }


}
