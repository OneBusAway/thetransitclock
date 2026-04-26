package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.hibernate.Session;
import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.configData.AgencyConfig;
import org.transitclock.core.AvlProcessor;
import org.transitclock.db.hibernate.HibernateUtils;
import org.transitclock.db.structs.ArrivalDeparture;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;

/**
 * Behavior tests that verify pipeline output reaches the database via
 * {@link org.transitclock.db.hibernate.DataDbLogger} — the async batched
 * writer that feeds the actual DB tables.
 *
 * <p>DataDbLogger has no public flush, so tests poll the DB with a bounded
 * timeout — see {@link #waitForDbRows}.
 */
public class DbPersistenceBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String BLOCK_ID = "SE-08";
	private static final String TRIP_ID = "868588900";

	private static final String STOP_FIRST_ID = "14253";
	private static final double STOP_FIRST_LAT = 38.953562;
	private static final double STOP_FIRST_LON = -77.447485;

	private static final String STOP_NEXT_ID = "13056";
	private static final double STOP_NEXT_LAT = 38.951704;
	private static final double STOP_NEXT_LON = -77.383009;

	/** 2016-06-20 11:50:00 America/New_York → 15:50:00 UTC. */
	private static final long AVL_AT_FIRST_STOP_EPOCH_MS = 1466437800000L;

	private static final AtomicLong nextTime = new AtomicLong(AVL_AT_FIRST_STOP_EPOCH_MS);

	private static final long DB_FLUSH_TIMEOUT_MS = 10_000L;
	private static final long DB_POLL_INTERVAL_MS = 100L;

	private static AvlReport avlReport(String vehicleId, double lat, double lon, long timeMs) {
		return new AvlReport(vehicleId, timeMs, lat, lon,
				Float.NaN, Float.NaN, "test");
	}

	private static void advanceVehicleFromFirstStopToNext(String vehicleId) {
		long t1 = nextTime.getAndAdd(1);
		CORE.setNow(t1);
		AvlReport first = avlReport(vehicleId, STOP_FIRST_LAT, STOP_FIRST_LON, t1);
		first.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(first);

		long t2 = t1 + 8L * 60_000L;
		// Bump the shared counter past t2 so no later test's getAndAdd
		// hands back a baseline that's already inside this test's span.
		nextTime.updateAndGet(cur -> Math.max(cur, t2 + 1));
		CORE.setNow(t2);
		AvlReport second = avlReport(vehicleId, STOP_NEXT_LAT, STOP_NEXT_LON, t2);
		second.setAssignment(BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(second);
	}

	private static List<ArrivalDeparture> queryArrivalDeparturesForVehicle(String vehicleId) {
		try (Session session = HibernateUtils.getSession(AgencyConfig.getAgencyId())) {
			return session.createQuery(
					"from ArrivalDeparture where vehicleId = :vehicleId",
					ArrivalDeparture.class)
					.setParameter("vehicleId", vehicleId)
					.getResultList();
		}
	}

	private static List<AvlReport> queryAvlReportsForVehicle(String vehicleId) {
		try (Session session = HibernateUtils.getSession(AgencyConfig.getAgencyId())) {
			return session.createQuery(
					"from AvlReport where vehicleId = :vehicleId",
					AvlReport.class)
					.setParameter("vehicleId", vehicleId)
					.getResultList();
		}
	}

	/** Polls {@code fetcher} until at least {@code minRows} are returned or
	 *  the timeout elapses. Returns the final list (possibly short — callers
	 *  assert on size). Throws AssertionError if the poll is interrupted
	 *  rather than returning a short list silently. */
	private static <T> List<T> waitForDbRows(Supplier<List<T>> fetcher, int minRows) {
		long deadline = System.currentTimeMillis() + DB_FLUSH_TIMEOUT_MS;
		List<T> rows = fetcher.get();
		while (rows.size() < minRows && System.currentTimeMillis() < deadline) {
			try {
				Thread.sleep(DB_POLL_INTERVAL_MS);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				throw new AssertionError(
						"waitForDbRows interrupted before reaching minRows=" + minRows
								+ " (last observed size=" + rows.size() + ")", ie);
			}
			rows = fetcher.get();
		}
		return rows;
	}

	// ---------- Tests ----------

	@Test
	public void avlReportsAreWrittenToDbAfterProcessing() {
		advanceVehicleFromFirstStopToNext("v-db-avl");

		List<AvlReport> rows = waitForDbRows(
				() -> queryAvlReportsForVehicle("v-db-avl"), 2);
		assertThat(rows)
				.as("both AVL reports for the vehicle should be persisted within the timeout")
				.hasSizeGreaterThanOrEqualTo(2);
	}

	@Test
	public void arrivalDepartureRecordsAreWrittenToDbAfterAdvancement() {
		advanceVehicleFromFirstStopToNext("v-db-ad");

		List<ArrivalDeparture> rows = waitForDbRows(
				() -> queryArrivalDeparturesForVehicle("v-db-ad"), 1);
		assertThat(rows)
				.as("at least one ArrivalDeparture row for the vehicle should be persisted")
				.isNotEmpty();
	}

	@Test
	public void persistedArrivalDepartureRecordsCarryExpectedMetadata() {
		advanceVehicleFromFirstStopToNext("v-db-meta");

		List<ArrivalDeparture> rows = waitForDbRows(
				() -> queryArrivalDeparturesForVehicle("v-db-meta"), 1);
		assertThat(rows).isNotEmpty();

		ArrivalDeparture first = rows.get(0);
		assertThat(first.getVehicleId()).isEqualTo("v-db-meta");
		assertThat(first.getBlockId()).isEqualTo(BLOCK_ID);
		assertThat(first.getTripId()).isEqualTo(TRIP_ID);
		assertThat(first.getStopId())
				.as("the first row should be at one of the two stops we advanced through")
				.isIn(STOP_FIRST_ID, STOP_NEXT_ID);
	}

	@Test
	public void bothArrivalAndDepartureVariantsArePersistedForTheAdvancement() {
		advanceVehicleFromFirstStopToNext("v-db-variants");

		List<ArrivalDeparture> rows = waitForDbRows(
				() -> queryArrivalDeparturesForVehicle("v-db-variants"), 2);
		assertThat(rows)
				.as("at least two records (for the advancement) should persist")
				.hasSizeGreaterThanOrEqualTo(2);

		boolean hasArrival = rows.stream().anyMatch(ArrivalDeparture::isArrival);
		boolean hasDeparture = rows.stream().anyMatch(ad -> !ad.isArrival());
		assertThat(hasArrival)
				.as("at least one persisted record should be an arrival")
				.isTrue();
		assertThat(hasDeparture)
				.as("at least one persisted record should be a departure")
				.isTrue();
	}
}
