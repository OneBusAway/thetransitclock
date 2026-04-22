package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.transitclock.applications.Core;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.ScheduleTime;
import org.transitclock.db.structs.Trip;
import org.transitclock.utils.Time;

public class RealTimeSchedAdhProcessorTest {

	private static final long AVL_EPOCH = 1_700_000_000_000L;

	@Test
	public void generate_returnsNullWhenNotPredictable() {
		VehicleState vs = mock(VehicleState.class);
		when(vs.isPredictable()).thenReturn(false);

		assertThat(RealTimeSchedAdhProcessor.generate(vs)).isNull();
	}

	@Test
	public void generate_atWaitStopBeforeDepartureReturnsZero() {
		long departureEpoch = AVL_EPOCH + 60_000L;
		VehicleState vs = predictableState();
		VehicleAtStopInfo stopInfo = waitStopWithDeparture(43200);
		when(vs.getMatch().getAtStop()).thenReturn(stopInfo);

		try (MockedStatic<Core> coreMock = stubCoreWithEpochTime(43200, departureEpoch)) {
			TemporalDifference result = RealTimeSchedAdhProcessor.generate(vs);

			assertThat(result).isNotNull();
			assertThat(result.getTemporalDifference()).isEqualTo(0);
		}
	}

	@Test
	public void generate_atWaitStopAfterDepartureReturnsLateness() {
		long departureEpoch = AVL_EPOCH - 30_000L;
		VehicleState vs = predictableState();
		VehicleAtStopInfo stopInfo = waitStopWithDeparture(43200);
		when(vs.getMatch().getAtStop()).thenReturn(stopInfo);

		try (MockedStatic<Core> coreMock = stubCoreWithEpochTime(43200, departureEpoch)) {
			TemporalDifference result = RealTimeSchedAdhProcessor.generate(vs);

			assertThat(result).isNotNull();
			assertThat(result.getTemporalDifference())
					.isEqualTo((int) (departureEpoch - AVL_EPOCH));
		}
	}

	@Test
	public void generate_atRegularStopWithDepartureReturnsDifference() {
		long departureEpoch = AVL_EPOCH + 45_000L;
		VehicleState vs = predictableState();
		VehicleAtStopInfo stopInfo = mock(VehicleAtStopInfo.class);
		ScheduleTime schedTime = new ScheduleTime(null, 43200);
		when(stopInfo.getScheduleTime()).thenReturn(schedTime);
		when(stopInfo.isWaitStop()).thenReturn(false);
		when(vs.getMatch().getAtStop()).thenReturn(stopInfo);

		try (MockedStatic<Core> coreMock = stubCoreWithEpochTime(43200, departureEpoch)) {
			TemporalDifference result = RealTimeSchedAdhProcessor.generate(vs);

			assertThat(result).isNotNull();
			assertThat(result.getTemporalDifference())
					.isEqualTo((int) (departureEpoch - AVL_EPOCH));
		}
	}

	@Test
	public void generate_notAtStopAndNoUpcomingScheduledStopReturnsNull() {
		VehicleState vs = predictableState();
		when(vs.getMatch().getAtStop()).thenReturn(null);
		when(vs.getMatch().getMatchAtNextStopWithScheduleTime()).thenReturn(null);

		assertThat(RealTimeSchedAdhProcessor.generate(vs)).isNull();
	}

	@Test
	public void generate_atStopButScheduleTimeNullFallsThroughToUpcomingStop() {
		// When a stopInfo exists but has no ScheduleTime, the method must
		// fall through to the "look at next stop with schedule time" branch.
		VehicleState vs = predictableState();
		VehicleAtStopInfo stopInfo = mock(VehicleAtStopInfo.class);
		when(stopInfo.getScheduleTime()).thenReturn(null);
		when(vs.getMatch().getAtStop()).thenReturn(stopInfo);
		when(vs.getMatch().getMatchAtNextStopWithScheduleTime()).thenReturn(null);

		assertThat(RealTimeSchedAdhProcessor.generate(vs)).isNull();
	}

	private VehicleState predictableState() {
		VehicleState vs = mock(VehicleState.class);
		when(vs.isPredictable()).thenReturn(true);
		when(vs.getVehicleId()).thenReturn("veh-1");

		AvlReport avl = mock(AvlReport.class);
		when(avl.getDate()).thenReturn(new Date(AVL_EPOCH));
		when(vs.getAvlReport()).thenReturn(avl);

		TemporalMatch match = mock(TemporalMatch.class);
		Trip trip = mock(Trip.class);
		when(match.getTrip()).thenReturn(trip);
		when(vs.getMatch()).thenReturn(match);
		return vs;
	}

	private static VehicleAtStopInfo waitStopWithDeparture(int scheduledDeparture) {
		VehicleAtStopInfo stopInfo = mock(VehicleAtStopInfo.class);
		when(stopInfo.getScheduleTime()).thenReturn(new ScheduleTime(null, scheduledDeparture));
		when(stopInfo.isWaitStop()).thenReturn(true);
		return stopInfo;
	}

	private static MockedStatic<Core> stubCoreWithEpochTime(int scheduledSecs,
			long returnedEpoch) {
		Time time = mock(Time.class);
		when(time.getEpochTime(eq(scheduledSecs), any(Date.class))).thenReturn(returnedEpoch);

		Core core = mock(Core.class);
		when(core.getTime()).thenReturn(time);

		MockedStatic<Core> coreMock = mockStatic(Core.class);
		coreMock.when(Core::getInstance).thenReturn(core);
		return coreMock;
	}

}
