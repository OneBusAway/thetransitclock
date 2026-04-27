/*
 * This file is part of Transitime.org
 *
 * Transitime.org is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License (GPL) as published by the
 * Free Software Foundation, either version 3 of the License, or any later
 * version.
 */
package org.transitclock.pipelinetests.persistence;

import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.transitclock.configData.AgencyConfig;
import org.transitclock.db.hibernate.HibernateUtils;

/** Shared session/transaction helpers for the persistence tests. */
final class PersistenceTestSupport {

	private PersistenceTestSupport() {
	}

	static Session openSession() {
		return HibernateUtils.getSession(AgencyConfig.getAgencyId());
	}

	static <T> T inSession(Function<Session, T> body) {
		try (Session session = openSession()) {
			return body.apply(session);
		}
	}

	static <T> T inSessionWithCommit(Function<Session, T> body) {
		try (Session session = openSession()) {
			Transaction tx = session.beginTransaction();
			try {
				T result = body.apply(session);
				tx.commit();
				return result;
			} catch (RuntimeException e) {
				tx.rollback();
				throw e;
			}
		}
	}
}
