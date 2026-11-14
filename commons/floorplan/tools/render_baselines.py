#!/usr/bin/env python3
"""Batch-render a faithful full-sheet CAD baseline (PDF + PNG) for every plan in a CAD dir.

One QCAD dwg2pdf per DXF: the whole model space exactly as the architect drew it, with every
dimension - no cropping. The source plans stack the floor plan several times in model space (one copy
per trade), so per-floor cropping picks the wrong copy and loses the dimensions; the full sheet is the
complete, faithful reference to evaluate and compare against.

Run tools/setup_qcad.sh once so QCAD is found automatically (or set QCAD_DWG2PDF).

Usage: python3 render_baselines.py <CAD_DIR> [OUT_DIR] [NAME_FILTER]
  OUT_DIR      default <CAD_DIR>/renders/baseline (git-ignored)
  NAME_FILTER  optional substring; only plans whose filename contains it are rendered
"""
import glob
import os
import sys

import render_floor_faithful as ff


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: render_baselines.py <CAD_DIR> [OUT_DIR] [NAME_FILTER]")
    cad_dir = sys.argv[1]
    out_dir = (len(sys.argv) > 2 and sys.argv[2]) or os.path.join(cad_dir, "renders", "baseline")
    name_filter = sys.argv[3] if len(sys.argv) > 3 else ""
    os.makedirs(out_dir, exist_ok=True)

    qcad = ff.find_qcad()
    if not qcad:
        raise SystemExit("QCAD not found. Run tools/setup_qcad.sh first (or set QCAD_DWG2PDF).")

    dxf_files = sorted(f for f in glob.glob(os.path.join(cad_dir, "*.dxf")) if name_filter in os.path.basename(f))
    if not dxf_files:
        raise SystemExit(f"no DXF matching '{name_filter}' in {cad_dir}")

    for dxf_path in dxf_files:
        name = os.path.splitext(os.path.basename(dxf_path))[0].split("-")[0]
        out_pdf = os.path.join(out_dir, f"{name}.fullsheet.qcad.pdf")
        ff.render_with_qcad(qcad, dxf_path, out_pdf)
        print(f"{name}: {out_pdf}" + (" (+ .qcad.png)" if os.path.exists(out_pdf[:-4] + ".png") else ""))


if __name__ == "__main__":
    main()
