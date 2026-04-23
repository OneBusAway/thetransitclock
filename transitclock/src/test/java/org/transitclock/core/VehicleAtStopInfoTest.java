package org.transitclock.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * VehicleAtStopInfo is a thin Indices subclass whose entire purpose is to
 * (a) pin the segmentIndex to 0 and (b) carry "at a stop" semantics for the
 * vehicle-state machine. Both constructor forms and the getClass()-based
 * equality with Indices are the behaviors worth locking down here.
 *
 * The methods that dereference the Block (toString, getStopId, atEndOfBlock)
 * require a fully-wired Block+Trip+StopPath graph and are exercised via the
 * integration tests. These unit tests stick to the null-block branch, mirroring
 * IndicesTest.
 */
public class VehicleAtStopInfoTest {

	@Test
	public void blockConstructorForcesSegmentIndexToZero() {
		VehicleAtStopInfo info = new VehicleAtStopInfo(null, 2, 5);
		assertEquals(2, info.getTripIndex());
		assertEquals(5, info.getStopPathIndex());
		assertEquals(0, info.getSegmentIndex());
		assertNull(info.getBlock());
	}

	@Test
	public void indicesConstructorCopiesFieldsAndForcesSegmentIndexToZero() {
		// Source Indices has a non-zero segment index. VehicleAtStopInfo
		// deliberately drops that — once we know a vehicle is at a stop the
		// segment offset within the stop path is no longer meaningful.
		Indices source = new Indices(null, 2, 5, 7);
		VehicleAtStopInfo info = new VehicleAtStopInfo(source);
		assertEquals(2, info.getTripIndex());
		assertEquals(5, info.getStopPathIndex());
		assertEquals(0, info.getSegmentIndex());
		assertNull(info.getBlock());
	}

	@Test
	public void bothConstructorFormsProduceEquivalentInstances() {
		VehicleAtStopInfo fromBlock = new VehicleAtStopInfo(null, 3, 4);
		VehicleAtStopInfo fromIndices =
				new VehicleAtStopInfo(new Indices(null, 3, 4, 0));
		// Indices defines equals() but not hashCode(), so two equal instances
		// won't have matching hash codes. Only assert logical equality here.
		assertEquals(fromBlock, fromIndices);
	}

	@Test
	public void vehicleAtStopInfoNotEqualToPlainIndicesWithSameFields() {
		// Indices.equals uses getClass()-based comparison, so subclass
		// instances never compare equal to base-class instances even when
		// the fields all match. This keeps different kinds of indices from
		// colliding in sets/maps.
		VehicleAtStopInfo sub = new VehicleAtStopInfo(null, 1, 2);
		Indices base = new Indices(null, 1, 2, 0);
		assertFalse(sub.equals(base));
		assertFalse(base.equals(sub));
	}

	@Test
	public void twoVehicleAtStopInfosWithSameFieldsAreEqual() {
		VehicleAtStopInfo a = new VehicleAtStopInfo(null, 1, 2);
		VehicleAtStopInfo b = new VehicleAtStopInfo(null, 1, 2);
		assertTrue(a.equals(b));
	}

	@Test
	public void differentStopPathIndexMakesInstancesUnequal() {
		VehicleAtStopInfo a = new VehicleAtStopInfo(null, 1, 2);
		VehicleAtStopInfo b = new VehicleAtStopInfo(null, 1, 3);
		assertFalse(a.equals(b));
	}

	@Test
	public void differentTripIndexMakesInstancesUnequal() {
		VehicleAtStopInfo a = new VehicleAtStopInfo(null, 1, 2);
		VehicleAtStopInfo b = new VehicleAtStopInfo(null, 5, 2);
		assertFalse(a.equals(b));
	}

	@Test
	public void segmentIndexIgnoredInSource() {
		// Even if the source Indices has a huge segment index, the derived
		// VehicleAtStopInfo normalizes it to 0.
		Indices source = new Indices(null, 0, 0, 999);
		VehicleAtStopInfo info = new VehicleAtStopInfo(source);
		assertEquals(0, info.getSegmentIndex());
	}
}
