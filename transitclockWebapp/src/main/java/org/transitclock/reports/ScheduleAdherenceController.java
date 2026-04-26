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
package org.transitclock.reports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.config.BooleanConfigValue;
import org.transitclock.config.IntegerConfigValue;
import org.transitclock.db.hibernate.HibernateUtils;
import org.transitclock.db.structs.ArrivalDeparture;
import org.transitclock.utils.Time;

public class ScheduleAdherenceController {
	 
	private static final Logger logger = LoggerFactory
			.getLogger(ScheduleAdherenceController.class);	
	// TODO: Combine routeScheduleAdherence and stopScheduleAdherence
	// - Make this a REST endpoint
	// problem - negative schedule adherence means we're late
	
	
	private static IntegerConfigValue scheduleEarlySeconds =
	    new IntegerConfigValue("transitclock.web.scheduleEarlyMinutes", 
	        -120, 
	        "Schedule Adherence early limit");

	public static int getScheduleEarlySeconds() {
	  return scheduleEarlySeconds.getValue();
	}
	
	 private static IntegerConfigValue scheduleLateSeconds =
	      new IntegerConfigValue("transitclock.web.scheduleLateMinutes", 
	          420, 
	          "Schedule Adherence late limit");

	 public static int getScheduleLateSeconds() {
	    return scheduleLateSeconds.getValue();
	  }
	 
	 private static BooleanConfigValue usePredictionLimits =
	     new BooleanConfigValue("transitme.web.userPredictionLimits", 
	         Boolean.FALSE,
	         "use the allowable early/late report params or use configured schedule limits");
	 
	// adherence is the delta between actual time and scheduledTime, in
	// milliseconds. AVG version is the per-group average; non-AVG is the
	// per-row value. Used by groupScheduleAdherence below as native SQL
	// fragments under the ArrivalsDepartures table alias `ad`.
	private static final String ADHERENCE_SQL = "(time - scheduledTime)";
	private static final String AVG_ADHERENCE_SQL = "avg(time - scheduledTime)";
	
	public static List<Object> stopScheduleAdherence(Date startDate,
			int numDays,
			String startTime,
			String endTime,
			List<String> stopIds,
			boolean byStop,
			String datatype) {

		return groupScheduleAdherence(startDate, numDays, startTime, endTime, "stopId", stopIds, byStop, datatype);
	}
	
	public static List<Object> routeScheduleAdherence(Date startDate,
			int numDays,
			String startTime,
			String endTime,
			List<String> routeIds,
			boolean byRoute,
			String datatype) {

		return groupScheduleAdherence(startDate, numDays, startTime, endTime, "routeId", routeIds, byRoute, datatype);
	}
	
	public static List<Integer> routeScheduleAdherenceSummary(Date startDate,
			int numDays,
			String startTime,
			String endTime,
			Double earlyLimitParam,
			Double lateLimitParam,
			List<String> routeIds) {
				
		int count = 0;
		int early = 0;
		int late = 0;
		int ontime = 0;
		Double earlyLimit = (usePredictionLimits.getValue() ? earlyLimitParam : (double)scheduleEarlySeconds.getValue());
		Double lateLimit = (usePredictionLimits.getValue() ? lateLimitParam : (double)scheduleLateSeconds.getValue());
		List<Object> results = routeScheduleAdherence(startDate, numDays, startTime, endTime, routeIds, false, null);

		for (Object o : results) {
			count++;
			HashMap hm = (HashMap) o;
			Double d = (Double)hm.get("scheduleAdherence");
			if (d > lateLimit) {
				late++;
			} else if (d < earlyLimit) {
				early++;
			} else {
				ontime++;
			}
		}
		logger.info("query complete -- earlyLimit={}, lateLimit={}, early={}, ontime={}, late={}, count={}",
				earlyLimit, lateLimit, early, ontime, late, count);
		double earlyPercent = (1.0 - (double)(count - early)/count) * 100;
		double onTimePercent = (1.0 - (double)(count - ontime)/count) * 100;
		double latePercent = (1.0 - (double)(count - late)/count) * 100;
		logger.info("count={} earlyPercent={} onTimePercent={} latePercent={}",
				count, earlyPercent, onTimePercent, latePercent);
		Integer[] summary = new Integer[] {count, (int) earlyPercent, (int) onTimePercent, (int) latePercent};
		return Arrays.asList(summary);
	}
	
	private static List<Object> groupScheduleAdherence(Date startDate, int numDays, String startTime, String endTime,
			String groupName, List<String> idsOrEmpty, boolean byGroup, String datatype) {

		// filter ids which may be empty.
		List<String> ids = new ArrayList<String>();
		if (idsOrEmpty != null)
			for (String id : idsOrEmpty)
				if (!StringUtils.isBlank(id)) {
					ids.add(id);
				}

		Date endDate = new Date(startDate.getTime() + (numDays * Time.MS_PER_DAY));

		// Hibernate 6 dropped the legacy DetachedCriteria + Projections /
		// Restrictions APIs that this report originally used to compose its
		// query. Replace with a native SQL build-up: the projection set is
		// either {group, count(*), avg(adherence)} when byGroup is true,
		// or {routeId, stopId, tripId, adherence} otherwise. The
		// ArrivalsDepartures table is queried directly because the
		// adherence projection mixes a SQL expression (avg(time -
		// scheduledTime)) with grouping.
		StringBuilder select = new StringBuilder("select ");
		if (byGroup) {
			select.append(groupName).append(" as ").append(groupName).append(", ");
			select.append("count(*) as count, ");
			select.append(AVG_ADHERENCE_SQL).append(" as scheduleAdherence");
		} else {
			select.append("routeId as routeId, stopId as stopId, tripId as tripId, ");
			select.append(ADHERENCE_SQL).append(" as scheduleAdherence");
		}

		// SQL-standard CAST(... AS TIME) is portable across PostgreSQL,
		// MySQL, and HSQL; the previous TIME(time) form was MySQL-specific
		// and produced a syntax error on Postgres. The :startTimeStr /
		// :endTimeStr parameters bind as VARCHAR, so cast them as well —
		// HSQL strict typing rejects VARCHAR-vs-TIME comparison and Postgres
		// silently allows it but a typed comparison reads more clearly.
		StringBuilder where = new StringBuilder(
				" from ArrivalsDepartures where time between :startDate and :endDate"
						+ " and scheduledTime is not null"
						+ " and cast(time as time) between"
						+ " cast(:startTimeStr as time) and cast(:endTimeStr as time)");
		if ("arrival".equals(datatype)) {
			where.append(" and isArrival = true");
		} else if ("departure".equals(datatype)) {
			where.append(" and isArrival = false");
		}
		if (!ids.isEmpty()) {
			where.append(" and ").append(groupName).append(" in (:ids)");
		}
		if (byGroup) {
			where.append(" group by ").append(groupName);
		}

		Session session = HibernateUtils.getSession();
		try {
			NativeQuery<?> q = session.createNativeQuery(select.toString() + where.toString(), Object.class)
					.setParameter("startDate", startDate)
					.setParameter("endDate", endDate)
					.setParameter("startTimeStr", startTime)
					.setParameter("endTimeStr", endTime);
			if (!ids.isEmpty()) {
				q.setParameter("ids", ids);
			}
			q.setTupleTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			@SuppressWarnings("unchecked")
			List<Object> results = (List<Object>) q.list();
			return results;
		} finally {
			session.close();
		}
	}

	 private static Date endOfDay(Date endDate) {
		 Calendar c = Calendar.getInstance();
		 c.setTime(endDate);
		 c.set(Calendar.HOUR, 23);
		 c.set(Calendar.MINUTE, 59);
		 c.set(Calendar.SECOND, 59);
		 return c.getTime();
	}

	 
}
