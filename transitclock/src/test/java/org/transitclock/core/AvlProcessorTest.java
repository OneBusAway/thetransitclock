package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class AvlProcessorTest {

	@Test
	public void getInstance_returnsSingleton() {
		assertThat(AvlProcessor.getInstance())
				.isNotNull()
				.isSameAs(AvlProcessor.getInstance());
	}

	@Test
	public void lastAvlReportTime_isZeroBeforeAnyReportIsProcessed() {
		// The singleton may have been touched by other tests that preceded this
		// one, so we don't assume a "clean" state — we only assert the
		// contract: if no regular report has been stored yet, the method
		// returns 0, otherwise it returns the epoch time of the stored report.
		AvlProcessor p = AvlProcessor.getInstance();
		if (p.getLastAvlReport() == null) {
			assertThat(p.lastAvlReportTime()).isEqualTo(0L);
		} else {
			assertThat(p.lastAvlReportTime()).isEqualTo(p.getLastAvlReport().getTime());
		}
	}
}
