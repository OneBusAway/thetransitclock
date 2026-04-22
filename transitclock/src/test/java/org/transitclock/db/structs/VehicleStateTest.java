package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Date;

import org.junit.Test;

/**
 * VehicleState's public constructor takes an org.transitclock.core.VehicleState
 * with many live dependencies. The Hibernate no-arg constructor is private, so
 * both instantiation and field mutation happen via reflection here. Covers the
 * sentinel values, equals/hashCode, and toString.
 */
public class VehicleStateTest {

	private static VehicleState instantiate() throws Exception {
		Constructor<VehicleState> ctor =
				VehicleState.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		return ctor.newInstance();
	}

	private static void set(VehicleState target, String name, Object value)
			throws Exception {
		Field f = VehicleState.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	public void noArgConstructorUsesSentinelValues() throws Exception {
		VehicleState v = instantiate();
		assertNull(v.getVehicleId());
		assertNull(v.getAvlTime());
		assertNull(v.getBlockId());
		assertNull(v.getTripId());
		assertNull(v.getRouteId());
		assertNull(v.getRouteShortName());
		assertNull(v.getSchedAdhMsec());
		assertNull(v.getSchedAdh());
		assertNull(v.getSchedAdhWithinBounds());
		assertNull(v.getIsDelayed());
		assertNull(v.getIsLayover());
		assertNull(v.getIsPredictable());
		assertNull(v.getIsWaitStop());
		assertNull(v.getIsForSchedBasedPreds());
	}

	@Test
	public void twoDefaultInstancesAreEqual() throws Exception {
		VehicleState a = instantiate();
		VehicleState b = instantiate();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachReflectivelySetField() throws Exception {
		String[] stringFields = {
				"vehicleId", "blockId", "tripId", "routeId",
				"routeShortName", "schedAdh" };
		for (String field : stringFields) {
			VehicleState base = instantiate();
			VehicleState changed = instantiate();
			set(changed, field, "val-" + field);
			assertNotEquals("field: " + field, base, changed);
		}

		String[] boxedBoolFields = {
				"schedAdhWithinBounds", "isDelayed", "isLayover",
				"isPredictable", "isWaitStop", "isForSchedBasedPreds" };
		for (String field : boxedBoolFields) {
			VehicleState base = instantiate();
			VehicleState changed = instantiate();
			set(changed, field, Boolean.TRUE);
			assertNotEquals("field: " + field, base, changed);
		}

		VehicleState differsAvl = instantiate();
		set(differsAvl, "avlTime", new Date(1L));
		assertNotEquals(instantiate(), differsAvl);

		VehicleState differsSchedAdhMsec = instantiate();
		set(differsSchedAdhMsec, "schedAdhMsec", Integer.valueOf(5000));
		assertNotEquals(instantiate(), differsSchedAdhMsec);
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		VehicleState v = instantiate();
		assertFalse(v.equals(null));
		assertFalse(v.equals("not a VehicleState"));
	}

	@Test
	public void toStringMentionsKeyIdFields() throws Exception {
		VehicleState v = instantiate();
		set(v, "vehicleId", "v1");
		set(v, "blockId", "b1");
		set(v, "tripId", "t1");
		set(v, "routeId", "r1");

		String s = v.toString();
		assertTrue(s.startsWith("VehicleState ["));
		assertTrue(s.contains("vehicleId=v1"));
		assertTrue(s.contains("blockId=b1"));
		assertTrue(s.contains("tripId=t1"));
		assertTrue(s.contains("routeId=r1"));
	}
}
