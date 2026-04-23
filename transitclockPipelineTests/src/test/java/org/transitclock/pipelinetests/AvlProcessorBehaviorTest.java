package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.core.AvlProcessor;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;

/**
 * Behavior tests for {@link AvlProcessor} that boot a real {@link org.transitclock.applications.Core}
 * against the WMATA 5A GTFS fixture and exercise the AVL ingest path end-to-end.
 * Unlike {@code AvlProcessorTest} (in the transitclock module), which uses
 * Mockito stubs and can only check the unpredictable/no-match short-circuit,
 * these tests drive real AVL reports through the real matcher + vehicle-state
 * machine.
 *
 * <p>The 5A fixture (under {@code transitclockIntegration/src/test/resources/gtfs/5A})
 * is a mid-2010s WMATA snapshot. Its calendars are stale by today's clock, so
 * {@link CoreHarness} rewrites {@code calendar.txt} end_dates at staging
 * time; runtime service-id lookups then key off Core's simulated clock, which
 * tests pin to a date in the original fixture's {@code calendar_dates.txt}
 * range (see the {@code HAPPY_PATH_*} constants below for the specific date
 * and block used by the matching test). If the fixture is ever refreshed,
 * those constants are the places to update.
 *
 * <p>Why a single behavior test class with one harness boot: the harness boot
 * costs ~1.5s and gives us per-JVM isolation (Surefire {@code reuseForks=false}
 * forks each test class), so we amortize the cost across as many tests as
 * makes sense. Keep related assertions together in this class rather than
 * creating new classes per scenario.
 */
public class AvlProcessorBehaviorTest {

	private static final Logger logger = LoggerFactory.getLogger(AvlProcessorBehaviorTest.class);

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	/**
	 * 2016-05-05 14:45:00 UTC — within the historical date range of the
	 * WMATA 5A calendar_dates exceptions. Anchoring Core's clock here
	 * means ServiceUtils' current-service-ids lookup has something to
	 * find, rather than today's date which has no calendar coverage.
	 */
	private static final long START_TIME_EPOCH_MS = 1462459500000L;

	/**
	 * Per-JVM monotonically-advancing clock. AvlProcessor silently drops AVL
	 * reports whose timestamp is not strictly greater than the last stored
	 * report's timestamp (see AvlProcessor#setLastAvlReport). JUnit 4's default
	 * method ordering is non-deterministic, so if two tests use the same
	 * {@code setNow} value the second one to run silently fails to update
	 * {@code lastAvlReport}. Each test advances this counter to guarantee a
	 * fresh, future-relative-to-last-test timestamp.
	 */
	private static final AtomicLong nextTime = new AtomicLong(START_TIME_EPOCH_MS);

	/**
	 * A location on 5A's route near I-66/Dulles Access Rd. Good enough that a
	 * vehicle reporting here with a reasonable assignment could plausibly
	 * match, though the focus of these tests is behavior presence/absence,
	 * not pixel-perfect matching.
	 */
	private static final double NEAR_ROUTE_LAT = 38.953562;
	private static final double NEAR_ROUTE_LON = -77.447485;

	/** Far outside the 5A service area (Pacific Northwest) — guaranteed
	 *  to not spatially match any WMATA stop path. */
	private static final double FAR_OFF_ROUTE_LAT = 47.6062;
	private static final double FAR_OFF_ROUTE_LON = -122.3321;

	// --- Known-good happy-path match fixture ---
	// Trip 868588900 on block SE-08 (service 8) departs stop 14253
	// (Dulles Airport Main Terminal, 38.953562,-77.447485) at 11:55:00
	// local time heading east to L'Enfant Plaza.
	// 2016-06-20 was a Monday with service 8 active (not in calendar_dates
	// exclusions). Setting Core's clock to 11:50 EDT on that date puts us
	// 5 minutes before the scheduled departure at the first stop.
	private static final String HAPPY_PATH_BLOCK_ID = "SE-08";
	private static final double HAPPY_PATH_LAT = 38.953562;
	private static final double HAPPY_PATH_LON = -77.447485;
	// 2016-06-20 11:50:00 America/New_York (EDT = UTC-4) → 15:50:00 UTC.
	private static final long HAPPY_PATH_EPOCH_MS = 1466437800000L;

	@Before
	public void advanceClockForThisTest() {
		// Each test gets a brand-new, strictly-greater epoch so that
		// AvlProcessor's "only store newer" guard in setLastAvlReport
		// doesn't reject this test's report because a prior test left
		// a future timestamp behind.
		advanceClockBy(60_000L);
	}

	/**
	 * Advances the harness clock by {@code deltaMs} and returns the new epoch.
	 * All clock advances within tests must go through this helper (or the
	 * {@code @Before} hook) so the shared {@link #nextTime} counter stays in
	 * sync with Core's clock. Calling {@code CORE.setNow} directly with an
	 * arbitrary epoch would let a later test's {@code @Before} advance into
	 * the past relative to the last mid-test jump, silently re-triggering
	 * the {@code setLastAvlReport} "only store newer" drop.
	 */
	private long advanceClockBy(long deltaMs) {
		long next = nextTime.addAndGet(deltaMs);
		CORE.setNow(next);
		return next;
	}

	/**
	 * Jumps the harness clock to a specific epoch (for tests that need a
	 * real-world date, e.g. a known active service day). Updates
	 * {@link #nextTime} to the larger of its current value and the jump
	 * target, so subsequent tests' {@code @Before} advances never rewind
	 * the clock.
	 */
	private void jumpClockTo(long epochMs) {
		CORE.setNow(epochMs);
		nextTime.updateAndGet(cur -> Math.max(cur, epochMs));
	}

	private static AvlReport avlReport(String vehicleId, double lat, double lon) {
		// Use CORE.clock() so the report's time advances with setNow(). Tests
		// that want a time offset from "now" can construct their own Date.
		AvlReport report = new AvlReport(
				vehicleId,
				CORE.clock().get(),
				lat, lon,
				Float.NaN,  // speed
				Float.NaN,  // heading
				"test");
		return report;
	}

	/**
	 * Computes an anchor epoch within the known-good SE-08 service window
	 * that is strictly greater than any timestamp prior tests already used,
	 * then pins Core's clock to it. Returns the anchor.
	 *
	 * <p>Tests that need a predictable vehicle must use this (not
	 * {@link #jumpClockTo}) because:
	 * <ul>
	 *   <li>multiple happy-path tests in the same JVM would otherwise all
	 *       set the clock to the exact same {@code HAPPY_PATH_EPOCH_MS};
	 *       the second test's AVL report would fail {@code setLastAvlReport}'s
	 *       "only store newer" guard and be silently dropped.</li>
	 *   <li>the 100-ms per-test nudge stays well inside the 5-minute
	 *       scheduled-departure window at stop 14253, so the spatial +
	 *       temporal match still succeeds.</li>
	 * </ul>
	 */
	private long pinClockToHappyPathAnchor() {
		long anchor = nextTime.updateAndGet(
				cur -> Math.max(cur, HAPPY_PATH_EPOCH_MS) + 100L);
		CORE.setNow(anchor);
		return anchor;
	}

	private static AvlReport reportAtFirstStop(String vehicleId, long epochMs,
			String assignmentId, AssignmentType assignmentType) {
		AvlReport report = new AvlReport(vehicleId, epochMs,
				HAPPY_PATH_LAT, HAPPY_PATH_LON,
				Float.NaN, Float.NaN, "test");
		report.setAssignment(assignmentId, assignmentType);
		return report;
	}

	// ---------- Tests ----------

	@Test
	public void lastAvlReportIsUpdatedAfterProcessing() {
		AvlReport report = avlReport("v-lastavl", NEAR_ROUTE_LAT, NEAR_ROUTE_LON);
		AvlProcessor.getInstance().processAvlReport(report);

		AvlReport stored = AvlProcessor.getInstance().getLastAvlReport();
		assertThat(stored)
				.as("AvlProcessor should remember the most recent report")
				.isNotNull();
		assertThat(stored.getVehicleId()).isEqualTo("v-lastavl");
	}

	@Test
	public void unassignedReportFarFromAnyRouteProducesUnpredictableVehicle() {
		// No assignment + off-route location. AvlProcessor should still
		// remember the report (so downstream timeout handling works), but
		// the vehicle state should not be predictable.
		AvlReport report = avlReport("v-offroute", FAR_OFF_ROUTE_LAT, FAR_OFF_ROUTE_LON);
		AvlProcessor.getInstance().processAvlReport(report);

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-offroute");
		// VehicleStateManager creates-on-demand, so this is not null even for
		// never-predictable vehicles.
		assertThat(state).isNotNull();
		assertThat(state.isPredictable())
				.as("off-route unassigned vehicle must not be predictable")
				.isFalse();
		// Distinguish "spatial match failed" from "short-circuited before
		// ever attempting to match" — the former is what this test asserts;
		// the latter would indicate a different kind of regression.
		assertThat(state.getMatch())
				.as("off-route unassigned vehicle must have no TemporalMatch")
				.isNull();
		// The AVL report is cached on the state regardless of predictability.
		assertThat(state.getAvlReport().getVehicleId()).isEqualTo("v-offroute");
	}

	@Test
	public void reportWithInvalidBlockAssignmentProducesUnpredictableVehicle() {
		// AssignmentType BLOCK_ID but a block id that doesn't exist in the
		// 5A dataset. AvlProcessor should not crash; vehicle should not be
		// predictable; reported location should still be cached.
		AvlReport report = avlReport("v-badblock", NEAR_ROUTE_LAT, NEAR_ROUTE_LON);
		report.setAssignment("DOES-NOT-EXIST", AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(report);

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-badblock");
		assertThat(state).isNotNull();
		assertThat(state.isPredictable())
				.as("invalid-block-assignment vehicle must not be predictable")
				.isFalse();
	}

	@Test
	public void processingTwoReportsForSameVehicleUpdatesTheSameState() {
		// Stability check: if we push two AVL reports for the same vehicle,
		// VehicleStateManager returns the same object with the most recent
		// AvlReport.
		AvlReport first = avlReport("v-sequence", NEAR_ROUTE_LAT, NEAR_ROUTE_LON);
		AvlProcessor.getInstance().processAvlReport(first);
		VehicleState afterFirst = VehicleStateManager.getInstance().getVehicleState("v-sequence");

		// Advance the harness clock to a strictly later time via the shared
		// counter — advanceClockBy keeps nextTime in sync with Core's clock
		// so a later test's @Before hook can't accidentally jump backward.
		long secondTime = advanceClockBy(30_000L);
		AvlReport second = avlReport("v-sequence",
				NEAR_ROUTE_LAT + 0.0001, NEAR_ROUTE_LON + 0.0001);
		AvlProcessor.getInstance().processAvlReport(second);
		VehicleState afterSecond = VehicleStateManager.getInstance().getVehicleState("v-sequence");

		assertThat(afterSecond)
				.as("same vehicle id should map to the same VehicleState object")
				.isSameAs(afterFirst);
		assertThat(afterSecond.getAvlReport().getTime())
				.as("VehicleState should carry the more recent report's time")
				.isEqualTo(secondTime);
	}

	@Test
	public void reportAtFirstStopOfActiveBlockProducesPredictableVehicle() {
		// Happy path: vehicle reports at the known first stop of a real
		// block on a date when that block's service runs, with the block
		// id as an explicit assignment. Expect the vehicle to become
		// predictable (spatial + temporal match succeeded).
		//
		// This is the single test in this suite that exercises the full
		// predictable branch of AvlProcessor#lowLevelProcessAvlReport. If
		// it starts failing, the matching pipeline has regressed and the
		// rest of the suite — all of which asserts !isPredictable — would
		// silently approve broken code.
		long when = pinClockToHappyPathAnchor();
		AvlReport report = reportAtFirstStop("v-happy", when,
				HAPPY_PATH_BLOCK_ID, AssignmentType.BLOCK_ID);

		AvlProcessor.getInstance().processAvlReport(report);

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-happy");
		assertThat(state).isNotNull();
		assertThat(state.isPredictable())
				.as("vehicle at first stop of active block SE-08 should be predictable")
				.isTrue();
		assertThat(state.getMatch())
				.as("predictable vehicle must have a concrete TemporalMatch")
				.isNotNull();
		assertThat(state.getAssignmentId()).isEqualTo(HAPPY_PATH_BLOCK_ID);
	}

	// Trip 868588900 belongs to block SE-08. A TRIP_ID assignment should
	// resolve (via BlockAssigner) to the same block and produce a
	// predictable vehicle, exercising the TRIP_ID branch of the assignment
	// lookup that the existing BLOCK_ID happy path does not touch.
	@Test
	public void reportWithTripIdAssignmentProducesPredictableVehicle() {
		long when = pinClockToHappyPathAnchor();
		AvlReport report = reportAtFirstStop("v-happy-tid", when,
				"868588900", AssignmentType.TRIP_ID);

		AvlProcessor.getInstance().processAvlReport(report);

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-happy-tid");
		assertThat(state).isNotNull();
		assertThat(state.isPredictable())
				.as("vehicle with TRIP_ID assignment on an active block should be predictable")
				.isTrue();
		assertThat(state.getMatch())
				.as("TRIP_ID-predictable vehicle must have a concrete TemporalMatch")
				.isNotNull();
		// The assignment-id on the state carries through as the block id
		// once BlockAssigner resolves the trip, not the raw TRIP_ID.
		assertThat(state.getAssignmentId()).isEqualTo(HAPPY_PATH_BLOCK_ID);
	}

	// cacheAvlReportWithoutProcessing is a public side-door used to keep
	// map animation smooth when AVL arrives faster than the matcher can
	// keep up: it updates the cached VehicleState's AvlReport but skips
	// all matching. An unpredictable vehicle stays unpredictable; a
	// predictable vehicle keeps its prior match unchanged.
	@Test
	public void cacheAvlReportWithoutProcessingUpdatesStateButDoesNotMatch() {
		long when = advanceClockBy(1_000L);
		AvlReport report = avlReport("v-cache-only", NEAR_ROUTE_LAT, NEAR_ROUTE_LON);

		AvlProcessor.getInstance().cacheAvlReportWithoutProcessing(report);

		VehicleState state = VehicleStateManager.getInstance().getVehicleState("v-cache-only");
		assertThat(state).isNotNull();
		assertThat(state.getAvlReport())
				.as("cached report should be attached to the vehicle state")
				.isNotNull();
		assertThat(state.getAvlReport().getVehicleId()).isEqualTo("v-cache-only");
		assertThat(state.getAvlReport().getTime()).isEqualTo(when);
		assertThat(state.isPredictable())
				.as("cacheAvlReportWithoutProcessing must never attempt matching")
				.isFalse();
		assertThat(state.getMatch())
				.as("no match should be produced when skipping processing")
				.isNull();
	}

	// makeVehicleUnpredictable must clear the TemporalMatch and flip
	// isPredictable back to false for a previously-predictable vehicle.
	// This is the public unwind path the TimeoutHandler and auto-reassign
	// flows rely on — a regression here would silently keep stale matches
	// alive on the wire.
	@Test
	public void makeVehicleUnpredictableClearsMatchOnPredictableVehicle() {
		long when = pinClockToHappyPathAnchor();
		AvlReport report = reportAtFirstStop("v-unwind", when,
				HAPPY_PATH_BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(report);
		VehicleState beforeUnwind =
				VehicleStateManager.getInstance().getVehicleState("v-unwind");
		assertThat(beforeUnwind.isPredictable())
				.as("setup assumption: happy path must produce a predictable vehicle")
				.isTrue();

		AvlProcessor.getInstance().makeVehicleUnpredictable(
				"v-unwind",
				"behavior-test-triggered unwind",
				org.transitclock.db.structs.VehicleEvent.ASSIGNMENT_CHANGED);

		VehicleState afterUnwind =
				VehicleStateManager.getInstance().getVehicleState("v-unwind");
		assertThat(afterUnwind)
				.as("VehicleStateManager should still yield the same state object")
				.isSameAs(beforeUnwind);
		assertThat(afterUnwind.isPredictable())
				.as("vehicle must be unpredictable after makeVehicleUnpredictable")
				.isFalse();
		assertThat(afterUnwind.getMatch())
				.as("match must be cleared after makeVehicleUnpredictable")
				.isNull();
	}

	// Schedule-based-predictions AVL reports are synthetic — they exist
	// to produce predictions for runs with no real vehicle assigned. They
	// must NOT update lastRegularReportProcessed, because that timestamp
	// drives the "AVL feed is up" monitoring check.
	@Test
	public void schedBasedPredsReportDoesNotUpdateLastRegularReport() {
		// Establish a baseline: push a regular report through, capture the
		// resulting lastAvlReportTime.
		long baselineEpoch = advanceClockBy(1_000L);
		AvlReport baseline = avlReport("v-regular", NEAR_ROUTE_LAT, NEAR_ROUTE_LON);
		AvlProcessor.getInstance().processAvlReport(baseline);
		long baselineLast = AvlProcessor.getInstance().lastAvlReportTime();
		assertThat(baselineLast)
				.as("regular report should update lastAvlReportTime")
				.isEqualTo(baselineEpoch);

		// Now send a schedule-based-preds report at a strictly later time.
		// If setLastAvlReport wrongly accepted it, lastAvlReportTime would
		// jump forward to this test's clock; the contract says it must not.
		long schedEpoch = advanceClockBy(30_000L);
		AvlReport schedBased = new AvlReport("v-schedbased",
				schedEpoch,
				NEAR_ROUTE_LAT, NEAR_ROUTE_LON,
				Float.NaN, Float.NaN, "test");
		schedBased.setAssignment(HAPPY_PATH_BLOCK_ID,
				AssignmentType.BLOCK_FOR_SCHED_BASED_PREDS);
		AvlProcessor.getInstance().processAvlReport(schedBased);

		assertThat(AvlProcessor.getInstance().lastAvlReportTime())
				.as("schedule-based-preds report must not advance the regular-AVL clock")
				.isEqualTo(baselineLast);
	}
}
