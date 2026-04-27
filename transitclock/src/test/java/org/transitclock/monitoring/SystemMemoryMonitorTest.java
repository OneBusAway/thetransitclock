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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SystemMemoryMonitorTest {

	/**
	 * Free-physical-memory accessor must return a non-null value on the
	 * Hotspot/OpenJDK runtimes the project supports (the platform
	 * {@code OperatingSystemMXBean} implements
	 * {@code com.sun.management.OperatingSystemMXBean}). Using a typed
	 * accessor instead of string-based reflection means the call is
	 * compile-time bound — there is no NoSuchMethodException failure mode
	 * to silently swallow.
	 */
	@Test
	public void getFreePhysicalMemoryBytes_returnsValueOnHotspot() {
		Long bytes = SystemMemoryMonitor.getFreePhysicalMemoryBytes();
		assertNotNull(
				"Free physical memory must be readable on a HotSpot/OpenJDK JVM",
				bytes);
		assertTrue("Free physical memory must be non-negative", bytes >= 0L);
	}

	/**
	 * CPU-load accessor must return a non-null value on the Hotspot/OpenJDK
	 * runtimes the project supports. The bean does sometimes return -1.0
	 * during the very first sample, so we accept any value in [-1.0, 1.0]
	 * and just require non-null.
	 */
	@Test
	public void getSystemCpuLoad_returnsValueOnHotspot() {
		Double load = SystemMemoryMonitor.getSystemCpuLoad();
		assertNotNull(
				"CPU load must be readable on a HotSpot/OpenJDK JVM",
				load);
		assertTrue("CPU load must be in [-1.0, 1.0]",
				load >= -1.0d && load <= 1.0d);
	}
}
