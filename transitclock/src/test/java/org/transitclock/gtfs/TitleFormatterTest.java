package org.transitclock.gtfs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TitleFormatterTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void processTitle_nullStaysNull() {
		TitleFormatter formatter = new TitleFormatter(null, false);
		assertThat(formatter.processTitle(null)).isNull();
	}

	@Test
	public void processTitle_withoutRegexFileReturnsOriginalUnchanged() {
		// capitalize config defaults to false and no regexes are loaded, so
		// the output is identical to the input.
		TitleFormatter formatter = new TitleFormatter(null, false);

		assertThat(formatter.processTitle("MAIN ST")).isEqualTo("MAIN ST");
		assertThat(formatter.processTitle("")).isEqualTo("");
	}

	@Test
	public void processTitle_appliesConfiguredRegexReplacements() throws IOException {
		File regexFile = writeRegexFile(
				"Bart=>BART",
				"Us=>US");

		TitleFormatter formatter = new TitleFormatter(regexFile.getAbsolutePath(), false);

		assertThat(formatter.processTitle("Bart Station")).isEqualTo("BART Station");
		assertThat(formatter.processTitle("Us Bank")).isEqualTo("US Bank");
	}

	@Test
	public void processTitle_enforcesSpacingAroundAmpersand() throws IOException {
		// These are the patterns in the class's Javadoc for fixing &-spacing.
		File regexFile = writeRegexFile(
				"&(?! )=>& ",
				"(?<! )&=> &");

		TitleFormatter formatter = new TitleFormatter(regexFile.getAbsolutePath(), false);

		assertThat(formatter.processTitle("Main&Elm")).isEqualTo("Main & Elm");
		assertThat(formatter.processTitle("Main & Elm")).isEqualTo("Main & Elm");
	}

	@Test
	public void isReplaceTitle_matchesConfiguredReplacement() throws IOException {
		File regexFile = writeRegexFile("Bart=>BART");

		TitleFormatter formatter = new TitleFormatter(regexFile.getAbsolutePath(), false);

		assertThat(formatter.isReplaceTitle("BART")).isTrue();
		assertThat(formatter.isReplaceTitle("Bart")).isFalse();
	}

	@Test
	public void regexFile_ignoresCommentsAndBlankLines() throws IOException {
		File regexFile = writeRegexFile(
				"-- dash comment",
				"// slash comment",
				"",
				"   ",
				"Bart=>BART");

		TitleFormatter formatter = new TitleFormatter(regexFile.getAbsolutePath(), false);

		assertThat(formatter.processTitle("Bart Station")).isEqualTo("BART Station");
	}

	@Test
	public void regexFile_skipsLinesWithoutDelimiter() throws IOException {
		// The "no-delimiter" line is logged as an error but should not throw,
		// and the following valid line should still be applied.
		File regexFile = writeRegexFile(
				"no delimiter on this line",
				"Bart=>BART");

		TitleFormatter formatter = new TitleFormatter(regexFile.getAbsolutePath(), false);

		assertThat(formatter.processTitle("Bart")).isEqualTo("BART");
	}

	@Test
	public void missingRegexFile_constructorDoesNotThrow() {
		// IOException on open is caught and logged; formatter should still work.
		TitleFormatter formatter = new TitleFormatter(
				"/does/not/exist/regex-does-not-exist.txt", false);

		assertThat(formatter.processTitle("unchanged")).isEqualTo("unchanged");
	}

	@Test
	public void logRegexesThatDidNotMakeDifference_withFlagDisabledIsNoOp() throws IOException {
		File regexFile = writeRegexFile("Bart=>BART");
		TitleFormatter formatter = new TitleFormatter(regexFile.getAbsolutePath(), false);

		// With logging disabled, the method just logs an error and returns.
		formatter.logRegexesThatDidNotMakeDifference();
	}

	@Test
	public void logRegexesThatDidNotMakeDifference_withFlagEnabledCompletes() throws IOException {
		File regexFile = writeRegexFile(
				"Bart=>BART",
				"Neverseen=>NOPE");
		TitleFormatter formatter = new TitleFormatter(regexFile.getAbsolutePath(), true);

		formatter.processTitle("Bart Station"); // first regex used, second isn't
		formatter.logRegexesThatDidNotMakeDifference();
	}

	private File writeRegexFile(String... lines) throws IOException {
		File file = tempFolder.newFile("regex.txt");
		Files.write(file.toPath(), String.join("\n", lines).getBytes());
		return file;
	}
}
