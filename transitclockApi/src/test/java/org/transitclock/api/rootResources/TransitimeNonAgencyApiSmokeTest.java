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
import org.transitclock.db.webstructs.WebAgency;
import org.transitclock.ipc.clients.ConfigInterfaceFactory;
import org.transitclock.ipc.interfaces.ConfigInterface;

/**
 * Smoke for {@link TransitimeNonAgencyApi}: hits {@code /command/agencies}
 * which iterates {@link WebAgency#getCachedOrderedListOfWebAgencies()} (we
 * seed it to empty so the response is a valid-but-empty {@code ApiAgencies})
 * and tests both JSON and XML serialization.
 */
public class TransitimeNonAgencyApiSmokeTest extends JaxRsResourceSmokeBase {

	@Override
	protected Application configure() {
		return new ResourceConfig(TransitimeNonAgencyApi.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		GtfsRtTestSupport.seedWebAgencies(Collections.emptyList());
		// ConfigInterfaceFactory is referenced by the predictionsByLoc handler;
		// not exercised here but seed an empty mock to keep the factory map
		// non-null for any future tests added to this class.
		ConfigInterface configIface = mock(ConfigInterface.class);
		when(configIface.getAgencies()).thenReturn(Collections.emptyList());
		GtfsRtTestSupport.seedFactoryMap(ConfigInterfaceFactory.class,
				"configInterfaceMap", AGENCY, configIface);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			GtfsRtTestSupport.clearWebAgencies();
			GtfsRtTestSupport.clearFactoryMap(ConfigInterfaceFactory.class,
					"configInterfaceMap");
		} finally {
			super.tearDown();
		}
	}

	@Test
	public void agenciesAsJson() {
		Response r = target("/key/" + KEY + "/command/agencies")
				.request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
		assertThat(r.readEntity(String.class)).isNotEmpty();
	}

	@Test
	public void agenciesAsXml() {
		Response r = target("/key/" + KEY + "/command/agencies")
				.request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_XML);
		assertThat(r.readEntity(String.class)).startsWith("<?xml");
	}
}
