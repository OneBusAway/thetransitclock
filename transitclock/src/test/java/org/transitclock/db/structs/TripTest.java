package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.transitclock.gtfs.gtfsStructs.GtfsTrip;
import org.transitclock.testutil.TransitFixtures;

/**
 * Trip is the central join between GTFS trips.txt and the runtime block
 * assignment. It's constructed once from a GtfsTrip + TitleFormatter, then
 * addScheduleTimes() progressively fills in startTime/endTime and the
 * per-stop ScheduleTime list. These tests pin that constructor wiring, the
 * startTime/endTime inference, the frequency-based copy constructors
 * (which drive isNoSchedule), and the blockId fallback chain.
 *
 * Tests that depend on Block/Route/TripPattern/TravelTimes live in the
 * integration suite — those are all populated by GtfsData/DbConfig.
 */
public class TripTest {

	@Test
	public void constructorWiresAllSimpleFieldsFromGtfsTrip() {
		GtfsTrip gtfs = TransitFixtures.gtfsTrip(
				"route1", "weekday", "trip1",
				"Downtown", "T1", "0", "block1", "shape1");
		Trip trip = TransitFixtures.trip(42, gtfs);

		assertEquals(42, trip.getConfigRev());
		assertEquals("trip1", trip.getId());
		assertEquals("T1", trip.getShortName());
		assertEquals("route1", trip.getRouteId());
		assertEquals("weekday", trip.getServiceId());
		assertEquals("0", trip.getDirectionId());
		assertEquals("block1", trip.getBlockId());
		assertEquals("shape1", trip.getShapeId());
		// Headsign gets run through TitleFormatter. With an empty formatter
		// it should be preserved as-is.
		assertEquals("Downtown", trip.getHeadsign());
		assertFalse(trip.isNoSchedule());
		assertFalse(trip.isExactTimesHeadway());
	}

	@Test
	public void startTimeAndEndTimeAreNullBeforeScheduleTimesAdded() {
		Trip trip = TransitFixtures.trip(TransitFixtures.simpleGtfsTrip("t1", "r1"));
		assertNull(trip.getStartTime());
		assertNull(trip.getEndTime());
	}

	@Test
	public void addScheduleTimesInfersStartFromEarliestDepartureAndEndFromLatestArrival() {
		// Typical trip pattern: mid-trip stops have departure only, last stop
		// has arrival only.
		Trip trip = TransitFixtures.tripWithStops("t1", "r1", Arrays.asList(
				TransitFixtures.departOnly(28_800),     // 08:00:00
				TransitFixtures.departOnly(29_100),     // 08:05:00
				TransitFixtures.departOnly(29_400),     // 08:10:00
				TransitFixtures.arriveOnly(29_700)));   // 08:15:00

		assertEquals(Integer.valueOf(28_800), trip.getStartTime());
		assertEquals(Integer.valueOf(29_700), trip.getEndTime());
	}

	@Test
	public void addScheduleTimesHandlesOutOfOrderEntries() {
		// addScheduleTimes walks the list comparing to current min/max,
		// so order of the input list shouldn't matter for the inference.
		Trip trip = TransitFixtures.tripWithStops("t1", "r1", Arrays.asList(
				TransitFixtures.departOnly(29_400),
				TransitFixtures.arriveOnly(29_700),
				TransitFixtures.departOnly(28_800),
				TransitFixtures.departOnly(29_100)));
		assertEquals(Integer.valueOf(28_800), trip.getStartTime());
		assertEquals(Integer.valueOf(29_700), trip.getEndTime());
	}

	@Test
	public void arriveDepartStopContributesBothDepartureForStartAndArrivalForEnd() {
		// A stop with both arrival and departure (e.g., a timed-transfer stop)
		// contributes to both bounds independently.
		Trip trip = TransitFixtures.tripWithStops("t1", "r1", Arrays.asList(
				TransitFixtures.arriveDepart(28_000, 28_100),
				TransitFixtures.arriveDepart(29_500, 29_600)));
		// Earliest departure is 28_100, latest arrival is 29_500.
		assertEquals(Integer.valueOf(28_100), trip.getStartTime());
		assertEquals(Integer.valueOf(29_500), trip.getEndTime());
	}

	@Test
	public void getScheduleTimeReturnsEntryAtIndex() {
		ScheduleTime s0 = TransitFixtures.departOnly(28_800);
		ScheduleTime s1 = TransitFixtures.departOnly(29_100);
		ScheduleTime s2 = TransitFixtures.arriveOnly(29_400);
		Trip trip = TransitFixtures.tripWithStops("t1", "r1",
				Arrays.asList(s0, s1, s2));

		assertSame(s0, trip.getScheduleTime(0));
		assertSame(s1, trip.getScheduleTime(1));
		assertSame(s2, trip.getScheduleTime(2));
		assertEquals(3, trip.getScheduleTimes().size());
	}

	@Test
	public void blockIdFallsBackToShortNameWhenGtfsBlockIdNull() {
		// GTFS block_id is optional. Trip uses short name as the fallback so
		// blocks still get assembled, matching agencies that use the short
		// name in the AVL feed.
		GtfsTrip gtfs = TransitFixtures.gtfsTrip(
				"route1", "weekday", "trip1",
				"Downtown", /*tripShortName*/ "SHORTX",
				"0", /*blockId*/ null, "shape1");
		Trip trip = TransitFixtures.trip(gtfs);
		assertEquals("SHORTX", trip.getBlockId());
	}

	@Test
	public void blockIdFallsBackToTripIdWhenBlockIdAndShortNameNull() {
		GtfsTrip gtfs = TransitFixtures.gtfsTrip(
				"route1", "weekday", "trip1",
				"Downtown", /*tripShortName*/ null,
				"0", /*blockId*/ null, "shape1");
		Trip trip = TransitFixtures.trip(gtfs);
		assertEquals("trip1", trip.getBlockId());
	}

	@Test
	public void frequencyBasedNoScheduleCopyConstructorFlipsNoSchedule() {
		// Trip(Trip, int, int) is used for frequencies.txt rows where
		// exact_times=false. The resulting Trip must report noSchedule=true.
		Trip base = TransitFixtures.tripWithStops("t1", "r1", Arrays.asList(
				TransitFixtures.departOnly(28_800),
				TransitFixtures.arriveOnly(29_700)));
		Trip frequencyTrip = new Trip(base, /*start*/ 36_000, /*end*/ 39_600);

		assertTrue(frequencyTrip.isNoSchedule());
		assertFalse(frequencyTrip.isExactTimesHeadway());
		assertEquals(Integer.valueOf(36_000), frequencyTrip.getStartTime());
		assertEquals(Integer.valueOf(39_600), frequencyTrip.getEndTime());
		assertEquals("t1", frequencyTrip.getId());
		// Schedule times are copied in from the base trip.
		assertEquals(2, frequencyTrip.getScheduleTimes().size());
	}

	@Test
	public void exactTimesHeadwayCopyConstructorOffsetsTimes() {
		// Trip(Trip, int) is used for frequencies.txt rows where
		// exact_times=true — each copy of the trip runs at a fixed offset
		// relative to the base schedule.
		Trip base = TransitFixtures.tripWithStops("t1", "r1", Arrays.asList(
				TransitFixtures.departOnly(0),
				TransitFixtures.arriveOnly(300)));
		int offset = 36_000; // shift by 10 hours
		Trip offsetTrip = new Trip(base, offset);

		assertFalse(offsetTrip.isNoSchedule());
		assertTrue("frequency copy with exact_times=true must flag exactTimesHeadway",
				offsetTrip.isExactTimesHeadway());
		// Start/end are shifted by the offset.
		assertEquals(Integer.valueOf(0 + offset), offsetTrip.getStartTime());
		assertEquals(Integer.valueOf(300 + offset), offsetTrip.getEndTime());
		// Every schedule time is shifted by the same offset. Without this
		// assertion, a regression that leaves schedule times at the base
		// (unshifted) values would pass the start/end checks above.
		assertEquals(2, offsetTrip.getScheduleTimes().size());
		assertEquals("departure time shifts with offset",
				Integer.valueOf(0 + offset),
				offsetTrip.getScheduleTimes().get(0).getDepartureTime());
		assertEquals("arrival time shifts with offset",
				Integer.valueOf(300 + offset),
				offsetTrip.getScheduleTimes().get(1).getArrivalTime());
		// Block id is mutated to be unique per-copy.
		assertFalse("block id must be disambiguated",
				offsetTrip.getBlockId().equals(base.getBlockId()));
		assertTrue("block id keeps the base as a prefix",
				offsetTrip.getBlockId().startsWith(base.getBlockId()));
	}

	@Test
	public void toShortStringIncludesTripIdAndRouteId() {
		Trip trip = TransitFixtures.tripWithStops("trip42", "route7",
				Arrays.asList(
						TransitFixtures.departOnly(28_800),
						TransitFixtures.arriveOnly(29_700)));
		String s = trip.toShortString();
		assertNotNull(s);
		assertTrue(s.contains("trip42"));
		assertTrue(s.contains("route7"));
	}
}
