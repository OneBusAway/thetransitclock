/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.api.gtfsRealtime;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TimeZone;

import org.transitclock.api.utils.AgencyTimezoneCache;

/**
 * Test helpers shared across the GTFS-RT producer tests.
 *
 * Production constructors of {@link org.transitclock.api.gtfsRealtime.GtfsRtVehicleFeed}
 * and {@link org.transitclock.api.gtfsRealtime.GtfsRtTripFeed} call
 * {@link AgencyTimezoneCache#get(String)} which in turn does an RMI lookup
 * against a running Core. To exercise the feed builders in isolation we seed
 * the cache by reflection so the constructor short-circuits.
 */
final class GtfsRtTestSupport {

	static final String AGENCY = "wmata";
	static final TimeZone AGENCY_TZ = TimeZone.getTimeZone("America/New_York");

	private GtfsRtTestSupport() {
	}

	@SuppressWarnings("unchecked")
	static void seedAgencyTimezoneCache() {
		try {
			Field f = AgencyTimezoneCache.class.getDeclaredField("timezonesMap");
			f.setAccessible(true);
			((Map<String, TimeZone>) f.get(null)).put(AGENCY, AGENCY_TZ);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(
					"Could not seed AgencyTimezoneCache for tests", e);
		}
	}
}
