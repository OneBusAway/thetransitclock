package org.transitclock.db.structs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.util.Date;

import org.junit.Test;

/**
 * MonitoringEvent.create() pushes the event through Core's DataDbLogger, so we
 * can't use it in a unit test. The private 5-arg value constructor contains
 * the logic we want to pin (message truncation at 512, NaN → 0.0 coercion).
 * We reach it via reflection.
 */
public class MonitoringEventTest {

	private static final int MAX_MESSAGE_LENGTH = 512;

	private static MonitoringEvent build(Date time, String type,
			boolean triggered, String message, double value) throws Exception {
		Constructor<MonitoringEvent> ctor = MonitoringEvent.class
				.getDeclaredConstructor(Date.class, String.class,
						boolean.class, String.class, double.class);
		ctor.setAccessible(true);
		return ctor.newInstance(time, type, triggered, message, value);
	}

	@Test
	public void shortMessageIsStoredAsIs() throws Exception {
		MonitoringEvent e = build(new Date(1L), "CPU", true, "short", 1.5);
		assertEquals("short", e.getMessage());
	}

	@Test
	public void longMessageIsTruncatedToMaxLength() throws Exception {
		// "a" * 600 → kept to exactly 512 chars.
		char[] chars = new char[600];
		java.util.Arrays.fill(chars, 'a');
		String longMessage = new String(chars);

		MonitoringEvent e = build(new Date(1L), "CPU", true, longMessage, 1.5);
		assertEquals(MAX_MESSAGE_LENGTH, e.getMessage().length());
		assertEquals(longMessage.substring(0, MAX_MESSAGE_LENGTH),
				e.getMessage());
	}

	@Test
	public void messageOfExactlyMaxLengthIsUntouched() throws Exception {
		char[] chars = new char[MAX_MESSAGE_LENGTH];
		java.util.Arrays.fill(chars, 'b');
		String exactlyMax = new String(chars);

		MonitoringEvent e = build(new Date(1L), "CPU", true, exactlyMax, 1.5);
		assertEquals(MAX_MESSAGE_LENGTH, e.getMessage().length());
		assertEquals(exactlyMax, e.getMessage());
	}

	@Test
	public void nanValueIsCoercedToZero() throws Exception {
		// MySQL can't store Double.NaN, so the ctor normalizes it to 0.0.
		MonitoringEvent e = build(new Date(1L), "CPU", true, "msg",
				Double.NaN);
		assertEquals(0.0, e.getValue(), 0.0);
	}

	@Test
	public void regularValueIsStoredAsIs() throws Exception {
		MonitoringEvent e = build(new Date(1L), "CPU", true, "msg", 42.5);
		assertEquals(42.5, e.getValue(), 1e-9);
	}

	@Test
	public void gettersRoundTrip() throws Exception {
		Date t = new Date(1_700_000_000_000L);
		MonitoringEvent e = build(t, "CPU", true, "ok", 3.14);
		assertEquals(t, e.getTime());
		assertEquals("CPU", e.getType());
		assertTrue(e.isTriggered());
		assertEquals("ok", e.getMessage());
		assertEquals(3.14, e.getValue(), 1e-9);
	}

	@Test
	public void protectedNoArgConstructorLeavesAllNullAndNaN() {
		// The no-arg ctor is protected → same-package access OK.
		MonitoringEvent e = new MonitoringEvent();
		assertNull(e.getTime());
		assertNull(e.getType());
		assertFalse(e.isTriggered());
		assertNull(e.getMessage());
		assertTrue(Double.isNaN(e.getValue()));
	}

	@Test
	public void equalsMatchesOnAllFields() throws Exception {
		Date t = new Date(1L);
		MonitoringEvent a = build(t, "CPU", true, "msg", 1.0);
		MonitoringEvent b = build(t, "CPU", true, "msg", 1.0);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsFlipsOnEachField() throws Exception {
		Date t = new Date(1L);
		MonitoringEvent base = build(t, "CPU", true, "msg", 1.0);

		assertNotEquals(base, build(new Date(2L), "CPU", true, "msg", 1.0));
		assertNotEquals(base, build(t, "MEM", true, "msg", 1.0));
		assertNotEquals(base, build(t, "CPU", false, "msg", 1.0));
		assertNotEquals(base, build(t, "CPU", true, "other", 1.0));
		assertNotEquals(base, build(t, "CPU", true, "msg", 2.0));
	}

	@Test
	public void equalsHandlesNullAndForeignTypes() throws Exception {
		MonitoringEvent e = build(new Date(1L), "CPU", true, "msg", 1.0);
		assertFalse(e.equals(null));
		assertFalse(e.equals("not a MonitoringEvent"));
	}

	@Test
	public void toStringMentionsAllFields() throws Exception {
		String s = build(new Date(1L), "CPU", true, "msg", 1.0).toString();
		assertTrue(s.startsWith("MonitoringEvent ["));
		assertTrue(s.contains("type=CPU"));
		assertTrue(s.contains("triggered=true"));
		assertTrue(s.contains("message=msg"));
		assertTrue(s.contains("value=1.0"));
	}
}
