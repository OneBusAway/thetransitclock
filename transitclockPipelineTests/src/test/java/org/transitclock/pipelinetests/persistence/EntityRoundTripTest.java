/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.pipelinetests.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.transitclock.pipelinetests.persistence.PersistenceTestSupport.inSession;
import static org.transitclock.pipelinetests.persistence.PersistenceTestSupport.inSessionWithCommit;

import java.util.Date;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.db.structs.Headway;
import org.transitclock.db.structs.HoldingTime;
import org.transitclock.db.structs.PredictionForStopPath;
import org.transitclock.db.webstructs.ApiKey;
import org.transitclock.pipelinetests.CoreHarness;

/**
 * Save → flush → fresh-session-read for a representative subset of
 * runtime-writable {@code @Entity} classes:
 * {@code ApiKey}, {@code Headway}, {@code HoldingTime},
 * {@code PredictionForStopPath}.
 * Catches Hibernate-6 type-mapping regressions
 * (boolean / temporal / integer column types) on entities the existing
 * pipeline tests don't already touch.
 *
 * <p>{@code DbPersistenceBehaviorTest} already round-trips {@code AvlReport}
 * and {@code ArrivalDeparture} through the {@code DataDbLogger} pipeline.
 * GTFS-config entities ({@code Route}, {@code Stop}, {@code Trip}, etc.) are
 * loaded by {@link CoreHarness} as part of the WMATA 5A fixture import,
 * which is itself a round-trip exercising those mappings.
 */
public class EntityRoundTripTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	@Test
	public void apiKeyRoundTrip() {
		// The @Id is applicationKey (a 20-char column), not a generated
		// surrogate. nanoTime hex suffix avoids PK collision if the fixture
		// or another test in this class persists an "applicationKey" first.
		String key = "k-" + Long.toHexString(System.nanoTime());
		ApiKey toSave = new ApiKey("test-app", key,
				"https://example.com", "owner@example.com", "555-1234", "description");

		inSessionWithCommit(s -> {
			s.save(toSave);
			return null;
		});

		// Java field is applicationKey; the public getter is getKey().
		List<ApiKey> rows = inSession(s ->
				s.createQuery(
						"from ApiKey where applicationKey = :key", ApiKey.class)
						.setParameter("key", key)
						.getResultList());

		assertThat(rows).hasSize(1);
		ApiKey loaded = rows.get(0);
		assertThat(loaded.getApplicationName()).isEqualTo("test-app");
		assertThat(loaded.getKey()).isEqualTo(key);
		assertThat(loaded.getApplicationUrl()).isEqualTo("https://example.com");
		assertThat(loaded.getEmail()).isEqualTo("owner@example.com");
		assertThat(loaded.getPhone()).isEqualTo("555-1234");
		assertThat(loaded.getDescription()).isEqualTo("description");
	}

	@Test
	public void headwayRoundTrip() {
		long headwayValue = 12345L;
		Date now = new Date();
		Headway toSave = new Headway(headwayValue, now,
				"v-headway", "v-other", "stop-1", "trip-1", "route-1",
				now, now);

		Long id = inSessionWithCommit(s -> {
			s.save(toSave);
			return toSave.getId();
		});

		Headway loaded = inSession(s -> s.get(Headway.class, id));

		assertThat(loaded).isNotNull();
		assertThat(loaded.getHeadway()).isEqualTo(headwayValue);
		assertThat(loaded.getVehicleId()).isEqualTo("v-headway");
		assertThat(loaded.getOtherVehicleId()).isEqualTo("v-other");
		assertThat(loaded.getStopId()).isEqualTo("stop-1");
		assertThat(loaded.getTripId()).isEqualTo("trip-1");
		assertThat(loaded.getRouteId()).isEqualTo("route-1");
	}

	@Test
	public void holdingTimeRoundTrip() {
		Date now = new Date();
		HoldingTime toSave = new HoldingTime(now, now,
				"v-hold", "stop-1", "trip-1", "route-1",
				false, true, now, false, 2);

		inSessionWithCommit(s -> {
			s.save(toSave);
			return null;
		});

		List<HoldingTime> rows = inSession(s ->
				s.createQuery(
						"from HoldingTime where vehicleId = :vehicleId", HoldingTime.class)
						.setParameter("vehicleId", "v-hold")
						.getResultList());

		assertThat(rows).hasSize(1);
		HoldingTime loaded = rows.get(0);
		assertThat(loaded.getStopId()).isEqualTo("stop-1");
		assertThat(loaded.isArrivalPredictionUsed()).isFalse();
		assertThat(loaded.isArrivalUsed()).isTrue();
		assertThat(loaded.getNumberPredictionsUsed()).isEqualTo(2);
	}

	@Test
	public void predictionForStopPathRoundTrip() {
		PredictionForStopPath toSave = new PredictionForStopPath(
				"v-rt", new Date(), 60.0, "trip-1", 0, "PHA", false, 1);

		inSessionWithCommit(s -> {
			s.save(toSave);
			return null;
		});

		List<PredictionForStopPath> rows = inSession(s ->
				s.createQuery(
						"from PredictionForStopPath where vehicleId = :vehicleId",
						PredictionForStopPath.class)
						.setParameter("vehicleId", "v-rt")
						.getResultList());

		assertThat(rows).hasSize(1);
		PredictionForStopPath loaded = rows.get(0);
		assertThat(loaded.getTripId()).isEqualTo("trip-1");
		assertThat(loaded.getStopPathIndex()).isEqualTo(0);
		assertThat(loaded.getAlgorithm()).isEqualTo("PHA");
		assertThat(loaded.getPredictionTime()).isEqualTo(60.0);
	}
}
