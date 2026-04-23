package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * MapKey is the base class for composite cache keys across the codebase, so
 * its equals/hashCode must be airtight. These tests exercise the 2-, 3-, and
 * 4-arg forms, null handling, subclass discrimination, and hashCode stability.
 */
public class MapKeyTest {

	@Test
	public void equalsAndHashCodeMatchForSameTwoArgInputs() {
		MapKey a = new MapKey("x", "y");
		MapKey b = new MapKey("x", "y");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsAndHashCodeMatchForSameThreeArgInputs() {
		MapKey a = MapKey.create("x", "y", "z");
		MapKey b = MapKey.create("x", "y", "z");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsAndHashCodeMatchForSameFourArgInputs() {
		MapKey a = MapKey.create("x", "y", "z", "w");
		MapKey b = MapKey.create("x", "y", "z", "w");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void notEqualWhenAnyPositionDiffers() {
		MapKey base = MapKey.create("a", "b", "c", "d");
		assertNotEquals(base, MapKey.create("A", "b", "c", "d"));
		assertNotEquals(base, MapKey.create("a", "B", "c", "d"));
		assertNotEquals(base, MapKey.create("a", "b", "C", "d"));
		assertNotEquals(base, MapKey.create("a", "b", "c", "D"));
	}

	@Test
	public void twoArgAndThreeArgKeysAreEqualWhenTrailingSlotsAreNull() {
		MapKey two = new MapKey("a", "b");
		MapKey three = new MapKey("a", "b", null);
		// Both fill the remaining slots with null, so they should compare equal.
		// This documents current behavior: arity is encoded via nulls, not a
		// separate field.
		assertEquals(two, three);
		assertEquals(two.hashCode(), three.hashCode());
	}

	@Test
	public void nullFieldsAreAllowedAndCompareEqual() {
		MapKey a = new MapKey(null, "b");
		MapKey b = new MapKey(null, "b");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, new MapKey("x", "b"));
	}

	@Test
	public void equalsRejectsNullAndUnrelatedType() {
		MapKey key = new MapKey("x", "y");
		assertFalse(key.equals(null));
		assertFalse(key.equals("not a MapKey"));
	}

	@Test
	public void reflexiveEquality() {
		MapKey key = new MapKey("x", "y");
		assertTrue(key.equals(key));
	}

	@Test
	public void subclassIsNotEqualToBaseClassEvenWithSameFields() {
		MapKey base = new MapKey("x", "y");
		MapKey sub = new NamedMapKey("x", "y");
		// getClass()-based check in equals means different concrete classes
		// never compare equal, even if fields line up. This protects callers
		// who use multiple MapKey subclasses in the same cache.
		assertNotEquals(base, sub);
		assertNotEquals(sub, base);
	}

	@Test
	public void hashCodeIsCachedAndStable() {
		MapKey key = MapKey.create("x", "y", "z", "w");
		int first = key.hashCode();
		int second = key.hashCode();
		assertEquals(first, second);
	}

	@Test
	public void toStringIncludesAllFourSlots() {
		MapKey key = new MapKey("a", "b", "c", "d");
		String s = key.toString();
		assertTrue(s.contains("o1=a"));
		assertTrue(s.contains("o2=b"));
		assertTrue(s.contains("o3=c"));
		assertTrue(s.contains("o4=d"));
	}

	@Test
	public void worksAsKeyInHashMap() {
		Map<MapKey, String> map = new HashMap<>();
		map.put(MapKey.create("route1", "stop1"), "first");
		map.put(MapKey.create("route1", "stop2"), "second");
		map.put(MapKey.create("route1", "stop1"), "third"); // overwrites
		assertEquals(2, map.size());
		assertEquals("third", map.get(MapKey.create("route1", "stop1")));
		assertEquals("second", map.get(MapKey.create("route1", "stop2")));
	}

	/** Subclass used to verify getClass()-based equals check. */
	private static class NamedMapKey extends MapKey {
		NamedMapKey(Object o1, Object o2) {
			super(o1, o2);
		}
	}
}
