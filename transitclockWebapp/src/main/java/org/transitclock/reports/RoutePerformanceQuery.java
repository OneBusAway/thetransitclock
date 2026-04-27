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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.db.hibernate.HibernateUtils;

/**
 * To find route performance information.
 * For now, route performance is the percentage of predictions for a route which are ontime.
 *
 * @author Simon Jacobs
 *
 */
public class RoutePerformanceQuery {
  private Session session;
  
  private static final Logger logger = LoggerFactory
      .getLogger(RoutePerformanceQuery.class);
  
  public static final String PREDICTION_TYPE_AFFECTED = "AffectedByWaitStop";
  public static final String PREDICTION_TYPE_NOT_AFFECTED = "NotAffectedByWaitStop";
  
  private static final String TRANSITIME_PREDICTION_SOURCE = "Transitime";
  
  public List<Object[]> query(String agencyId, Date startDate, int numDays, double allowableEarlyMin, double allowableLateMin, String predictionType, String predictionSource) {

    int msecLo = (int) (allowableEarlyMin * 60 * 1000 * -1);
    int msecHi = (int) (allowableLateMin * 60 * 1000);
    Calendar c = Calendar.getInstance();
    c.setTime(startDate);
    c.add(Calendar.DAY_OF_YEAR,numDays);
    Date endDate = c.getTime();

    // Hibernate 6 dropped the legacy Criteria + Projections / Restrictions
    // APIs this report originally used. The query mixes a SQL projection
    // (avg(predictionAccuracyMsecs)) with a group-by — easiest expressed
    // as native SQL, which is what the original sqlProjection effectively
    // was anyway.
    StringBuilder sql = new StringBuilder(
        "select routeId as routeId, avg(predictionAccuracyMsecs) as performance"
            + " from PredictionAccuracy"
            + " where arrivalDepartureTime >= :startDate"
            + "   and arrivalDepartureTime <= :endDate"
            + "   and predictionAccuracyMsecs >= :msecLo"
            + "   and predictionAccuracyMsecs <= :msecHi");

    if (predictionType == PREDICTION_TYPE_AFFECTED) {
      sql.append(" and affectedByWaitStop = true");
    } else if (predictionType == PREDICTION_TYPE_NOT_AFFECTED) {
      sql.append(" and affectedByWaitStop = false");
    }

    boolean filterTransitime = false;
    boolean filterNonTransitime = false;
    if (predictionSource != null && !StringUtils.isEmpty(predictionSource)) {
      if (predictionSource.equals(TRANSITIME_PREDICTION_SOURCE)) {
        sql.append(" and predictionSource = :predictionSource");
        filterTransitime = true;
      } else {
        sql.append(" and predictionSource <> :predictionSource");
        filterNonTransitime = true;
      }
    }

    sql.append(" group by routeId order by performance desc");

    try {
      session = HibernateUtils.getSession(agencyId);
      NativeQuery<?> q = session.createNativeQuery(sql.toString(), Object.class)
          .setParameter("startDate", startDate)
          .setParameter("endDate", endDate)
          .setParameter("msecLo", msecLo)
          .setParameter("msecHi", msecHi);
      if (filterTransitime || filterNonTransitime) {
        q.setParameter("predictionSource", TRANSITIME_PREDICTION_SOURCE);
      }
      q.setTupleTransformer(AliasToEntityMapResultTransformer.INSTANCE);
      @SuppressWarnings("unchecked")
      List<Object[]> results = (List<Object[]>) q.list();
      return results;
    }
    catch(HibernateException e) {
      logger.error(e.toString());
      return null;
    }
    finally {
      session.close();
    }
  }

}
