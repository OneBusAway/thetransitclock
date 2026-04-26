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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Test;
import org.transitclock.api.gtfsRealtime.GtfsRtTestSupport;
import org.transitclock.ipc.clients.VehiclesInterfaceFactory;
import org.transitclock.ipc.interfaces.VehiclesInterface;

/**
 * Smoke for {@link SiriApi}: hits {@code /command/siri/vehicleMonitoring}
 * with empty vehicle data so the SIRI XML/JSON wrapper still serializes
 * cleanly. Uses both JSON and XML accept variants.
 */
public class SiriApiSmokeTest extends JaxRsResourceSmokeBase {

	@Override
	protected Application configure() {
		return new ResourceConfig(SiriApi.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		VehiclesInterface vehiclesIface = mock(VehiclesInterface.class);
		when(vehiclesIface.getComplete()).thenReturn(Collections.emptyList());
		GtfsRtTestSupport.seedFactoryMap(VehiclesInterfaceFactory.class,
				"vehiclesInterfaceMap", AGENCY, vehiclesIface);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			GtfsRtTestSupport.clearFactoryMap(VehiclesInterfaceFactory.class,
					"vehiclesInterfaceMap");
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void vehicleMonitoringAsJson() {
		Response r = target("/key/" + KEY + "/agency/" + AGENCY
				+ "/command/siri/vehicleMonitoring")
				.request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
		assertThat(r.readEntity(String.class)).isNotEmpty();
	}

	@Test
	public void vehicleMonitoringAsXml() {
		Response r = target("/key/" + KEY + "/agency/" + AGENCY
				+ "/command/siri/vehicleMonitoring")
				.request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_XML);
		String body = r.readEntity(String.class);
		assertThat(body).startsWith("<?xml");
	}
}
