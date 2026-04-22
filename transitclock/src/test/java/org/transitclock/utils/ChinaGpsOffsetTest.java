package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.transitclock.utils.ChinaGpsOffset.LatLon;

/**
 * ChinaGpsOffset converts between WGS84 and GCJ-02 ("Mars") coordinates,
 * which is required for displaying China-region GPS data on consumer maps.
 * The offset is non-trivial (tens to hundreds of meters), so the roundtrip
 * accuracy and the China-bounding-box gating are both load-bearing.
 */
public class ChinaGpsOffsetTest {

	@Test
	public void outOfChinaBoundingBox() {
		// Lon too low (west of 72.004)
		assertTrue(ChinaGpsOffset.outOfChina(30.0, 71.0));
		// Lon too high (east of 137.8347)
		assertTrue(ChinaGpsOffset.outOfChina(30.0, 138.0));
		// Lat too low (south of 0.8293)
		assertTrue(ChinaGpsOffset.outOfChina(0.0, 110.0));
		// Lat too high (north of 55.8271)
		assertTrue(ChinaGpsOffset.outOfChina(56.0, 110.0));
	}

	@Test
	public void insideChinaBoundingBox() {
		// Beijing
		assertFalse(ChinaGpsOffset.outOfChina(39.9042, 116.4074));
		// Zhengzhou (used in the class's own main())
		assertFalse(ChinaGpsOffset.outOfChina(34.79521, 113.69259));
		// Shanghai
		assertFalse(ChinaGpsOffset.outOfChina(31.2304, 121.4737));
	}

	@Test
	public void transformIsIdentityOutsideChina() {
		// Seattle — no Mars offset applied, result must equal input exactly.
		LatLon result = ChinaGpsOffset.transform(47.6062, -122.3321);
		assertEquals(47.6062, result.getLat(), 0.0);
		assertEquals(-122.3321, result.getLon(), 0.0);
	}

	@Test
	public void transformBackIsIdentityOutsideChina() {
		// transformBack iterates transform(); if transform is identity then
		// transformBack must be too.
		LatLon result = ChinaGpsOffset.transformBack(47.6062, -122.3321);
		assertEquals(47.6062, result.getLat(), 0.0);
		assertEquals(-122.3321, result.getLon(), 0.0);
	}

	@Test
	public void transformShiftsCoordinatesInsideChina() {
		// Zhengzhou. The Mars offset in central China is about 0.001–0.01 deg
		// in each axis. Just assert that _some_ nonzero shift is applied.
		double origLat = 34.79521;
		double origLon = 113.69259;
		LatLon mars = ChinaGpsOffset.transform(origLat, origLon);
		assertTrue("lat should shift", Math.abs(mars.getLat() - origLat) > 1e-6);
		assertTrue("lon should shift", Math.abs(mars.getLon() - origLon) > 1e-6);
		// Shift is bounded — should be well under 0.02 degrees in magnitude,
		// which would be ~2 km and unreasonable.
		assertTrue(Math.abs(mars.getLat() - origLat) < 0.02);
		assertTrue(Math.abs(mars.getLon() - origLon) < 0.02);
	}

	@Test
	public void transformBackRoundTripsInsideChina() {
		// transformBack does 3 iterations of the inverse; the class docstring
		// claims ~7-decimal-place accuracy after 3 iterations.
		double origLat = 34.79521;
		double origLon = 113.69259;
		LatLon mars = ChinaGpsOffset.transform(origLat, origLon);
		LatLon back = ChinaGpsOffset.transformBack(mars.getLat(), mars.getLon());
		assertEquals(origLat, back.getLat(), 1e-6);
		assertEquals(origLon, back.getLon(), 1e-6);
	}

	@Test
	public void roundTripAccurateForMultipleChinaLocations() {
		double[][] locations = {
				{39.9042, 116.4074}, // Beijing
				{31.2304, 121.4737}, // Shanghai
				{22.3193, 114.1694}, // near Hong Kong border (just inside bbox)
				{43.8383, 125.3245}, // Changchun
		};
		for (double[] loc : locations) {
			double origLat = loc[0];
			double origLon = loc[1];
			LatLon mars = ChinaGpsOffset.transform(origLat, origLon);
			LatLon back = ChinaGpsOffset.transformBack(mars.getLat(), mars.getLon());
			assertEquals("lat for " + origLat + "," + origLon,
					origLat, back.getLat(), 1e-5);
			assertEquals("lon for " + origLat + "," + origLon,
					origLon, back.getLon(), 1e-5);
		}
	}

	@Test
	public void latLonHoldsConstructedValues() {
		LatLon ll = new LatLon(12.5, -77.25);
		assertEquals(12.5, ll.getLat(), 0.0);
		assertEquals(-77.25, ll.getLon(), 0.0);
	}
}
