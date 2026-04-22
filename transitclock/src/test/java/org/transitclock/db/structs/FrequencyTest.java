package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.transitclock.gtfs.gtfsStructs.GtfsFrequency;

public class FrequencyTest {

	private static final String[] HEADERS = { "trip_id", "start_time",
			"end_time", "headway_secs", "exact_times" };

	private static CSVRecord row(String... values) throws IOException {
		String line = String.join(",", values);
		CSVFormat fmt = CSVFormat.DEFAULT.withHeader(HEADERS);
		try (CSVParser parser = new CSVParser(new StringReader(line), fmt)) {
			return parser.getRecords().get(0);
		}
	}

	private static GtfsFrequency gtfs(String tripId, String startTime,
			String endTime, String headwaySecs, String exactTimes)
			throws IOException {
		return new GtfsFrequency(
				row(tripId, startTime, endTime, headwaySecs, exactTimes),
				false, "frequencies.txt");
	}

	@Test
	public void gettersRoundTripFromConstructor() throws Exception {
		Frequency f = new Frequency(3,
				gtfs("t1", "06:00:00", "09:00:00", "600", "1"));
		assertEquals(3, f.getConfigRev());
		assertEquals("t1", f.getTripId());
		assertEquals(6 * 3600, f.getStartTime());
		assertEquals(9 * 3600, f.getEndTime());
		assertEquals(600, f.getHeadwaySecs());
		assertTrue(f.getExactTimes());
	}

	@Test
	public void exactTimesAbsentDefaultsFalse() throws Exception {
		// Optional boolean not provided → GtfsFrequency returns null, Frequency
		// coerces that to false.
		Frequency f = new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", ""));
		assertFalse(f.getExactTimes());
	}

	@Test
	public void exactTimesZeroIsFalse() throws Exception {
		// "0" is a valid value but CsvBase.getOptionalBooleanValue only flips
		// true on "1"/"t"/"true"; "0" returns Boolean.FALSE.
		Frequency f = new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", "0"));
		assertFalse(f.getExactTimes());
	}

	@Test
	public void equalsMatchesOnAllFields() throws Exception {
		Frequency a = new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", "1"));
		Frequency b = new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", "1"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachField() throws Exception {
		Frequency base = new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", "1"));

		assertNotEquals(base, new Frequency(2,
				gtfs("t1", "06:00:00", "09:00:00", "600", "1")));
		assertNotEquals(base, new Frequency(1,
				gtfs("t2", "06:00:00", "09:00:00", "600", "1")));
		assertNotEquals(base, new Frequency(1,
				gtfs("t1", "07:00:00", "09:00:00", "600", "1")));
		assertNotEquals(base, new Frequency(1,
				gtfs("t1", "06:00:00", "10:00:00", "600", "1")));
		assertNotEquals(base, new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "900", "1")));
		assertNotEquals(base, new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", "0")));
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		Frequency f = new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", "1"));
		assertFalse(f.equals(null));
		assertFalse(f.equals("not a Frequency"));
	}

	@Test
	public void toStringMentionsAllFields() throws Exception {
		String s = new Frequency(1,
				gtfs("t1", "06:00:00", "09:00:00", "600", "1")).toString();
		assertTrue(s.startsWith("Frequency ["));
		assertTrue(s.contains("configRev=1"));
		assertTrue(s.contains("tripId=t1"));
		assertTrue(s.contains("startTime=" + (6 * 3600)));
		assertTrue(s.contains("endTime=" + (9 * 3600)));
		assertTrue(s.contains("headwaySecs=600"));
		assertTrue(s.contains("exactTimes=true"));
	}
}
