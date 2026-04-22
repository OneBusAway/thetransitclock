package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.transitclock.db.structs.Block;
import org.transitclock.db.structs.Location;
import org.transitclock.db.structs.StopPath;
import org.transitclock.db.structs.Trip;
import org.transitclock.db.structs.TripPattern;
import org.transitclock.db.structs.VectorWithHeading;

public class SpatialMatchTest {

	/**
	 * Builds a block with one Trip containing {@code numStopPaths} stop paths,
	 * each a single 200m segment, non-layover, with beforeStopDistance=50 and
	 * afterStopDistance=50.
	 */
	private static Block buildSimpleBlock(int numStopPaths,
			boolean[] layoverFlags) {
		Block block = mock(Block.class);
		Trip trip = mock(Trip.class);
		TripPattern tripPattern = mock(TripPattern.class);

		VectorWithHeading segment = new VectorWithHeading(
				new Location(0.0, 0.0), new Location(0.0, 0.002)); // ~222m east
		double segLen = segment.length();

		for (int i = 0; i < numStopPaths; i++) {
			StopPath sp = mock(StopPath.class);
			when(sp.getNumberSegments()).thenReturn(1);
			when(sp.getSegmentVector(0)).thenReturn(segment);
			when(sp.getSegmentVectors()).thenReturn(Collections.singletonList(segment));
			when(sp.getLength()).thenReturn(segLen);
			when(sp.getBeforeStopDistance()).thenReturn(50.0);
			when(sp.getAfterStopDistance()).thenReturn(50.0);
			when(sp.isLayoverStop()).thenReturn(layoverFlags != null && layoverFlags[i]);
			when(sp.getStopId()).thenReturn("stop-" + i);
			when(sp.getGtfsStopSeq()).thenReturn(i);
			when(block.getStopPath(0, i)).thenReturn(sp);
			when(block.isLayover(0, i)).thenReturn(layoverFlags != null && layoverFlags[i]);
			when(block.isWaitStop(0, i)).thenReturn(false);
			when(block.numSegments(0, i)).thenReturn(1);
			when(block.getSegmentVector(0, i, 0)).thenReturn(segment);
			when(trip.getStopPath(i)).thenReturn(sp);
			when(tripPattern.getStopPath(i)).thenReturn(sp);
		}
		when(trip.getBlock()).thenReturn(block);
		when(trip.getTripPattern()).thenReturn(tripPattern);
		when(trip.getNumberStopPaths()).thenReturn(numStopPaths);
		when(trip.getLength()).thenReturn(segLen * numStopPaths);

		when(block.getTrips()).thenReturn(Collections.singletonList(trip));
		when(block.getTrip(0)).thenReturn(trip);
		when(block.numTrips()).thenReturn(1);
		when(block.numStopPaths(0)).thenReturn(numStopPaths);
		when(block.isNoSchedule()).thenReturn(false);
		when(block.getId()).thenReturn("block-1");
		return block;
	}

	private static SpatialMatch matchAt(Block block, int stopPathIndex,
			double distanceAlongSegment) {
		return new SpatialMatch(1_700_000_000_000L, block,
				/* tripIndex */ 0, stopPathIndex,
				/* segmentIndex */ 0,
				/* distanceToSegment */ 0.0,
				distanceAlongSegment);
	}

	@Test
	public void getters_reflectConstructorArgs() {
		Block block = buildSimpleBlock(2, null);
		SpatialMatch match = matchAt(block, 0, 50.0);

		assertThat(match.getAvlTime()).isEqualTo(1_700_000_000_000L);
		assertThat(match.getBlock()).isSameAs(block);
		assertThat(match.getTripIndex()).isEqualTo(0);
		assertThat(match.getStopPathIndex()).isEqualTo(0);
		assertThat(match.getSegmentIndex()).isEqualTo(0);
		assertThat(match.getDistanceAlongSegment()).isCloseTo(50.0, within(1e-9));
		assertThat(match.getDistanceToSegment()).isCloseTo(0.0, within(1e-9));
		assertThat(match.getLocation()).isNotNull();
	}

	@Test
	public void distanceAlongStopPath_equalsDistanceAlongSegmentWhenOnFirstSegment() {
		Block block = buildSimpleBlock(2, null);
		SpatialMatch match = matchAt(block, 0, 60.0);

		assertThat(match.getDistanceAlongStopPath()).isCloseTo(60.0, within(1e-9));
	}

	@Test
	public void distanceRemainingInStopPath_isSegmentLengthMinusProgress() {
		Block block = buildSimpleBlock(2, null);
		// Grab the segment length the helper used.
		VectorWithHeading seg = block.getStopPath(0, 0).getSegmentVector(0);
		SpatialMatch match = matchAt(block, 0, 30.0);

		assertThat(match.getDistanceRemainingInStopPath())
				.isCloseTo(seg.length() - 30.0, within(1e-9));
	}

	@Test
	public void atStop_nearEndOfPathIsAtStop() {
		Block block = buildSimpleBlock(2, null);
		VectorWithHeading seg = block.getStopPath(0, 0).getSegmentVector(0);
		// 10m from end → well within the 50m beforeStopDistance.
		SpatialMatch match = matchAt(block, 0, seg.length() - 10.0);

		assertThat(match.isAtStop()).isTrue();
		assertThat(match.atEndOfPathStop()).isTrue();
		assertThat(match.getAtEndStop()).isNotNull();
		assertThat(match.getAtBeginningStop()).isNull();
	}

	@Test
	public void atStop_justAfterPreviousStopIsAtBeginningStop() {
		Block block = buildSimpleBlock(2, null);
		// 10m into stopPath 1 → within afterStopDistance from the boundary.
		SpatialMatch match = matchAt(block, 1, 10.0);

		assertThat(match.isAtStop()).isTrue();
		assertThat(match.atBeginningOfPathStop()).isTrue();
		assertThat(match.atEndOfPathStop()).isFalse();
		assertThat(match.getAtBeginningStop()).isNotNull();
		assertThat(match.getAtEndStop()).isNull();
	}

	@Test
	public void atStop_inMiddleOfPathIsNotAtStop() {
		Block block = buildSimpleBlock(2, null);
		VectorWithHeading seg = block.getStopPath(0, 0).getSegmentVector(0);
		// Midway through segment, far from both 50m boundaries.
		SpatialMatch match = matchAt(block, 0, seg.length() / 2.0);

		assertThat(match.isAtStop()).isFalse();
		assertThat(match.getAtStop()).isNull();
		assertThat(match.atEndOfPathStop()).isFalse();
		assertThat(match.atBeginningOfPathStop()).isFalse();
	}

	@Test
	public void atStop_layoverStopAlwaysCountsAsAtStop() {
		// Layover flag forces atStop regardless of how far from the stop we are.
		Block block = buildSimpleBlock(2, new boolean[] {true, false});
		SpatialMatch match = matchAt(block, 0, 5.0); // almost at the start

		assertThat(match.isAtStop()).isTrue();
		assertThat(match.atEndOfPathStop()).isTrue();
	}

	@Test
	public void isLastTripOfBlock_trueOnlyOnFinalTrip() {
		// buildSimpleBlock only has one trip, so tripIndex 0 is the last trip.
		Block block = buildSimpleBlock(1, null);
		SpatialMatch match = matchAt(block, 0, 10.0);
		assertThat(match.isLastTripOfBlock()).isTrue();
	}

	@Test
	public void isLastTripOfBlock_falseWhenMoreTripsFollow() {
		Block block = buildSimpleBlock(1, null);
		Trip firstTrip = block.getTrips().get(0);
		// Pretend block has two trips; tripIndex 0 is not the last.
		Trip secondTrip = mock(Trip.class);
		when(block.getTrips()).thenReturn(Arrays.asList(firstTrip, secondTrip));

		SpatialMatch match = matchAt(block, 0, 10.0);
		assertThat(match.isLastTripOfBlock()).isFalse();
	}

	@Test
	public void lessThan_comparesByTripStopSegmentThenDistance() {
		Block block = buildSimpleBlock(3, null);
		SpatialMatch earlier = matchAt(block, 0, 50.0);
		SpatialMatch later = matchAt(block, 1, 10.0);
		SpatialMatch same = matchAt(block, 0, 50.0);

		assertThat(earlier.lessThan(later)).isTrue();
		assertThat(later.lessThan(earlier)).isFalse();
		assertThat(earlier.lessThan(same)).isFalse(); // strict
		assertThat(earlier.lessThanOrEqualTo(same)).isTrue();
	}

	@Test
	public void lessThan_comparesByDistanceAlongSegmentWhenIndicesEqual() {
		Block block = buildSimpleBlock(2, null);
		SpatialMatch a = matchAt(block, 0, 30.0);
		SpatialMatch b = matchAt(block, 0, 60.0);

		assertThat(a.lessThan(b)).isTrue();
		assertThat(b.lessThan(a)).isFalse();
	}

	@Test
	public void numberStopsBetweenMatches_singleTripCountsStopPathsTraversed() {
		Block block = buildSimpleBlock(4, null);
		SpatialMatch m1 = matchAt(block, 1, 10.0);
		SpatialMatch m2 = matchAt(block, 3, 5.0);

		assertThat(SpatialMatch.numberStopsBetweenMatches(m1, m2)).isEqualTo(2);
	}

	@Test
	public void numberStopsBetweenMatches_crossingTripIncludesIntermediateStops() {
		// numberStopsBetweenMatches is static and only calls block.getTrip(i)
		// and getNumberStopPaths on it, so a minimal mock is enough.
		Block block = mock(Block.class);
		Trip firstTrip = mock(Trip.class);
		when(firstTrip.getNumberStopPaths()).thenReturn(5);
		Trip secondTrip = mock(Trip.class);
		when(secondTrip.getNumberStopPaths()).thenReturn(7);
		when(block.getTrip(0)).thenReturn(firstTrip);
		when(block.getTrip(1)).thenReturn(secondTrip);
		when(block.getId()).thenReturn("block-1");

		SpatialMatch m1 = mock(SpatialMatch.class);
		when(m1.getTripIndex()).thenReturn(0);
		when(m1.getStopPathIndex()).thenReturn(2);
		SpatialMatch m2 = mock(SpatialMatch.class);
		when(m2.getTripIndex()).thenReturn(1);
		when(m2.getStopPathIndex()).thenReturn(3);
		when(m2.getBlock()).thenReturn(block);

		// Formula in class: (stopIdxInLastTrip - stopIdxInFirstTrip)
		//                 + sum(numberStopPaths for intermediate trips)
		// (3 - 2) + 5 = 6.
		assertThat(SpatialMatch.numberStopsBetweenMatches(m1, m2)).isEqualTo(6);
	}

	@Test
	public void isAtStopByTripAndPath_matchesWhenAtSpecifiedStop() {
		Block block = buildSimpleBlock(2, null);
		VectorWithHeading seg = block.getStopPath(0, 0).getSegmentVector(0);
		SpatialMatch match = matchAt(block, 0, seg.length() - 10.0);

		assertThat(match.isAtStop(0, 0)).isTrue();
		assertThat(match.isAtStop(0, 1)).isFalse();
	}

	@Test
	public void isLayoverAndWaitStop_delegateToBlock() {
		Block block = buildSimpleBlock(2, null);
		when(block.isLayover(0, 1)).thenReturn(true);
		when(block.isWaitStop(0, 1)).thenReturn(true);

		SpatialMatch match = matchAt(block, 1, 50.0);

		assertThat(match.isLayover()).isTrue();
		assertThat(match.isWaitStop()).isTrue();
	}

	@Test
	public void isLayover_falseForNoScheduleBlocks() {
		// Frequency-based unscheduled blocks can't have layovers even if the
		// underlying stop path is flagged.
		Block block = buildSimpleBlock(2, new boolean[] {true, false});
		when(block.isNoSchedule()).thenReturn(true);
		// Use a non-at-stop position to avoid the layover-implies-at-stop branch.
		VectorWithHeading seg = block.getStopPath(0, 0).getSegmentVector(0);
		// Need a match away from layover to observe isLayover() — but the
		// constructor treats layovers as always-at-stop, that's fine; isLayover
		// returns false independently because isNoSchedule short-circuits.
		SpatialMatch match = new SpatialMatch(0, block, 0, 1, 0, 0.0, seg.length() / 2);

		assertThat(match.isLayover()).isFalse();
	}

	@Test
	public void getIndices_returnsIndependentIndicesInstance() {
		Block block = buildSimpleBlock(3, null);
		SpatialMatch match = matchAt(block, 1, 50.0);

		Indices a = match.getIndices();
		Indices b = match.getIndices();

		assertThat(a).isNotSameAs(b);
		assertThat(a.getTripIndex()).isEqualTo(0);
		assertThat(a.getStopPathIndex()).isEqualTo(1);
	}

	@Test
	public void getStopPath_delegatesToTrip() {
		Block block = buildSimpleBlock(2, null);
		SpatialMatch match = matchAt(block, 1, 20.0);

		assertThat(match.getStopPath()).isSameAs(block.getTrip(0).getStopPath(1));
	}

	@Test
	public void getSegmentVector_returnsLastSegmentOfCurrentStopPath() {
		// When there's only one segment per path the "last segment" is that one.
		Block block = buildSimpleBlock(2, null);
		SpatialMatch match = matchAt(block, 0, 30.0);

		assertThat(match.getSegmentVector())
				.isSameAs(block.getSegmentVector(0, 0, 0));
	}

	@Test
	public void getScheduledWaitStopTimeSecs_returnsMinusOneOnException() {
		Block block = buildSimpleBlock(2, null);
		when(block.getScheduleTime(anyInt(), anyInt()))
				.thenThrow(new RuntimeException("no schedule time"));

		SpatialMatch match = matchAt(block, 0, 50.0);

		assertThat(match.getScheduledWaitStopTimeSecs()).isEqualTo(-1);
		assertThat(match.getScheduledWaitStopTime()).isEqualTo(-1);
	}
}
