package org.transitclock.gtfs;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.transitclock.db.structs.ActiveRevisions;
import org.transitclock.db.structs.TravelTimesForTrip;

public class TravelTimesProcessorForGtfsUpdatesTest {

	@Test
	public void constructor_storesInputsAndIsAccessibleViaGetters() {
		ActiveRevisions revs = new ActiveRevisions();
		TravelTimesProcessorForGtfsUpdates p = new TravelTimesProcessorForGtfsUpdates(
				revs, /* originalTravelTimesRev */ 5,
				/* maxTravelTimeSegmentLength */ 200.0,
				/* defaultWaitTimeAtStopMsec */ 30_000,
				/* maxSpeedKph */ 50.0);

		// Nothing processed yet.
		assertThat(p.getNumberOfTravelTimes()).isEqualTo(0);
		assertThat(p.getOriginalNumberOfTravelTimes()).isEqualTo(0);
	}

	@Test
	public void setNumberOfTravelTimes_nullIsIgnoredButValuesRoundTrip() {
		TravelTimesProcessorForGtfsUpdates p = new TravelTimesProcessorForGtfsUpdates(
				new ActiveRevisions(), 0, 200.0, 30_000, 50.0);

		p.setNumberOfTravelTimes(42);
		assertThat(p.getNumberOfTravelTimes()).isEqualTo(42);

		// Null is explicitly guarded against; the prior value survives.
		p.setNumberOfTravelTimes(null);
		assertThat(p.getNumberOfTravelTimes()).isEqualTo(42);

		p.setOriginalNumberOfTravelTimes(17);
		assertThat(p.getOriginalNumberOfTravelTimes()).isEqualTo(17);
	}

	@Test
	public void numberOfTravelTimes_sumsListSizesAcrossTripPatterns() throws Exception {
		Map<String, List<TravelTimesForTrip>> map = new HashMap<>();
		map.put("pattern-A", Arrays.asList(
				mockTravelTimes(), mockTravelTimes(), mockTravelTimes()));
		map.put("pattern-B", Arrays.asList(mockTravelTimes()));
		map.put("pattern-C", Collections.<TravelTimesForTrip>emptyList());

		Method m = TravelTimesProcessorForGtfsUpdates.class.getDeclaredMethod(
				"numberOfTravelTimes", Map.class);
		m.setAccessible(true);
		int result = (int) m.invoke(null, map);

		assertThat(result).isEqualTo(4);
	}

	private static TravelTimesForTrip mockTravelTimes() {
		return org.mockito.Mockito.mock(TravelTimesForTrip.class);
	}
}
