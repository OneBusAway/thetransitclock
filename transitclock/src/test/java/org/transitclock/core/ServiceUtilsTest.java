package org.transitclock.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.transitclock.db.structs.Agency;
import org.transitclock.db.structs.Calendar;
import org.transitclock.db.structs.CalendarDate;
import org.transitclock.gtfs.DbConfig;

public class ServiceUtilsTest {

	// Noon UTC on known weekdays so TZ offsets don't flip the day.
	private static final long MON_2024_06_03_NOON_UTC = 1_717_416_000_000L;
	private static final long TUE_2024_06_04_NOON_UTC = 1_717_502_400_000L;

	private DbConfig dbConfig;
	private Agency agency;

	@Before
	public void setUp() {
		dbConfig = mock(DbConfig.class);
		agency = mock(Agency.class);
		when(agency.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
		when(dbConfig.getFirstAgency()).thenReturn(agency);
		when(dbConfig.getCalendars()).thenReturn(Collections.emptyList());
		when(dbConfig.getCalendarDates(any(Date.class))).thenReturn(null);
	}

	@Test
	public void getDayOfWeek_returnsAgencyLocalDayOfWeek() {
		ServiceUtils utils = new ServiceUtils(dbConfig);

		assertThat(utils.getDayOfWeek(new Date(MON_2024_06_03_NOON_UTC)))
				.isEqualTo(java.util.Calendar.MONDAY);
		assertThat(utils.getDayOfWeek(new Date(TUE_2024_06_04_NOON_UTC)))
				.isEqualTo(java.util.Calendar.TUESDAY);
	}

	@Test
	public void constructor_withNoAgencyStillWorks() {
		when(dbConfig.getFirstAgency()).thenReturn(null);

		ServiceUtils utils = new ServiceUtils(dbConfig);

		// Fall-through is a default-timezone GregorianCalendar; still functional.
		assertThat(utils.getDayOfWeek(new Date(MON_2024_06_03_NOON_UTC))).isPositive();
	}

	@Test
	public void getServiceIdsForDayNoCache_returnsIdsMatchingDayOfWeek() {
		Calendar weekday = calendarFor("weekday", true, true, true, true, true, false, false,
				MON_2024_06_03_NOON_UTC - 86_400_000L,
				MON_2024_06_03_NOON_UTC + 86_400_000L);
		Calendar weekend = calendarFor("weekend", false, false, false, false, false, true, true,
				MON_2024_06_03_NOON_UTC - 86_400_000L,
				MON_2024_06_03_NOON_UTC + 86_400_000L);
		when(dbConfig.getCalendars()).thenReturn(Arrays.asList(weekday, weekend));

		ServiceUtils utils = new ServiceUtils(dbConfig);
		List<String> ids = utils.getServiceIdsForDayNoCache(new Date(MON_2024_06_03_NOON_UTC));

		assertThat(ids).containsExactly("weekday");
	}

	@Test
	public void getServiceIdsForDayNoCache_fallsBackToMostRecentWhenAllExpired() {
		// Both calendars are expired; the more-recent one should still be used.
		long target = MON_2024_06_03_NOON_UTC;
		Calendar expiredOld = calendarFor("old", true, true, true, true, true, true, true,
				target - 30L * 86_400_000L, target - 20L * 86_400_000L);
		Calendar expiredRecent = calendarFor("recent", true, true, true, true, true, true, true,
				target - 10L * 86_400_000L, target - 1L * 86_400_000L);
		when(dbConfig.getCalendars()).thenReturn(Arrays.asList(expiredOld, expiredRecent));

		ServiceUtils utils = new ServiceUtils(dbConfig);
		List<String> ids = utils.getServiceIdsForDayNoCache(new Date(target));

		assertThat(ids).containsExactly("recent");
	}

	@Test
	public void getServiceIdsForDayNoCache_calendarDateAddsService() {
		Calendar weekday = calendarFor("weekday", true, true, true, true, true, false, false,
				MON_2024_06_03_NOON_UTC - 86_400_000L,
				MON_2024_06_03_NOON_UTC + 86_400_000L);
		when(dbConfig.getCalendars()).thenReturn(Collections.singletonList(weekday));

		CalendarDate addHoliday = mock(CalendarDate.class);
		when(addHoliday.addService()).thenReturn(true);
		when(addHoliday.getServiceId()).thenReturn("holiday-extra");
		when(dbConfig.getCalendarDates(any(Date.class)))
				.thenReturn(Collections.singletonList(addHoliday));

		ServiceUtils utils = new ServiceUtils(dbConfig);
		List<String> ids = utils.getServiceIdsForDayNoCache(new Date(MON_2024_06_03_NOON_UTC));

		assertThat(ids).containsExactlyInAnyOrder("weekday", "holiday-extra");
	}

	@Test
	public void getServiceIdsForDayNoCache_calendarDateRemovesService() {
		Calendar weekday = calendarFor("weekday", true, true, true, true, true, false, false,
				MON_2024_06_03_NOON_UTC - 86_400_000L,
				MON_2024_06_03_NOON_UTC + 86_400_000L);
		when(dbConfig.getCalendars()).thenReturn(Collections.singletonList(weekday));

		CalendarDate removeHoliday = mock(CalendarDate.class);
		when(removeHoliday.addService()).thenReturn(false);
		when(removeHoliday.getServiceId()).thenReturn("weekday");
		when(dbConfig.getCalendarDates(any(Date.class)))
				.thenReturn(Collections.singletonList(removeHoliday));

		ServiceUtils utils = new ServiceUtils(dbConfig);
		List<String> ids = utils.getServiceIdsForDayNoCache(new Date(MON_2024_06_03_NOON_UTC));

		assertThat(ids).isEmpty();
	}

	@Test
	public void getServiceIdsForDay_cachesBySameServiceDate() {
		Calendar weekday = calendarFor("weekday", true, true, true, true, true, false, false,
				MON_2024_06_03_NOON_UTC - 86_400_000L,
				MON_2024_06_03_NOON_UTC + 86_400_000L);
		when(dbConfig.getCalendars()).thenReturn(Collections.singletonList(weekday));

		ServiceUtils utils = new ServiceUtils(dbConfig);
		List<String> first = utils.getServiceIdsForDay(new Date(MON_2024_06_03_NOON_UTC));
		List<String> second = utils.getServiceIdsForDay(
				new Date(MON_2024_06_03_NOON_UTC + 3_600_000L)); // same day, later hour

		assertThat(first).containsExactly("weekday");
		assertThat(second).isSameAs(first); // cache returns the exact same list instance
	}

	@Test
	public void getServiceIdsForDay_longOverloadDelegates() {
		Calendar weekday = calendarFor("weekday", true, true, true, true, true, false, false,
				MON_2024_06_03_NOON_UTC - 86_400_000L,
				MON_2024_06_03_NOON_UTC + 86_400_000L);
		when(dbConfig.getCalendars()).thenReturn(Collections.singletonList(weekday));

		ServiceUtils utils = new ServiceUtils(dbConfig);

		assertThat(utils.getServiceIdsForDay(MON_2024_06_03_NOON_UTC))
				.containsExactly("weekday");
	}

	private static Calendar calendarFor(String serviceId,
			boolean mon, boolean tue, boolean wed, boolean thu, boolean fri,
			boolean sat, boolean sun,
			long startEpochMs, long endEpochMs) {
		Calendar c = mock(Calendar.class);
		when(c.getServiceId()).thenReturn(serviceId);
		when(c.getMonday()).thenReturn(mon);
		when(c.getTuesday()).thenReturn(tue);
		when(c.getWednesday()).thenReturn(wed);
		when(c.getThursday()).thenReturn(thu);
		when(c.getFriday()).thenReturn(fri);
		when(c.getSaturday()).thenReturn(sat);
		when(c.getSunday()).thenReturn(sun);
		when(c.getStartDate()).thenReturn(new Date(startEpochMs));
		when(c.getEndDate()).thenReturn(new Date(endEpochMs));
		return c;
	}
}
