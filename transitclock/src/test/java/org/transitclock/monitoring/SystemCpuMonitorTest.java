/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Transitime.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transitime.org .  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transitclock.monitoring;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.Calendar;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.transitclock.utils.Time;

public class SystemCpuMonitorTest {

	private static boolean isInFirstTwelveMinutesOfDay() {
		Calendar c = Calendar.getInstance();
		int sec = c.get(Calendar.HOUR_OF_DAY) * 3600
				+ c.get(Calendar.MINUTE) * 60
				+ c.get(Calendar.SECOND);
		return sec < 12 * 60;
	}

	/**
	 * Regression for an NPE when the second CPU reading returns null. With
	 * the old reflection-based code path this threw {@code NullPointerException}
	 * out of {@code triggered()}; we want it to degrade to {@code false}
	 * (cannot determine load, so do not trigger) and not bubble.
	 *
	 * Setup: first reading is exactly the configured threshold (1.0) so the
	 * code enters the "spike, take a second reading" branch. The second
	 * reading is forced to null.
	 */
	@Test
	public void triggered_doesNotNpeWhenSecondReadingIsNull() {
		if (isInFirstTwelveMinutesOfDay()) {
			// triggered() short-circuits during the daily log-rotate window;
			// running the test then would not exercise the second-reading path.
			return;
		}
		try (MockedStatic<SystemMemoryMonitor> mxBean =
				Mockito.mockStatic(SystemMemoryMonitor.class, Mockito.CALLS_REAL_METHODS);
				MockedStatic<Time> time = Mockito.mockStatic(Time.class, Mockito.CALLS_REAL_METHODS)) {
			mxBean.when(SystemMemoryMonitor::getSystemCpuLoad)
					.thenReturn(1.0d, (Double) null);
			time.when(() -> Time.sleep(anyLong())).thenAnswer(inv -> null);

			SystemCpuMonitor monitor = new SystemCpuMonitor(null, "test-agency");
			assertFalse("triggered() must return false when CPU reading is unavailable",
					monitor.triggered());
		}
	}
}
