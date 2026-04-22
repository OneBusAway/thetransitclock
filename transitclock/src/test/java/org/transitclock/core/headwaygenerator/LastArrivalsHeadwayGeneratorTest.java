package org.transitclock.core.headwaygenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.transitclock.core.HeadwayGenerator;
import org.transitclock.core.VehicleState;
import org.transitclock.db.structs.Headway;

public class LastArrivalsHeadwayGeneratorTest {

	@Test
	public void implementsHeadwayGenerator() {
		assertThat(new LastArrivalsHeadwayGenerator()).isInstanceOf(HeadwayGenerator.class);
	}

	@Test
	public void generate_swallowsExceptionAndReturnsNull() {
		LastArrivalsHeadwayGenerator gen = new LastArrivalsHeadwayGenerator();
		assertThat(gen.generate(mock(VehicleState.class))).isNull();
	}

	@Test
	public void average_variance_cov_matchHandComputedValues() throws Exception {
		List<Headway> headways = Arrays.asList(
				headway(60.0), headway(120.0), headway(180.0));

		double avg = (Double) invokePrivate("average", List.class, headways);
		double variance = (Double) invokePrivate("variance", List.class, headways);
		double cov = (Double) invokePrivate("coefficientOfVariance", List.class, headways);

		// mean = 120; squared deviations 60²+0²+60² = 7200; variance = 2400.
		assertThat(avg).isCloseTo(120.0, within(1e-9));
		assertThat(variance).isCloseTo(2400.0, within(1e-9));
		assertThat(cov).isCloseTo(2400.0 / (120.0 * 120.0), within(1e-9));
	}

	private static Headway headway(double ms) {
		Headway h = mock(Headway.class);
		when(h.getHeadway()).thenReturn(ms);
		return h;
	}

	private static Object invokePrivate(String name, Class<?> paramType, Object arg)
			throws Exception {
		Method m = LastArrivalsHeadwayGenerator.class.getDeclaredMethod(name, paramType);
		m.setAccessible(true);
		return m.invoke(new LastArrivalsHeadwayGenerator(), arg);
	}
}
