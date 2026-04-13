package com.ilm.sandwich.tools;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for RouteManager distance/duration formatting and input validation.
 */
public class RouteManagerTest {

    // --- formatDistanceInfo language tests ---

    @Test
    public void formatDistanceInfo_german() {
        String result = RouteManager.formatDistanceInfo("2.5 km", "30 min", "de");
        assertTrue(result.contains("Ziel ist"));
        assertTrue(result.contains("2.5 km"));
        assertTrue(result.contains("30 min"));
        assertTrue(result.contains("entfernt"));
    }

    @Test
    public void formatDistanceInfo_english() {
        String result = RouteManager.formatDistanceInfo("1.5 mi", "20 min", "en");
        assertTrue(result.contains("Destination is"));
        assertTrue(result.contains("1.5 mi"));
        assertTrue(result.contains("20 min"));
        assertTrue(result.contains("away"));
    }

    @Test
    public void formatDistanceInfo_spanish() {
        String result = RouteManager.formatDistanceInfo("3 km", "45 min", "es");
        assertTrue(result.contains("Destino"));
        assertTrue(result.contains("3 km"));
        assertTrue(result.contains("45 min"));
    }

    @Test
    public void formatDistanceInfo_french() {
        String result = RouteManager.formatDistanceInfo("5 km", "1 hour", "fr");
        assertTrue(result.contains("Destination est"));
        assertTrue(result.contains("5 km"));
        assertTrue(result.contains("1 hour"));
    }

    @Test
    public void formatDistanceInfo_polish() {
        String result = RouteManager.formatDistanceInfo("2 km", "25 min", "pl");
        assertTrue(result.contains("Docelowy"));
        assertTrue(result.contains("2 km"));
        assertTrue(result.contains("25 min"));
    }

    @Test
    public void formatDistanceInfo_italian() {
        String result = RouteManager.formatDistanceInfo("4 km", "50 min", "it");
        assertTrue(result.contains("Destination si trova"));
        assertTrue(result.contains("4 km"));
        assertTrue(result.contains("50 min"));
    }

    @Test
    public void formatDistanceInfo_unsupportedLanguage_fallback() {
        String result = RouteManager.formatDistanceInfo("3 km", "35 min", "ja");
        assertTrue(result.contains("Distance:"));
        assertTrue(result.contains("3 km"));
        assertTrue(result.contains("35 min"));
    }

    @Test
    public void formatDistanceInfo_caseInsensitive() {
        String resultUpper = RouteManager.formatDistanceInfo("1 km", "10 min", "DE");
        String resultLower = RouteManager.formatDistanceInfo("1 km", "10 min", "de");
        assertEquals("Language matching should be case-insensitive", resultUpper, resultLower);
    }

    @Test
    public void formatDistanceInfo_containsNewline() {
        // All formats should contain a newline for two-line display
        String result = RouteManager.formatDistanceInfo("5 km", "1 h", "en");
        assertTrue("Should contain newline", result.contains("\n"));
    }

    // --- RouteResult.failure() tests ---

    @Test
    public void routeResult_failure_isNotSuccess() {
        RouteManager.RouteResult failure = RouteManager.RouteResult.failure();
        assertFalse(failure.success);
        assertNull(failure.endAddress);
        assertNull(failure.distanceInfo);
        assertNull(failure.northeastBound);
        assertNull(failure.southwestBound);
        assertNull(failure.stepsArray);
        assertEquals(0, failure.phases);
    }
}
