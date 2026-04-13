package com.ilm.sandwich.data;

/**
 * Immutable snapshot of the current navigation state.
 * Replaces the static fields previously scattered across Core.java.
 */
public class NavigationState {

    public static final NavigationState DEFAULT = new NavigationState(0, 0, 0, 0, 0, 150);

    public final double latitude;
    public final double longitude;
    public final double azimuth;
    public final int stepCount;
    public final float lastGpsError;
    public final int altitude;

    public NavigationState(double latitude, double longitude, double azimuth,
                           int stepCount, float lastGpsError, int altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.azimuth = azimuth;
        this.stepCount = stepCount;
        this.lastGpsError = lastGpsError;
        this.altitude = altitude;
    }

    public NavigationState withPosition(double lat, double lon) {
        return new NavigationState(lat, lon, azimuth, stepCount, lastGpsError, altitude);
    }

    public NavigationState withStep(double lat, double lon, double azimuth, int stepCount) {
        return new NavigationState(lat, lon, azimuth, stepCount, lastGpsError, altitude);
    }

    public NavigationState withGpsError(float error) {
        return new NavigationState(latitude, longitude, azimuth, stepCount, error, altitude);
    }

    public NavigationState withAltitude(int altitude) {
        return new NavigationState(latitude, longitude, azimuth, stepCount, lastGpsError, altitude);
    }
}
