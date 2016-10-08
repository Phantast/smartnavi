package com.ilm.sandwich.tools;

import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.util.Log;

import com.ilm.sandwich.BuildConfig;
import com.ilm.sandwich.GoogleMap;
import com.ilm.sandwich.sensors.Core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

public class PlacesAutoComplete extends AsyncTask<String, String, StringBuilder> {

    private static final String LOG_TAG = "PlacesAutocomplete";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    @Override
    protected StringBuilder doInBackground(String... input) {

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();

        try {
            StringBuilder sb = new StringBuilder(Config.PLACES_API_URL + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?sensor=true&key=" + Config.PLACES_API_KEY);
            sb.append("&language=" + Locale.getDefault().getLanguage());
            sb.append("&location=" + Core.startLat + "," + Core.startLon);
            sb.append("&radius=1000");
            sb.append("&input=" + URLEncoder.encode(input[0], "utf8"));
            if (BuildConfig.debug) {
                Log.i(LOG_TAG, "Places Autocomplete for: " + input[0]);
                Log.i(LOG_TAG, "Places Autocomplete for: " + sb.toString());
            }
            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestProperty("Referer", "smartnavi-app.com/app/places");
            } catch (Exception e) {
                e.printStackTrace();
            }
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }

            try {
                // Fallback
                JSONObject jsonObj = new JSONObject(jsonResults.toString());
                String status = (String) jsonObj.get("status");
                if (status.equals("OVER_QUERY_LIMIT")) {
                    // react to over query limit
                    Config.PLACES_API_UNDER_LIMIT = false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (MalformedURLException e) {
            if (BuildConfig.debug) {
                Log.e(LOG_TAG, "Error processing Places API URL", e);
            }
        } catch (IOException e) {
            if (BuildConfig.debug) {
                Log.e(LOG_TAG, "Error connecting to Places API", e);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return jsonResults;
    }

    @Override
    protected void onPostExecute(StringBuilder jsonResults) {
        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");
            if (BuildConfig.debug)
                Log.i(LOG_TAG, "Places Autocomplete Result: " + jsonObj.toString());
            // builds MatrixCursor for autocomplete search view
            MatrixCursor cursor = new MatrixCursor(Config.COLUMNS);
            for (int i = 0; i < predsJsonArray.length(); i++) {
                int pos = i + 1;
                cursor.addRow(new String[]{"" + pos, predsJsonArray.getJSONObject(i).getString("description")});
            }
            try {
                GoogleMap.cursor = cursor;
                GoogleMap.changeSuggestionAdapter.sendEmptyMessage(0);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
            e.printStackTrace();
        }
    }
}