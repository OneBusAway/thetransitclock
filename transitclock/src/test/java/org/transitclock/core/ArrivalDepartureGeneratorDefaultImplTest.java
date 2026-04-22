package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

		verify(vs).isPredictable();
		verify(vs, never()).getMatch();
	}

	@Test
	public void generate_nullMatchIsANoOp() {
		VehicleState vs = mock(VehicleState.class);
		when(vs.isPredictable()).thenReturn(true);
		when(vs.getMatch()).thenReturn(null);

		new ArrivalDepartureGeneratorDefaultImpl().generate(vs);

		verify(vs).isPredictable();
		verify(vs).getMatch();
		verify(vs, never()).getPreviousMatch();
	}

	@Test
	public void generate_doesNotTouchTrivialCollaboratorsWhenUnpredictable() {
		VehicleState vs = mock(VehicleState.class);
		when(vs.isPredictable()).thenReturn(false);
		TemporalMatch match = mock(TemporalMatch.class);
		when(vs.getMatch()).thenReturn(match);

		new ArrivalDepartureGeneratorDefaultImpl().generate(vs);

		verify(vs).isPredictable();
		// Guard short-circuited before ever reading match off the state.
		verify(vs, never()).getMatch();
		verifyNoInteractions(match);
	}
}
