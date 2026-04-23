package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.StopArrivalDepartureCacheFactory;
import org.transitclock.core.dataCache.StopArrivalDepartureCacheKey;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;
import org.transitclock.ipc.data.IpcArrivalDeparture;

/**
 * Behavior tests for {@link org.transitclock.core.ArrivalDepartureGeneratorDefaultImpl}
 * reached via {@code MatchProcessor.processArrivalDepartures}.
 *
 * <p>Drives a vehicle through two stops on block SE-08's first trip
 * (868588900) and asserts that arrival/departure records show up in
 * {@link org.transitclock.core.dataCache.StopArrivalDepartureCacheInterface}.
 * The current pipeline suite only checks {@code isPredictable()} — a
 * regression that breaks stop-traversal detection would leave the vehicle
 * predictable but silently stop logging arrivals/departures, making this a
 * distinct layer of coverage from {@link AvlProcessorBehaviorTest} and
 * {@link PredictionGeneratorBehaviorTest}.
 *
 * <p>Trip 868588900 stop_times (verified via the 5A fixture):
 * <pre>
 *   stop_sequence 1:  14253 Dulles Airport         11:55:00
 *   stop_sequence 4:  13056 Herndon-Monroe Park&amp;Ride 12:03:00
 *   stop_sequence 6:  14078 N Moore + 19th         12:29:57
 * </pre>
 * The happy-path report anchors the vehicle at stop 14253 at 11:50; the
 * advancement report places it at 13056 a few minutes after its scheduled
 * arrival there.
 */
public class ArrivalDepartureBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String BLOCK_ID = "SE-08";

	// First stop of trip 868588900.
	private static final String STOP_FIRST_ID = "14253";
	private static final double STOP_FIRST_LAT = 38.953562;
	private static final double STOP_FIRST_LON = -77.447485;

	// A later stop on the same trip (stop_sequence 4).
	private static final String STOP_ADVANCED_ID = "13056";
	private static final double STOP_ADVANCED_LAT = 38.951704;
	private static final double STOP_ADVANCED_LON = -77.383009;

	/** 2016-06-20 11:50:00 America/New_York (EDT = UTC-4) → 15:50:00 UTC.
	 *  Anchors the vehicle at the first stop 5 minutes before its scheduled
	 *  departure, matching the known-good configuration of the happy-path
	 *  test in {@link AvlProcessorBehaviorTest}. */
	private static final long AVL_AT_FIRST_STOP_EPOCH_MS = 1466437800000L;

	/** Keeps AVL timestamps strictly increasing across tests in this class. */
	private static final AtomicLong nextTime = new AtomicLong(AVL_AT_FIRST_STOP_EPOCH_MS);

	private static AvlReport avlReport(String vehicleId, double lat, double lon, long timeMs) {
		return new AvlReport(vehicleId, timeMs, lat, lon,
				Float.NaN, Float.NaN, "test");
	}

	/**
	 * Pushes two AVL reports for {@code vehicleId}: one at the first stop of
	 * trip 868588900, then one at the next timepoint ~8 minutes later. The
	 * second report creates a previous-match / current-match pair with stops
	 * traversed in between, which is the precondition for
	 * ArrivalDepartureGenerator to actually produce records (without a
	 * previous match and stopPathIndex=0 it short-circuits).
	 *
	 * @return the VehicleState after the second report was processed.
	 */
	private static VehicleState advanceVehicleFromFirstStopToNext(String vehicleId) {
		// Step 1: report at the first stop — establishes the match but
		// generates no arrival/departure records because there is no
		// previous match and stopPathIndex is 0.
		long t1 = nextTime.getAndAdd(1);
		CORE.setNow(t1);
		AvlReport first = avlReport(vehicleId, STOP_FIRST_LAT, STOP_FIRST_LON, t1);
		first.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(first);

		// Step 2: report at the advanced stop. Core's clock jumps forward so
		// that travel-time-based extrapolation is plausible; the
		// previous-match is the at-stop match from step 1.
		long t2 = t1 + (8 * 60_000L); // 8 minutes later
		nextTime.updateAndGet(cur -> Math.max(cur, t2 + 1));
		CORE.setNow(t2);
		AvlReport second = avlReport(vehicleId, STOP_ADVANCED_LAT, STOP_ADVANCED_LON, t2);
		second.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(second);

		return VehicleStateManager.getInstance().getVehicleState(vehicleId);
	}

	private static List<IpcArrivalDeparture> stopHistory(String stopId, long atEpochMs) {
		StopArrivalDepartureCacheKey key = new StopArrivalDepartureCacheKey(stopId, new Date(atEpochMs));
		List<IpcArrivalDeparture> events =
				StopArrivalDepartureCacheFactory.getInstance().getStopHistory(key);
		// Cache returns null for "no entries" (see ehcache StopArrivalDepartureCache).
		// Normalize to an empty list so callers can use a uniform filter/assert pattern.
		return events == null ? List.of() : events;
	}

	// ---------- Tests ----------

	@Test
	public void advancingVehicleRemainsPredictableWithNewerMatch() {
		// Precondition for all other assertions in this class — if the second
		// report doesn't match, no arrival/departure can be generated and the
		// rest of the suite fails for a pre-check reason rather than a real
		// regression. Run this first.
		VehicleState state = advanceVehicleFromFirstStopToNext("v-ad-pre");
		assertThat(state.isPredictable())
				.as("vehicle should remain predictable after advancing to next stop")
				.isTrue();
		assertThat(state.getPreviousMatch())
				.as("advancing produces a previous-match snapshot (required for AD generation)")
				.isNotNull();
	}

	@Test
	public void advancingVehicleLogsDepartureAtFirstStop() {
		// When the vehicle was at stop 14253 and then moved away, the
		// generator should emit a Departure at 14253 with isArrival=false.
		VehicleState state = advanceVehicleFromFirstStopToNext("v-ad-depart");
		long avlTime = state.getAvlReport().getTime();

		List<IpcArrivalDeparture> history = stopHistory(STOP_FIRST_ID, avlTime);
		assertThat(history)
				.as("StopArrivalDepartureCache should have at least one record for stop %s", STOP_FIRST_ID)
				.isNotEmpty();

		boolean hasDeparture = history.stream()
				.filter(e -> "v-ad-depart".equals(e.getVehicleId()))
				.anyMatch(e -> !e.isArrival());
		assertThat(hasDeparture)
				.as("a departure record for vehicle v-ad-depart at stop %s should exist", STOP_FIRST_ID)
				.isTrue();
	}

	@Test
	public void advancingVehicleLogsArrivalAtNextStop() {
		// Counterpart to the departure test: when the vehicle arrives at
		// 13056, the generator should emit an Arrival there.
		VehicleState state = advanceVehicleFromFirstStopToNext("v-ad-arrive");
		long avlTime = state.getAvlReport().getTime();

		List<IpcArrivalDeparture> history = stopHistory(STOP_ADVANCED_ID, avlTime);
		assertThat(history)
				.as("StopArrivalDepartureCache should have at least one record for stop %s", STOP_ADVANCED_ID)
				.isNotEmpty();

		boolean hasArrival = history.stream()
				.filter(e -> "v-ad-arrive".equals(e.getVehicleId()))
				.anyMatch(IpcArrivalDeparture::isArrival);
		assertThat(hasArrival)
				.as("an arrival record for vehicle v-ad-arrive at stop %s should exist", STOP_ADVANCED_ID)
				.isTrue();
	}

	@Test
	public void generatedRecordsCarryCorrectBlockAndTripMetadata() {
		// Pull any record generated for our vehicle out of the first stop's
		// cache and assert its block/trip/stop fields line up with what we
		// assigned. Defends against a silent regression that swaps
		// identifying fields during IpcArrivalDeparture marshalling.
		//
		// NOTE: IpcArrivalDeparture marks routeId, routeShortName, and
		// serviceId as `transient`, so they are null after the Kyro
		// serializer round-trips through ehcache. Assertions therefore only
		// cover fields that survive serialization.
		VehicleState state = advanceVehicleFromFirstStopToNext("v-ad-meta");
		long avlTime = state.getAvlReport().getTime();

		List<IpcArrivalDeparture> history = stopHistory(STOP_FIRST_ID, avlTime);
		IpcArrivalDeparture mine = history.stream()
				.filter(e -> "v-ad-meta".equals(e.getVehicleId()))
				.findFirst()
				.orElseThrow(() -> new AssertionError(
						"no arrival/departure recorded for v-ad-meta at stop " + STOP_FIRST_ID));

		assertThat(mine.getBlockId()).isEqualTo(BLOCK_ID);
		assertThat(mine.getStopId()).isEqualTo(STOP_FIRST_ID);
		assertThat(mine.getTripId()).isEqualTo("868588900");
		assertThat(mine.getDirectionId()).isEqualTo("0");
	}
}
