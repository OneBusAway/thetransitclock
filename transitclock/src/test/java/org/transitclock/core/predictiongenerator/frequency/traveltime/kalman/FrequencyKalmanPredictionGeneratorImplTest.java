package org.transitclock.core.predictiongenerator.frequency.traveltime.kalman;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.transitclock.core.PredictionGeneratorDefaultImpl;
import org.transitclock.core.predictiongenerator.PredictionComponentElementsGenerator;
import org.transitclock.core.predictiongenerator.frequency.traveltime.average.HistoricalAveragePredictionGeneratorImpl;
import org.transitclock.core.predictiongenerator.lastvehicle.LastVehiclePredictionGeneratorImpl;

public class FrequencyKalmanPredictionGeneratorImplTest {

	@Test
	public void classHierarchy_flowsThroughHistoricalAverageAndLastVehicleToDefault() {
		KalmanPredictionGeneratorImpl gen = new KalmanPredictionGeneratorImpl();

		// Frequency Kalman is layered: Kalman → HistoricalAverage → LastVehicle
		// → PredictionGeneratorDefaultImpl. Each layer provides fallback.
		assertThat(gen)
				.isInstanceOf(HistoricalAveragePredictionGeneratorImpl.class)
				.isInstanceOf(LastVehiclePredictionGeneratorImpl.class)
				.isInstanceOf(PredictionGeneratorDefaultImpl.class)
				.isInstanceOf(PredictionComponentElementsGenerator.class);
	}

	@Test
	public void maxPredictionsTimeSecs_usesInheritedDefault() {
		// Not overridden in any of the intermediate layers, so the 45-min
		// default from PredictionGeneratorDefaultImpl still applies.
		assertThat(PredictionGeneratorDefaultImpl.getMaxPredictionsTimeSecs())
				.isEqualTo(45 * 60);
	}
}
