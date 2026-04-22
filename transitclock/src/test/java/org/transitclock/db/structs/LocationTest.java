package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.transitclock.utils.Geo;

public class LocationTest {

	@Test
	public void gettersReturnConstructorArgs() {
		Location l = new Location(37.5, -122.25);
		assertEquals(37.5, l.getLat(), 1e-12);
		assertEquals(-122.25, l.getLon(), 1e-12);
	}

	@Test
	public void equalsAndHashCodeBasedOnLatAndLon() {
		Location a = new Location(37.5, -122.25);
		Location b = new Location(37.5, -122.25);
		Location c = new Location(37.5, -122.26);
		Location d = new Location(37.6, -122.25);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
		assertNotEquals(a, d);
		assertFalse(a.equals(null));
		assertFalse(a.equals("not a Location"));
	}

	@Test
	public void distanceDelegatesToGeoDistance() {
		Location a = new Location(0.0, 0.0);
		Location b = new Location(1.0, 0.0);
		assertEquals(Geo.distance(a, b), a.distance(b), 1e-9);
	}

	@Test
	public void distanceToVectorDelegatesToGeoDistance() {
		Location a = new Location(0.0, 0.0);
		Vector v = new Vector(new Location(-1.0, 0.0), new Location(1.0, 0.0));
		assertEquals(Geo.distance(a, v), a.distance(v), 1e-9);
	}

	@Test
	public void matchDistanceAlongVectorDelegatesToGeo() {
		Location a = new Location(0.0, 0.0);
		Vector v = new Vector(new Location(-1.0, 0.0), new Location(1.0, 0.0));
		assertEquals(Geo.matchDistanceAlongVector(a, v),
				a.matchDistanceAlongVector(v), 1e-9);
	}

	@Test
	public void toStringIsFiveDecimalCommaSeparatedBracketed() {
		Location l = new Location(37.5, -122.25);
		String s = l.toString();
		// Format from Geo.format uses 5 decimal places; don't lock in the
		// locale-specific decimal separator, but confirm the bracket framing
		// and presence of both coordinates.
		assertEquals("[" + Geo.format(37.5) + ", " + Geo.format(-122.25) + "]", s);
	}
}
