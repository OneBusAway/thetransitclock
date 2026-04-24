package org.transitclock.integration_tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.transitclock.core.dataCache.VehicleDataCache;
import org.transitclock.ipc.data.IpcVehicleComplete;
import org.transitclock.playback.PlaybackModule;
import org.transitclock.utils.Time;

/*
 * This tests Transitime successfully recovering from detours. In this AVL trace the
 * bus goes off-route and returns to the route. We test that after the bus returns
 * to the route, it is not assigned to layover state and its schedule adherence
 * is reasonable.
 */
public class RecoverFromDetourTest {

	private static final String GTFS = "src/test/resources/gtfs/A40";
	private static final String AVL = "src/test/resources/avl/A40_3151.csv";
	private static final String VEHICLE = "3151";

	@Test
	public void test() {
		PlaybackModule.runTrace(GTFS, AVL);
		IpcVehicleComplete v = VehicleDataCache.getInstance().getVehicle(VEHICLE);
		assertFalse(v.isLayover());
		int adh = Math.abs(v.getRealTimeSchedAdh().getTemporalDifference());
		assertTrue(adh < 10 * Time.MIN_IN_MSECS);
	}
	
}
