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
 * <p>{@link AvlProcessorBehaviorTest} and {@link ArrivalDepartureBehaviorTest}
 * cover the two-AVL case. These tests go one step further to catch regressions
 * that only surface across multiple hops — e.g. {@code stopPathIndex} that
 * resets on every report, predictions that never evict stops the vehicle has
 * already passed, arrival/departure records that stop accumulating after the
 * first advancement.
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

	/** 2016-06-20 11:50:00 America/New_York (EDT = UTC-4) → 15:50:00 UTC.
	 *  Pins the vehicle at the first stop 5 minutes before its scheduled
	 *  departure — matches the known-good configuration of the other
	 *  pipeline tests. */
	private static final long AVL_AT_STOP_1_EPOCH_MS = 1466437800000L;

	/** Keeps AVL timestamps strictly increasing across tests in this class. */
	private static final AtomicLong nextTime = new AtomicLong(AVL_AT_STOP_1_EPOCH_MS);

	private static AvlReport avlReport(String vehicleId, double lat, double lon, long timeMs) {
		return new AvlReport(vehicleId, timeMs, lat, lon,
				Float.NaN, Float.NaN, "test");
	}

	/**
	 * Result of advancing a vehicle through the three scheduled stops: the
	 * stopPathIndex observed after each report, and the final VehicleState.
	 * Exposed so individual tests can make targeted assertions without having
	 * to thread the advance logic through every test.
	 */
	private static final class ProgressionSnapshot {
		final List<Integer> stopPathIndices;
		final List<String> tripIds;
		final VehicleState finalState;
		final long finalAvlTime;

		ProgressionSnapshot(List<Integer> indices, List<String> trips,
				VehicleState state, long finalAvlTime) {
			this.stopPathIndices = indices;
			this.tripIds = trips;
			this.finalState = state;
			this.finalAvlTime = finalAvlTime;
		}
	}

	/**
	 * Pushes three AVL reports — at stops 14253, 13056, 14078 — with the
	 * clock advanced to the scheduled arrival of each. Collects the match's
	 * stopPathIndex and tripId after each report so tests can assert on
	 * advancement across the sequence.
	 */
	private static ProgressionSnapshot progressThroughThreeStops(String vehicleId) {
		List<Integer> indices = new ArrayList<>();
		List<String> trips = new ArrayList<>();

		// Each test just needs its own starting timestamp — different vehicle
		// ids keep per-vehicle state isolated, and the Core clock is reset by
		// setNow() inside the loop. Only +1ms of spacing between tests is
		// needed so different tests don't use an identical AVL timestamp on
		// the off chance that matters.
		long baseline = nextTime.getAndAdd(1);
		long[] timestamps = new long[] {
				baseline,                        // 11:50 EDT (5 min before stop 1's scheduled time)
				baseline + 13L * 60_000L,        // 12:03 EDT (scheduled arrival at stop 2)
				baseline + 40L * 60_000L         // 12:30 EDT (scheduled arrival at stop 3, rounded)
		};
		double[][] locations = new double[][] {
				{ STOP_1_LAT, STOP_1_LON },
				{ STOP_2_LAT, STOP_2_LON },
				{ STOP_3_LAT, STOP_3_LON }
		};

		VehicleState state = null;
		for (int i = 0; i < timestamps.length; i++) {
			long t = timestamps[i];
			CORE.setNow(t);
			AvlReport report = avlReport(vehicleId, locations[i][0], locations[i][1], t);
			report.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
			AvlProcessor.getInstance().processAvlReport(report);

			state = VehicleStateManager.getInstance().getVehicleState(vehicleId);
			TemporalMatch match = state.getMatch();
			indices.add(match == null ? -1 : match.getStopPathIndex());
			trips.add(match == null || match.getTrip() == null ? null : match.getTrip().getId());
		}

		return new ProgressionSnapshot(indices, trips, state, timestamps[timestamps.length - 1]);
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
		// Precondition for everything else in this class: if the vehicle falls
		// off the match at any point in the sequence, the downstream
		// progression assertions are moot.
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-pred");

		assertThat(snap.finalState.isPredictable())
				.as("vehicle should remain predictable through the trip")
				.isTrue();
		// Every report should have produced a match (indices[i] != -1 means
		// getMatch() was non-null on that iteration).
		assertThat(snap.stopPathIndices)
				.as("every AVL report should produce a non-null match")
				.allSatisfy(idx -> assertThat(idx).isNotEqualTo(-1));
	}

	@Test
	public void stopPathIndexAdvancesMonotonicallyAcrossReports() {
		// A regression that resets the match on each report (or always
		// re-snaps to the trip's first stop) would leave stopPathIndex stuck
		// at 0 across the sequence. Assert it strictly increases from the
		// first report to the last.
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-spi");

		assertThat(snap.stopPathIndices.get(0))
				.as("first report at stop 14253 should land at stopPathIndex 0")
				.isEqualTo(0);
		assertThat(snap.stopPathIndices.get(2))
				.as("third report at stop 14078 should be at a later stopPathIndex than the first")
				.isGreaterThan(snap.stopPathIndices.get(0));
		// Monotonic across every pair of consecutive reports.
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
		// All three stops are on trip 868588900. A regression where the
		// matcher jumps to a different trip (e.g. a near-parallel trip on the
		// same block) mid-progression would silently break predictions for
		// the rest of the trip.
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-trip");

		assertThat(snap.tripIds)
				.as("every report should resolve to trip %s", TRIP_ID)
				.allSatisfy(tripId -> assertThat(tripId).isEqualTo(TRIP_ID));
	}

	@Test
	public void multipleArrivalDepartureRecordsAccumulateAsVehicleProgresses() {
		// Each advancement between stops can produce arrival(s) and
		// departure(s) at the stops traversed. After advancing through three
		// scheduled stops we should have at least two distinct records for
		// our vehicle across the cache — a strictly stronger assertion than
		// ArrivalDepartureBehaviorTest's "at least one record exists" after
		// a single advancement.
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-ad");

		List<IpcArrivalDeparture> all = new ArrayList<>();
		all.addAll(stopHistory(STOP_1_ID, snap.finalAvlTime));
		all.addAll(stopHistory(STOP_2_ID, snap.finalAvlTime));
		all.addAll(stopHistory(STOP_3_ID, snap.finalAvlTime));

		long myRecordCount = all.stream()
				.filter(e -> "v-prog-ad".equals(e.getVehicleId()))
				.count();
		assertThat(myRecordCount)
				.as("progression across three stops should produce at least two AD records for the vehicle")
				.isGreaterThanOrEqualTo(2);
	}

	@Test
	public void predictionsDoNotIncludeAlreadyPassedFirstStop() {
		// After the vehicle has moved past stop 14253, its own predictions
		// list on the VehicleState should no longer include a prediction for
		// stop 14253 on the active trip — the generator rebuilds predictions
		// from the current stopPathIndex forward, so a regression that left
		// stale predictions in place would be user-visible (the UI would
		// keep saying "arriving at Dulles Airport" long after the bus had
		// left).
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-prune");

		boolean anyPredictionForFirstStopOnActiveTrip = snap.finalState.getPredictions().stream()
				.filter(p -> TRIP_ID.equals(p.getTripId()))
				.map(IpcPrediction::getStopId)
				.anyMatch(STOP_1_ID::equals);
		assertThat(anyPredictionForFirstStopOnActiveTrip)
				.as("passed first stop %s should not appear in predictions for the active trip after progression", STOP_1_ID)
				.isFalse();
	}

	@Test
	public void routeShortNameStaysStableAcrossProgression() {
		// The match's Route reference is used to route predictions into
		// PredictionDataCache under the route's short name. If that reference
		// changes mid-trip (e.g. to a branch with a different short name) the
		// IPC cache entries would scatter across route buckets and API
		// consumers would see only partial coverage.
		ProgressionSnapshot snap = progressThroughThreeStops("v-prog-route");

		TemporalMatch finalMatch = snap.finalState.getMatch();
		assertThat(finalMatch).as("final match must be non-null").isNotNull();
		assertThat(finalMatch.getRoute()).isNotNull();
		assertThat(finalMatch.getRoute().getShortName())
				.as("route short name should remain %s through progression", ROUTE_SHORT_NAME)
				.isEqualTo(ROUTE_SHORT_NAME);
	}
}
