package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.core.holdingmethod.HoldingTimeGeneratorDefaultImpl;
import org.transitclock.db.structs.HoldingTime;
import org.transitclock.ipc.data.IpcArrivalDeparture;

/**
 * Behavior tests for {@link HoldingTimeGeneratorDefaultImpl}. The existing
 * unit test in {@code transitclock/src/test} only covers the private
 * {@code calculateHoldingTime} math helper via reflection; the public
 * {@code generateHoldingTime} contract had no coverage.
 */
public class HoldingTimeGeneratorBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String NON_CONTROL_STOP_ID = "14253";
	private static final String ROUTE_ID = "5A";
	private static final String TRIP_ID = "868588900";

	private static IpcArrivalDeparture event(String vehicleId, String stopId,
			boolean isArrival, long epochMs) {
		IpcArrivalDeparture ad = new IpcArrivalDeparture();
		ad.setVehicleId(vehicleId);
		ad.setStopId(stopId);
		ad.setRouteId(ROUTE_ID);
		ad.setTripId(TRIP_ID);
		ad.setArrival(isArrival);
		ad.setTime(new Date(epochMs));
		ad.setAvlTime(new Date(epochMs));
		return ad;
	}

	// ---------- Tests ----------

	@Test
	public void generateHoldingTime_nonArrivalEventReturnsNull() {
		IpcArrivalDeparture departure = event(
				"v-ht-depart", NON_CONTROL_STOP_ID, /*isArrival*/ false,
				1466437800000L);

		HoldingTime result = new HoldingTimeGeneratorDefaultImpl()
				.generateHoldingTime(/*vehicleState*/ null, departure);

		assertThat(result)
				.as("departure events must not produce a holding time")
				.isNull();
	}

	@Test
	public void generateHoldingTime_arrivalAtNonControlStopReturnsNull() {
		IpcArrivalDeparture arrival = event(
				"v-ht-arr-nonctrl", NON_CONTROL_STOP_ID, /*isArrival*/ true,
				1466437800000L);

		HoldingTime result = new HoldingTimeGeneratorDefaultImpl()
				.generateHoldingTime(/*vehicleState*/ null, arrival);

		assertThat(result)
				.as("arrivals at non-control stops must not produce a holding time")
				.isNull();
	}

	@Test
	public void getControlPointStops_defaultsToEmptyList() {
		// Callers of getControlPointStops iterate without null-guarding, so an
		// unset controlStops config must produce an empty list, not null.
		assertThat(new HoldingTimeGeneratorDefaultImpl().getControlPointStops())
				.as("with no transitclock.holding.controlStops configured, "
						+ "getControlPointStops() must return an empty (non-null) list")
				.isNotNull()
				.isEmpty();
	}
}
