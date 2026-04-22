package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Test;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.Headway;
import org.transitclock.db.structs.HoldingTime;

/**
 * Named {@code CoreVehicleStateTest} to keep it distinct from the existing
 * VehicleStateTest that lives in {@code db/structs/} and tests a different
 * (entity) class of the same name.
 */
public class CoreVehicleStateTest {

	@Test
	public void newVehicleState_hasVehicleIdAndSensibleDefaults() {
		VehicleState vs = new VehicleState("veh-42");

		assertThat(vs.getVehicleId()).isEqualTo("veh-42");
		assertThat(vs.isPredictable()).isFalse();
		assertThat(vs.getMatch()).isNull();
		assertThat(vs.getAvlReport()).isNull();
		assertThat(vs.getHeadway()).isNull();
		assertThat(vs.getHoldingTime()).isNull();
		assertThat(vs.numberOfBadMatches()).isEqualTo(0);
		assertThat(vs.overLimitOfBadMatches()).isFalse();
	}

	@Test
	public void setAvlReport_pushesOntoHistoryAndIsReturnedAsCurrent() {
		VehicleState vs = new VehicleState("veh-1");
		AvlReport first = avlReportAt(1_000L);
		AvlReport second = avlReportAt(2_000L);

		vs.setAvlReport(first);
		assertThat(vs.getAvlReport()).isSameAs(first);

		vs.setAvlReport(second);
		assertThat(vs.getAvlReport()).isSameAs(second);
	}

	@Test
	public void incrementNumberOfBadMatches_walksTowardLimit() {
		VehicleState vs = new VehicleState("veh-1");

		vs.incrementNumberOfBadMatches();
		vs.incrementNumberOfBadMatches();

		assertThat(vs.numberOfBadMatches()).isEqualTo(2);
	}

	@Test
	public void setMatch_nullMakesVehicleUnpredictableAndResetsBadMatches() {
		VehicleState vs = new VehicleState("veh-1");
		vs.incrementNumberOfBadMatches();

		vs.setMatch(null);

		assertThat(vs.isPredictable()).isFalse();
		assertThat(vs.numberOfBadMatches()).isEqualTo(0);
		assertThat(vs.getMatch()).isNull();
	}

	@Test
	public void setMatch_nonNullIsRetrievable() {
		VehicleState vs = new VehicleState("veh-1");
		TemporalMatch m = mock(TemporalMatch.class);

		vs.setMatch(m);

		assertThat(vs.getMatch()).isSameAs(m);
	}

	@Test
	public void setHeadway_andHoldingTimeRoundtrip() {
		VehicleState vs = new VehicleState("veh-1");
		Headway h = mock(Headway.class);
		HoldingTime ht = mock(HoldingTime.class);

		vs.setHeadway(h);
		vs.setHoldingTime(ht);

		assertThat(vs.getHeadway()).isSameAs(h);
		assertThat(vs.getHoldingTime()).isSameAs(ht);
	}

	@Test
	public void incrementTripCounter_startsAtZeroAndIncrements() {
		VehicleState vs = new VehicleState("veh-1");
		assertThat(vs.getTripCounter()).isEqualTo(0);

		vs.incrementTripCounter();
		assertThat(vs.getTripCounter()).isEqualTo(1);

		vs.incrementTripCounter();
		assertThat(vs.getTripCounter()).isEqualTo(2);
	}

	@Test
	public void putAndGetTripStartTime_roundtripsOnTripCounter() {
		VehicleState vs = new VehicleState("veh-1");
		vs.putTripStartTime(5, 123_456_789L);

		assertThat(vs.getTripStartTime(5)).isEqualTo(123_456_789L);
		assertThat(vs.getTripStartTime(999)).isNull();
	}

	@Test
	public void arrivalToStoreToDb_setAndGet() {
		VehicleState vs = new VehicleState("veh-1");
		org.transitclock.db.structs.Arrival arrival =
				mock(org.transitclock.db.structs.Arrival.class);

		vs.setArrivalToStoreToDb(arrival);

		assertThat(vs.getArrivalToStoreToDb()).isSameAs(arrival);
	}

	@Test
	public void realTimeSchedAdh_isOnlyVisibleWhenPredictable() {
		// Stored internally but getRealTimeSchedAdh() hides it from
		// unpredictable vehicles. A fresh VehicleState is not predictable.
		VehicleState vs = new VehicleState("veh-1");
		TemporalDifference diff = new TemporalDifference(15_000);

		vs.setRealTimeSchedAdh(diff);

		assertThat(vs.getRealTimeSchedAdh()).isNull(); // not predictable → null
	}

	private static AvlReport avlReportAt(long epochMs) {
		AvlReport report = mock(AvlReport.class);
		when(report.getTime()).thenReturn(epochMs);
		when(report.getDate()).thenReturn(new Date(epochMs));
		return report;
	}
}
