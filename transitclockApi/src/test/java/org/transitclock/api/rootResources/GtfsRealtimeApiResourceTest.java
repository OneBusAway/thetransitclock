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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import org.transitclock.api.gtfsRealtime.GtfsRtTestSupport;
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
 * (via {@link GtfsRtTestSupport}) instead of intercepting their static
 * accessors.
 */
public class GtfsRealtimeApiResourceTest extends JerseyTest {

	private static final String KEY = "test-key";
	private static final String AGENCY = GtfsRtTestSupport.AGENCY;

	private TimeZone savedDefault;

	@Override
	protected Application configure() {
		return new ResourceConfig(GtfsRealtimeApi.class);
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();

		savedDefault = TimeZone.getDefault();
		TimeZone.setDefault(GtfsRtTestSupport.AGENCY_TZ);

		GtfsRtTestSupport.seedAgencyTimezoneCache();
		GtfsRtTestSupport.seedApiKeyCache(KEY);

		Collection<IpcVehicleGtfsRealtime> vehicles = cannedVehicles();
		VehiclesInterface vehiclesIface = mock(VehiclesInterface.class);
		when(vehiclesIface.getGtfsRealtime()).thenReturn(vehicles);
		GtfsRtTestSupport.seedFactoryMap(VehiclesInterfaceFactory.class,
				"vehiclesInterfaceMap", AGENCY, vehiclesIface);

		List<IpcPredictionsForRouteStopDest> preds = cannedPredictionsByStop();
		PredictionsInterface predictionsIface = mock(PredictionsInterface.class);
		when(predictionsIface.getAllPredictions(anyInt())).thenReturn(preds);
		GtfsRtTestSupport.seedFactoryMap(PredictionsInterfaceFactory.class,
				"predictionsInterfaceMap", AGENCY, predictionsIface);

		GtfsRtTestSupport.clearProducerCaches();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			GtfsRtTestSupport.clearFactoryMap(VehiclesInterfaceFactory.class,
					"vehiclesInterfaceMap");
			GtfsRtTestSupport.clearFactoryMap(PredictionsInterfaceFactory.class,
					"predictionsInterfaceMap");
			GtfsRtTestSupport.clearApiKeyCache();
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

	private Collection<IpcVehicleGtfsRealtime> cannedVehicles() {
		long now = System.currentTimeMillis();
		IpcVehicleGtfsRealtime v = GtfsRtTestSupport.mockVehicle("V-AT-STOP", now);
		when(v.getAtOrNextStopId()).thenReturn("STOP-1");
		when(v.getAtOrNextGtfsStopSeq()).thenReturn(1);
		when(v.isAtStop()).thenReturn(true);
		when(v.getTripId()).thenReturn("trip-A");
		return Collections.singletonList(v);
	}

	private List<IpcPredictionsForRouteStopDest> cannedPredictionsByStop() {
		long now = System.currentTimeMillis();
		IpcPrediction p = GtfsRtTestSupport.mockPrediction(
				"trip-A", "STOP-1", 1, now + 60_000L, now);

		IpcPredictionsForRouteStopDest forStop = mock(IpcPredictionsForRouteStopDest.class);
		when(forStop.getPredictionsForRouteStop()).thenReturn(Arrays.asList(p));
		return Collections.singletonList(forStop);
	}
}
