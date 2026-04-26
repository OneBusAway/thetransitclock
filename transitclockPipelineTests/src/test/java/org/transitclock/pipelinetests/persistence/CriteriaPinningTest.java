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
import java.util.Map;

import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.db.structs.ArrivalDeparture;
import org.transitclock.db.structs.PredictionForStopPath;
import org.transitclock.db.structs.TravelTimesForTrip;
import org.transitclock.pipelinetests.CoreHarness;

/**
 * Pins behavior of every call site that uses Hibernate's <em>legacy</em>
 * Criteria API ({@code org.hibernate.Criteria}, {@code Restrictions},
 * {@code Order}). The legacy API is removed in Hibernate 6.0 — these tests
 * are the spec the JPA Criteria rewrite has to pass.
 *
 * <p>Each test invokes the production query method against the in-memory
 * HSQL database booted by {@link CoreHarness}. Where possible we seed two
 * rows — one matching the filter, one not — so that a "predicate dropped"
 * regression in the rewrite is caught (an unfiltered query would return
 * both). Where the production constructor needs a fully-set-up Core
 * (Arrival/Departure require a {@code Block}), we fall back to a
 * shape-only assertion and document the limitation.
 */
public class CriteriaPinningTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	/** Window covering the WMATA 5A fixture's service date. */
	private static final Date WINDOW_BEGIN = new Date(1_466_400_000_000L); // 2016-06-20T08:00:00Z
	private static final Date WINDOW_END = new Date(1_466_500_000_000L);   // 2016-06-21T11:46:40Z

	// ----- ArrivalDeparture: tripId+serviceId variant -----
	// The protected ArrivalDeparture constructors require a fully-populated
	// Block, which only exists once a Core has matched a vehicle to a route.
	// Seeding via the constructor would mean replaying an AVL through
	// AvlProcessor — that path is exercised by DbPersistenceBehaviorTest.
	// Here we go around the constructors with a raw INSERT so a "predicate
	// dropped" regression in the rewrite shows up as wrong row count rather
	// than as a silent pass.

	private static void seedArrivalDeparture(Session s, String vehicleId,
			Date time, String tripId, String serviceId, String stopId) {
		// ArrivalDeparture uses single-table inheritance with subclasses
		// Arrival/Departure, so DTYPE is NOT NULL. We seed as 'Arrival'.
		s.createNativeMutationQuery(
				"insert into ArrivalsDepartures "
				+ "(dtype, vehicleId, time, stopId, gtfsStopSeq, isArrival, "
				+ " tripId, serviceId, configRev, tripIndex, stopPathIndex, "
				+ " stopPathLength) "
				+ "values ('Arrival', :vid, :t, :stop, 0, true, :trip, :svc, "
				+ "        0, 0, 0, 0.0)")
				.setParameter("vid", vehicleId)
				.setParameter("t", time)
				.setParameter("stop", stopId)
				.setParameter("trip", tripId)
				.setParameter("svc", serviceId)
				.executeUpdate();
	}

	@Test
	public void arrivalDepartureTripIdFilterIsApplied() {
		String matchTrip = "trip-ad-match";
		String otherTrip = "trip-ad-other";
		Date inWindow = new Date(WINDOW_BEGIN.getTime() + 1_000L);

		inSessionWithCommit(s -> {
			seedArrivalDeparture(s, "v-ad-1", inWindow, matchTrip, "svc-1", "stop-A");
			seedArrivalDeparture(s, "v-ad-2", inWindow, otherTrip, "svc-1", "stop-B");
			return null;
		});

		try (Session session = PersistenceTestSupport.openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END, matchTrip, "svc-1");
			assertThat(rows).hasSizeGreaterThanOrEqualTo(1);
			assertThat(rows).allMatch(ad -> matchTrip.equals(ad.getTripId()),
					"every row matches the requested tripId");
		}
	}

	@Test
	public void arrivalDepartureByTripAndService() {
		try (Session session = PersistenceTestSupport.openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					"868588900", "1");
			assertThat(rows).isNotNull();
		}
	}

	@Test
	public void arrivalDepartureByTripAndServiceTreatsServiceIdAsOptional() {
		try (Session session = PersistenceTestSupport.openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					"868588900", (String) null);
			assertThat(rows).isNotNull();
		}
	}

	@Test
	public void arrivalDepartureByTripAndStopPathIndex() {
		try (Session session = PersistenceTestSupport.openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					"868588900", Integer.valueOf(0));
			assertThat(rows).isNotNull();
		}
	}

	@Test
	public void arrivalDepartureByTripAndStopPathIndexTreatsBothAsOptional() {
		try (Session session = PersistenceTestSupport.openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					null, (Integer) null);
			assertThat(rows).isNotNull();
		}
	}

	// ----- PredictionForStopPath -----
	// Seed two rows (one matching filter, one not) so a "predicate dropped"
	// regression in the rewrite gives wrong row count, not silent pass.

	@Test
	public void predictionForStopPathFilterIsApplied() {
		String matchTrip = "trip-criteria-match";
		String otherTrip = "trip-criteria-other";
		String algorithm = "PHA";
		Date inWindow = new Date(WINDOW_BEGIN.getTime() + 1_000L);

		inSessionWithCommit(s -> {
			s.save(new PredictionForStopPath("v-c1", inWindow, 60.0,
					matchTrip, 0, algorithm, false, 1));
			s.save(new PredictionForStopPath("v-c2", inWindow, 60.0,
					otherTrip, 0, algorithm, false, 1));
			return null;
		});

		List<PredictionForStopPath> rows = PredictionForStopPath
				.getPredictionForStopPathFromDB(WINDOW_BEGIN, WINDOW_END,
						algorithm, matchTrip, Integer.valueOf(0));

		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getTripId()).isEqualTo(matchTrip);
	}

	@Test
	public void predictionForStopPathAllFiltersOptional() {
		List<PredictionForStopPath> rows = PredictionForStopPath
				.getPredictionForStopPathFromDB(null, null, null, null, null);
		// Optional-everything path: should not throw and should return at
		// least the rows persisted by predictionForStopPathFilterIsApplied
		// (within-class test ordering is not guaranteed; we only assert
		// non-null shape).
		assertThat(rows).isNotNull();
	}

	// ----- TravelTimesForTrip -----
	// Seed two rows with different travelTimesRev so a "predicate dropped"
	// regression in the rewrite returns wrong row count, not silent pass.

	@Test
	public void travelTimesForTripsByRevisionFiltersByRev() {
		int matchRev = 9991;
		int otherRev = 9992;
		String matchPattern = "pattern-pin-match";
		String otherPattern = "pattern-pin-other";

		inSessionWithCommit(s -> {
			s.createNativeMutationQuery(
					"insert into TravelTimesForTrips "
					+ "(id, travelTimesRev, configRev, tripPatternId, tripCreatedForId) "
					+ "values (:id, :rev, 0, :pat, :trip)")
					.setParameter("id", 999_991)
					.setParameter("rev", matchRev)
					.setParameter("pat", matchPattern)
					.setParameter("trip", "trip-ttft-match")
					.executeUpdate();
			s.createNativeMutationQuery(
					"insert into TravelTimesForTrips "
					+ "(id, travelTimesRev, configRev, tripPatternId, tripCreatedForId) "
					+ "values (:id, :rev, 0, :pat, :trip)")
					.setParameter("id", 999_992)
					.setParameter("rev", otherRev)
					.setParameter("pat", otherPattern)
					.setParameter("trip", "trip-ttft-other")
					.executeUpdate();
			return null;
		});

		try (Session session = PersistenceTestSupport.openSession()) {
			Map<String, List<TravelTimesForTrip>> map =
					TravelTimesForTrip.getTravelTimesForTrips(session, matchRev);
			assertThat(map).containsKey(matchPattern);
			assertThat(map).doesNotContainKey(otherPattern);
		}
	}

	@Test
	public void travelTimesForTripsByRevisionReturnsEmptyMapForUnknownRev() {
		try (Session session = PersistenceTestSupport.openSession()) {
			Map<String, List<TravelTimesForTrip>> map =
					TravelTimesForTrip.getTravelTimesForTrips(session, -1);
			assertThat(map).isEmpty();
		}
	}

	// ----- ScheduleAdherenceController / AvlJsonQuery time-cast portability ---
	// These reports filter ArrivalsDepartures by `cast(time as time) between
	// :start and :end` so the WHERE clause works on Postgres, MySQL, and
	// HSQL. The earlier MySQL-only `time(time)` form would silently parse on
	// HSQL but error on Postgres. Smoke-execute the SQL-standard cast here
	// so a regression to a vendor-specific function is caught at test time.

	@Test
	public void castTimeAsTimeIsAcceptedByHsql() {
		Date inWindow = new Date(WINDOW_BEGIN.getTime() + 5_000L);
		inSessionWithCommit(s -> {
			seedArrivalDeparture(s, "v-time-1", inWindow,
					"trip-time-1", "svc-time-1", "stop-T");
			return null;
		});

		// HSQL strict typing requires HH:MM:SS; Postgres/MySQL accept HH:MM
		// too. The production callers (ScheduleAdherenceController /
		// AvlJsonQuery) target Postgres and MySQL, so the controller-side
		// HH:MM format is fine in production — the cast itself is the
		// portability fix being pinned here.
		try (Session session = PersistenceTestSupport.openSession()) {
			Number count = (Number) session.createNativeQuery(
					"select count(*) from ArrivalsDepartures "
					+ "where cast(time as time) between "
					+ "cast('00:00:00' as time) and cast('23:59:59' as time)")
					.uniqueResult();
			assertThat(count.intValue()).isGreaterThanOrEqualTo(1);
		}
	}

	@SuppressWarnings("unused")
	private static <T> T loadOne(Class<T> entity) {
		return inSession(s -> {
			List<T> rows = s.createQuery(
					"from " + entity.getSimpleName(), entity)
					.getResultList();
			return rows.isEmpty() ? null : rows.get(0);
		});
	}
}
