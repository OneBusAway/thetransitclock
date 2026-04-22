package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public class PredictionForStopPathTest {

	private static PredictionForStopPath build() {
		return new PredictionForStopPath(
				"v1",
				new Date(1_700_000_000_000L),
				42_000.0,
				"t1",
				3,
				"alg1",
				true,
				21_600);
	}

	@Test
	public void valueConstructorPopulatesAllFields() {
		PredictionForStopPath p = build();
		assertEquals("v1", p.getVehicleId());
		assertEquals(new Date(1_700_000_000_000L), p.getCreationTime());
		assertEquals(Double.valueOf(42_000.0), p.getPredictionTime());
		assertEquals("t1", p.getTripId());
		assertEquals(Integer.valueOf(3), p.getStopPathIndex());
		assertEquals("alg1", p.getAlgorithm());
		assertTrue(p.isTravelTime());
		assertEquals(Integer.valueOf(21_600), p.getStartTime());
	}

	@Test
	public void noArgConstructorDefaultsTravelTimeToTrue() {
		// Value ctor defaults differ from no-arg: the no-arg path sets travelTime
		// = true and nulls out every other field.
		PredictionForStopPath p = new PredictionForStopPath();
		assertNull(p.getVehicleId());
		assertNull(p.getCreationTime());
		assertNull(p.getPredictionTime());
		assertNull(p.getTripId());
		assertNull(p.getStopPathIndex());
		assertNull(p.getAlgorithm());
		assertNull(p.getStartTime());
		assertTrue(p.isTravelTime());
	}

	@Test
	public void settersRoundTrip() {
		PredictionForStopPath p = new PredictionForStopPath();
		Date created = new Date(99L);
		p.setVehicleId("vX");
		p.setCreationTime(created);
		p.setPredictionTime(1.5);
		p.setTripId("tX");
		p.setStopPathIndex(9);
		p.setAlgorithm("algX");
		p.setStartTime(600);
		p.setTravelTime(false);

		assertEquals("vX", p.getVehicleId());
		assertEquals(created, p.getCreationTime());
		assertEquals(Double.valueOf(1.5), p.getPredictionTime());
		assertEquals("tX", p.getTripId());
		assertEquals(Integer.valueOf(9), p.getStopPathIndex());
		assertEquals("algX", p.getAlgorithm());
		assertEquals(Integer.valueOf(600), p.getStartTime());
		assertFalse(p.isTravelTime());
	}

	@Test
	public void equalsMatchesOnAllFields() {
		assertEquals(build(), build());
		assertEquals(build().hashCode(), build().hashCode());
	}

	@Test
	public void equalsFlipsOnMutatedFields() {
		PredictionForStopPath base = build();

		PredictionForStopPath diffVehicle = build();
		diffVehicle.setVehicleId("v2");
		assertNotEquals(base, diffVehicle);

		PredictionForStopPath diffTrip = build();
		diffTrip.setTripId("t2");
		assertNotEquals(base, diffTrip);

		PredictionForStopPath diffAlg = build();
		diffAlg.setAlgorithm("other");
		assertNotEquals(base, diffAlg);

		PredictionForStopPath diffStopIdx = build();
		diffStopIdx.setStopPathIndex(99);
		assertNotEquals(base, diffStopIdx);

		PredictionForStopPath diffStart = build();
		diffStart.setStartTime(12345);
		assertNotEquals(base, diffStart);

		PredictionForStopPath diffTravel = build();
		diffTravel.setTravelTime(false);
		assertNotEquals(base, diffTravel);

		PredictionForStopPath diffPredTime = build();
		diffPredTime.setPredictionTime(99.0);
		assertNotEquals(base, diffPredTime);

		PredictionForStopPath diffCreation = build();
		diffCreation.setCreationTime(new Date(1L));
		assertNotEquals(base, diffCreation);
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() {
		PredictionForStopPath p = build();
		assertFalse(p.equals(null));
		assertFalse(p.equals("not a PredictionForStopPath"));
	}
}
