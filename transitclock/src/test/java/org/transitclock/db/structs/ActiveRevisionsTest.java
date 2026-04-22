package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ActiveRevisionsTest {

	@Test
	public void defaultConstructorUsesSentinelRevisions() {
		ActiveRevisions r = new ActiveRevisions();
		assertEquals(-1, r.getConfigRev());
		assertEquals(-1, r.getTravelTimesRev());
	}

	@Test
	public void settersUpdateRevisions() {
		ActiveRevisions r = new ActiveRevisions();
		r.setConfigRev(7);
		r.setTravelTimesRev(3);
		assertEquals(7, r.getConfigRev());
		assertEquals(3, r.getTravelTimesRev());
	}

	@Test
	public void isValidRequiresBothNonNegative() {
		ActiveRevisions r = new ActiveRevisions();
		assertFalse("default -1/-1 is not valid", r.isValid());

		r.setConfigRev(0);
		assertFalse("only configRev set, travelTimesRev still -1", r.isValid());

		r.setConfigRev(-1);
		r.setTravelTimesRev(0);
		assertFalse("only travelTimesRev set, configRev still -1", r.isValid());

		r.setConfigRev(0);
		assertTrue("both zero is valid", r.isValid());

		r.setConfigRev(7);
		r.setTravelTimesRev(3);
		assertTrue(r.isValid());
	}

	@Test
	public void toStringMentionsBothRevisions() {
		ActiveRevisions r = new ActiveRevisions();
		r.setConfigRev(5);
		r.setTravelTimesRev(9);
		String s = r.toString();
		assertTrue(s.contains("configRev=5"));
		assertTrue(s.contains("travelTimesRev=9"));
	}
}
