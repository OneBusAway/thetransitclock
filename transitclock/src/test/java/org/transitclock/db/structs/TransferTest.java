package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Test;
import org.transitclock.gtfs.gtfsStructs.GtfsTransfer;

public class TransferTest {

	private static final String[] HEADERS =
			{ "from_stop_id", "to_stop_id", "transfer_type", "min_transfer_time" };

	private static CSVRecord row(String... values) throws IOException {
		String line = String.join(",", values);
		CSVFormat fmt = CSVFormat.DEFAULT.withHeader(HEADERS);
		try (CSVParser parser = new CSVParser(new StringReader(line), fmt)) {
			return parser.getRecords().get(0);
		}
	}

	private static GtfsTransfer gtfs(String fromStop, String toStop,
			String transferType, String minTransferTime) throws IOException {
		return new GtfsTransfer(
				row(fromStop, toStop, transferType, minTransferTime),
				false, "transfers.txt");
	}

	@Test
	public void gettersRoundTripFromConstructor() throws Exception {
		Transfer t = new Transfer(5, gtfs("stopA", "stopB", "1", "300"));
		assertEquals(5, t.getConfigRev());
		assertEquals("stopA", t.getFromStopId());
		assertEquals("stopB", t.getToStopId());
		assertEquals("1", t.getTransferType());
		assertEquals(Integer.valueOf(300), t.getMinTransferTime());
	}

	@Test
	public void missingMinTransferTimeIsNull() throws Exception {
		// min_transfer_time is optional — empty string normalizes to null.
		Transfer t = new Transfer(1, gtfs("a", "b", "0", ""));
		assertNull(t.getMinTransferTime());
	}

	@Test
	public void equalsMatchesOnAllFields() throws Exception {
		Transfer a = new Transfer(1, gtfs("stopA", "stopB", "1", "300"));
		Transfer b = new Transfer(1, gtfs("stopA", "stopB", "1", "300"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachField() throws Exception {
		Transfer base = new Transfer(1, gtfs("stopA", "stopB", "1", "300"));

		assertNotEquals(base, new Transfer(2, gtfs("stopA", "stopB", "1", "300")));
		assertNotEquals(base, new Transfer(1, gtfs("X",     "stopB", "1", "300")));
		assertNotEquals(base, new Transfer(1, gtfs("stopA", "X",     "1", "300")));
		assertNotEquals(base, new Transfer(1, gtfs("stopA", "stopB", "2", "300")));
		assertNotEquals(base, new Transfer(1, gtfs("stopA", "stopB", "1", "301")));
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		Transfer t = new Transfer(1, gtfs("a", "b", "0", "60"));
		assertFalse(t.equals(null));
		assertFalse(t.equals("not a Transfer"));
	}

	@Test
	public void toStringMentionsAllFields() throws Exception {
		String s = new Transfer(1, gtfs("stopA", "stopB", "1", "300")).toString();
		assertTrue(s.startsWith("Transfer ["));
		assertTrue(s.contains("configRev=1"));
		assertTrue(s.contains("fromStopId=stopA"));
		assertTrue(s.contains("toStopId=stopB"));
		assertTrue(s.contains("transferType=1"));
		assertTrue(s.contains("minTransferTime=300"));
	}
}
