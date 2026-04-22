package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class TimeTest {

	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	// 2024-01-15 00:00:00 UTC
	private static final long JAN_15_2024_UTC = 1_705_276_800_000L;

	@Test
	public void msConstantsHangTogether() {
		assertEquals(1000, Time.MS_PER_SEC);
		assertEquals(60_000, Time.MS_PER_MIN);
		assertEquals(3_600_000L, Time.MS_PER_HOUR);
		assertEquals(86_400_000L, Time.MS_PER_DAY);
	}

	@Test
	public void parseTimeOfDayHandlesHhMmSs() {
		assertEquals(3723, Time.parseTimeOfDay("01:02:03"));
		assertEquals(43_200, Time.parseTimeOfDay("12:00:00"));
		assertEquals(86_399, Time.parseTimeOfDay("23:59:59"));
	}

	@Test
	public void parseTimeOfDayHandlesHhMm() {
		assertEquals(43_200, Time.parseTimeOfDay("12:00"));
		assertEquals(3600, Time.parseTimeOfDay("01:00"));
	}

	@Test
	public void parseTimeOfDayHandlesGtfsOverflowHours() {
		assertEquals(28 * 3600 + 15 * 60, Time.parseTimeOfDay("28:15:00"));
	}

	@Test
	public void parseTimeOfDayHandlesNegativeValues() {
		assertEquals(-3600, Time.parseTimeOfDay("-01:00:00"));
		assertEquals(-3723, Time.parseTimeOfDay("-01:02:03"));
	}

	@Test
	public void timeOfDayStrFormatsZeroPaddedHhMmSs() {
		assertEquals("00:00:00", Time.timeOfDayStr(0L));
		assertEquals("01:02:03", Time.timeOfDayStr(3723L));
		assertEquals("23:59:59", Time.timeOfDayStr(86_399L));
	}

	@Test
	public void timeOfDayStrAllowsOverflowAndNegativeValues() {
		assertEquals("28:15:00", Time.timeOfDayStr(28L * 3600 + 15 * 60));
		assertEquals("-01:00:00", Time.timeOfDayStr(-3600L));
		assertEquals("-01:02:03", Time.timeOfDayStr(-3723L));
	}

	@Test
	public void timeOfDayStrNullIntegerReturnsNull() {
		assertNull(Time.timeOfDayStr((Integer) null));
	}

	@Test
	public void timeOfDayShortStrFormatsHhMm() {
		assertEquals("01:02", Time.timeOfDayShortStr(3723L));
		assertEquals("-01:00", Time.timeOfDayShortStr(-3600L));
	}

	@Test
	public void timeOfDayShortStrNullIntegerReturnsNull() {
		assertNull(Time.timeOfDayShortStr((Integer) null));
	}

	@Test
	public void parseTimeOfDayRoundTripsViaTimeOfDayStr() {
		int[] samples = {0, 3723, 43_200, 86_399,
				28 * 3600 + 15 * 60, -3723};
		for (int sec : samples) {
			String s = Time.timeOfDayStr((long) sec);
			assertEquals("round trip for " + sec,
					sec, Time.parseTimeOfDay(s));
		}
	}

	@Test
	public void elapsedTimeStrShortFormIsSeconds() {
		String s = Time.elapsedTimeStr(60_000);
		assertTrue("expected seconds format, got " + s, s.endsWith(" sec"));
	}

	@Test
	public void elapsedTimeStrLongFormIsMinutes() {
		String s = Time.elapsedTimeStr(3 * Time.MS_PER_MIN);
		assertTrue("expected minutes format, got " + s, s.endsWith(" min"));
	}

	@Test
	public void getStartOfDayTruncatesToMidnightInSpecifiedZone() {
		Date noonUtc = new Date(JAN_15_2024_UTC + 12 * Time.MS_PER_HOUR);
		assertEquals(JAN_15_2024_UTC, Time.getStartOfDay(noonUtc, UTC));
	}

	@Test
	public void getStartOfDayIsIdempotent() {
		Date midnightUtc = new Date(JAN_15_2024_UTC);
		assertEquals(JAN_15_2024_UTC, Time.getStartOfDay(midnightUtc, UTC));
	}

	@Test
	public void getStartOfDayHonorsTimeZoneOffset() {
		// 2024-01-15 03:00 UTC is still 2024-01-14 in America/Los_Angeles.
		Date earlyMorningUtc = new Date(JAN_15_2024_UTC + 3 * Time.MS_PER_HOUR);
		TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");
		long start = Time.getStartOfDay(earlyMorningUtc, la);
		// Midnight LA on Jan 14 == 08:00 UTC on Jan 14 == JAN_15_2024_UTC - 16h.
		assertEquals(JAN_15_2024_UTC - 16 * Time.MS_PER_HOUR, start);
	}

	@Test
	public void getSecondsIntoDayReadsCalendarTimeInGivenZone() {
		Time time = new Time("UTC");
		long epoch = JAN_15_2024_UTC + 3723L * 1000L; // 01:02:03 UTC
		assertEquals(3723, time.getSecondsIntoDay(epoch));
	}

	@Test
	public void getMsecsIntoDayIncludesMillis() {
		Time time = new Time("UTC");
		long epoch = JAN_15_2024_UTC + 500L; // 00:00:00.500 UTC
		assertEquals(500, time.getMsecsIntoDay(new Date(epoch)));
	}

	@Test
	public void getEpochTimeSameDayNeedsNoAdjustment() {
		Time time = new Time("UTC");
		Date noon = new Date(JAN_15_2024_UTC + 12 * Time.MS_PER_HOUR);
		long result = time.getEpochTime(3600 /* 01:00 */, noon);
		assertEquals(JAN_15_2024_UTC + Time.MS_PER_HOUR, result);
	}

	@Test
	public void getEpochTimeForwardDayBoundaryRollsBack() {
		Time time = new Time("UTC");
		// Reference just after midnight; scheduled time is just before midnight.
		// Naive calendar set would produce the reference date's 23:58, which is
		// ~23.97h into the future — should roll back a day.
		Date justAfterMidnight =
				new Date(JAN_15_2024_UTC + 2 * Time.MS_PER_MIN);
		int secondsIntoDay = 23 * 3600 + 58 * 60; // 23:58:00
		long result = time.getEpochTime(secondsIntoDay, justAfterMidnight);
		long expected = JAN_15_2024_UTC - 2 * Time.MS_PER_MIN; // Jan 14 23:58
		assertEquals(expected, result);
	}

	@Test
	public void getEpochTimeBackwardDayBoundaryRollsForward() {
		Time time = new Time("UTC");
		// Reference just before midnight; scheduled time is just after midnight.
		Date justBeforeMidnight =
				new Date(JAN_15_2024_UTC + 24 * Time.MS_PER_HOUR - 2 * Time.MS_PER_MIN);
		int secondsIntoDay = 2 * 60; // 00:02:00
		long result = time.getEpochTime(secondsIntoDay, justBeforeMidnight);
		long expected = JAN_15_2024_UTC + 24 * Time.MS_PER_HOUR
				+ 2 * Time.MS_PER_MIN; // Jan 16 00:02
		assertEquals(expected, result);
	}
}
