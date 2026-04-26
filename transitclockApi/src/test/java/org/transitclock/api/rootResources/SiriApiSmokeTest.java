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
import org.junit.Test;
import org.transitclock.ipc.clients.VehiclesInterfaceFactory;
import org.transitclock.ipc.interfaces.VehiclesInterface;

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
		registerFactoryMock(VehiclesInterfaceFactory.class, "vehiclesInterfaceMap", vehiclesIface);
	}

	@Test
	public void vehicleMonitoringAsJson() {
		Response r = agencyCommand("siri/vehicleMonitoring")
				.request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)).isTrue();
		assertThat(r.readEntity(String.class)).isNotEmpty();
	}

	@Test
	public void vehicleMonitoringAsXml() {
		Response r = agencyCommand("siri/vehicleMonitoring")
				.request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)).isTrue();
		assertThat(r.readEntity(String.class)).startsWith("<?xml");
	}
}
