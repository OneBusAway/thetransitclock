package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public class MeasuredArrivalTimeTest {

	private static MeasuredArrivalTime sample() {
		return new MeasuredArrivalTime(new Date(1_700_000_000_000L),
				"stop1", "route1", "R1", "dir0", "Downtown");
	}

	@Test
	public void constructorRoundTripsViaEquals() {
		// No public getters, but equals covers every field the composite id uses.
		MeasuredArrivalTime a = sample();
		MeasuredArrivalTime b = sample();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnAnyField() {
		Date time = new Date(1_700_000_000_000L);
		MeasuredArrivalTime base = new MeasuredArrivalTime(time,
				"stop1", "route1", "R1", "dir0", "Downtown");

		assertNotEquals(base, new MeasuredArrivalTime(new Date(time.getTime() + 1),
				"stop1", "route1", "R1", "dir0", "Downtown"));
		assertNotEquals(base, new MeasuredArrivalTime(time,
				"x", "route1", "R1", "dir0", "Downtown"));
		assertNotEquals(base, new MeasuredArrivalTime(time,
				"stop1", "x", "R1", "dir0", "Downtown"));
		assertNotEquals(base, new MeasuredArrivalTime(time,
				"stop1", "route1", "x", "dir0", "Downtown"));
		assertNotEquals(base, new MeasuredArrivalTime(time,
				"stop1", "route1", "R1", "x", "Downtown"));
		assertNotEquals(base, new MeasuredArrivalTime(time,
				"stop1", "route1", "R1", "dir0", "x"));

		assertFalse(base.equals(null));
		assertFalse(base.equals("not a MeasuredArrivalTime"));
	}

	@Test
	public void toStringMentionsAllFields() {
		String s = sample().toString();
		assertTrue(s.contains("stopId=stop1"));
		assertTrue(s.contains("routeId=route1"));
		assertTrue(s.contains("routeShortName=R1"));
		assertTrue(s.contains("directionId=dir0"));
		assertTrue(s.contains("headsign=Downtown"));
		assertTrue(s.contains("time="));
	}

	@Test
	public void getUpdateSqlEmitsInsertWithAllValues() {
		String sql = sample().getUpdateSql();
		assertTrue(sql.startsWith(
				"INSERT INTO MeasuredArrivalTimes (time, stopId, routeId, "
				+ "routeShortName, directionId, headsign) VALUES("));
		assertTrue(sql.contains("'stop1'"));
		assertTrue(sql.contains("'route1'"));
		assertTrue(sql.contains("'R1'"));
		assertTrue(sql.contains("'dir0'"));
		assertTrue(sql.contains("'Downtown'"));
		assertTrue(sql.endsWith(");"));
	}

	@Test
	public void getUpdateSqlInterpolatesNullsLiterally() {
		// No escaping / no null-handling in the implementation, so null fields
		// end up as the literal string "null" between quotes.
		MeasuredArrivalTime r = new MeasuredArrivalTime(new Date(0L),
				"s", "r", null, null, null);
		String sql = r.getUpdateSql();
		assertTrue(sql.contains("'null'"));
	}
}
