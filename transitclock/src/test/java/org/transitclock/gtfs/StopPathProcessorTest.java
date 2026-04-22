package org.transitclock.gtfs;

import java.util.Collections;

import org.junit.Test;
import org.transitclock.db.structs.Stop;
import org.transitclock.db.structs.TripPattern;
import org.transitclock.gtfs.gtfsStructs.GtfsShape;

public class StopPathProcessorTest {

	@Test
	public void processPathSegments_withEmptyInputsIsANoOp() {
		StopPathProcessor processor = new StopPathProcessor(
				Collections.<GtfsShape>emptyList(),
				Collections.<String, Stop>emptyMap(),
				Collections.<TripPattern>emptyList(),
				/* offsetDistance */ 0.0,
				/* maxStopToPathDistance */ 50.0,
				/* maxDistanceForEliminatingVertices */ 5.0,
				/* trimPathBeforeFirstStopOfTrip */ false,
				/* maxDistanceBetweenStops */ 1000.0,
				/* disableSpecialLoopBackToBeginningCase */ false);

		// Should walk the (empty) trip patterns and finish cleanly.
		processor.processPathSegments();
	}

	@Test
	public void constructor_handlesDuplicateShapeIdsAndSortsThem() {
		GtfsShape s1 = new GtfsShape("shape-A", 47.6, -122.3, 2, 0);
		GtfsShape s2 = new GtfsShape("shape-A", 47.6001, -122.3001, 1, 10);

		// Constructor groups by shapeId and sorts each group — no patterns
		// or stops needed to exercise that path; the call should not throw.
		new StopPathProcessor(
				java.util.Arrays.asList(s1, s2),
				Collections.<String, Stop>emptyMap(),
				Collections.<TripPattern>emptyList(),
				0.0, 50.0, 5.0, false, 1000.0, false);
	}
}
