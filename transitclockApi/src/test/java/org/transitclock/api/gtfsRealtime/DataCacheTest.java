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

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class DataCacheTest {

	private static final String AGENCY = "wmata";
	private static final int CACHE_SECS = 15;

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
		AtomicLong now = new AtomicLong(1_000_000L);
		DataCache cache = new DataCache(now::get);
		FeedMessage msg = canned();

		cache.put(AGENCY, msg);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isSameAs(msg);
	}

	@Test
	public void getWithinTtlReturnsCachedValue() {
		AtomicLong now = new AtomicLong(1_000_000L);
		DataCache cache = new DataCache(now::get);
		cache.put(AGENCY, canned());

		now.addAndGet((CACHE_SECS - 1) * 1000L);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isNotNull();
	}

	@Test
	public void getAfterTtlEvictsAndReturnsNull() {
		AtomicLong now = new AtomicLong(1_000_000L);
		DataCache cache = new DataCache(now::get);
		cache.put(AGENCY, canned());

		now.addAndGet((CACHE_SECS + 1) * 1000L);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isNull();
		// Subsequent reads at the same wall-clock should not resurrect.
		assertThat(cache.get(AGENCY, CACHE_SECS)).isNull();
	}

	@Test
	public void putOverwritesExpiredEntry() {
		AtomicLong now = new AtomicLong(1_000_000L);
		DataCache cache = new DataCache(now::get);
		cache.put(AGENCY, canned());

		now.addAndGet((CACHE_SECS + 1) * 1000L);
		FeedMessage fresh = canned();
		cache.put(AGENCY, fresh);

		assertThat(cache.get(AGENCY, CACHE_SECS)).isSameAs(fresh);
	}

	@Test
	public void cacheIsKeyedByAgency() {
		AtomicLong now = new AtomicLong(1_000_000L);
		DataCache cache = new DataCache(now::get);
		FeedMessage a = canned();
		FeedMessage b = canned();

		cache.put("agency-a", a);
		cache.put("agency-b", b);

		assertThat(cache.get("agency-a", CACHE_SECS)).isSameAs(a);
		assertThat(cache.get("agency-b", CACHE_SECS)).isSameAs(b);
		assertThat(cache.get("agency-c", CACHE_SECS)).isNull();
	}
}
