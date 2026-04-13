package com.ilm.sandwich.sensors;

/**
 * Pure math utility for computing new geographic position after a step.
 * Extracted from Core.newStep() for testability.
 *
 * @author Christian Henke
 * https://smartnavi.app
 */
public class PositionCalculator {

    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double DEG_LAT_PER_METER = 0.000008984725966; // 1 meter in latitude degrees

    /**
     * Compute new position after a single step.
     *
     * @param lat               current latitude in degrees
     * @param lon               current longitude in degrees
     * @param azimuthDegrees    heading in degrees (0 = north, 90 = east, 180 = south, 270 = west)
     * @param stepLength        step length in meters
     * @param distanceLongitude distance of one degree of longitude in km at this latitude
     * @return double[] {newLat, newLon}
     */
    public static double[] computeNewPosition(double lat, double lon,
                                               double azimuthDegrees, float stepLength,
                                               double distanceLongitude) {
        double azimuthRad = azimuthDegrees * DEG_TO_RAD;
        double deltaLat = Math.abs(Math.cos(azimuthRad) * DEG_LAT_PER_METER * stepLength);
        double deltaLon = Math.abs(Math.sin(azimuthRad) / (distanceLongitude * 1000) * stepLength);

        // Latitude: north (azimuth 270-360 or 0-90) increases, south decreases
        if (azimuthDegrees > 270 || azimuthDegrees < 90) {
            lat += deltaLat;
        } else {
            lat -= deltaLat;
        }

        // Longitude: east (azimuth 0-180) increases, west decreases
        if (azimuthDegrees < 180) {
            lon += deltaLon;
        } else {
            lon -= deltaLon;
        }

        return new double[]{lat, lon};
    }
}
