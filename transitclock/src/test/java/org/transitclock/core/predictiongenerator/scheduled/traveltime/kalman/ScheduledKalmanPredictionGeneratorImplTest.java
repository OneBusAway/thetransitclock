package org.transitclock.core.predictiongenerator.scheduled.traveltime.kalman;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.transitclock.core.Indices;
import org.transitclock.core.PredictionGeneratorDefaultImpl;
import org.transitclock.core.VehicleState;
import org.transitclock.core.predictiongenerator.PredictionComponentElementsGenerator;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.TravelTimesForStopPath;
import org.transitclock.db.structs.Trip;

public class ScheduledKalmanPredictionGeneratorImplTest {

	@Test
	public void extendsDefaultImpl_soFallbackBehaviorIsPreserved() {
		KalmanPredictionGeneratorImpl gen = new KalmanPredictionGeneratorImpl();
		assertThat(gen).isInstanceOf(PredictionGeneratorDefaultImpl.class);
		assertThat(gen).isInstanceOf(PredictionComponentElementsGenerator.class);
	}

	@Test
	public void getStopTimeForPath_inheritedFromDefaultImpl() {
		TravelTimesForStopPath ttsp = mock(TravelTimesForStopPath.class);
		when(ttsp.getStopTimeMsec()).thenReturn(25_000);

		Trip trip = mock(Trip.class);
		when(trip.getTravelTimesForStopPath(1)).thenReturn(ttsp);

		Indices indices = mock(Indices.class);
		when(indices.getTrip()).thenReturn(trip);
		when(indices.getStopPathIndex()).thenReturn(1);

		KalmanPredictionGeneratorImpl gen = new KalmanPredictionGeneratorImpl();

		// Not overridden on Kalman, so delegates straight through to TravelTimes.
		long result = gen.getStopTimeForPath(indices, mock(AvlReport.class),
				mock(VehicleState.class));

		assertThat(result).isEqualTo(25_000L);
	}
}
