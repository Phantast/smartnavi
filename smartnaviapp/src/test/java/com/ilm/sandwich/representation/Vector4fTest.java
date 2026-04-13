package com.ilm.sandwich.representation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for Vector4f operations.
 * Includes regression test for the LERP formula bug.
 */
public class Vector4fTest {

    private static final float EPSILON = 0.0001f;

    @Test
    public void lerp_atZero_returnsFirst() {
        // Regression test: previously used (1.0f * t) instead of (1.0f - t)
        Vector4f a = new Vector4f();
        a.setXYZW(1, 2, 3, 4);
        Vector4f b = new Vector4f();
        b.setXYZW(5, 6, 7, 8);
        Vector4f out = new Vector4f();

        a.lerp(b, out, 0.0f);

        assertEquals(1.0f, out.x(), EPSILON);
        assertEquals(2.0f, out.y(), EPSILON);
        assertEquals(3.0f, out.z(), EPSILON);
        assertEquals(4.0f, out.w(), EPSILON);
    }

    @Test
    public void lerp_atOne_returnsSecond() {
        Vector4f a = new Vector4f();
        a.setXYZW(1, 2, 3, 4);
        Vector4f b = new Vector4f();
        b.setXYZW(5, 6, 7, 8);
        Vector4f out = new Vector4f();

        a.lerp(b, out, 1.0f);

        assertEquals(5.0f, out.x(), EPSILON);
        assertEquals(6.0f, out.y(), EPSILON);
        assertEquals(7.0f, out.z(), EPSILON);
        assertEquals(8.0f, out.w(), EPSILON);
    }

    @Test
    public void lerp_atHalf_returnsMidpoint() {
        Vector4f a = new Vector4f();
        a.setXYZW(0, 0, 0, 0);
        Vector4f b = new Vector4f();
        b.setXYZW(10, 20, 30, 40);
        Vector4f out = new Vector4f();

        a.lerp(b, out, 0.5f);

        assertEquals(5.0f, out.x(), EPSILON);
        assertEquals(10.0f, out.y(), EPSILON);
        assertEquals(15.0f, out.z(), EPSILON);
        assertEquals(20.0f, out.w(), EPSILON);
    }

    @Test
    public void multiplyByScalar() {
        Vector4f v = new Vector4f();
        v.setXYZW(1, 2, 3, 4);
        v.multiplyByScalar(3.0f);
        assertEquals(3.0f, v.x(), EPSILON);
        assertEquals(6.0f, v.y(), EPSILON);
        assertEquals(9.0f, v.z(), EPSILON);
        assertEquals(12.0f, v.w(), EPSILON);
    }

    @Test
    public void dotProduct() {
        Vector4f a = new Vector4f();
        a.setXYZW(1, 2, 3, 4);
        Vector4f b = new Vector4f();
        b.setXYZW(5, 6, 7, 8);
        // 1*5 + 2*6 + 3*7 + 4*8 = 5+12+21+32 = 70
        float dot = a.dotProduct(b);
        assertEquals(70.0f, dot, EPSILON);
    }
}
