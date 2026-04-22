package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.transitclock.gtfs.gtfsStructs.GtfsFareRule;

/**
 * FareRule is a composite-id entity whose id columns (routeId, originId,
 * destinationId, containsId) cannot be null in the database. The implementation
 * compensates by storing an empty string internally when GTFS provides null,
 * and the getters flip empty back to null on read.
 */
public class FareRuleTest {

	private static final String[] HEADERS = { "fare_id", "route_id",
			"origin_id", "destination_id", "contains_id" };

	private static CSVRecord row(String... values) throws IOException {
		String line = String.join(",", values);
		CSVFormat fmt = CSVFormat.DEFAULT.withHeader(HEADERS);
		try (CSVParser parser = new CSVParser(new StringReader(line), fmt)) {
			return parser.getRecords().get(0);
		}
	}

	private static GtfsFareRule gtfs(String fareId, String routeId,
			String originId, String destinationId, String containsId)
			throws IOException {
		return new GtfsFareRule(
				row(fareId, routeId, originId, destinationId, containsId),
				false, "fare_rules.txt");
	}

	@Test
	public void gettersRoundTripFromConstructor() throws Exception {
		FareRule r = new FareRule(2,
				gtfs("adult", "r1", "zoneA", "zoneB", "zoneC"), null);
		assertEquals(2, r.getConfigRev());
		assertEquals("adult", r.getFareId());
		assertEquals("r1", r.getRouteId());
		assertEquals("zoneA", r.getOriginId());
		assertEquals("zoneB", r.getDestinationId());
		assertEquals("zoneC", r.getContainsId());
	}

	@Test
	public void nullIdFieldsComeBackAsNullFromGetters() throws Exception {
		// Empty-string columns → getters return null for consistency, even
		// though the underlying DB column stores an empty string.
		FareRule r = new FareRule(1,
				gtfs("adult", "", "", "", ""), null);
		assertNull(r.getRouteId());
		assertNull(r.getOriginId());
		assertNull(r.getDestinationId());
		assertNull(r.getContainsId());
	}

	@Test
	public void properRouteIdOverridesGtfsRouteId() throws Exception {
		// When properRouteId is provided, it replaces the GTFS route_id value.
		FareRule r = new FareRule(1,
				gtfs("adult", "gtfsRoute", "zoneA", "zoneB", "zoneC"),
				"parentRoute");
		assertEquals("parentRoute", r.getRouteId());
	}

	@Test
	public void properRouteIdIsUsedEvenWhenGtfsRouteIdIsNull() throws Exception {
		// null GTFS route_id → without override would be "" → getter null.
		// With override the override wins.
		FareRule r = new FareRule(1,
				gtfs("adult", "", "zoneA", "zoneB", "zoneC"),
				"parentRoute");
		assertEquals("parentRoute", r.getRouteId());
	}

	@Test
	public void equalsMatchesOnAllIdFields() throws Exception {
		FareRule a = new FareRule(1,
				gtfs("adult", "r1", "A", "B", "C"), null);
		FareRule b = new FareRule(1,
				gtfs("adult", "r1", "A", "B", "C"), null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachField() throws Exception {
		FareRule base = new FareRule(1,
				gtfs("adult", "r1", "A", "B", "C"), null);

		assertNotEquals(base, new FareRule(2,
				gtfs("adult", "r1", "A", "B", "C"), null));
		assertNotEquals(base, new FareRule(1,
				gtfs("student", "r1", "A", "B", "C"), null));
		assertNotEquals(base, new FareRule(1,
				gtfs("adult", "r2", "A", "B", "C"), null));
		assertNotEquals(base, new FareRule(1,
				gtfs("adult", "r1", "X", "B", "C"), null));
		assertNotEquals(base, new FareRule(1,
				gtfs("adult", "r1", "A", "X", "C"), null));
		assertNotEquals(base, new FareRule(1,
				gtfs("adult", "r1", "A", "B", "X"), null));
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		FareRule r = new FareRule(1,
				gtfs("adult", "r1", "A", "B", "C"), null);
		assertFalse(r.equals(null));
		assertFalse(r.equals("not a FareRule"));
	}

	@Test
	public void toStringMentionsAllFields() throws Exception {
		String s = new FareRule(1,
				gtfs("adult", "r1", "A", "B", "C"), null).toString();
		assertTrue(s.startsWith("FareRule ["));
		assertTrue(s.contains("configRev=1"));
		assertTrue(s.contains("fareId=adult"));
		assertTrue(s.contains("routeId=r1"));
		assertTrue(s.contains("originId=A"));
		assertTrue(s.contains("destinationId=B"));
		assertTrue(s.contains("containsId=C"));
	}
}
