package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.transitclock.configData.CoreConfig;
import org.transitclock.db.structs.Location;
import org.transitclock.db.structs.StopPath;
import org.transitclock.db.structs.TravelTimesForStopPath;
import org.transitclock.db.structs.Trip;

public class TravelTimesTest {

	@Test
	public void getInstance_returnsSingleton() {
		TravelTimes a = TravelTimes.getInstance();
		TravelTimes b = TravelTimes.getInstance();
		assertThat(a).isNotNull().isSameAs(b);
	}

	@Test
	public void travelTimeAsTheCrowFlies_shortDistanceUsesShortSpeed() {
		float shortSpeed = CoreConfig.getShortDistanceDeadheadingSpeed();
		float cutoff = CoreConfig.getDeadheadingShortVersusLongDistance();
		double distance = cutoff / 2.0; // below the cutoff → short speed only
		int expectedMsec = (int) ((distance / shortSpeed) * 1000);

		int actual = TravelTimes.travelTimeAsTheCrowFlies(distance);

		assertThat(actual).isEqualTo(expectedMsec);
	}

	@Test
	public void travelTimeAsTheCrowFlies_longDistanceCombinesBothSpeeds() {
		float shortSpeed = CoreConfig.getShortDistanceDeadheadingSpeed();
		float longSpeed = CoreConfig.getLongDistanceDeadheadingSpeed();
		float cutoff = CoreConfig.getDeadheadingShortVersusLongDistance();
		double distance = cutoff * 3.0;

		double expectedSecs = cutoff / shortSpeed + (distance - cutoff) / longSpeed;
		int expectedMsec = (int) (expectedSecs * 1000);

		int actual = TravelTimes.travelTimeAsTheCrowFlies(distance);

		assertThat(actual).isEqualTo(expectedMsec);
	}

	@Test
	public void travelTimeAsTheCrowFlies_exactCutoffTreatsAsShort() {
		// distance == cutoff falls into the `else` branch (shortDistanceTravel=distance, long=0).
		float shortSpeed = CoreConfig.getShortDistanceDeadheadingSpeed();
		float cutoff = CoreConfig.getDeadheadingShortVersusLongDistance();
		int expectedMsec = (int) ((cutoff / shortSpeed) * 1000);

		assertThat(TravelTimes.travelTimeAsTheCrowFlies(cutoff)).isEqualTo(expectedMsec);
	}

	@Test
	public void travelTimeAsTheCrowFlies_zeroDistanceReturnsZero() {
		assertThat(TravelTimes.travelTimeAsTheCrowFlies(0)).isEqualTo(0);
	}

	@Test
	public void travelTimeFromLayoverArrivalToNewLoc_nonLayoverReturnsZero() {
		SpatialMatch match = mock(SpatialMatch.class);
		when(match.isLayover()).thenReturn(false);

		int result = TravelTimes.travelTimeFromLayoverArrivalToNewLoc(
				match, new Location(0.0, 0.0));

		assertThat(result).isEqualTo(0);
	}

	@Test
	public void travelTimeFromLayoverArrivalToNewLoc_noPreviousStopReturnsZero() {
		SpatialMatch match = mock(SpatialMatch.class);
		when(match.isLayover()).thenReturn(true);
		when(match.getMatchAtPreviousStop()).thenReturn(null);

		int result = TravelTimes.travelTimeFromLayoverArrivalToNewLoc(
				match, new Location(47.6, -122.3));

		assertThat(result).isEqualTo(0);
	}

	@Test
	public void travelTimeFromLayoverArrivalToNewLoc_computesCrowFliesFromPreviousStop() {
		// Build a previous-stop match whose stopPath ends at a known location.
		Location endOfPrevTrip = new Location(47.6000, -122.3000);
		Location newLoc = new Location(47.6100, -122.3000); // ~1.1km north

		StopPath prevStopPath = mock(StopPath.class);
		when(prevStopPath.getEndOfPathLocation()).thenReturn(endOfPrevTrip);

		SpatialMatch prevMatch = mock(SpatialMatch.class);
		when(prevMatch.getStopPath()).thenReturn(prevStopPath);

		SpatialMatch match = mock(SpatialMatch.class);
		when(match.isLayover()).thenReturn(true);
		when(match.getMatchAtPreviousStop()).thenReturn(prevMatch);

		int result = TravelTimes.travelTimeFromLayoverArrivalToNewLoc(match, newLoc);

		// Should match the pure-function crow-flies travel time for that distance.
		double distance = endOfPrevTrip.distance(newLoc);
		int expected = TravelTimes.travelTimeAsTheCrowFlies(distance);
		assertThat(result).isEqualTo(expected).isPositive();
	}

	@Test
	public void expectedTravelTimeForStopPath_returnsStopPathTravelTimeFromTrip() {
		TravelTimesForStopPath ttsp = mock(TravelTimesForStopPath.class);
		when(ttsp.getStopPathTravelTimeMsec()).thenReturn(60_000);

		Trip trip = mock(Trip.class);
		when(trip.getTravelTimesForStopPath(3)).thenReturn(ttsp);

		Indices indices = mock(Indices.class);
		when(indices.getTrip()).thenReturn(trip);
		when(indices.getStopPathIndex()).thenReturn(3);

		int result = TravelTimes.getInstance().expectedTravelTimeForStopPath(indices);

		assertThat(result).isEqualTo(60_000);
	}

	@Test
	public void expectedStopTimeForStopPath_returnsStopTimeFromTrip() {
		TravelTimesForStopPath ttsp = mock(TravelTimesForStopPath.class);
		when(ttsp.getStopTimeMsec()).thenReturn(15_000);

		Trip trip = mock(Trip.class);
		when(trip.getTravelTimesForStopPath(0)).thenReturn(ttsp);

		Indices indices = mock(Indices.class);
		when(indices.getTrip()).thenReturn(trip);
		when(indices.getStopPathIndex()).thenReturn(0);

		int result = TravelTimes.getInstance().expectedStopTimeForStopPath(indices);

		assertThat(result).isEqualTo(15_000);
	}
}
