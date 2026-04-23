package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.PredictionDataCache;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;
import org.transitclock.ipc.data.IpcPrediction;
import org.transitclock.ipc.data.IpcPredictionsForRouteStopDest;

/**
 * Behavior tests for {@link org.transitclock.core.PredictionGeneratorDefaultImpl}
 * (reached via {@code MatchProcessor.processPredictions}). These tests verify
 * that a successful AVL match produces predictions with plausible content —
 * {@link AvlProcessorBehaviorTest} only checks that the vehicle becomes
 * {@code isPredictable()}; it never inspects the generated predictions.
 *
 * <p>Reuses the same happy-path fixture as {@link AvlProcessorBehaviorTest}:
 * block SE-08, trip 868588900, first stop 14253 at 2016-06-20 11:50 EDT.
 * If the happy-path test fails, these will too — fix that one first.
 */
public class PredictionGeneratorBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String ROUTE_SHORT_NAME = "5A";
	private static final String BLOCK_ID = "SE-08";
	private static final String TRIP_ID = "868588900";

	private static final double FIRST_STOP_LAT = 38.953562;
	private static final double FIRST_STOP_LON = -77.447485;

	/**
	 * Scheduled departure of trip 868588900 from first stop 14253:
	 * 11:55:00 America/New_York on 2016-06-20 → 15:55:00 UTC.
	 * Pin the clock 5 minutes before that so the vehicle is in a
	 * sensible "pre-departure" posture when it reports.
	 */
	private static final long HAPPY_PATH_EPOCH_MS = 1466437800000L;

	/**
	 * Stop IDs on trip 868588900 in sequence order (per
	 * {@code transitclockIntegration/src/test/resources/gtfs/5A/stop_times.txt}):
	 * 14253 (11:55), 13056 (12:03), 14078 (12:30), 15241 (12:42:50), 17201 (12:43).
	 *
	 * <p>PredictionGenerator's default horizon is 45 minutes from the AVL
	 * timestamp (see {@code transitclock.core.maxPredictionsTimeSecs}), so
	 * 15241 and 17201 fall outside the horizon for an 11:50 AVL report and
	 * intentionally receive no predictions. Tests only assert on the
	 * in-horizon stops.
	 */
	private static final String STOP_FIRST = "14253";
	private static final String STOP_MID = "14078";

	/** Keeps the AVL timestamp strictly increasing across tests in this class. */
	private static final AtomicLong nextTime = new AtomicLong(HAPPY_PATH_EPOCH_MS);

	private static AvlReport avlReport(String vehicleId, double lat, double lon, long timeMs) {
		return new AvlReport(vehicleId, timeMs, lat, lon,
				Float.NaN, Float.NaN, "test");
	}

	private static void pushHappyPathReport(String vehicleId) {
		long t = nextTime.getAndIncrement();
		CORE.setNow(t);
		AvlReport report = avlReport(vehicleId, FIRST_STOP_LAT, FIRST_STOP_LON, t);
		report.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(report);
	}

	private static List<IpcPrediction> predictionsForVehicleAtStop(
			String vehicleId, String stopId) {
		List<IpcPredictionsForRouteStopDest> forStop =
				PredictionDataCache.getInstance().getPredictions(
						ROUTE_SHORT_NAME, null, stopId);
		return forStop.stream()
				.flatMap(p -> p.getPredictionsForRouteStop().stream())
				.filter(p -> vehicleId.equals(p.getVehicleId()))
				.toList();
	}

	// ---------- Tests ----------

	@Test
	public void happyPathMatchPopulatesVehicleStatePredictions() {
		pushHappyPathReport("v-preds-vs");

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-preds-vs");
		assertThat(state.isPredictable())
				.as("pre-req: vehicle must match before predictions are expected")
				.isTrue();
		assertThat(state.getPredictions())
				.as("matched vehicle should have a non-empty predictions list on its state")
				.isNotNull()
				.isNotEmpty();
	}

	@Test
	public void happyPathMatchPopulatesPredictionDataCacheForFirstStop() {
		pushHappyPathReport("v-preds-cache-first");

		List<IpcPrediction> preds = predictionsForVehicleAtStop("v-preds-cache-first", STOP_FIRST);
		assertThat(preds)
				.as("PredictionDataCache should have at least one prediction for the vehicle at its first stop")
				.isNotEmpty();
	}

	@Test
	public void predictionTimesAreInTheFutureRelativeToAvlClock() {
		pushHappyPathReport("v-preds-future");

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-preds-future");
		long avlTime = state.getAvlReport().getTime();

		// Every prediction should be at or after the AVL report time. A
		// prediction in the past would mean PredictionGenerator produced a
		// stale ETA, which is a user-visible defect (UI would show "arriving"
		// forever). isNotEmpty() guards against an empty-list vacuous pass.
		assertThat(state.getPredictions())
				.as("predictions should not be in the past relative to the AVL report")
				.isNotEmpty()
				.allSatisfy(p -> assertThat(p.getPredictionTime())
						.isGreaterThanOrEqualTo(avlTime));
	}

	@Test
	public void predictionsCarryExpectedTripAndRouteMetadata() {
		pushHappyPathReport("v-preds-meta");

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-preds-meta");
		// All predictions for this vehicle belong to the block we assigned;
		// the vehicle hasn't progressed to the next block's trips yet, so
		// every prediction should carry trip 868588900 or a later trip in
		// block SE-08.
		assertThat(state.getPredictions())
				.as("all predictions must come from the assigned block")
				.isNotEmpty()
				.allSatisfy(p -> assertThat(p.getBlockId()).isEqualTo(BLOCK_ID));
		assertThat(state.getPredictions())
				.as("all predictions must carry our route's short name")
				.isNotEmpty()
				.allSatisfy(p -> assertThat(p.getRouteShortName()).isEqualTo(ROUTE_SHORT_NAME));
		// The vehicle id is copied onto each prediction for downstream IPC.
		assertThat(state.getPredictions())
				.isNotEmpty()
				.allSatisfy(p -> assertThat(p.getVehicleId()).isEqualTo("v-preds-meta"));
	}

	@Test
	public void predictionsCoverInHorizonStopsOfCurrentTrip() {
		pushHappyPathReport("v-preds-coverage");

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-preds-coverage");
		// Trip 868588900 has 5 scheduled stops; only the first three
		// (14253, 13056, 14078) fall inside the 45-minute prediction
		// horizon from an 11:50 EDT AVL report. Assert we got at least
		// the first and a mid-trip stop — this catches an off-by-one in
		// stop iteration that would drop the middle of the trip while
		// still emitting something for the first stop (the shape of a
		// subtle regression that a pure "is the list non-empty" check
		// would miss).
		List<String> currentTripStopIds = state.getPredictions().stream()
				.filter(p -> TRIP_ID.equals(p.getTripId()))
				.map(IpcPrediction::getStopId)
				.distinct()
				.toList();
		assertThat(currentTripStopIds)
				.as("predictions for trip %s should cover first and mid-trip stops", TRIP_ID)
				.contains(STOP_FIRST, STOP_MID);
	}

	@Test
	public void bothArrivalAndDeparturePredictionsArePresentOnActiveTrip() {
		pushHappyPathReport("v-preds-arrdep");

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-preds-arrdep");
		// PredictionGenerator emits an arrival prediction for stops the
		// vehicle will arrive at, and a departure prediction at stops where
		// there's a scheduled dwell (notably the first stop of the trip).
		// Missing either variant on the entire active trip would mean the UI
		// shows only "arriving" but not "departing" (or vice versa) — a
		// silent capability regression rather than a crash. We assert at
		// trip scope rather than per-stop because the per-stop emission
		// mix depends on which stops have scheduled departure times in
		// the GTFS, which is a different invariant than this test is
		// checking.
		boolean anyArrival = state.getPredictions().stream()
				.filter(p -> TRIP_ID.equals(p.getTripId()))
				.anyMatch(IpcPrediction::isArrival);
		boolean anyDeparture = state.getPredictions().stream()
				.filter(p -> TRIP_ID.equals(p.getTripId()))
				.anyMatch(p -> !p.isArrival());
		assertThat(anyArrival)
				.as("at least one prediction for the active trip should be an arrival")
				.isTrue();
		assertThat(anyDeparture)
				.as("at least one prediction for the active trip should be a departure")
				.isTrue();
	}
}
