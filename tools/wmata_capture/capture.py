#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.10"
# dependencies = [
#   "requests",
#   "gtfs-realtime-bindings",
# ]
# ///
"""
WMATA GTFS + GTFS-RT capture for transitclock integration-test fixtures.

Downloads WMATA's static GTFS once, then polls the GTFS-RT VehiclePositions
feed at a configurable interval and writes per-vehicle CSV files in the
exact format transitclock's BatchCsvAvlFeedModule expects:

    vehicleId,time,assignmentId,assignmentType,heading,latitude,longitude
    1234,"04-23-2026 08:23:52",NS-26,BLOCK_ID,180,38.95778,-77.03645

Run:

    WMATA_API_KEY=xxx uv run capture.py \\
        --output-dir ./output/capture-2026-04-23 \\
        --duration-hours 4 \\
        --poll-interval 30 \\
        [--routes S2,3T,5A] \\
        [--vehicles 2113,8062]

Outputs:

    <output-dir>/gtfs/          unpacked static GTFS (trips.txt, stops.txt, ...)
    <output-dir>/avl/<route>_<vehicle>.csv    one CSV per captured vehicle
    <output-dir>/capture.log    append-only run log

The API key is read from the environment only; never accept it as a CLI
flag (it would end up in shell history, ps output, or crash dumps). See
README.md for the full operator playbook.
"""

from __future__ import annotations

import argparse
import csv
import io
import logging
import os
import re
import signal
import sys
import time
import zipfile
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional
from zoneinfo import ZoneInfo

import requests
from google.transit import gtfs_realtime_pb2

WMATA_GTFS_STATIC_URL = "https://api.wmata.com/gtfs/bus-gtfs-static.zip"
WMATA_GTFS_RT_VP_URL = "https://api.wmata.com/gtfs/bus-gtfsrt-vehiclepositions.pb"

EASTERN = ZoneInfo("America/New_York")

CSV_HEADER = [
    "vehicleId",
    "time",
    "assignmentId",
    "assignmentType",
    "heading",
    "latitude",
    "longitude",
]

log = logging.getLogger("wmata_capture")


@dataclass
class Stats:
    polls_ok: int = 0
    polls_failed: int = 0
    rows_written: int = 0
    rows_skipped_no_trip: int = 0
    rows_deduped: int = 0
    per_vehicle: dict[str, int] = field(default_factory=lambda: defaultdict(int))


class CaptureState:
    """Per-run state held in memory; flushed on shutdown."""

    def __init__(self, output_dir: Path, trip_id_to_block: dict[str, str]):
        self.output_dir = output_dir
        self.avl_dir = output_dir / "avl"
        self.avl_dir.mkdir(parents=True, exist_ok=True)

        self.trip_id_to_block = trip_id_to_block
        self.stats = Stats()
        # (vehicle_id, unix_timestamp_seconds) pairs already emitted this session.
        self.seen: set[tuple[str, int]] = set()
        # Open file handles keyed by filename — so header is only written once.
        self._writers: dict[Path, csv.writer] = {}
        self._handles: dict[Path, io.TextIOWrapper] = {}

    def writer_for(self, path: Path) -> csv.writer:
        if path in self._writers:
            return self._writers[path]
        existed = path.exists()
        fh = path.open("a", newline="", encoding="utf-8")
        w = csv.writer(fh)
        if not existed:
            w.writerow(CSV_HEADER)
            fh.flush()
        self._writers[path] = w
        self._handles[path] = fh
        return w

    def close(self) -> None:
        for fh in self._handles.values():
            try:
                fh.flush()
                fh.close()
            except Exception:
                log.exception("error closing csv handle")


# --------------------------------------------------------------------------
# Static GTFS
# --------------------------------------------------------------------------


def download_static_gtfs(api_key: str, output_dir: Path) -> Path:
    """Download and unpack static GTFS. Returns the unpacked directory."""
    gtfs_dir = output_dir / "gtfs"
    if gtfs_dir.exists() and (gtfs_dir / "trips.txt").exists():
        log.info("Static GTFS already present at %s — skipping download", gtfs_dir)
        return gtfs_dir

    gtfs_dir.mkdir(parents=True, exist_ok=True)
    log.info("Downloading static GTFS from %s", WMATA_GTFS_STATIC_URL)
    resp = requests.get(
        WMATA_GTFS_STATIC_URL,
        headers={"api_key": api_key, "Cache-Control": "no-cache"},
        timeout=60,
    )
    resp.raise_for_status()
    log.info("Static GTFS downloaded (%d bytes); unpacking", len(resp.content))
    with zipfile.ZipFile(io.BytesIO(resp.content)) as zf:
        zf.extractall(gtfs_dir)
    return gtfs_dir


def build_trip_to_block_map(gtfs_dir: Path) -> dict[str, str]:
    """Parse trips.txt into {trip_id: block_id}. Empty block_ids are dropped."""
    trips_csv = gtfs_dir / "trips.txt"
    mapping: dict[str, str] = {}
    with trips_csv.open(encoding="utf-8-sig", newline="") as fh:
        reader = csv.DictReader(fh)
        if "trip_id" not in reader.fieldnames:
            raise RuntimeError(f"trips.txt missing trip_id column: {reader.fieldnames}")
        has_block = "block_id" in reader.fieldnames
        for row in reader:
            trip_id = row.get("trip_id", "").strip()
            if not trip_id:
                continue
            block_id = row.get("block_id", "").strip() if has_block else ""
            if block_id:
                mapping[trip_id] = block_id
    log.info(
        "Loaded %d trip→block mappings from %s (trips.txt had block_id: %s)",
        len(mapping),
        trips_csv,
        "yes" if mapping else "no",
    )
    return mapping


# --------------------------------------------------------------------------
# Polling
# --------------------------------------------------------------------------


def fetch_vehicle_positions(
    api_key: str,
    session: requests.Session,
) -> Optional[gtfs_realtime_pb2.FeedMessage]:
    """One fetch+parse. Returns None on transient failure (caller retries)."""
    try:
        resp = session.get(
            WMATA_GTFS_RT_VP_URL,
            headers={"api_key": api_key, "Cache-Control": "no-cache"},
            timeout=30,
        )
        resp.raise_for_status()
    except requests.RequestException as e:
        log.warning("GTFS-RT fetch failed: %s", e)
        return None
    try:
        feed = gtfs_realtime_pb2.FeedMessage()
        feed.ParseFromString(resp.content)
    except Exception:
        log.exception("failed to parse GTFS-RT protobuf")
        return None
    return feed


_UNSAFE_FILENAME_CHARS = re.compile(r"[^A-Za-z0-9._-]")


def safe_filename_part(s: str) -> str:
    """Normalise a route/vehicle id into something safe to use in a filename."""
    return _UNSAFE_FILENAME_CHARS.sub("_", s) or "unknown"


def format_timestamp(epoch_seconds: int) -> str:
    """BatchCsvAvlFeedModule expects MM-dd-yyyy HH:mm:ss in JVM default TZ.

    WMATA bus AVL is Eastern — emit America/New_York local time regardless of
    the capture host's TZ so the resulting fixtures are stable.
    """
    dt = datetime.fromtimestamp(epoch_seconds, tz=timezone.utc).astimezone(EASTERN)
    return dt.strftime("%m-%d-%Y %H:%M:%S")


def record_vehicle(
    state: CaptureState,
    vehicle_msg,
    route_filter: Optional[set[str]],
    vehicle_filter: Optional[set[str]],
) -> None:
    """Translate a single GTFS-RT VehiclePosition into a CSV row on disk."""
    vid_obj = getattr(vehicle_msg, "vehicle", None)
    vehicle_id = (vid_obj.id if vid_obj and vid_obj.id else "").strip()
    if not vehicle_id:
        return
    if vehicle_filter and vehicle_id not in vehicle_filter:
        return

    trip = getattr(vehicle_msg, "trip", None)
    trip_id = (trip.trip_id if trip and trip.trip_id else "").strip()
    route_id = (trip.route_id if trip and trip.route_id else "").strip()

    if route_filter and route_id not in route_filter:
        return

    # Resolve assignment.
    assignment_id = ""
    assignment_type = ""
    if trip_id:
        block_id = state.trip_id_to_block.get(trip_id)
        if block_id:
            assignment_id, assignment_type = block_id, "BLOCK_ID"
        else:
            assignment_id, assignment_type = trip_id, "TRIP_ID"
    else:
        state.stats.rows_skipped_no_trip += 1
        return

    # Position / heading / timestamp.
    pos = getattr(vehicle_msg, "position", None)
    if pos is None:
        return
    lat = getattr(pos, "latitude", None)
    lon = getattr(pos, "longitude", None)
    if lat is None or lon is None:
        return
    heading = round(getattr(pos, "bearing", 0.0) or 0.0)

    ts = int(getattr(vehicle_msg, "timestamp", 0) or 0)
    if ts <= 0:
        # No vehicle-reported timestamp — fall back to the feed header's ts.
        # Caller doesn't have access here, so skip; header ts is used only
        # as a last resort in the main loop.
        return

    dedup_key = (vehicle_id, ts)
    if dedup_key in state.seen:
        state.stats.rows_deduped += 1
        return
    state.seen.add(dedup_key)

    time_str = format_timestamp(ts)

    route_part = safe_filename_part(route_id or "UNKROUTE")
    vehicle_part = safe_filename_part(vehicle_id)
    out_path = state.avl_dir / f"{route_part}_{vehicle_part}.csv"
    writer = state.writer_for(out_path)

    writer.writerow(
        [
            vehicle_id,
            time_str,
            assignment_id,
            assignment_type,
            heading,
            f"{lat:.6f}",
            f"{lon:.6f}",
        ]
    )
    state.stats.rows_written += 1
    state.stats.per_vehicle[f"{route_part}_{vehicle_part}"] += 1


# --------------------------------------------------------------------------
# Main loop
# --------------------------------------------------------------------------


def _load_env_file(path: Path) -> None:
    """Minimal .env loader so users don't need `set -a; source .env` shell gymnastics.

    KEY=VALUE per line. Comments (#) and blanks are ignored. Surrounding
    single or double quotes are stripped. Does NOT overwrite keys already
    present in os.environ, so an explicit `export WMATA_API_KEY=...` still
    wins over the file.
    """
    if not path.is_file():
        return
    for raw in path.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, _, value = line.partition("=")
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        if key and key not in os.environ:
            os.environ[key] = value


def run_capture(args: argparse.Namespace) -> int:
    # Auto-load .env from the script's own directory (tools/wmata_capture/.env).
    # Users can also export WMATA_API_KEY directly — that takes precedence.
    _load_env_file(Path(__file__).resolve().parent / ".env")

    api_key = os.environ.get("WMATA_API_KEY", "").strip()
    if not api_key:
        print(
            "WMATA_API_KEY is not set. Put it in tools/wmata_capture/.env "
            "(copy .env.example) or export it in your shell. Never pass it "
            "on the command line.",
            file=sys.stderr,
        )
        return 2

    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    # File log captures everything — repeated runs append so we keep history.
    file_handler = logging.FileHandler(output_dir / "capture.log")
    file_handler.setFormatter(
        logging.Formatter("%(asctime)s %(levelname)s %(name)s %(message)s")
    )
    log.addHandler(file_handler)

    log.info("Capture starting; output_dir=%s", output_dir)

    gtfs_dir = download_static_gtfs(api_key, output_dir)
    trip_to_block = build_trip_to_block_map(gtfs_dir)

    route_filter = {r.strip() for r in args.routes.split(",") if r.strip()} if args.routes else None
    vehicle_filter = {v.strip() for v in args.vehicles.split(",") if v.strip()} if args.vehicles else None
    if route_filter:
        log.info("Filtering to routes: %s", sorted(route_filter))
    if vehicle_filter:
        log.info("Filtering to vehicles: %s", sorted(vehicle_filter))

    state = CaptureState(output_dir, trip_to_block)
    http = requests.Session()

    stop = {"flag": False}

    def _handle_sigint(signum, frame):
        log.info("Shutdown signal received (%s); will stop after current poll", signum)
        stop["flag"] = True

    signal.signal(signal.SIGINT, _handle_sigint)
    signal.signal(signal.SIGTERM, _handle_sigint)

    deadline: Optional[float] = None
    if args.duration_hours and args.duration_hours > 0:
        deadline = time.time() + args.duration_hours * 3600
        log.info("Will stop at %s (duration=%.2fh)", datetime.fromtimestamp(deadline), args.duration_hours)
    else:
        log.info("No duration limit — will run until SIGINT")

    consecutive_failures = 0

    try:
        while not stop["flag"]:
            poll_started = time.time()
            feed = fetch_vehicle_positions(api_key, http)
            if feed is None:
                state.stats.polls_failed += 1
                consecutive_failures += 1
                # Exponential-ish backoff, capped at 2 minutes.
                backoff = min(args.poll_interval * (2 ** min(consecutive_failures, 4)), 120)
                log.warning("Backing off %ss after %d consecutive failure(s)", backoff, consecutive_failures)
                if _sleep_interruptible(backoff, stop):
                    break
                continue

            consecutive_failures = 0
            state.stats.polls_ok += 1

            for entity in feed.entity:
                if entity.HasField("vehicle"):
                    record_vehicle(state, entity.vehicle, route_filter, vehicle_filter)

            log.info(
                "poll ok: entities=%d rows_so_far=%d vehicles=%d",
                len(feed.entity),
                state.stats.rows_written,
                len(state.stats.per_vehicle),
            )

            # Flush periodically so Ctrl-C doesn't lose recent rows.
            for fh in state._handles.values():
                fh.flush()

            if deadline and time.time() >= deadline:
                log.info("Duration reached; stopping")
                break

            elapsed = time.time() - poll_started
            to_sleep = max(0.0, args.poll_interval - elapsed)
            if _sleep_interruptible(to_sleep, stop):
                break

    finally:
        state.close()

        log.info("=" * 60)
        log.info("Capture summary")
        log.info("  polls ok:          %d", state.stats.polls_ok)
        log.info("  polls failed:      %d", state.stats.polls_failed)
        log.info("  rows written:      %d", state.stats.rows_written)
        log.info("  rows deduped:      %d", state.stats.rows_deduped)
        log.info("  rows w/o trip_id:  %d", state.stats.rows_skipped_no_trip)
        log.info("  vehicles captured: %d", len(state.stats.per_vehicle))
        top = sorted(state.stats.per_vehicle.items(), key=lambda kv: -kv[1])[:10]
        for name, n in top:
            log.info("    %s → %d rows", name, n)

    return 0


def _sleep_interruptible(seconds: float, stop: dict) -> bool:
    """Sleep up to `seconds`, returning early if stop['flag'] goes True.

    Returns True if the stop flag was observed (caller should break).
    """
    end = time.time() + seconds
    while not stop["flag"]:
        remaining = end - time.time()
        if remaining <= 0:
            return False
        time.sleep(min(remaining, 1.0))
    return True


def parse_args(argv: list[str]) -> argparse.Namespace:
    ap = argparse.ArgumentParser(
        description="Capture WMATA GTFS + GTFS-RT VehiclePositions into transitclock AVL-CSV fixtures.",
    )
    ap.add_argument("--output-dir", required=True, help="Destination directory (created if absent).")
    ap.add_argument(
        "--duration-hours",
        type=float,
        default=0.0,
        help="Stop after this many hours. 0 or omitted = run until SIGINT.",
    )
    ap.add_argument(
        "--poll-interval",
        type=float,
        default=30.0,
        help="Seconds between GTFS-RT polls (default 30).",
    )
    ap.add_argument(
        "--routes",
        default="",
        help="Comma-separated GTFS route_ids to keep (default: all).",
    )
    ap.add_argument(
        "--vehicles",
        default="",
        help="Comma-separated vehicle ids to keep (default: all).",
    )
    ap.add_argument(
        "--log-level",
        default="INFO",
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
    )
    return ap.parse_args(argv)


def main(argv: Optional[list[str]] = None) -> int:
    args = parse_args(argv if argv is not None else sys.argv[1:])

    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s %(levelname)s %(message)s",
        stream=sys.stderr,
    )

    try:
        return run_capture(args)
    except KeyboardInterrupt:
        log.info("Interrupted by user")
        return 130


if __name__ == "__main__":
    raise SystemExit(main())
