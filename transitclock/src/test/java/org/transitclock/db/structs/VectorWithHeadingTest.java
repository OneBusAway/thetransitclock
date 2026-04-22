package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VectorWithHeadingTest {

	private static final float TOL = 1e-3f;

	@Test
	public void headingNormalizedToZeroThreeSixty() {
		Location origin = new Location(0.0, 0.0);
		VectorWithHeading north = new VectorWithHeading(origin,
				new Location(0.001, 0.0));
		VectorWithHeading east = new VectorWithHeading(origin,
				new Location(0.0, 0.001));
		VectorWithHeading south = new VectorWithHeading(origin,
				new Location(-0.001, 0.0));
		VectorWithHeading west = new VectorWithHeading(origin,
				new Location(0.0, -0.001));

		assertEquals(0.0f, north.getHeading(), TOL);
		assertEquals(90.0f, east.getHeading(), TOL);
		assertEquals(180.0f, south.getHeading(), TOL);
		// Plain Vector.heading() returns -90 for west; VectorWithHeading
		// normalizes to the [0, 360) range, so this should be 270.
		assertEquals(270.0f, west.getHeading(), TOL);
	}

	@Test
	public void headingOKDelegatesToGeo() {
		VectorWithHeading east = new VectorWithHeading(
				new Location(0.0, 0.0), new Location(0.0, 0.001));
		// east heading = 90°. Vehicle at 80° is within 15° of segment.
		assertTrue(east.headingOK(80f, 15f));
		// Vehicle at 0° is 90° off, outside 15°.
		assertFalse(east.headingOK(0f, 15f));
	}

	@Test
	public void unknownVehicleHeadingIsOK() {
		VectorWithHeading any = new VectorWithHeading(
				new Location(0.0, 0.0), new Location(0.0, 0.001));
		assertTrue(any.headingOK(Float.NaN, 1f));
	}

	@Test
	public void inheritsVectorBehavior() {
		Location a = new Location(0.0, 0.0);
		Location b = new Location(0.0, 0.001);
		VectorWithHeading v = new VectorWithHeading(a, b);
		assertEquals(a, v.getL1());
		assertEquals(b, v.getL2());
		assertTrue(v.length() > 0.0);
	}
}
