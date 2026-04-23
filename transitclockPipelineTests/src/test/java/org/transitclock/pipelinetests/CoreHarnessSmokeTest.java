package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;

/**
 * Proves the {@link CoreHarness} boots a real {@link org.transitclock.applications.Core}
 * against the WMATA 5A GTFS dataset and exposes usable accessors. If this
 * suite fails, no other pipeline test in the module can be trusted — start
 * debugging here first.
 */
public class CoreHarnessSmokeTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	@Test
	public void coreSingletonIsNotNullAfterBoot() {
		assertThat(CORE.core())
				.as("Core.getInstance() should be non-null after harness boot")
				.isNotNull();
	}

	@Test
	public void dbConfigExposesLoadedGtfsData() {
		// The 5A dataset is a single-route agency. If GTFS loaded correctly,
		// getRoutes() should return at least one route and getBlocks() should
		// return at least one block — the specifics of the 5A dataset.
		assertThat(CORE.dbConfig())
				.as("DbConfig must be populated")
				.isNotNull();
		assertThat(CORE.dbConfig().getRoutes())
				.as("5A dataset should expose at least one route")
				.isNotEmpty();
		assertThat(CORE.dbConfig().getBlocks())
				.as("5A dataset should expose at least one block")
				.isNotEmpty();
	}

	@Test
	public void setNowRoundTripsThroughClock() {
		long fixedTime = 1_713_974_400_000L; // 2024-04-24T12:00:00Z, an arbitrary fixed point
		CORE.setNow(fixedTime);
		// After setNow, the clock reports the same epoch we set. This is what
		// pipeline tests will rely on for deterministic time-of-day checks.
		assertThat(CORE.clock().get())
				.as("clock().get() should return the epoch we set via setNow")
				.isEqualTo(fixedTime);
	}
}
