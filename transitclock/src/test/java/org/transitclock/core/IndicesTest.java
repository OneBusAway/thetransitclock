package org.transitclock.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Indices reaches into Block (a final Hibernate entity) for most of its
 * increment/decrement state machine. The 4-arg constructor skips validation
 * when block is null, so these tests exercise the pure-int comparators and
 * boundary predicates that don't require a Block fixture.
 */
public class IndicesTest {

	private static Indices at(int trip, int stopPath, int segment) {
		return new Indices(null, trip, stopPath, segment);
	}

	@Test
	public void lessThanComparesTripIndexFirst() {
		assertTrue(at(0, 5, 5).lessThan(at(1, 0, 0)));
		assertFalse(at(1, 0, 0).lessThan(at(0, 5, 5)));
	}

	@Test
	public void lessThanBreaksTiesByStopPathThenSegment() {
		assertTrue(at(1, 2, 0).lessThan(at(1, 3, 0)));
		assertFalse(at(1, 3, 0).lessThan(at(1, 2, 0)));
		assertTrue(at(1, 3, 2).lessThan(at(1, 3, 3)));
		assertFalse(at(1, 3, 3).lessThan(at(1, 3, 3)));
	}

	@Test
	public void equalsConsidersAllIndices() {
		Indices a = at(1, 2, 3);
		Indices b = at(1, 2, 3);
		Indices differentSegment = at(1, 2, 4);
		Indices differentStopPath = at(1, 5, 3);
		Indices differentTrip = at(2, 2, 3);

		assertEquals(a, b);
		assertFalse(a.equals(differentSegment));
		assertFalse(a.equals(differentStopPath));
		assertFalse(a.equals(differentTrip));
		assertFalse(a.equals(null));
		assertFalse(a.equals("not an Indices"));
	}

	@Test
	public void cloneProducesDistinctButEqualInstance() {
		Indices original = at(1, 2, 3);
		Indices copy = original.clone();
		assertNotSame(original, copy);
		assertEquals(original, copy);
		assertEquals(original.getTripIndex(), copy.getTripIndex());
		assertEquals(original.getStopPathIndex(), copy.getStopPathIndex());
		assertEquals(original.getSegmentIndex(), copy.getSegmentIndex());
	}

	@Test
	public void equalStopPathIgnoresSegmentIndex() {
		Indices a = at(1, 2, 3);
		Indices b = at(1, 2, 99);
		assertTrue(a.equalStopPath(b));
		assertFalse(a.equalStopPath(at(1, 3, 3)));
		assertFalse(a.equalStopPath(at(2, 2, 3)));
	}

	@Test
	public void beforeBeginningOfBlockChecksTripIndexSignOnly() {
		assertTrue(at(-1, 0, 0).beforeBeginningOfBlock());
		assertFalse(at(0, 0, 0).beforeBeginningOfBlock());
		assertFalse(at(5, 0, 0).beforeBeginningOfBlock());
	}

	@Test
	public void atBeginningOfTripRequiresBothStopPathAndSegmentZero() {
		assertTrue(at(0, 0, 0).atBeginningOfTrip());
		assertTrue(at(3, 0, 0).atBeginningOfTrip());
		assertFalse(at(0, 1, 0).atBeginningOfTrip());
		assertFalse(at(0, 0, 1).atBeginningOfTrip());
	}

	@Test
	public void gettersReflectConstructorArgs() {
		Indices i = at(4, 7, 2);
		assertEquals(4, i.getTripIndex());
		assertEquals(7, i.getStopPathIndex());
		assertEquals(2, i.getSegmentIndex());
		assertEquals(null, i.getBlock());
	}
}
