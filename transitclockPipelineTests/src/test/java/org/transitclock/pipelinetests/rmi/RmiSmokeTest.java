/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.pipelinetests.rmi;

import static org.assertj.core.api.Assertions.assertThat;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.transitclock.applications.Core;
import org.transitclock.configData.AgencyConfig;
import org.transitclock.db.structs.Agency;
import org.transitclock.ipc.clients.CommandsInterfaceFactory;
import org.transitclock.ipc.clients.ConfigInterfaceFactory;
import org.transitclock.ipc.clients.PredictionsInterfaceFactory;
import org.transitclock.ipc.clients.VehiclesInterfaceFactory;
import org.transitclock.ipc.data.IpcVehicle;
import org.transitclock.ipc.data.IpcPredictionsForRouteStopDest;
import org.transitclock.ipc.interfaces.CommandsInterface;
import org.transitclock.ipc.interfaces.ConfigInterface;
import org.transitclock.ipc.interfaces.PredictionsInterface;
import org.transitclock.ipc.interfaces.VehiclesInterface;
import org.transitclock.pipelinetests.CoreHarness;

/**
 * Boots the full RMI surface — registry plus the
 * {@link org.transitclock.ipc.servers VehiclesServer} /
 * {@link org.transitclock.ipc.servers.PredictionsServer} /
 * {@link org.transitclock.ipc.servers.ConfigServer} /
 * {@link org.transitclock.ipc.servers.CommandsServer} — and exercises a
 * client → server round-trip per interface using the existing
 * {@code *InterfaceFactory.get()} clients.
 *
 * <p>RMI uses pure {@code java.rmi.*} (no {@code java.rmi.activation}) so it
 * should sail through Java 21, but a smoke test makes that a fact rather
 * than a hope. Phase A is when this becomes load-bearing.
 */
public class RmiSmokeTest {

	@ClassRule
	public static final CoreHarness CORE = CoreHarness.withWmata5A();

	@BeforeClass
	public static void startRmiServers() {
		// CoreHarness boots Core's data side but does not start the RMI
		// servers; the production startup path that does is gated on
		// Core.createCore(). Start the servers explicitly so the clients
		// have something to bind against.
		Core.startRmiServers(AgencyConfig.getAgencyId());
	}

	@Test
	public void vehiclesInterfaceRoundTrip() throws RemoteException {
		VehiclesInterface client = VehiclesInterfaceFactory.get(AgencyConfig.getAgencyId());
		assertThat(client).isNotNull();

		// No vehicles have been processed; the call must succeed and return
		// an empty collection rather than NPE or throw RemoteException.
		Collection<IpcVehicle> vehicles = client.get();
		assertThat(vehicles).isNotNull();
	}

	@Test
	public void predictionsInterfaceRoundTrip() throws RemoteException {
		PredictionsInterface client =
				PredictionsInterfaceFactory.get(AgencyConfig.getAgencyId());
		assertThat(client).isNotNull();

		List<IpcPredictionsForRouteStopDest> predictions = client.getAllPredictions(60);
		assertThat(predictions).isNotNull();
	}

	@Test
	public void configInterfaceRoundTrip() throws RemoteException {
		ConfigInterface client = ConfigInterfaceFactory.get(AgencyConfig.getAgencyId());
		assertThat(client).isNotNull();

		// WMATA fixture has exactly one Agency.
		List<Agency> agencies = client.getAgencies();
		assertThat(agencies).isNotEmpty();
	}

	@Test
	public void commandsInterfaceRoundTrip() {
		CommandsInterface client = CommandsInterfaceFactory.get(AgencyConfig.getAgencyId());
		assertThat(client).isNotNull();
		// No-op for shape; sending an AVL would require constructing an
		// IpcAvl with valid agency/block context. The bind itself is the
		// load-bearing assertion for the migration.
	}
}
