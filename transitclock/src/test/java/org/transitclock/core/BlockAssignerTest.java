package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.transitclock.applications.Core;
import org.transitclock.db.structs.AvlReport;
import org.transitclock.db.structs.Block;
import org.transitclock.db.structs.Trip;
import org.transitclock.gtfs.DbConfig;

public class BlockAssignerTest {

	@Test
	public void getInstance_returnsSingleton() {
		assertThat(BlockAssigner.getInstance())
				.isNotNull()
				.isSameAs(BlockAssigner.getInstance());
	}

	@Test
	public void getBlockAssignment_nullReportReturnsNull() {
		assertThat(BlockAssigner.getInstance().getBlockAssignment(null)).isNull();
	}

	@Test
	public void getBlockAssignment_nullAssignmentIdReturnsNull() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn(null);

		assertThat(BlockAssigner.getInstance().getBlockAssignment(report)).isNull();
	}

	@Test
	public void getBlockAssignment_routeTypeReturnsNull() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("route-1");
		when(report.isBlockIdAssignmentType()).thenReturn(false);
		when(report.isTripIdAssignmentType()).thenReturn(false);
		when(report.isTripShortNameAssignmentType()).thenReturn(false);
		when(report.isRouteIdAssignmentType()).thenReturn(true);

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Core core = mock(Core.class);
			when(core.getDbConfig()).thenReturn(mock(DbConfig.class));
			coreMock.when(Core::getInstance).thenReturn(core);

			assertThat(BlockAssigner.getInstance().getBlockAssignment(report)).isNull();
		}
	}

	@Test
	public void getBlockAssignment_blockTypeFindsActiveBlockForServiceId() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("block-42");
		when(report.getVehicleId()).thenReturn("veh-1");
		when(report.isBlockIdAssignmentType()).thenReturn(true);
		when(report.getDate()).thenReturn(new Date(1_700_000_000_000L));
		when(report.getTime()).thenReturn(1_700_000_000_000L);

		Block block = mock(Block.class);
		when(block.getId()).thenReturn("block-42");
		when(block.isActive(1_700_000_000_000L, 90 * 60)).thenReturn(true);

		DbConfig dbConfig = mock(DbConfig.class);
		when(dbConfig.getBlock("weekday", "block-42")).thenReturn(block);

		ServiceUtils serviceUtils = mock(ServiceUtils.class);
		when(serviceUtils.getServiceIds(report.getDate()))
				.thenReturn(Collections.singletonList("weekday"));

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Core core = mock(Core.class);
			when(core.getDbConfig()).thenReturn(dbConfig);
			when(core.getServiceUtils()).thenReturn(serviceUtils);
			coreMock.when(Core::getInstance).thenReturn(core);

			Block result = BlockAssigner.getInstance().getBlockAssignment(report);

			assertThat(result).isSameAs(block);
		}
	}

	@Test
	public void getBlockAssignment_blockTypeNoMatchReturnsNull() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("block-unknown");
		when(report.getVehicleId()).thenReturn("veh-1");
		when(report.isBlockIdAssignmentType()).thenReturn(true);
		when(report.getDate()).thenReturn(new Date(1_700_000_000_000L));

		DbConfig dbConfig = mock(DbConfig.class);
		when(dbConfig.getBlock("weekday", "block-unknown")).thenReturn(null);

		ServiceUtils serviceUtils = mock(ServiceUtils.class);
		when(serviceUtils.getServiceIds(report.getDate()))
				.thenReturn(Collections.singletonList("weekday"));

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Core core = mock(Core.class);
			when(core.getDbConfig()).thenReturn(dbConfig);
			when(core.getServiceUtils()).thenReturn(serviceUtils);
			coreMock.when(Core::getInstance).thenReturn(core);

			assertThat(BlockAssigner.getInstance().getBlockAssignment(report)).isNull();
		}
	}

	@Test
	public void getBlockAssignment_blockTypeChoosesActiveOverInactiveForMultipleServiceIds() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("block-42");
		when(report.getVehicleId()).thenReturn("veh-1");
		when(report.isBlockIdAssignmentType()).thenReturn(true);
		when(report.getDate()).thenReturn(new Date(1_700_000_000_000L));
		when(report.getTime()).thenReturn(1_700_000_000_000L);

		Block yesterdayBlock = mock(Block.class);
		when(yesterdayBlock.getId()).thenReturn("block-42");
		when(yesterdayBlock.isActive(1_700_000_000_000L, 90 * 60)).thenReturn(false);

		Block todayBlock = mock(Block.class);
		when(todayBlock.getId()).thenReturn("block-42");
		when(todayBlock.isActive(1_700_000_000_000L, 90 * 60)).thenReturn(true);

		DbConfig dbConfig = mock(DbConfig.class);
		when(dbConfig.getBlock("yesterday", "block-42")).thenReturn(yesterdayBlock);
		when(dbConfig.getBlock("today", "block-42")).thenReturn(todayBlock);

		ServiceUtils serviceUtils = mock(ServiceUtils.class);
		// "yesterday" first so the initial activeBlock is the inactive one;
		// then "today" arrives and isActive replaces it.
		when(serviceUtils.getServiceIds(report.getDate()))
				.thenReturn(Arrays.asList("yesterday", "today"));

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Core core = mock(Core.class);
			when(core.getDbConfig()).thenReturn(dbConfig);
			when(core.getServiceUtils()).thenReturn(serviceUtils);
			coreMock.when(Core::getInstance).thenReturn(core);

			Block result = BlockAssigner.getInstance().getBlockAssignment(report);

			assertThat(result).isSameAs(todayBlock);
		}
	}

	@Test
	public void getBlockAssignment_tripIdTypeReturnsTripsBlock() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("trip-1");
		when(report.getVehicleId()).thenReturn("veh-1");
		when(report.isBlockIdAssignmentType()).thenReturn(false);
		when(report.isTripIdAssignmentType()).thenReturn(true);

		Block block = mock(Block.class);
		when(block.getId()).thenReturn("block-99");

		Trip trip = mock(Trip.class);
		when(trip.getBlock()).thenReturn(block);

		DbConfig dbConfig = mock(DbConfig.class);
		when(dbConfig.getTrip("trip-1")).thenReturn(trip);

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Core core = mock(Core.class);
			when(core.getDbConfig()).thenReturn(dbConfig);
			coreMock.when(Core::getInstance).thenReturn(core);

			assertThat(BlockAssigner.getInstance().getBlockAssignment(report)).isSameAs(block);
		}
	}

	@Test
	public void getBlockAssignment_tripIdTypeUnknownTripReturnsNull() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("trip-unknown");
		when(report.getVehicleId()).thenReturn("veh-1");
		when(report.isTripIdAssignmentType()).thenReturn(true);

		DbConfig dbConfig = mock(DbConfig.class);
		when(dbConfig.getTrip("trip-unknown")).thenReturn(null);

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Core core = mock(Core.class);
			when(core.getDbConfig()).thenReturn(dbConfig);
			coreMock.when(Core::getInstance).thenReturn(core);

			assertThat(BlockAssigner.getInstance().getBlockAssignment(report)).isNull();
		}
	}

	@Test
	public void getBlockAssignment_tripShortNameTypeReturnsTripsBlock() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("short-A");
		when(report.getVehicleId()).thenReturn("veh-1");
		when(report.isTripShortNameAssignmentType()).thenReturn(true);

		Block block = mock(Block.class);
		when(block.getId()).thenReturn("block-7");
		Trip trip = mock(Trip.class);
		when(trip.getBlock()).thenReturn(block);

		DbConfig dbConfig = mock(DbConfig.class);
		when(dbConfig.getTripUsingTripShortName("short-A")).thenReturn(trip);

		try (MockedStatic<Core> coreMock = mockStatic(Core.class)) {
			Core core = mock(Core.class);
			when(core.getDbConfig()).thenReturn(dbConfig);
			coreMock.when(Core::getInstance).thenReturn(core);

			assertThat(BlockAssigner.getInstance().getBlockAssignment(report)).isSameAs(block);
		}
	}

	@Test
	public void getRouteIdAssignment_returnsAssignmentWhenRouteType() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("route-9");
		when(report.isRouteIdAssignmentType()).thenReturn(true);

		assertThat(BlockAssigner.getInstance().getRouteIdAssignment(report)).isEqualTo("route-9");
	}

	@Test
	public void getRouteIdAssignment_returnsNullWhenNotRouteType() {
		AvlReport report = mock(AvlReport.class);
		when(report.getAssignmentId()).thenReturn("block-5");
		when(report.isRouteIdAssignmentType()).thenReturn(false);

		assertThat(BlockAssigner.getInstance().getRouteIdAssignment(report)).isNull();
	}

	@Test
	public void getRouteIdAssignment_returnsNullForNullReport() {
		assertThat(BlockAssigner.getInstance().getRouteIdAssignment(null)).isNull();
	}
}
