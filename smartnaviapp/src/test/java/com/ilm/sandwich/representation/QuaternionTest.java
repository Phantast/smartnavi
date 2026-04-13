package com.ilm.sandwich.representation;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for Quaternion math operations.
 * Includes regression tests for the multiplyByScalar infinite recursion bug.
 */
public class QuaternionTest {

    private static final float EPSILON = 0.0001f;

    @Test
    public void multiplyByScalar_doesNotStackOverflow() {
        // Regression test: previously called itself recursively
        Quaternion q = new Quaternion();
        q.setXYZW(1, 2, 3, 4);
        q.multiplyByScalar(2.0f);
        assertEquals(2.0f, q.x(), EPSILON);
        assertEquals(4.0f, q.y(), EPSILON);
        assertEquals(6.0f, q.z(), EPSILON);
        assertEquals(8.0f, q.w(), EPSILON);
    }

    @Test
    public void multiplyByScalar_zero() {
        Quaternion q = new Quaternion();
        q.setXYZW(1, 2, 3, 4);
        q.multiplyByScalar(0.0f);
        assertEquals(0.0f, q.x(), EPSILON);
        assertEquals(0.0f, q.y(), EPSILON);
        assertEquals(0.0f, q.z(), EPSILON);
        assertEquals(0.0f, q.w(), EPSILON);
    }

    @Test
    public void multiplyByScalar_one() {
        Quaternion q = new Quaternion();
        q.setXYZW(1, 2, 3, 4);
        q.multiplyByScalar(1.0f);
        assertEquals(1.0f, q.x(), EPSILON);
        assertEquals(2.0f, q.y(), EPSILON);
        assertEquals(3.0f, q.z(), EPSILON);
        assertEquals(4.0f, q.w(), EPSILON);
    }

    @Test
    public void dotProduct_identity() {
        Quaternion q1 = new Quaternion();
        q1.setXYZW(1, 0, 0, 0);
        Quaternion q2 = new Quaternion();
        q2.setXYZW(1, 0, 0, 0);
        float dot = q1.dotProduct(q2);
        assertEquals(1.0f, dot, EPSILON);
    }

    @Test
    public void dotProduct_orthogonal() {
        Quaternion q1 = new Quaternion();
        q1.setXYZW(1, 0, 0, 0);
        Quaternion q2 = new Quaternion();
        q2.setXYZW(0, 1, 0, 0);
        float dot = q1.dotProduct(q2);
        assertEquals(0.0f, dot, EPSILON);
    }

    @Test
    public void addQuat() {
        Quaternion q1 = new Quaternion();
        q1.setXYZW(1, 2, 3, 4);
        Quaternion q2 = new Quaternion();
        q2.setXYZW(5, 6, 7, 8);
        q1.addQuat(q2);
        assertEquals(6.0f, q1.x(), EPSILON);
        assertEquals(8.0f, q1.y(), EPSILON);
        assertEquals(10.0f, q1.z(), EPSILON);
        assertEquals(12.0f, q1.w(), EPSILON);
    }
}
