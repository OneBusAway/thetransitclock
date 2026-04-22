package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.transitclock.gtfs.gtfsStructs.GtfsCalendarDate;

public class CalendarDateTest {

	private static final DateFormat GTFS_DATE =
			new SimpleDateFormat("yyyyMMdd");

	private static final String[] HEADERS =
			{ "service_id", "date", "exception_type" };

	private static CSVRecord row(String... values) throws IOException {
		String line = String.join(",", values);
		CSVFormat fmt = CSVFormat.DEFAULT.withHeader(HEADERS);
		try (CSVParser parser = new CSVParser(new StringReader(line), fmt)) {
			return parser.getRecords().get(0);
		}
	}

	private static GtfsCalendarDate gtfs(String serviceId, String date,
			String exceptionType) throws IOException, java.text.ParseException {
		return new GtfsCalendarDate(row(serviceId, date, exceptionType),
				false, "calendar_dates.txt");
	}

	@Test
	public void gettersRoundTripFromConstructor() throws Exception {
		CalendarDate cd = new CalendarDate(5,
				gtfs("weekday", "20240704", "2"), GTFS_DATE);
		assertEquals(5, cd.getConfigRev());
		assertEquals("weekday", cd.getServiceId());
		assertEquals(GTFS_DATE.parse("20240704"), cd.getDate());
		assertEquals(GTFS_DATE.parse("20240704").getTime(), cd.getTime());
		assertEquals("2", cd.getExceptionType());
	}

	@Test
	public void addServiceTrueOnlyWhenExceptionTypeIsOne() throws Exception {
		// GTFS: 1 = service added, 2 = service removed.
		CalendarDate added = new CalendarDate(1,
				gtfs("svc", "20240101", "1"), GTFS_DATE);
		CalendarDate removed = new CalendarDate(1,
				gtfs("svc", "20240101", "2"), GTFS_DATE);
		assertTrue(added.addService());
		assertFalse(removed.addService());
	}

	@Test
	public void unparseableDateFallsBackToNow() throws Exception {
		// Bad date strings are logged and replaced with the current time so the
		// constructor never throws. Verify the replacement is a valid Date
		// within a reasonable tolerance of now.
		long before = System.currentTimeMillis();
		CalendarDate cd = new CalendarDate(1,
				gtfs("svc", "not-a-date", "1"), GTFS_DATE);
		long after = System.currentTimeMillis();
		Date fallback = cd.getDate();
		assertTrue("fallback date should be near now",
				fallback.getTime() >= before && fallback.getTime() <= after);
	}

	@Test
	public void toStringMentionsFieldsAndAddSubtractToken() throws Exception {
		String added = new CalendarDate(1,
				gtfs("svc", "20240101", "1"), GTFS_DATE).toString();
		String removed = new CalendarDate(1,
				gtfs("svc", "20240101", "2"), GTFS_DATE).toString();

		assertTrue(added.startsWith("CalendarDate ["));
		assertTrue(added.contains("serviceId=svc"));
		assertTrue(added.contains("exceptionType=1"));
		assertTrue(added.contains("(add service)"));
		assertTrue(removed.contains("(subtract service)"));
	}

	@Test
	public void equalsMatchesOnAllIdFields() throws Exception {
		CalendarDate a = new CalendarDate(1,
				gtfs("svc", "20240101", "1"), GTFS_DATE);
		CalendarDate b = new CalendarDate(1,
				gtfs("svc", "20240101", "1"), GTFS_DATE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachField() throws Exception {
		CalendarDate base = new CalendarDate(1,
				gtfs("svc", "20240101", "1"), GTFS_DATE);

		assertNotEquals(base, new CalendarDate(2,
				gtfs("svc", "20240101", "1"), GTFS_DATE));
		assertNotEquals(base, new CalendarDate(1,
				gtfs("other", "20240101", "1"), GTFS_DATE));
		assertNotEquals(base, new CalendarDate(1,
				gtfs("svc", "20240102", "1"), GTFS_DATE));
		assertNotEquals(base, new CalendarDate(1,
				gtfs("svc", "20240101", "2"), GTFS_DATE));
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		CalendarDate cd = new CalendarDate(1,
				gtfs("svc", "20240101", "1"), GTFS_DATE);
		assertFalse(cd.equals(null));
		assertFalse(cd.equals("not a CalendarDate"));
	}
}
