/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.api.rootResources;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.transitclock.api.gtfsRealtime.GtfsRtTestSupport;

/**
 * Common JerseyTest scaffolding for resource smoke tests: ClassRule-pinned
 * agency timezone, API-key seeding in {@code @Before}, API-key cleanup in
 * {@code @After}. Subclasses register their resource via
 * {@link #configure()} and seed/clear the RMI factory maps they need.
 */
public abstract class JaxRsResourceSmokeBase extends JerseyTest {

	protected static final String KEY = "test-key";
	protected static final String AGENCY = GtfsRtTestSupport.AGENCY;

	@ClassRule
	public static final GtfsRtTestSupport.AgencyTimezone AGENCY_TZ =
			new GtfsRtTestSupport.AgencyTimezone();

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		GtfsRtTestSupport.seedApiKeyCache(KEY);
	}

	@After
	@Override
	public void tearDown() throws Exception {
		try {
			GtfsRtTestSupport.clearApiKeyCache();
		} finally {
			super.tearDown();
		}
	}
}
