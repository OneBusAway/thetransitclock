package org.transitclock.pipelinetests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitclock.applications.Core;
import org.transitclock.config.ConfigFileReader;
import org.transitclock.configData.AgencyConfig;
import org.transitclock.db.structs.ActiveRevisions;
import org.transitclock.gtfs.DbConfig;
import org.transitclock.gtfs.GtfsData;
import org.transitclock.gtfs.TitleFormatter;
import org.transitclock.utils.SystemTime;
import org.transitclock.utils.Time;

/**
 * JUnit 4 ClassRule that boots a real {@link Core} against an in-memory HSQL
 * database loaded with a small GTFS dataset. Pipeline tests extend their
 * coverage over real matcher/generator behavior rather than stubs, while
 * staying isolated from the heavier {@code transitclockIntegration} module
 * (which runs full AVL-CSV replay traces).
 *
 * <p>Usage:
 * <pre>
 *   public class MyPipelineTest {
 *       &#64;ClassRule
 *       public static final CoreHarness CORE = CoreHarness.withWmata5A();
 *
 *       &#64;Test
 *       public void somePipelineBehavior() {
 *           CORE.setNow(...);
 *           // exercise AvlProcessor etc. here, then assert on CORE.dbConfig()
 *           // or PredictionDataCache.getInstance()
 *       }
 *   }
 * </pre>
 *
 * <p>Teardown relies on {@code reuseForks=false} in the Surefire configuration
 * — each test class gets a fresh JVM, so there is no need to hand-roll a Core
 * singleton reset. If that property ever changes, static state from a prior
 * run will leak into the next class and these tests will misbehave.
 *
 * <p>The GTFS dataset used is the {@code 5A} route from the WMATA fixture
 * checked in under the {@code transitclockIntegration} module. We read it via
 * a relative filesystem path rather than importing it via Maven, so this
 * module doesn't need a compile-time dep on {@code transitclockIntegration}
 * (which is itself excluded from the default reactor build).
 */
public class CoreHarness extends ExternalResource {

	private static final Logger logger = LoggerFactory.getLogger(CoreHarness.class);

	/** GTFS defaults copied from {@code PlaybackModule} — they mirror
	 *  {@code GtfsFileProcessor} defaults. */
	private static final double PATH_OFFSET_DISTANCE = 0.0;
	private static final double MAX_STOP_TO_PATH_DISTANCE = 60.0;
	private static final double MAX_DISTANCE_FOR_ELIMINATING_VERTICES = 3.0;
	private static final int DEFAULT_WAIT_TIME_AT_STOP_MSEC = 10 * Time.MS_PER_SEC;
	private static final double MAX_SPEED_KPH = 97.0;
	private static final double MAX_TRAVEL_TIME_SEGMENT_LENGTH = 200.0;
	private static final double MAX_DISTANCE_BETWEEN_STOPS = 1000.0;
	private static final boolean DISABLE_SPECIAL_LOOP_BACK_TO_BEGINNING = false;

	/** Relative path from this module's basedir to the WMATA 5A GTFS files. */
	private static final String WMATA_5A_GTFS_DIR =
			"../transitclockIntegration/src/test/resources/gtfs/5A";

	/** Agency id used by the checked-in transitclockConfigHsql.xml. */
	private static final String AGENCY_ID = "1";

	/** Path (relative to module basedir) to the Core config file. */
	private static final String CORE_CONFIG_FILE =
			"src/test/resources/transitclockConfigHsql.xml";

	/** JVM system properties the harness sets at boot and restores at teardown. */
	private static final String[] MANAGED_SYSTEM_PROPERTIES = {
			"transitclock.configFiles",
			"transitclock.core.agencyId",
	};

	private final String gtfsDirectory;
	private final Consumer<Core> afterBootHook;

	/**
	 * Values these properties held before the harness modified them, so
	 * {@link #after()} can restore them. Kept separate per-property because
	 * a null value means the property was not set at all (and must be cleared
	 * on teardown, not set back to the string "null").
	 */
	private final java.util.Map<String, String> priorSystemProperties = new java.util.HashMap<>();

	private CoreHarness(String gtfsDirectory, Consumer<Core> afterBootHook) {
		this.gtfsDirectory = gtfsDirectory;
		this.afterBootHook = afterBootHook;
	}

	/**
	 * Boots against the WMATA 5A dataset — a single-route, single-day GTFS
	 * snapshot (~460KB). Small enough to import in a second or two.
	 */
	public static CoreHarness withWmata5A() {
		return new CoreHarness(WMATA_5A_GTFS_DIR, c -> {});
	}

	@Override
	protected void before() throws Throwable {
		logger.info("Booting Core for pipeline tests (gtfs={})", gtfsDirectory);
		long start = System.currentTimeMillis();

		// If a prior test class in the same JVM left Core booted, it will
		// leak its DbConfig into ours. The pipeline-tests module runs with
		// surefire's reuseForks=false so each class gets a fresh JVM, but if
		// that setting ever drifts this check turns "mysterious test failures"
		// into an explicit refusal.
		if (Core.isCoreApplication()) {
			throw new IllegalStateException(
					"Core.getInstance() is already set before CoreHarness.before(). "
							+ "This suggests another test class ran in the same JVM. "
							+ "Ensure surefire's reuseForks=false for this module.");
		}

		// Capture prior values so after() can restore whatever was there
		// (usually nothing) when the harness is torn down.
		for (String key : MANAGED_SYSTEM_PROPERTIES) {
			priorSystemProperties.put(key, System.getProperty(key));
		}
		// These properties are what Core looks at during createCore().
		System.setProperty("transitclock.configFiles", CORE_CONFIG_FILE);
		System.setProperty("transitclock.core.agencyId", AGENCY_ID);

		ConfigFileReader.processConfig();

		// The checked-in WMATA datasets have calendar end_dates from 2016–2018,
		// which fails GtfsData.isCalendarActiveInTheFuture (that check uses
		// real System.currentTimeMillis, not Core.getSystemTime, so we can't
		// work around it at runtime). Stage the GTFS to a temp dir with
		// rewritten calendar end_dates so the import-time filter passes.
		Path staged = stageGtfsWithFutureCalendarEndDates(gtfsDirectory);

		setupGtfs(staged.toString());

		// Diagnostic: confirm ActiveRevisions reflects the import before
		// Core tries to read DbConfig from it.
		ActiveRevisions postImportRevs = ActiveRevisions.get(AGENCY_ID);
		logger.info("ActiveRevisions after GTFS import: {}", postImportRevs);

		// Trigger lazy singleton construction; this is the point at which
		// DbConfig is read back out of the DB we just populated.
		Core core = Core.getInstance();
		if (core == null) {
			throw new IllegalStateException(
					"Core.getInstance() returned null after boot; check config file and agency id.");
		}

		afterBootHook.accept(core);

		logger.info("Core boot complete in {} ms. Loaded routes={}, blocks={}",
				System.currentTimeMillis() - start,
				core.getDbConfig().getRoutes().size(),
				core.getDbConfig().getBlocks().size());
	}

	@Override
	protected void after() {
		// Restore the JVM system properties we changed. Under reuseForks=false
		// this is belt-and-suspenders (the JVM dies next), but a misconfigured
		// fork setting would otherwise leak configFiles/agencyId into a
		// sibling test class and cause surprising failures.
		for (String key : MANAGED_SYSTEM_PROPERTIES) {
			String prior = priorSystemProperties.get(key);
			if (prior == null) {
				System.clearProperty(key);
			} else {
				System.setProperty(key, prior);
			}
		}
		priorSystemProperties.clear();
	}

	/**
	 * Copies the source GTFS directory into a temp directory, rewriting
	 * {@code calendar.txt} so that every row's end_date lands far in the
	 * future. This is the minimum edit needed to satisfy
	 * {@code GtfsData.isCalendarActiveInTheFuture}, which otherwise filters
	 * out all trips for the stale WMATA fixture.
	 *
	 * <p>We intentionally do NOT rewrite {@code calendar_dates.txt}. Runtime
	 * service-id lookup uses {@code Core.getSystemTime()}, which tests
	 * control via {@link #setNow(long)}: tests can set Core's clock to a
	 * historical date that the original {@code calendar_dates.txt} has
	 * service for, so no synthetic runtime dates are needed.
	 */
	private static Path stageGtfsWithFutureCalendarEndDates(String sourceDir)
			throws IOException {
		Path source = Path.of(sourceDir);
		if (!Files.isDirectory(source)) {
			throw new IOException("GTFS source directory not found: " + source.toAbsolutePath());
		}

		Path staged = Files.createTempDirectory("pipelinetests-gtfs-");
		staged.toFile().deleteOnExit();

		try (Stream<Path> files = Files.list(source)) {
			for (Path file : files.collect(Collectors.toList())) {
				if (!Files.isRegularFile(file)) {
					// Silent skipping would quietly drop data. WMATA 5A is
					// flat, but an agency's GTFS dir with a subdirectory or
					// symlink would otherwise be imported with missing files
					// and surface the problem as confusing "X not found"
					// errors far from here.
					throw new IOException("Unexpected non-regular entry in GTFS source dir: "
							+ file.toAbsolutePath()
							+ " (CoreHarness expects a flat directory of .txt files)");
				}
				Path dest = staged.resolve(file.getFileName());
				if (file.getFileName().toString().equals("calendar.txt")) {
					rewriteCalendarWithFutureEndDates(file, dest);
				} else {
					Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
		return staged;
	}

	/**
	 * Reads {@code calendar.txt}, replaces end_date (column 10, index 9
	 * 0-based) with a far-future date, and writes to {@code dest}.
	 *
	 * <p>GTFS does not require a fixed column order — feeds identify columns
	 * by header name — but every WMATA test fixture checked in under
	 * {@code transitclockIntegration} uses the canonical column order
	 * (service_id, monday..sunday, start_date, end_date). This rewriter
	 * relies on that canonical order and would need a header-aware parser
	 * if a fixture with a different ordering is ever introduced.
	 */
	static void rewriteCalendarWithFutureEndDates(Path source, Path dest)
			throws IOException {
		// 20500101 chosen to comfortably outlive any reasonable test run.
		// If Y2K50 becomes a concern, we'll have bigger problems.
		final String futureEndDate = "20500101";

		List<String> lines = Files.readAllLines(source);
		List<String> out = new ArrayList<>(lines.size());
		if (!lines.isEmpty()) {
			out.add(lines.get(0)); // header passes through unchanged
		}
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.isEmpty()) {
				out.add(line);
				continue;
			}
			String[] parts = line.split(",", -1);
			if (parts.length < 10) {
				// A malformed row here would quietly defeat the rewrite — the
				// row's stale end_date would ship unchanged and Gtfs import
				// would filter its trips out. Fail loudly so the contract
				// maintains itself.
				throw new IOException("Malformed calendar.txt at line " + (i + 1)
						+ ": expected 10 columns, got " + parts.length
						+ ". Line content: " + line);
			}
			parts[9] = futureEndDate;
			out.add(String.join(",", parts));
		}
		Files.write(dest, out);
	}

	/**
	 * Loads the GTFS directory into the HSQL database. Mirrors
	 * {@code PlaybackModule#setupGtfs}.
	 */
	private static void setupGtfs(String gtfsDirectoryName) {
		TitleFormatter titleFormatter = new TitleFormatter(null, true);
		boolean shouldStoreNewRevs = true;
		boolean shouldDeleteRevs = false;
		GtfsData gtfsData = new GtfsData(
				/*configRev*/ 1,
				/*notes*/ null,
				/*zipFileLastModifiedTime*/ null,
				shouldStoreNewRevs,
				shouldDeleteRevs,
				AgencyConfig.getAgencyId(),
				gtfsDirectoryName,
				/*supplementDir*/ null,
				PATH_OFFSET_DISTANCE,
				MAX_STOP_TO_PATH_DISTANCE,
				MAX_DISTANCE_FOR_ELIMINATING_VERTICES,
				DEFAULT_WAIT_TIME_AT_STOP_MSEC,
				MAX_SPEED_KPH,
				MAX_TRAVEL_TIME_SEGMENT_LENGTH,
				/*trimPathBeforeFirstStopOfTrip*/ false,
				titleFormatter,
				MAX_DISTANCE_BETWEEN_STOPS,
				DISABLE_SPECIAL_LOOP_BACK_TO_BEGINNING);
		gtfsData.processData();
	}

	// ---------- Accessors for tests ----------

	/** The live Core singleton booted by this harness. */
	public Core core() {
		return Core.getInstance();
	}

	/** Shortcut to {@code core().getDbConfig()}. */
	public DbConfig dbConfig() {
		return core().getDbConfig();
	}

	/** The clock Core is using — {@link org.transitclock.utils.SystemCurrentTime}
	 *  by default. Tests that need deterministic time should call
	 *  {@link #setNow(long)} which swaps it for a {@code SettableSystemTime}. */
	public SystemTime clock() {
		// Core exposes a getSystemTime() that returns long, not the SystemTime
		// instance. The instance is only reachable via the setter — we rely on
		// setNow() to put us into SettableSystemTime mode for time-controlled
		// tests. Return a lambda view of the current epoch reading.
		return () -> core().getSystemTime();
	}

	/**
	 * Swaps Core's clock into {@link org.transitclock.utils.SettableSystemTime}
	 * mode at the given epoch-millis. Calling this once fixes Core's notion of
	 * "now" until another {@code setNow} / {@code advanceClock} call.
	 */
	public void setNow(long epochMillis) {
		core().setSystemTime(epochMillis);
	}

	/** Convenience overload taking a {@link Date}. */
	public void setNow(Date now) {
		setNow(now.getTime());
	}
}
