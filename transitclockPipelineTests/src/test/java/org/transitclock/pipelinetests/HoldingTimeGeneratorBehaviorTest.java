package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.core.holdingmethod.HoldingTimeGeneratorDefaultImpl;
import org.transitclock.db.structs.HoldingTime;
import org.transitclock.ipc.data.IpcArrivalDeparture;

/**
 * Behavior tests for {@link HoldingTimeGeneratorDefaultImpl} against a booted
 * Core. The existing unit test in {@code transitclock/src/test} only exercises
 * the private pure-math {@code calculateHoldingTime} helper via reflection;
 * the public {@code generateHoldingTime} contract — and specifically its
 * "off-by-default unless control stops are configured" safety guarantee —
 * had no coverage.
 *
 * <p>The 5A fixture does not configure {@code transitclock.holding.controlStops},
 * so every stop in the dataset is a non-control stop. Every test below takes
 * advantage of that: they assert the contract that holding times are never
 * produced for stops the operator hasn't opted in, which is the invariant a
 * deployment relies on when it hasn't turned this feature on.
 *
 * <p>If a future change makes control-stop defaulting opt-out rather than
 * opt-in, these tests will fail and force a deliberate review.
 */
public class HoldingTimeGeneratorBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	/** Any stop id in the 5A fixture that is NOT configured as a control stop.
	 *  Because the fixture doesn't configure any control stops, this is every
	 *  stop — pick one that exists so event metadata is plausible. */
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
		// A departure (isArrival=false) must never produce a holding time,
		// regardless of control-stop configuration. The entire feature is
		// gated on isArrival(); a regression that accepted departures would
		// silently produce holding times on the wrong side of stops.
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
		// The 5A fixture configures no control stops. An arrival at any stop
		// therefore hits the !isControlStop branch and must return null. This
		// test locks in the "off by default" contract: deployments that
		// haven't configured transitclock.holding.controlStops must never see
		// a holding time produced.
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
		// The HoldingTimeGenerator interface exposes the configured control
		// points so callers (UI, IPC) can render the active feature set.
		// If controlStops is unset the implementation should return an empty
		// list rather than null — unconditional callers rely on that to
		// iterate without null-guarding.
		assertThat(new HoldingTimeGeneratorDefaultImpl().getControlPointStops())
				.as("with no transitclock.holding.controlStops configured, "
						+ "getControlPointStops() must return an empty (non-null) list")
				.isNotNull()
				.isEmpty();
	}
}
