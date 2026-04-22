package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.transitclock.utils.Geo;

public class VectorTest {

	private static final double TOL = 1e-6;

	@Test
	public void gettersReturnConstructorEndpoints() {
		Location a = new Location(0.0, 0.0);
		Location b = new Location(1.0, 0.0);
		Vector v = new Vector(a, b);
		assertSame(a, v.getL1());
		assertSame(b, v.getL2());
	}

	@Test
	public void lengthMatchesGeoDistance() {
		Location a = new Location(0.0, 0.0);
		Location b = new Location(1.0, 0.0);
		Vector v = new Vector(a, b);
		assertEquals(Geo.distance(a, b), v.length(), 1e-9);
	}

	@Test
	public void lengthOfZeroVectorIsZero() {
		Location a = new Location(37.5, -122.25);
		assertEquals(0.0, new Vector(a, a).length(), 1e-12);
	}

	@Test
	public void distanceDelegatesToGeo() {
		Vector v = new Vector(new Location(-1.0, 0.0), new Location(1.0, 0.0));
		Location p = new Location(0.0, 0.001);
		assertEquals(Geo.distance(p, v), v.distance(p), 1e-9);
	}

	@Test
	public void matchDistanceAlongVectorDelegatesToGeo() {
		Vector v = new Vector(new Location(-1.0, 0.0), new Location(1.0, 0.0));
		Location p = new Location(0.0, 0.001);
		assertEquals(Geo.matchDistanceAlongVector(p, v),
				v.matchDistanceAlongVector(p), 1e-9);
	}

	@Test
	public void headingForCardinalDirections() {
		// Heading: degrees clockwise from north. Not normalized to [0, 360).
		Location origin = new Location(0.0, 0.0);
		Vector north = new Vector(origin, new Location(0.001, 0.0));
		Vector east = new Vector(origin, new Location(0.0, 0.001));
		Vector south = new Vector(origin, new Location(-0.001, 0.0));
		Vector west = new Vector(origin, new Location(0.0, -0.001));

		assertEquals(0.0, north.heading(), TOL);
		assertEquals(90.0, east.heading(), TOL);
		assertEquals(180.0, south.heading(), TOL);
		assertEquals(-90.0, west.heading(), TOL);
	}

	@Test
	public void angleForCardinalDirections() {
		// Angle: radians counterclockwise from the equator (east = 0, north = +π/2).
		Location origin = new Location(0.0, 0.0);
		Vector east = new Vector(origin, new Location(0.0, 0.001));
		Vector north = new Vector(origin, new Location(0.001, 0.0));

		assertEquals(0.0, east.angle(), TOL);
		assertEquals(Math.PI / 2, north.angle(), TOL);
	}

	@Test
	public void beginningOfZeroLengthIsDegenerate() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(0.0, 0.002));
		Vector start = v.beginning(0.0);
		assertEquals(v.getL1(), start.getL1());
		assertEquals(v.getL1(), start.getL2());
		assertEquals(0.0, start.length(), 1e-9);
	}

	@Test
	public void beginningOfFullLengthReproducesOriginal() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(0.0, 0.002));
		Vector start = v.beginning(v.length());
		assertEquals(v.getL2(), start.getL2());
		assertEquals(v.length(), start.length(), 1e-6);
	}

	@Test
	public void beginningReturnsPrefixOfRequestedLength() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(0.0, 0.002));
		double target = v.length() / 2.0;
		Vector prefix = v.beginning(target);
		assertEquals(target, prefix.length(), 1e-6);
		assertEquals(v.getL1(), prefix.getL1());
	}

	@Test
	public void endReturnsSuffixStartingAtRequestedOffset() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(0.0, 0.002));
		double offset = v.length() / 2.0;
		Vector suffix = v.end(offset);
		assertEquals(v.getL2(), suffix.getL2());
		// Remaining suffix length is full length - offset.
		assertEquals(v.length() - offset, suffix.length(), 1e-6);
	}

	@Test
	public void beginningAndEndShareInteriorPoint() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(0.0, 0.002));
		double at = v.length() / 3.0;
		Location fromBeginning = v.beginning(at).getL2();
		Location fromEnd = v.end(at).getL1();
		// Both interpolate to the same ratio; points should match.
		assertEquals(fromBeginning.getLat(), fromEnd.getLat(), 1e-12);
		assertEquals(fromBeginning.getLon(), fromEnd.getLon(), 1e-12);
	}

	@Test
	public void locAlongVectorAgreesWithBeginningEndpoint() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(0.0, 0.002));
		double target = v.length() / 4.0;
		Location withLength = v.locAlongVector(target);
		Location viaBeginning = v.beginning(target).getL2();
		assertEquals(viaBeginning.getLat(), withLength.getLat(), 1e-12);
		assertEquals(viaBeginning.getLon(), withLength.getLon(), 1e-12);
	}

	@Test
	public void middleExtractsInteriorSlice() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(0.0, 0.004));
		double full = v.length();
		// Slice from 25% to 75% of the original length.
		Vector mid = v.middle(full * 0.25, full * 0.75);
		assertEquals(full * 0.5, mid.length(), 1e-6);
	}

	@Test
	public void zeroLengthVectorBeginningIsSafe() {
		Location a = new Location(0.0, 0.0);
		Vector zero = new Vector(a, a);
		Vector prefix = zero.beginning(100.0);
		// When length is zero, ratio clamps to 0 → degenerate vector at l1.
		assertEquals(a, prefix.getL1());
		assertEquals(a, prefix.getL2());
	}

	@Test
	public void toStringIncludesBothEndpointsAndLength() {
		Vector v = new Vector(new Location(0.0, 0.0), new Location(1.0, 0.0));
		String s = v.toString();
		assertTrue(s.startsWith("Vector ["));
		assertTrue(s.contains("l1="));
		assertTrue(s.contains("l2="));
		assertTrue(s.contains("length="));
	}
}
