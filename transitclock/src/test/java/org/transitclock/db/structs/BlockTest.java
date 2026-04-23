package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.transitclock.testutil.TransitFixtures;

/**
 * Block groups a sequence of trips run by one vehicle in a day. Most of its
 * methods are thin delegations (getId, numTrips, getTrip(i)), but the
 * constructor's derivation of routeIds and the getTrip(String) / getTripIndex
 * lookups have enough logic that regressions are easy to miss.
 *
 * The time-of-day isActive variants and numStopPaths(trip) depend on
 * Core.getInstance() / TravelTimesForTrip respectively, so those aren't
 * tested here — they belong in the integration suite.
 */
public class BlockTest {

	private static Trip trip(String tripId, String routeId) {
		return TransitFixtures.tripWithStops(tripId, routeId, Arrays.asList(
				TransitFixtures.departOnly(28_800),
				TransitFixtures.arriveOnly(29_700)));
	}

	@Test
	public void constructorStoresBlockIdAndServiceIdAndConfigRev() {
		Block block = TransitFixtures.block(
				17, "blockA", "weekday",
				28_800, 32_400,
				Arrays.asList(trip("t1", "r1")));
		assertEquals("blockA", block.getId());
		assertEquals("weekday", block.getServiceId());
		assertEquals(17, block.getConfigRev());
		assertEquals(28_800, block.getStartTime());
		assertEquals(32_400, block.getEndTime());
	}

	@Test
	public void numTripsIsListSize() {
		Block block = TransitFixtures.blockOf("b1", "weekday",
				trip("t1", "r1"),
				trip("t2", "r1"),
				trip("t3", "r1"));
		assertEquals(3, block.numTrips());
	}

	@Test
	public void getTripByIndexReturnsNullForOutOfRange() {
		Block block = TransitFixtures.blockOf("b1", "weekday",
				trip("t1", "r1"));
		assertNotNull(block.getTrip(0));
		assertNull(block.getTrip(1));
		assertNull(block.getTrip(-1));
		assertNull(block.getTrip(99));
	}

	@Test
	public void getTripByIdFindsMatchingTrip() {
		Trip t1 = trip("t1", "r1");
		Trip t2 = trip("t2", "r1");
		Block block = TransitFixtures.blockOf("b1", "weekday", t1, t2);

		assertSame(t1, block.getTrip("t1"));
		assertSame(t2, block.getTrip("t2"));
		assertNull(block.getTrip("no-such-trip"));
	}

	@Test
	public void getTripIndexFindsPositionByIdentity() {
		Trip t1 = trip("t1", "r1");
		Trip t2 = trip("t2", "r1");
		Trip t3 = trip("t3", "r1");
		Block block = TransitFixtures.blockOf("b1", "weekday", t1, t2, t3);

		assertEquals(0, block.getTripIndex(t1));
		assertEquals(1, block.getTripIndex(t2));
		assertEquals(2, block.getTripIndex(t3));
	}

	@Test
	public void getTripIndexReturnsNegativeOneForUnknownTrip() {
		Trip known = trip("t1", "r1");
		Trip foreign = trip("other", "r1");
		Block block = TransitFixtures.blockOf("b1", "weekday", known);
		assertEquals(-1, block.getTripIndex(foreign));
	}

	@Test
	public void routeIdsDedupeAcrossTrips() {
		// Constructor collects route ids from trips into a Set — duplicate
		// routeIds across trips should show up only once. This matters for
		// hasRoute()-style lookups (via the set) that operate on the block.
		Block block = TransitFixtures.blockOf("b1", "weekday",
				trip("t1", "r1"),
				trip("t2", "r2"),
				trip("t3", "r1"),   // duplicate
				trip("t4", "r2"));  // duplicate

		Set<String> routeIds = block.getRouteIds();
		assertEquals(2, routeIds.size());
		assertTrue(routeIds.contains("r1"));
		assertTrue(routeIds.contains("r2"));
	}

	@Test
	public void routeIdsFromSingleTripContainsOnlyThatRoute() {
		Block block = TransitFixtures.blockOf("b1", "weekday",
				trip("t1", "routeOnly"));
		assertEquals(Collections.singleton("routeOnly"), block.getRouteIds());
	}

	@Test
	public void isNoScheduleDelegatesToFirstTrip() {
		// Trip built from GtfsTrip has noSchedule=false. After wrapping in
		// the frequency-based copy constructor it's true. Test both.
		Trip scheduled = trip("t1", "r1");
		Block scheduledBlock = TransitFixtures.blockOf("b1", "weekday", scheduled);
		assertFalse(scheduledBlock.isNoSchedule());
		assertTrue(scheduledBlock.hasSchedule());

		Trip frequency = new Trip(scheduled, 36_000, 39_600);
		Block frequencyBlock = TransitFixtures.blockOf("b2", "weekday", frequency);
		assertTrue(frequencyBlock.isNoSchedule());
		assertFalse(frequencyBlock.hasSchedule());
	}

	@Test
	public void getTripsReturnsAllTripsInInsertionOrder() {
		Trip t1 = trip("t1", "r1");
		Trip t2 = trip("t2", "r1");
		Block block = TransitFixtures.blockOf("b1", "weekday", t1, t2);
		// getTrips() wraps with unmodifiableList but preserves order.
		assertEquals(Arrays.asList(t1, t2), block.getTrips());
	}
}
