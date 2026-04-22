package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.transitclock.db.structs.TravelTimesForStopPath.HowSet;

/**
 * TravelTimesForStopPath's value constructor has no Core dependency — the
 * trip arg is for logging only and may be null. The travelTimesMsec list
 * is copied into an ArrayList<Integer> via a cast, so callers must pass a
 * mutable ArrayList or something assignable to it.
 */
public class TravelTimesForStopPathTest {

	private static List<Integer> mutable(Integer... vals) {
		// Value ctor casts the List to ArrayList, so Arrays.asList/emptyList
		// won't work — use a real ArrayList.
		return new java.util.ArrayList<>(Arrays.asList(vals));
	}

	private static TravelTimesForStopPath build() {
		return new TravelTimesForStopPath(1, 2, "sp1", 100.0,
				mutable(1000, 2000, 3000), 5000, 0, HowSet.AVL, null);
	}

	@Test
	public void gettersRoundTripFromConstructor() {
		TravelTimesForStopPath tt = build();
		assertEquals(1, tt.getConfigRev());
		assertEquals(2, tt.getTravelTimesRev());
		assertEquals("sp1", tt.getStopPathId());
		assertEquals(100.0, tt.getTravelTimeSegmentLength(), 1e-6);
		assertEquals(Arrays.asList(1000, 2000, 3000), tt.getTravelTimesMsec());
		assertEquals(5000, tt.getStopTimeMsec());
		assertEquals(0, tt.getDaysOfWeekOverride());
		assertEquals(HowSet.AVL, tt.getHowSet());
	}

	@Test
	public void getStopPathTravelTimeMsecSumsList() {
		TravelTimesForStopPath tt = build();
		assertEquals(6000, tt.getStopPathTravelTimeMsec());
	}

	@Test
	public void getStopPathTravelTimeMsecHandlesEmptyList() {
		TravelTimesForStopPath tt = new TravelTimesForStopPath(1, 1, "sp", 50.0,
				mutable(), 1000, 0, HowSet.SCHED, null);
		assertEquals(0, tt.getStopPathTravelTimeMsec());
		assertEquals(0, tt.getNumberTravelTimeSegments());
	}

	@Test
	public void getTravelTimeSegmentMsecReturnsIndexedValue() {
		TravelTimesForStopPath tt = build();
		assertEquals(1000, tt.getTravelTimeSegmentMsec(0));
		assertEquals(2000, tt.getTravelTimeSegmentMsec(1));
		assertEquals(3000, tt.getTravelTimeSegmentMsec(2));
	}

	@Test
	public void isValidReturnsTrueForNonNegativeTimes() {
		TravelTimesForStopPath tt = build();
		assertTrue(tt.isValid());
	}

	@Test
	public void isValidReturnsFalseForNegativeTravelTime() {
		TravelTimesForStopPath tt = new TravelTimesForStopPath(1, 1, "sp", 50.0,
				mutable(1000, -1, 1000), 1000, 0, HowSet.AVL, null);
		assertFalse(tt.isValid());
	}

	@Test
	public void isValidReturnsFalseForNegativeStopTime() {
		TravelTimesForStopPath tt = new TravelTimesForStopPath(1, 1, "sp", 50.0,
				mutable(1000), -1, 0, HowSet.AVL, null);
		assertFalse(tt.isValid());
	}

	@Test
	public void cloneKeepsFieldsButUpdatesTravelTimesRev() {
		TravelTimesForStopPath original = build();
		TravelTimesForStopPath copy = original.clone(99);
		assertNotSame(original, copy);
		assertEquals(99, copy.getTravelTimesRev());
		assertEquals(original.getConfigRev(), copy.getConfigRev());
		assertEquals(original.getStopPathId(), copy.getStopPathId());
		assertEquals(original.getTravelTimeSegmentLength(),
				copy.getTravelTimeSegmentLength(), 1e-6);
		assertEquals(original.getTravelTimesMsec(), copy.getTravelTimesMsec());
		assertEquals(original.getStopTimeMsec(), copy.getStopTimeMsec());
		assertEquals(original.getHowSet(), copy.getHowSet());
	}

	@Test
	public void howSetIsScheduleBasedForSpeedAndSched() {
		// The comment declares SPEED+SCHED as schedule-based; SERVC/TRIP/AVL are not.
		assertTrue(HowSet.SPEED.isScheduleBased());
		assertTrue(HowSet.SCHED.isScheduleBased());
		assertFalse(HowSet.SERVC.isScheduleBased());
		assertFalse(HowSet.TRIP.isScheduleBased());
		assertFalse(HowSet.AVL.isScheduleBased());
	}

	@Test
	public void equalsMatchesOnAllFields() {
		TravelTimesForStopPath a = build();
		TravelTimesForStopPath b = build();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachField() {
		TravelTimesForStopPath base = build();

		assertNotEquals(base, new TravelTimesForStopPath(2, 2, "sp1", 100.0,
				mutable(1000, 2000, 3000), 5000, 0, HowSet.AVL, null));
		assertNotEquals(base, new TravelTimesForStopPath(1, 3, "sp1", 100.0,
				mutable(1000, 2000, 3000), 5000, 0, HowSet.AVL, null));
		assertNotEquals(base, new TravelTimesForStopPath(1, 2, "sp2", 100.0,
				mutable(1000, 2000, 3000), 5000, 0, HowSet.AVL, null));
		assertNotEquals(base, new TravelTimesForStopPath(1, 2, "sp1", 200.0,
				mutable(1000, 2000, 3000), 5000, 0, HowSet.AVL, null));
		assertNotEquals(base, new TravelTimesForStopPath(1, 2, "sp1", 100.0,
				mutable(1000, 2000), 5000, 0, HowSet.AVL, null));
		assertNotEquals(base, new TravelTimesForStopPath(1, 2, "sp1", 100.0,
				mutable(1000, 2000, 3000), 6000, 0, HowSet.AVL, null));
		assertNotEquals(base, new TravelTimesForStopPath(1, 2, "sp1", 100.0,
				mutable(1000, 2000, 3000), 5000, 1, HowSet.AVL, null));
		assertNotEquals(base, new TravelTimesForStopPath(1, 2, "sp1", 100.0,
				mutable(1000, 2000, 3000), 5000, 0, HowSet.SCHED, null));
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() {
		TravelTimesForStopPath tt = build();
		assertFalse(tt.equals(null));
		assertFalse(tt.equals("not a TravelTimesForStopPath"));
	}

	@Test
	public void toStringMentionsAllFields() {
		String s = build().toString();
		assertTrue(s.startsWith("TravelTimesForStopPath ["));
		assertTrue(s.contains("configRev=1"));
		assertTrue(s.contains("travelTimesRev=2"));
		assertTrue(s.contains("stopPathId=sp1"));
		assertTrue(s.contains("travelTimesMsec=[1000, 2000, 3000]"));
		assertTrue(s.contains("stopTimeMsec=5000"));
		assertTrue(s.contains("howSet=AVL"));
	}

	@Test
	public void toStringEmphasizeTravelTimesLeadsWithStopTime() {
		String s = build().toStringEmphasizeTravelTimes();
		assertTrue(s.startsWith("TTForStopPath ["));
		assertTrue(s.contains("stopTimeMsec=5000"));
		assertTrue(s.contains("travelTimesMsec=[1000, 2000, 3000]"));
	}

	@Test
	public void isValidTreatsNullTravelListAsOkaySoLongAsStopTimeNonNegative() {
		// Guard clause is "if (travelTimesMsec != null)"; with the private no-arg
		// ctor path the list is null. The public ctor we can reach always
		// supplies a list, but an empty list still satisfies the same branch.
		TravelTimesForStopPath empty = new TravelTimesForStopPath(1, 1, "sp",
				50.0, mutable(), 0, 0, HowSet.SCHED, null);
		assertTrue(empty.isValid());

		// Sanity: singletonList shortcut still works (must be wrapped in a real
		// ArrayList to satisfy the internal cast).
		TravelTimesForStopPath single = new TravelTimesForStopPath(1, 1, "sp",
				50.0, new java.util.ArrayList<>(Collections.singletonList(0)),
				0, 0, HowSet.SCHED, null);
		assertTrue(single.isValid());
	}
}
