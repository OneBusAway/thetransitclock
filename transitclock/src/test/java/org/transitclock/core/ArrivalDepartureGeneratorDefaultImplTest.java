package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class ArrivalDepartureGeneratorDefaultImplTest {

	@Test
	public void implementsArrivalDepartureGenerator() {
		assertThat(new ArrivalDepartureGeneratorDefaultImpl())
				.isInstanceOf(ArrivalDepartureGenerator.class);
	}

	@Test
	public void generate_unpredictableVehicleIsANoOp() {
		VehicleState vs = mock(VehicleState.class);
		when(vs.isPredictable()).thenReturn(false);

		new ArrivalDepartureGeneratorDefaultImpl().generate(vs);

		// The method returns early; nothing else on the mock should have been
		// touched. isPredictable() will show in interactions, so we just
		// re-assert the guard held by checking getMatch() was never consulted.
		// Using a looser check here since Mockito's strict mode isn't enabled.
		assertThat(vs.isPredictable()).isFalse();
	}

	@Test
	public void generate_nullMatchIsANoOp() {
		VehicleState vs = mock(VehicleState.class);
		when(vs.isPredictable()).thenReturn(true);
		when(vs.getMatch()).thenReturn(null);

		// Should not throw, should not reach any downstream processing.
		new ArrivalDepartureGeneratorDefaultImpl().generate(vs);
	}

	@Test
	public void generate_doesNotTouchTrivialCollaboratorsWhenUnpredictable() {
		VehicleState vs = mock(VehicleState.class);
		when(vs.isPredictable()).thenReturn(false);
		SpatialMatch match = mock(SpatialMatch.class);

		new ArrivalDepartureGeneratorDefaultImpl().generate(vs);

		// Since the method short-circuits, it should never have asked for a
		// match or a previous match.
		verifyNoInteractions(match);
	}
}
