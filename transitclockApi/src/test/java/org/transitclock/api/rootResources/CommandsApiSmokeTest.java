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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Test;
import org.transitclock.api.gtfsRealtime.GtfsRtTestSupport;
import org.transitclock.ipc.clients.CommandsInterfaceFactory;
import org.transitclock.ipc.data.IpcAvl;
import org.transitclock.ipc.interfaces.CommandsInterface;

/**
 * Smoke for {@link CommandsApi}: hits {@code /command/pushAvl} once via GET
 * (query-string form) and once via POST (JSON body), each with JSON and XML
 * accept variants. Plan §0.2 calls for "one representative endpoint per HTTP
 * method".
 */
public class CommandsApiSmokeTest extends JaxRsResourceSmokeBase {

	@Override
	protected Application configure() {
		return new ResourceConfig(CommandsApi.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		CommandsInterface commandsIface = mock(CommandsInterface.class);
		when(commandsIface.pushAvl(any(IpcAvl.class))).thenReturn("OK");
		GtfsRtTestSupport.seedFactoryMap(CommandsInterfaceFactory.class,
				"commandsInterfaceMap", AGENCY, commandsIface);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			GtfsRtTestSupport.clearFactoryMap(CommandsInterfaceFactory.class,
					"commandsInterfaceMap");
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void pushAvlGetAsJson() {
		Response r = target("/key/" + KEY + "/agency/" + AGENCY + "/command/pushAvl")
				.queryParam("v", "V1").queryParam("t", "1700000000000")
				.queryParam("lat", "38.9").queryParam("lon", "-77.0")
				.request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
		assertThat(r.readEntity(String.class)).contains("AVL processed");
	}

	@Test
	public void pushAvlGetAsXml() {
		Response r = target("/key/" + KEY + "/agency/" + AGENCY + "/command/pushAvl")
				.queryParam("v", "V1").queryParam("t", "1700000000000")
				.queryParam("lat", "38.9").queryParam("lon", "-77.0")
				.request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_XML);
		String body = r.readEntity(String.class);
		assertThat(body).startsWith("<?xml").contains("AVL processed");
	}

	@Test
	public void pushAvlPostAsJson() {
		String avlBody = "{\"avl\":[{\"v\":\"V1\",\"t\":1700000000000,\"lat\":38.9,\"lon\":-77.0}]}";

		Response r = target("/key/" + KEY + "/agency/" + AGENCY + "/command/pushAvl")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(avlBody, MediaType.APPLICATION_JSON));

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
	}
}
