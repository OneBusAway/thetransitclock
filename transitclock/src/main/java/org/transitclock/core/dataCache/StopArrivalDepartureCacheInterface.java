package org.transitclock.core.dataCache;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.transitclock.db.structs.ArrivalDeparture;
import org.transitclock.ipc.data.IpcArrivalDeparture;

public abstract class StopArrivalDepartureCacheInterface {

	abstract  public  List<IpcArrivalDeparture> getStopHistory(StopArrivalDepartureCacheKey key);

	abstract  public StopArrivalDepartureCacheKey putArrivalDeparture(ArrivalDeparture arrivalDeparture);

	public void populateCacheFromDb(Session session, Date startDate, Date endDate) {
		List<ArrivalDeparture> results = session.createQuery(
				"from ArrivalDeparture where time between :start and :end order by time asc",
				ArrivalDeparture.class)
				.setParameter("start", startDate)
				.setParameter("end", endDate)
				.getResultList();

		for (ArrivalDeparture result : results) {
			this.putArrivalDeparture(result);
			//TODO might be better with its own populateCacheFromdb
			DwellTimeModelCacheFactory.getInstance().addSample(result);
		}
	}

}