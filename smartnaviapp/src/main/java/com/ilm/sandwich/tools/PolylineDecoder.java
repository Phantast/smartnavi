package com.ilm.sandwich.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Decodes Google Maps encoded polyline strings into coordinate pairs.
 * Based on the Google Encoded Polyline Algorithm Format.
 * <p>
 * Extracted from GoogleMap.decodePoly() for testability.
 *
 * @author Christian Henke
 * https://smartnavi.app
 */
public class PolylineDecoder {

    /**
     * Decodes an encoded polyline string into a list of lat/lng pairs.
     * Each entry is a double[] of {latitude, longitude}.
     *
     * @param encoded the encoded polyline string
     * @return list of coordinate pairs, empty list if input is null or empty
     */
    public static List<double[]> decode(String encoded) {
        List<double[]> poly = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return poly;
        }

        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            poly.add(new double[]{lat / 1E5, lng / 1E5});
        }
        return poly;
    }
}
