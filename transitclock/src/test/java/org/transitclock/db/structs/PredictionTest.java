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
 * Prediction's two public value constructors both call Core.getInstance(), so
 * these tests use the protected no-arg constructor (same-package access) and
 * set fields via reflection to cover equals/hashCode and toString. All
 * "sentinel" expectations pin what the no-arg ctor sets.
 */
public class PredictionTest {

	private static void set(Prediction target, String name, Object value)
			throws Exception {
		Field f = Prediction.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	public void noArgConstructorUsesSentinelValues() {
		Prediction p = new Prediction();
		assertNull(p.getPredictionTime());
		assertNull(p.getAvlTime());
		assertNull(p.getCreationTime());
		assertNull(p.getVehicleId());
		assertNull(p.getStopId());
		assertNull(p.getTripId());
		assertNull(p.getRouteId());
		assertFalse(p.isAffectedByWaitStop());
		assertFalse(p.isArrival());
		assertFalse(p.isSchedBasedPred());
		assertEquals(-1, p.getGtfsStopSeq());
	}

	@Test
	public void twoDefaultInstancesAreEqual() {
		Prediction a = new Prediction();
		Prediction b = new Prediction();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachReflectivelySetField() throws Exception {
		String[] stringFields = {
				"vehicleId", "stopId", "tripId", "routeId" };
		for (String field : stringFields) {
			Prediction base = new Prediction();
			Prediction changed = new Prediction();
			set(changed, field, "val-" + field);
			assertNotEquals("field: " + field, base, changed);
		}

		// Boolean fields flip from false (sentinel) → true.
		String[] booleanFields = {
				"affectedByWaitStop", "isArrival", "schedBasedPred" };
		for (String field : booleanFields) {
			Prediction base = new Prediction();
			Prediction changed = new Prediction();
			set(changed, field, true);
			assertNotEquals("field: " + field, base, changed);
		}

		// Date fields.
		String[] dateFields = { "predictionTime", "avlTime", "creationTime" };
		for (String field : dateFields) {
			Prediction base = new Prediction();
			Prediction changed = new Prediction();
			set(changed, field, new Date(1L));
			assertNotEquals("field: " + field, base, changed);
		}

		// configRev is an int.
		Prediction configChanged = new Prediction();
		set(configChanged, "configRev", 7);
		assertNotEquals(new Prediction(), configChanged);
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() {
		Prediction p = new Prediction();
		assertFalse(p.equals(null));
		assertFalse(p.equals("not a Prediction"));
	}

	@Test
	public void toStringMentionsCoreFields() throws Exception {
		Prediction p = new Prediction();
		set(p, "vehicleId", "v1");
		set(p, "stopId", "s1");
		set(p, "tripId", "t1");
		set(p, "routeId", "r1");
		set(p, "isArrival", true);

		String s = p.toString();
		assertTrue(s.startsWith("Prediction ["));
		assertTrue(s.contains("vehicleId=v1"));
		assertTrue(s.contains("stopId=s1"));
		assertTrue(s.contains("tripId=t1"));
		assertTrue(s.contains("routeId=r1"));
		assertTrue(s.contains("isArrival=true"));
	}
}
