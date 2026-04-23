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
 * (reached via {@code TemporalMatcher}) by reporting the same fixture at
 * different wall-clock offsets from the schedule and asserting the sign and
 * approximate magnitude of the resulting {@link TemporalDifference}.
 *
 * <p>{@link MatchingBehaviorTest} only asserts that
 * {@code state.getRealTimeSchedAdh()} is non-null. A regression where the
 * processor always returned zero, the wrong sign, or a value scaled by a
 * different unit would pass that check while silently breaking the UI's
 * "early / on-time / late" indicators and API filters.
 *
 * <p>Fixture: trip 868588900 on block SE-08, first stop 14253 scheduled
 * for departure at 11:55:00 EDT (= 15:55:00 UTC = epoch ms 1466438100000).
 *
 * <p>{@link TemporalDifference} convention: positive is "ahead of schedule"
 * (early), negative is "behind schedule" (late). See TemporalDifference.java.
 *
 * <p>These tests are intentionally confined to the ORIGIN-stop axis (0 ms
 * and positive offsets). Testing "early at mid-trip" would require either
 * a multi-AVL warm-up or a relaxed {@code allowableEarlySeconds} config,
 * and testing magnitude scaling across two different vehicles on the same
 * block collides with {@code exclusiveBlockAssignments=true}. Both of
 * those are worth covering eventually, but they warrant their own test
 * class with appropriate setup.
 */
public class ScheduleAdherenceBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String BLOCK_ID = "SE-08";
	private static final double FIRST_STOP_LAT = 38.953562;
	private static final double FIRST_STOP_LON = -77.447485;

	/** 2016-06-20 11:55:00 America/New_York (EDT) → 15:55:00 UTC.
	 *  This is the scheduled departure time of trip 868588900 from its
	 *  first stop. Tests offset from here to construct "on-time" and
	 *  "late" scenarios. */
	private static final long SCHEDULED_DEPARTURE_EPOCH_MS = 1466438100000L;

	private static final long TWO_MIN_MS = 2L * 60_000L;
	private static final long FIVE_MIN_MS = 5L * 60_000L;

	/** Slack for "close to zero" and "close to expected magnitude" checks.
	 *  AvlProcessor's scheduled-time lookup rounds and the projected
	 *  position on the path can differ by several seconds from the
	 *  published stop coordinates, so strict equality would flake. 90
	 *  seconds is tight enough that early / on-time / late remain
	 *  clearly distinguishable. */
	private static final long SLACK_MS = 90_000L;

	/** Keeps AVL timestamps strictly distinct across tests in this class.
	 *  The test itself controls the actual schedule offset; this counter
	 *  just prevents two tests from using an identical timestamp. */
	private static final AtomicLong distinctness = new AtomicLong(0);

	private static AvlReport avlReport(String vehicleId, double lat, double lon, long timeMs) {
		return new AvlReport(vehicleId, timeMs, lat, lon,
				Float.NaN, Float.NaN, "test");
	}

	private static VehicleState reportAtOriginWithOffset(String vehicleId, long offsetMs) {
		long t = SCHEDULED_DEPARTURE_EPOCH_MS + offsetMs + distinctness.getAndIncrement();
		CORE.setNow(t);
		AvlReport report = avlReport(vehicleId, FIRST_STOP_LAT, FIRST_STOP_LON, t);
		report.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(report);
		return VehicleStateManager.getInstance().getVehicleState(vehicleId);
	}

	// ---------- Tests ----------

	@Test
	public void onTimeVehicleAtOriginReportsNearZeroAdherence() {
		// Report at scheduled departure time. Adherence should be well
		// within slack of zero.
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
		// 2 minutes late, still at origin. Adherence should be negative
		// and its magnitude larger than our zero-slack — small enough
		// that the next test can reasonably assert the larger-delay
		// case produces a more-negative value.
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
		// Strongest assertion in the class: a 5-minute delay at the
		// origin should produce an adherence near -5 minutes
		// (-300000 ms), not some unrelated metric derived from path
		// distance only. ±SLACK_MS absorbs projection slop.
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
