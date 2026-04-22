package org.transitclock.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.transitclock.configData.CoreConfig;

public class TemporalDifferenceTest {

	@Test
	public void intConstructorRoundTrips() {
		assertEquals(60_000, new TemporalDifference(60_000).early());
		assertEquals(-60_000, new TemporalDifference(-60_000).early());
		assertEquals(0, new TemporalDifference(0).early());
	}

	@Test
	public void longConstructorNarrowsToInt() {
		long oneHourMs = 3_600_000L;
		assertEquals(3_600_000, new TemporalDifference(oneHourMs).early());
	}

	@Test
	public void addTimeWhenLateMakesMoreLate() {
		TemporalDifference td = new TemporalDifference(-60_000);
		td.addTime(30_000);
		assertEquals(-90_000, td.early());
	}

	@Test
	public void addTimeWhenEarlyScalesByEarlyToLateRatio() {
		double ratio = CoreConfig.getEarlyToLateRatio();
		TemporalDifference td = new TemporalDifference(60_000);
		td.addTime(30_000);
		assertEquals(60_000 + (int) (30_000 / ratio), td.early());
	}

	@Test
	public void addTimeWhenOnTimeScalesByEarlyToLateRatio() {
		double ratio = CoreConfig.getEarlyToLateRatio();
		TemporalDifference td = new TemporalDifference(0);
		td.addTime(30_000);
		assertEquals((int) (30_000 / ratio), td.early());
	}

	@Test
	public void isEarlierThanIsStrict() {
		assertFalse("boundary is not 'earlier than'",
				new TemporalDifference(30_000).isEarlierThan(30));
		assertTrue(new TemporalDifference(30_001).isEarlierThan(30));
		assertFalse(new TemporalDifference(29_999).isEarlierThan(30));
		assertFalse("a late vehicle is never 'earlier than' any positive bound",
				new TemporalDifference(-60_000).isEarlierThan(30));
	}

	@Test
	public void isLaterThanIsStrict() {
		assertFalse("boundary is not 'later than'",
				new TemporalDifference(-30_000).isLaterThan(30));
		assertTrue(new TemporalDifference(-30_001).isLaterThan(30));
		assertFalse(new TemporalDifference(-29_999).isLaterThan(30));
		assertFalse("an early vehicle is never 'later than' any positive bound",
				new TemporalDifference(60_000).isLaterThan(30));
	}

	@Test
	public void isWithinBoundsWithExplicitArgsIsInclusiveAtBoundary() {
		// isWithinBounds is !isEarlierThan(e) && !isLaterThan(l), so the
		// boundary is within.
		assertTrue(new TemporalDifference(30_000).isWithinBounds(30, 30));
		assertTrue(new TemporalDifference(-30_000).isWithinBounds(30, 30));

		assertFalse(new TemporalDifference(30_001).isWithinBounds(30, 30));
		assertFalse(new TemporalDifference(-30_001).isWithinBounds(30, 30));
	}

	@Test
	public void noArgIsWithinBoundsUsesStrictLessThan() {
		int earlyMs = CoreConfig.getAllowableEarlySeconds() * 1000;
		int lateMs = CoreConfig.getAllowableLateSeconds() * 1000;

		// Exactly at the boundary is NOT within bounds (the no-arg variant uses
		// strict <, unlike isWithinBounds(early, late)).
		assertFalse(new TemporalDifference(earlyMs).isWithinBounds());
		assertFalse(new TemporalDifference(-lateMs).isWithinBounds());

		assertTrue(new TemporalDifference(earlyMs - 1).isWithinBounds());
		assertTrue(new TemporalDifference(-(lateMs - 1)).isWithinBounds());
	}

	@Test
	public void betterThanNullIsAlwaysTrue() {
		assertTrue(new TemporalDifference(Integer.MAX_VALUE).betterThan(null));
		assertTrue(new TemporalDifference(0).betterThan(null));
		assertTrue(new TemporalDifference(Integer.MIN_VALUE + 1).betterThan(null));
	}

	@Test
	public void betterThanPenalizesEarlinessByRatio() {
		// With the default earlyToLateRatio of 3.0, being 60s late (abs=60_000)
		// is "better" than being 60s early (abs=180_000).
		TemporalDifference early60 = new TemporalDifference(60_000);
		TemporalDifference late60 = new TemporalDifference(-60_000);

		assertTrue(late60.betterThan(early60));
		assertFalse(early60.betterThan(late60));
	}

	@Test
	public void betterThanIsStrict() {
		TemporalDifference a = new TemporalDifference(-30_000);
		TemporalDifference b = new TemporalDifference(-30_000);
		assertFalse("equal temporal differences are not strictly better",
				a.betterThan(b));
		assertTrue(a.betterThanOrEqualTo(b));
	}

	@Test
	public void betterThanOrEqualToNullIsTrue() {
		assertTrue(new TemporalDifference(0).betterThanOrEqualTo(null));
	}

	@Test
	public void toStringTagsEarlyOnTimeLate() {
		assertTrue(new TemporalDifference(60_000).toString().endsWith("(early)"));
		assertTrue(new TemporalDifference(0).toString().endsWith("(ontime)"));
		assertTrue(new TemporalDifference(-60_000).toString().endsWith("(late)"));
	}
}
