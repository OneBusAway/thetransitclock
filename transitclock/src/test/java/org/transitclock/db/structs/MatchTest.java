package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Date;

import org.hibernate.classic.Lifecycle;
import org.junit.Test;

/**
 * Match's public value constructor reads Core.getInstance() and needs a live
 * core.VehicleState with a non-null Block and TemporalMatch, so tests reach
 * the protected no-arg constructor directly (same-package access). Individual
 * fields are then set via reflection to exercise equals/hashCode and the
 * Lifecycle intern-on-load callback.
 */
public class MatchTest {

	private static void set(Match target, String name, Object value)
			throws Exception {
		Field f = Match.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	public void noArgConstructorUsesSentinelValues() {
		Match m = new Match();
		assertNull(m.getVehicleId());
		assertNull(m.getDate());
		assertEquals(-1, m.getConfigRev());
		assertNull(m.getServiceId());
		assertNull(m.getBlockId());
		assertNull(m.getTripId());
		assertEquals(-1, m.getStopPathIndex());
		assertEquals(-1, m.getSegmentIndex());
		assertTrue(Float.isNaN(m.getDistanceAlongSegment()));
		assertTrue(Float.isNaN(m.getDistanceAlongStopPath()));
		assertFalse(m.isAtStop());
	}

	@Test
	public void twoDefaultInstancesAreEqual() {
		Match a = new Match();
		Match b = new Match();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnReflectivelySetField() throws Exception {
		Match base = new Match();
		Match changed = new Match();
		set(changed, "vehicleId", "v1");
		assertNotEquals(base, changed);
	}

	@Test
	public void equalsFlipsOnAtStop() throws Exception {
		Match base = new Match();
		Match changed = new Match();
		set(changed, "atStop", true);
		assertNotEquals(base, changed);
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() {
		Match m = new Match();
		assertFalse(m.equals(null));
		assertFalse(m.equals("not a Match"));
	}

	@Test
	public void onLoadInternsStringMembers() throws Exception {
		// new String("abc").intern() returns a different reference than the
		// "abc" String literal before interning, the same after.
		Match m = new Match();
		String vId = new String("veh").intern() + "_copy";
		String tId = new String("trip").intern() + "_copy";
		String bId = new String("blk").intern() + "_copy";
		String sId = new String("svc").intern() + "_copy";

		set(m, "vehicleId", new String(vId));
		set(m, "tripId", new String(tId));
		set(m, "blockId", new String(bId));
		set(m, "serviceId", new String(sId));

		m.onLoad(null, null);

		// After onLoad the fields should equal the intern()'d version of the
		// original, which is reference-identical to vId.intern().
		assertEquals(vId, m.getVehicleId());
		assertTrue(m.getVehicleId() == vId.intern());
		assertTrue(m.getTripId() == tId.intern());
		assertTrue(m.getBlockId() == bId.intern());
		assertTrue(m.getServiceId() == sId.intern());
	}

	@Test
	public void onLoadToleratesNullStrings() throws Exception {
		// All id strings null in the default no-arg → onLoad must not NPE.
		Match m = new Match();
		m.onLoad(null, null);
	}

	@Test
	public void lifecycleCallbacksReturnNoVeto() throws Exception {
		Match m = new Match();
		assertEquals(Lifecycle.NO_VETO, m.onSave(null));
		assertEquals(Lifecycle.NO_VETO, m.onUpdate(null));
		assertEquals(Lifecycle.NO_VETO, m.onDelete(null));
	}

	@Test
	public void getTimeReflectsAvlTime() throws Exception {
		Match m = new Match();
		Date avl = new Date(1_700_000_000_000L);
		set(m, "avlTime", avl);
		assertEquals(avl, m.getDate());
		assertEquals(avl.getTime(), m.getTime());
	}

	@Test
	public void toStringMentionsCoreIdentityFields() throws Exception {
		Match m = new Match();
		set(m, "vehicleId", "v1");
		set(m, "tripId", "t1");
		set(m, "atStop", true);
		String s = m.toString();
		assertTrue(s.startsWith("Match ["));
		assertTrue(s.contains("vehicleId=v1"));
		assertTrue(s.contains("tripId=t1"));
		assertTrue(s.contains("atStop=true"));
	}
}
