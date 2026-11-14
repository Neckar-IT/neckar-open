#!/usr/bin/env bash
# Render example CAD floor plans through the floor-plan pipeline — reproducibly.
#
#   render_examples.sh <CAD_DIR> [NAME_FILTER]
#
#   CAD_DIR      directory with *.dwg / *.dxf
#   NAME_FILTER  only render files whose base name contains this substring (default: all)
#
# Bootstraps a local venv (.venv/ next to this script) on first run. When a base name exists
# as both .dwg and .dxf, the .dxf is used (no ODA conversion needed; the architect exports and
# their <name>.rooms.json share the base name either way). Output: CAD_DIR/renders/<base>/
# (git-ignored) — one directory per plan, one subdirectory per floor with index.html picker.
# Each target directory is removed before its run, so a re-run never mixes stale floors in.
set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CAD_DIR="${1:-}"
NAME_FILTER="${2:-}"
VENV="$TOOLS_DIR/.venv"

[ -n "$CAD_DIR" ] || { echo "usage: render_examples.sh <CAD_DIR> [NAME_FILTER]" >&2; exit 2; }
[ -d "$CAD_DIR" ] || { echo "CAD directory not found: $CAD_DIR" >&2; exit 1; }

if [ ! -x "$VENV/bin/python" ]; then
  echo "bootstrapping venv at $VENV"
  python3 -m venv "$VENV"
fi
"$VENV/bin/pip" install --quiet --requirement "$TOOLS_DIR/requirements.txt"

shopt -s nullglob
failed=()
for src in "$CAD_DIR"/*.dwg; do
  base="$(basename "$src" .dwg)"
  [ -f "$CAD_DIR/$base.dxf" ] && continue   # DXF twin exists -> rendered in the DXF loop below
  [ -n "$NAME_FILTER" ] && [[ "$base" != *"$NAME_FILTER"* ]] && continue
  echo "=== $base"
  rm -rf "$CAD_DIR/renders/$base"
  "$VENV/bin/python" "$TOOLS_DIR/floorplan_pipeline.py" "$src" "$CAD_DIR/renders/$base" || failed+=("$base")
done
for src in "$CAD_DIR"/*.dxf; do
  base="$(basename "$src" .dxf)"
  [ -n "$NAME_FILTER" ] && [[ "$base" != *"$NAME_FILTER"* ]] && continue
  echo "=== $base"
  rm -rf "$CAD_DIR/renders/$base"
  "$VENV/bin/python" "$TOOLS_DIR/floorplan_pipeline.py" "$src" "$CAD_DIR/renders/$base" || failed+=("$base")
done

if [ "${#failed[@]}" -gt 0 ]; then
  echo "FAILED: ${failed[*]}" >&2
  exit 1
fi
echo "all renders in $CAD_DIR/renders/"
