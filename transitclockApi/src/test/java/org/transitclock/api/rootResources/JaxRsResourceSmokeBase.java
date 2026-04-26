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

import jakarta.ws.rs.client.WebTarget;

import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.transitclock.api.gtfsRealtime.GtfsRtTestSupport;

/**
 * JerseyTest scaffolding for the resource smoke tests. Resources read
 * agency timezone, API keys, and RMI client interfaces from process-global
 * static caches, so each test must seed those caches in setUp and clear
 * them in tearDown — the {@link #registerFactoryMock} / {@link
 * #registerCleanup} pair gives subclasses a single registration point that
 * the base unwinds automatically.
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
				} catch (RuntimeException e) {
					// Keep unwinding remaining cleanups so one failed clear
					// doesn't strand other static state, but surface the
					// failure to stderr — silent leakage of static state
					// into the next test class is the worst outcome.
					System.err.println("Cleanup action failed: " + e);
					e.printStackTrace(System.err);
				}
			}
			teardownActions.clear();
			GtfsRtTestSupport.clearApiKeyCache();
		} finally {
			super.tearDown();
		}
	}

	protected final void registerFactoryMock(Class<?> factoryClass,
			String mapField, Object iface) {
		GtfsRtTestSupport.seedFactoryMap(factoryClass, mapField,
				GtfsRtTestSupport.AGENCY, iface);
		registerCleanup(() -> GtfsRtTestSupport.clearFactoryMap(factoryClass, mapField));
	}

	protected final void registerCleanup(Runnable action) {
		teardownActions.add(action);
	}

	protected final WebTarget agencyCommand(String command) {
		return target("/key/" + KEY + "/agency/" + GtfsRtTestSupport.AGENCY
				+ "/command/" + command);
	}

	protected final WebTarget keyCommand(String command) {
		return target("/key/" + KEY + "/command/" + command);
	}
}
