package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.TemporalDifference;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;

/**
 * Behavior tests that exercise {@link org.transitclock.core.RealTimeSchedAdhProcessor}
 * by reporting the first-stop fixture at different wall-clock offsets from the
 * schedule and asserting the sign and approximate magnitude of the resulting
 * {@link TemporalDifference}.
 *
 * <p>Fixture: trip 868588900 on block SE-08, first stop 14253 scheduled for
 * departure at 11:55:00 EDT (= epoch ms 1466438100000).
 *
 * <p>{@link TemporalDifference} convention: positive means ahead of schedule
 * (early), negative means behind schedule (late). A vehicle waiting at its
 * ORIGIN stop before scheduled departure is treated as on-time (adherence
 * ≈ 0), not early — these tests cover the origin-stop / late / on-time axis
 * only. Mid-trip-early scenarios collide with {@code allowableEarlySeconds=180}
 * without a multi-AVL warm-up; multi-vehicle magnitude comparison collides
 * with {@code exclusiveBlockAssignments=true}.
 */
public class ScheduleAdherenceBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String BLOCK_ID = "SE-08";
	private static final double FIRST_STOP_LAT = 38.953562;
	private static final double FIRST_STOP_LON = -77.447485;

	/** 2016-06-20 11:55:00 America/New_York (EDT) → 15:55:00 UTC. Scheduled
	 *  departure time of trip 868588900 from its first stop. */
	private static final long SCHEDULED_DEPARTURE_EPOCH_MS = 1466438100000L;

	private static final long TWO_MIN_MS = 2L * 60_000L;
	private static final long FIVE_MIN_MS = 5L * 60_000L;

	/** Slack for "close to" assertions. AvlProcessor's scheduled-time lookup
	 *  rounds, and the projected position on the path can differ by several
	 *  seconds from the published stop coordinates, so strict equality flakes. */
	private static final long SLACK_MS = 90_000L;

	private static final AtomicLong distinctness = new AtomicLong(0);

	private static VehicleState reportAtOriginWithOffset(String vehicleId, long offsetMs) {
		long t = SCHEDULED_DEPARTURE_EPOCH_MS + offsetMs + distinctness.getAndIncrement();
		CORE.setNow(t);
		AvlReport report = new AvlReport(
				vehicleId, t, FIRST_STOP_LAT, FIRST_STOP_LON,
				Float.NaN, Float.NaN, "test");
		report.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(report);
		return VehicleStateManager.getInstance().getVehicleState(vehicleId);
	}

	// ---------- Tests ----------

	@Test
	public void onTimeVehicleAtOriginReportsNearZeroAdherence() {
		VehicleState state = reportAtOriginWithOffset("v-sched-ontime", 0L);
		assertThat(state.isPredictable()).as("precondition: vehicle must match").isTrue();

		TemporalDifference adh = state.getRealTimeSchedAdh();
		assertThat(adh).isNotNull();
		assertThat(Math.abs(adh.getTemporalDifference()))
				.as("on-schedule report should have |adherence| below %s ms (got %s)",
						SLACK_MS, adh.getTemporalDifference())
				.isLessThanOrEqualTo((int) SLACK_MS);
	}

	@Test
	public void mildlyLateVehicleAtOriginReportsNegativeAdherence() {
		VehicleState state = reportAtOriginWithOffset("v-sched-late-2", TWO_MIN_MS);
		assertThat(state.isPredictable()).as("precondition: vehicle must match").isTrue();

		TemporalDifference adh = state.getRealTimeSchedAdh();
		assertThat(adh).isNotNull();
		assertThat(adh.getTemporalDifference())
				.as("vehicle 2 min past scheduled departure, still at origin, should be late (negative)")
				.isNegative();
		assertThat(Math.abs(adh.getTemporalDifference()))
				.as("2 min late: |adherence| should be clearly above zero-slack (got %s)",
						adh.getTemporalDifference())
				.isGreaterThan((int) SLACK_MS);
	}

	@Test
	public void lateAdherenceApproximatesDelayMagnitude() {
		VehicleState state = reportAtOriginWithOffset("v-sched-late-5", FIVE_MIN_MS);
		assertThat(state.isPredictable()).as("precondition: vehicle must match").isTrue();

		TemporalDifference adh = state.getRealTimeSchedAdh();
		assertThat(adh).isNotNull();
		long expected = -FIVE_MIN_MS;
		assertThat((long) adh.getTemporalDifference())
				.as("adherence should be approximately -5 min for a 5-min-late origin report (got %s)",
						adh.getTemporalDifference())
				.isBetween(expected - SLACK_MS, expected + SLACK_MS);
	}
}
