package org.transitclock.core.predictiongenerator.bias;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.transitclock.utils.Time;

public class BiasAdjusterTest {

	private static final double RATE = 0.0006;

	@Test
	public void linearConstructorStoresRate() {
		LinearBiasAdjuster adjuster = new LinearBiasAdjuster(RATE);
		assertEquals(RATE, adjuster.getRate(), 1e-12);
	}

	@Test
	public void linearDefaultConstructorPicksUpConfiguredRate() {
		LinearBiasAdjuster adjuster = new LinearBiasAdjuster();
		// Default configured rate is 0.0006 (see LinearBiasAdjuster.rate config).
		assertEquals(0.0006, adjuster.getRate(), 1e-12);
	}

	@Test
	public void linearAdjustmentOfZeroIsZero() {
		LinearBiasAdjuster adjuster = new LinearBiasAdjuster(RATE);
		assertEquals(0L, adjuster.adjustPrediction(0L));
	}

	@Test
	public void linearAdjustmentReducesPositiveHorizon() {
		// With default updown=-1 the adjustment subtracts from the prediction.
		LinearBiasAdjuster adjuster = new LinearBiasAdjuster(RATE);
		long horizon = 20 * Time.MS_PER_MIN;
		long adjusted = adjuster.adjustPrediction(horizon);
		assertTrue("expected adjusted (" + adjusted + ") < horizon (" + horizon + ")",
				adjusted < horizon);
		assertTrue("expected adjusted > 0", adjusted > 0);
	}

	@Test
	public void linearAdjustmentGrowsWithHorizon() {
		// Larger horizon should absorb a larger absolute reduction.
		LinearBiasAdjuster adjuster = new LinearBiasAdjuster(RATE);
		long small = 5 * Time.MS_PER_MIN;
		long large = 20 * Time.MS_PER_MIN;
		long reductionSmall = small - adjuster.adjustPrediction(small);
		long reductionLarge = large - adjuster.adjustPrediction(large);
		assertTrue(
				"reductionLarge=" + reductionLarge + " should exceed reductionSmall=" + reductionSmall,
				reductionLarge > reductionSmall);
	}

	@Test
	public void linearGetPercentageReflectsLastCall() {
		LinearBiasAdjuster adjuster = new LinearBiasAdjuster(RATE);
		adjuster.adjustPrediction(20 * Time.MS_PER_MIN);
		double pctAt20 = adjuster.getPercentage();
		adjuster.adjustPrediction(5 * Time.MS_PER_MIN);
		double pctAt5 = adjuster.getPercentage();
		assertTrue("percentage should be larger at larger horizon",
				pctAt20 > pctAt5);
	}

	@Test
	public void exponentialAdjustmentOfZeroIsZero() {
		ExponentialBiasAdjuster adjuster = new ExponentialBiasAdjuster();
		assertEquals(0L, adjuster.adjustPrediction(0L));
	}

	@Test
	public void exponentialAdjustmentReducesPositiveHorizon() {
		ExponentialBiasAdjuster adjuster = new ExponentialBiasAdjuster();
		long horizon = 20 * Time.MS_PER_MIN;
		long adjusted = adjuster.adjustPrediction(horizon);
		assertTrue("expected adjusted < horizon", adjusted < horizon);
		assertTrue("expected adjusted > 0", adjusted > 0);
	}

	@Test
	public void exponentialAdjustmentGrowsSuperLinearlyWithHorizon() {
		// Relative reduction (fraction of horizon removed) should increase with horizon.
		ExponentialBiasAdjuster adjuster = new ExponentialBiasAdjuster();
		long small = 5 * Time.MS_PER_MIN;
		long large = 20 * Time.MS_PER_MIN;
		double ratioSmall = (small - adjuster.adjustPrediction(small)) / (double) small;
		double ratioLarge = (large - adjuster.adjustPrediction(large)) / (double) large;
		assertTrue("ratioLarge=" + ratioLarge + " should exceed ratioSmall=" + ratioSmall,
				ratioLarge > ratioSmall);
	}

	@Test
	public void exponentialBaseNumberMatchesConfiguredDefault() {
		ExponentialBiasAdjuster adjuster = new ExponentialBiasAdjuster();
		assertEquals(1.1, adjuster.getNumber(), 1e-12);
	}
}
