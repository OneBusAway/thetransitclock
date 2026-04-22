package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScheduleTimeTest {

	@Test
	public void gettersReturnConstructorArgs() {
		ScheduleTime st = new ScheduleTime(100, 200);
		assertEquals(Integer.valueOf(100), st.getArrivalTime());
		assertEquals(Integer.valueOf(200), st.getDepartureTime());
	}

	@Test
	public void getTimePrefersDepartureOverArrival() {
		ScheduleTime st = new ScheduleTime(100, 200);
		assertEquals(Integer.valueOf(200), st.getTime());
	}

	@Test
	public void getTimeFallsBackToArrivalWhenDepartureIsNull() {
		ScheduleTime st = new ScheduleTime(100, null);
		assertEquals(Integer.valueOf(100), st.getTime());
	}

	@Test
	public void getTimeReturnsNullWhenBothNull() {
		ScheduleTime st = new ScheduleTime(null, null);
		assertNull(st.getTime());
	}

	@Test
	public void getTimeReturnsDepartureWhenArrivalIsNull() {
		ScheduleTime st = new ScheduleTime(null, 200);
		assertEquals(Integer.valueOf(200), st.getTime());
	}

	@Test
	public void toStringIncludesAvailableFields() {
		// Departure-only: should contain "d=" but not "a=".
		String departOnly = new ScheduleTime(null, 3723).toString();
		assertTrue(departOnly.contains("d="));
		assertTrue(!departOnly.contains("a="));

		// Arrival-only: should contain "a=" but not "d=".
		String arriveOnly = new ScheduleTime(3723, null).toString();
		assertTrue(arriveOnly.contains("a="));
		assertTrue(!arriveOnly.contains("d="));

		// Both: should contain both fields separated by a comma.
		String both = new ScheduleTime(3600, 3723).toString();
		assertTrue(both.contains("a="));
		assertTrue(both.contains("d="));
		assertTrue(both.contains(", "));
	}
}
