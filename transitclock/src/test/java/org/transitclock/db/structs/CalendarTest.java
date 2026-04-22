package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.transitclock.gtfs.gtfsStructs.GtfsCalendar;
import org.transitclock.utils.Time;

public class CalendarTest {

	private static final DateFormat GTFS_DATE =
			new SimpleDateFormat("yyyyMMdd");

	private static GtfsCalendar gtfs(String mon, String tue, String wed,
			String thu, String fri, String sat, String sun,
			String start, String end) {
		return new GtfsCalendar("svc", mon, tue, wed, thu, fri, sat, sun,
				start, end);
	}

	@Test
	public void dayFlagsInterpretedAsOneMeansTrue() {
		Calendar c = new Calendar(1,
				gtfs("1", "0", "1", "0", "1", "0", "1",
						"20240101", "20240107"), GTFS_DATE);
		assertTrue(c.getMonday());
		assertFalse(c.getTuesday());
		assertTrue(c.getWednesday());
		assertFalse(c.getThursday());
		assertTrue(c.getFriday());
		assertFalse(c.getSaturday());
		assertTrue(c.getSunday());
	}

	@Test
	public void nullDayFlagsTreatedAsFalse() {
		Calendar c = new Calendar(1,
				gtfs(null, null, null, null, null, null, null,
						"20240101", "20240107"), GTFS_DATE);
		assertFalse(c.getMonday());
		assertFalse(c.getTuesday());
		assertFalse(c.getWednesday());
		assertFalse(c.getThursday());
		assertFalse(c.getFriday());
		assertFalse(c.getSaturday());
		assertFalse(c.getSunday());
	}

	@Test
	public void nonOneValuesTreatedAsFalse() {
		// Only the literal "1" flips the flag true.
		Calendar c = new Calendar(1,
				gtfs("yes", "true", "2", "", " 1 ", "0", "1",
						"20240101", "20240107"), GTFS_DATE);
		assertFalse(c.getMonday());
		assertFalse(c.getTuesday());
		assertFalse(c.getWednesday());
		assertFalse(c.getThursday());
		// Surrounding spaces get trimmed, so " 1 " counts as "1".
		assertTrue(c.getFriday());
		assertFalse(c.getSaturday());
		assertTrue(c.getSunday());
	}

	@Test
	public void startDateParsedAsGtfsFormat() throws Exception {
		Calendar c = new Calendar(1,
				gtfs("1", "1", "1", "1", "1", "0", "0",
						"20240101", "20240107"), GTFS_DATE);
		Date expected = GTFS_DATE.parse("20240101");
		assertEquals(expected, c.getStartDate());
	}

	@Test
	public void endDateIsConfiguredEndPlusOneDay() throws Exception {
		Calendar c = new Calendar(1,
				gtfs("1", "1", "1", "1", "1", "0", "0",
						"20240101", "20240107"), GTFS_DATE);
		Date configured = GTFS_DATE.parse("20240107");
		long expectedEnd = configured.getTime() + Time.MS_PER_DAY;
		assertEquals(expectedEnd, c.getEndDate().getTime());
	}

	@Test
	public void stringFormattersUseMmDdYyyy() throws Exception {
		Calendar c = new Calendar(1,
				gtfs("1", "1", "1", "1", "1", "0", "0",
						"20240101", "20240107"), GTFS_DATE);
		// formatter inside Calendar is SimpleDateFormat("MM-dd-yyyy").
		assertEquals("01-01-2024", c.getStartDateStr());
		assertEquals("01-07-2024", c.getEndDateStr());
	}

	@Test
	public void serviceIdAndConfigRevRoundTrip() {
		Calendar c = new Calendar(7,
				gtfs("1", "1", "1", "1", "1", "0", "0",
						"20240101", "20240107"), GTFS_DATE);
		assertEquals(7, c.getConfigRev());
		assertEquals("svc", c.getServiceId());
	}

	@Test
	public void equalsAndHashCodeMatchOnAllIdFields() {
		Calendar a = new Calendar(1,
				gtfs("1", "1", "1", "1", "1", "0", "0",
						"20240101", "20240107"), GTFS_DATE);
		Calendar b = new Calendar(1,
				gtfs("1", "1", "1", "1", "1", "0", "0",
						"20240101", "20240107"), GTFS_DATE);
		Calendar differentRev = new Calendar(2,
				gtfs("1", "1", "1", "1", "1", "0", "0",
						"20240101", "20240107"), GTFS_DATE);
		Calendar differentSaturday = new Calendar(1,
				gtfs("1", "1", "1", "1", "1", "1", "0",
						"20240101", "20240107"), GTFS_DATE);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, differentRev);
		assertNotEquals(a, differentSaturday);
		assertFalse(a.equals(null));
		assertFalse(a.equals("not a Calendar"));
	}
}
