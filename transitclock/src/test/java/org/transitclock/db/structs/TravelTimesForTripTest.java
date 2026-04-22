package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.transitclock.db.structs.TravelTimesForStopPath.HowSet;

/**
 * TravelTimesForTrip's public constructor needs a live Trip (with TripPattern).
 * The Hibernate no-arg constructor is private, so we reach it via reflection.
 * tripPatternId is referenced unguarded by equals()/hashCode(), so we set it
 * reflectively before exercising those paths. The behavioral methods under
 * test (add(), isValid(), purelyScheduleBased()) don't read tripPatternId.
 */
public class TravelTimesForTripTest {

	private static TravelTimesForTrip instantiate() throws Exception {
		Constructor<TravelTimesForTrip> ctor =
				TravelTimesForTrip.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		return ctor.newInstance();
	}

	private static TravelTimesForTrip instantiateWithTripPattern(
			String tripPatternId) throws Exception {
		TravelTimesForTrip t = instantiate();
		Field f = TravelTimesForTrip.class.getDeclaredField("tripPatternId");
		f.setAccessible(true);
		f.set(t, tripPatternId);
		return t;
	}

	private static TravelTimesForStopPath stopPath(HowSet howSet,
			Integer... travelTimesMsec) {
		return new TravelTimesForStopPath(1, 1, "sp", 50.0,
				new ArrayList<>(Arrays.asList(travelTimesMsec)),
				1000, 0, howSet, null);
	}

	private static TravelTimesForStopPath invalidStopPath() {
		// Negative travel time triggers TravelTimesForStopPath.isValid() = false.
		return stopPath(HowSet.AVL, 1000, -1, 2000);
	}

	@Test
	public void noArgConstructorUsesSentinelValues() throws Exception {
		TravelTimesForTrip t = instantiate();
		assertEquals(-1, t.getConfigRev());
		assertEquals(-1, t.getTravelTimeRev());
		assertNull(t.getTripPatternId());
		assertNull(t.getTripCreatedForId());
		assertEquals(0, t.numberOfStopPaths());
		assertTrue(t.getTravelTimesForStopPaths().isEmpty());
	}

	@Test
	public void addAppendsToInternalList() throws Exception {
		TravelTimesForTrip t = instantiate();
		TravelTimesForStopPath sp1 = stopPath(HowSet.AVL, 1000);
		TravelTimesForStopPath sp2 = stopPath(HowSet.AVL, 2000);
		t.add(sp1);
		t.add(sp2);

		assertEquals(2, t.numberOfStopPaths());
		assertSame(sp1, t.getTravelTimesForStopPath(0));
		assertSame(sp2, t.getTravelTimesForStopPath(1));
		assertEquals(Arrays.asList(sp1, sp2), t.getTravelTimesForStopPaths());
	}

	@Test
	public void isValidReturnsTrueForEmptyList() throws Exception {
		// Vacuously true when there are no stop paths to validate.
		TravelTimesForTrip t = instantiate();
		assertTrue(t.isValid());
	}

	@Test
	public void isValidReturnsTrueWhenAllChildrenValid() throws Exception {
		TravelTimesForTrip t = instantiate();
		t.add(stopPath(HowSet.AVL, 1000, 2000));
		t.add(stopPath(HowSet.AVL, 500));
		assertTrue(t.isValid());
	}

	@Test
	public void isValidReturnsFalseIfAnyChildInvalid() throws Exception {
		TravelTimesForTrip t = instantiate();
		t.add(stopPath(HowSet.AVL, 1000));
		t.add(invalidStopPath());
		assertFalse(t.isValid());
	}

	@Test
	public void purelyScheduleBasedReturnsTrueForEmptyList() throws Exception {
		// Vacuously true when the list has no entries.
		TravelTimesForTrip t = instantiate();
		assertTrue(t.purelyScheduleBased());
	}

	@Test
	public void purelyScheduleBasedIsTrueWhenAllChildrenAreSpeedOrSched()
			throws Exception {
		TravelTimesForTrip t = instantiate();
		t.add(stopPath(HowSet.SPEED, 1000));
		t.add(stopPath(HowSet.SCHED, 2000));
		assertTrue(t.purelyScheduleBased());
	}

	@Test
	public void purelyScheduleBasedIsFalseWhenAnyChildIsAvl() throws Exception {
		TravelTimesForTrip t = instantiate();
		t.add(stopPath(HowSet.SCHED, 1000));
		t.add(stopPath(HowSet.AVL, 2000));
		assertFalse(t.purelyScheduleBased());
	}

	@Test
	public void purelyScheduleBasedIsFalseForServcAndTrip() throws Exception {
		// Per HowSet.isScheduleBased(): only SPEED + SCHED count as schedule-based.
		TravelTimesForTrip servc = instantiate();
		servc.add(stopPath(HowSet.SERVC, 1000));
		assertFalse(servc.purelyScheduleBased());

		TravelTimesForTrip trip = instantiate();
		trip.add(stopPath(HowSet.TRIP, 1000));
		assertFalse(trip.purelyScheduleBased());
	}

	@Test
	public void equalsMatchesOnConfigAndPatternId() throws Exception {
		TravelTimesForTrip a = instantiateWithTripPattern("tp1");
		TravelTimesForTrip b = instantiateWithTripPattern("tp1");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsDivergesOnTripPatternId() throws Exception {
		TravelTimesForTrip a = instantiateWithTripPattern("tp1");
		TravelTimesForTrip b = instantiateWithTripPattern("tp2");
		assertNotEquals(a, b);
	}

	@Test
	public void equalsIgnoresTripCreatedForId() throws Exception {
		// Documented VERY IMPORTANT: tripCreatedForId is deliberately excluded
		// from equals/hashCode so cached entries can be reused across trips.
		TravelTimesForTrip a = instantiateWithTripPattern("tp1");
		TravelTimesForTrip b = instantiateWithTripPattern("tp1");
		Field f = TravelTimesForTrip.class.getDeclaredField("tripCreatedForId");
		f.setAccessible(true);
		f.set(a, "trip-1");
		f.set(b, "trip-2");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		TravelTimesForTrip t = instantiateWithTripPattern("tp1");
		assertFalse(t.equals(null));
		assertFalse(t.equals("not a TravelTimesForTrip"));
	}

	@Test
	public void toStringMentionsConfigRevAndPatternId() throws Exception {
		TravelTimesForTrip t = instantiateWithTripPattern("tp1");
		String s = t.toString();
		assertTrue(s.startsWith("TravelTimesForTrip ["));
		assertTrue(s.contains("configRev=-1"));
		assertTrue(s.contains("tripPatternId=tp1"));
	}

	@Test
	public void toStringWithNewlinesIsMultilineForStopPaths() throws Exception {
		TravelTimesForTrip t = instantiateWithTripPattern("tp1");
		t.add(stopPath(HowSet.AVL, 1000));
		String s = t.toStringWithNewlines();
		assertTrue(s.contains("tripPatternId=tp1"));
		// Expect at least one newline-prefixed "TTForStopPath [" entry.
		assertTrue(s.contains("\n     TTForStopPath ["));
	}
}
