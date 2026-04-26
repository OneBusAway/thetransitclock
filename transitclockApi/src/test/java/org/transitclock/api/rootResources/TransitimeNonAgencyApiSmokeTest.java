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

import java.util.Collections;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;
import org.transitclock.api.gtfsRealtime.GtfsRtTestSupport;
import org.transitclock.db.webstructs.WebAgency;

/**
 * Smoke for {@link TransitimeNonAgencyApi}: hits {@code /command/agencies}
 * which iterates {@link WebAgency#getCachedOrderedListOfWebAgencies()}. We
 * seed it to empty so the response is a valid-but-empty {@code ApiAgencies}.
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
		registerCleanup(GtfsRtTestSupport::clearWebAgencies);
	}

	@Test
	public void agenciesAsJson() {
		Response r = keyCommand("agencies").request(MediaType.APPLICATION_JSON).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_JSON);
		assertThat(r.readEntity(String.class)).isNotEmpty();
	}

	@Test
	public void agenciesAsXml() {
		Response r = keyCommand("agencies").request(MediaType.APPLICATION_XML).get();

		assertThat(r.getStatus()).isEqualTo(200);
		assertThat(r.getMediaType().toString()).startsWith(MediaType.APPLICATION_XML);
		assertThat(r.readEntity(String.class)).startsWith("<?xml");
	}
}
