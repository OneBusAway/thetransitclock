package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.TemporalMatch;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.StopArrivalDepartureCacheFactory;
import org.transitclock.core.dataCache.StopArrivalDepartureCacheKey;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;
import org.transitclock.ipc.data.IpcArrivalDeparture;
import org.transitclock.ipc.data.IpcPrediction;

/**
 * Behavior tests that drive a vehicle through three stops on trip 868588900
 * (block SE-08) and assert that pipeline state advances correctly as each
 * successive AVL report is processed.
 *
 * <p>Stops used (from the 5A GTFS fixture):
 * <pre>
 *   14253 Dulles Airport         (11:55:00 EDT, 38.953562, -77.447485)
 *   13056 Herndon-Monroe         (12:03:00 EDT, 38.951704, -77.383009)
 *   14078 N Moore + 19th         (12:29:57 EDT, 38.896633, -77.071622)
 * </pre>
 */
public class VehicleProgressionBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String BLOCK_ID = "SE-08";
	private static final String TRIP_ID = "868588900";
	private static final String ROUTE_SHORT_NAME = "5A";

	private static final String STOP_1_ID = "14253";
	private static final double STOP_1_LAT = 38.953562;
	private static final double STOP_1_LON = -77.447485;

	private static final String STOP_2_ID = "13056";
	private static final double STOP_2_LAT = 38.951704;
	private static final double STOP_2_LON = -77.383009;

	private static final String STOP_3_ID = "14078";
	private static final double STOP_3_LAT = 38.896633;
	private static final double STOP_3_LON = -77.071622;

	/** 2016-06-20 11:50:00 America/New_York (EDT = UTC-4) → 15:50:00 UTC. */
	private static final long AVL_AT_STOP_1_EPOCH_MS = 1466437800000L;

	private static final AtomicLong nextTime = new AtomicLong(AVL_AT_STOP_1_EPOCH_MS);

	private static AvlReport avlReport(String vehicleId, double lat, double lon, long timeMs) {
		return new AvlReport(vehicleId, timeMs, lat, lon,
				Float.NaN, Float.NaN, "test");
	}

	/** Result of advancing a vehicle through three scheduled stops: the
	 *  stopPathIndex and tripId observed after each report. */
	private static final class ProgressionSnapshot {
		final List<Integer> stopPathIndices;
		final List<String> tripIds;

		ProgressionSnapshot(List<Integer> indices, List<String> trips) {
			this.stopPathIndices = indices;
			this.tripIds = trips;
		}
	}

	private static ProgressionSnapshot progressThroughThreeStops(String vehicleId) {
		List<Integer> indices = new ArrayList<>();
		List<String> trips = new ArrayList<>();

		// +1ms between tests; distinct vehicle ids keep per-vehicle state
		// isolated, and the Core clock is set by setNow() each iteration.
		long baseline = nextTime.getAndAdd(1);
		long[] timestamps = new long[] {
				baseline,                        // 11:50 EDT
				baseline + 13L * 60_000L,        // 12:03 EDT (scheduled arrival at stop 2)
				baseline + 40L * 60_000L         // 12:30 EDT (scheduled arrival at stop 3, rounded)
		};
		double[][] locations = new double[][] {
				{ STOP_1_LAT, STOP_1_LON },
				{ STOP_2_LAT, STOP_2_LON },
				{ STOP_3_LAT, STOP_3_LON }
		};

		for (int i = 0; i < timestamps.length; i++) {
			long t = timestamps[i];
			CORE.setNow(t);
			AvlReport report = avlReport(vehicleId, locations[i][0], locations[i][1], t);
			report.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
			AvlProcessor.getInstance().processAvlReport(report);

			VehicleState state = VehicleStateManager.getInstance().getVehicleState(vehicleId);
			TemporalMatch match = state.getMatch();
			indices.add(match == null ? -1 : match.getStopPathIndex());
			trips.add(match == null || match.getTrip() == null ? null : match.getTrip().getId());
		}

		return new ProgressionSnapshot(indices, trips);
	}

	private static VehicleState stateFor(String vehicleId) {
		return VehicleStateManager.getInstance().getVehicleState(vehicleId);
	}

	private static List<IpcArrivalDeparture> stopHistory(String stopId, long atEpochMs) {
		StopArrivalDepartureCacheKey key =
				new StopArrivalDepartureCacheKey(stopId, new Date(atEpochMs));
		List<IpcArrivalDeparture> events =
				StopArrivalDepartureCacheFactory.getInstance().getStopHistory(key);
		return events == null ? List.of() : events;
	}

	// ---------- Tests ----------

	@Test
	public void vehicleRemainsPredictableAcrossAllThreeReports() {
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-pred");

		assertThat(stateFor("v-prog-pred").isPredictable())
				.as("vehicle should remain predictable through the trip")
				.isTrue();
		assertThat(snap.stopPathIndices)
				.as("every AVL report should produce a non-null match (-1 sentinel means null)")
				.allSatisfy(idx -> assertThat(idx).isNotEqualTo(-1));
	}

	@Test
	public void stopPathIndexAdvancesMonotonicallyAcrossReports() {
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-spi");

		assertThat(snap.stopPathIndices.get(0))
				.as("first report at stop 14253 should land at stopPathIndex 0")
				.isEqualTo(0);
		assertThat(snap.stopPathIndices.get(2))
				.as("third report at stop 14078 should be at a later stopPathIndex than the first")
				.isGreaterThan(snap.stopPathIndices.get(0));
		for (int i = 1; i < snap.stopPathIndices.size(); i++) {
			int prev = snap.stopPathIndices.get(i - 1);
			int curr = snap.stopPathIndices.get(i);
			assertThat(curr)
					.as("stopPathIndex at report %s (%s) should be >= the previous (%s)", i, curr, prev)
					.isGreaterThanOrEqualTo(prev);
		}
	}

	@Test
	public void tripReferenceStaysOnSingleTripThroughProgression() {
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-trip");

		assertThat(snap.tripIds)
				.as("every report should resolve to trip %s", TRIP_ID)
				.allSatisfy(tripId -> assertThat(tripId).isEqualTo(TRIP_ID));
	}

	@Test
	public void multipleArrivalDepartureRecordsAccumulateAsVehicleProgresses() {
		progressThroughThreeStops("v-prog-ad");
		long avlTime = stateFor("v-prog-ad").getAvlReport().getTime();

		List<IpcArrivalDeparture> all = new ArrayList<>();
		all.addAll(stopHistory(STOP_1_ID, avlTime));
		all.addAll(stopHistory(STOP_2_ID, avlTime));
		all.addAll(stopHistory(STOP_3_ID, avlTime));

		long myRecordCount = all.stream()
				.filter(e -> "v-prog-ad".equals(e.getVehicleId()))
				.count();
		assertThat(myRecordCount)
				.as("progression across three stops should produce at least two AD records for the vehicle")
				.isGreaterThanOrEqualTo(2);
	}

	@Test
	public void predictionsDoNotIncludeAlreadyPassedFirstStop() {
		progressThroughThreeStops("v-prog-prune");

		boolean anyPredictionForFirstStopOnActiveTrip = stateFor("v-prog-prune").getPredictions().stream()
				.filter(p -> TRIP_ID.equals(p.getTripId()))
				.map(IpcPrediction::getStopId)
				.anyMatch(STOP_1_ID::equals);
		assertThat(anyPredictionForFirstStopOnActiveTrip)
				.as("passed first stop %s should not appear in predictions for the active trip after progression", STOP_1_ID)
				.isFalse();
	}

	@Test
	public void routeShortNameStaysStableAcrossProgression() {
		progressThroughThreeStops("v-prog-route");

		TemporalMatch finalMatch = stateFor("v-prog-route").getMatch();
		assertThat(finalMatch).as("final match must be non-null").isNotNull();
		assertThat(finalMatch.getRoute()).isNotNull();
		assertThat(finalMatch.getRoute().getShortName())
				.as("route short name should remain %s through progression", ROUTE_SHORT_NAME)
				.isEqualTo(ROUTE_SHORT_NAME);
	}
}
