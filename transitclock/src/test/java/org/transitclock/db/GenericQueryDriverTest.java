/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 */

package org.transitclock.db;

import org.junit.Test;

/**
 * Verifies the modern MySQL Connector/J driver class is on the test
 * classpath. {@link GenericQuery#getConnection} hardcodes
 * {@code com.mysql.cj.jdbc.Driver} (the legacy {@code com.mysql.jdbc.Driver}
 * was retired in Connector/J 8 and is on borrowed time as a compatibility
 * shim — see Phase A review I3). A swallowed {@code ClassNotFoundException}
 * here would silently degrade real connections at runtime, so pin the
 * class name with a load-or-fail check.
 */
public class GenericQueryDriverTest {

	@Test
	public void modernMysqlDriverClassIsLoadable() throws ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
	}

	@Test
	public void postgresDriverClassIsLoadable() throws ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
	}
}
