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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.transitclock.db.structs.Agency;
import org.transitclock.ipc.clients.ConfigInterfaceFactory;
import org.transitclock.ipc.data.IpcRouteSummary;
import org.transitclock.ipc.interfaces.ConfigInterface;

public class TransitimeApiSmokeTest extends JaxRsResourceSmokeBase {

	@Override
	protected Application configure() {
		return new ResourceConfig(TransitimeApi.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		// Materialize all dependent mocks BEFORE wiring them into the
		// configIface stubs — Mockito's UnfinishedStubbingException fires
		// if mock-creation runs inside another when().thenReturn(...) call.
		List<String> ids = Arrays.asList("V1", "V2", "V3");
		Agency agency = mock(Agency.class);
		lenient().when(agency.getName()).thenReturn("WMATA");
		List<Agency> agencies = Collections.singletonList(agency);
		IpcRouteSummary route = routeSummary("5A", "5A", "Crystal City");
		List<IpcRouteSummary> routes = Collections.singletonList(route);

		ConfigInterface configIface = mock(ConfigInterface.class);
		when(configIface.getVehicleIds()).thenReturn(ids);
		when(configIface.getAgencies()).thenReturn(agencies);
		when(configIface.getRoutes()).thenReturn(routes);

		registerFactoryMock(ConfigInterfaceFactory.class, "configInterfaceMap", configIface);
	}

	private static IpcRouteSummary routeSummary(String id, String shortName, String longName) {
		IpcRouteSummary r = mock(IpcRouteSummary.class);
		lenient().when(r.getId()).thenReturn(id);
		lenient().when(r.getShortName()).thenReturn(shortName);
		lenient().when(r.getName()).thenReturn(longName);
		lenient().when(r.getLongName()).thenReturn(longName);
		lenient().when(r.getType()).thenReturn("3");
		return r;
	}

	@Test
	public void vehicleIdsAsJson() {
		Response r = agencyCommand("vehicleIds").request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)).isTrue();
		String body = r.readEntity(String.class);
		assertThat(body).contains("V1").contains("V2").contains("V3");
	}

	@Test
	public void vehicleIdsAsXml() {
		Response r = agencyCommand("vehicleIds").request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)).isTrue();
		String body = r.readEntity(String.class);
		assertThat(body).contains("V1").contains("V2").contains("V3");
		assertThat(body).startsWith("<?xml");
	}

	/**
	 * Routes endpoint exercises a deeply-nested JAXB-serialized DTO
	 * ({@code ApiRoutes} → {@code ApiRoute}) — the most likely site for a
	 * Jakarta XML Bind 4.x regression in Phase B.
	 */
	@Test
	public void routesAsJson() {
		Response r = agencyCommand("routes").request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)).isTrue();
		assertThat(r.readEntity(String.class)).contains("5A");
	}

	@Test
	public void routesAsXml() {
		Response r = agencyCommand("routes").request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)).isTrue();
		String body = r.readEntity(String.class);
		assertThat(body).startsWith("<?xml").contains("5A");
	}
}
