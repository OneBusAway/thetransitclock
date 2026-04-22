package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.transitclock.applications.Core;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.Block;
import org.transitclock.db.structs.Location;
import org.transitclock.db.structs.StopPath;
import org.transitclock.db.structs.Trip;
import org.transitclock.utils.Time;

public class TemporalMatcherTest {

	@Test
	public void getInstance_returnsSingleton() {
		assertThat(TemporalMatcher.getInstance())
				.isNotNull()
				.isSameAs(TemporalMatcher.getInstance());
	}

	@Test
	public void matchToLayoverStopEvenIfOffRoute_emptyPotentialTripsReturnsNull() {
		AvlReport avl = mock(AvlReport.class);

		Trip result = TemporalMatcher.getInstance()
				.matchToLayoverStopEvenIfOffRoute(avl, Collections.emptyList());

		assertThat(result).isNull();
	}

	@Test
	public void matchToLayoverStopEvenIfOffRoute_returnsTripReachableByDeadhead() {
		// AVL at 07:59 UTC, trip starts at 08:00. Distance to the trip start is
		// tiny, so as-the-crow-flies travel time will fit inside that minute.
		long avlEpoch = 1_700_000_000_000L;
		int tripStartSecs = 1_000; // arbitrary secs into day
		long msecsIntoDay = tripStartSecs * 1000L - 60_000L; // 60s before start

		AvlReport avl = mock(AvlReport.class);
		when(avl.getDate()).thenReturn(new Date(avlEpoch));
		when(avl.getLocation()).thenReturn(new Location(47.6000, -122.3000));
		when(avl.getVehicleId()).thenReturn("veh-1");

		Trip trip = mock(Trip.class);
		when(trip.getStartTime()).thenReturn(tripStartSecs);
		StopPath firstStop = mock(StopPath.class);
		when(firstStop.getEndOfPathLocation())
				.thenReturn(new Location(47.6001, -122.3000)); // ~10m away
		when(trip.getStopPath(0)).thenReturn(firstStop);
		Block block = mock(Block.class);
		when(trip.getBlock()).thenReturn(block);
		when(trip.getIndexInBlock()).thenReturn(0);
		when(trip.getId()).thenReturn("trip-1");

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Time time = mock(Time.class);
			when(time.getMsecsIntoDay(any(Date.class), anyLong())).thenReturn(msecsIntoDay);
			Core core = mock(Core.class);
			when(core.getTime()).thenReturn(time);
			coreMock.when(Core::getInstance).thenReturn(core);

			Trip result = TemporalMatcher.getInstance()
					.matchToLayoverStopEvenIfOffRoute(avl, Collections.singletonList(trip));

			assertThat(result).isSameAs(trip);
		}
	}

	@Test
	public void matchToLayoverStopEvenIfOffRoute_tripAlreadyStartedReturnsNull() {
		// If AVL time is after trip start, vehicle can't deadhead — method skips.
		long avlEpoch = 1_700_000_000_000L;
		int tripStartSecs = 1_000;
		// msecsIntoDay > tripStartTimeMsecs → the "not in future" branch.
		long msecsIntoDay = tripStartSecs * 1000L + 60_000L;

		AvlReport avl = mock(AvlReport.class);
		when(avl.getDate()).thenReturn(new Date(avlEpoch));
		when(avl.getVehicleId()).thenReturn("veh-1");

		Trip trip = mock(Trip.class);
		when(trip.getStartTime()).thenReturn(tripStartSecs);
		when(trip.getIndexInBlock()).thenReturn(0);
		when(trip.getId()).thenReturn("trip-1");

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Time time = mock(Time.class);
			when(time.getMsecsIntoDay(any(Date.class), anyLong())).thenReturn(msecsIntoDay);
			Core core = mock(Core.class);
			when(core.getTime()).thenReturn(time);
			coreMock.when(Core::getInstance).thenReturn(core);

			Trip result = TemporalMatcher.getInstance()
					.matchToLayoverStopEvenIfOffRoute(avl, Collections.singletonList(trip));

			assertThat(result).isNull();
		}
	}
}
