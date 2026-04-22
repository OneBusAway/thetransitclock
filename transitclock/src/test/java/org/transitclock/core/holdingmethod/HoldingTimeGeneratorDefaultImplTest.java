package org.transitclock.core.holdingmethod;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.Test;

public class HoldingTimeGeneratorDefaultImplTest {

	@Test
	public void implementsHoldingTimeGenerator() {
		assertThat(new HoldingTimeGeneratorDefaultImpl())
				.isInstanceOf(HoldingTimeGenerator.class);
	}

	@Test
	public void calculateHoldingTime_threePredictionsPickWorstAverage() throws Exception {
		// N = {11, 18, 28}, last_dep = 0, current_arr = 5, max_predictions = 3.
		//   (11-0)/2 = 5.5  → 5
		//   (18-0)/3 = 6
		//   (28-0)/4 = 7    ← max
		// holding = max(7 - (5-0), 0) = 2
		Long[] n = {11L, 18L, 28L};
		assertThat(invokeCalc(5L, 0L, n, 3)).isEqualTo(2L);
	}

	@Test
	public void calculateHoldingTime_clampsNegativeResultToZero() throws Exception {
		// N = {10}, last_dep = 0, max_predictions = 1.
		// (10-0)/2 = 5  → worst is 5.
		// current_arr=10 makes (current-last) > worst, so result clamps to 0.
		Long[] n = {10L};
		assertThat(invokeCalc(10L, 0L, n, 1)).isEqualTo(0L);
	}

	@Test
	public void calculateHoldingTime_respectsMaxPredictions() throws Exception {
		// With full array considered, third entry 28 wins and holding=2 (above).
		// Capping max_predictions to 1 should only look at first element:
		//   (11-0)/2 = 5  → worst = 5
		// holding = max(5 - 5, 0) = 0.
		Long[] n = {11L, 18L, 28L};
		assertThat(invokeCalc(5L, 0L, n, 1)).isEqualTo(0L);
	}

	@Test
	public void calculateHoldingTime_singlePredictionMatchesHandCalc() throws Exception {
		// N={10L,14L,19L}, current=3, last_dep=0, max=3
		//   (10-0)/2 = 5
		//   (14-0)/3 = 4
		//   (19-0)/4 = 4  (integer division)
		// worst = 5; holding = max(5 - (3-0), 0) = 2.
		Long[] n = {10L, 14L, 19L};
		assertThat(invokeCalc(3L, 0L, n, 3)).isEqualTo(2L);
	}

	private static Long invokeCalc(long currentArr, long lastDep, Long[] n, int maxPreds)
			throws Exception {
		Method m = HoldingTimeGeneratorDefaultImpl.class.getDeclaredMethod(
				"calculateHoldingTime", Long.class, Long.class, Long[].class, int.class);
		m.setAccessible(true);
		return (Long) m.invoke(null, currentArr, lastDep, n, maxPreds);
	}
}
