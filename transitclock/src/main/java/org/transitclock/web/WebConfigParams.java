/*
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 * 
 * Transitime.org is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Transitime.org . If not, see <http://www.gnu.org/licenses/>.
 */
package org.transitclock.web;

import org.transitclock.config.StringConfigValue;

/**
 * Contains Java properties use by web server. These parameters are read in
 * using ReadConfigListener class.
 * 
 * @author Michael Smith
 *
 */
public class WebConfigParams {
	public static String getMapTileUrl() {
		return mapTileUrl.getValue();
	}
	private static StringConfigValue mapTileUrl =
			new StringConfigValue("transitclock.web.mapTileUrl",
					"https://tile.openstreetmap.org/{z}/{x}/{y}.png",
					"Specifies the URL used by Leaflet maps to fetch map "
					+ "tiles. The default points at the OpenStreetMap "
					+ "public tile server. This is fine for development "
					+ "and trial deployments, but the OSMF Tile Usage "
					+ "Policy "
					+ "(https://operations.osmfoundation.org/policies/tiles/) "
					+ "explicitly forbids using OSM's public tiles as the "
					+ "default for an app with end-users, requires an "
					+ "identifying User-Agent, and reserves the right to "
					+ "block heavy traffic. Production deployments must "
					+ "override this with a paid tile provider (Mapbox, "
					+ "MapTiler, Thunderforest, …) or a self-hosted tile "
					+ "server.");

	public static String getMapTileCopyright() {
		return mapTileCopyright.getValue();
	}
	private static StringConfigValue mapTileCopyright =
			new StringConfigValue("transitclock.web.mapTileCopyright",
					"OpenStreetMap",
					"Map attribution shown next to the OSM credit. Match "
					+ "this to whoever is actually serving the tiles "
					+ "configured by transitclock.web.mapTileUrl.");
}
