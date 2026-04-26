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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.transitclock.ipc.data.IpcVehicleGtfsRealtime;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

/**
 * Golden-fixture regression suite: builds VehiclePositions and TripUpdates
 * FeedMessages from canned, deterministic inputs and compares the resulting
 * byte stream against committed {@code .pb} fixtures. Catches any wire-format
 * regression introduced by the Java 21 jump (Phase A) or the Jakarta jump
 * (Phase B).
 *
 * <p>To regenerate the fixtures (after a deliberate change to canned inputs
 * or producer behavior), run:
 *
 * <pre>
 *   mvn -pl transitclockApi -am test -Dtest=GoldenFixtureTest -Dgtfsrt.regen=true
 * </pre>
 *
 * The regen mode writes the freshly-built bytes back into
 * {@code transitclockApi/src/test/resources/gtfsrt/}. Commit the diff after
 * eyeballing it.
 */
public class GoldenFixtureTest {

	private static final long FIXED_TIME_MS = 1_700_000_000_000L;
	private static final String VEHICLE_FIXTURE = "/gtfsrt/vehicle_positions_baseline.pb";
	private static final String TRIP_FIXTURE = "/gtfsrt/trip_updates_baseline.pb";
	private static final Path FIXTURES_SOURCE_DIR =
			Paths.get("src/test/resources/gtfsrt");

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

	@Test
	public void vehiclePositionsMatchGoldenFixture() throws IOException {
		FeedMessage built = buildVehiclesMessage();
		assertFixtureMatches(VEHICLE_FIXTURE, "vehicle_positions_baseline.pb", built);
	}

	@Test
	public void tripUpdatesMatchGoldenFixture() throws IOException {
		FeedMessage built = buildTripUpdatesMessage();
		assertFixtureMatches(TRIP_FIXTURE, "trip_updates_baseline.pb", built);
	}

	private void assertFixtureMatches(String classpathPath, String fileName, FeedMessage built)
			throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		built.writeTo(baos);
		byte[] actual = baos.toByteArray();

		if (Boolean.getBoolean("gtfsrt.regen")) {
			Path target = FIXTURES_SOURCE_DIR.resolve(fileName);
			Files.createDirectories(target.getParent());
			Files.write(target, actual);
			return;
		}

		byte[] expected = readFixture(classpathPath);

		// Structural equality first — produces a useful diff if shape changed.
		assertThat(FeedMessage.parseFrom(actual)).isEqualTo(FeedMessage.parseFrom(expected));

		// Byte equality second — pins the exact wire format.
		assertThat(actual).isEqualTo(expected);
	}

	private byte[] readFixture(String classpathPath) throws IOException {
		try (InputStream in = GoldenFixtureTest.class.getResourceAsStream(classpathPath)) {
			if (in == null) {
				throw new AssertionError("Missing fixture on classpath: " + classpathPath
						+ ". Regenerate with -Dgtfsrt.regen=true.");
			}
			return in.readAllBytes();
		}
	}

	private FeedMessage buildVehiclesMessage() {
		GtfsRtVehicleFeed feed =
				new GtfsRtVehicleFeed(GtfsRtTestSupport.AGENCY, () -> FIXED_TIME_MS);
		return feed.createMessage(cannedVehicles());
	}

	private FeedMessage buildTripUpdatesMessage() {
		GtfsRtTripFeed feed =
				new GtfsRtTripFeed(GtfsRtTestSupport.AGENCY, () -> FIXED_TIME_MS);
		return feed.createMessage(cannedTripPredictions());
	}

	/**
	 * Deterministic canned set covering predictable+at-stop, predictable+
	 * in-transit, and an unpredictable vehicle.
	 */
	private List<IpcVehicleGtfsRealtime> cannedVehicles() {
		return Arrays.asList(
				vehicle("V-AT-STOP", true, true, 38.9012f, -77.0369f, 12.5f, 95.0f, 7),
				vehicle("V-IN-TRANSIT", true, false, 38.8951f, -77.0364f, 18.2f, 180.0f, 8),
				vehicle("V-UNPREDICTABLE", false, false, 38.8895f, -77.0353f, 0.0f, Float.NaN, null));
	}

	private IpcVehicleGtfsRealtime vehicle(String id, boolean predictable, boolean atStop,
			float lat, float lon, float speed, float heading, Integer stopSeq) {
		IpcVehicleGtfsRealtime v = mock(IpcVehicleGtfsRealtime.class);
		lenient().when(v.getId()).thenReturn(id);
		lenient().when(v.getLicensePlate()).thenReturn("PLATE-" + id);
		lenient().when(v.getLatitude()).thenReturn(lat);
		lenient().when(v.getLongitude()).thenReturn(lon);
		lenient().when(v.getHeading()).thenReturn(heading);
		lenient().when(v.getSpeed()).thenReturn(speed);
		lenient().when(v.getGpsTime()).thenReturn(FIXED_TIME_MS);
		lenient().when(v.getRouteId()).thenReturn("5A");
		lenient().when(v.getTripId()).thenReturn("trip-" + id);
		lenient().when(v.getTripStartEpochTime()).thenReturn(FIXED_TIME_MS);
		lenient().when(v.getFreqStartTime()).thenReturn(0L);
		lenient().when(v.isCanceled()).thenReturn(false);
		lenient().when(v.isTripUnscheduled()).thenReturn(false);
		lenient().when(v.getAtOrNextStopId()).thenReturn("STOP-" + (stopSeq == null ? "X" : stopSeq));
		lenient().when(v.getAtOrNextGtfsStopSeq()).thenReturn(stopSeq);
		lenient().when(v.isPredictable()).thenReturn(predictable);
		lenient().when(v.isAtStop()).thenReturn(atStop);
		return v;
	}

	/**
	 * Deterministic canned set covering: a normal multi-stop trip, a delayed
	 * single-stop trip, and a schedule-based prediction. Iteration order is
	 * pinned via LinkedHashMap so the proto entity order is stable.
	 */
	private Map<String, List<IpcPrediction>> cannedTripPredictions() {
		Map<String, List<IpcPrediction>> m = new LinkedHashMap<>();
		m.put("trip-A", Arrays.asList(
				prediction("trip-A", "STOP-1", 1, FIXED_TIME_MS + 60_000L, false, false, false, false),
				prediction("trip-A", "STOP-2", 2, FIXED_TIME_MS + 180_000L, false, false, false, false),
				prediction("trip-A", "STOP-3", 3, FIXED_TIME_MS + 300_000L, false, false, false, true)));
		m.put("trip-B", Collections.singletonList(
				prediction("trip-B", "STOP-1", 1, FIXED_TIME_MS + 90_000L, false, true, false, false)));
		m.put("trip-C", Collections.singletonList(
				prediction("trip-C", "STOP-1", 1, FIXED_TIME_MS + 120_000L, true, false, false, false)));
		return m;
	}

	private IpcPrediction prediction(String tripId, String stopId, int seq, long predTimeMs,
			boolean schedBased, boolean delayed, boolean lateAndSubsequent, boolean isArrival) {
		IpcPrediction p = mock(IpcPrediction.class);
		lenient().when(p.getRouteId()).thenReturn("5A");
		lenient().when(p.getTripId()).thenReturn(tripId);
		lenient().when(p.getStopId()).thenReturn(stopId);
		lenient().when(p.getGtfsStopSeq()).thenReturn(seq);
		lenient().when(p.getVehicleId()).thenReturn("V-" + tripId);
		lenient().when(p.getPredictionTime()).thenReturn(predTimeMs);
		lenient().when(p.getAvlTime()).thenReturn(FIXED_TIME_MS);
		lenient().when(p.getTripStartEpochTime()).thenReturn(FIXED_TIME_MS);
		lenient().when(p.getFreqStartTime()).thenReturn(0L);
		lenient().when(p.isCanceled()).thenReturn(false);
		lenient().when(p.isTripUnscheduled()).thenReturn(false);
		lenient().when(p.isSchedBasedPred()).thenReturn(schedBased);
		lenient().when(p.isDelayed()).thenReturn(delayed);
		lenient().when(p.isLateAndSubsequentTripSoMarkAsUncertain()).thenReturn(lateAndSubsequent);
		lenient().when(p.isArrival()).thenReturn(isArrival);
		lenient().when(p.getDelay()).thenReturn(null);
		return p;
	}
}
