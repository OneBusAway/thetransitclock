package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.NumberFormat;
import java.text.ParseException;

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
	public void elapsedMsecStrParsesAsNonNegativeNumber() throws ParseException {
		IntervalTimer timer = new IntervalTimer();
		String str = timer.elapsedMsecStr();
		// Format is "#.###" but DecimalFormat trims trailing zeros, so we
		// can't assert "three decimal digits" literally. The meaningful
		// contract is that the output is a well-formed non-negative number.
		// Use a locale-aware parser since DecimalFormat output uses the JVM's
		// default locale separators (e.g. "," in de_DE).
		double value = NumberFormat.getNumberInstance().parse(str).doubleValue();
		assertTrue("elapsedMsecStr parses to a non-negative number", value >= 0);
	}

	@Test
	public void toStringReturnsSameAsElapsedMsecStr() throws ParseException {
		IntervalTimer timer = new IntervalTimer();
		// The values may differ slightly because they read nanoTime() on
		// separate calls, so parse and compare as doubles with a small delta.
		NumberFormat fmt = NumberFormat.getNumberInstance();
		double viaMethod = fmt.parse(timer.elapsedMsecStr()).doubleValue();
		double viaToString = fmt.parse(timer.toString()).doubleValue();
		assertEquals(viaMethod, viaToString, 5.0);
	}
}
