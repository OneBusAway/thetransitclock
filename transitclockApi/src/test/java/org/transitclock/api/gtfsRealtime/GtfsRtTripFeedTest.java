/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.api.gtfsRealtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.transitclock.ipc.data.IpcPrediction;

import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

/**
 * Builder unit tests for {@link GtfsRtTripFeed#createMessage(Map)}. Exercises
 * the field-by-field shape of the produced FeedMessage from canned
 * IpcPrediction inputs (no Jersey, no servlet container, no RMI).
 *
 * Pinned uncertainty values per the producer:
 *   schedule-based prediction → 300
 *   delayed                   → 301 (DELAYED_UNCERTAINTY_VALUE)
 *   late+subsequent trip      → 302 (LATE_AND_SUBSEQUENT_TRIP_UNCERTAINTY_VALUE)
 *   "delayed" wins over the other two by precedence order in the producer.
 */
public class GtfsRtTripFeedTest {

	private static final long FIXED_TIME_MS = 1_700_000_000_000L; // 2023-11-14 22:13:20Z
	private static final long EXPECTED_HEADER_TIMESTAMP_S = FIXED_TIME_MS / 1_000L;
	private static final long PREDICTION_TIME_MS = FIXED_TIME_MS + 60_000L; // +60s

	private static TimeZone savedDefault;

	@BeforeClass
	public static void seedTimezone() {
		GtfsRtTestSupport.seedAgencyTimezoneCache();
		savedDefault = TimeZone.getDefault();
		TimeZone.setDefault(GtfsRtTestSupport.AGENCY_TZ);
	}

	@AfterClass
	public static void restoreTimezone() {
		if (savedDefault != null) {
			TimeZone.setDefault(savedDefault);
		}
	}

	private GtfsRtTripFeed feed() {
		return new GtfsRtTripFeed(GtfsRtTestSupport.AGENCY, () -> FIXED_TIME_MS);
	}

	private IpcPrediction basePred(String tripId, String stopId, int seq) {
		IpcPrediction p = mock(IpcPrediction.class);
		lenient().when(p.getRouteId()).thenReturn("5A");
		lenient().when(p.getTripId()).thenReturn(tripId);
		lenient().when(p.getStopId()).thenReturn(stopId);
		lenient().when(p.getGtfsStopSeq()).thenReturn(seq);
		lenient().when(p.getVehicleId()).thenReturn("V-" + tripId);
		lenient().when(p.getPredictionTime()).thenReturn(PREDICTION_TIME_MS);
		lenient().when(p.getAvlTime()).thenReturn(FIXED_TIME_MS);
		lenient().when(p.getTripStartEpochTime()).thenReturn(FIXED_TIME_MS);
		lenient().when(p.getFreqStartTime()).thenReturn(0L);
		lenient().when(p.isCanceled()).thenReturn(false);
		lenient().when(p.isTripUnscheduled()).thenReturn(false);
		lenient().when(p.isSchedBasedPred()).thenReturn(false);
		lenient().when(p.isDelayed()).thenReturn(false);
		lenient().when(p.isLateAndSubsequentTripSoMarkAsUncertain()).thenReturn(false);
		lenient().when(p.isArrival()).thenReturn(false);
		lenient().when(p.getDelay()).thenReturn(null);
		return p;
	}

	private Map<String, List<IpcPrediction>> oneTrip(String tripId, IpcPrediction... preds) {
		Map<String, List<IpcPrediction>> m = new LinkedHashMap<>();
		m.put(tripId, Arrays.asList(preds));
		return m;
	}

	@Test
	public void emptyMapProducesValidHeaderAndNoEntities() {
		FeedMessage msg = feed().createMessage(Collections.emptyMap());

		assertThat(msg.getHeader().getGtfsRealtimeVersion()).isEqualTo("1.0");
		assertThat(msg.getHeader().getIncrementality()).isEqualTo(Incrementality.FULL_DATASET);
		assertThat(msg.getHeader().getTimestamp()).isEqualTo(EXPECTED_HEADER_TIMESTAMP_S);
		assertThat(msg.getEntityCount()).isZero();
	}

	@Test
	public void scheduledPredictionHasNoUncertaintyAndScheduledRelationship() {
		IpcPrediction p = basePred("trip-1", "S1", 3);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		TripUpdate tu = msg.getEntity(0).getTripUpdate();
		assertThat(tu.getStopTimeUpdateCount()).isEqualTo(1);
		StopTimeUpdate stu = tu.getStopTimeUpdate(0);
		assertThat(stu.getStopSequence()).isEqualTo(3);
		assertThat(stu.getStopId()).isEqualTo("S1");
		assertThat(stu.getDeparture().getTime()).isEqualTo(PREDICTION_TIME_MS / 1_000L);
		assertThat(stu.getDeparture().hasUncertainty()).isFalse();
		assertThat(tu.getTrip().getScheduleRelationship())
				.isEqualTo(TripDescriptor.ScheduleRelationship.SCHEDULED);
	}

	@Test
	public void schedBasedPredictionGetsUncertainty300() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.isSchedBasedPred()).thenReturn(true);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		assertThat(msg.getEntity(0).getTripUpdate().getStopTimeUpdate(0).getDeparture()
				.getUncertainty()).isEqualTo(300);
	}

	@Test
	public void delayedPredictionGetsUncertainty301() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.isDelayed()).thenReturn(true);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		assertThat(msg.getEntity(0).getTripUpdate().getStopTimeUpdate(0).getDeparture()
				.getUncertainty()).isEqualTo(301);
	}

	@Test
	public void lateAndSubsequentPredictionGetsUncertainty302WhenNotDelayed() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.isLateAndSubsequentTripSoMarkAsUncertain()).thenReturn(true);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		assertThat(msg.getEntity(0).getTripUpdate().getStopTimeUpdate(0).getDeparture()
				.getUncertainty()).isEqualTo(302);
	}

	@Test
	public void delayedTakesPrecedenceOverLateAndSubsequent() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.isLateAndSubsequentTripSoMarkAsUncertain()).thenReturn(true);
		when(p.isDelayed()).thenReturn(true);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		// Delayed (301) is set last in the producer's branch chain, so it
		// wins over LateAndSubsequent (302).
		assertThat(msg.getEntity(0).getTripUpdate().getStopTimeUpdate(0).getDeparture()
				.getUncertainty()).isEqualTo(301);
	}

	@Test
	public void canceledTripSuppressesStopTimeUpdates() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.isCanceled()).thenReturn(true);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		TripUpdate tu = msg.getEntity(0).getTripUpdate();
		assertThat(tu.getTrip().getScheduleRelationship())
				.isEqualTo(TripDescriptor.ScheduleRelationship.CANCELED);
		assertThat(tu.getStopTimeUpdateCount()).isZero();
	}

	@Test
	public void unscheduledTripSetsUnscheduledRelationship() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.isTripUnscheduled()).thenReturn(true);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		assertThat(msg.getEntity(0).getTripUpdate().getTrip().getScheduleRelationship())
				.isEqualTo(TripDescriptor.ScheduleRelationship.UNSCHEDULED);
	}

	@Test
	public void frequencyBasedTripSetsStartTime() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.getFreqStartTime()).thenReturn(FIXED_TIME_MS);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		// 2023-11-14 22:13:20 UTC → 17:13:20 EST in America/New_York.
		assertThat(msg.getEntity(0).getTripUpdate().getTrip().getStartTime())
				.isEqualTo("17:13:20");
	}

	@Test
	public void arrivalPredictionUsesArrivalEvent() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.isArrival()).thenReturn(true);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		StopTimeUpdate stu = msg.getEntity(0).getTripUpdate().getStopTimeUpdate(0);
		assertThat(stu.hasArrival()).isTrue();
		assertThat(stu.hasDeparture()).isFalse();
	}

	@Test
	public void multipleStopTimeUpdatesAreSortedByGtfsStopSeq() {
		IpcPrediction a = basePred("trip-1", "S3", 3);
		IpcPrediction b = basePred("trip-1", "S1", 1);
		IpcPrediction c = basePred("trip-1", "S2", 2);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", a, b, c));

		TripUpdate tu = msg.getEntity(0).getTripUpdate();
		assertThat(tu.getStopTimeUpdateCount()).isEqualTo(3);
		assertThat(tu.getStopTimeUpdate(0).getStopId()).isEqualTo("S1");
		assertThat(tu.getStopTimeUpdate(1).getStopId()).isEqualTo("S2");
		assertThat(tu.getStopTimeUpdate(2).getStopId()).isEqualTo("S3");
	}

	@Test
	public void multipleTripsProduceMultipleEntities() {
		IpcPrediction p1 = basePred("trip-1", "S1", 1);
		IpcPrediction p2 = basePred("trip-2", "S1", 1);
		Map<String, List<IpcPrediction>> m = new LinkedHashMap<>();
		m.put("trip-1", Collections.singletonList(p1));
		m.put("trip-2", Collections.singletonList(p2));

		FeedMessage msg = feed().createMessage(m);

		assertThat(msg.getEntityCount()).isEqualTo(2);
	}

	@Test
	public void avlTimestampIsConvertedToSeconds() {
		IpcPrediction p = basePred("trip-1", "S1", 3);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		assertThat(msg.getEntity(0).getTripUpdate().getTimestamp())
				.isEqualTo(FIXED_TIME_MS / 1_000L);
	}

	@Test
	public void delayIsNotIncludedByDefault() {
		IpcPrediction p = basePred("trip-1", "S1", 3);
		when(p.getDelay()).thenReturn(120);

		FeedMessage msg = feed().createMessage(oneTrip("trip-1", p));

		// includeTripUpdateDelay defaults to false so delay should not be set.
		assertThat(msg.getEntity(0).getTripUpdate().hasDelay()).isFalse();
	}
}
