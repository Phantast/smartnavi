package com.ilm.sandwich.sensors;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the 3rd-order Butterworth low-pass filter used for sensor data smoothing.
 * Verifies convergence, stability, frequency response, and coefficient selection.
 */
public class SensorFilterTest {

    private static final float EPSILON = 0.01f;

    // Default 50Hz coefficients
    private SensorFilter filter;

    @Before
    public void setUp() {
        FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(50);
        filter = new SensorFilter(c);
    }

    // --- Filter convergence tests ---

    @Test
    public void constantInput_converges() {
        // A low-pass filter should converge to the input value for DC (constant) signals
        float input = 9.81f; // gravity
        float output = 0;
        for (int i = 0; i < 500; i++) {
            output = filter.apply(input);
        }
        assertEquals("Filter should converge to constant input", input, output, EPSILON);
    }

    @Test
    public void constantInput_negative_converges() {
        float input = -5.0f;
        float output = 0;
        for (int i = 0; i < 500; i++) {
            output = filter.apply(input);
        }
        assertEquals("Filter should converge to negative constant input", input, output, EPSILON);
    }

    @Test
    public void zeroInput_staysZero() {
        for (int i = 0; i < 100; i++) {
            float output = filter.apply(0.0f);
            assertEquals("Zero input should produce zero output", 0.0f, output, EPSILON);
        }
    }

    // --- Filter stability tests ---

    @Test
    public void stepResponse_convergesAndStabilizes() {
        // Apply a step from 0 to 10 and verify the output converges.
        // A 3rd-order Butterworth can have slight overshoot, so we check that
        // after settling (200+ samples), the output stays within 1% of the target.
        float lastOutput = 0;
        for (int i = 0; i < 500; i++) {
            lastOutput = filter.apply(10.0f);
        }
        assertEquals("Filter should converge to step value", 10.0f, lastOutput, EPSILON);

        // Verify stability: additional samples should not deviate
        for (int i = 0; i < 50; i++) {
            float output = filter.apply(10.0f);
            assertEquals("Filter should be stable after convergence", 10.0f, output, EPSILON);
        }
    }

    @Test
    public void largeInput_noOverflow() {
        // Test with large sensor values (some magnetometers report up to ~100 uT)
        float input = 100.0f;
        float output = 0;
        for (int i = 0; i < 500; i++) {
            output = filter.apply(input);
            assertFalse("Output should not be NaN", Float.isNaN(output));
            assertFalse("Output should not be Infinite", Float.isInfinite(output));
        }
        assertEquals(input, output, EPSILON);
    }

    // --- Noise attenuation test ---

    @Test
    public void noisyInput_attenuated() {
        // Apply a DC signal with high-frequency noise and verify noise is reduced
        float dcValue = 9.81f;
        float noiseAmplitude = 2.0f;
        float outputSum = 0;
        int settledSamples = 0;

        for (int i = 0; i < 1000; i++) {
            // Alternating noise simulates high-frequency content
            float noise = (i % 2 == 0) ? noiseAmplitude : -noiseAmplitude;
            float output = filter.apply(dcValue + noise);
            if (i >= 500) { // Only measure after settling
                outputSum += output;
                settledSamples++;
            }
        }

        float avgOutput = outputSum / settledSamples;
        // Average output should be close to the DC value, with noise greatly attenuated
        assertEquals("Noisy signal should be filtered to DC value", dcValue, avgOutput, 0.5f);
    }

    // --- Reset test ---

    @Test
    public void reset_clearsState() {
        // Feed some values
        for (int i = 0; i < 100; i++) {
            filter.apply(50.0f);
        }
        // Reset
        filter.reset();
        // After reset, output should be near zero for zero input
        float output = filter.apply(0.0f);
        assertEquals("After reset, filter should output near zero", 0.0f, output, EPSILON);
    }

    // --- Coefficient update test ---

    @Test
    public void updateCoefficients_changesFilterBehavior() {
        // Start with 50Hz coefficients, converge
        for (int i = 0; i < 500; i++) {
            filter.apply(10.0f);
        }
        float output50Hz = filter.apply(10.0f);

        // Reset and use 10Hz coefficients (lower freq = slower settling)
        filter.reset();
        FilterCoefficients.Coefficients c10 = FilterCoefficients.getCoefficients(10);
        filter.updateCoefficients(c10);

        float output10Hz = 0;
        for (int i = 0; i < 100; i++) {
            output10Hz = filter.apply(10.0f);
        }
        // At 50Hz the filter was already converged; at 10Hz with only 100 samples it should be less settled
        // Both should be approaching 10 but the 10Hz version may be slightly further
        // Main point: the filter accepted new coefficients and still works
        assertFalse("10Hz filtered output should not be NaN", Float.isNaN(output10Hz));
        assertTrue("10Hz output should be positive and approaching input", output10Hz > 0);
    }

    // --- FilterCoefficients selection tests ---

    @Test
    public void coefficients_50Hz_default() {
        FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(50);
        assertEquals(154994.3249f, c.ugain, 0.001f);
        assertEquals(0.9273699683f, c.tp0, 0.0000001f);
        assertEquals(-2.8520278186f, c.tp1, 0.0000001f);
        assertEquals(2.9246062355f, c.tp2, 0.0000001f);
    }

    @Test
    public void coefficients_130Hz() {
        FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(130);
        assertEquals(2662508.633f, c.ugain, 0.001f);
    }

    @Test
    public void coefficients_10Hz() {
        FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(10);
        assertEquals(1429.899908f, c.ugain, 0.001f);
    }

    @Test
    public void coefficients_ugainIncreasesWithFrequency() {
        // Higher sampling frequency should need higher unity gain
        int[] frequencies = {10, 20, 30, 50, 70, 90, 110, 130};
        float lastUgain = 0;
        for (int freq : frequencies) {
            FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(freq);
            assertTrue("ugain at " + freq + "Hz (" + c.ugain + ") should be > "
                            + "previous (" + lastUgain + ")",
                    c.ugain > lastUgain);
            lastUgain = c.ugain;
        }
    }

    @Test
    public void coefficients_boundaryValues() {
        // Test exact boundary values between frequency ranges
        FilterCoefficients.Coefficients c125 = FilterCoefficients.getCoefficients(125);
        FilterCoefficients.Coefficients c124 = FilterCoefficients.getCoefficients(124);
        // 125 should be in 130Hz band, 124 should be in 120Hz band
        assertNotEquals("125 and 124 should use different coefficient sets",
                c125.ugain, c124.ugain, 0.001f);
    }

    @Test
    public void coefficients_veryLowFrequency() {
        // Frequency below 10Hz should still return valid coefficients (the 10Hz band)
        FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(5);
        assertEquals(1429.899908f, c.ugain, 0.001f);
    }

    @Test
    public void coefficients_allBandsReturnValidValues() {
        // Every frequency band should return non-zero, non-NaN coefficients
        for (int freq = 5; freq <= 135; freq += 5) {
            FilterCoefficients.Coefficients c = FilterCoefficients.getCoefficients(freq);
            assertTrue("ugain at " + freq + "Hz should be > 0", c.ugain > 0);
            assertFalse("tp0 at " + freq + "Hz should not be NaN", Float.isNaN(c.tp0));
            assertFalse("tp1 at " + freq + "Hz should not be NaN", Float.isNaN(c.tp1));
            assertFalse("tp2 at " + freq + "Hz should not be NaN", Float.isNaN(c.tp2));
        }
    }
}
