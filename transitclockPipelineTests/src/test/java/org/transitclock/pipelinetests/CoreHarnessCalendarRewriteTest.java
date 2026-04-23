package org.transitclock.pipelinetests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link CoreHarness#rewriteCalendarWithFutureEndDates}, which
 * runs at GTFS staging time and is the single point that fixes stale calendar
 * end_dates. Silent fallbacks here would quietly defeat the whole harness, so
 * the method must fail loudly on any shape it can't handle.
 */
public class CoreHarnessCalendarRewriteTest {

	@Rule
	public final TemporaryFolder tmp = new TemporaryFolder();

	private Path writeCalendar(String... lines) throws IOException {
		Path source = tmp.newFile("calendar.txt").toPath();
		Files.write(source, List.of(lines));
		return source;
	}

	@Test
	public void canonicalFormatRewritesEndDateOnEveryDataRow() throws IOException {
		Path source = writeCalendar(
				"service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date",
				"13,0,0,0,0,0,0,0,20160327,20180618",
				"14,1,1,1,1,1,0,0,20160327,20180618");
		Path dest = tmp.newFile("out.txt").toPath();

		CoreHarness.rewriteCalendarWithFutureEndDates(source, dest);

		List<String> out = Files.readAllLines(dest);
		assertThat(out).hasSize(3);
		// Header unchanged.
		assertThat(out.get(0)).isEqualTo(
				"service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date");
		// Data rows get their end_date (column 10) rewritten but keep everything else.
		assertThat(out.get(1)).isEqualTo("13,0,0,0,0,0,0,0,20160327,20500101");
		assertThat(out.get(2)).isEqualTo("14,1,1,1,1,1,0,0,20160327,20500101");
	}

	@Test
	public void malformedRowWithTooFewColumnsCausesLoudFailure() throws IOException {
		// A row with 9 columns instead of 10 — maybe a truncated copy-paste.
		// The old code silently passed this through, leaving the stale end_date
		// in place and defeating the whole reason for the rewrite. New contract:
		// any row we can't rewrite must blow up the test, not degrade quietly.
		Path source = writeCalendar(
				"service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date",
				"13,0,0,0,0,0,0,0,20160327"); // missing end_date column
		Path dest = tmp.newFile("out.txt").toPath();

		assertThatThrownBy(() ->
				CoreHarness.rewriteCalendarWithFutureEndDates(source, dest))
				.isInstanceOf(IOException.class)
				.hasMessageContaining("calendar.txt")
				.hasMessageContaining("line 2");
	}

	@Test
	public void emptyDataRowsArePreservedVerbatim() throws IOException {
		// Some GTFS producers emit trailing blank lines. The rewriter should
		// tolerate those (they're not malformed, just empty) and pass them through.
		Path source = writeCalendar(
				"service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date",
				"13,0,0,0,0,0,0,0,20160327,20180618",
				"");
		Path dest = tmp.newFile("out.txt").toPath();

		CoreHarness.rewriteCalendarWithFutureEndDates(source, dest);

		List<String> out = Files.readAllLines(dest);
		// Files.readAllLines strips the trailing empty line; the important
		// thing is that we didn't throw on it.
		assertThat(out).contains("13,0,0,0,0,0,0,0,20160327,20500101");
	}
}
