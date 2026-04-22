package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ExtentTest {

	@Test
	public void freshExtentStartsWithInvertedInfinityBounds() {
		Extent e = new Extent();
		assertEquals(Double.POSITIVE_INFINITY, e.getMinLat(), 0.0);
		assertEquals(Double.NEGATIVE_INFINITY, e.getMaxLat(), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, e.getMinLon(), 0.0);
		assertEquals(Double.NEGATIVE_INFINITY, e.getMaxLon(), 0.0);
	}

	@Test
	public void addLocationExpandsBounds() {
		Extent e = new Extent();
		e.add(new Location(37.5, -122.25));
		assertEquals(37.5, e.getMinLat(), 0.0);
		assertEquals(37.5, e.getMaxLat(), 0.0);
		assertEquals(-122.25, e.getMinLon(), 0.0);
		assertEquals(-122.25, e.getMaxLon(), 0.0);

		e.add(new Location(37.6, -122.20));
		assertEquals(37.5, e.getMinLat(), 0.0);
		assertEquals(37.6, e.getMaxLat(), 0.0);
		assertEquals(-122.25, e.getMinLon(), 0.0);
		assertEquals(-122.20, e.getMaxLon(), 0.0);
	}

	@Test
	public void addExtentMergesBounds() {
		Extent a = new Extent();
		a.add(new Location(37.5, -122.25));
		a.add(new Location(37.6, -122.20));

		Extent b = new Extent();
		b.add(new Location(37.4, -122.30));
		b.add(new Location(37.55, -122.22));

		a.add(b);
		assertEquals(37.4, a.getMinLat(), 0.0);
		assertEquals(37.6, a.getMaxLat(), 0.0);
		assertEquals(-122.30, a.getMinLon(), 0.0);
		assertEquals(-122.20, a.getMaxLon(), 0.0);
	}

	@Test
	public void equalsAndHashCodeBasedOnAllFourCorners() {
		Extent a = new Extent();
		a.add(new Location(37.5, -122.25));
		a.add(new Location(37.6, -122.20));

		Extent b = new Extent();
		b.add(new Location(37.5, -122.25));
		b.add(new Location(37.6, -122.20));

		Extent c = new Extent();
		c.add(new Location(37.5, -122.25));
		c.add(new Location(37.7, -122.20));

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertFalse(a.equals(null));
		assertFalse(a.equals("not an Extent"));
	}

	@Test
	public void isWithinDistanceTrueForInteriorPoint() {
		Extent e = new Extent();
		e.add(new Location(37.5, -122.25));
		e.add(new Location(37.6, -122.20));
		assertTrue(e.isWithinDistance(new Location(37.55, -122.22), 0.0));
	}

	@Test
	public void isWithinDistanceFalseForFarPoint() {
		Extent e = new Extent();
		e.add(new Location(37.5, -122.25));
		e.add(new Location(37.6, -122.20));
		// A degree of latitude is ~111 km; 10 degrees away is way beyond 100 m.
		assertFalse(e.isWithinDistance(new Location(47.5, -122.22), 100.0));
	}

	@Test
	public void isWithinDistanceAllowsPointJustOutsideBoundary() {
		Extent e = new Extent();
		e.add(new Location(37.5, -122.25));
		e.add(new Location(37.6, -122.20));
		// Point ~50 m north of the box; within 1 km buffer should return true.
		Location justNorth = new Location(37.6005, -122.22);
		assertTrue(e.isWithinDistance(justNorth, 1000.0));
		// With a zero-meter buffer the same point is outside.
		assertFalse(e.isWithinDistance(justNorth, 0.0));
	}
}
