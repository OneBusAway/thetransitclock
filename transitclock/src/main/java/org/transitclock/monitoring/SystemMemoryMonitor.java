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

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.config.LongConfigValue;
import org.transitclock.utils.EmailSender;
import org.transitclock.utils.StringUtils;

/**
 * For monitoring CPU, available memory, and available disk space.
 * <p>
 * Note: Linux will use a great deal of RAM for caching and such when memory is
 * available. Therefore most of the time the free memory will be quite low. But
 * this is OK since the operating system will give up the RAM being used for
 * caching and such if a process needs it. Therefore the allowable free memory
 * should be set to quite a low value.
 *
 * @author SkiBu Smith
 *
 */
public class SystemMemoryMonitor extends MonitorBase {

	LongConfigValue availableFreePhysicalMemoryThreshold = new LongConfigValue(
			"transitclock.monitoring.availableFreePhysicalMemoryThreshold", 
			10 * 1024 * 1024L, // ~10 MB 
			"If available free physical memory is less than this "
			+ "value then free memory monitoring is triggered. This should be "
			+ "relatively small since on Linux the operating system will use "
			+ "most of the memory for buffers and such when it is available. "
			+ "Therefore even when only a small amount of memory is available "
			+ "the system is still OK.");

	private static LongConfigValue availableFreePhysicalMemoryThresholdGap =
			new LongConfigValue(
					"transitclock.monitoring.availableFreePhysicalMemoryThresholdGap", 
					150 * 1024 * 1024L, // ~150 MB 
					"When transitioning from triggered to untriggered don't "
					+ "want to send out an e-mail right away if actually "
					+ "dithering. Therefore will only send out OK e-mail if the "
					+ "value is now above availableFreePhysicalMemoryThreshold + "
					+ "availableFreePhysicalMemoryThresholdGap ");

	private static final Logger logger = LoggerFactory
			.getLogger(SystemMemoryMonitor.class);

	/********************** Member Functions **************************/

	/**
	 * Simple constructor
	 * 
	 * @param emailSender
	 * @param agencyId
	 */
	public SystemMemoryMonitor(EmailSender emailSender, String agencyId) {
		super(emailSender, agencyId);
	}

	/**
	 * Returns the platform {@link OperatingSystemMXBean} cast to the Sun
	 * extension interface, or {@code null} on a non-HotSpot JVM that does
	 * not implement it (logged once per call). Callers must null-check.
	 */
	private static OperatingSystemMXBean sunOsBean() {
		java.lang.management.OperatingSystemMXBean bean =
				ManagementFactory.getOperatingSystemMXBean();
		if (bean instanceof OperatingSystemMXBean sun) {
			return sun;
		}
		logger.error("Platform OperatingSystemMXBean is not a "
				+ "com.sun.management.OperatingSystemMXBean; "
				+ "system metrics are unavailable on this JVM.");
		return null;
	}

	/**
	 * Free physical memory in bytes, or {@code null} on a JVM that does not
	 * expose the Sun extension interface.
	 */
	public static Long getFreePhysicalMemoryBytes() {
		OperatingSystemMXBean bean = sunOsBean();
		return bean == null ? null : bean.getFreeMemorySize();
	}

	/**
	 * Recent system CPU load in {@code [0.0, 1.0]} (or {@code -1.0} if not
	 * yet available), or {@code null} on a JVM that does not expose the Sun
	 * extension interface.
	 */
	@SuppressWarnings("deprecation") // getSystemCpuLoad replaced by getCpuLoad in JDK 14; switch is a Phase B item
	public static Double getSystemCpuLoad() {
		OperatingSystemMXBean bean = sunOsBean();
		return bean == null ? null : bean.getSystemCpuLoad();
	}
	
	/* (non-Javadoc)
	 * @see org.transitclock.monitoring.MonitorBase#triggered()
	 */
	/**
	 * Sees if recent available memory is lower than value specified by
	 * availableFreePhysicalMemoryThreshold.
	 * 
	 * @return True if available memory is lower than
	 *         availableFreePhysicalMemoryThreshold. If available memory is
	 *         higher or can't determine available memory then returns false.
	 */
	@Override
	protected boolean triggered() {
		Long freePhysicalMemory = getFreePhysicalMemoryBytes();
		if (freePhysicalMemory == null) {
			// Could not determine available memory so have to return false
			return false;
		}

		// Provide message explaining situation
		setMessage("Free physical memory is "
				+ StringUtils.memoryFormat(freePhysicalMemory)
				+ " while the limit is "
				+ StringUtils.memoryFormat(
						availableFreePhysicalMemoryThreshold.getValue())
				+ ".",
				freePhysicalMemory);

		addStat("Free", StringUtils.memoryFormat(freePhysicalMemory));
		addStat("Minimum", StringUtils.memoryFormat(
				availableFreePhysicalMemoryThreshold.getValue()));

		// Determine the threshold for triggering. If already triggered
		// then raise the threshold by availableFreePhysicalMemoryThresholdGap
		// in order to prevent lots of e-mail being sent out if the value
		// is dithering around availableFreePhysicalMemoryThreshold.
		long threshold = availableFreePhysicalMemoryThreshold.getValue();
		if (wasTriggered())
			threshold += availableFreePhysicalMemoryThresholdGap.getValue();

		// Return true if problem detected
		return freePhysicalMemory < threshold;
	}

	/* (non-Javadoc)
	 * @see org.transitclock.monitoring.MonitorBase#type()
	 */
	@Override
	protected String type() {
		return "System Memory";
	}

}
