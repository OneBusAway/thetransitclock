package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.db.structs.Block;
import org.transitclock.db.structs.ScheduleTime;
import org.transitclock.db.structs.Trip;
import org.transitclock.gtfs.DbConfig;

/**
 * Phase B's Hibernate 6.5 upgrade replaced
 * {@code ResourceRegistryStandardImpl}'s tolerant Hibernate-5 internals with
 * an unsynchronized {@code HashMap} that's iterated during JDBC-resource
 * cleanup. Two of {@code DbConfig}'s {@code globalSession} readers
 * ({@link Trip#getScheduleTime(int)} and the {@code ValidateSessionThread}
 * probe) were touching the session without synchronizing on
 * {@code Block#lazyLoadingSyncObject}, the lock the rest of the codebase
 * already used. Under live AVL load the unsynced callers raced the matcher
 * and threw {@code ConcurrentModificationException} in
 * {@code releaseResources} / {@code PSQLException: This ResultSet is closed},
 * silently killing the matcher worker thread — what made block assignment
 * sit at 0% for the user-visible regression.
 *
 * <p>This test reproduces the race deterministically by hammering
 * {@code DbConfig.getTrip(...)} and {@link Trip#getScheduleTime(int)} from
 * many threads against the shared {@code globalSession}. The pre-fix code
 * trips a {@code ConcurrentModificationException} or
 * {@code GenericJDBCException} within seconds; the post-fix code completes
 * cleanly. HSQL behaves the same as PostgreSQL here because the bug is in
 * Hibernate's per-session resource registry, not the JDBC driver.
 */
public class GlobalSessionConcurrencyTest {

    @ClassRule
    public static final CoreHarness CORE = CoreHarness.withWmata5A();

    private static final int THREAD_COUNT = 32;
    private static final long DURATION_MS = 8_000L;

    @Test
    public void concurrentGlobalSessionReadersDoNotRaceUnderLoad() throws Exception {
        DbConfig dbConfig = CORE.dbConfig();

        // Block.getTrips() is the lazy ManyToMany path; the Trips it returns
        // do NOT have scheduledTimesList eager-fetched (unlike Trip.getTrip's
        // explicit "left join fetch" query), so Trip.getScheduleTime() on
        // them really does have to hit the DB the first time. That's the
        // exact code path that raced in production. Sampling a stable list
        // up-front so all worker threads can hammer the same Trip instances
        // — the bug fires when two threads' Hibernate operations on those
        // shared instances overlap mid-cleanup.
        List<Block> blocks = new ArrayList<>();
        for (Block b : dbConfig.getBlocks()) {
            blocks.add(b);
            if (blocks.size() >= 16) break;
        }
        assertThat(blocks)
                .as("Need at least one block in the fixture to exercise the race")
                .isNotEmpty();

        // Pre-resolve the trips so every worker iterates the same shared
        // Trip instances. Using a Set keeps the work-set bounded.
        List<Trip> trips = new ArrayList<>();
        for (Block b : blocks) {
            for (Trip t : b.getTrips()) {
                trips.add(t);
                if (trips.size() >= 64) break;
            }
            if (trips.size() >= 64) break;
        }
        assertThat(trips).isNotEmpty();

        // Sample distinct trip-ids for the synced cache-miss path too —
        // mixing synced (DbConfig.getTrip) and unsynced (Trip.getScheduleTime)
        // callers is what produced the production stack trace.
        List<String> tripIds = new ArrayList<>();
        Iterator<Trip> tripIt = dbConfig.getTrips().values().iterator();
        while (tripIt.hasNext() && tripIds.size() < 40) {
            tripIds.add(tripIt.next().getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch go = new CountDownLatch(1);
        AtomicLong opCount = new AtomicLong();
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());

        long deadline = System.currentTimeMillis() + DURATION_MS;
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int seed = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    int idx = seed;
                    while (System.currentTimeMillis() < deadline) {
                        try {
                            // Unsynced lazy-load (the bug site): Trip
                            // instances reached via Block.getTrips() have
                            // their scheduledTimesList still as a lazy
                            // PersistentList. Calling getScheduleTime
                            // triggers the DB hit on globalSession, which
                            // races against the synced reader below.
                            Trip t = trips.get(Math.floorMod(idx, trips.size()));
                            try {
                                ScheduleTime s = t.getScheduleTime(0);
                                if (s != null) opCount.incrementAndGet();
                            } catch (IndexOutOfBoundsException ignore) {
                                // Some trips may have empty scheduledTimesList
                                // for the chosen index; not interesting here.
                            }

                            // Synced reader on the same globalSession.
                            String tripId = tripIds.get(Math.floorMod(idx, tripIds.size()));
                            Trip resolved = dbConfig.getTrip(tripId);
                            if (resolved != null) opCount.incrementAndGet();

                            // Block.getTrips() is the third path that lazy-
                            // loads against globalSession. Calling it on
                            // every iteration mostly hits the cached
                            // PersistentList after the first round, but
                            // the first-call path is the racy one.
                            Block b = blocks.get(Math.floorMod(idx, blocks.size()));
                            if (!b.getTrips().isEmpty()) opCount.incrementAndGet();

                            idx++;
                        } catch (Throwable t) {
                            failures.add(t);
                            return;
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await(10, TimeUnit.SECONDS);
        go.countDown();
        pool.shutdown();
        boolean clean = pool.awaitTermination(DURATION_MS + 30_000L, TimeUnit.MILLISECONDS);
        assertThat(clean)
                .as("Worker pool didn't terminate cleanly within budget — "
                        + "likely a hang in Hibernate session cleanup")
                .isTrue();

        assertThat(failures)
                .as("Concurrent globalSession readers threw %d exception(s); "
                        + "first: %s", failures.size(),
                        failures.isEmpty() ? "(none)" : failures.get(0))
                .isEmpty();
        assertThat(opCount.get())
                .as("Workers should have completed at least one full op each")
                .isGreaterThan((long) THREAD_COUNT);
    }
}
