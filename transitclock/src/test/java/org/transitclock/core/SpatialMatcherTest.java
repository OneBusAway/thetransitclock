package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.transitclock.core.SpatialMatcher.MatchingType;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.Block;
import org.transitclock.db.structs.Trip;

public class SpatialMatcherTest {

	@Test
	public void problemMatch_nullMatchReturnsFalse() {
		assertThat(SpatialMatcher.problemMatchDueToLackOfHeadingInfo(
				null, mock(VehicleState.class), MatchingType.STANDARD_MATCHING))
				.isFalse();
	}

	@Test
	public void problemMatch_layoverShortCircuitsToFalse() {
		SpatialMatch match = mock(SpatialMatch.class);
		when(match.isLayover()).thenReturn(true);
		Trip trip = mock(Trip.class);
		when(match.getTrip()).thenReturn(trip);

		VehicleState vs = mock(VehicleState.class);
		AvlReport avl = mock(AvlReport.class);
		when(vs.getAvlReport()).thenReturn(avl);

		assertThat(SpatialMatcher.problemMatchDueToLackOfHeadingInfo(
				match, vs, MatchingType.STANDARD_MATCHING)).isFalse();
	}

	@Test
	public void problemMatch_validHeadingShortCircuitsToFalse() {
		SpatialMatch match = mock(SpatialMatch.class);
		when(match.isLayover()).thenReturn(false);
		Trip trip = mock(Trip.class);
		when(match.getTrip()).thenReturn(trip);

		VehicleState vs = mock(VehicleState.class);
		AvlReport avl = mock(AvlReport.class);
		when(avl.getHeading()).thenReturn(180.0f); // non-NaN → heading is valid
		when(vs.getAvlReport()).thenReturn(avl);

		assertThat(SpatialMatcher.problemMatchDueToLackOfHeadingInfo(
				match, vs, MatchingType.STANDARD_MATCHING)).isFalse();
	}

	@Test
	public void problemMatch_noHeadingAndNoPreviousAvlReportIsAProblem() {
		SpatialMatch match = mock(SpatialMatch.class);
		when(match.isLayover()).thenReturn(false);
		Trip trip = mock(Trip.class);
		when(match.getTrip()).thenReturn(trip);

		VehicleState vs = mock(VehicleState.class);
		AvlReport avl = mock(AvlReport.class);
		when(avl.getHeading()).thenReturn(Float.NaN);
		when(vs.getAvlReport()).thenReturn(avl);
		// No previous AVL report far enough back → can't confirm direction.
		when(vs.getPreviousAvlReport(org.mockito.ArgumentMatchers.anyDouble()))
				.thenReturn(null);

		assertThat(SpatialMatcher.problemMatchDueToLackOfHeadingInfo(
				match, vs, MatchingType.STANDARD_MATCHING)).isTrue();
	}

	@Test
	public void getSpatialMatches_nullTripListReturnsEmpty() {
		AvlReport avl = mock(AvlReport.class);
		Block block = mock(Block.class);

		assertThat(SpatialMatcher.getSpatialMatches(avl, block, null,
				MatchingType.STANDARD_MATCHING)).isEmpty();
	}

	@Test
	public void getSpatialMatches_emptyTripListReturnsEmpty() {
		AvlReport avl = mock(AvlReport.class);
		Block block = mock(Block.class);

		assertThat(SpatialMatcher.getSpatialMatches(avl, block,
				Collections.emptyList(), MatchingType.STANDARD_MATCHING)).isEmpty();
	}

	@Test
	public void matchingType_enumValuesAreStable() {
		// Ordinals/values are part of the persisted/public API surface.
		assertThat(MatchingType.values())
				.containsExactly(MatchingType.STANDARD_MATCHING,
						MatchingType.AUTO_ASSIGNING_MATCHING);
	}
}
