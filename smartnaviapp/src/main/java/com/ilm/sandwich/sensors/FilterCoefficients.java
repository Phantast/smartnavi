package com.ilm.sandwich.sensors;

/**
 * Provides 3rd-order Butterworth low-pass filter coefficients based on sensor sampling frequency.
 * Corner frequency is approximately 0.3 Hz for all frequency bands.
 * <p>
 * Extracted from Core.changeDelay() for testability.
 *
 * @author Christian Henke
 * https://smartnavi.app
 */
public class FilterCoefficients {

    /**
     * Container for filter coefficients.
     */
    public static class Coefficients {
        public final float ugain;
        public final float tp0;
        public final float tp1;
        public final float tp2;

        public Coefficients(float ugain, float tp0, float tp1, float tp2) {
            this.ugain = ugain;
            this.tp0 = tp0;
            this.tp1 = tp1;
            this.tp2 = tp2;
        }
    }

    /**
     * Get filter coefficients for the given sampling frequency.
     * The coefficients define a 3rd-order Butterworth low-pass filter
     * with a corner frequency of approximately 0.3 Hz.
     *
     * @param freq sampling frequency in Hz (typically 10-130)
     * @return coefficients for the low-pass filter
     */
    public static Coefficients getCoefficients(int freq) {
        if (freq >= 125) {        // ~130 Hz
            return new Coefficients(2662508.633f, 0.9714168814f, -2.9424208232f, 2.9710009372f);
        } else if (freq >= 115) { // ~120 Hz
            return new Coefficients(2096647.970f, 0.9690721133f, -2.9376603253f, 2.9685843964f);
        } else if (freq >= 105) { // ~110 Hz
            return new Coefficients(1617241.715f, 0.9663083052f, -2.9320417512f, 2.9657284993f);
        } else if (freq >= 95) {  // ~100 Hz
            return new Coefficients(1217122.860f, 0.9630021159f, -2.9253101348f, 2.9623014461f);
        } else if (freq >= 85) {  // ~90 Hz
            return new Coefficients(889124.3983f, 0.9589765397f, -2.9170984005f, 2.9581128632f);
        } else if (freq >= 75) {  // ~80 Hz
            return new Coefficients(626079.3215f, 0.9539681632f, -2.9068581408f, 2.9528771997f);
        } else if (freq >= 65) {  // ~70 Hz
            return new Coefficients(420820.6222f, 0.9475671238f, -2.8937318862f, 2.9461457520f);
        } else if (freq >= 55) {  // ~60 Hz
            return new Coefficients(266181.2926f, 0.9390989403f, -2.8762997235f, 2.9371707284f);
        } else if (freq >= 45) {  // ~50 Hz
            return new Coefficients(154994.3249f, 0.9273699683f, -2.8520278186f, 2.9246062355f);
        } else if (freq >= 35) {  // ~40 Hz
            return new Coefficients(80092.71123f, 0.9100493001f, -2.8159101079f, 2.9057609235f);
        } else if (freq >= 28) {  // ~30 Hz
            return new Coefficients(34309.44333f, 0.8818931306f, -2.7564831952f, 2.8743568927f);
        } else if (freq >= 23) {  // ~25 Hz
            return new Coefficients(20097.49869f, 0.8599919781f, -2.7096291328f, 2.8492390952f);
        } else if (freq >= 15) {  // ~20 Hz
            return new Coefficients(10477.51171f, 0.8281462754f, -2.6404834928f, 2.8115736773f);
        } else {                  // ~10 Hz
            return new Coefficients(1429.899908f, 0.6855359773f, -2.3146825811f, 2.6235518066f);
        }
    }
}
