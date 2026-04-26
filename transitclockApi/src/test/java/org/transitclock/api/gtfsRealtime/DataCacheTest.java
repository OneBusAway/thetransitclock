/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.api.gtfsRealtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.transitclock.utils.SettableSystemTime;
import org.transitclock.utils.Time;

import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class DataCacheTest {

	private static final String AGENCY = "wmata";
	private static final int CACHE_SECS = 15;
	private static final long T0 = 1_000_000L;

	private FeedMessage canned() {
		return FeedMessage.newBuilder()
				.setHeader(FeedHeader.newBuilder()
						.setGtfsRealtimeVersion("1.0")
						.setIncrementality(Incrementality.FULL_DATASET)
						.setTimestamp(0L))
				.build();
	}

	@Test
	public void getReturnsNullWhenEmpty() {
		assertThat(new DataCache().get(AGENCY, CACHE_SECS)).isNull();
	}

	@Test
	public void putThenGetReturnsCachedValue() {
		DataCache cache = new DataCache(new SettableSystemTime(T0));
		FeedMessage msg = canned();

		cache.put(AGENCY, msg);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isSameAs(msg);
	}

	@Test
	public void getWithinTtlReturnsCachedValue() {
		SettableSystemTime clock = new SettableSystemTime(T0);
		DataCache cache = new DataCache(clock);
		cache.put(AGENCY, canned());

		clock.set(T0 + (CACHE_SECS - 1) * Time.MS_PER_SEC);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isNotNull();
	}

	@Test
	public void getAfterTtlEvictsAndReturnsNull() {
		SettableSystemTime clock = new SettableSystemTime(T0);
		DataCache cache = new DataCache(clock);
		cache.put(AGENCY, canned());

		clock.set(T0 + (CACHE_SECS + 1) * Time.MS_PER_SEC);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isNull();
		assertThat(cache.get(AGENCY, CACHE_SECS)).isNull();
	}

	@Test
	public void putOverwritesExpiredEntry() {
		SettableSystemTime clock = new SettableSystemTime(T0);
		DataCache cache = new DataCache(clock);
		cache.put(AGENCY, canned());

		clock.set(T0 + (CACHE_SECS + 1) * Time.MS_PER_SEC);
		FeedMessage fresh = canned();
		cache.put(AGENCY, fresh);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isSameAs(fresh);
	}

	@Test
	public void cacheIsKeyedByAgency() {
		DataCache cache = new DataCache(new SettableSystemTime(T0));
		FeedMessage a = canned();
		FeedMessage b = canned();

		cache.put("agency-a", a);
		cache.put("agency-b", b);

		assertThat(cache.get("agency-a", CACHE_SECS)).isSameAs(a);
		assertThat(cache.get("agency-b", CACHE_SECS)).isSameAs(b);
		assertThat(cache.get("agency-c", CACHE_SECS)).isNull();
	}
}
