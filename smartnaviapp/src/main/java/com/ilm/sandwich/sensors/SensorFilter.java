package com.ilm.sandwich.sensors;

/**
 * 3rd-order Butterworth low-pass filter for a single axis.
 * Implements the same filtering algorithm used in Core.imbaMagnetic() / Core.imbaGravity().
 * <p>
 * Each instance filters one axis. Create 3 instances (x, y, z) per sensor.
 *
 * @author Christian Henke
 * https://smartnavi.app
 */
public class SensorFilter {

    private final double[] x = new double[4];
    private final double[] y = new double[4];
    private float ugain;
    private float tp0;
    private float tp1;
    private float tp2;

    /**
     * Create a filter with the given coefficients.
     *
     * @param ugain unity gain factor
     * @param tp0   feedback coefficient 0
     * @param tp1   feedback coefficient 1
     * @param tp2   feedback coefficient 2
     */
    public SensorFilter(float ugain, float tp0, float tp1, float tp2) {
        this.ugain = ugain;
        this.tp0 = tp0;
        this.tp1 = tp1;
        this.tp2 = tp2;
    }

    /**
     * Create a filter from a Coefficients object.
     */
    public SensorFilter(FilterCoefficients.Coefficients c) {
        this(c.ugain, c.tp0, c.tp1, c.tp2);
    }

    /**
     * Apply one sample through the filter.
     *
     * @param input the new input sample (raw sensor value)
     * @return the filtered output
     */
    public float apply(float input) {
        x[0] = x[1];
        x[1] = x[2];
        x[2] = x[3];
        x[3] = input / ugain;
        y[0] = y[1];
        y[1] = y[2];
        y[2] = y[3];
        y[3] = (x[0] + x[3]) + 3 * (x[1] + x[2]) + (tp0 * y[0]) + (tp1 * y[1]) + (tp2 * y[2]);
        return (float) y[3];
    }

    /**
     * Update filter coefficients (e.g., when sampling frequency changes).
     */
    public void updateCoefficients(float ugain, float tp0, float tp1, float tp2) {
        this.ugain = ugain;
        this.tp0 = tp0;
        this.tp1 = tp1;
        this.tp2 = tp2;
    }

    /**
     * Update filter coefficients from a Coefficients object.
     */
    public void updateCoefficients(FilterCoefficients.Coefficients c) {
        updateCoefficients(c.ugain, c.tp0, c.tp1, c.tp2);
    }

    /**
     * Reset filter state to zero.
     */
    public void reset() {
        for (int i = 0; i < 4; i++) {
            x[i] = 0;
            y[i] = 0;
        }
    }
}
