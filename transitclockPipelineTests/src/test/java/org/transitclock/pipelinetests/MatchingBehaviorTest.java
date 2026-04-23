package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.configData.CoreConfig;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.TemporalMatch;
import org.transitclock.core.VehicleAtStopInfo;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;
import org.transitclock.utils.Geo;

/**
 * Behavior tests for the spatial + temporal matching stack
 * ({@link org.transitclock.core.SpatialMatcher} /
 * {@link org.transitclock.core.TemporalMatcher}) as observed through the
 * {@link TemporalMatch} that AvlProcessor stashes on each
 * {@link VehicleState}.
 *
 * <p>{@link AvlProcessorBehaviorTest} asserts "vehicle is predictable" — a
 * coarse signal. A regression where the matcher snaps to the wrong segment,
 * picks the wrong trip, or silently loses adherence information could leave
 * that check green while breaking predictions for real users. These tests
 * inspect the match itself: the stop path index, the distance-to-segment, the
 * at-stop indicator, the trip/block reference, and the adherence value.
 */
public class MatchingBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String BLOCK_ID = "SE-08";
	private static final String TRIP_ID = "868588900";

	private static final String FIRST_STOP_ID = "14253";
	private static final double FIRST_STOP_LAT = 38.953562;
	private static final double FIRST_STOP_LON = -77.447485;

	/** 2016-06-20 11:50:00 America/New_York (EDT = UTC-4) → 15:50:00 UTC. */
	private static final long HAPPY_PATH_EPOCH_MS = 1466437800000L;

	/** Keeps AVL timestamps strictly increasing across tests in this class. */
	private static final AtomicLong nextTime = new AtomicLong(HAPPY_PATH_EPOCH_MS);

	private static AvlReport avlReport(String vehicleId, double lat, double lon, long timeMs) {
		return new AvlReport(vehicleId, timeMs, lat, lon,
				Float.NaN, Float.NaN, "test");
	}

	private static VehicleState matchAtFirstStop(String vehicleId) {
		long t = nextTime.getAndIncrement();
		CORE.setNow(t);
		AvlReport report = avlReport(vehicleId, FIRST_STOP_LAT, FIRST_STOP_LON, t);
		report.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(report);
		return VehicleStateManager.getInstance().getVehicleState(vehicleId);
	}

	// ---------- Tests ----------

	@Test
	public void matchAtFirstStopReportsAtStopIndicator() {
		// A vehicle reporting exactly at the first stop's published lat/lon
		// should be marked as at-stop — SpatialMatcher uses a tolerance on
		// the distance along the path to decide this. Regression: if the
		// at-stop threshold drifts, predictions would stop treating a vehicle
		// as "at" the stop and would begin predicting future arrivals while
		// the vehicle is still parked there.
		VehicleState state = matchAtFirstStop("v-match-atstop");
		TemporalMatch match = state.getMatch();

		assertThat(match).as("happy-path match is a prerequisite").isNotNull();
		assertThat(match.isAtStop())
				.as("vehicle at the first stop's coordinates should be marked at-stop")
				.isTrue();

		VehicleAtStopInfo atStop = match.getAtStop();
		assertThat(atStop)
				.as("getAtStop() should expose the at-stop indices when isAtStop() is true")
				.isNotNull();
		assertThat(atStop.getStopId()).isEqualTo(FIRST_STOP_ID);
	}

	@Test
	public void matchStopPathIndexIsZeroAtBeginningOfTrip() {
		// First stop of the trip → stopPathIndex 0. Off-by-one regressions
		// in trip iteration would surface as wrong predictions for the
		// next stop (i.e. skipping the first stop altogether).
		VehicleState state = matchAtFirstStop("v-match-spi0");
		TemporalMatch match = state.getMatch();

		assertThat(match).isNotNull();
		assertThat(match.getStopPathIndex())
				.as("first stop of a trip has stopPathIndex 0")
				.isEqualTo(0);
	}

	@Test
	public void matchCarriesTripAndBlockReferences() {
		// The match's trip and block references are used by every downstream
		// stage (prediction, arrival/departure, holding). If these are null
		// the vehicle is formally predictable but nothing else works —
		// exactly the kind of bug a binary isPredictable() check misses.
		VehicleState state = matchAtFirstStop("v-match-refs");
		TemporalMatch match = state.getMatch();

		assertThat(match).isNotNull();
		assertThat(match.getTrip()).isNotNull();
		assertThat(match.getTrip().getId()).isEqualTo(TRIP_ID);
		assertThat(match.getBlock()).isNotNull();
		assertThat(match.getBlock().getId()).isEqualTo(BLOCK_ID);
		assertThat(match.getRoute()).isNotNull();
		assertThat(match.getRoute().getId()).isEqualTo("5A");
	}

	@Test
	public void matchDistanceIsWithinMaxDistanceFromSegmentConfig() {
		// Sanity: when we drop a vehicle exactly on the published stop
		// location, the distance-to-segment should be well under the
		// configured threshold. A large distance here would indicate the
		// matcher picked the wrong segment or the projection math is off.
		VehicleState state = matchAtFirstStop("v-match-dist");
		TemporalMatch match = state.getMatch();

		assertThat(match).isNotNull();
		double maxDist = CoreConfig.getMaxDistanceFromSegment();
		assertThat(match.getDistanceToSegment())
				.as("match distance should be <= configured maxDistanceFromSegment (%s m)", maxDist)
				.isLessThanOrEqualTo(maxDist);
	}

	@Test
	public void vehicleStateCarriesScheduleAdherenceAfterMatch() {
		// TemporalMatcher populates real-time schedule adherence on the
		// vehicle state. A missing adherence value means the UI can't show
		// "on time" / "late" indicators and API consumers that filter on
		// adherence would see incorrect results.
		VehicleState state = matchAtFirstStop("v-match-adh");

		assertThat(state.getRealTimeSchedAdh())
				.as("matched vehicle should have a non-null real-time schedule adherence")
				.isNotNull();
	}

	@Test
	public void matchLocationProjectsCloseToReportedLocation() {
		// The matcher projects the AVL location onto the nearest path
		// segment. The projected location should be close to where the
		// vehicle actually reported — "close" meaning within the
		// configured max-distance-from-segment, since anything farther
		// wouldn't have been accepted as a match in the first place.
		VehicleState state = matchAtFirstStop("v-match-proj");
		TemporalMatch match = state.getMatch();

		assertThat(match).isNotNull();
		assertThat(match.getLocation()).isNotNull();

		double distance = Geo.distance(match.getLocation(),
				state.getAvlReport().getLocation());
		double maxDist = CoreConfig.getMaxDistanceFromSegment();
		assertThat(distance)
				.as("projected match location should be within matcher tolerance of the AVL point")
				.isLessThanOrEqualTo(maxDist);
	}
}
