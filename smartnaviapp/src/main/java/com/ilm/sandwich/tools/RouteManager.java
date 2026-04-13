package com.ilm.sandwich.tools;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.ilm.sandwich.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Handles fetching and parsing walking route data from the Google Directions API.
 * Extracted from GoogleMap.routeTask inner class.
 *
 * @author Christian Henke
 * https://smartnavi.app
 */
public class RouteManager {

    private static final String TAG = "RouteManager";
    private static final String DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json";

    /**
     * Result of a route request, containing all parsed data needed for display.
     */
    public static class RouteResult {
        public final boolean success;
        public final String endAddress;
        public final String distanceInfo;
        public final LatLng northeastBound;
        public final LatLng southwestBound;
        public final JSONArray stepsArray;
        public final int phases;

        private RouteResult(boolean success, String endAddress, String distanceInfo,
                            LatLng northeastBound, LatLng southwestBound,
                            JSONArray stepsArray, int phases) {
            this.success = success;
            this.endAddress = endAddress;
            this.distanceInfo = distanceInfo;
            this.northeastBound = northeastBound;
            this.southwestBound = southwestBound;
            this.stepsArray = stepsArray;
            this.phases = phases;
        }

        public static RouteResult failure() {
            return new RouteResult(false, null, null, null, null, null, 0);
        }
    }

    /**
     * Fetch a walking route from src to dest. Call from a background thread.
     */
    public static RouteResult fetchRoute(LatLng src, LatLng dest, String language) {
        if (src == null || dest == null) {
            Log.e(TAG, "src or dest is null: src=" + src + " dest=" + dest);
            return RouteResult.failure();
        }

        HttpRequests httpJSON = new HttpRequests();
        httpJSON.setURL(DIRECTIONS_URL);
        httpJSON.setMethod("GET");
        httpJSON.addValue("origin", src.latitude + "," + src.longitude);
        httpJSON.addValue("destination", dest.latitude + "," + dest.longitude);
        httpJSON.addValue("key", BuildConfig.DIRECTIONS_API_KEY);
        httpJSON.addValue("mode", "walking");
        httpJSON.addValue("language", language);

        String response = httpJSON.doRequest();
        if (BuildConfig.debug) {
            Log.i(TAG, "Response: " + response);
        }

        try {
            JSONObject json = new JSONObject(response);
            String status = json.optString("status", "UNKNOWN");
            Log.i(TAG, "API status: " + status);

            if (!"OK".equals(status)) {
                Log.e(TAG, "Directions API error: " + status
                        + " - " + json.optString("error_message", "no details"));
                return RouteResult.failure();
            }

            JSONArray routesArray = json.getJSONArray("routes");
            JSONObject routesObject = routesArray.optJSONObject(0);
            JSONArray legsArray = routesObject.getJSONArray("legs");
            JSONObject legsObject = legsArray.optJSONObject(0);

            String duration = legsObject.optJSONObject("duration").getString("text");
            String distance = legsObject.optJSONObject("distance").getString("text");

            JSONObject bounds = routesObject.getJSONObject("bounds");
            JSONObject northeast = bounds.getJSONObject("northeast");
            LatLng northeastLatLng = new LatLng(northeast.optDouble("lat"), northeast.optDouble("lng"));
            JSONObject southwest = bounds.getJSONObject("southwest");
            LatLng southwestLatLng = new LatLng(southwest.optDouble("lat"), southwest.optDouble("lng"));

            String endAddress = legsObject.getString("end_address");
            String[] parts = endAddress.split(",", 3);
            try {
                endAddress = parts[0] + "\n" + parts[1];
            } catch (Exception e) {
                // Single-line address is fine
            }

            String distanceInfo = formatDistanceInfo(distance, duration, language);

            JSONArray stepsArray = legsObject.getJSONArray("steps");
            int phases = stepsArray.length();

            return new RouteResult(true, endAddress, distanceInfo,
                    northeastLatLng, southwestLatLng, stepsArray, phases);

        } catch (Exception e) {
            Log.e(TAG, "Failed to parse route", e);
            return RouteResult.failure();
        }
    }

    // Package-private for testability
    static String formatDistanceInfo(String distance, String duration, String language) {
        if (language.equalsIgnoreCase("de")) {
            return "Ziel ist " + distance + "\n" + "oder " + duration + " entfernt.";
        } else if (language.equalsIgnoreCase("es")) {
            return "Destino es de " + distance + "\n" + "o " + duration + " de distancia.";
        } else if (language.equalsIgnoreCase("fr")) {
            return "Destination est de " + distance + "\n" + "ou " + duration + ".";
        } else if (language.equalsIgnoreCase("pl")) {
            return "Docelowy jest " + distance + "\n" + "lub " + duration + ".";
        } else if (language.equalsIgnoreCase("it")) {
            return "Destination si trova a " + distance + "\n" + "o " + duration + ".";
        } else if (language.equalsIgnoreCase("en")) {
            return "Destination is " + distance + "\n" + "or " + duration + " away.";
        } else {
            return "Distance: " + distance + "\n or " + duration + ".";
        }
    }
}
