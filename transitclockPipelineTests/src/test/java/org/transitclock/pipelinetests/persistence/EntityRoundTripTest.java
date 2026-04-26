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

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.configData.AgencyConfig;
import org.transitclock.db.hibernate.HibernateUtils;
import org.transitclock.db.structs.Headway;
import org.transitclock.db.structs.HoldingTime;
import org.transitclock.db.structs.MeasuredArrivalTime;
import org.transitclock.db.structs.PredictionForStopPath;
import org.transitclock.db.webstructs.ApiKey;
import org.transitclock.pipelinetests.CoreHarness;

/**
 * Save → flush → fresh-session-read for a representative subset of
 * {@code @Entity} classes. Catches Hibernate-6 type-mapping regressions
 * (boolean/enum/temporal) on the entities the runtime writes.
 *
 * <p>{@code DbPersistenceBehaviorTest} already round-trips {@code AvlReport}
 * and {@code ArrivalDeparture} through {@code DataDbLogger}; GTFS-config
 * entities ({@code Route}, {@code Stop}, {@code Trip}, etc.) are loaded by
 * {@link CoreHarness} as part of the WMATA 5A fixture import, which is
 * itself a round-trip exercising those mappings. This class fills the gap.
 */
public class EntityRoundTripTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static <T> T inSessionWithCommit(Function<Session, T> body) {
		try (Session session = HibernateUtils.getSession(AgencyConfig.getAgencyId())) {
			Transaction tx = session.beginTransaction();
			try {
				T result = body.apply(session);
				tx.commit();
				return result;
			} catch (RuntimeException e) {
				tx.rollback();
				throw e;
			}
		}
	}

	private static <T> T inSession(Function<Session, T> body) {
		try (Session session = HibernateUtils.getSession(AgencyConfig.getAgencyId())) {
			return body.apply(session);
		}
	}

	@Test
	public void apiKeyRoundTrip() {
		String key = "k-" + Long.toHexString(System.nanoTime());
		ApiKey toSave = new ApiKey("test-app", key,
				"https://example.com", "owner@example.com", "555-1234", "description");

		inSessionWithCommit(s -> {
			s.save(toSave);
			return null;
		});

		// Java field is applicationKey; the public getter is getKey().
		@SuppressWarnings("unchecked")
		List<ApiKey> rows = (List<ApiKey>) inSession(s ->
				s.createCriteria(ApiKey.class)
						.add(Restrictions.eq("applicationKey", key))
						.list());

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
			s.flush();
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

		@SuppressWarnings("unchecked")
		List<HoldingTime> rows = (List<HoldingTime>) inSession(s ->
				s.createCriteria(HoldingTime.class)
						.add(Restrictions.eq("vehicleId", "v-hold"))
						.list());

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

		@SuppressWarnings("unchecked")
		List<PredictionForStopPath> rows = (List<PredictionForStopPath>) inSession(s ->
				s.createCriteria(PredictionForStopPath.class)
						.add(Restrictions.eq("vehicleId", "v-rt"))
						.list());

		assertThat(rows).hasSize(1);
		PredictionForStopPath loaded = rows.get(0);
		assertThat(loaded.getTripId()).isEqualTo("trip-1");
		assertThat(loaded.getStopPathIndex()).isEqualTo(0);
		assertThat(loaded.getAlgorithm()).isEqualTo("PHA");
		assertThat(loaded.getPredictionTime()).isEqualTo(60.0);
	}

	@Test
	public void measuredArrivalTimeRoundTrip() {
		MeasuredArrivalTime toSave = new MeasuredArrivalTime(
				new Date(), "stop-mat", "route-1", "rsn-1", "head", "dir");

		inSessionWithCommit(s -> {
			s.save(toSave);
			return null;
		});

		// MeasuredArrivalTime has no public getters (it's written by the
		// website via raw SQL). Pin the round-trip by row count alone.
		@SuppressWarnings("unchecked")
		List<MeasuredArrivalTime> rows = (List<MeasuredArrivalTime>) inSession(s ->
				s.createCriteria(MeasuredArrivalTime.class)
						.add(Restrictions.eq("stopId", "stop-mat"))
						.list());

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0)).isNotNull();
	}
}
