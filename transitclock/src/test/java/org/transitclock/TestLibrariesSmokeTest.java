package org.transitclock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.transitclock.utils.MathUtils;
import org.transitclock.utils.SystemTime;

/**
 * Verifies that JUnit 4.13.2 (assertThrows), Mockito 5 core + inline
 * (mockStatic), and AssertJ 3 are all wired up correctly on the test
 * classpath. If any of these break at build time, there's no point
 * writing Tier 1 tests on top of them.
 */
public class TestLibrariesSmokeTest {

	@Test
	public void junit_413_assertThrowsIsAvailable() {
		IllegalArgumentException ex = assertThrows(
				IllegalArgumentException.class,
				() -> { throw new IllegalArgumentException("boom"); });
		assertThat(ex).hasMessage("boom");
	}

	@Test
	public void mockito_canStubAndVerifyInstanceMethods() {
		SystemTime time = mock(SystemTime.class);
		when(time.get()).thenReturn(1_700_000_000_000L);

		long first = time.get();
		long second = time.get();

		assertThat(first).isEqualTo(1_700_000_000_000L);
		assertThat(second).isEqualTo(1_700_000_000_000L);
		verify(time, times(2)).get();
	}

	@Test
	public void mockitoInline_canMockStaticMethods() {
		try (MockedStatic<MathUtils> mocked = mockStatic(MathUtils.class)) {
			mocked.when(() -> MathUtils.round(1.2345, 2)).thenReturn(9.99);

			assertThat(MathUtils.round(1.2345, 2)).isEqualTo(9.99);
			mocked.verify(() -> MathUtils.round(1.2345, 2));
		}

		// Outside the scope, the real implementation is restored.
		assertThat(MathUtils.round(1.2345, 2)).isCloseTo(1.23, within(1e-9));
	}

	@Test
	public void assertj_fluentChainedAssertions() {
		assertThat("TheTransitClock")
				.isNotNull()
				.startsWith("The")
				.endsWith("Clock")
				.hasSize(15);

		assertThat(3.14159).isCloseTo(Math.PI, within(0.001));
	}
}
