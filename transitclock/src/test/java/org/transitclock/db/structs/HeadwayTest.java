package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

/**
 * Headway's 9-arg constructor calls Core.getInstance().getDbConfig() which
 * requires a live Core singleton, so these tests use the no-arg Hibernate
 * constructor and exercise the setter surface instead.
 */
public class HeadwayTest {

	private static Headway build(double headwayMs, String vehicleId, String other,
			String stopId, String tripId, String routeId) {
		Headway h = new Headway();
		h.setHeadway(headwayMs);
		h.setVehicleId(vehicleId);
		h.setOtherVehicleId(other);
		h.setStopId(stopId);
		h.setTripId(tripId);
		h.setRouteId(routeId);
		return h;
	}

	@Test
	public void settersAndGettersRoundTrip() {
		Date now = new Date();
		Date later = new Date(now.getTime() + 60_000);
		Headway h = new Headway();
		h.setConfigRev(42);
		h.setHeadway(120_000);
		h.setAverage(100_000);
		h.setVariance(25.0);
		h.setCoefficientOfVariation(0.5);
		h.setNumVehicles(3);
		h.setCreationTime(now);
		h.setVehicleId("v1");
		h.setOtherVehicleId("v2");
		h.setStopId("s1");
		h.setTripId("t1");
		h.setRouteId("r1");
		h.setFirstDeparture(now);
		h.setSecondDeparture(later);

		assertEquals(42, h.getConfigRev());
		assertEquals(120_000.0, h.getHeadway(), 0.0);
		assertEquals(100_000.0, h.getAverage(), 0.0);
		assertEquals(25.0, h.getVariance(), 0.0);
		assertEquals(0.5, h.getCoefficientOfVariation(), 0.0);
		assertEquals(3, h.getNumVehicles());
		assertEquals(now, h.getCreationTime());
		assertEquals("v1", h.getVehicleId());
		assertEquals("v2", h.getOtherVehicleId());
		assertEquals("s1", h.getStopId());
		assertEquals("t1", h.getTripId());
		assertEquals("r1", h.getRouteId());
		assertEquals(now, h.getFirstDeparture());
		assertEquals(later, h.getSecondDeparture());
	}

	@Test
	public void equalsUsesCoreFieldsButIgnoresTimingAndStats() {
		Headway base = build(120_000, "v1", "v2", "s1", "t1", "r1");
		Headway same = build(120_000, "v1", "v2", "s1", "t1", "r1");
		// Differing timing/stats still leaves equality intact — equals only
		// compares headway + five id fields.
		same.setAverage(999_999);
		same.setVariance(999.0);
		same.setFirstDeparture(new Date(1));
		assertEquals(base, same);
		assertEquals(base.hashCode(), same.hashCode());
	}

	@Test
	public void equalsFlipsOnAnyOfTheCompareFields() {
		Headway base = build(120_000, "v1", "v2", "s1", "t1", "r1");

		assertNotEquals(base, build(121_000, "v1", "v2", "s1", "t1", "r1"));
		assertNotEquals(base, build(120_000, "x", "v2", "s1", "t1", "r1"));
		assertNotEquals(base, build(120_000, "v1", "x", "s1", "t1", "r1"));
		assertNotEquals(base, build(120_000, "v1", "v2", "x", "t1", "r1"));
		assertNotEquals(base, build(120_000, "v1", "v2", "s1", "x", "r1"));
		assertNotEquals(base, build(120_000, "v1", "v2", "s1", "t1", "x"));

		assertFalse(base.equals(null));
		assertFalse(base.equals("not a Headway"));
	}

	@Test
	public void toStringMentionsIdentityFields() {
		Headway h = build(120_000, "v1", "v2", "s1", "t1", "r1");
		String s = h.toString();
		assertTrue(s.contains("Headway ["));
		assertTrue(s.contains("headway=120000"));
		assertTrue(s.contains("vehicleId=v1"));
		assertTrue(s.contains("routeId=r1"));
	}
}
