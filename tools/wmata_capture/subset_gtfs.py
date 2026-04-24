#!/usr/bin/env -S uv run
# /// script
# requires-python = ">=3.10"
# dependencies = []
# ///
"""
Subset a full GTFS feed down to a single route, producing the kind of
self-contained mini-feed the transitclockIntegration fixtures expect.

Given --input pointing at an unpacked full GTFS dir and --route as a
route_short_name (e.g. "D40"), writes a subsetted GTFS to --output with:

  - routes.txt      only the matching row
  - trips.txt       only trips whose route_id matches
  - stop_times.txt  only rows whose trip_id is in the kept trips
  - stops.txt       only stops referenced by kept stop_times
                    (and their parent_station rows, transitively)
  - shapes.txt      only shapes referenced by kept trips
  - calendar.txt / calendar_dates.txt
                    only service_ids referenced by kept trips
  - agency.txt, feed_info.txt  copied verbatim (small, always safe)

Other files in --input are ignored rather than blindly copied — WMATA
sometimes ships extras (timepoints.txt etc.) that the old fixtures
don't include.
"""
from __future__ import annotations

import argparse
import csv
import shutil
import sys
from pathlib import Path


def read_csv(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open(newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        fields = reader.fieldnames or []
        rows = list(reader)
    return fields, rows


def write_csv(path: Path, fields: list[str], rows: list[dict[str, str]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def resolve_route_id(routes_fields: list[str], routes_rows: list[dict[str, str]], route_short_name: str) -> str:
    if "route_short_name" not in routes_fields:
        raise SystemExit("routes.txt has no route_short_name column")
    matches = [r for r in routes_rows if r["route_short_name"] == route_short_name]
    if not matches:
        raise SystemExit(f"No route with route_short_name={route_short_name!r} in input feed")
    if len(matches) > 1:
        ids = ",".join(r["route_id"] for r in matches)
        raise SystemExit(f"Multiple routes match {route_short_name!r}: {ids}. Disambiguate.")
    return matches[0]["route_id"]


def subset(input_dir: Path, output_dir: Path, route_short_name: str) -> None:
    if output_dir.exists():
        raise SystemExit(f"Refusing to overwrite existing {output_dir} — delete it first")

    routes_fields, routes_rows = read_csv(input_dir / "routes.txt")
    route_id = resolve_route_id(routes_fields, routes_rows, route_short_name)
    kept_route = [r for r in routes_rows if r["route_id"] == route_id]

    trips_fields, trips_rows = read_csv(input_dir / "trips.txt")
    kept_trips = [t for t in trips_rows if t["route_id"] == route_id]
    kept_trip_ids = {t["trip_id"] for t in kept_trips}
    kept_service_ids = {t["service_id"] for t in kept_trips}
    kept_shape_ids = {t["shape_id"] for t in kept_trips if t.get("shape_id")}

    st_fields, st_rows = read_csv(input_dir / "stop_times.txt")
    kept_stop_times = [r for r in st_rows if r["trip_id"] in kept_trip_ids]
    kept_stop_ids = {r["stop_id"] for r in kept_stop_times}

    stops_fields, stops_rows = read_csv(input_dir / "stops.txt")
    stops_by_id = {s["stop_id"]: s for s in stops_rows}
    # transitively include parent stations
    closed: set[str] = set()
    frontier = set(kept_stop_ids)
    while frontier:
        sid = frontier.pop()
        if sid in closed or sid not in stops_by_id:
            continue
        closed.add(sid)
        parent = stops_by_id[sid].get("parent_station", "")
        if parent and parent not in closed:
            frontier.add(parent)
    kept_stops = [stops_by_id[s] for s in sorted(closed) if s in stops_by_id]

    output_dir.mkdir(parents=True)

    write_csv(output_dir / "routes.txt", routes_fields, kept_route)
    write_csv(output_dir / "trips.txt", trips_fields, kept_trips)
    write_csv(output_dir / "stop_times.txt", st_fields, kept_stop_times)
    write_csv(output_dir / "stops.txt", stops_fields, kept_stops)

    shapes_path = input_dir / "shapes.txt"
    if shapes_path.exists() and kept_shape_ids:
        shapes_fields, shapes_rows = read_csv(shapes_path)
        kept_shapes = [r for r in shapes_rows if r["shape_id"] in kept_shape_ids]
        write_csv(output_dir / "shapes.txt", shapes_fields, kept_shapes)

    cal_path = input_dir / "calendar.txt"
    if cal_path.exists():
        cal_fields, cal_rows = read_csv(cal_path)
        kept_cal = [r for r in cal_rows if r["service_id"] in kept_service_ids]
        write_csv(output_dir / "calendar.txt", cal_fields, kept_cal)

    cd_path = input_dir / "calendar_dates.txt"
    if cd_path.exists():
        cd_fields, cd_rows = read_csv(cd_path)
        kept_cd = [r for r in cd_rows if r["service_id"] in kept_service_ids]
        write_csv(output_dir / "calendar_dates.txt", cd_fields, kept_cd)

    for verbatim in ("agency.txt", "feed_info.txt"):
        src = input_dir / verbatim
        if src.exists():
            shutil.copy2(src, output_dir / verbatim)

    report = [
        f"route_short_name={route_short_name} route_id={route_id}",
        f"trips:       {len(kept_trips)} / {len(trips_rows)}",
        f"stop_times:  {len(kept_stop_times)} / {len(st_rows)}",
        f"stops:       {len(kept_stops)} / {len(stops_rows)}",
        f"shapes:      {len(kept_shape_ids)} shape_id(s)",
        f"services:    {len(kept_service_ids)} service_id(s)",
        f"output:      {output_dir}",
    ]
    print("\n".join(report))


def parse_args(argv: list[str]) -> argparse.Namespace:
    ap = argparse.ArgumentParser(description="Subset a GTFS feed to a single route.")
    ap.add_argument("--input", required=True, type=Path, help="Full unpacked GTFS directory")
    ap.add_argument("--output", required=True, type=Path, help="Destination directory (must not exist)")
    ap.add_argument("--route", required=True, help="route_short_name to keep (e.g. D40)")
    return ap.parse_args(argv)


def main() -> int:
    args = parse_args(sys.argv[1:])
    subset(args.input, args.output, args.route)
    return 0


if __name__ == "__main__":
    sys.exit(main())
