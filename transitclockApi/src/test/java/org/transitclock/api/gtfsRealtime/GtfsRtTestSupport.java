/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.api.gtfsRealtime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TimeZone;

import org.junit.rules.ExternalResource;
import org.transitclock.api.utils.AgencyTimezoneCache;
import org.transitclock.db.webstructs.ApiKey;
import org.transitclock.db.webstructs.ApiKeyManager;
import org.transitclock.ipc.data.IpcPrediction;
import org.transitclock.ipc.data.IpcVehicleGtfsRealtime;

/**
 * Test helpers shared across the GTFS-RT producer tests. Includes mock
 * builders, timezone-cache seeding, the JVM-default-timezone pin/restore
 * needed because {@link GtfsRtTripFeed}'s time-of-day formatter uses the JVM
 * default (only the date formatter is bound to the agency timezone), and
 * reflection-based seeders for the production-side singletons that the
 * HTTP-level resource tests rely on.
 */
public final class GtfsRtTestSupport {

	public static final String AGENCY = "wmata";
	public static final TimeZone AGENCY_TZ = TimeZone.getTimeZone("America/New_York");

	private GtfsRtTestSupport() {
	}

	@SuppressWarnings("unchecked")
	public static void seedAgencyTimezoneCache() {
		try {
			Field f = AgencyTimezoneCache.class.getDeclaredField("timezonesMap");
			f.setAccessible(true);
			((Map<String, TimeZone>) f.get(null)).put(AGENCY, AGENCY_TZ);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(
					"Could not seed AgencyTimezoneCache for tests", e);
		}
	}

	/**
	 * JUnit {@code @ClassRule} that seeds the agency timezone cache and pins
	 * the JVM default timezone to {@link #AGENCY_TZ} for the lifetime of the
	 * test class, restoring on tear-down. Used because the trip/vehicle feed
	 * time-of-day formatter resolves against the JVM default.
	 */
	public static final class AgencyTimezone extends ExternalResource {
		private TimeZone saved;

		@Override
		protected void before() {
			seedAgencyTimezoneCache();
			saved = TimeZone.getDefault();
			TimeZone.setDefault(AGENCY_TZ);
		}

		@Override
		protected void after() {
			if (saved != null) {
				TimeZone.setDefault(saved);
			}
		}
	}

	/**
	 * Builds a fully-stubbed mock of {@link IpcVehicleGtfsRealtime} with sane
	 * defaults: predictable, in-transit, NaN heading/speed, no
	 * frequency/cancellation/unscheduled flags. Per-test callers override
	 * specific fields via further {@code when(...).thenReturn(...)} calls.
	 */
	public static IpcVehicleGtfsRealtime mockVehicle(String id, long fixedTimeMs) {
		IpcVehicleGtfsRealtime v = mock(IpcVehicleGtfsRealtime.class);
		when(v.getId()).thenReturn(id);
		when(v.getLicensePlate()).thenReturn("PLATE-" + id);
		when(v.getLatitude()).thenReturn(38.9f);
		when(v.getLongitude()).thenReturn(-77.0f);
		when(v.getHeading()).thenReturn(Float.NaN);
		when(v.getSpeed()).thenReturn(Float.NaN);
		when(v.getGpsTime()).thenReturn(fixedTimeMs);
		when(v.getRouteId()).thenReturn("5A");
		when(v.getTripId()).thenReturn("trip-" + id);
		when(v.getTripStartEpochTime()).thenReturn(fixedTimeMs);
		when(v.getFreqStartTime()).thenReturn(0L);
		when(v.isCanceled()).thenReturn(false);
		when(v.isTripUnscheduled()).thenReturn(false);
		when(v.getAtOrNextStopId()).thenReturn("STOP-1");
		when(v.getAtOrNextGtfsStopSeq()).thenReturn(7);
		when(v.isPredictable()).thenReturn(true);
		when(v.isAtStop()).thenReturn(false);
		return v;
	}

	/**
	 * Builds a fully-stubbed mock of {@link IpcPrediction} with sane
	 * defaults: not canceled, not delayed, scheduled, departure event.
	 * Per-test callers override specific fields.
	 */
	public static IpcPrediction mockPrediction(String tripId, String stopId, int seq,
			long predictionTimeMs, long avlTimeMs) {
		IpcPrediction p = mock(IpcPrediction.class);
		when(p.getRouteId()).thenReturn("5A");
		when(p.getTripId()).thenReturn(tripId);
		when(p.getStopId()).thenReturn(stopId);
		when(p.getGtfsStopSeq()).thenReturn(seq);
		when(p.getVehicleId()).thenReturn("V-" + tripId);
		when(p.getPredictionTime()).thenReturn(predictionTimeMs);
		when(p.getAvlTime()).thenReturn(avlTimeMs);
		when(p.getTripStartEpochTime()).thenReturn(avlTimeMs);
		when(p.getFreqStartTime()).thenReturn(0L);
		when(p.isCanceled()).thenReturn(false);
		when(p.isTripUnscheduled()).thenReturn(false);
		when(p.isSchedBasedPred()).thenReturn(false);
		when(p.isDelayed()).thenReturn(false);
		when(p.isLateAndSubsequentTripSoMarkAsUncertain()).thenReturn(false);
		when(p.isArrival()).thenReturn(false);
		when(p.getDelay()).thenReturn(null);
		return p;
	}

	/**
	 * Seeds the {@link ApiKeyManager} singleton's cache with a known-valid
	 * key and pins the "last keys read" timestamp to {@code Long.MAX_VALUE}
	 * so any cache miss for an unknown key returns {@code false} immediately
	 * — without falling through to a Hibernate {@code getApiKeys()} lookup
	 * that has no DB connection in tests.
	 */
	@SuppressWarnings("unchecked")
	public static void seedApiKeyCache(String key) {
		try {
			ApiKeyManager mgr = ApiKeyManager.getInstance();
			Field cache = ApiKeyManager.class.getDeclaredField("apiKeyCache");
			cache.setAccessible(true);
			((Map<String, ApiKey>) cache.get(mgr)).put(key, mock(ApiKey.class));

			Field last = ApiKeyManager.class.getDeclaredField("lastTimeKeysReadIntoCache");
			last.setAccessible(true);
			last.setLong(mgr, Long.MAX_VALUE);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not seed ApiKeyManager cache", e);
		}
	}

	public static void clearApiKeyCache() {
		try {
			ApiKeyManager mgr = ApiKeyManager.getInstance();
			Field cache = ApiKeyManager.class.getDeclaredField("apiKeyCache");
			cache.setAccessible(true);
			((Map<?, ?>) cache.get(mgr)).clear();

			Field last = ApiKeyManager.class.getDeclaredField("lastTimeKeysReadIntoCache");
			last.setAccessible(true);
			last.setLong(mgr, 0L);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Could not clear ApiKeyManager cache", e);
		}
	}

	/**
	 * Seeds the agency-keyed map of an RMI client factory (e.g.,
	 * {@code VehiclesInterfaceFactory.vehiclesInterfaceMap}) so resource tests
	 * hit a mock instead of binding a real RMI registry.
	 */
	@SuppressWarnings("unchecked")
	public static <T> void seedFactoryMap(Class<?> factoryClass, String mapField,
			String agencyId, T iface) {
		try {
			Field f = factoryClass.getDeclaredField(mapField);
			f.setAccessible(true);
			((Map<String, T>) f.get(null)).put(agencyId, iface);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(
					"Could not seed factory map " + factoryClass.getSimpleName()
							+ "." + mapField, e);
		}
	}

	public static void clearFactoryMap(Class<?> factoryClass, String mapField) {
		try {
			Field f = factoryClass.getDeclaredField(mapField);
			f.setAccessible(true);
			((Map<?, ?>) f.get(null)).clear();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(
					"Could not clear factory map " + factoryClass.getSimpleName()
							+ "." + mapField, e);
		}
	}

	/**
	 * Drops cached FeedMessages from the producer-side {@link DataCache}
	 * statics so each test sees a fresh build path.
	 */
	public static void clearProducerCaches() {
		clearStaticDataCache(GtfsRtVehicleFeed.class, "vehicleFeedDataCache");
		clearStaticDataCache(GtfsRtTripFeed.class, "tripFeedDataCache");
	}

	private static void clearStaticDataCache(Class<?> producer, String fieldName) {
		try {
			Field f = producer.getDeclaredField(fieldName);
			f.setAccessible(true);
			Object cache = f.get(null);
			Field mapField = cache.getClass().getDeclaredField("cacheMap");
			mapField.setAccessible(true);
			((Map<?, ?>) mapField.get(cache)).clear();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(
					"Could not clear static DataCache " + producer.getSimpleName()
							+ "." + fieldName, e);
		}
	}
}
