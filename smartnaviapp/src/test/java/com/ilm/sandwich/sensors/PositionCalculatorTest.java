package com.ilm.sandwich.sensors;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for dead-reckoning position calculation from step + azimuth.
 */
public class PositionCalculatorTest {

    private static final double EPSILON = 0.0000001;

    // At 52 degrees latitude (Berlin), 1 degree of longitude is about 68.7 km
    private static final double DISTANCE_LONGITUDE_BERLIN = 68.7;
    // At the equator, 1 degree of longitude is about 111.32 km
    private static final double DISTANCE_LONGITUDE_EQUATOR = 111.32;
    // Standard step length in meters (~0.79m for 175cm person)
    private static final float STEP_LENGTH = 175f / 222f;

    @Test
    public void walkNorth_latitudeIncreases() {
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 0.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
        assertTrue("Latitude should increase when walking north",
                result[0] > 52.0);
        assertEquals("Longitude should not change when walking due north",
                13.0, result[1], EPSILON);
    }

    @Test
    public void walkSouth_latitudeDecreases() {
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 180.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
        assertTrue("Latitude should decrease when walking south",
                result[0] < 52.0);
        // At exactly 180 degrees, sin(180)=0 so longitude shouldn't change
        assertEquals("Longitude should not change when walking due south",
                13.0, result[1], EPSILON);
    }

    @Test
    public void walkEast_longitudeIncreases() {
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 90.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
        assertTrue("Longitude should increase when walking east",
                result[1] > 13.0);
        // At exactly 90 degrees, cos(90)=0 so latitude shouldn't change
        assertEquals("Latitude should not change when walking due east",
                52.0, result[0], EPSILON);
    }

    @Test
    public void walkWest_longitudeDecreases() {
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 270.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
        assertTrue("Longitude should decrease when walking west",
                result[1] < 13.0);
        // At exactly 270 degrees, cos(270)=0 so latitude shouldn't change
        assertEquals("Latitude should not change when walking due west",
                52.0, result[0], EPSILON);
    }

    @Test
    public void walkNortheast_bothIncrease() {
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 45.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
        assertTrue("Latitude should increase when walking NE",
                result[0] > 52.0);
        assertTrue("Longitude should increase when walking NE",
                result[1] > 13.0);
    }

    @Test
    public void walkSouthwest_bothDecrease() {
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 225.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
        assertTrue("Latitude should decrease when walking SW",
                result[0] < 52.0);
        assertTrue("Longitude should decrease when walking SW",
                result[1] < 13.0);
    }

    @Test
    public void southernHemisphere_walkNorth_latitudeIncreases() {
        // Sydney: -33.87, 151.21
        double[] result = PositionCalculator.computeNewPosition(
                -33.87, 151.21, 0.0, STEP_LENGTH, DISTANCE_LONGITUDE_EQUATOR);
        assertTrue("Walking north from southern hemisphere should increase latitude",
                result[0] > -33.87);
    }

    @Test
    public void southernHemisphere_walkSouth_latitudeDecreases() {
        double[] result = PositionCalculator.computeNewPosition(
                -33.87, 151.21, 180.0, STEP_LENGTH, DISTANCE_LONGITUDE_EQUATOR);
        assertTrue("Walking south from southern hemisphere should decrease latitude",
                result[0] < -33.87);
    }

    @Test
    public void stepLength_affectsDistance() {
        double[] shortStep = PositionCalculator.computeNewPosition(
                52.0, 13.0, 0.0, 0.5f, DISTANCE_LONGITUDE_BERLIN);
        double[] longStep = PositionCalculator.computeNewPosition(
                52.0, 13.0, 0.0, 1.0f, DISTANCE_LONGITUDE_BERLIN);
        double shortDelta = shortStep[0] - 52.0;
        double longDelta = longStep[0] - 52.0;
        // Long step should move exactly twice as far as short step
        assertEquals(longDelta, shortDelta * 2, EPSILON);
    }

    @Test
    public void azimuth355_isNorthward() {
        // 355 degrees is nearly north (5 degrees west of north)
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 355.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
        assertTrue("355 degrees should be northward (lat increases)",
                result[0] > 52.0);
        // 355 degrees is west of north, so longitude should decrease
        assertTrue("355 degrees should be slightly west (lon decreases)",
                result[1] < 13.0);
    }

    @Test
    public void multipleSteps_accumulateCorrectly() {
        double lat = 52.0, lon = 13.0;
        int steps = 100;
        for (int i = 0; i < steps; i++) {
            double[] result = PositionCalculator.computeNewPosition(
                    lat, lon, 0.0, STEP_LENGTH, DISTANCE_LONGITUDE_BERLIN);
            lat = result[0];
            lon = result[1];
        }
        // After 100 steps north at ~0.79m/step, should move ~79 meters north
        // 79m * 0.000008984725966 deg/m = ~0.0007098 degrees
        double expectedDelta = 100 * STEP_LENGTH * 0.000008984725966;
        assertEquals(52.0 + expectedDelta, lat, EPSILON);
        assertEquals(13.0, lon, EPSILON); // No east-west movement
    }

    @Test
    public void zeroStepLength_noMovement() {
        double[] result = PositionCalculator.computeNewPosition(
                52.0, 13.0, 45.0, 0.0f, DISTANCE_LONGITUDE_BERLIN);
        assertEquals(52.0, result[0], EPSILON);
        assertEquals(13.0, result[1], EPSILON);
    }
}
