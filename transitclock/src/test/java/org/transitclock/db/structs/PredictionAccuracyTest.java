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
 * PredictionAccuracy's public value constructor reads
 * Core.getInstance().getDbConfig().getRouteById(routeId), so tests use the
 * protected no-arg constructor (same-package access) and set fields via
 * reflection to exercise getPredictionLengthMsecs(), onLoad() interning,
 * and equals/hashCode.
 */
public class PredictionAccuracyTest {

	private static void set(PredictionAccuracy target, String name, Object value)
			throws Exception {
		Field f = PredictionAccuracy.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	@Test
	public void noArgConstructorUsesSentinelValues() {
		PredictionAccuracy p = new PredictionAccuracy();
		assertNull(p.getRouteId());
		assertNull(p.getRouteShortName());
		assertNull(p.getDirectionId());
		assertNull(p.getStopId());
		assertNull(p.getTripId());
		assertNull(p.getArrivalDepartureTime());
		assertNull(p.getPredictedTime());
		assertNull(p.getPredictionReadTime());
		assertEquals(-1, p.getPredictionAccuracyMsecs());
		assertNull(p.getPredictionSource());
		assertNull(p.getVehicleId());
		assertNull(p.isAffectedByWaitStop());
		assertNull(p.getPredictionAlgorithm());
	}

	@Test
	public void getPredictionLengthMsecsIsPredictedMinusRead() throws Exception {
		PredictionAccuracy p = new PredictionAccuracy();
		set(p, "predictedTime", new Date(10_000L));
		set(p, "predictionReadTime", new Date(3_000L));
		assertEquals(7_000, p.getPredictionLengthMsecs());
	}

	@Test
	public void getPredictionLengthMsecsCanBeNegative() throws Exception {
		// Documented: no guard — if predictedTime < readTime, returns negative.
		PredictionAccuracy p = new PredictionAccuracy();
		set(p, "predictedTime", new Date(3_000L));
		set(p, "predictionReadTime", new Date(10_000L));
		assertEquals(-7_000, p.getPredictionLengthMsecs());
	}

	@Test
	public void onLoadInternsStringMembers() throws Exception {
		PredictionAccuracy p = new PredictionAccuracy();
		String route = new String("route").intern() + "_x";
		String shortName = new String("short").intern() + "_x";
		String dir = new String("dir").intern() + "_x";
		String stop = new String("stop").intern() + "_x";
		String trip = new String("trip").intern() + "_x";
		String src = new String("src").intern() + "_x";
		String alg = new String("alg").intern() + "_x";
		String veh = new String("veh").intern() + "_x";

		set(p, "routeId", new String(route));
		set(p, "routeShortName", new String(shortName));
		set(p, "directionId", new String(dir));
		set(p, "stopId", new String(stop));
		set(p, "tripId", new String(trip));
		set(p, "predictionSource", new String(src));
		set(p, "predictionAlgorithm", new String(alg));
		set(p, "vehicleId", new String(veh));

		p.onLoad(null, null);

		// After intern() the fields should be reference-identical to the
		// corresponding x.intern() result.
		assertTrue(p.getRouteId() == route.intern());
		assertTrue(p.getRouteShortName() == shortName.intern());
		assertTrue(p.getDirectionId() == dir.intern());
		assertTrue(p.getStopId() == stop.intern());
		assertTrue(p.getTripId() == trip.intern());
		assertTrue(p.getPredictionSource() == src.intern());
		assertTrue(p.getPredictionAlgorithm() == alg.intern());
		assertTrue(p.getVehicleId() == veh.intern());
	}

	@Test
	public void onLoadToleratesNullStrings() throws Exception {
		// All id strings null in the default no-arg → onLoad must not NPE.
		new PredictionAccuracy().onLoad(null, null);
	}

	@Test
	public void lifecycleCallbacksReturnNoVeto() throws Exception {
		PredictionAccuracy p = new PredictionAccuracy();
		assertEquals(Lifecycle.NO_VETO, p.onSave(null));
		assertEquals(Lifecycle.NO_VETO, p.onUpdate(null));
		assertEquals(Lifecycle.NO_VETO, p.onDelete(null));
	}

	@Test
	public void twoDefaultInstancesAreEqual() {
		PredictionAccuracy a = new PredictionAccuracy();
		PredictionAccuracy b = new PredictionAccuracy();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnReflectivelySetField() throws Exception {
		PredictionAccuracy base = new PredictionAccuracy();
		PredictionAccuracy changed = new PredictionAccuracy();
		set(changed, "vehicleId", "v1");
		assertNotEquals(base, changed);
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() {
		PredictionAccuracy p = new PredictionAccuracy();
		assertFalse(p.equals(null));
		assertFalse(p.equals("not a PredictionAccuracy"));
	}

	@Test
	public void toStringMentionsKeyFields() throws Exception {
		PredictionAccuracy p = new PredictionAccuracy();
		set(p, "routeId", "r1");
		set(p, "stopId", "s1");
		set(p, "predictedTime", new Date(10_000L));
		set(p, "predictionReadTime", new Date(3_000L));
		String s = p.toString();
		assertTrue(s.startsWith("PredictionAccuracy ["));
		assertTrue(s.contains("routeId=r1"));
		assertTrue(s.contains("stopId=s1"));
		// getPredictionLengthMsecs() feeds into toString.
		assertTrue(s.contains("predictionLengthMsecs=7000"));
	}
}
