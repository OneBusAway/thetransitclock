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

public class LastDepartureHeadwayGeneratorTest {

	@Test
	public void implementsHeadwayGenerator() {
		assertThat(new LastDepartureHeadwayGenerator()).isInstanceOf(HeadwayGenerator.class);
	}

	@Test
	public void generate_swallowsExceptionAndReturnsNull() {
		// VehicleState is a bare mock — getMatch() returns null, so the chain
		// vehicleState.getMatch().getMatchAtPreviousStop()... will NPE. The
		// generator catches everything and returns null.
		LastDepartureHeadwayGenerator gen = new LastDepartureHeadwayGenerator();
		assertThat(gen.generate(mock(VehicleState.class))).isNull();
	}

	@Test
	public void average_isArithmeticMeanOfHeadways() throws Exception {
		List<Headway> headways = Arrays.asList(
				headway(100.0), headway(200.0), headway(300.0));

		double avg = (Double) invokePrivate("average", List.class, headways);

		assertThat(avg).isCloseTo(200.0, within(1e-9));
	}

	@Test
	public void variance_isMeanSquaredDeviation() throws Exception {
		// mean = 200; deviations² sum = 100²+0²+100² = 20000; /3 = 6666.667
		List<Headway> headways = Arrays.asList(
				headway(100.0), headway(200.0), headway(300.0));

		double variance = (Double) invokePrivate("variance", List.class, headways);

		assertThat(variance).isCloseTo(20000.0 / 3.0, within(1e-9));
	}

	@Test
	public void coefficientOfVariance_isVarianceOverSquaredMean() throws Exception {
		List<Headway> headways = Arrays.asList(
				headway(100.0), headway(200.0), headway(300.0));
		double expected = (20000.0 / 3.0) / (200.0 * 200.0);

		double cov = (Double) invokePrivate("coefficientOfVariance", List.class, headways);

		assertThat(cov).isCloseTo(expected, within(1e-9));
	}

	private static Headway headway(double ms) {
		// Real Headway construction reaches into Core.getInstance().getDbConfig();
		// a mock with getHeadway() stubbed is enough for the math under test.
		Headway h = mock(Headway.class);
		when(h.getHeadway()).thenReturn(ms);
		return h;
	}

	private static Object invokePrivate(String name, Class<?> paramType, Object arg)
			throws Exception {
		Method m = LastDepartureHeadwayGenerator.class.getDeclaredMethod(name, paramType);
		m.setAccessible(true);
		return m.invoke(new LastDepartureHeadwayGenerator(), arg);
	}
}
