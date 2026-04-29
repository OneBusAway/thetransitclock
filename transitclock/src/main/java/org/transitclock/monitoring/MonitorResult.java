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

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contains status for an individual monitor. To be passed via IPC to client.
 *
 * @author SkiBu Smith
 *
 */
public class MonitorResult implements Serializable {

	private final String type;
	private final String message;
	// Never null; empty when the monitor doesn't expose structured stats.
	private final Map<String, String> stats;

	// serialVersionUID intentionally unchanged when 'stats' was added so that
	// a webapp on the new class can still deserialize streams from an older
	// Core that lacks the field. Default deserialization leaves 'stats' null
	// in that case (constructor is bypassed); readResolve() restores the
	// "never null" invariant.
	private static final long serialVersionUID = 8865389000445125279L;

	/********************** Member Functions **************************/

	public MonitorResult(String type, String message, Map<String, String> stats) {
		this.type = type;
		this.message = message;
		this.stats = stats == null
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<>(stats));
	}

	@Override
	public String toString() {
		return "MonitorResult [type=" + type + ", message=" + message
				+ ", stats=" + stats + "]";
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public Map<String, String> getStats() {
		return stats;
	}

	private Object readResolve() {
		return stats == null ? new MonitorResult(type, message, null) : this;
	}

}
