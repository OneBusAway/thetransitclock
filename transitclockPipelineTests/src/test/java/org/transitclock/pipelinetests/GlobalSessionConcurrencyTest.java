package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
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
 * Hammers the three {@code globalSession} lazy-load paths from many threads
 * to catch the Hibernate 6 cross-thread {@code Session} race. Hibernate 6's
 * {@code ResourceRegistryStandardImpl} iterates an unsynchronized HashMap
 * during JDBC-resource cleanup; if any caller touches the session without
 * the canonical {@code Block#lazyLoadingSyncObject} lock, a concurrent
 * cleanup throws CME / "ResultSet is closed". The bug is in Hibernate's
 * per-session resource registry, so HSQL reproduces the same race as
 * Postgres.
 */
public class GlobalSessionConcurrencyTest {

    @ClassRule
    public static final CoreHarness CORE = CoreHarness.withWmata5A();

    /**
     * Tuned to reliably reproduce the pre-fix CME within ~3s on a 4-core
     * dev machine. Lower thread counts or shorter durations let the race
     * slip past; higher values just slow CI without improving signal.
     */
    private static final int THREAD_COUNT = 32;
    private static final long DURATION_MS = 8_000L;
    private static final int TRIP_ID_SAMPLE_SIZE = 40;
    private static final int BLOCK_SAMPLE_SIZE = 16;

    @Test
    public void concurrentGlobalSessionReadersDoNotRaceUnderLoad() throws Exception {
        DbConfig dbConfig = CORE.dbConfig();

        // Shared lookup keys only — no pre-resolution, so workers race the
        // first lazy-load of each entity (the actual bug surface).
        List<Block> blocks = new ArrayList<>();
        for (Block b : dbConfig.getBlocks()) {
            blocks.add(b);
            if (blocks.size() >= BLOCK_SAMPLE_SIZE) break;
        }
        assertThat(blocks).isNotEmpty();

        List<String> tripIds = new ArrayList<>();
        Iterator<Trip> tripIt = dbConfig.getTrips().values().iterator();
        while (tripIt.hasNext() && tripIds.size() < TRIP_ID_SAMPLE_SIZE) {
            tripIds.add(tripIt.next().getId());
        }

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch go = new CountDownLatch(1);
        AtomicLong opCount = new AtomicLong();
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

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
                            // Synced cache-miss path.
                            Trip resolved = dbConfig.getTrip(
                                    tripIds.get(Math.floorMod(idx, tripIds.size())));
                            if (resolved != null) opCount.incrementAndGet();

                            // Lazy ManyToMany load + per-Trip lazy
                            // ElementCollection access — the matcher hot path
                            // that produced the production CME.
                            Block b = blocks.get(Math.floorMod(idx, blocks.size()));
                            for (Trip t : b.getTrips()) {
                                try {
                                    ScheduleTime s = t.getScheduleTime(0);
                                    if (s != null) opCount.incrementAndGet();
                                } catch (IndexOutOfBoundsException ignore) {
                                    // Trips with empty scheduledTimesList for index 0.
                                }
                                break;
                            }
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
                .as("Worker pool didn't terminate cleanly — likely hang in Hibernate cleanup")
                .isTrue();

        assertThat(failures)
                .as("Concurrent globalSession readers threw %d exception(s); first: %s",
                        failures.size(),
                        failures.isEmpty() ? "(none)" : failures.get(0))
                .isEmpty();
        assertThat(opCount.get())
                .as("Workers should have completed at least one full op each")
                .isGreaterThan((long) THREAD_COUNT);
    }
}
