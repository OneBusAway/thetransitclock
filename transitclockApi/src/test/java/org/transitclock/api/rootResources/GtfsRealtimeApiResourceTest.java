/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.api.rootResources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.transitclock.api.utils.AgencyTimezoneCache;
import org.transitclock.db.webstructs.ApiKey;
import org.transitclock.db.webstructs.ApiKeyManager;
import org.transitclock.ipc.clients.PredictionsInterfaceFactory;
import org.transitclock.ipc.clients.VehiclesInterfaceFactory;
import org.transitclock.ipc.data.IpcPrediction;
import org.transitclock.ipc.data.IpcPredictionsForRouteStopDest;
import org.transitclock.ipc.data.IpcVehicleGtfsRealtime;
import org.transitclock.ipc.interfaces.PredictionsInterface;
import org.transitclock.ipc.interfaces.VehiclesInterface;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

/**
 * HTTP-level smoke for the GTFS-RT JAX-RS resource. Spins
 * {@link GtfsRealtimeApi} in-process on Jersey Test Framework's Grizzly2
 * container so the wire format, content negotiation, and resource bindings
 * are exercised end-to-end without Tomcat.
 *
 * <p>Mockito's mockStatic is thread-local and the resource runs on Grizzly's
 * request threads, so we seed the production-side singletons by reflection
 * instead of intercepting their static accessors.
 */
public class GtfsRealtimeApiResourceTest extends JerseyTest {

	private static final String KEY = "test-key";
	private static final String AGENCY = "wmata";

	private TimeZone savedDefault;

	@Override
	protected Application configure() {
		return new ResourceConfig(GtfsRealtimeApi.class);
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		// Pin JVM TZ since the producer's time-of-day formatter uses it.
		savedDefault = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

		// Seed AgencyTimezoneCache so the producer constructor short-circuits
		// past the RMI lookup.
		seedAgencyTimezoneCache();

		// Seed the API-key singleton with a known-valid key so the
		// resource's validate() short-circuits past any DB lookup.
		seedApiKeyCache(KEY);

		// Pre-populate the RMI client factory maps so the resource hits our
		// mocks instead of trying to bind a real RMI registry.
		Collection<IpcVehicleGtfsRealtime> vehicles = cannedVehicles();
		VehiclesInterface vehiclesIface = mock(VehiclesInterface.class);
		when(vehiclesIface.getGtfsRealtime()).thenReturn(vehicles);
		seedFactoryMap(VehiclesInterfaceFactory.class, "vehiclesInterfaceMap",
				AGENCY, vehiclesIface);

		List<IpcPredictionsForRouteStopDest> preds = cannedPredictionsByStop();
		PredictionsInterface predictionsIface = mock(PredictionsInterface.class);
		when(predictionsIface.getAllPredictions(anyInt())).thenReturn(preds);
		seedFactoryMap(PredictionsInterfaceFactory.class, "predictionsInterfaceMap",
				AGENCY, predictionsIface);

		// Drop any cached GTFS-RT messages from prior tests.
		clearProducerCaches();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			clearFactoryMap(VehiclesInterfaceFactory.class, "vehiclesInterfaceMap");
			clearFactoryMap(PredictionsInterfaceFactory.class, "predictionsInterfaceMap");
			clearApiKeyCache();
		} finally {
			if (savedDefault != null) TimeZone.setDefault(savedDefault);
			super.tearDown();
		}
	}

	@Test
	public void vehiclePositionsBinaryIsParseableFeedMessage() throws Exception {
		Response r = endpoint("vehiclePositions", null).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);

		byte[] body = r.readEntity(byte[].class);
		FeedMessage fm = FeedMessage.parseFrom(body);
		assertThat(fm.getEntityCount()).isGreaterThan(0);
		assertThat(fm.getEntity(0).getVehicle().getVehicle().getId()).startsWith("V-");
	}

	@Test
	public void vehiclePositionsHumanIsTextWithExpectedSubstring() {
		Response r = endpoint("vehiclePositions", "human").get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).isEqualTo(MediaType.TEXT_PLAIN);
		String body = r.readEntity(String.class);
		assertThat(body).contains("vehicle");
		assertThat(body).contains("V-AT-STOP");
	}

	@Test
	public void tripUpdatesBinaryIsParseableFeedMessage() throws Exception {
		Response r = endpoint("tripUpdates", null).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);

		byte[] body = r.readEntity(byte[].class);
		FeedMessage fm = FeedMessage.parseFrom(body);
		assertThat(fm.getEntityCount()).isGreaterThan(0);
		assertThat(fm.getEntity(0).getTripUpdate().getStopTimeUpdateCount()).isGreaterThan(0);
	}

	@Test
	public void tripUpdatesHumanIsTextWithExpectedSubstring() {
		Response r = endpoint("tripUpdates", "human").get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).isEqualTo(MediaType.TEXT_PLAIN);
		String body = r.readEntity(String.class);
		assertThat(body).contains("trip_update");
		assertThat(body).contains("trip-A");
	}

	@Test
	public void invalidApiKeyIsRejected() {
		Response r = target("/key/wrong-key/agency/" + AGENCY + "/command/gtfs-rt/vehiclePositions")
				.request(MediaType.APPLICATION_OCTET_STREAM).get();

		assertThat(r.getStatus()).isEqualTo(401);
	}

	private Invocation.Builder endpoint(String name, String formatOverride) {
		String path = "/key/" + KEY + "/agency/" + AGENCY + "/command/gtfs-rt/" + name;
		return formatOverride == null
				? target(path).request(MediaType.APPLICATION_OCTET_STREAM)
				: target(path).queryParam("format", formatOverride).request(MediaType.TEXT_PLAIN);
	}

	private static void seedAgencyTimezoneCache() throws Exception {
		Field f = AgencyTimezoneCache.class.getDeclaredField("timezonesMap");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<String, TimeZone> map = (Map<String, TimeZone>) f.get(null);
		map.put(AGENCY, TimeZone.getTimeZone("America/New_York"));
	}

	private static void seedApiKeyCache(String key) throws Exception {
		ApiKeyManager mgr = ApiKeyManager.getInstance();
		Field cacheField = ApiKeyManager.class.getDeclaredField("apiKeyCache");
		cacheField.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<String, ApiKey> cache = (Map<String, ApiKey>) cacheField.get(mgr);
		cache.put(key, mock(ApiKey.class));
	}

	private static void clearApiKeyCache() throws Exception {
		ApiKeyManager mgr = ApiKeyManager.getInstance();
		Field cacheField = ApiKeyManager.class.getDeclaredField("apiKeyCache");
		cacheField.setAccessible(true);
		((Map<?, ?>) cacheField.get(mgr)).clear();
	}

	@SuppressWarnings("unchecked")
	private static <T> void seedFactoryMap(Class<?> factoryClass, String mapField,
			String agencyId, T iface) throws Exception {
		Field f = factoryClass.getDeclaredField(mapField);
		f.setAccessible(true);
		((Map<String, T>) f.get(null)).put(agencyId, iface);
	}

	private static void clearFactoryMap(Class<?> factoryClass, String mapField) throws Exception {
		Field f = factoryClass.getDeclaredField(mapField);
		f.setAccessible(true);
		((Map<?, ?>) f.get(null)).clear();
	}

	private static void clearProducerCaches() throws Exception {
		clearStaticDataCache(
				Class.forName("org.transitclock.api.gtfsRealtime.GtfsRtVehicleFeed"),
				"vehicleFeedDataCache");
		clearStaticDataCache(
				Class.forName("org.transitclock.api.gtfsRealtime.GtfsRtTripFeed"),
				"tripFeedDataCache");
	}

	private static void clearStaticDataCache(Class<?> producer, String fieldName) throws Exception {
		Field f = producer.getDeclaredField(fieldName);
		f.setAccessible(true);
		Object cache = f.get(null);
		Field mapField = cache.getClass().getDeclaredField("cacheMap");
		mapField.setAccessible(true);
		((Map<?, ?>) mapField.get(cache)).clear();
	}

	private Collection<IpcVehicleGtfsRealtime> cannedVehicles() {
		IpcVehicleGtfsRealtime v = mock(IpcVehicleGtfsRealtime.class);
		long now = System.currentTimeMillis();
		lenient().when(v.getId()).thenReturn("V-AT-STOP");
		lenient().when(v.getLicensePlate()).thenReturn("PLATE-1");
		lenient().when(v.getLatitude()).thenReturn(38.9f);
		lenient().when(v.getLongitude()).thenReturn(-77.0f);
		lenient().when(v.getHeading()).thenReturn(Float.NaN);
		lenient().when(v.getSpeed()).thenReturn(Float.NaN);
		lenient().when(v.getGpsTime()).thenReturn(now);
		lenient().when(v.getRouteId()).thenReturn("5A");
		lenient().when(v.getTripId()).thenReturn("trip-A");
		lenient().when(v.getTripStartEpochTime()).thenReturn(now);
		lenient().when(v.getFreqStartTime()).thenReturn(0L);
		lenient().when(v.isCanceled()).thenReturn(false);
		lenient().when(v.isTripUnscheduled()).thenReturn(false);
		lenient().when(v.getAtOrNextStopId()).thenReturn("STOP-1");
		lenient().when(v.getAtOrNextGtfsStopSeq()).thenReturn(1);
		lenient().when(v.isPredictable()).thenReturn(true);
		lenient().when(v.isAtStop()).thenReturn(true);
		return Collections.singletonList(v);
	}

	private List<IpcPredictionsForRouteStopDest> cannedPredictionsByStop() {
		IpcPrediction p = mock(IpcPrediction.class);
		long now = System.currentTimeMillis();
		lenient().when(p.getRouteId()).thenReturn("5A");
		lenient().when(p.getTripId()).thenReturn("trip-A");
		lenient().when(p.getStopId()).thenReturn("STOP-1");
		lenient().when(p.getGtfsStopSeq()).thenReturn(1);
		lenient().when(p.getVehicleId()).thenReturn("V-A");
		lenient().when(p.getPredictionTime()).thenReturn(now + 60_000L);
		lenient().when(p.getAvlTime()).thenReturn(now);
		lenient().when(p.getTripStartEpochTime()).thenReturn(now);
		lenient().when(p.getFreqStartTime()).thenReturn(0L);
		lenient().when(p.isCanceled()).thenReturn(false);
		lenient().when(p.isTripUnscheduled()).thenReturn(false);
		lenient().when(p.isSchedBasedPred()).thenReturn(false);
		lenient().when(p.isDelayed()).thenReturn(false);
		lenient().when(p.isLateAndSubsequentTripSoMarkAsUncertain()).thenReturn(false);
		lenient().when(p.isArrival()).thenReturn(false);

		IpcPredictionsForRouteStopDest forStop = mock(IpcPredictionsForRouteStopDest.class);
		lenient().when(forStop.getPredictionsForRouteStop()).thenReturn(Arrays.asList(p));
		return Collections.singletonList(forStop);
	}
}
