package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Date;

import org.junit.Test;

/**
 * The two value constructors on HoldingTime call Core.getInstance().getDbConfig()
 * which isn't available in a unit test, so most of this test uses the public
 * no-arg (Hibernate) constructor. The behavioral methods getTimeToLeave() /
 * leaveStop() read the final holdingTime field, which the no-arg constructor
 * nulls out — we set it via reflection to exercise those paths.
 */
public class HoldingTimeTest {

	private static void setFinalDate(HoldingTime target, String fieldName, Date value)
			throws Exception {
		Field f = HoldingTime.class.getDeclaredField(fieldName);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	public void noArgConstructorUsesSentinelValues() {
		HoldingTime h = new HoldingTime();
		assertNull(h.getHoldingTime());
		assertNull(h.getCreationTime());
		assertNull(h.getVehicleId());
		assertNull(h.getStopId());
		assertNull(h.getTripId());
		assertNull(h.getRouteId());
		assertNull(h.getArrivalTime());
		assertFalse(h.isArrivalPredictionUsed());
		assertFalse(h.isArrivalUsed());
		assertFalse(h.isHasD1());
		assertEquals(-1, h.getNumberPredictionsUsed());
	}

	@Test
	public void mutableSettersRoundTrip() {
		HoldingTime h = new HoldingTime();
		h.setHasD1(true);
		h.setNumberPredictionsUsed(5);
		assertTrue(h.isHasD1());
		assertEquals(5, h.getNumberPredictionsUsed());
	}

	@Test
	public void twoDefaultInstancesAreEqual() {
		// Every id-field-relevant value is identical via the no-arg ctor.
		assertEquals(new HoldingTime(), new HoldingTime());
		assertEquals(new HoldingTime().hashCode(),
				new HoldingTime().hashCode());
	}

	@Test
	public void equalsIsReflexiveAndHandlesForeignTypes() {
		HoldingTime h = new HoldingTime();
		assertEquals(h, h);
		assertFalse(h.equals(null));
		assertFalse(h.equals("not a HoldingTime"));
	}

	@Test
	public void equalsDivergesWhenHasD1OrNumberPredictionsUsedDiffer() {
		// hasD1 and numberPredictionsUsed are NOT part of equals/hashCode —
		// only configRev, creationTime, holdingTime, the id fields,
		// arrivalTime, arrivalPredictionUsed, and arrivalUsed are.
		HoldingTime a = new HoldingTime();
		HoldingTime b = new HoldingTime();
		b.setHasD1(true);
		b.setNumberPredictionsUsed(99);
		assertEquals("hasD1/numberPredictionsUsed aren't in equals", a, b);
	}

	@Test
	public void getTimeToLeaveReturnsHoldingTimeWhenCurrentIsEarlier() throws Exception {
		HoldingTime h = new HoldingTime();
		Date hold = new Date(10_000L);
		setFinalDate(h, "holdingTime", hold);
		Date current = new Date(5_000L);
		assertEquals(hold, h.getTimeToLeave(current));
	}

	@Test
	public void getTimeToLeaveReturnsCurrentWhenCurrentIsLater() throws Exception {
		HoldingTime h = new HoldingTime();
		Date hold = new Date(10_000L);
		setFinalDate(h, "holdingTime", hold);
		Date current = new Date(20_000L);
		assertEquals(current, h.getTimeToLeave(current));
	}

	@Test
	public void getTimeToLeaveOnEqualInstantsReturnsHoldingTime() throws Exception {
		// currentTime.after(holdingTime) is strict, so equal instants fall
		// through to the "return holdingTime" branch.
		HoldingTime h = new HoldingTime();
		Date hold = new Date(10_000L);
		setFinalDate(h, "holdingTime", hold);
		assertEquals(hold, h.getTimeToLeave(new Date(10_000L)));
	}

	@Test
	public void leaveStopIsTrueOnlyWhenHoldingTimeStrictlyBeforeCurrent() throws Exception {
		HoldingTime h = new HoldingTime();
		setFinalDate(h, "holdingTime", new Date(10_000L));
		assertTrue("current after holdingTime", h.leaveStop(new Date(20_000L)));
		assertFalse("current equal to holdingTime", h.leaveStop(new Date(10_000L)));
		assertFalse("current before holdingTime", h.leaveStop(new Date(5_000L)));
	}

	@Test
	public void toStringMentionsFields() {
		HoldingTime h = new HoldingTime();
		h.setHasD1(true);
		h.setNumberPredictionsUsed(3);
		String s = h.toString();
		assertTrue(s.startsWith("HoldingTime ["));
		assertTrue(s.contains("hasD1=true"));
		assertTrue(s.contains("numberPredictionsUsed=3"));
		assertTrue(s.contains("configRev=-1"));
	}

	@Test
	public void equalsDistinguishesOnReflectivelyPopulatedIdFields() throws Exception {
		HoldingTime a = new HoldingTime();
		HoldingTime b = new HoldingTime();

		// Populate holdingTime on b — should no longer equal a.
		setFinalDate(b, "holdingTime", new Date(1L));
		assertNotEquals(a, b);
	}
}
