#!/usr/bin/env -S uv run
# /// script
# requires-python = ">=3.10"
# dependencies = []
# ///
"""
Rank AVL traces by how detour-like they look, for picking a fixture
vehicle for transitclockIntegration's RecoverFromDetourTest.

The test asserts a vehicle ends in a non-layover state with reasonable
schedule adherence after going off-route and returning. A "good" fixture
trace therefore needs to contain an off-route excursion followed by a
return. Row count alone doesn't predict this — most long traces are
well-behaved vehicles that never left their shape.

Heuristic: for each AVL point compute the min haversine distance to
the route's GTFS shape points (union of all shape_ids the route uses).
Classify points as off-route if distance exceeds --off-route-m. Score
vehicles by the longest off-route run that was bracketed by on-route
points on both sides — that's "went off and came back."

Output a ranked table; trust-but-verify by eyeballing the top few
in a map before promoting.
"""
from __future__ import annotations

import argparse
import csv
import math
import sys
from pathlib import Path


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def load_shape_points(gtfs_dir: Path) -> list[tuple[float, float]]:
    points: list[tuple[float, float]] = []
    with (gtfs_dir / "shapes.txt").open(newline="", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            points.append((float(row["shape_pt_lat"]), float(row["shape_pt_lon"])))
    return points


def min_distance_m(lat: float, lon: float, shape_points: list[tuple[float, float]]) -> float:
    best = float("inf")
    for slat, slon in shape_points:
        d = haversine_m(lat, lon, slat, slon)
        if d < best:
            best = d
    return best


def score_vehicle(avl_path: Path, shape_points: list[tuple[float, float]], off_route_m: float) -> dict:
    rows: list[tuple[float, float, float]] = []  # (t_epoch_approx_index, lat, lon) — index is fine for run-length
    with avl_path.open(newline="", encoding="utf-8") as f:
        for i, r in enumerate(csv.DictReader(f)):
            rows.append((float(i), float(r["latitude"]), float(r["longitude"])))

    if not rows:
        return {"vehicle": avl_path.stem, "rows": 0, "skip": True}

    distances = [min_distance_m(lat, lon, shape_points) for _, lat, lon in rows]
    off = [d > off_route_m for d in distances]

    # longest off-route run that has on-route points on both sides.
    longest_bracketed_run = 0
    current_run = 0
    saw_on_before = False
    run_has_on_before = False
    for is_off in off:
        if is_off:
            if current_run == 0:
                run_has_on_before = saw_on_before
            current_run += 1
        else:
            if current_run > 0 and run_has_on_before:
                if current_run > longest_bracketed_run:
                    longest_bracketed_run = current_run
            current_run = 0
            saw_on_before = True
    # any still-open run at end of trace doesn't count (no "return" yet)

    off_count = sum(1 for x in off if x)

    return {
        "vehicle": avl_path.stem,
        "rows": len(rows),
        "max_dist_m": max(distances),
        "median_dist_m": sorted(distances)[len(distances) // 2],
        "off_points": off_count,
        "off_pct": 100.0 * off_count / len(rows),
        "longest_bracketed_off_run": longest_bracketed_run,
        "ends_on_route": not off[-1],
        "skip": False,
    }


def parse_args(argv: list[str]) -> argparse.Namespace:
    ap = argparse.ArgumentParser(description="Rank AVL traces by detour-likeness against a route's GTFS shape.")
    ap.add_argument("--gtfs", required=True, type=Path, help="Subsetted GTFS dir for the route (contains shapes.txt)")
    ap.add_argument("--avl-dir", required=True, type=Path, help="Directory of <route>_<vehicle>.csv AVL files")
    ap.add_argument("--route-prefix", required=True, help="File prefix to filter (e.g. A40 → matches A40_*.csv)")
    ap.add_argument("--off-route-m", type=float, default=150.0, help="Meters beyond route shape to count as off-route (default 150)")
    ap.add_argument("--top", type=int, default=10, help="How many top candidates to print (default 10)")
    return ap.parse_args(argv)


def main() -> int:
    args = parse_args(sys.argv[1:])
    shape_points = load_shape_points(args.gtfs)
    if not shape_points:
        raise SystemExit(f"No shape points loaded from {args.gtfs}/shapes.txt")

    avl_files = sorted(args.avl_dir.glob(f"{args.route_prefix}_*.csv"))
    if not avl_files:
        raise SystemExit(f"No {args.route_prefix}_*.csv files under {args.avl_dir}")

    print(f"Loaded {len(shape_points)} shape points for {args.route_prefix}")
    print(f"Scoring {len(avl_files)} AVL files (off-route threshold: {args.off_route_m:.0f} m)")
    print()

    results = []
    for path in avl_files:
        res = score_vehicle(path, shape_points, args.off_route_m)
        if not res["skip"]:
            results.append(res)

    # detour candidates: had a bracketed off-route run, ended back on route
    results.sort(key=lambda r: (r["longest_bracketed_off_run"], r["max_dist_m"]), reverse=True)

    print(f"{'vehicle':<12} {'rows':>5} {'max_m':>8} {'med_m':>7} {'off%':>6} {'longest_off_run':>16} {'ends_on':>8}")
    print("-" * 70)
    for r in results[: args.top]:
        print(
            f"{r['vehicle']:<12} {r['rows']:>5} {r['max_dist_m']:>8.0f} "
            f"{r['median_dist_m']:>7.0f} {r['off_pct']:>5.1f}% "
            f"{r['longest_bracketed_off_run']:>16} {str(r['ends_on_route']):>8}"
        )

    print()
    print("Columns:")
    print("  max_m              max distance from route shape (m)")
    print("  med_m              median distance from route shape (m)")
    print("  off%               pct of AVL points beyond --off-route-m")
    print("  longest_off_run    longest consecutive off-route span bracketed by on-route points")
    print("  ends_on            True if final AVL point is back on route")
    print()
    print("Best detour candidates: high longest_off_run AND ends_on=True.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
