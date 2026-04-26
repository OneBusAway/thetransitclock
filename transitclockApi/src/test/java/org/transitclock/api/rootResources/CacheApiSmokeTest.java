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
import org.transitclock.ipc.clients.CacheQueryInterfaceFactory;
import org.transitclock.ipc.interfaces.CacheQueryInterface;

/**
 * Smoke for {@link CacheApi}: hits {@code /command/kalmanerrorcachekeys} as
 * a representative GET in JSON and XML form. The endpoint exercises the
 * resource → CacheQueryInterface → JAXB serialization path.
 */
public class CacheApiSmokeTest extends JaxRsResourceSmokeBase {

	@Override
	protected Application configure() {
		return new ResourceConfig(CacheApi.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		CacheQueryInterface cacheIface = mock(CacheQueryInterface.class);
		when(cacheIface.getKalmanErrorCacheKeys()).thenReturn(Collections.emptyList());
		GtfsRtTestSupport.seedFactoryMap(CacheQueryInterfaceFactory.class,
				"cachequeryInterfaceMap", AGENCY, cacheIface);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			GtfsRtTestSupport.clearFactoryMap(CacheQueryInterfaceFactory.class,
					"cachequeryInterfaceMap");
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void kalmanErrorCacheKeysAsJson() {
		Response r = target("/key/" + KEY + "/agency/" + AGENCY
				+ "/command/kalmanerrorcachekeys")
				.request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
		assertThat(r.readEntity(String.class)).isNotEmpty();
	}

	@Test
	public void kalmanErrorCacheKeysAsXml() {
		Response r = target("/key/" + KEY + "/agency/" + AGENCY
				+ "/command/kalmanerrorcachekeys")
				.request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_XML);
		assertThat(r.readEntity(String.class)).startsWith("<?xml");
	}
}
