package org.transitclock.testutil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.transitclock.db.structs.Block;
import org.transitclock.db.structs.Location;
import org.transitclock.db.structs.ScheduleTime;
import org.transitclock.db.structs.StopPath;
import org.transitclock.db.structs.Trip;
import org.transitclock.gtfs.TitleFormatter;
import org.transitclock.gtfs.gtfsStructs.GtfsTrip;

/**
 * Test-only fixtures for assembling the minimum Trip/Block/StopPath graph
 * required to exercise non-trivial methods on the Hibernate entities without
 * spinning up a full GTFS import or a running Core/DbConfig. Each factory
 * method accepts overrides for the fields that typically matter in a test
 * and fills in sensible defaults for everything else.
 *
 * Not every db.structs class can be constructed by hand — TripPattern,
 * TravelTimesForTrip, and Route all require a live GtfsData graph or reach
 * into Core.getInstance(). Tests that need those must rely on the integration
 * suite. This helper intentionally covers only the entities whose public
 * constructors take simple values or other test-constructible entities.
 */
public final class TransitFixtures {

	public static final int DEFAULT_CONFIG_REV = 7;

	// The TitleFormatter is needed as a Trip constructor arg. Passing a null
	// filename makes it log-but-proceed, which is fine for tests — we don't
	// exercise title formatting here.
	private static final TitleFormatter NULL_TITLE_FORMATTER =
			new TitleFormatter(null, /*logUnusedRegexs*/ false);

	private TransitFixtures() {}

	/**
	 * Builds a GtfsTrip with all the fields populated. Use
	 * {@link #simpleGtfsTrip(String, String)} for the common case of just a
	 * trip id + route id.
	 */
	public static GtfsTrip gtfsTrip(String routeId, String serviceId,
			String tripId, String tripHeadsign, String tripShortName,
			String directionId, String blockId, String shapeId) {
		return new GtfsTrip(routeId, serviceId, tripId, tripHeadsign,
				tripShortName, directionId, blockId, shapeId);
	}

	/**
	 * A GtfsTrip with fixed defaults and only tripId + routeId varying.
	 */
	public static GtfsTrip simpleGtfsTrip(String tripId, String routeId) {
		return gtfsTrip(routeId, /*serviceId*/ "weekday", tripId,
				/*tripHeadsign*/ "Downtown",
				/*tripShortName*/ tripId + "-short",
				/*directionId*/ "0",
				/*blockId*/ "block-" + tripId,
				/*shapeId*/ "shape-" + tripId);
	}

	/**
	 * Builds a Trip from a GtfsTrip. The first public constructor wires all
	 * the immutable fields; startTime/endTime are still null until
	 * {@link #withScheduleTimes(Trip, List)} is called.
	 */
	public static Trip trip(int configRev, GtfsTrip gtfsTrip) {
		return new Trip(configRev, gtfsTrip, /*properRouteId*/ null,
				/*routeShortName*/ gtfsTrip.getRouteId() + "-short",
				/*unprocessedHeadsign*/ gtfsTrip.getTripHeadsign(),
				NULL_TITLE_FORMATTER);
	}

	/**
	 * Convenience: builds a Trip with {@link #DEFAULT_CONFIG_REV}.
	 */
	public static Trip trip(GtfsTrip gtfsTrip) {
		return trip(DEFAULT_CONFIG_REV, gtfsTrip);
	}

	/**
	 * Adds schedule times in place and returns the same Trip so calls chain.
	 * The Trip constructor leaves startTime/endTime null; addScheduleTimes
	 * derives them from the min/max across the added schedule times.
	 */
	public static Trip withScheduleTimes(Trip trip, List<ScheduleTime> times) {
		trip.addScheduleTimes(times);
		return trip;
	}

	/**
	 * A trip with a tripId, routeId, and a list of (arrival, departure)
	 * seconds-of-day pairs. Pass {@code null} for arrival or departure to
	 * model mid-trip stops (departure only) or end-of-trip stops (arrival
	 * only). Returns the fully-built Trip.
	 */
	public static Trip tripWithStops(String tripId, String routeId,
			List<ScheduleTime> scheduleTimes) {
		return withScheduleTimes(
				trip(simpleGtfsTrip(tripId, routeId)),
				scheduleTimes);
	}

	/**
	 * Builds a Block wrapping the given trips, with start/end time inferred
	 * from the trips' start/end times. Callers that want to control the
	 * block times explicitly should use {@link #block} instead.
	 */
	public static Block blockOf(String blockId, String serviceId,
			Trip... trips) {
		List<Trip> tripList = Arrays.asList(trips);
		int startTime = Integer.MAX_VALUE;
		int endTime = Integer.MIN_VALUE;
		for (Trip t : tripList) {
			if (t.getStartTime() != null && t.getStartTime() < startTime)
				startTime = t.getStartTime();
			if (t.getEndTime() != null && t.getEndTime() > endTime)
				endTime = t.getEndTime();
		}
		// If no trip has a schedule, fall back to zero-length block at time 0.
		if (startTime == Integer.MAX_VALUE) {
			startTime = 0;
			endTime = 0;
		}
		return new Block(DEFAULT_CONFIG_REV, blockId, serviceId,
				startTime, endTime, tripList);
	}

	/**
	 * Full-control Block constructor for when inferred times aren't what the
	 * test wants.
	 */
	public static Block block(int configRev, String blockId, String serviceId,
			int startTime, int endTime, List<Trip> trips) {
		return new Block(configRev, blockId, serviceId, startTime, endTime,
				trips);
	}

	/**
	 * Builds a StopPath with the minimum fields populated. Most flag/break
	 * fields default to something plausible for a regular mid-trip stop.
	 */
	public static StopPath stopPath(int configRev, String pathId, String stopId,
			int gtfsStopSeq, String routeId, boolean lastStopInTrip,
			boolean layoverStop, boolean waitStop,
			boolean scheduleAdherenceStop, Integer breakTime) {
		return new StopPath(configRev, pathId, stopId, gtfsStopSeq,
				lastStopInTrip, routeId, layoverStop, waitStop,
				scheduleAdherenceStop, breakTime,
				/*maxDistance*/ null, /*maxSpeed*/ null,
				/*shapeDistanceTraveled*/ null);
	}

	/**
	 * A plain mid-trip stop path — not a layover, not a wait stop, no break,
	 * not schedule-adherence, not the last stop in trip.
	 */
	public static StopPath simpleStopPath(String pathId, String stopId,
			int gtfsStopSeq) {
		return stopPath(DEFAULT_CONFIG_REV, pathId, stopId, gtfsStopSeq,
				/*routeId*/ "route1", /*lastStopInTrip*/ false,
				/*layoverStop*/ false, /*waitStop*/ false,
				/*scheduleAdherenceStop*/ false, /*breakTime*/ null);
	}

	/**
	 * Attaches an ArrayList of locations to the StopPath (the only form
	 * setLocations accepts). Returns the same StopPath so calls chain.
	 */
	public static StopPath withLocations(StopPath stopPath,
			Location... locations) {
		ArrayList<Location> list = new ArrayList<>(Arrays.asList(locations));
		stopPath.setLocations(list);
		return stopPath;
	}

	/**
	 * A convenience for producing a {@code ScheduleTime} where only one of
	 * arrival/departure is set — the common shape for mid-trip vs end-of-trip
	 * stops. Positional args keep tests readable.
	 */
	public static ScheduleTime departOnly(int departureSeconds) {
		return new ScheduleTime(null, departureSeconds);
	}

	public static ScheduleTime arriveOnly(int arrivalSeconds) {
		return new ScheduleTime(arrivalSeconds, null);
	}

	public static ScheduleTime arriveDepart(int arrivalSeconds,
			int departureSeconds) {
		return new ScheduleTime(arrivalSeconds, departureSeconds);
	}

	/**
	 * Returns an immutable empty schedule-times list for tests that want to
	 * exercise a Trip without any schedule data.
	 */
	public static List<ScheduleTime> noScheduleTimes() {
		return Collections.emptyList();
	}
}
