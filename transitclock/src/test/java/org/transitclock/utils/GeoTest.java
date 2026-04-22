package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.transitclock.db.structs.Location;
import org.transitclock.db.structs.Vector;

public class GeoTest {

	private static final double TOL_METERS = 1.0;

	@Test
	public void unitConstantsAreConsistent() {
		assertEquals(1.0f, Geo.KPH_TO_MPS * 3600f / 1000f, 0.001f);
		assertEquals(1.0f, Geo.MPH_TO_MPS * Geo.MPS_TO_MPH, 0.001f);
		assertEquals(Geo.KPH_TO_MPS * 10f,
				Geo.converKmPerHrToMetersPerSecond(10f), 0.0001f);
	}

	@Test
	public void distanceBetweenIdenticalPointsIsZero() {
		Location l = new Location(37.5, -122.0);
		assertEquals(0.0, Geo.distance(l, l), 1e-9);
		assertEquals(0.0, Geo.distanceHaversine(l, l), 1e-9);
	}

	@Test
	public void oneDegreeOfLatitudeIsAboutMeridianArc() {
		double expected = Math.PI * Geo.RADIUS_OF_EARTH_IN_METERS / 180.0;
		Location a = new Location(0.0, 0.0);
		Location b = new Location(1.0, 0.0);
		assertEquals(expected, Geo.distance(a, b), TOL_METERS);
		assertEquals(expected, Geo.distanceHaversine(a, b), TOL_METERS);
	}

	@Test
	public void haversineAndEquirectangularAgreeOnSmallDistances() {
		Location a = new Location(37.80000, -122.43600);
		Location b = new Location(37.80050, -122.43650);
		double haversine = Geo.distanceHaversine(a, b);
		double equirect = Geo.distance(a, b);
		assertEquals(haversine, equirect, 0.1);
	}

	@Test
	public void distanceLongitudeShrinksAtHighLatitude() {
		Location equatorA = new Location(0.0, 0.0);
		Location equatorB = new Location(0.0, 1.0);
		Location highA = new Location(60.0, 0.0);
		Location highB = new Location(60.0, 1.0);
		double atEquator = Geo.distance(equatorA, equatorB);
		double atSixty = Geo.distance(highA, highB);
		// cos(60°) = 0.5, so the arc length should be about half.
		assertEquals(0.5, atSixty / atEquator, 0.005);
	}

	@Test
	public void distanceToVectorPerpendicularInsideSegment() {
		Location v1 = new Location(0.0, -0.001);
		Location v2 = new Location(0.0, 0.001);
		Vector v = new Vector(v1, v2);
		Location p = new Location(0.001, 0.0); // 0.001 deg north of midpoint
		double expected = Math.PI * Geo.RADIUS_OF_EARTH_IN_METERS / 180.0 * 0.001;
		assertEquals(expected, Geo.distance(p, v), TOL_METERS);
		assertEquals(expected, Geo.distanceIfMatch(p, v), TOL_METERS);
	}

	@Test
	public void distanceBeyondEndOfVectorFallsBackToEndpoint() {
		Location v1 = new Location(0.0, -0.001);
		Location v2 = new Location(0.0, 0.001);
		Vector v = new Vector(v1, v2);
		Location pastEnd = new Location(0.0, 0.005);
		double fromEnd = Geo.distance(pastEnd, v2);
		assertEquals(fromEnd, Geo.distance(pastEnd, v), 1e-6);
		assertTrue("past end must not match",
				Double.isNaN(Geo.distanceIfMatch(pastEnd, v)));
	}

	@Test
	public void distanceBeforeStartOfVectorFallsBackToStartPoint() {
		Location v1 = new Location(0.0, -0.001);
		Location v2 = new Location(0.0, 0.001);
		Vector v = new Vector(v1, v2);
		Location beforeStart = new Location(0.0, -0.005);
		double fromStart = Geo.distance(beforeStart, v1);
		assertEquals(fromStart, Geo.distance(beforeStart, v), 1e-6);
		assertTrue("before start must not match",
				Double.isNaN(Geo.distanceIfMatch(beforeStart, v)));
	}

	@Test
	public void zeroLengthVectorHandledWithoutNaNInDistance() {
		Location p = new Location(0.0, 0.0);
		Location shared = new Location(0.001, 0.001);
		Vector v = new Vector(shared, shared);
		double expected = Geo.distance(p, shared);
		assertEquals(expected, Geo.distance(p, v), 1e-9);
		assertTrue(Double.isNaN(Geo.distanceIfMatch(p, v)));
		assertEquals(0.0, Geo.matchDistanceAlongVector(p, v), 1e-9);
	}

	@Test
	public void matchDistanceAlongVectorMidpointIsHalfLength() {
		Location v1 = new Location(0.0, -0.001);
		Location v2 = new Location(0.0, 0.001);
		Vector v = new Vector(v1, v2);
		Location mid = new Location(0.001, 0.0);
		double fullLen = Geo.distance(v1, v2);
		assertEquals(fullLen / 2, Geo.matchDistanceAlongVector(mid, v),
				TOL_METERS);
	}

	@Test
	public void matchDistanceAlongVectorClampsOutsideSegment() {
		Location v1 = new Location(0.0, -0.001);
		Location v2 = new Location(0.0, 0.001);
		Vector v = new Vector(v1, v2);
		double fullLen = Geo.distance(v1, v2);
		Location pastEnd = new Location(0.0, 0.005);
		Location beforeStart = new Location(0.0, -0.005);
		assertEquals(fullLen,
				Geo.matchDistanceAlongVector(pastEnd, v), 1e-9);
		assertEquals(0.0,
				Geo.matchDistanceAlongVector(beforeStart, v), 1e-9);
	}

	@Test
	public void offsetByZeroIsIdentity() {
		Location l = new Location(37.5, -122.0);
		Location same = Geo.offset(l, 0.0, 0.0);
		assertEquals(l.getLat(), same.getLat(), 1e-12);
		assertEquals(l.getLon(), same.getLon(), 1e-12);
	}

	@Test
	public void offsetAndInverseOffsetRoundTrip() {
		Location l = new Location(37.5, -122.0);
		Location shifted = Geo.offset(l, 100.0, 50.0);
		Location back = Geo.offset(shifted, -100.0, -50.0);
		assertEquals(l.getLat(), back.getLat(), 1e-6);
		assertEquals(l.getLon(), back.getLon(), 1e-6);
	}

	@Test
	public void offsetPushesLatAndLonTheRightWay() {
		Location l = new Location(0.0, 0.0);
		Location shifted = Geo.offset(l, 111195.0, 111195.0);
		assertEquals(1.0, shifted.getLat(), 0.001);
		assertEquals(1.0, shifted.getLon(), 0.001);
	}

	@Test
	public void headingOKWhenVehicleHeadingUnknown() {
		assertTrue(Geo.headingOK(Float.NaN, 123f, 10f));
	}

	@Test
	public void headingOKWrapsAcrossZero() {
		assertTrue("10° vs 350° should be within 30°",
				Geo.headingOK(10f, 350f, 30f));
		assertTrue("350° vs 10° should be within 30°",
				Geo.headingOK(350f, 10f, 30f));
	}

	@Test
	public void headingOKIsStrict() {
		assertFalse("delta equal to allowable is not ok (strict <)",
				Geo.headingOK(30f, 0f, 30f));
		assertTrue(Geo.headingOK(29f, 0f, 30f));
	}

	@Test
	public void headingOKRejectsLargeDelta() {
		assertFalse(Geo.headingOK(90f, 0f, 30f));
		assertFalse(Geo.headingOK(180f, 0f, 30f));
	}

	@Test
	public void distanceFormatHandlesSpecialValues() {
		assertEquals("NaN", Geo.distanceFormat(Double.NaN));
		assertEquals("Double.MAX_VALUE", Geo.distanceFormat(Double.MAX_VALUE));
		assertEquals("null", Geo.distanceFormat((Double) null));
	}

	@Test
	public void headingFormatHandlesNaN() {
		assertEquals("NaN", Geo.headingFormat(Float.NaN));
	}

	@Test
	public void speedFormatHandlesNaN() {
		assertEquals("NaN", Geo.speedFormat(Float.NaN));
	}
}
