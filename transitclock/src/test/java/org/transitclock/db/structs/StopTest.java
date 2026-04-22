package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.transitclock.gtfs.TitleFormatter;
import org.transitclock.gtfs.gtfsStructs.GtfsStop;

/**
 * Stop's public constructor needs a GtfsStop and a TitleFormatter. GtfsStop
 * has a plain-args convenience constructor; TitleFormatter with a null regex
 * file logs an IOException and leaves its regex list empty, so processTitle()
 * becomes an identity transform when transitclock.gtfs.capitalize is false
 * (the default).
 */
public class StopTest {

	private static final double LAT = 37.7749;
	private static final double LON = -122.4194;

	private static TitleFormatter noopFormatter() {
		return new TitleFormatter(null, false);
	}

	private static Stop buildStop(String stopId, Integer stopCode, String name,
			Integer stopCodeBaseValue) {
		GtfsStop gtfsStop = new GtfsStop(stopId, stopCode, name, LAT, LON);
		return new Stop(1, gtfsStop, stopCodeBaseValue, noopFormatter());
	}

	@Test
	public void basicGettersRoundTripFromConstructor() {
		Stop s = buildStop("100", 555, "Main & 1st", null);
		assertEquals(1, s.getConfigRev());
		assertEquals("100", s.getId());
		assertEquals(Integer.valueOf(555), s.getCode());
		assertEquals("Main & 1st", s.getName());
		assertEquals(new Location(LAT, LON), s.getLoc());
	}

	@Test
	public void explicitStopCodeIsPreferredOverStopId() {
		// stop_code from GTFS wins even if stop_id is also numeric.
		Stop s = buildStop("999", 42, "Stop", 10_000);
		assertEquals(Integer.valueOf(42), s.getCode());
	}

	@Test
	public void missingStopCodeFallsBackToNumericStopId() {
		// stop_code missing → try parsing stop_id. No base value means no offset.
		Stop s = buildStop("42", null, "Stop", null);
		assertEquals(Integer.valueOf(42), s.getCode());
	}

	@Test
	public void missingStopCodeAppliesBaseValueOffset() {
		// When configured, the numeric stop_id is offset by stopCodeBaseValue.
		Stop s = buildStop("42", null, "Stop", 100_000);
		assertEquals(Integer.valueOf(100_042), s.getCode());
	}

	@Test
	public void nonNumericStopIdLeavesCodeNull() {
		// Parsing fails silently; the stopCodeBaseValue is irrelevant.
		Stop s = buildStop("Terminal-A", null, "Terminal A", 100_000);
		assertNull(s.getCode());
	}

	@Test
	public void booleanFlagsDefaultFalseWhenGtfsValueIsNull() {
		// GtfsStop plain-args ctor nulls out timepointStop/layoverStop/waitStop/hidden.
		// timepointStop and hidden are primitive booleans → default false.
		// layoverStop and waitStop are Boolean → passed through as null.
		Stop s = buildStop("1", null, "Stop", null);
		assertFalse(s.isTimepointStop());
		assertFalse(s.isHidden());
		assertNull(s.isLayoverStop());
		assertNull(s.isWaitStop());
	}

	@Test
	public void equalsMatchesOnAllIdFields() {
		Stop a = buildStop("100", 555, "Main", null);
		Stop b = buildStop("100", 555, "Main", null);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsDivergesOnStopId() {
		Stop a = buildStop("100", 555, "Main", null);
		Stop b = buildStop("101", 555, "Main", null);
		assertNotEquals(a, b);
	}

	@Test
	public void equalsDivergesOnName() {
		Stop a = buildStop("100", 555, "Main", null);
		Stop b = buildStop("100", 555, "Broadway", null);
		assertNotEquals(a, b);
	}

	@Test
	public void equalsDivergesOnConfigRev() {
		GtfsStop gtfsStop = new GtfsStop("100", 555, "Main", LAT, LON);
		Stop rev1 = new Stop(1, gtfsStop, null, noopFormatter());
		Stop rev2 = new Stop(2, gtfsStop, null, noopFormatter());
		assertNotEquals(rev1, rev2);
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() {
		Stop a = buildStop("100", 555, "Main", null);
		assertFalse(a.equals(null));
		assertFalse(a.equals("not a Stop"));
	}

	@Test
	public void toStringMentionsConfigRevAndId() {
		String s = buildStop("100", 555, "Main", null).toString();
		assertTrue(s.startsWith("Stop ["));
		assertTrue(s.contains("configRev=1"));
		assertTrue(s.contains("id=100"));
		assertTrue(s.contains("code=555"));
	}
}
