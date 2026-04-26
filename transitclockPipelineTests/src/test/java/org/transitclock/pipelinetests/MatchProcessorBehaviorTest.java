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
import org.transitclock.core.MatchProcessor;
import org.transitclock.core.VehicleState;
import org.transitclock.core.dataCache.VehicleStateManager;
import org.transitclock.db.hibernate.HibernateUtils;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.AvlReport.AssignmentType;
import org.transitclock.db.structs.Prediction;
import org.transitclock.ipc.data.IpcPrediction;

/**
 * Behavior tests for {@link MatchProcessor#generateResultsOfMatch(VehicleState)}
 * against the real WMATA 5A fixture. MatchProcessor had no direct coverage
 * before this file — its behavior was exercised only incidentally through
 * {@code AvlProcessor}.
 */
public class MatchProcessorBehaviorTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	private static final String HAPPY_PATH_BLOCK_ID = "SE-08";
	private static final double HAPPY_PATH_LAT = 38.953562;
	private static final double HAPPY_PATH_LON = -77.447485;
	/** 2016-06-20 11:50:00 America/New_York — mirrors AvlProcessorBehaviorTest. */
	private static final long HAPPY_PATH_EPOCH_MS = 1466437800000L;

	/** Keeps AVL timestamps strictly monotonic across tests so that
	 *  {@code AvlProcessor#setLastAvlReport}'s "only store newer" guard
	 *  never silently rejects a test's setup report. */
	private static final AtomicLong nextTime = new AtomicLong(HAPPY_PATH_EPOCH_MS);

	private static final long DB_FLUSH_TIMEOUT_MS = 10_000L;
	private static final long DB_POLL_INTERVAL_MS = 100L;

	private static VehicleState happyMatchState(String vehicleId) {
		long when = nextTime.updateAndGet(cur -> Math.max(cur, HAPPY_PATH_EPOCH_MS) + 1_000L);
		CORE.setNow(when);

		AvlReport report = new AvlReport(vehicleId, when,
				HAPPY_PATH_LAT, HAPPY_PATH_LON,
				Float.NaN, Float.NaN, "test");
		report.setAssignment(HAPPY_PATH_BLOCK_ID, AssignmentType.BLOCK_ID);
		AvlProcessor.getInstance().processAvlReport(report);

		VehicleState state = VehicleStateManager.getInstance().getVehicleState(vehicleId);
		assertThat(state)
				.as("VehicleStateManager should yield a state for %s after processing", vehicleId)
				.isNotNull();
		assertThat(state.isPredictable())
				.as("setup assumption: happy path should produce a predictable vehicle")
				.isTrue();
		assertThat(state.getMatch())
				.as("setup assumption: happy path should produce a TemporalMatch")
				.isNotNull();
		return state;
	}

	private static List<Prediction> queryPredictionsForVehicle(String vehicleId) {
		try (Session session = HibernateUtils.getSession(AgencyConfig.getAgencyId())) {
			return session.createQuery(
					"from Prediction where vehicleId = :vehicleId",
					Prediction.class)
					.setParameter("vehicleId", vehicleId)
					.getResultList();
		}
	}

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
	public void getInstance_returnsSameSingleton() {
		assertThat(MatchProcessor.getInstance())
				.isNotNull()
				.isSameAs(MatchProcessor.getInstance());
	}

	@Test
	public void generateResultsOfMatch_unpredictableVehicleIsNoOp() {
		VehicleState state = new VehicleState("v-mp-unpred");
		assertThat(state.isPredictable())
				.as("setup assumption: new VehicleState is unpredictable")
				.isFalse();

		MatchProcessor.getInstance().generateResultsOfMatch(state);

		assertThat(state.getPredictions())
				.as("unpredictable vehicle must not have predictions populated")
				.isNull();
		assertThat(state.getHeadway())
				.as("unpredictable vehicle must not have a headway set")
				.isNull();
		assertThat(queryPredictionsForVehicle("v-mp-unpred"))
				.as("unpredictable vehicle must not queue Prediction rows to the DB")
				.isEmpty();
	}

	@Test
	public void generateResultsOfMatch_predictableVehiclePopulatesPredictions() {
		// AvlProcessor already invoked generateResultsOfMatch during setup; clear
		// the list so the next assertion proves this call repopulated it.
		VehicleState state = happyMatchState("v-mp-populates");
		state.setPredictions(null);

		MatchProcessor.getInstance().generateResultsOfMatch(state);

		List<IpcPrediction> predictions = state.getPredictions();
		assertThat(predictions)
				.as("MatchProcessor should repopulate predictions for a predictable vehicle")
				.isNotNull()
				.isNotEmpty();
		assertThat(predictions)
				.as("every emitted prediction should be tagged with this vehicle")
				.allMatch(p -> "v-mp-populates".equals(p.getVehicleId()));
	}

	@Test
	public void generateResultsOfMatch_predictableVehicleQueuesPredictionsToDb() {
		// Proves the async DbLogger path in processPredictions ran.
		VehicleState state = happyMatchState("v-mp-dbrows");
		state.setPredictions(null);

		MatchProcessor.getInstance().generateResultsOfMatch(state);

		List<Prediction> rows = waitForDbRows(
				() -> queryPredictionsForVehicle("v-mp-dbrows"), 1);
		assertThat(rows)
				.as("at least one Prediction row for the vehicle should reach the DB within the timeout")
				.isNotEmpty();
	}
}
