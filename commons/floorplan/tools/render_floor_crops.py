#!/usr/bin/env python3
"""Render each dimensioned single floor of a plan, cropped out of the model space.

The architect stacks the plan several times in model space: an upper band of undimensioned trade copies
(plumbing, drainage) and a lower band of dimensioned architectural floors. Cropping to the room cluster
(render_floor_faithful) hit the undimensioned copy and a too-tight window, so the dimensions were lost.

This tool instead finds the building copies (2D-clusters the 00_Wände polylines), keeps the ones that
carry dimensions, crops each to its wall bounding box + margin (dropping the site-context layers), and
renders it faithfully with QCAD - a sharp, dimensioned single-floor sheet, the cropped counterpart to
the full-sheet baseline.

Run tools/setup_qcad.sh once so QCAD is found automatically (or set QCAD_DWG2PDF).

Usage: python3 render_floor_crops.py <CAD_DIR> [OUT_DIR] [NAME_FILTER]
  OUT_DIR      default <CAD_DIR>/renders/baseline (git-ignored)
  NAME_FILTER  optional substring; only plans whose filename contains it are rendered
"""
import glob
import os
import sys

import ezdxf
from ezdxf import bbox

import render_floor_faithful as ff

COPY_GAP_M = 15.0     # wall clusters separated by more than this are separate building copies
CROP_MARGIN_M = 6.0   # keep the wall bbox + this much (captures the dimension chains around the floor)
MIN_DIMENSIONS = 5    # a copy with at least this many A_Bemassung texts is a dimensioned architectural floor

# Layers dropped from a floor crop: the site context (belongs to the Lageplan) plus the solid colour
# fills (00_Farben) - large room-colour hatches that reach well beyond the floor and blow up the
# auto-fit so the plan renders tiny. Neither is part of the dimensioned architectural drawing.
CROP_DROP_LAYERS = ff.SITE_LAYERS | {"00_Farben"}


def wall_clusters(msp, cache):
    """2D-cluster the 00_Wände polyline centers into building copies; return a bbox (x0,y0,x1,y1) each."""
    centers = []
    for e in msp:
        if e.dxf.layer == "00_Wände" and e.dxftype() == "LWPOLYLINE":
            b = bbox.extents([e], fast=True, cache=cache)
            if b.has_data:
                centers.append((b.center.x, b.center.y, b.extmin.x, b.extmin.y, b.extmax.x, b.extmax.y))
    clusters = []
    for c in sorted(centers):
        for cl in clusters:
            if any(abs(c[0] - q[0]) < COPY_GAP_M and abs(c[1] - q[1]) < COPY_GAP_M for q in cl):
                cl.append(c)
                break
        else:
            clusters.append([c])
    boxes = []
    for cl in clusters:
        boxes.append((min(q[2] for q in cl), min(q[3] for q in cl),
                      max(q[4] for q in cl), max(q[5] for q in cl)))
    return boxes


def count_dimensions(msp, box, cache):
    x0, y0, x1, y1 = box
    n = 0
    for e in msp:
        if e.dxf.layer == "A_Bemassung" and e.dxftype() == "MTEXT":
            b = bbox.extents([e], fast=True, cache=cache)
            if b.has_data and x0 <= b.center.x <= x1 and y0 <= b.center.y <= y1:
                n += 1
    return n


def crop_to_box(dxf_path, box, margin, out_dxf):
    """Load the DXF fresh and keep only entities within box+margin, minus the site-context layers."""
    x0, y0, x1, y1 = box[0] - margin, box[1] - margin, box[2] + margin, box[3] + margin
    document = ezdxf.readfile(dxf_path)
    msp = document.modelspace()
    cache = bbox.Cache()
    to_delete = []
    for e in msp:
        if e.dxf.layer in CROP_DROP_LAYERS:
            to_delete.append(e)
            continue
        b = bbox.extents([e], fast=True, cache=cache)
        if not b.has_data or b.extmax.x < x0 or b.extmin.x > x1 or b.extmax.y < y0 or b.extmin.y > y1:
            to_delete.append(e)
            continue
        # layout-spanning strays (long leaders, neighbour-plot outlines reaching in) blow up QCAD's
        # auto-fit so the floor renders tiny; drop them the way render_floor_faithful does.
        if (b.extmax.x - b.extmin.x) > ff.MAX_ENTITY_SPAN_M or (b.extmax.y - b.extmin.y) > ff.MAX_ENTITY_SPAN_M:
            to_delete.append(e)
    for e in to_delete:
        msp.delete_entity(e)
    document.saveas(out_dxf)


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: render_floor_crops.py <CAD_DIR> [OUT_DIR] [NAME_FILTER]")
    cad_dir = sys.argv[1]
    out_dir = (len(sys.argv) > 2 and sys.argv[2]) or os.path.join(cad_dir, "renders", "baseline")
    name_filter = sys.argv[3] if len(sys.argv) > 3 else ""
    os.makedirs(out_dir, exist_ok=True)
    crops_dir = os.path.join(out_dir, "crops")
    os.makedirs(crops_dir, exist_ok=True)

    qcad = ff.find_qcad()
    if not qcad:
        raise SystemExit("QCAD not found. Run tools/setup_qcad.sh first (or set QCAD_DWG2PDF).")

    dxf_files = sorted(f for f in glob.glob(os.path.join(cad_dir, "*.dxf")) if name_filter in os.path.basename(f))
    if not dxf_files:
        raise SystemExit(f"no DXF matching '{name_filter}' in {cad_dir}")

    for dxf_path in dxf_files:
        name = os.path.splitext(os.path.basename(dxf_path))[0].split("-")[0]
        document = ezdxf.readfile(dxf_path)
        msp = document.modelspace()
        cache = bbox.Cache()
        boxes = wall_clusters(msp, cache)
        dimensioned = [(b, count_dimensions(msp, b, cache)) for b in boxes]
        dimensioned = [(b, d) for b, d in dimensioned if d >= MIN_DIMENSIONS]
        dimensioned.sort(key=lambda bd: bd[0][0])  # left to right
        print(f"{name}: {len(dimensioned)} dimensioned floor(s)")
        for i, (box, dims) in enumerate(dimensioned, start=1):
            base = f"{name}.floor{i}"
            out_dxf = os.path.join(crops_dir, f"{base}.ezdxf-crop.dxf")
            crop_to_box(dxf_path, box, CROP_MARGIN_M, out_dxf)
            out_pdf = os.path.join(out_dir, f"{base}.qcad.pdf")
            ff.render_with_qcad(qcad, out_dxf, out_pdf)
            print(f"  {base}: {dims} dimensions -> {out_pdf}" +
                  (" (+ .qcad.png)" if os.path.exists(out_pdf[:-4] + ".png") else ""))


if __name__ == "__main__":
    main()
