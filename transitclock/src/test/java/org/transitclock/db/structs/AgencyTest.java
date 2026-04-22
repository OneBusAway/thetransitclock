package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.transitclock.gtfs.gtfsStructs.GtfsAgency;

/**
 * Agency's public constructor takes a GtfsAgency (which itself only has a
 * CSVRecord-based public constructor). We stage a single-row CSV in memory to
 * build GtfsAgency instances. Agency also requires a List<Route> — we pass
 * an empty list so Extent stays at its default ±infinity state with no adds.
 */
public class AgencyTest {

	private static final String[] HEADERS = {
			"agency_id", "agency_name", "agency_url", "agency_timezone",
			"agency_lang", "agency_phone", "agency_fare_url" };

	private static CSVRecord row(String... values) throws IOException {
		// commons-csv 1.1 doesn't have withFirstRecordAsHeader, so we hand it
		// the header names explicitly and parse just the data line.
		String line = String.join(",", values);
		CSVFormat fmt = CSVFormat.DEFAULT.withHeader(HEADERS);
		try (CSVParser parser = new CSVParser(new StringReader(line), fmt)) {
			return parser.getRecords().get(0);
		}
	}

	private static GtfsAgency gtfs(String id, String name, String url,
			String tz, String lang, String phone, String fareUrl)
			throws IOException {
		return new GtfsAgency(row(id, name, url, tz, lang, phone, fareUrl),
				false, "agency.txt");
	}

	private static Agency defaultAgency() throws IOException {
		GtfsAgency g = gtfs("ag1", "My Agency", "http://example.com",
				"America/Los_Angeles", "en", "555-1234", "http://fares.example.com");
		return new Agency(3, g, Collections.emptyList());
	}

	@Test
	public void gettersRoundTripFromConstructor() throws Exception {
		Agency a = defaultAgency();
		assertEquals(3, a.getConfigRev());
		assertEquals("ag1", a.getId());
		assertEquals("My Agency", a.getName());
		assertEquals("http://example.com", a.getUrl());
		assertEquals("America/Los_Angeles", a.getTimeZoneStr());
		assertEquals("en", a.getLang());
		assertEquals("555-1234", a.getPhone());
		assertEquals("http://fares.example.com", a.getFareUrl());
	}

	@Test
	public void extentIsEmptyWhenNoRoutesProvided() throws Exception {
		// With no routes, Extent.add is never called — min/max stay at ±infinity.
		Agency a = defaultAgency();
		Extent e = a.getExtent();
		assertNotNull(e);
		assertEquals(Double.POSITIVE_INFINITY, e.getMinLat(), 0.0);
		assertEquals(Double.NEGATIVE_INFINITY, e.getMaxLat(), 0.0);
		assertEquals(Double.POSITIVE_INFINITY, e.getMinLon(), 0.0);
		assertEquals(Double.NEGATIVE_INFINITY, e.getMaxLon(), 0.0);
	}

	@Test
	public void getTimeZoneResolvesAgencyTimezoneString() throws Exception {
		Agency a = defaultAgency();
		TimeZone tz = a.getTimeZone();
		assertNotNull(tz);
		assertEquals("America/Los_Angeles", tz.getID());
	}

	@Test
	public void getTimeZoneIsCached() throws Exception {
		// Second call should return the same TimeZone instance, not a fresh lookup.
		Agency a = defaultAgency();
		TimeZone first = a.getTimeZone();
		TimeZone second = a.getTimeZone();
		assertSame(first, second);
	}

	@Test
	public void getTimeIsLazilyConstructedAndCached() throws Exception {
		Agency a = defaultAgency();
		assertNotNull(a.getTime());
		assertSame(a.getTime(), a.getTime());
	}

	@Test
	public void optionalFieldsAreNullWhenAbsent() throws Exception {
		// Only required columns populated → optional ones come back null.
		// Empty strings in CsvBase.getValue() are normalized to null.
		GtfsAgency g = gtfs("ag1", "My Agency", "http://example.com",
				"UTC", "", "", "");
		Agency a = new Agency(1, g, Collections.emptyList());
		assertNull(a.getLang());
		assertNull(a.getPhone());
		assertNull(a.getFareUrl());
	}

	@Test
	public void equalsMatchesOnAllFields() throws Exception {
		Agency a = defaultAgency();
		Agency b = defaultAgency();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsDivergesOnConfigRev() throws Exception {
		GtfsAgency g = gtfs("ag1", "My Agency", "http://example.com",
				"UTC", "en", "555-1234", "http://fares.example.com");
		Agency rev1 = new Agency(1, g, Collections.emptyList());
		Agency rev2 = new Agency(2, g, Collections.emptyList());
		assertNotEquals(rev1, rev2);
	}

	@Test
	public void equalsDivergesOnName() throws Exception {
		GtfsAgency left = gtfs("ag1", "Left", "http://example.com", "UTC",
				"en", "555-1234", "http://fares.example.com");
		GtfsAgency right = gtfs("ag1", "Right", "http://example.com", "UTC",
				"en", "555-1234", "http://fares.example.com");
		Agency a = new Agency(1, left, Collections.emptyList());
		Agency b = new Agency(1, right, Collections.emptyList());
		assertNotEquals(a, b);
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		Agency a = defaultAgency();
		assertFalse(a.equals(null));
		assertFalse(a.equals("not an Agency"));
	}

	@Test
	public void toStringMentionsAllFields() throws Exception {
		String s = defaultAgency().toString();
		assertTrue(s.startsWith("Agency ["));
		assertTrue(s.contains("configRev=3"));
		assertTrue(s.contains("agencyId=ag1"));
		assertTrue(s.contains("agencyName=My Agency"));
		assertTrue(s.contains("agencyTimezone=America/Los_Angeles"));
		assertTrue(s.contains("agencyFareUrl=http://fares.example.com"));
	}
}
