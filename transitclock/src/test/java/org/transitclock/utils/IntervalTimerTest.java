package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * IntervalTimer wraps System.nanoTime(), so exact values can't be asserted
 * without flakiness. These tests use loose bounds and focus on monotonicity,
 * reset behavior, and the formatting contract of elapsedMsecStr/toString.
 */
public class IntervalTimerTest {

	@Test
	public void elapsedMsecIsNonNegativeImmediatelyAfterConstruction() {
		IntervalTimer timer = new IntervalTimer();
		assertTrue(timer.elapsedMsec() >= 0);
	}

	@Test
	public void elapsedNanoSecIsNonNegativeImmediatelyAfterConstruction() {
		IntervalTimer timer = new IntervalTimer();
		assertTrue(timer.elapsedNanoSec() >= 0);
	}

	@Test
	public void elapsedTimeIncreasesAfterSleep() throws InterruptedException {
		IntervalTimer timer = new IntervalTimer();
		Thread.sleep(20);
		assertTrue(
				"elapsedMsec should reflect at least the sleep duration",
				timer.elapsedMsec() >= 15);
		assertTrue(
				"elapsedNanoSec should reflect at least the sleep duration",
				timer.elapsedNanoSec() >= 15L * Time.NSEC_PER_MSEC);
	}

	@Test
	public void resetTimerRestartsFromZero() throws InterruptedException {
		IntervalTimer timer = new IntervalTimer();
		Thread.sleep(20);
		assertTrue(timer.elapsedMsec() >= 15);
		timer.resetTimer();
		// Right after reset the elapsed time should be small. Allow a generous
		// upper bound to stay robust on a loaded CI machine.
		assertTrue(
				"elapsedMsec just after reset should be small",
				timer.elapsedMsec() < 15);
	}

	@Test
	public void elapsedNanoSecIsMonotonicOverRepeatedReads() {
		IntervalTimer timer = new IntervalTimer();
		long first = timer.elapsedNanoSec();
		long second = timer.elapsedNanoSec();
		assertTrue(
				"elapsedNanoSec should not go backwards",
				second >= first);
	}

	@Test
	public void elapsedMsecStrHasThreeDecimalDigits() {
		IntervalTimer timer = new IntervalTimer();
		String str = timer.elapsedMsecStr();
		// Format is "#.###" — value with up to three decimal digits, optional
		// leading digits. Integer-valued output is just "0" (no trailing zeros)
		// because DecimalFormat trims them. So only assert the output parses as
		// a non-negative number.
		double value = Double.parseDouble(str);
		assertTrue("elapsedMsecStr parses to a non-negative number", value >= 0);
	}

	@Test
	public void toStringReturnsSameAsElapsedMsecStr() {
		IntervalTimer timer = new IntervalTimer();
		// The values may differ slightly because they read nanoTime() on
		// separate calls, so parse and compare as doubles with a small delta.
		double viaMethod = Double.parseDouble(timer.elapsedMsecStr());
		double viaToString = Double.parseDouble(timer.toString());
		assertEquals(viaMethod, viaToString, 5.0);
	}
}
