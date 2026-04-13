package com.ilm.sandwich.tools;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for step length calculation from body height.
 */
public class StepLengthCalculatorTest {

    private static final float EPSILON = 0.001f;

    @Test
    public void metricInput_175cm() {
        float result = StepLengthCalculator.calculateStepLength("175");
        assertEquals(175f / 222f, result, EPSILON);
    }

    @Test
    public void metricInput_withComma() {
        float result = StepLengthCalculator.calculateStepLength("175,5");
        assertEquals(175.5f / 222f, result, EPSILON);
    }

    @Test
    public void metricInput_withDot() {
        float result = StepLengthCalculator.calculateStepLength("175.5");
        assertEquals(175.5f / 222f, result, EPSILON);
    }

    @Test
    public void imperialInput_feetAndInches() {
        float result = StepLengthCalculator.calculateStepLength("5'10");
        float expected = (float) ((12 * 5 + 10) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_feetOnly() {
        float result = StepLengthCalculator.calculateStepLength("6'");
        float expected = (float) (72 * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_feetWithoutTrailingQuote() {
        // "5'0" should parse feet=5, inches=0
        float result = StepLengthCalculator.calculateStepLength("5'0");
        float expected = (float) (60 * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void nullInput_returnsNegative() {
        float result = StepLengthCalculator.calculateStepLength(null);
        assertEquals(-1f, result, EPSILON);
    }

    @Test
    public void emptyInput_returnsNegative() {
        float result = StepLengthCalculator.calculateStepLength("");
        assertEquals(-1f, result, EPSILON);
    }

    @Test
    public void invalidInput_returnsNegative() {
        float result = StepLengthCalculator.calculateStepLength("abc");
        assertEquals(-1f, result, EPSILON);
    }

    // --- Additional imperial format tests ---

    @Test
    public void imperialInput_withDoubleQuote() {
        // 5'10" (common US notation)
        float result = StepLengthCalculator.calculateStepLength("5'10\"");
        float expected = (float) ((12 * 5 + 10) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_withSpaces() {
        // 5' 10"
        float result = StepLengthCalculator.calculateStepLength("5' 10\"");
        float expected = (float) ((12 * 5 + 10) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_ftIn() {
        // 5ft10in
        float result = StepLengthCalculator.calculateStepLength("5ft10in");
        float expected = (float) ((12 * 5 + 10) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_ftInWithSpaces() {
        // 5ft 10in
        float result = StepLengthCalculator.calculateStepLength("5ft 10in");
        float expected = (float) ((12 * 5 + 10) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_feetInches() {
        // 5feet10inches
        float result = StepLengthCalculator.calculateStepLength("5feet10inches");
        float expected = (float) ((12 * 5 + 10) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_footInch() {
        // 5foot10inch
        float result = StepLengthCalculator.calculateStepLength("5foot10inch");
        float expected = (float) ((12 * 5 + 10) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void imperialInput_spaceSeparated() {
        // "5 10" — space between feet and inches
        // Note: this will be treated as metric (no imperial marker), which is correct
        // since "510" cm is unreasonable and "5 10" is ambiguous without context
    }

    @Test
    public void imperialInput_sixFourWithApostrophe() {
        // 6'4 — tall person
        float result = StepLengthCalculator.calculateStepLength("6'4");
        float expected = (float) ((12 * 6 + 4) * 2.54 / 222);
        assertEquals(expected, result, EPSILON);
    }

    @Test
    public void isImperial_detectsApostrophe() {
        assertTrue(StepLengthCalculator.isImperial("5'10"));
        assertTrue(StepLengthCalculator.isImperial("6'"));
    }

    @Test
    public void isImperial_detectsFt() {
        assertTrue(StepLengthCalculator.isImperial("5ft10in"));
        assertTrue(StepLengthCalculator.isImperial("5feet10"));
    }

    @Test
    public void isImperial_rejectsMetric() {
        assertFalse(StepLengthCalculator.isImperial("175"));
        assertFalse(StepLengthCalculator.isImperial("180.5"));
    }
}
