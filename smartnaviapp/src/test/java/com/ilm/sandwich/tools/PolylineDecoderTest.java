package com.ilm.sandwich.tools;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for Google Maps encoded polyline decoding.
 */
public class PolylineDecoderTest {

    private static final double EPSILON = 0.00001;

    @Test
    public void nullInput_returnsEmptyList() {
        List<double[]> result = PolylineDecoder.decode(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void emptyInput_returnsEmptyList() {
        List<double[]> result = PolylineDecoder.decode("");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void singlePoint_origin() {
        // (0.0, 0.0) encodes to "??"
        // lat=0: 0 << 1 = 0, chunks=[0], 0+63='?'
        // lng=0: same
        List<double[]> result = PolylineDecoder.decode("??");
        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0)[0], EPSILON);
        assertEquals(0.0, result.get(0)[1], EPSILON);
    }

    @Test
    public void singlePoint_smallPositive() {
        // (0.00001, 0.00001): lat=1, lng=1
        // 1 << 1 = 2 (positive), chunk=2, 2+63=65='A'
        List<double[]> result = PolylineDecoder.decode("AA");
        assertEquals(1, result.size());
        assertEquals(0.00001, result.get(0)[0], EPSILON);
        assertEquals(0.00001, result.get(0)[1], EPSILON);
    }

    @Test
    public void singlePoint_smallNegative() {
        // (-0.00001, -0.00001): lat=-1, lng=-1
        // -1 << 1 = -2, invert = 1, chunk=1, 1+63=64='@'
        List<double[]> result = PolylineDecoder.decode("@@");
        assertEquals(1, result.size());
        assertEquals(-0.00001, result.get(0)[0], EPSILON);
        assertEquals(-0.00001, result.get(0)[1], EPSILON);
    }

    @Test
    public void twoPoints_incrementalEncoding() {
        // Point 1: (0.00001, 0.00001) -> "AA"
        // Point 2: (0.00002, 0.00002) -> delta=(1,1) -> "AA"
        // Full: "AAAA"
        List<double[]> result = PolylineDecoder.decode("AAAA");
        assertEquals(2, result.size());
        assertEquals(0.00001, result.get(0)[0], EPSILON);
        assertEquals(0.00001, result.get(0)[1], EPSILON);
        assertEquals(0.00002, result.get(1)[0], EPSILON);
        assertEquals(0.00002, result.get(1)[1], EPSILON);
    }

    @Test
    public void googleDocumentedExample_firstPoint() {
        // From Google's Encoded Polyline Algorithm documentation:
        // "_p~iF~ps|U" encodes the first point (38.5, -120.2)
        List<double[]> result = PolylineDecoder.decode("_p~iF~ps|U");
        assertEquals(1, result.size());
        assertEquals(38.5, result.get(0)[0], EPSILON);
        assertEquals(-120.2, result.get(0)[1], EPSILON);
    }

    @Test
    public void googleDocumentedExample_threePoints() {
        // Full Google example: [(38.5, -120.2), (40.7, -120.95), (43.252, -126.453)]
        // Encoded: "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        List<double[]> result = PolylineDecoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
        assertEquals(3, result.size());

        assertEquals(38.5, result.get(0)[0], EPSILON);
        assertEquals(-120.2, result.get(0)[1], EPSILON);

        assertEquals(40.7, result.get(1)[0], EPSILON);
        assertEquals(-120.95, result.get(1)[1], EPSILON);

        assertEquals(43.252, result.get(2)[0], EPSILON);
        assertEquals(-126.453, result.get(2)[1], EPSILON);
    }

    @Test
    public void multiplePoints_accumulateCorrectly() {
        // Verify that delta encoding accumulates properly
        // First point: "AA" = (0.00001, 0.00001)
        // Each subsequent "AA" adds (0.00001, 0.00001) as delta
        List<double[]> result = PolylineDecoder.decode("AAAAAA");
        assertEquals(3, result.size());
        assertEquals(0.00001, result.get(0)[0], EPSILON);
        assertEquals(0.00002, result.get(1)[0], EPSILON);
        assertEquals(0.00003, result.get(2)[0], EPSILON);
    }
}
