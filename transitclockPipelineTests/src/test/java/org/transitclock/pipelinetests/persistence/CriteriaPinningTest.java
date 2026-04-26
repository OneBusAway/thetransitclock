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
import java.util.Map;

import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.configData.AgencyConfig;
import org.transitclock.db.hibernate.HibernateUtils;
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
 * <p>Each test invokes a production query method, exercising it against the
 * in-memory HSQL database loaded by {@link CoreHarness}. The assertions are
 * deliberately conservative: we pin "the call returns successfully and gives
 * a usable shape," not an exact row count, because the WMATA 5A fixture is
 * static and most tables are empty (no AVL has been processed). After the
 * Hibernate 6 / jakarta.persistence.criteria rewrite, every test in this
 * class must still pass.
 */
public class CriteriaPinningTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static Session openSession() {
		return HibernateUtils.getSession(AgencyConfig.getAgencyId());
	}

	private static Date instant(long epochMs) {
		return new Date(epochMs);
	}

	/** Window covering the WMATA 5A fixture's service date. */
	private static final Date WINDOW_BEGIN = instant(1_466_400_000_000L); // 2016-06-20T08:00:00Z
	private static final Date WINDOW_END = instant(1_466_500_000_000L);   // 2016-06-21T11:46:40Z

	// ----- ArrivalDeparture: tripId+serviceId variant (line 643) -----

	@Test
	public void arrivalDepartureByTripAndService() {
		try (Session session = openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					"868588900", "1");
			assertThat(rows).isNotNull();
		}
	}

	@Test
	public void arrivalDepartureByTripAndServiceTreatsServiceIdAsOptional() {
		try (Session session = openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					"868588900", (String) null);
			assertThat(rows).isNotNull();
		}
	}

	// ----- ArrivalDeparture: tripId+stopPathIndex variant (line 669) -----

	@Test
	public void arrivalDepartureByTripAndStopPathIndex() {
		try (Session session = openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					"868588900", Integer.valueOf(0));
			assertThat(rows).isNotNull();
		}
	}

	@Test
	public void arrivalDepartureByTripAndStopPathIndexTreatsBothAsOptional() {
		try (Session session = openSession()) {
			List<ArrivalDeparture> rows = ArrivalDeparture.getArrivalsDeparturesFromDb(
					session, WINDOW_BEGIN, WINDOW_END,
					null, (Integer) null);
			assertThat(rows).isNotNull();
		}
	}

	// ----- PredictionForStopPath (line 214) -----

	@Test
	public void predictionForStopPathAllFiltersApplied() {
		List<PredictionForStopPath> rows = PredictionForStopPath
				.getPredictionForStopPathFromDB(WINDOW_BEGIN, WINDOW_END,
						"some-algo", "868588900", Integer.valueOf(0));
		assertThat(rows).isNotNull();
	}

	@Test
	public void predictionForStopPathAllFiltersOptional() {
		List<PredictionForStopPath> rows = PredictionForStopPath
				.getPredictionForStopPathFromDB(null, null, null, null, null);
		assertThat(rows).isNotNull();
	}

	// ----- TravelTimesForTrip (line 230) -----

	@Test
	public void travelTimesForTripsByRevisionReturnsMap() {
		try (Session session = openSession()) {
			Map<String, List<TravelTimesForTrip>> map =
					TravelTimesForTrip.getTravelTimesForTrips(session, 0);
			assertThat(map).isNotNull();
		}
	}
}
