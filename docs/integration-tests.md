# Refreshing the WMATA integration-test fixtures

This doc is a working plan for refreshing the aging 2016 WMATA fixtures
that back the `transitclockIntegration` module. It's written for both
developers and future Claude sessions — if you're picking this up cold, start
at "Current state" and read through.

Related:
- `tools/wmata_capture/README.md` — the capture tool itself.
- GitHub issues `OneBusAway/thetransitclock#7` (detour test) and `#8`
  (prediction accuracy baseline).

## Progress so far

- **Step 1 capture — DONE.** Output at `tools/wmata_capture/output/fixtures-20260423/`.
  1,441 polls / 0 failures / 26,646 rows / 85 vehicles across D40, A40, D20.
- **Step 2 vehicle selection — DONE.** Picks:
  - `D40_5506` (736 rows, 100% BLOCK_ID) for the prediction accuracy test
  - `A40_3151` (575 rows, 2,430 m off-route excursion over 26 AVL points,
    ends on-route) for the detour test. Initial pick A40_3115 exited
    layover correctly but ended >10 min behind schedule at the end of
    AM-rush traffic, failing the test's adherence assertion; A40_3151
    ended at −7.5 min, within the 10-min threshold.
  - `D20_7198` (843 rows) kept as a spare for future tests
- **Step 3 promotion — DONE.** Subsetted GTFS + chosen AVL traces landed under
  `transitclockIntegration/src/test/resources/{gtfs,avl}/`. Old S2 and 3T
  fixtures removed. 5A fixtures left in place (their tests still pass).
- **Step 4 test edits — DONE.** `RecoverFromDetourTest` is un-ignored and
  points at `A40_3151`, passing. `PredictionAccuracyIntegrationTest` has
  its path constants updated but remains `@Ignore`d — the blocker turned
  out to be deeper than fixture rot (see Step 4b).
- **Step 4b pred baseline — BLOCKED (non-determinism).** Attempted to
  generate `pred/D40_5506.csv` by adding a one-shot dumper that calls
  `session.createCriteria(Prediction.class).list()` after `PlaybackModule
  .runTrace`. The dump succeeds, but replaying the same AVL trace in a
  *separate* JVM produces **different prediction counts on every run**
  (observed 2155, 2696, and 2118 across three runs; AD counts also
  varied by >2x). A frozen CSV baseline therefore cannot serve as a
  regression signal — the `newTotalError <= oldTotalError` and
  `oldTotalPreds <= newTotalPreds` assertions fail on run-to-run
  variance, not on predictor regression. Most likely source is unsorted
  collection iteration order in the predictor pipeline carrying over
  into which predictions get generated / persisted, but that's
  speculation — a real fix needs a deterministic replay.
- **Step 5 validate — DONE (for non-ignored tests).** `mvn -P
  include-integration-tests test` on the module passes: 3 tests green
  (detour + two 5A), 1 skipped (testPredictions). Matches the pre-
  refresh test count with one test now genuinely live instead of pinned
  to stale 2016 data.

## Re-enabling testPredictions

Two plausible paths for future work:

1. **Make the predictor deterministic.** Find the source(s) of
   non-determinism — likely `HashMap` iteration order, or thread
   scheduling in `AvlProcessor` / prediction generation — and replace
   with deterministic equivalents (`LinkedHashMap`, single-thread
   replay mode). Then a CSV baseline works as originally intended.
2. **Rewrite the assertions to be tolerance-based.** Instead of exact
   CSV comparison, assert on properties that should be stable even
   across non-deterministic runs — e.g. "mean absolute prediction error
   over all observed stops is below X seconds" or "90th-percentile
   error is below Y seconds." No baseline CSV needed. Loses regression
   detection against prior predictor versions but gains stability.

Option 2 is the smaller change. Option 1 is the more honest fix and
would also help other tests that replay traces.

Post-capture utilities now live in `tools/wmata_capture/`:
- `subset_gtfs.py` — slice the full feed to one route with referential closure
- `find_detour_candidates.py` — rank AVL traces by detour-likeness against GTFS shapes

---

## Current state of the integration module

Four tests under `transitclockIntegration/src/test/java/org/transitclock/integration_tests/`
consume WMATA fixtures. Two currently pass, two are `@Ignore`d:

| Test | Route / vehicle fixture | Status | Issue |
|---|---|---|---|
| `prediction/PredictionAccuracyIntegrationTest` | `S2_2113` (avl **+** pred baseline) | `@Ignore` | #8 |
| `RecoverFromDetourTest` | `3T_3757` | `@Ignore` | #7 |
| `GenerateEffectiveScheduleDifferenceTest` | `5A_8062` | passes | — |
| `EffectiveScheduleDifferenceDuringLayoverTest` | `5A_8062` | passes | — |

Fixture layout:

- `transitclockIntegration/src/test/resources/gtfs/{S2,3T,5A}/` — unpacked static GTFS per route
- `transitclockIntegration/src/test/resources/avl/{S2_2113,3T_3757,5A_8062}.csv` — AVL traces
- `transitclockIntegration/src/test/resources/pred/S2_2113.csv` — **predictor output baseline** (only the accuracy test uses this)

The tests all call `PlaybackModule.runTrace(GTFS, AVL)`, which boots a
real Core against an in-memory database, replays the AVL CSV, and lets
the matcher/prediction pipeline run end-to-end. No mocks.

## Why the fixtures can't be regenerated in place

**None of `S2`, `3T`, or `5A` exist in today's WMATA GTFS.** WMATA's
"Better Bus Network" redesign (effective June 2025) renamed every
route to a `<zone-letter><number>` scheme. Current short names are all
in `A*`, `C*`, `D*`, `F*`, `M*`, `P*` ranges — 127 routes total, none
matching the 2016 fixture names.

So the refresh can't just re-capture the same route IDs. We have to
pick *current* routes, capture fresh traces, and rename fixtures +
update the route-ID constants in the tests.

## Route selection

Picking routes that (a) exist today, (b) have enough trips/vehicles
during the capture window to produce dense traces, and (c) give us a
shot at exercising the pinned test scenarios.

**Block-id coverage is no longer a selection worry** — the old README
caveat about patchy `block_id` in `trips.txt` is obsolete post-redesign.
Spot check: 100% of trips in the current GTFS carry a `block_id`.

**Minimum-scope recommendation** (fix only what's broken — #7 and #8):

| Replaces | New route | Rationale |
|---|---|---|
| S2 (16th St DC trunk) | **D40** "7 St–Georgia Av" | High-frequency DC N-S trunk; 1,600+ trips/day; plenty of predictor signal |
| 3T (VA crosstown) | **A40** "Columbia Pike–National Landing" | Busy VA arterial; dense urban section maximizes odds of finding a detour event for the detour test |

If you want variety for future tests, D20 ("H Street") is a good third
pick at zero extra capture cost — one process can filter multiple
routes. But nothing in the currently-failing test suite needs it.

## Step 1 — Capture overnight

Prereqs: `WMATA_API_KEY` in `tools/wmata_capture/.env` (already present
as of this writing — the capture script auto-loads it).

```sh
cd /Users/aaron/repos/onebusaway/transitime/tools/wmata_capture

nohup uv run capture.py \
    --output-dir ./output/fixtures-$(date +%Y%m%d) \
    --duration-hours 12 \
    --poll-interval 30 \
    --routes D20,D40,A40 \
    > /dev/null 2>&1 &
```

Why these flags:

- `--duration-hours 12` — starting evening PT, 12h captures past DC AM
  rush (06:00–10:00 ET), which is the richest window per the capture
  tool's README.
- `--poll-interval 30` — WMATA's GTFS-RT updates roughly every 60s;
  the script de-dups overlapping observations within a run.
- `--routes D40,A40` — both in one process (cheaper than two, dedup is
  shared). Add `,D20` if you want an extra route for variety.

Monitor live:

```sh
tail -f tools/wmata_capture/output/fixtures-*/capture.log
```

## Step 2 — Inspect the capture and pick vehicles

In the morning, survey output:

```sh
cd tools/wmata_capture/output/fixtures-<date>

# Fattest traces per route (more rows = more predictor signal)
ls -S avl/ | head -20

# Sanity: row counts per vehicle
wc -l avl/*.csv | sort -n -r | head -20

# Confirm block_id assignments came through (should be mostly BLOCK, not TRIP_ID)
awk -F, 'NR>1 {print $4}' avl/D40_*.csv | sort | uniq -c
```

Pick one vehicle per route to promote. Criteria:

- **For the prediction accuracy test (replaces S2_2113):** pick the
  longest/densest D40 trace — more prediction horizons = stronger
  signal. Cover the AM rush window if possible.
- **For the detour test (replaces 3T_3757):** this is the hard one.
  The test asserts a vehicle exits layover state after going off-route
  and returning. Not every vehicle contains a detour event. Options,
  in order of preference:
    1. Manually scan A40 traces for vehicles whose lat/lon briefly
       departs and rejoins the route shape — those are detour
       candidates. `transitclockIntegration/src/test/resources/avl/3T_3757.csv`
       is the reference for what a "good" detour trace looks like.
    2. If no clean detour is found, this test may need to be re-scoped
       to a more generally observable scenario (e.g. "vehicle recovers
       schedule adherence after an AVL gap") rather than one-for-one
       replaced. Flag this as a finding in the PR; don't silently loosen
       the assertions.

## Step 3 — Promote fixtures

From the repo root, for each route (example below shows D40 replacing S2):

```sh
# 1. Swap GTFS
rm -rf transitclockIntegration/src/test/resources/gtfs/S2
cp -R tools/wmata_capture/output/fixtures-<date>/gtfs \
      transitclockIntegration/src/test/resources/gtfs/D40

# 2. Swap AVL CSV (pick the vehicle from Step 2)
rm transitclockIntegration/src/test/resources/avl/S2_2113.csv
cp tools/wmata_capture/output/fixtures-<date>/avl/D40_<vehicle>.csv \
   transitclockIntegration/src/test/resources/avl/D40_<vehicle>.csv
```

Repeat for A40 replacing 3T. The GTFS dir in step 1 is the *whole feed*
unpacked — `PlaybackModule.runTrace` only uses the route referenced by
the test, but it loads the full dir. Copying the full GTFS twice (once
per route directory) is fine and matches the current layout.

## Step 4 — Update test constants and regenerate the pred baseline

### Route/vehicle constants

Java files to edit (these are the exact occurrences as of this writing):

- `transitclockIntegration/src/test/java/org/transitclock/integration_tests/prediction/PredictionAccuracyIntegrationTest.java`
  - `GTFS = "src/test/resources/gtfs/S2"` → new path
  - `AVL = "src/test/resources/avl/S2_2113.csv"` → new path
  - `PREDICTIONS_CSV = "src/test/resources/pred/S2_2113.csv"` → new path
  - `@Ignore(...)` — remove
- `transitclockIntegration/src/test/java/org/transitclock/integration_tests/RecoverFromDetourTest.java`
  - `GTFS`, `AVL`, `VEHICLE` → new values
  - `@Ignore(...)` — remove

Do **not** touch the two `5A` tests unless you're intentionally
expanding scope — they currently pass and changing their fixtures means
re-validating their assertions.

### Regenerating `pred/*.csv` (prediction accuracy baseline)

The baseline is the *output* of the current predictor replayed against
the new AVL trace. It's not produced by the capture tool. Process:

1. With `@Ignore` removed on `PredictionAccuracyIntegrationTest` and
   the new fixtures in place, run the test once:
   ```sh
   mvn -pl transitclockIntegration test \
       -Dtest=PredictionAccuracyIntegrationTest \
       -P include-integration-tests
   ```
2. The test's `setUp()` fetches `List<Prediction>` from Hibernate
   (`PredictionAccuracyIntegrationTest.java:80`). Dump those rows to a
   CSV with the **exact same header** as the current `pred/S2_2113.csv`:
   ```
   id,affectedByWaitStop,avlTime,configRev,creationTime,gtfsStopSeq,isArrival,predictionTime,routeId,schedBasedPred,stopId,tripId,vehicleId
   ```
   Easiest approach: add a one-shot `@Before`/`@After` hook (or a
   scratch `@Test` that you delete afterwards) that writes the CSV
   before the assertions run. The existing test code already binds to
   the same `Prediction` entity — reuse its fields.
3. Save the CSV as `transitclockIntegration/src/test/resources/pred/D40_<vehicle>.csv`
   and point `PREDICTIONS_CSV` at it.
4. Re-run the test. It compares *new* predictions against the *baseline
   you just generated*, so on first pass it should be close to a
   no-op (new ≈ old). The assertions (`newTotalPreds >= oldTotalPreds`,
   `newTotalError <= oldTotalError`, `oldBetter/bothTotalPreds <= 0.5`)
   then mean "the predictor must not regress against this captured
   baseline in future." That's the regression-signal the fixture refresh
   is restoring.
5. **Leave `@Ignore` off.** Re-adding it defeats the whole refresh.

## Step 5 — Validate

```sh
# Run just the two previously-ignored tests
mvn -pl transitclockIntegration test -P include-integration-tests \
    -Dtest=PredictionAccuracyIntegrationTest,RecoverFromDetourTest

# Then run the whole integration suite to ensure 5A tests still pass
mvn -pl transitclockIntegration -am test -P include-integration-tests

# Then the full reactor with everything enabled
mvn install -P run-all-tests
```

## Pitfalls / things Claude should double-check

- **Don't commit the WMATA API key.** `.env` is gitignored; verify with
  `git status` before committing fixture changes.
- **Don't re-add `@Ignore`** after regenerating the pred baseline. The
  whole point of the refresh is to make those tests live signal again.
  If the test fails on first live run, investigate — don't silence it.
- **Don't break the passing 5A tests.** They use fixtures named `5A` on
  disk. If you're tempted to rename `5A` → something current for tidy
  consistency, stop — that means re-validating the 5A tests' assertions
  against new data, which is out of scope for the "fix broken tests"
  goal. Leave 5A alone unless the user explicitly expands scope.
- **Verify block-assignment type in the captured AVL.** Column 4
  (`assignmentType`) should be mostly `BLOCK`, with `TRIP_ID` fallback
  only for the minority of trips where WMATA didn't populate `block_id`.
  Current GTFS shows 100% `block_id` coverage, so `TRIP_ID` fallbacks
  should be rare. Heavy `TRIP_ID` usage means something changed and the
  promotion should pause.
- **The capture's timezone is `America/New_York`** regardless of the
  host clock. `BatchCsvAvlFeedModule` parses with the JVM default TZ,
  so tests must run with `-Duser.timezone=America/New_York` or
  equivalent — check how the existing suite sets this before assuming
  it'll "just work" on a Pacific-time dev box.
- **Don't resume into the same `--output-dir`.** The capture's dedup
  state isn't persisted across runs; resuming produces duplicates.
  Always start a new timestamped dir.
- **The detour test may need more than a fixture swap.** Real detour
  events are rare in any 12-hour capture. If you can't find one,
  document that in the PR and propose either (a) a longer/targeted
  capture, or (b) re-scoping the test — don't paper over it by
  weakening assertions.
