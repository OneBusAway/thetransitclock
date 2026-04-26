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
import java.util.List;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.transitclock.ipc.data.IpcVehicleGtfsRealtime;

import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;

/**
 * Builder unit tests for {@link GtfsRtVehicleFeed#createMessage(java.util.Collection)}.
 * Exercises the field-by-field shape of the produced FeedMessage from canned
 * IpcVehicleGtfsRealtime inputs (no Jersey, no servlet container, no RMI).
 */
public class GtfsRtVehicleFeedTest {

	private static final long FIXED_TIME_MS = 1_700_000_000_000L; // 2023-11-14 22:13:20Z
	private static final long EXPECTED_HEADER_TIMESTAMP_S = FIXED_TIME_MS / 1_000L;

	private static TimeZone savedDefault;

	@BeforeClass
	public static void seedTimezone() {
		GtfsRtTestSupport.seedAgencyTimezoneCache();
		// The producer's time-of-day formatter is not bound to the agency
		// timezone (only the date formatter is); pin the JVM default so
		// assertions are deterministic across CI hosts.
		savedDefault = TimeZone.getDefault();
		TimeZone.setDefault(GtfsRtTestSupport.AGENCY_TZ);
	}

	@AfterClass
	public static void restoreTimezone() {
		if (savedDefault != null) {
			TimeZone.setDefault(savedDefault);
		}
	}

	private GtfsRtVehicleFeed feed() {
		return new GtfsRtVehicleFeed(GtfsRtTestSupport.AGENCY, () -> FIXED_TIME_MS);
	}

	private IpcVehicleGtfsRealtime baseVehicle(String id) {
		IpcVehicleGtfsRealtime v = mock(IpcVehicleGtfsRealtime.class);
		// IpcVehicle parent state
		lenient().when(v.getId()).thenReturn(id);
		lenient().when(v.getLicensePlate()).thenReturn("PLATE-" + id);
		lenient().when(v.getLatitude()).thenReturn(38.9f);
		lenient().when(v.getLongitude()).thenReturn(-77.0f);
		lenient().when(v.getHeading()).thenReturn(Float.NaN);
		lenient().when(v.getSpeed()).thenReturn(Float.NaN);
		lenient().when(v.getGpsTime()).thenReturn(FIXED_TIME_MS);
		// Trip / route
		lenient().when(v.getRouteId()).thenReturn("5A");
		lenient().when(v.getTripId()).thenReturn("trip-001");
		lenient().when(v.getTripStartEpochTime()).thenReturn(FIXED_TIME_MS);
		lenient().when(v.getFreqStartTime()).thenReturn(0L);
		lenient().when(v.isCanceled()).thenReturn(false);
		lenient().when(v.isTripUnscheduled()).thenReturn(false);
		// Stop info
		lenient().when(v.getAtOrNextStopId()).thenReturn("STOP-1");
		lenient().when(v.getAtOrNextGtfsStopSeq()).thenReturn(7);
		// Predictability
		lenient().when(v.isPredictable()).thenReturn(true);
		lenient().when(v.isAtStop()).thenReturn(false);
		return v;
	}

	@Test
	public void emptyCollectionProducesValidHeaderAndNoEntities() {
		FeedMessage msg = feed().createMessage(Collections.emptyList());

		assertThat(msg.getHeader().getGtfsRealtimeVersion()).isEqualTo("1.0");
		assertThat(msg.getHeader().getIncrementality()).isEqualTo(Incrementality.FULL_DATASET);
		assertThat(msg.getHeader().getTimestamp()).isEqualTo(EXPECTED_HEADER_TIMESTAMP_S);
		assertThat(msg.getEntityCount()).isZero();
	}

	@Test
	public void predictableAtStopProducesStoppedAtStatus() {
		IpcVehicleGtfsRealtime v = baseVehicle("V1");
		when(v.isAtStop()).thenReturn(true);

		FeedMessage msg = feed().createMessage(Collections.singletonList(v));

		assertThat(msg.getEntityCount()).isEqualTo(1);
		VehiclePosition vp = msg.getEntity(0).getVehicle();
		assertThat(vp.getCurrentStatus()).isEqualTo(VehicleStopStatus.STOPPED_AT);
		assertThat(vp.getCurrentStopSequence()).isEqualTo(7);
		assertThat(vp.getStopId()).isEqualTo("STOP-1");
	}

	@Test
	public void predictableInTransitProducesInTransitToStatus() {
		IpcVehicleGtfsRealtime v = baseVehicle("V2");
		when(v.isAtStop()).thenReturn(false);

		FeedMessage msg = feed().createMessage(Collections.singletonList(v));

		VehiclePosition vp = msg.getEntity(0).getVehicle();
		assertThat(vp.getCurrentStatus()).isEqualTo(VehicleStopStatus.IN_TRANSIT_TO);
		assertThat(vp.getCurrentStopSequence()).isEqualTo(7);
	}

	@Test
	public void unpredictableVehicleHasNoCurrentStatusAndNoStopSequence() {
		IpcVehicleGtfsRealtime v = baseVehicle("V3");
		when(v.isPredictable()).thenReturn(false);

		FeedMessage msg = feed().createMessage(Collections.singletonList(v));

		VehiclePosition vp = msg.getEntity(0).getVehicle();
		assertThat(vp.hasCurrentStatus()).isFalse();
		assertThat(vp.hasCurrentStopSequence()).isFalse();
	}

	@Test
	public void canceledTripSetsCanceledScheduleRelationship() {
		IpcVehicleGtfsRealtime v = baseVehicle("V4");
		when(v.isCanceled()).thenReturn(true);

		FeedMessage msg = feed().createMessage(Collections.singletonList(v));

		// isCanceled is applied before the SCHEDULED/UNSCHEDULED branch in the
		// producer, so SCHEDULED ends up overriding CANCELED. This test pins
		// the current behavior so future rewrites preserve or knowingly change
		// it.
		assertThat(msg.getEntity(0).getVehicle().getTrip().getScheduleRelationship())
				.isEqualTo(ScheduleRelationship.SCHEDULED);
	}

	@Test
	public void unscheduledTripSetsUnscheduledScheduleRelationship() {
		IpcVehicleGtfsRealtime v = baseVehicle("V5");
		when(v.isTripUnscheduled()).thenReturn(true);

		FeedMessage msg = feed().createMessage(Collections.singletonList(v));

		assertThat(msg.getEntity(0).getVehicle().getTrip().getScheduleRelationship())
				.isEqualTo(ScheduleRelationship.UNSCHEDULED);
	}

	@Test
	public void frequencyBasedTripSetsStartTime() {
		IpcVehicleGtfsRealtime v = baseVehicle("V6");
		when(v.getFreqStartTime()).thenReturn(FIXED_TIME_MS);

		FeedMessage msg = feed().createMessage(Collections.singletonList(v));

		// HH:mm:ss in the agency timezone (America/New_York at 2023-11-14
		// 22:13:20Z is 17:13:20 EST).
		assertThat(msg.getEntity(0).getVehicle().getTrip().getStartTime())
				.isEqualTo("17:13:20");
		assertThat(msg.getEntity(0).getVehicle().getTrip().getStartDate())
				.isEqualTo("20231114");
	}

	@Test
	public void headingAndSpeedAreOmittedWhenNaN() {
		FeedMessage msg = feed().createMessage(Collections.singletonList(baseVehicle("V7")));

		com.google.transit.realtime.GtfsRealtime.Position pos =
				msg.getEntity(0).getVehicle().getPosition();
		assertThat(pos.hasBearing()).isFalse();
		assertThat(pos.hasSpeed()).isFalse();
	}

	@Test
	public void headingAndSpeedAreSetWhenFinite() {
		IpcVehicleGtfsRealtime v = baseVehicle("V8");
		when(v.getHeading()).thenReturn(123.4f);
		when(v.getSpeed()).thenReturn(8.5f);

		FeedMessage msg = feed().createMessage(Collections.singletonList(v));

		com.google.transit.realtime.GtfsRealtime.Position pos =
				msg.getEntity(0).getVehicle().getPosition();
		assertThat(pos.getBearing()).isEqualTo(123.4f);
		assertThat(pos.getSpeed()).isEqualTo(8.5f);
	}

	@Test
	public void licensePlatePropagatesToVehicleDescriptor() {
		FeedMessage msg = feed().createMessage(Collections.singletonList(baseVehicle("V9")));

		assertThat(msg.getEntity(0).getVehicle().getVehicle().getLicensePlate())
				.isEqualTo("PLATE-V9");
	}

	@Test
	public void multipleVehiclesProduceMultipleEntitiesInOrder() {
		List<IpcVehicleGtfsRealtime> vehicles =
				Arrays.asList(baseVehicle("VA"), baseVehicle("VB"), baseVehicle("VC"));

		FeedMessage msg = feed().createMessage(vehicles);

		assertThat(msg.getEntityCount()).isEqualTo(3);
		assertThat(msg.getEntity(0).getId()).isEqualTo("VA");
		assertThat(msg.getEntity(1).getId()).isEqualTo("VB");
		assertThat(msg.getEntity(2).getId()).isEqualTo("VC");
	}

	@Test
	public void gpsTimestampIsConvertedToSeconds() {
		FeedMessage msg = feed().createMessage(Collections.singletonList(baseVehicle("V10")));

		assertThat(msg.getEntity(0).getVehicle().getTimestamp())
				.isEqualTo(FIXED_TIME_MS / 1_000L);
	}
}
