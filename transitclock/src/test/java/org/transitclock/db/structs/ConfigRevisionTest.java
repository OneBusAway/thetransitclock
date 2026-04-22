package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

public class ConfigRevisionTest {

	@Test
	public void gettersRoundTripFromConstructor() {
		Date processed = new Date(1_700_000_000_000L);
		Date zipMtime = new Date(1_699_000_000_000L);
		ConfigRevision r = new ConfigRevision(42, processed, zipMtime, "initial import");

		assertEquals(42, r.getConfigRev());
		assertEquals(processed, r.getProcessedTime());
		assertEquals(zipMtime, r.getZipFileLastModifiedTime());
		assertEquals("initial import", r.getNotes());
	}

	@Test
	public void nullableFieldsAreAllowed() {
		// zipFileLastModifiedTime is only populated when GTFS comes from a zip,
		// and notes is freeform — both must tolerate null.
		ConfigRevision r = new ConfigRevision(1, new Date(0L), null, null);
		assertNull(r.getZipFileLastModifiedTime());
		assertNull(r.getNotes());
	}

	@Test
	public void toStringMentionsAllFields() {
		Date processed = new Date(1_700_000_000_000L);
		Date zipMtime = new Date(1_699_000_000_000L);
		String s = new ConfigRevision(7, processed, zipMtime, "note").toString();

		assertTrue(s.contains("configRev=7"));
		assertTrue(s.contains("processedTime="));
		assertTrue(s.contains("zipFileLastModifiedTime="));
		assertTrue(s.contains("notes=note"));
	}
}
