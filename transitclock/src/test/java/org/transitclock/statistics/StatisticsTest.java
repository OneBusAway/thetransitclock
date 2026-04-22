package org.transitclock.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class StatisticsTest {

	@Test
	public void mean_ofSingleValueReturnsThatValue() {
		assertThat(Statistics.mean(Collections.singletonList(42))).isEqualTo(42);
	}

	@Test
	public void mean_ofMultipleValuesTruncates() {
		// 10/3 = 3.333..., integer division truncates toward zero.
		assertThat(Statistics.mean(Arrays.asList(3, 3, 4))).isEqualTo(3);
	}

	@Test
	public void mean_doubleArrayReturnsExactAverage() {
		assertThat(Statistics.mean(new double[] {1.0, 2.0, 3.0, 4.0}))
				.isCloseTo(2.5, within(1e-9));
	}

	@Test
	public void mean_doubleArrayEmptyIsNaN() {
		assertThat(Statistics.mean(new double[0])).isNaN();
	}

	@Test
	public void mean_intArrayReturnsDoubleAverage() {
		assertThat(Statistics.mean(new int[] {1, 2, 3, 4}))
				.isCloseTo(2.5, within(1e-9));
	}

	@Test
	public void toArray_nullReturnsEmpty() {
		assertThat(Statistics.toArray(null)).isEmpty();
	}

	@Test
	public void toArray_convertsInOrder() {
		assertThat(Statistics.toArray(Arrays.asList(5, 7, 9)))
				.containsExactly(5, 7, 9);
	}

	@Test
	public void toDoubleArray_fromIntArrayConverts() {
		assertThat(Statistics.toDoubleArray(new int[] {1, 2, 3}))
				.containsExactly(1.0, 2.0, 3.0);
	}

	@Test
	public void toDoubleArray_fromNullListReturnsEmpty() {
		assertThat(Statistics.toDoubleArray((List<Integer>) null)).isEmpty();
	}

	@Test
	public void toDoubleArray_fromListConverts() {
		assertThat(Statistics.toDoubleArray(Arrays.asList(4, 5)))
				.containsExactly(4.0, 5.0);
	}

	@Test
	public void getSampleStandardDeviation_matchesN_minus_1Formula() {
		// values {2,4,4,4,5,5,7,9}, mean=5, sample stddev = sqrt(32/7) ~= 2.138.
		double[] values = {2, 4, 4, 4, 5, 5, 7, 9};
		double mean = Statistics.mean(values);
		assertThat(mean).isCloseTo(5.0, within(1e-9));

		double stdDev = Statistics.getSampleStandardDeviation(values, mean);
		assertThat(stdDev).isCloseTo(Math.sqrt(32.0 / 7.0), within(1e-9));
	}

	@Test
	public void getSampleStandardDeviation_singleValueIsNaN() {
		// variance divides by N-1 == 0, so result is NaN.
		double[] values = {10.0};
		assertThat(Statistics.getSampleStandardDeviation(values, 10.0)).isNaN();
	}

	@Test
	public void filteredMean_twoOrFewerValuesReturnsPlainMean() {
		assertThat(Statistics.filteredMean(Arrays.asList(10, 1_000_000), 0.5))
				.isEqualTo((10 + 1_000_000) / 2);
	}

	@Test
	public void filteredMean_noOutliersReturnsUnfilteredMean() {
		// All values within fractionalLimit of the mean, so nothing is filtered.
		List<Integer> values = Arrays.asList(100, 110, 105, 95, 100);
		assertThat(Statistics.filteredMean(values, 0.5))
				.isEqualTo(Statistics.mean(values));
	}

	@Test
	public void filteredMean_dropsWorstOutlier() {
		// 1_000_000 is far above the 100-ish values; it should be removed.
		List<Integer> values = Arrays.asList(100, 105, 110, 95, 1_000_000);
		int withOutlier = Statistics.mean(values);
		int filtered = Statistics.filteredMean(values, 0.7);

		// Filtered mean should be close to the 100-ish cluster and well below
		// the unfiltered mean which is dragged up by the outlier.
		assertThat(filtered).isLessThan(withOutlier).isBetween(90, 120);
	}

	@Test
	public void biasedFilteredMean_zeroBiasEqualsFilteredMean() {
		List<Integer> values = Arrays.asList(100, 105, 110, 95, 1_000_000);
		assertThat(Statistics.biasedFilteredMean(values, 0.7, 0.0))
				.isEqualTo(Statistics.filteredMean(values, 0.7));
	}

	@Test
	public void biasedFilteredMean_subtractsStandardDeviationTimesBias() {
		// Use a dataset where nothing gets filtered so we can compute by hand.
		List<Integer> values = Arrays.asList(10, 12, 14, 16, 18);
		double[] doubles = Statistics.toDoubleArray(Statistics.toArray(values));
		double mean = Statistics.mean(doubles);
		double stdDev = Statistics.getSampleStandardDeviation(doubles, mean);

		int biased = Statistics.biasedFilteredMean(values, 0.7, 1.0);

		assertThat(biased).isEqualTo((int) Math.round(mean - stdDev));
	}

	@Test
	public void biasedFilteredMean_withFewValuesFallsBackToMean() {
		// Dataset collapses to ≤2 values after filtering → returns simple mean.
		List<Integer> values = Arrays.asList(100, 120);
		assertThat(Statistics.biasedFilteredMean(values, 0.5, 1.0))
				.isEqualTo(Statistics.mean(values));
	}
}
