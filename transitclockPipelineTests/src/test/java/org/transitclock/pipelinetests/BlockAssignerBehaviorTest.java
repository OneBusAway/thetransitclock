package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.core.BlockAssigner;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;
import org.transitclock.db.structs.Block;

/**
 * Behavior tests for {@link BlockAssigner} against a real
 * {@link org.transitclock.gtfs.DbConfig} loaded from the WMATA 5A fixture.
 *
 * <p>BlockAssigner is 180 lines but touches every AVL report flowing through
 * AvlProcessor. A regression here would cause silent mass-unassignment of
 * vehicles — each vehicle becomes {@code !isPredictable} because its block
 * can't be resolved, so AvlProcessorBehaviorTest would still pass the
 * "match at first stop" case while everything in production reports
 * unpredictable. These tests exercise the three non-trivial lookup paths
 * (BLOCK_ID, TRIP_ID, TRIP_SHORT_NAME) plus the negative cases.
 *
 * <p>The 5A fixture has no {@code trip_short_name} column in {@code trips.txt},
 * so {@link org.transitclock.gtfs.gtfsStructs.GtfsTrip} falls back to using
 * {@code trip_id} as the short name. Hence a TRIP_SHORT_NAME assignment with
 * "868588900" resolves via the same underlying trip as a TRIP_ID assignment
 * with "868588900".
 */
public class BlockAssignerBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String KNOWN_BLOCK_ID = "SE-08";
	private static final String KNOWN_TRIP_ID = "868588900";
	private static final String KNOWN_ROUTE_ID = "5A";

	/** 2016-06-20 11:50:00 America/New_York — same anchor the other behavior
	 *  tests use to ensure the relevant service IDs are active. */
	private static final long ASSIGNMENT_TEST_EPOCH_MS = 1466437800000L;

	private static AvlReport avlWithAssignment(
			String vehicleId, String assignmentId, AssignmentType type) {
		AvlReport report = new AvlReport(vehicleId,
				ASSIGNMENT_TEST_EPOCH_MS,
				38.953562, -77.447485,
				Float.NaN, Float.NaN, "test");
		if (assignmentId != null) {
			report.setAssignment(assignmentId, type);
		}
		return report;
	}

	// ---------- Tests ----------

	@Test
	public void blockIdAssignmentResolvesToKnownBlock() {
		CORE.setNow(ASSIGNMENT_TEST_EPOCH_MS);
		AvlReport report = avlWithAssignment("v-bid", KNOWN_BLOCK_ID, AssignmentType.BLOCK_ID);

		Block block = BlockAssigner.getInstance().getBlockAssignment(report);

		assertThat(block)
				.as("BLOCK_ID assignment %s should resolve to a non-null Block on an active service day",
						KNOWN_BLOCK_ID)
				.isNotNull();
		assertThat(block.getId()).isEqualTo(KNOWN_BLOCK_ID);
	}

	@Test
	public void tripIdAssignmentResolvesToContainingBlock() {
		CORE.setNow(ASSIGNMENT_TEST_EPOCH_MS);
		AvlReport report = avlWithAssignment("v-tid", KNOWN_TRIP_ID, AssignmentType.TRIP_ID);

		Block block = BlockAssigner.getInstance().getBlockAssignment(report);

		assertThat(block)
				.as("TRIP_ID assignment should resolve to the trip's containing block")
				.isNotNull();
		// Trip 868588900 belongs to block SE-08 per trips.txt.
		assertThat(block.getId()).isEqualTo(KNOWN_BLOCK_ID);
	}

	@Test
	public void tripShortNameAssignmentResolvesToContainingBlock() {
		// In the 5A fixture trip_short_name falls back to trip_id because
		// the trips.txt has no trip_short_name column. So "868588900" is
		// a valid trip short name here.
		CORE.setNow(ASSIGNMENT_TEST_EPOCH_MS);
		AvlReport report = avlWithAssignment("v-tsn", KNOWN_TRIP_ID, AssignmentType.TRIP_SHORT_NAME);

		Block block = BlockAssigner.getInstance().getBlockAssignment(report);

		assertThat(block)
				.as("TRIP_SHORT_NAME assignment should resolve to the trip's block via short-name lookup")
				.isNotNull();
		assertThat(block.getId()).isEqualTo(KNOWN_BLOCK_ID);
	}

	@Test
	public void routeIdAssignmentDoesNotResolveToBlock() {
		// ROUTE_ID is not a block assignment — BlockAssigner.getBlockAssignment
		// must return null for it. If this ever changes, vehicles assigned to
		// a route would unexpectedly be matched to a block, changing the
		// prediction semantics.
		CORE.setNow(ASSIGNMENT_TEST_EPOCH_MS);
		AvlReport report = avlWithAssignment("v-rid", KNOWN_ROUTE_ID, AssignmentType.ROUTE_ID);

		Block block = BlockAssigner.getInstance().getBlockAssignment(report);

		assertThat(block)
				.as("ROUTE_ID assignments should never produce a block")
				.isNull();
	}

	@Test
	public void unassignedReportProducesNoBlock() {
		AvlReport report = avlWithAssignment("v-unset", null, AssignmentType.UNSET);

		Block block = BlockAssigner.getInstance().getBlockAssignment(report);

		assertThat(block)
				.as("AVL reports with no assignment should not produce a block")
				.isNull();
	}

	@Test
	public void invalidBlockIdProducesNoBlock() {
		CORE.setNow(ASSIGNMENT_TEST_EPOCH_MS);
		AvlReport report = avlWithAssignment("v-bad-bid", "DOES-NOT-EXIST", AssignmentType.BLOCK_ID);

		Block block = BlockAssigner.getInstance().getBlockAssignment(report);

		assertThat(block)
				.as("nonsense BLOCK_ID should return null rather than throwing")
				.isNull();
	}

	@Test
	public void invalidTripIdProducesNoBlock() {
		CORE.setNow(ASSIGNMENT_TEST_EPOCH_MS);
		AvlReport report = avlWithAssignment("v-bad-tid", "DOES-NOT-EXIST", AssignmentType.TRIP_ID);

		Block block = BlockAssigner.getInstance().getBlockAssignment(report);

		assertThat(block)
				.as("nonsense TRIP_ID should return null rather than throwing")
				.isNull();
	}

	@Test
	public void getRouteIdAssignmentReturnsIdOnlyForRouteIdType() {
		// Exercises the second public entry point. The BLOCK_ID variant
		// should not accidentally return the block id as if it were a
		// route id.
		AvlReport asRoute = avlWithAssignment("v-ret-route", KNOWN_ROUTE_ID, AssignmentType.ROUTE_ID);
		AvlReport asBlock = avlWithAssignment("v-ret-block", KNOWN_BLOCK_ID, AssignmentType.BLOCK_ID);

		assertThat(BlockAssigner.getInstance().getRouteIdAssignment(asRoute))
				.as("ROUTE_ID assignment should surface the id")
				.isEqualTo(KNOWN_ROUTE_ID);
		assertThat(BlockAssigner.getInstance().getRouteIdAssignment(asBlock))
				.as("BLOCK_ID assignment should not be misreported as a route id")
				.isNull();
	}
}
