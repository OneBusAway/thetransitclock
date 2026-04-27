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

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.transitclock.ipc.clients.CommandsInterfaceFactory;
import org.transitclock.ipc.interfaces.CommandsInterface;

public class CommandsApiSmokeTest extends JaxRsResourceSmokeBase {

	@Override
	protected Application configure() {
		return new ResourceConfig(CommandsApi.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		// Resource handler builds its own ApiCommandAck regardless of the
		// CommandsInterface return value, so no method-level stubbing needed.
		registerFactoryMock(CommandsInterfaceFactory.class, "commandsInterfaceMap",
				mock(CommandsInterface.class));
	}

	@Test
	public void pushAvlGetAsJson() {
		Response r = agencyCommand("pushAvl")
				.queryParam("v", "V1").queryParam("t", "1700000000000")
				.queryParam("lat", "38.9").queryParam("lon", "-77.0")
				.request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)).isTrue();
		assertThat(r.readEntity(String.class)).contains("AVL processed");
	}

	@Test
	public void pushAvlGetAsXml() {
		Response r = agencyCommand("pushAvl")
				.queryParam("v", "V1").queryParam("t", "1700000000000")
				.queryParam("lat", "38.9").queryParam("lon", "-77.0")
				.request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_XML_TYPE)).isTrue();
		String body = r.readEntity(String.class);
		assertThat(body).startsWith("<?xml").contains("AVL processed");
	}

	@Test
	public void pushAvlPostAsJson() {
		String avlBody = "{\"avl\":[{\"v\":\"V1\",\"t\":1700000000000,\"lat\":38.9,\"lon\":-77.0}]}";

		Response r = agencyCommand("pushAvl")
				.request(MediaType.APPLICATION_JSON)
				.post(Entity.entity(avlBody, MediaType.APPLICATION_JSON));

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)).isTrue();
	}
}
