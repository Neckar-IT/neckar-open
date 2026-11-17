#!/usr/bin/env bash
# Install the portable QCAD build into tools/.qcad/ for headless baseline rendering.
#
# QCAD's `dwg2pdf` renders DXF faithfully - native ACI colours, intact hatches, correct text - which is
# the local, automatic equivalent of the Autodesk Viewer baseline (verified: same drawing). DXF support
# is GPL and needs no DWG plugin. render_floor_faithful.py / render_baselines.py find this install
# automatically (see find_qcad()).
#
# Idempotent: re-running is a no-op once installed. The .qcad/ directory is git-ignored (~135 MB).
set -euo pipefail

VERSION="3.32.9"
URL="https://www.qcad.org/archives/qcad/qcad-${VERSION}-trial-linux-qt6-x86_64.tar.gz"

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.qcad"
BIN="$DIR/dwg2pdf"

if [ -x "$BIN" ]; then
  echo "QCAD already installed: $BIN"
  exit 0
fi

mkdir -p "$DIR"
echo "Downloading QCAD ${VERSION} (~135 MB)..."
curl -sSL -o "$DIR/qcad.tar.gz" "$URL"
tar xzf "$DIR/qcad.tar.gz" -C "$DIR" --strip-components=1
rm -f "$DIR/qcad.tar.gz"

if [ -x "$BIN" ]; then
  echo "QCAD ready: $BIN"
else
  echo "ERROR: dwg2pdf not found after extraction; check the archive layout in $DIR" >&2
  exit 1
fi
