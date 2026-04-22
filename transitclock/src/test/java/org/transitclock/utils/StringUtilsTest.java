package org.transitclock.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class StringUtilsTest {

	@Test
	public void paddedNamePadsFirstNumericRun() {
		// paddedName uses 8 digits. "Y2" has 1 digit, so 7 zeros are prepended.
		assertEquals("Y00000002", StringUtils.paddedName("Y2"));
		assertEquals("Y00000101", StringUtils.paddedName("Y101"));
	}

	@Test
	public void paddedNameLeavesAlphaOnlyNamesUntouched() {
		assertEquals("downtown", StringUtils.paddedName("downtown"));
	}

	@Test
	public void paddedNamePreservesTrailingSuffix() {
		// "10A" → padded to 8 digits for the "10" run, followed by "A".
		assertEquals("0000001" + "0A", StringUtils.paddedName("10A"));
	}

	@Test
	public void sortIdsNumericallyOrdersByNumericValue() {
		List<String> ids = new ArrayList<>(
				Arrays.asList("Y101", "Y2", "Y10", "Y1"));
		StringUtils.sortIdsNumerically(ids);
		assertEquals(Arrays.asList("Y1", "Y2", "Y10", "Y101"), ids);
	}

	@Test
	public void sortIdsUsesPlainStringOrdering() {
		List<String> ids = new ArrayList<>(
				Arrays.asList("Y2", "Y101", "Y10", "Y1"));
		StringUtils.sortIds(ids);
		// Lexicographic: "Y1" < "Y10" < "Y101" < "Y2".
		assertEquals(Arrays.asList("Y1", "Y10", "Y101", "Y2"), ids);
	}

	@Test
	public void padWithBlanksNullInputReturnsNull() {
		assertNull(StringUtils.padWithBlanks(null, 5));
	}

	@Test
	public void padWithBlanksPrependsSpacesToReachLength() {
		assertEquals("  foo", StringUtils.padWithBlanks("foo", 5));
	}

	@Test
	public void padWithBlanksReturnsOriginalWhenAlreadyLongEnough() {
		assertEquals("foobar", StringUtils.padWithBlanks("foobar", 3));
		assertEquals("foo", StringUtils.padWithBlanks("foo", 3));
	}

	@Test
	public void nanDigitFormattersReturnNaNLiteral() {
		assertEquals("NaN", StringUtils.oneDigitFormat(Double.NaN));
		assertEquals("NaN", StringUtils.twoDigitFormat(Double.NaN));
		assertEquals("NaN", StringUtils.threeDigitFormat(Double.NaN));
		assertEquals("NaN", StringUtils.sixDigitFormat(Double.NaN));
	}

	@Test
	public void distanceFormatSwitchesToKilometersAboveThreshold() {
		// 3001 m > 3000 threshold → rendered in km.
		assertTrue(StringUtils.distanceFormat(3001).endsWith("km"));
		assertTrue(StringUtils.distanceFormat(500).endsWith("m"));
	}

	@Test
	public void memoryFormatScalesUnits() {
		assertTrue(StringUtils.memoryFormat(512L).endsWith(" bytes"));
		assertTrue(StringUtils.memoryFormat(50_000L).endsWith("KB"));
		assertTrue(StringUtils.memoryFormat(50_000_000L).endsWith("MB"));
		assertTrue(StringUtils.memoryFormat(50_000_000_000L).endsWith("GB"));
	}

}
