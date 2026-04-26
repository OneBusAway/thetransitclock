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

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.transitclock.ipc.clients.ConfigInterfaceFactory;
import org.transitclock.ipc.interfaces.ConfigInterface;

/**
 * Smoke for {@link TransitimeApi}: hits {@code /command/vehicleIds} with
 * both JSON and XML accept variants.
 */
public class TransitimeApiSmokeTest extends JaxRsResourceSmokeBase {

	@Override
	protected Application configure() {
		return new ResourceConfig(TransitimeApi.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		List<String> vehicleIds = Arrays.asList("V1", "V2", "V3");
		ConfigInterface configIface = mock(ConfigInterface.class);
		when(configIface.getVehicleIds()).thenReturn(vehicleIds);
		registerFactoryMock(ConfigInterfaceFactory.class, "configInterfaceMap", configIface);
	}

	@Test
	public void vehicleIdsAsJson() {
		Response r = agencyCommand("vehicleIds").request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
		String body = r.readEntity(String.class);
		assertThat(body).contains("V1").contains("V2").contains("V3");
	}

	@Test
	public void vehicleIdsAsXml() {
		Response r = agencyCommand("vehicleIds").request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_XML);
		String body = r.readEntity(String.class);
		assertThat(body).contains("V1").contains("V2").contains("V3");
		assertThat(body).startsWith("<?xml");
	}
}
