package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.TravelTimesForStopPath;
import org.transitclock.db.structs.Trip;

public class PredictionGeneratorDefaultImplTest {

	@Test
	public void getMaxPredictionsTimeSecs_defaultIs45Minutes() {
		// Config default: 45 * 60 = 2700 seconds.
		assertThat(PredictionGeneratorDefaultImpl.getMaxPredictionsTimeSecs())
				.isEqualTo(45 * 60);
	}

	@Test
	public void getTravelTimeForPath_returnsIndicesTravelTimeWhenStoreFlagOff() {
		// The store-predictions config defaults to false, so the method should
		// just return indices.getTravelTimeForPath() unchanged.
		Indices indices = mock(Indices.class);
		when(indices.getTravelTimeForPath()).thenReturn(12_345);

		PredictionGeneratorDefaultImpl gen = new PredictionGeneratorDefaultImpl();

		long result = gen.getTravelTimeForPath(indices, mock(AvlReport.class),
				mock(VehicleState.class));

		assertThat(result).isEqualTo(12_345L);
	}

	@Test
	public void getStopTimeForPath_delegatesToTravelTimes() {
		TravelTimesForStopPath ttsp = mock(TravelTimesForStopPath.class);
		when(ttsp.getStopTimeMsec()).thenReturn(30_000);

		Trip trip = mock(Trip.class);
		when(trip.getTravelTimesForStopPath(2)).thenReturn(ttsp);

		Indices indices = mock(Indices.class);
		when(indices.getTrip()).thenReturn(trip);
		when(indices.getStopPathIndex()).thenReturn(2);

		PredictionGeneratorDefaultImpl gen = new PredictionGeneratorDefaultImpl();

		long result = gen.getStopTimeForPath(indices, mock(AvlReport.class),
				mock(VehicleState.class));

		assertThat(result).isEqualTo(30_000L);
	}

	@Test
	public void expectedTravelTimeFromMatchToEndOfStopPath_delegatesToTravelTimes() {
		// This path hits a real SpatialMatch-backed TravelTimes call; the
		// simplest way to get signal is to mock the trip + TravelTimesForStopPath
		// the helper will look up.
		TravelTimesForStopPath ttsp = mock(TravelTimesForStopPath.class);
		when(ttsp.getNumberTravelTimeSegments()).thenReturn(1);
		when(ttsp.getTravelTimeSegmentMsec(0)).thenReturn(10_000);
		when(ttsp.getStopPathTravelTimeMsec()).thenReturn(10_000);

		Trip trip = mock(Trip.class);
		when(trip.getTravelTimesForStopPath(0)).thenReturn(ttsp);

		org.transitclock.db.structs.StopPath stopPath =
				mock(org.transitclock.db.structs.StopPath.class);
		when(stopPath.getLength()).thenReturn(100.0);

		SpatialMatch match = mock(SpatialMatch.class);
		when(match.getTrip()).thenReturn(trip);
		when(match.getStopPathIndex()).thenReturn(0);
		when(match.getStopPath()).thenReturn(stopPath);
		when(match.getDistanceAlongStopPath()).thenReturn(0.0); // at start of path

		PredictionGeneratorDefaultImpl gen = new PredictionGeneratorDefaultImpl();

		long result = gen.expectedTravelTimeFromMatchToEndOfStopPath(
				mock(AvlReport.class), match);

		// At start of path, the full 10_000 ms of travel time remains.
		assertThat(result).isEqualTo(10_000L);
	}
}
