package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * TripPatternKey is used as a map/set key to detect whether a trip pattern
 * already exists while building up GTFS data. Its equals/hashCode intentionally
 * use {@link StopPath#basicEquals} / {@link StopPath#basicHashCode} because
 * the StopPath objects are not yet finalized at that point. These tests verify
 * that weaker comparison as well as the shapeId equality semantics.
 */
public class TripPatternKeyTest {

	/**
	 * Builds a StopPath with just the fields that basicEquals/basicHashCode
	 * look at: configRev, stopPathId, routeId, stopId, gtfsStopSeq.
	 */
	private static StopPath stopPath(int configRev, String pathId, String stopId,
			int gtfsStopSeq, String routeId) {
		return new StopPath(configRev, pathId, stopId, gtfsStopSeq,
				false, routeId, false, false, false,
				null, null, null, null);
	}

	@Test
	public void constructorRejectsNullStopPathsList() {
		try {
			new TripPatternKey("shape1", null);
			fail("Expected RuntimeException for null stopPaths");
		} catch (RuntimeException expected) {
			// expected
		}
	}

	@Test
	public void equalsAndHashCodeForIdenticalKeys() {
		List<StopPath> paths = Arrays.asList(
				stopPath(1, "p1", "s1", 0, "r1"),
				stopPath(1, "p2", "s2", 1, "r1"));
		TripPatternKey a = new TripPatternKey("shapeA", paths);
		TripPatternKey b = new TripPatternKey("shapeA", paths);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsIgnoresNonBasicStopPathFields() {
		// layoverStop is not part of basicEquals, so flipping it must not
		// change equality. This is the whole point of using basicEquals.
		StopPath p1 = stopPath(1, "p1", "s1", 0, "r1");
		StopPath p1Variant = new StopPath(1, "p1", "s1", 0,
				/*lastStopInTrip*/ true, "r1",
				/*layoverStop*/ true, /*waitStop*/ true,
				/*scheduleAdherenceStop*/ true,
				/*breakTime*/ 30, null, null, null);
		TripPatternKey a = new TripPatternKey("shapeA",
				Collections.singletonList(p1));
		TripPatternKey b = new TripPatternKey("shapeA",
				Collections.singletonList(p1Variant));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void differentShapeIdMakesKeysUnequal() {
		List<StopPath> paths = Collections.singletonList(
				stopPath(1, "p1", "s1", 0, "r1"));
		TripPatternKey a = new TripPatternKey("shapeA", paths);
		TripPatternKey b = new TripPatternKey("shapeB", paths);
		assertNotEquals(a, b);
	}

	@Test
	public void differentStopPathOrderMakesKeysUnequal() {
		StopPath p1 = stopPath(1, "p1", "s1", 0, "r1");
		StopPath p2 = stopPath(1, "p2", "s2", 1, "r1");
		TripPatternKey a = new TripPatternKey("shapeA", Arrays.asList(p1, p2));
		TripPatternKey b = new TripPatternKey("shapeA", Arrays.asList(p2, p1));
		assertNotEquals(a, b);
	}

	@Test
	public void differentPathCountMakesKeysUnequal() {
		StopPath p1 = stopPath(1, "p1", "s1", 0, "r1");
		StopPath p2 = stopPath(1, "p2", "s2", 1, "r1");
		TripPatternKey shorter = new TripPatternKey("shapeA",
				Collections.singletonList(p1));
		TripPatternKey longer = new TripPatternKey("shapeA",
				Arrays.asList(p1, p2));
		assertNotEquals(shorter, longer);
	}

	@Test
	public void differentStopIdBreaksEquality() {
		StopPath p1 = stopPath(1, "p1", "s1", 0, "r1");
		StopPath p1DiffStop = stopPath(1, "p1", "sX", 0, "r1");
		TripPatternKey a = new TripPatternKey("shapeA",
				Collections.singletonList(p1));
		TripPatternKey b = new TripPatternKey("shapeA",
				Collections.singletonList(p1DiffStop));
		assertNotEquals(a, b);
	}

	@Test
	public void differentConfigRevBreaksEquality() {
		TripPatternKey a = new TripPatternKey("shapeA",
				Collections.singletonList(stopPath(1, "p1", "s1", 0, "r1")));
		TripPatternKey b = new TripPatternKey("shapeA",
				Collections.singletonList(stopPath(2, "p1", "s1", 0, "r1")));
		assertNotEquals(a, b);
	}

	@Test
	public void equalsHandlesNullAndWrongType() {
		TripPatternKey key = new TripPatternKey("shapeA",
				new ArrayList<StopPath>());
		assertFalse(key.equals(null));
		assertFalse(key.equals("not a TripPatternKey"));
		assertTrue(key.equals(key));
	}

	@Test
	public void worksAsHashMapKey() {
		List<StopPath> paths = Arrays.asList(
				stopPath(1, "p1", "s1", 0, "r1"),
				stopPath(1, "p2", "s2", 1, "r1"));
		Map<TripPatternKey, String> map = new HashMap<>();
		map.put(new TripPatternKey("shapeA", paths), "patternA");
		// Same logical key — must overwrite, not add.
		map.put(new TripPatternKey("shapeA", paths), "patternA-dup");
		assertEquals(1, map.size());
		assertEquals("patternA-dup",
				map.get(new TripPatternKey("shapeA", paths)));
	}

	@Test
	public void gettersReturnConstructorValues() {
		List<StopPath> paths = Collections.singletonList(
				stopPath(1, "p1", "s1", 0, "r1"));
		TripPatternKey key = new TripPatternKey("shapeA", paths);
		assertEquals("shapeA", key.getShapeId());
		assertSame(paths, key.getStopPaths());
	}

	@Test
	public void hashCodeSurvivesNullShapeId() {
		TripPatternKey key = new TripPatternKey(null,
				Collections.singletonList(stopPath(1, "p1", "s1", 0, "r1")));
		// Just asserting no NPE — and it should round-trip equals with a
		// similarly null-shape key.
		assertNotNull(Integer.valueOf(key.hashCode()));
		TripPatternKey same = new TripPatternKey(null,
				Collections.singletonList(stopPath(1, "p1", "s1", 0, "r1")));
		assertEquals(key, same);
		assertEquals(key.hashCode(), same.hashCode());
	}
}
