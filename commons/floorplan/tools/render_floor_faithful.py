#!/usr/bin/env python3
"""Faithful, original-fidelity render of a SINGLE floor.

ezdxf and Matplotlib flatten hatches and colour-fills and cannot honour the architect's lineweights;
a real CAD engine does. QCAD's headless `dwg2pdf` renders faithfully (native ACI colours, correct
lineweights, proper hatch patterns, text masks) but only ever renders the whole model space - where all
floors plus the sections, elevations and site plans sit together.

This tool bridges the two without ever deleting floor geometry (deleting entities mid-element cuts
walls and breaks hatch boundaries). It only removes the site-context *layers*, then lets QCAD clip to
one floor via a print window:

  1. Read the floor's bounding box from the extracted `<name>.rooms.json` (per-floor calibration).
  2. Drop only the site-context layers (property, terrain, existing buildings, drainage) - they belong
     to the Lageplan, not the floor, and never take part in the floor's walls or hatches. Everything
     else stays exactly as drawn, fully intact.
  3. Render with QCAD at a fixed architectural scale (1:50) onto a paper sized to the floor's window,
     offset so that window fills the page. Geometry outside the window is clipped cleanly at the page
     edge - the floor itself is rendered complete, with intact hatches and the architect's lineweights.

QCAD is located via the QCAD_DWG2PDF environment variable or `dwg2pdf` on PATH. Download the portable
build once (see README); it is not committed.

Usage: python3 render_floor_faithful.py <DXF> <ROOMS_JSON> <FLOOR_INDEX> <OUT_DIR> [MARGIN_M]
"""
import json
import os
import shutil
import subprocess
import sys

import ezdxf
from ezdxf import bbox

# Site-context layers dropped from a single-floor render (they belong to the Lageplan, not the floor).
SITE_LAYERS = {"00_Grundstück", "00_Gelände", "00_Bestandsgebäude", "00_Entwässerung", "00_EntwässerungSanitär"}
DEFAULT_MARGIN_M = 3.5  # keep window = room cluster + this margin (walls, dimensions and notes); cut in empty space
# Entities wider/taller than this are layout-spanning (long dimension chains, leaders to other floors)
# and are dropped so QCAD's auto-fit frames the floor itself, not a far-reaching stray line.
MAX_ENTITY_SPAN_M = 30.0
# Raster resolution for the PNG preview. The whole sheet is large, so a full-sheet baseline needs a
# high DPI for the individual floors to be legible; override via FLOORPLAN_RASTER_DPI. The PDF is vector
# and stays sharp at any zoom regardless of this value.
RASTER_DPI = int(os.environ.get("FLOORPLAN_RASTER_DPI", "400"))


def prepare_floor(dxf_path, rooms_json_path, floor_index, out_dxf, margin):
    """Isolate one floor into out_dxf: drop the site-context layers and every entity whose bounding box
    lies outside the floor window (room cluster + margin) or spans more than MAX_ENTITY_SPAN_M (a stray
    layout line). The kept geometry is the floor, intact; QCAD then auto-fits and centres it."""
    floors = json.load(open(rooms_json_path, encoding="utf-8"))["floors"]
    calibration = floors[floor_index - 1]["calibration"]
    x0, y0 = calibration["dxfMinX"] - margin, calibration["dxfMinY"] - margin
    x1, y1 = calibration["dxfMaxX"] + margin, calibration["dxfMaxY"] + margin

    document = ezdxf.readfile(dxf_path)
    model_space = document.modelspace()
    cache = bbox.Cache()
    to_delete = []
    kept = 0
    for entity in model_space:
        if entity.dxf.layer in SITE_LAYERS:
            to_delete.append(entity)
            continue
        try:
            box = bbox.extents([entity], fast=True, cache=cache)
        except Exception:
            box = None
        if box is None or not box.has_data:
            to_delete.append(entity)
            continue
        outside = box.extmax.x < x0 or box.extmin.x > x1 or box.extmax.y < y0 or box.extmin.y > y1
        spanning = (box.extmax.x - box.extmin.x) > MAX_ENTITY_SPAN_M or (box.extmax.y - box.extmin.y) > MAX_ENTITY_SPAN_M
        if outside or spanning:
            to_delete.append(entity)
        else:
            kept += 1
    for entity in to_delete:
        model_space.delete_entity(entity)
    document.saveas(out_dxf)
    return kept


def find_qcad():
    """QCAD_DWG2PDF env, then dwg2pdf on PATH, then the local tools/.qcad/ install (setup_qcad.sh)."""
    local = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".qcad", "dwg2pdf")
    return os.environ.get("QCAD_DWG2PDF") or shutil.which("dwg2pdf") or (local if os.path.isfile(local) else None)


def render_with_qcad(qcad, out_dxf, out_pdf):
    subprocess.run(
        [qcad, "-platform", "offscreen", "-f", "-a", "-c", "-auto-orientation", "-p", "A3", "-w", "0.2",
         "-o", out_pdf, out_dxf],
        check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    if shutil.which("pdftoppm"):
        # -singlefile: write "<out_pdf without .pdf>.png" (no page-number "-1" suffix)
        subprocess.run(["pdftoppm", "-png", "-singlefile", "-r", str(RASTER_DPI), out_pdf, out_pdf[:-4]],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def main():
    if len(sys.argv) < 5:
        raise SystemExit("usage: render_floor_faithful.py <DXF> <ROOMS_JSON> <FLOOR_INDEX> <OUT_DIR> [MARGIN_M]")
    dxf_path, rooms_json_path, floor_index, out_dir = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4]
    margin = float(sys.argv[5]) if len(sys.argv) > 5 else DEFAULT_MARGIN_M
    os.makedirs(out_dir, exist_ok=True)

    name = os.path.splitext(os.path.basename(dxf_path))[0].split("-")[0]
    base = f"{name}.floor{floor_index}"
    # The isolated floor DXF is an ezdxf crop (intermediate); the render carries the QCAD tool tag.
    out_dxf = os.path.join(out_dir, f"{base}.ezdxf-crop.dxf")
    kept = prepare_floor(dxf_path, rooms_json_path, floor_index, out_dxf, margin)
    print(f"{base}: isolated to {kept} entities -> {out_dxf}")

    qcad = find_qcad()
    if not qcad:
        print("QCAD not found (set QCAD_DWG2PDF or put dwg2pdf on PATH). Render the isolated DXF with:")
        print(f"  dwg2pdf -platform offscreen -f -a -c -auto-orientation -p A3 -w 0.2 "
              f"-o {os.path.join(out_dir, base)}.qcad.pdf {out_dxf}")
        return
    out_pdf = os.path.join(out_dir, f"{base}.qcad.pdf")
    render_with_qcad(qcad, out_dxf, out_pdf)
    print(f"faithful render -> {out_pdf}" + (" (+ .qcad.png)" if shutil.which("pdftoppm") else ""))


if __name__ == "__main__":
    main()
