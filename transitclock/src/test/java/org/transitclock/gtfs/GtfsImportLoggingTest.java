package org.transitclock.gtfs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Pins the GTFS-import log sites that previously dominated import time when
 * stdout is piped through Docker — thousands of stdout lines from
 * {@link StopPathProcessor} and one INFO line per block from {@link DbWriter}.
 * Source-level checks because triggering the real code paths requires a full
 * TripPattern/Stop/GtfsShape graph or a Hibernate session.
 */
public class GtfsImportLoggingTest {

    private static final Path STOP_PATH_PROCESSOR_SOURCE = moduleSource(
            "src/main/java/org/transitclock/gtfs/StopPathProcessor.java");
    private static final Path DB_WRITER_SOURCE = moduleSource(
            "src/main/java/org/transitclock/gtfs/DbWriter.java");

    private static final Pattern STDOUT_NOISE = Pattern.compile(
            "System\\.(out|err)\\.(print|println|printf)|\\.printStackTrace\\(");

    @Test
    public void stopPathProcessor_doesNotPrintToStdout() throws IOException {
        assertThat(activeLinesMatching(STOP_PATH_PROCESSOR_SOURCE, STDOUT_NOISE))
                .isEmpty();
    }

    @Test
    public void dbWriter_perBlockSavingMessageIsNotLoggedAtInfo() throws IOException {
        assertThat(readSource(DB_WRITER_SOURCE))
                .doesNotContain("logger.info(\"Saving block #");
    }

    @Test
    public void dbWriter_flushingMessagesAreNotLoggedAtInfo() throws IOException {
        String source = readSource(DB_WRITER_SOURCE);
        assertThat(source).doesNotContain("logger.info(\"flushing with");
        assertThat(source).doesNotContain("logger.info(\"flushed with");
    }

    private static String readSource(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static List<String> activeLinesMatching(Path path, Pattern pattern)
            throws IOException {
        List<String> hits = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            int commentIdx = line.indexOf("//");
            String code = commentIdx < 0 ? line : line.substring(0, commentIdx);
            if (pattern.matcher(code).find()) {
                hits.add(line);
            }
        }
        return hits;
    }

    /** Resolves a module-relative path whether cwd is the module dir (Maven) or the repo root (some IDE configs). */
    private static Path moduleSource(String relativePath) {
        Path[] candidates = { Paths.get(relativePath), Paths.get("transitclock", relativePath) };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not locate " + relativePath
                + " from cwd " + Paths.get("").toAbsolutePath());
    }
}
