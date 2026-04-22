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
import org.transitclock.gtfs.gtfsStructs.GtfsFareAttribute;

public class FareAttributeTest {

	private static final String[] HEADERS = { "fare_id", "price", "currency_type",
			"payment_method", "transfers", "transfer_duration" };

	private static CSVRecord row(String... values) throws IOException {
		String line = String.join(",", values);
		CSVFormat fmt = CSVFormat.DEFAULT.withHeader(HEADERS);
		try (CSVParser parser = new CSVParser(new StringReader(line), fmt)) {
			return parser.getRecords().get(0);
		}
	}

	private static GtfsFareAttribute gtfs(String fareId, String price,
			String currency, String paymentMethod, String transfers,
			String transferDuration) throws IOException {
		return new GtfsFareAttribute(
				row(fareId, price, currency, paymentMethod, transfers, transferDuration),
				false, "fare_attributes.txt");
	}

	@Test
	public void gettersRoundTripFromConstructor() throws Exception {
		FareAttribute fa = new FareAttribute(7,
				gtfs("adult", "2.75", "USD", "0", "2", "7200"));
		assertEquals(7, fa.getConfigRev());
		assertEquals("adult", fa.getFareId());
		assertEquals(2.75f, fa.getPrice(), 0.0001f);
		assertEquals("USD", fa.getCurrencyType());
		assertEquals("0", fa.getPaymentMethod());
		assertEquals("2", fa.getTransfers());
		assertEquals(Integer.valueOf(7200), fa.getTransferDuration());
	}

	@Test
	public void optionalFieldsAreNullWhenAbsent() throws Exception {
		// "transfers" is documented as required in GTFS but the code reads it as
		// optional; transfer_duration is truly optional. Empty strings normalize
		// to null in CsvBase.getValue().
		FareAttribute fa = new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "", ""));
		assertNull(fa.getTransfers());
		assertNull(fa.getTransferDuration());
	}

	@Test
	public void equalsMatchesOnAllFields() throws Exception {
		FareAttribute a = new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "2", "7200"));
		FareAttribute b = new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "2", "7200"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachField() throws Exception {
		FareAttribute base = new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "2", "7200"));

		assertNotEquals(base, new FareAttribute(2,
				gtfs("adult", "2.75", "USD", "0", "2", "7200")));
		assertNotEquals(base, new FareAttribute(1,
				gtfs("student", "2.75", "USD", "0", "2", "7200")));
		assertNotEquals(base, new FareAttribute(1,
				gtfs("adult", "3.25", "USD", "0", "2", "7200")));
		assertNotEquals(base, new FareAttribute(1,
				gtfs("adult", "2.75", "EUR", "0", "2", "7200")));
		assertNotEquals(base, new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "1", "2", "7200")));
		assertNotEquals(base, new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "0", "7200")));
		assertNotEquals(base, new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "2", "3600")));
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		FareAttribute fa = new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "2", "7200"));
		assertFalse(fa.equals(null));
		assertFalse(fa.equals("not a FareAttribute"));
	}

	@Test
	public void toStringMentionsAllFields() throws Exception {
		String s = new FareAttribute(1,
				gtfs("adult", "2.75", "USD", "0", "2", "7200")).toString();
		assertTrue(s.startsWith("FareAttribute ["));
		assertTrue(s.contains("configRev=1"));
		assertTrue(s.contains("fareId=adult"));
		assertTrue(s.contains("price=2.75"));
		assertTrue(s.contains("currencyType=USD"));
		assertTrue(s.contains("paymentMethod=0"));
		assertTrue(s.contains("transfers=2"));
		assertTrue(s.contains("transferDuration=7200"));
	}
}
