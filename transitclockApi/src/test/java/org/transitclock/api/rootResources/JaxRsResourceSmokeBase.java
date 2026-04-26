/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.api.rootResources;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.transitclock.api.gtfsRealtime.GtfsRtTestSupport;

/**
 * Common JerseyTest scaffolding for resource smoke tests: a ClassRule-pinned
 * agency timezone, API-key seeding in {@code @Before}, and a teardown
 * registry so subclasses can {@link #registerFactoryMock} their RMI client
 * stubs without writing matching {@code @After} cleanup code.
 *
 * <p>Subclasses register their resource via {@link #configure()} and call
 * {@link #registerFactoryMock} (or {@link #registerCleanup}) in their own
 * {@code setUp} after invoking {@code super.setUp()}.
 */
public abstract class JaxRsResourceSmokeBase extends JerseyTest {

	protected static final String KEY = "test-key";

	@ClassRule
	public static final GtfsRtTestSupport.AgencyTimezone AGENCY_TZ =
			new GtfsRtTestSupport.AgencyTimezone();

	private final List<Runnable> teardownActions = new ArrayList<>();

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
			for (Runnable action : teardownActions) {
				try {
					action.run();
				} catch (RuntimeException keepUnwinding) {
					// Keep unwinding remaining cleanups even if one fails.
				}
			}
			teardownActions.clear();
			GtfsRtTestSupport.clearApiKeyCache();
		} finally {
			super.tearDown();
		}
	}

	/**
	 * Seeds an RMI client factory's static map with the given mock and
	 * registers the matching {@code clearFactoryMap} to run at tearDown.
	 */
	protected final void registerFactoryMock(Class<?> factoryClass,
			String mapField, Object iface) {
		GtfsRtTestSupport.seedFactoryMap(factoryClass, mapField,
				GtfsRtTestSupport.AGENCY, iface);
		registerCleanup(() -> GtfsRtTestSupport.clearFactoryMap(factoryClass, mapField));
	}

	/** Registers an arbitrary cleanup callback for tearDown. */
	protected final void registerCleanup(Runnable action) {
		teardownActions.add(action);
	}

	/** Builds a target under {@code /key/{KEY}/agency/{AGENCY}/command/...}. */
	protected final WebTarget agencyCommand(String command) {
		return target("/key/" + KEY + "/agency/" + GtfsRtTestSupport.AGENCY
				+ "/command/" + command);
	}

	/** Builds a target under {@code /key/{KEY}/command/...} (no agency segment). */
	protected final WebTarget keyCommand(String command) {
		return target("/key/" + KEY + "/command/" + command);
	}
}
