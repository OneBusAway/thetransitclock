package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

/**
 * Arrival and Departure are tiny wrappers over ArrivalDeparture that exist
 * solely to pin the isArrival flag and (for Arrival) to expose a
 * withUpdatedTime copy. Their value is precisely that distinction, so these
 * tests verify the isArrival/isDeparture discrimination and getter delegation.
 *
 * The configRev-taking constructors are used throughout so the tests don't
 * depend on a running Core (the other constructor form fetches configRev via
 * Core.getInstance()). Passing a null Block keeps the tests free of the
 * Hibernate/Trip/StopPath graph — the base constructor short-circuits most
 * derived-field computation when block is null.
 */
public class ArrivalDepartureSubclassesTest {

	private static final int CONFIG_REV = 7;
	private static final String VEHICLE_ID = "v1";

	private static Date time(long millis) {
		return new Date(millis);
	}

	@Test
	public void arrivalHasIsArrivalTrue() {
		Arrival a = new Arrival(CONFIG_REV, VEHICLE_ID, time(1_000_000),
				time(999_000), null, 0, 1, null);
		assertTrue(a.isArrival());
		assertFalse(a.isDeparture());
	}

	@Test
	public void departureHasIsArrivalFalse() {
		Departure d = new Departure(CONFIG_REV, VEHICLE_ID, time(1_000_000),
				time(999_000), null, 0, 1, null);
		assertFalse(d.isArrival());
		assertTrue(d.isDeparture());
	}

	@Test
	public void arrivalExposesConstructorFields() {
		Date t = time(2_000_000);
		Date avl = time(1_998_000);
		Arrival a = new Arrival(CONFIG_REV, VEHICLE_ID, t, avl, null, 3, 5, null);
		assertEquals(VEHICLE_ID, a.getVehicleId());
		assertEquals(CONFIG_REV, a.getConfigRev());
		assertEquals(t, a.getDate());
		assertEquals(t.getTime(), a.getTime());
		assertSame(avl, a.getAvlTime());
		assertEquals(3, a.getTripIndex());
		assertEquals(5, a.getStopPathIndex());
		assertNull(a.getFreqStartTime());
	}

	@Test
	public void departureExposesConstructorFields() {
		Date t = time(2_000_000);
		Date avl = time(1_998_000);
		Departure d = new Departure(CONFIG_REV, VEHICLE_ID, t, avl, null, 4, 2, null);
		assertEquals(VEHICLE_ID, d.getVehicleId());
		assertEquals(CONFIG_REV, d.getConfigRev());
		assertEquals(t, d.getDate());
		assertEquals(t.getTime(), d.getTime());
		assertSame(avl, d.getAvlTime());
		assertEquals(4, d.getTripIndex());
		assertEquals(2, d.getStopPathIndex());
	}

	@Test
	public void freqStartTimeRoundTrips() {
		Date freqStart = time(500_000);
		Arrival a = new Arrival(CONFIG_REV, VEHICLE_ID, time(1_000_000),
				time(999_000), null, 0, 1, freqStart);
		assertSame(freqStart, a.getFreqStartTime());

		Departure d = new Departure(CONFIG_REV, VEHICLE_ID, time(1_000_000),
				time(999_000), null, 0, 1, freqStart);
		assertSame(freqStart, d.getFreqStartTime());
	}

	@Test
	public void nullBlockResultsInNullBlockAndDefaultFields() {
		// When block is null the base constructor takes the short-circuit
		// branch: no derived fields (stopId/routeId/etc.) are populated.
		// This test pins that behavior so we notice if the null-block branch
		// ever starts resolving those fields differently.
		Arrival a = new Arrival(CONFIG_REV, VEHICLE_ID, time(1_000_000),
				time(999_000), null, 0, 1, null);
		assertNull(a.getBlock());
		assertEquals("", a.getStopId());
		assertEquals("", a.getTripId());
		assertEquals("", a.getServiceId());
		assertNull(a.getScheduledDate());
		assertEquals(0f, a.getStopPathLength(), 0.0f);
		assertEquals(Integer.valueOf(0), a.getStopOrder());
	}

	@Test
	public void arrivalAndDepartureAreNotEqualEvenWithSameFields() {
		// ArrivalDeparture.equals() includes the isArrival flag, so an
		// Arrival and a Departure built from the same fields must differ.
		Date t = time(1_000_000);
		Date avl = time(999_000);
		Arrival a = new Arrival(CONFIG_REV, VEHICLE_ID, t, avl, null, 0, 1, null);
		Departure d = new Departure(CONFIG_REV, VEHICLE_ID, t, avl, null, 0, 1, null);
		assertFalse(a.equals(d));
		assertFalse(d.equals(a));
	}

	@Test
	public void twoArrivalsWithSameFieldsAreEqual() {
		Date t = time(1_000_000);
		Date avl = time(999_000);
		Arrival a = new Arrival(CONFIG_REV, VEHICLE_ID, t, avl, null, 0, 1, null);
		Arrival b = new Arrival(CONFIG_REV, VEHICLE_ID, t, avl, null, 0, 1, null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void toStringDistinguishesArrivalAndDeparture() {
		Arrival a = new Arrival(CONFIG_REV, VEHICLE_ID, time(1_000_000),
				time(999_000), null, 0, 1, null);
		Departure d = new Departure(CONFIG_REV, VEHICLE_ID, time(1_000_000),
				time(999_000), null, 0, 1, null);
		// Base class toString starts with "Arrival  " or "Departure".
		assertTrue(a.toString().startsWith("Arrival"));
		assertTrue(d.toString().startsWith("Departure"));
	}
}
