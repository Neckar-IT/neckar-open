#!/usr/bin/env python3
"""Render the SAME model-space window through every available engine, side by side, for comparison.

Given a DXF that still carries paper-space layout viewports, this extracts one layout's main content
viewport window (the exact model-space rectangle that viewport frames) and renders it three ways so
their fidelity can be judged against each other:

  ezdxf     ezdxf drawing add-on + matplotlib backend; native crop (axis limits = window). Pure Python,
            no trial delay, truly headless.
  qcad      QCAD dwg2pdf (Professional trial: faithful native ACI / lineweights / hatch, 15 s delay per
            run). Located via $QCAD_DWG2PDF or dwg2pdf on PATH.
  librecad  LibreCAD dxf2pdf (GPL, no delay; needs an X server -> wrapped in xvfb-run). Located via
            $LIBRECAD_DXF2PDF, or `librecad` on PATH run as `librecad dxf2pdf`.

The CAD engines (qcad, librecad) have no window-plot flag, so they render a copy of model space with
everything outside the window deleted, then auto-fit. ezdxf clips natively via axis limits.

Engines that are not installed are skipped with a note (so this can run before LibreCAD is present).

Usage: render_engine_comparison.py <dxf> <layout-name> <out_dir> [dpi]
"""
import os
import shutil
import subprocess
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

import ezdxf
from ezdxf.addons.drawing import RenderContext, Frontend
from ezdxf.addons.drawing.matplotlib import MatplotlibBackend

import _ezdxf_font_guard  # noqa: F401  # font-metric divide-by-zero guard (import for side effect)
import viewport_pipeline as vp

RASTER_DPI = 200  # px per rendered PDF inch, for pdftoppm on the CAD-engine outputs


def render_ezdxf(dxf_path, window, out_png, dpi):
    x0, y0, x1, y1 = window
    doc = ezdxf.readfile(dxf_path)
    for mtext in doc.modelspace().query("MTEXT"):   # drop background masks (else they render as black boxes)
        try:
            mtext.set_bg_color(None)
        except Exception:
            pass
    msp = doc.modelspace()
    figure = plt.figure(figsize=(max(x1 - x0, 1e-3), max(y1 - y0, 1e-3)), dpi=dpi)
    axis = figure.add_axes([0, 0, 1, 1])
    Frontend(RenderContext(doc), MatplotlibBackend(axis)).draw_entities(msp)
    axis.set_xlim(x0, x1)          # set AFTER drawing so it is not overridden by autoscale
    axis.set_ylim(y0, y1)
    axis.set_aspect("equal")
    axis.axis("off")
    figure.savefig(out_png, dpi=dpi, facecolor="white")
    plt.close(figure)


def content_aspect_paper(window):
    """Paper sized to the window's own aspect ratio (mm), so an auto-fit render fills it."""
    long_mm = 900.0
    w, h = window[2] - window[0], window[3] - window[1]
    if w >= h:
        return f"{long_mm:.0f}x{max(long_mm * h / w, 1):.0f}"
    return f"{max(long_mm * w / h, 1):.0f}x{long_mm:.0f}"


def pdf_to_png(pdf, png):
    subprocess.run(["pdftoppm", "-png", "-r", str(RASTER_DPI), "-singlefile", pdf, png[:-4]],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def render_qcad(cropped_dxf, window, out_pdf, out_png):
    qcad = os.environ.get("QCAD_DWG2PDF") or shutil.which("dwg2pdf")
    if not qcad:
        return "skipped (QCAD not found: set $QCAD_DWG2PDF or put dwg2pdf on PATH)"
    subprocess.run([qcad, "-platform", "offscreen", "-f", "-a", "-c",
                    "-p", content_aspect_paper(window), "-w", "0.2", "-o", out_pdf, cropped_dxf],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    pdf_to_png(out_pdf, out_png)
    return f"ok -> {os.path.basename(out_png)}"


def render_librecad(cropped_dxf, window, out_pdf, out_png):
    tool = os.environ.get("LIBRECAD_DXF2PDF")
    if tool:
        base_cmd = [tool]
    elif shutil.which("librecad"):
        base_cmd = ["librecad", "dxf2pdf"]
    else:
        return "skipped (LibreCAD not found: install librecad, or set $LIBRECAD_DXF2PDF)"
    paper = content_aspect_paper(window)
    cmd = base_cmd + ["-a", "-c", "-p", paper, "-o", out_pdf, cropped_dxf]
    if not os.environ.get("LIBRECAD_DXF2PDF") and shutil.which("xvfb-run"):
        cmd = ["xvfb-run", "-a"] + cmd  # LibreCAD needs an X server
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    pdf_to_png(out_pdf, out_png)
    return f"ok -> {os.path.basename(out_png)}"


def main():
    if len(sys.argv) < 4:
        raise SystemExit("usage: render_engine_comparison.py <dxf> <layout-name> <out_dir> [dpi]")
    dxf_path, layout_name, out_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    dpi = int(sys.argv[4]) if len(sys.argv) > 4 else 150
    os.makedirs(out_dir, exist_ok=True)

    window = vp.main_viewport_window(dxf_path, layout_name)
    x0, y0, x1, y1 = window
    with open(os.path.join(out_dir, "00-window.txt"), "w") as handle:
        handle.write(f"layout: {layout_name}\nmodel window (m):\n"
                     f"  X: {x0:.4f} .. {x1:.4f}  ({x1 - x0:.3f} m)\n"
                     f"  Y: {y0:.4f} .. {y1:.4f}  ({y1 - y0:.3f} m)\n")
    print(f"window X[{x0:.2f},{x1:.2f}] Y[{y0:.2f},{y1:.2f}]  ({x1-x0:.2f} x {y1-y0:.2f} m)")

    # shared crop for the CAD engines (they have no window-plot flag)
    cropped = os.path.join(out_dir, "01-cropped-to-window.dxf")
    vp.crop_dxf(dxf_path, window, cropped, drop_span_factor=1.2)

    results = {}
    render_ezdxf(dxf_path, window, os.path.join(out_dir, "engine-ezdxf.png"), dpi)
    results["ezdxf"] = "ok -> engine-ezdxf.png"
    results["qcad"] = render_qcad(cropped, window,
                                  os.path.join(out_dir, "engine-qcad.pdf"),
                                  os.path.join(out_dir, "engine-qcad.png"))
    results["librecad"] = render_librecad(cropped, window,
                                          os.path.join(out_dir, "engine-librecad.pdf"),
                                          os.path.join(out_dir, "engine-librecad.png"))
    print("\nengine results:")
    for name, status in results.items():
        print(f"  {name:9s} {status}")


if __name__ == "__main__":
    main()
