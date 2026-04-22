package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class MathUtilsTest {

	@Test
	public void roundPassesNaNThrough() {
		assertTrue(Double.isNaN(MathUtils.round(Double.NaN, 3)));
	}

	@Test
	public void roundsHalfAwayFromZero() {
		assertEquals(1.24, MathUtils.round(1.235, 2), 1e-12);
		assertEquals(-1.24, MathUtils.round(-1.235, 2), 1e-12);
	}

	@Test
	public void roundTruncatesToRequestedDigits() {
		assertEquals(37.123, MathUtils.round(37.123456, 3), 1e-12);
		assertEquals(1.0, MathUtils.round(1.0, 5), 1e-12);
		assertEquals(-1.23, MathUtils.round(-1.234, 2), 1e-12);
	}

	@Test
	public void sumOfEmptyIsZero() {
		assertEquals(0.0, MathUtils.sum(Collections.<Double>emptyList()), 1e-12);
	}

	@Test
	public void sumAddsValues() {
		assertEquals(6.0, MathUtils.sum(Arrays.asList(1.0, 2.0, 3.0)), 1e-12);
		assertEquals(0.0, MathUtils.sum(Arrays.asList(-1.5, 1.5)), 1e-12);
	}

	@Test
	public void averageComputesArithmeticMean() {
		assertEquals(2.0, MathUtils.average(Arrays.asList(1.0, 2.0, 3.0)), 1e-12);
	}

	@Test
	public void averageOfEmptyIsNaN() {
		assertTrue(Double.isNaN(MathUtils.average(Collections.<Double>emptyList())));
	}

	@Test
	public void minReturnsSmallestValue() {
		assertEquals(-3.0,
				MathUtils.min(Arrays.asList(-3.0, 0.0, 4.5)), 1e-12);
	}

	@Test(expected = IllegalArgumentException.class)
	public void minOfEmptyThrows() {
		MathUtils.min(Collections.<Double>emptyList());
	}

	@Test
	public void maxReturnsLargestValue() {
		assertEquals(4.5,
				MathUtils.max(Arrays.asList(-3.0, 0.0, 4.5)), 1e-12);
	}

	@Test
	public void maxIsFlooredAtZeroForAllNegativeInputs() {
		// Documents current implementation behavior: max is seeded at 0, so an
		// all-negative collection returns 0.0 rather than the true maximum.
		assertEquals(0.0,
				MathUtils.max(Arrays.asList(-5.0, -3.0, -7.0)), 1e-12);
	}

	@Test(expected = IllegalArgumentException.class)
	public void maxOfEmptyThrows() {
		MathUtils.max(Collections.<Double>emptyList());
	}
}
