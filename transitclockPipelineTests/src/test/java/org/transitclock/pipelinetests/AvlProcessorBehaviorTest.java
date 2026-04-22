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
 * <p>The 5A fixture has calendars for service_ids 8, 13, 14, 15, 16, 22 with
 * calendar_dates.txt entries scattered from 2003–2016. The historical sample
 * AVL data (not loaded here) came from 2016-05-05; that date has service, so
 * we anchor Core's clock to roughly that time-of-day on a date where we know
 * at least some service_id is active.
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

	@Before
	public void advanceClockForThisTest() {
		// Each test gets a brand-new, strictly-greater epoch so that
		// AvlProcessor's "only store newer" guard in setLastAvlReport
		// doesn't reject this test's report because a prior test left
		// a future timestamp behind.
		CORE.setNow(nextTime.getAndAdd(60_000L));
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

		// Advance the harness clock to a strictly later time and send a
		// second report. Same vehicle id.
		long secondTime = CORE.clock().get() + 30_000L;
		CORE.setNow(secondTime);
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
	public void processAvlReportDoesNotThrowForTypicalNonMatchingReport() {
		// The integration-like contract: processing should never propagate
		// exceptions to the caller, regardless of whether a match is found.
		// This catches the common "forgot to guard a null" regressions.
		AvlReport report = avlReport("v-smoke", NEAR_ROUTE_LAT, NEAR_ROUTE_LON);
		try {
			AvlProcessor.getInstance().processAvlReport(report);
		} catch (RuntimeException e) {
			logger.error("processAvlReport threw unexpectedly", e);
			throw new AssertionError(
					"processAvlReport should swallow matching errors, but threw: " + e, e);
		}
	}
}
