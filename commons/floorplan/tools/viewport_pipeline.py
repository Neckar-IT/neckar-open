#!/usr/bin/env python3
"""Faithful per-viewport rendering pipeline — both variants, into one clean, step-named directory.

Given a DXF that still carries active paper-space layout viewports (e.g. one produced by re-converting
the architect DWG with ODA File Converter), this takes ONE layout's main viewport and produces a
faithful render of exactly the model-space window that viewport frames — in two ways, so they can be
compared:

  Variant A — crop, then render:  delete the model-space geometry outside the viewport window, then let
              QCAD render (auto-fit) what remains.
  Variant B — render, then crop:  render a slightly larger region faithfully (geometry left intact,
              nothing deleted inside), then crop the raster to exactly the viewport window.

Variant B stays deterministic without fighting QCAD's offset/scale: four corner markers are drawn at
the window(+margin) corners, so the ink bounding box of the render equals that rectangle in pixels;
the exact window is then a linear sub-rectangle of that.

Output directory (all files self-describing and ordered):
  00-source.dwg                                  the source DWG (copied, if given)
  01-converted-to-dxf.dxf                        the DXF with active viewports (copied input)
  02-viewport-window.txt                         the extracted viewport window (model units)
  crop-then-render.1-cropped-to-window.dxf       model space cropped to the window
  crop-then-render.2-faithful-render.png         faithful QCAD render of the crop
  render-then-crop.1-region-render.png           faithful render of window+margin (corner-marked)
  render-then-crop.2-cropped-to-window.png       the region render cropped to exactly the window

QCAD is located via QCAD_DWG2PDF or `dwg2pdf` on PATH; pdftoppm rasterizes the PDFs; Pillow crops.

Usage: python3 viewport_pipeline.py <converted.dxf> <layout-name> <out_dir> [margin_m]
"""
import os
import shutil
import subprocess
import sys

import ezdxf
from ezdxf import bbox
from ezdxf.math import Vec3
from PIL import Image

MARKER_LAYER = "_pipeline_frame"
RASTER_DPI = 400
PAPER_LONG_MM = 900   # long side of the (content-aspect) paper; with RASTER_DPI -> ~14000 px long side
TRIM_PAD_PX = 12


def _is_overview_viewport(vp):
    """True for the paper-space overview viewport (it maps the paper 1:1 onto itself, not model space).

    It is NOT identified by `id` == 1: an ODA File Converter round-trip leaves every viewport `id` at 0.
    The reliable signature is the 1:1 mapping — view_center_point == paper center and view_height ==
    paper height — which no content viewport has.
    """
    d = vp.dxf
    return (abs(d.view_center_point[0] - d.center[0]) < 1e-3 and
            abs(d.view_center_point[1] - d.center[1]) < 1e-3 and
            abs(d.view_height - d.height) < 1e-3)


def _viewport_wcs_window(viewport):
    """The model-space (WCS) rectangle a paper-space viewport frames — the exact crop.

    Uses ezdxf's own viewport transformation (get_transformation_matrix, model->paperspace) so the
    view_target offset and view_twist_angle are honoured — a plain view_center_point +- view_height
    box is wrong for any twisted or off-target viewport. The 3D top-view matrix is singular in z, so it
    cannot be inverted directly; we recover its 2D affine (paper = A*model + t) from three basis points,
    invert the 2x2, and map the viewport's paper-space clip corners back to WCS.
    """
    matrix = viewport.get_transformation_matrix()
    origin = matrix.transform(Vec3(0, 0, 0))
    unit_x = matrix.transform(Vec3(1, 0, 0))
    unit_y = matrix.transform(Vec3(0, 1, 0))
    a11, a21 = unit_x.x - origin.x, unit_x.y - origin.y
    a12, a22 = unit_y.x - origin.x, unit_y.y - origin.y
    determinant = a11 * a22 - a12 * a21

    def paper_to_model(point):
        bx, by = point.x - origin.x, point.y - origin.y
        return ((a22 * bx - a12 * by) / determinant, (-a21 * bx + a11 * by) / determinant)

    model_corners = [paper_to_model(corner) for corner in viewport.clipping_rect_corners()]
    xs = [x for x, _ in model_corners]
    ys = [y for _, y in model_corners]
    return (min(xs), min(ys), max(xs), max(ys))


def main_viewport_window(dxf_path, layout_name):
    doc = ezdxf.readfile(dxf_path)
    layout = doc.layouts.get(layout_name)
    content = [v for v in layout.query("VIEWPORT") if not _is_overview_viewport(v)]
    if not content:
        raise SystemExit(f"layout {layout_name!r} has no content viewports")
    main = max(content, key=lambda v: v.dxf.get("width", 0) * v.dxf.get("height", 0))
    return _viewport_wcs_window(main)


def crop_dxf(src_dxf, window, out_dxf, drop_span_factor, corner_markers=False):
    """Keep entities whose bbox intersects `window` (dropping layout-spanning strays); save to out_dxf."""
    x0, y0, x1, y1 = window
    span_w, span_h = (x1 - x0) * drop_span_factor, (y1 - y0) * drop_span_factor
    doc = ezdxf.readfile(src_dxf)
    msp = doc.modelspace()
    cache = bbox.Cache()
    to_delete = []
    for e in msp:
        try:
            b = bbox.extents([e], fast=True, cache=cache)
        except Exception:
            b = None
        if b is None or not b.has_data:
            to_delete.append(e)
            continue
        outside = b.extmax.x < x0 or b.extmin.x > x1 or b.extmax.y < y0 or b.extmin.y > y1
        spanning = (b.extmax.x - b.extmin.x) > span_w or (b.extmax.y - b.extmin.y) > span_h
        if outside or spanning:
            to_delete.append(e)
    for e in to_delete:
        msp.delete_entity(e)
    if corner_markers:
        doc.layers.add(MARKER_LAYER) if MARKER_LAYER not in doc.layers else None
        d = min(x1 - x0, y1 - y0) * 0.01  # small tick
        for (mx, my) in [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]:
            msp.add_line((mx - d, my), (mx + d, my), dxfattribs={"layer": MARKER_LAYER})
            msp.add_line((mx, my - d), (mx, my + d), dxfattribs={"layer": MARKER_LAYER})
    doc.saveas(out_dxf)


def content_aspect_paper(dxf):
    """Paper sized to the drawing's own aspect ratio, so QCAD auto-fit fills it (no wasted margins)."""
    doc = ezdxf.readfile(dxf)
    b = bbox.extents(doc.modelspace(), fast=True)
    w, h = b.extmax.x - b.extmin.x, b.extmax.y - b.extmin.y
    if w >= h:
        return f"{PAPER_LONG_MM:.0f}x{max(PAPER_LONG_MM * h / w, 1):.0f}"
    return f"{max(PAPER_LONG_MM * w / h, 1):.0f}x{PAPER_LONG_MM:.0f}"


def qcad_render_png(qcad, dxf, pdf, png):
    subprocess.run([qcad, "-platform", "offscreen", "-f", "-a", "-c", "-p", content_aspect_paper(dxf),
                    "-w", "0.2", "-o", pdf, dxf], check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.run(["pdftoppm", "-png", "-r", str(RASTER_DPI), "-singlefile", pdf, png[:-4]],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def trim_to_ink(png, pad=TRIM_PAD_PX):
    """Crop the PNG tightly to its non-white content (plus a small padding)."""
    from PIL import ImageChops
    im = Image.open(png).convert("RGB")
    diff = ImageChops.difference(im, Image.new("RGB", im.size, (255, 255, 255))).convert("L")
    box = diff.getbbox()
    if box:
        x0, y0, x1, y1 = box
        im.crop((max(0, x0 - pad), max(0, y0 - pad),
                 min(im.width, x1 + pad), min(im.height, y1 + pad))).save(png)


def ink_bbox(png):
    im = Image.open(png).convert("L")
    w, h = im.size
    px = im.load()
    xs0 = ys0 = 10 ** 9
    xs1 = ys1 = -1
    for y in range(h):
        for x in range(w):
            if px[x, y] < 200:
                if x < xs0: xs0 = x
                if x > xs1: xs1 = x
                if y < ys0: ys0 = y
                if y > ys1: ys1 = y
    return xs0, ys0, xs1, ys1


def crop_to_window(region_png, region_window, target_window, out_png):
    """region_png shows region_window (its ink bbox = the marker corners); crop to target_window."""
    rx0, ry0, rx1, ry1 = region_window
    tx0, ty0, tx1, ty1 = target_window
    ix0, iy0, ix1, iy1 = ink_bbox(region_png)
    sx = (ix1 - ix0) / (rx1 - rx0)
    sy = (iy1 - iy0) / (ry1 - ry0)
    # model x grows right -> pixel x grows right; model y grows up -> pixel y grows DOWN
    px0 = ix0 + (tx0 - rx0) * sx
    px1 = ix0 + (tx1 - rx0) * sx
    py0 = iy1 - (ty1 - ry0) * sy
    py1 = iy1 - (ty0 - ry0) * sy
    Image.open(region_png).crop((round(px0), round(py0), round(px1), round(py1))).save(out_png)


def main():
    if len(sys.argv) < 4:
        raise SystemExit("usage: viewport_pipeline.py <converted.dxf> <layout-name> <out_dir> [margin_m]")
    src_dxf, layout_name, out_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    margin = float(sys.argv[4]) if len(sys.argv) > 4 else 4.0
    os.makedirs(out_dir, exist_ok=True)
    qcad = os.environ.get("QCAD_DWG2PDF") or shutil.which("dwg2pdf")
    if not qcad:
        raise SystemExit("QCAD not found (set QCAD_DWG2PDF or put dwg2pdf on PATH)")

    shutil.copy(src_dxf, os.path.join(out_dir, "01-converted-to-dxf.dxf"))
    window = main_viewport_window(src_dxf, layout_name)
    x0, y0, x1, y1 = window
    with open(os.path.join(out_dir, "02-viewport-window.txt"), "w") as f:
        f.write(f"layout: {layout_name}\nmain viewport window (model units):\n"
                f"  x: {x0:.3f} .. {x1:.3f}  ({x1 - x0:.2f} m)\n  y: {y0:.3f} .. {y1:.3f}  ({y1 - y0:.2f} m)\n")
    print(f"02 window: x[{x0:.1f},{x1:.1f}] y[{y0:.1f},{y1:.1f}]")

    # Variant "crop then render"
    cropped = os.path.join(out_dir, "crop-then-render.1-cropped-to-window.dxf")
    crop_dxf(src_dxf, window, cropped, drop_span_factor=1.2)
    ctr_png = os.path.join(out_dir, "crop-then-render.2-faithful-render.png")
    qcad_render_png(qcad, cropped, os.path.join(out_dir, "crop-then-render.2-faithful-render.pdf"), ctr_png)
    trim_to_ink(ctr_png)
    print("crop-then-render done")

    # Variant "render then crop" (render window + margin with corner markers, then crop to window)
    region = (x0 - margin, y0 - margin, x1 + margin, y1 + margin)
    region_dxf = os.path.join(out_dir, "render-then-crop.0-region.dxf")
    crop_dxf(src_dxf, region, region_dxf, drop_span_factor=1.2, corner_markers=True)
    region_png = os.path.join(out_dir, "render-then-crop.1-region-render.png")
    qcad_render_png(qcad, region_dxf, os.path.join(out_dir, "render-then-crop.1-region-render.pdf"), region_png)
    rtc_png = os.path.join(out_dir, "render-then-crop.2-cropped-to-window.png")
    crop_to_window(region_png, region, window, rtc_png)   # calibrated off the raw (untrimmed) markers
    trim_to_ink(rtc_png)
    trim_to_ink(region_png)
    print("render-then-crop done")


if __name__ == "__main__":
    main()
