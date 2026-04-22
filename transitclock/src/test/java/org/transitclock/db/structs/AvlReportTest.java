package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.transitclock.configData.AvlConfig;
import org.transitclock.db.structs.AvlReport.AssignmentType;

public class AvlReportTest {

	// Clearly inside the default validation window (North America).
	private static final double SF_LAT = 37.7749;
	private static final double SF_LON = -122.4194;

	private static AvlReport sfReport() {
		return new AvlReport("v1", 1_705_320_000_000L, SF_LAT, SF_LON,
				10.0f, 90.0f, "test");
	}

	@Test
	public void constructorNormalizesNaNSpeedAndHeadingToInvalid() {
		AvlReport r = new AvlReport("v1", 0L, SF_LAT, SF_LON,
				Float.NaN, Float.NaN, "test");
		assertFalse(r.isSpeedValid());
		assertFalse(r.isHeadingValid());
		assertTrue(Float.isNaN(r.getSpeed()));
		assertTrue(Float.isNaN(r.getHeading()));
	}

	@Test
	public void noSpeedConstructorLeavesSpeedAndHeadingInvalid() {
		AvlReport r = new AvlReport("v1", 0L, SF_LAT, SF_LON, "test");
		assertFalse(r.isSpeedValid());
		assertFalse(r.isHeadingValid());
	}

	@Test
	public void locationOverloadIsEquivalentToLatLonOverload() {
		AvlReport fromLatLon = new AvlReport("v1", 100L, SF_LAT, SF_LON,
				10.0f, 45.0f, "test");
		AvlReport fromLocation = new AvlReport("v1", 100L,
				new Location(SF_LAT, SF_LON), 10.0f, 45.0f, "test");
		assertEquals(fromLatLon, fromLocation);
	}

	@Test
	public void sourceLongerThanTenCharsIsTruncated() {
		// SOURCE_LENGTH is private but known to be 10.
		AvlReport r = new AvlReport("v1", 0L, SF_LAT, SF_LON,
				1f, 0f, "source-is-way-too-long");
		assertEquals(10, r.getSource().length());
		assertEquals("source-is-", r.getSource());
	}

	@Test
	public void shortSourceIsUntouched() {
		AvlReport r = new AvlReport("v1", 0L, SF_LAT, SF_LON, 1f, 0f, "svc");
		assertEquals("svc", r.getSource());
	}

	@Test
	public void latLonGettersForwardToLocation() {
		AvlReport r = sfReport();
		assertEquals(SF_LAT, r.getLat(), 1e-9);
		assertEquals(SF_LON, r.getLon(), 1e-9);
		assertEquals(new Location(SF_LAT, SF_LON), r.getLocation());
	}

	@Test
	public void getHeadingReturnsNaNWhenSpeedBelowMinForValidHeading() {
		double minSpeed = AvlConfig.minSpeedForValidHeading();
		// Speed just under the threshold → heading treated as invalid.
		float slow = (float) (minSpeed - 0.1);
		AvlReport r = new AvlReport("v1", 0L, SF_LAT, SF_LON,
				slow, 90f, "test");
		assertTrue(Float.isNaN(r.getHeading()));
	}

	@Test
	public void getHeadingReturnsValueWhenSpeedAboveThreshold() {
		double minSpeed = AvlConfig.minSpeedForValidHeading();
		float fast = (float) (minSpeed + 5.0);
		AvlReport r = new AvlReport("v1", 0L, SF_LAT, SF_LON,
				fast, 90f, "test");
		assertEquals(90.0f, r.getHeading(), 1e-6);
	}

	@Test
	public void getHeadingReturnsValueWhenSpeedIsUnknown() {
		// Speed unknown (NaN) → the low-speed heuristic doesn't apply.
		AvlReport r = new AvlReport("v1", 0L, SF_LAT, SF_LON,
				Float.NaN, 45f, "test");
		assertEquals(45.0f, r.getHeading(), 1e-6);
	}

	@Test
	public void getLatencyIsZeroWhenNotProcessed() {
		AvlReport r = sfReport();
		// setTimeProcessed() delegates to Core.getSystemTime(), which isn't
		// available in a unit test, so we only exercise the unprocessed path
		// here.
		assertEquals(0L, r.getLatency());
	}

	@Test
	public void defaultAssignmentTypeIsUnset() {
		AvlReport r = sfReport();
		assertEquals(AssignmentType.UNSET, r.getAssignmentType());
		assertFalse(r.isBlockIdAssignmentType());
		assertFalse(r.isTripIdAssignmentType());
		assertFalse(r.isTripShortNameAssignmentType());
		assertFalse(r.isRouteIdAssignmentType());
	}

	@Test
	public void blockAssignmentTypeCoversBothBlockVariants() {
		AvlReport regular = sfReport();
		regular.setAssignment("blk1", AssignmentType.BLOCK_ID);
		assertTrue(regular.isBlockIdAssignmentType());

		AvlReport schedBased = sfReport();
		schedBased.setAssignment("blk2",
				AssignmentType.BLOCK_FOR_SCHED_BASED_PREDS);
		assertTrue(schedBased.isBlockIdAssignmentType());
	}

	@Test
	public void tripAndRouteAssignmentPredicatesAreMutuallyExclusive() {
		AvlReport trip = sfReport();
		trip.setAssignment("t1", AssignmentType.TRIP_ID);
		assertTrue(trip.isTripIdAssignmentType());
		assertFalse(trip.isRouteIdAssignmentType());
		assertFalse(trip.isTripShortNameAssignmentType());

		AvlReport route = sfReport();
		route.setAssignment("r1", AssignmentType.ROUTE_ID);
		assertTrue(route.isRouteIdAssignmentType());
		assertFalse(route.isTripIdAssignmentType());

		AvlReport shortName = sfReport();
		shortName.setAssignment("short", AssignmentType.TRIP_SHORT_NAME);
		assertTrue(shortName.isTripShortNameAssignmentType());
	}

	@Test
	public void setAssignmentRejectsNullIdWithNonUnsetType() {
		AvlReport r = sfReport();
		r.setAssignment("good", AssignmentType.BLOCK_ID);
		// This is an invalid combination; implementation logs and returns
		// without updating, so the previous assignment remains.
		r.setAssignment(null, AssignmentType.BLOCK_ID);
		assertEquals("good", r.getAssignmentId());
		assertEquals(AssignmentType.BLOCK_ID, r.getAssignmentType());
	}

	@Test
	public void setAssignmentAllowsNullWithUnsetType() {
		AvlReport r = sfReport();
		r.setAssignment("tmp", AssignmentType.BLOCK_ID);
		r.setAssignment(null, AssignmentType.UNSET);
		assertNull(r.getAssignmentId());
		assertEquals(AssignmentType.UNSET, r.getAssignmentType());
	}

	@Test
	public void validateDataReturnsNullForGoodReport() {
		// Use current time to stay inside the ±10y / +1min tolerance window.
		AvlReport r = new AvlReport("v1", System.currentTimeMillis(),
				SF_LAT, SF_LON, 10f, 90f, "test");
		assertNull(r.validateData());
	}

	@Test
	public void validateDataFlagsNullVehicleId() {
		AvlReport r = new AvlReport(null, System.currentTimeMillis(),
				SF_LAT, SF_LON, 10f, 90f, "test");
		String err = r.validateData();
		assertNotNull(err);
		assertTrue(err.contains("VehicleId is null"));
	}

	@Test
	public void validateDataFlagsEmptyVehicleId() {
		AvlReport r = new AvlReport("", System.currentTimeMillis(),
				SF_LAT, SF_LON, 10f, 90f, "test");
		String err = r.validateData();
		assertNotNull(err);
		assertTrue(err.contains("VehicleId is empty string"));
	}

	@Test
	public void validateDataFlagsOutOfRangeLatitude() {
		// Min lat default is 15.0 for North America.
		AvlReport r = new AvlReport("v1", System.currentTimeMillis(),
				0.0, SF_LON, 10f, 90f, "test");
		String err = r.validateData();
		assertNotNull(err);
		assertTrue("expected latitude error, got: " + err, err.contains("Latitude"));
	}

	@Test
	public void validateDataFlagsNegativeSpeed() {
		AvlReport r = new AvlReport("v1", System.currentTimeMillis(),
				SF_LAT, SF_LON, -5f, 90f, "test");
		String err = r.validateData();
		assertNotNull(err);
		assertTrue(err.contains("Speed"));
	}

	@Test
	public void validateDataFlagsHeadingOutOfRange() {
		AvlReport r = new AvlReport("v1", System.currentTimeMillis(),
				SF_LAT, SF_LON, 10f, 361f, "test");
		String err = r.validateData();
		assertNotNull(err);
		assertTrue(err.contains("Heading"));
	}

	@Test
	public void equalsIsBasedOnAllFields() {
		AvlReport a = sfReport();
		AvlReport b = sfReport();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		AvlReport differentVehicle = new AvlReport("v2",
				a.getTime(), SF_LAT, SF_LON, 10.0f, 90.0f, "test");
		assertNotEquals(a, differentVehicle);

		assertFalse(a.equals(null));
		assertFalse(a.equals("not an AvlReport"));
	}

	@Test
	public void copyConstructorWithNewTimeKeepsOtherFields() {
		AvlReport original = sfReport();
		Date newTime = new Date(original.getTime() + 5_000);
		AvlReport copy = new AvlReport(original, newTime);

		assertEquals(original.getVehicleId(), copy.getVehicleId());
		assertEquals(original.getLocation(), copy.getLocation());
		assertEquals(newTime.getTime(), copy.getTime());
		assertEquals(original.getSpeed(), copy.getSpeed(), 1e-6);
	}

	@Test
	public void copyConstructorWithAssignmentReplacesAssignment() {
		AvlReport original = sfReport();
		AvlReport copy = new AvlReport(original, "b1", AssignmentType.BLOCK_ID);
		assertEquals("b1", copy.getAssignmentId());
		assertEquals(AssignmentType.BLOCK_ID, copy.getAssignmentType());
		assertEquals(original.getVehicleId(), copy.getVehicleId());
		assertEquals(original.getTime(), copy.getTime());
	}
}
