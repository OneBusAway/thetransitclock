package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.transitclock.testutil.TransitFixtures;

/**
 * StopPath holds the list of Locations that form a path from the previous
 * stop to this stop, plus a pile of flags (layover, wait, schedule
 * adherence, last-in-trip) and an optional break time. Most methods are
 * plain getters, but {@link StopPath#setLocations} does real work: it
 * recomputes {@code pathLength} as the sum of great-circle distances between
 * consecutive locations. These tests pin that behavior and the flag getters.
 *
 * Not tested here: getSegmentVectors() / getSegmentVector(). Those are
 * populated by Hibernate's onLoad() callback and stay null under
 * setLocations alone — calling getSegmentVector() on a setLocations-only
 * StopPath NPEs rather than returning null, so there's no exercisable
 * contract we can pin without calling onLoad(null, null), which happens to
 * work today but is an implementation detail we don't want to lock in.
 */
public class StopPathExtendedTest {

	@Test
	public void simpleStopPathHasFlagsAllFalseAndNoBreak() {
		StopPath path = TransitFixtures.simpleStopPath("p1", "s1", 0);
		assertFalse(path.isLayoverStop());
		assertFalse(path.isWaitStop());
		assertFalse(path.isScheduleAdherenceStop());
		assertFalse(path.isLastStopInTrip());
		// Not a layover → getBreakTimeSec is 0 regardless of stored break.
		assertEquals(0, path.getBreakTimeSec());
	}

	@Test
	public void layoverStopReturnsStoredBreakTime() {
		StopPath path = TransitFixtures.stopPath(
				TransitFixtures.DEFAULT_CONFIG_REV,
				"p1", "s1", 0, "route1",
				/*lastStopInTrip*/ true,
				/*layoverStop*/ true,
				/*waitStop*/ false,
				/*scheduleAdherenceStop*/ true,
				/*breakTime*/ 180);
		assertTrue(path.isLayoverStop());
		assertTrue(path.isLastStopInTrip());
		assertTrue(path.isScheduleAdherenceStop());
		assertEquals(180, path.getBreakTimeSec());
	}

	@Test
	public void gettersReturnConstructorValues() {
		StopPath path = TransitFixtures.stopPath(
				42, "pathX", "stopX", 7, "routeX",
				true, false, true, false, null);
		assertEquals(42, path.getConfigRev());
		assertEquals("pathX", path.getStopPathId());
		assertEquals("stopX", path.getStopId());
		assertEquals(7, path.getGtfsStopSeq());
		assertTrue(path.isLastStopInTrip());
		assertTrue(path.isWaitStop());
	}

	@Test
	public void tripPatternIdStartsNullAndCanBeSet() {
		StopPath path = TransitFixtures.simpleStopPath("p1", "s1", 0);
		assertNull(path.getTripPatternId());
		path.setTripPatternId("pattern-A");
		assertEquals("pattern-A", path.getTripPatternId());
	}

	@Test
	public void setLocationsComputesPathLengthAsSumOfSegmentDistances() {
		// Two collinear points ~111 km apart (one degree of latitude).
		Location a = new Location(0.0, 0.0);
		Location b = new Location(1.0, 0.0);
		Location c = new Location(2.0, 0.0);

		StopPath path = TransitFixtures.withLocations(
				TransitFixtures.simpleStopPath("p1", "s1", 0),
				a, b, c);

		double expected = a.distance(b) + b.distance(c);
		assertEquals(expected, path.getLength(), 1e-6);
	}

	@Test
	public void setLocationsWithSingleLocationGivesZeroLength() {
		// Zero segments between one point → pathLength = 0.
		StopPath path = TransitFixtures.withLocations(
				TransitFixtures.simpleStopPath("p1", "s1", 0),
				new Location(47.6, -122.3));
		assertEquals(0.0, path.getLength(), 0.0);
		// One location → zero segments.
		assertEquals(0, path.getNumberSegments());
	}

	@Test
	public void getNumberSegmentsIsLocationsMinusOne() {
		StopPath path = TransitFixtures.withLocations(
				TransitFixtures.simpleStopPath("p1", "s1", 0),
				new Location(0.0, 0.0),
				new Location(1.0, 0.0),
				new Location(2.0, 0.0),
				new Location(3.0, 0.0));
		assertEquals(3, path.getNumberSegments());
	}

	@Test
	public void locationAccessorsReturnPositionalEntries() {
		Location a = new Location(47.60, -122.33);
		Location b = new Location(47.61, -122.33);
		Location c = new Location(47.62, -122.33);
		StopPath path = TransitFixtures.withLocations(
				TransitFixtures.simpleStopPath("p1", "s1", 0),
				a, b, c);

		assertSame(a, path.getLocation(0));
		assertSame(c, path.getLocation(2));
		// getStopLocation and getEndOfPathLocation both return the last
		// entry (the stop is at the end of the path).
		assertSame(c, path.getStopLocation());
		assertSame(c, path.getEndOfPathLocation());
		assertEquals(Arrays.asList(a, b, c), path.getLocations());
	}

	@Test
	public void setLocationsReplacesPathLengthOnSubsequentCalls() {
		StopPath path = TransitFixtures.simpleStopPath("p1", "s1", 0);
		path.setLocations(new ArrayList<>(Arrays.asList(
				new Location(0.0, 0.0),
				new Location(0.1, 0.0))));
		double shortLen = path.getLength();

		path.setLocations(new ArrayList<>(Arrays.asList(
				new Location(0.0, 0.0),
				new Location(1.0, 0.0))));
		double longLen = path.getLength();

		// Overwriting locations must recompute pathLength from scratch —
		// not add to the previous value.
		assertTrue("longer path should exceed shorter", longLen > shortLen);
		// And approximately ten times — one degree of lat is ten times 0.1.
		assertEquals(longLen, 10 * shortLen, longLen * 0.01);
	}
}
