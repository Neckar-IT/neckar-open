"""Shared helpers for the floor-plan renderers (layers / stack / style exploration).

All three tools drive ezdxf's drawing add-on and crop a single floor out of a multi-floor DXF model
space using the per-floor calibration from `<name>.rooms.json`. Importing this module also installs the
font-metric guard so layers carrying DXF text can be rasterized.

Figure size is fixed in inches-per-meter; DPI is the resolution knob. Output pixels per meter =
INCHES_PER_METER * DPI. Point-based fonts and line widths stay proportional at any DPI.
Resolution (output pixels per drawing-metre = INCHES_PER_METER * DPI) is overridable via the
FLOORPLAN_DPI environment variable, default 600 px/m.
"""
import json
import os
import re

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Polygon as MplPolygon

import ezdxf
from ezdxf.addons.drawing import RenderContext, Frontend
from ezdxf.addons.drawing.matplotlib import MatplotlibBackend
from ezdxf.addons.drawing.config import (
    Configuration, ColorPolicy, LineweightPolicy, BackgroundPolicy,
)

import _ezdxf_font_guard  # noqa: F401  # font-metric divide-by-zero guard (import for side effect)

DRAWABLE = "LINE LWPOLYLINE POLYLINE ARC CIRCLE ELLIPSE SPLINE SOLID HATCH INSERT TEXT MTEXT DIMENSION"
INCHES_PER_METER = 1.0
DPI = int(os.environ.get("FLOORPLAN_DPI", "600"))
MARGIN_FRACTION = 0.04
# Entities whose representative point is farther than this (× floor size) from the floor are off-view
# (other floors, elevations, the section) and are skipped - they fall outside the crop anyway.
KEEP_FRACTION = 0.55

# Semantic grouping of the architect's DXF layers, shared by the stack renderer and style exploration.
# Text is grouped by entity type (TEXT/MTEXT/DIMENSION), not by layer - see TEXT_TYPES below - so that
# annotations never pollute the geometry groups regardless of which layer the architect drew them on.
WALL_LAYERS = {"00_Wände", "00_Wände tragend", "00_Wände nichttragend"}
OPENING_LAYERS = {"00_Öffnung"}
# Breakthroughs / penetrations (pipe + duct sleeves): their own group - they carry solid hatches that
# would otherwise read as stray black blocks on the structure layer.
PENETRATION_LAYERS = {"00_Durchbrüche"}
FURNITURE_LAYERS = {"00_Möbel", "00_Sanitär", "00_Haustechnik"}
SYMBOL_LAYERS = {"00_Symbole"}

# Entity types that are annotation, not geometry. Collected into the text group from every layer.
TEXT_TYPES = {"TEXT", "MTEXT", "DIMENSION"}


def representative_point(entity):
    dxftype = entity.dxftype()
    try:
        if dxftype == "LINE":
            return entity.dxf.start.x, entity.dxf.start.y
        if dxftype in ("ARC", "CIRCLE", "ELLIPSE"):
            return entity.dxf.center.x, entity.dxf.center.y
        if dxftype in ("INSERT", "MTEXT", "TEXT"):
            point = entity.dxf.insert if dxftype == "INSERT" else entity.dxf.get("insert", None)
            return (point.x, point.y) if point is not None else None
        if dxftype == "LWPOLYLINE":
            first = entity[0]
            return first[0], first[1]
        if dxftype == "POLYLINE":
            vertex = next(iter(entity.vertices))
            return vertex.dxf.location.x, vertex.dxf.location.y
        if dxftype == "SOLID":
            return entity.dxf.vtx0.x, entity.dxf.vtx0.y
    except (AttributeError, IndexError, StopIteration, TypeError):
        return None
    return None


def floor_extent(calibration):
    width = calibration["dxfMaxX"] - calibration["dxfMinX"]
    height = calibration["dxfMaxY"] - calibration["dxfMinY"]
    return (calibration["dxfMinX"] - width * MARGIN_FRACTION, calibration["dxfMaxX"] + width * MARGIN_FRACTION,
            calibration["dxfMinY"] - height * MARGIN_FRACTION, calibration["dxfMaxY"] + height * MARGIN_FRACTION,
            width, height)


def in_floor(entity, extent):
    x0, x1, y0, y1, width, height = extent
    point = representative_point(entity)
    if point is None:
        return True
    return (x0 - width * KEEP_FRACTION <= point[0] <= x1 + width * KEEP_FRACTION and
            y0 - height * KEEP_FRACTION <= point[1] <= y1 + height * KEEP_FRACTION)


def new_figure(extent, facecolor=None):
    """A figure whose extent (and thus pixel size at the shared DPI) is identical for every layer."""
    x0, x1, y0, y1, _, _ = extent
    figure = plt.figure(figsize=((x1 - x0) * INCHES_PER_METER, (y1 - y0) * INCHES_PER_METER), dpi=DPI)
    axis = figure.add_axes([0, 0, 1, 1])
    axis.set_xlim(x0, x1)
    axis.set_ylim(y0, y1)
    axis.set_aspect("equal")
    axis.axis("off")
    if facecolor is not None:
        axis.set_facecolor(facecolor)
    return figure, axis


def mpl_config(fg=None, bg=BackgroundPolicy.WHITE, bg_color="#ffffff",
               color_policy=ColorPolicy.CUSTOM, lw_policy=LineweightPolicy.RELATIVE_FIXED, lw_scaling=2.0):
    kwargs = dict(
        color_policy=color_policy,
        background_policy=bg, custom_bg_color=bg_color,
        lineweight_policy=lw_policy, lineweight_scaling=lw_scaling,
    )
    if color_policy == ColorPolicy.CUSTOM:
        kwargs["custom_fg_color"] = fg
    return Configuration(**kwargs)


def draw(axis, document, entities, cfg):
    Frontend(RenderContext(document), MatplotlibBackend(axis), config=cfg).draw_entities(entities)


def room_points(room, calibration):
    scale = calibration["scale"]
    return [(v["x"] / scale + calibration["dxfMinX"], calibration["dxfMaxY"] - v["y"] / scale)
            for v in room["bounds"]["vertices"]]


def room_centroid(points):
    return sum(p[0] for p in points) / len(points), sum(p[1] for p in points) / len(points)


def sanitize(layer_name):
    table = str.maketrans({"ä": "ae", "ö": "oe", "ü": "ue", "Ä": "Ae", "Ö": "Oe", "Ü": "Ue", "ß": "ss", " ": "_"})
    return layer_name.translate(table)


# --- MTEXT inline-formatting cleanup (Python port of it.neckar.dxf.cleanMTextFormatting) ---
# The architect's MTEXT labels carry inline formatting, e.g. {\fBahnschrift Light|b0|i0;...\P...}.
# The font selector references a font not installed here; ezdxf then computes the paragraph line
# spacing of the \P breaks with broken metrics and the multi-line label explodes to span the whole
# floor. Stripping the formatting to plain text (as the Kotlin extractor does) makes ezdxf fall back
# to the style font with correct spacing. Keep this in sync with dxf/MTextFormatting.kt.
_PH_BACKSLASH, _PH_BRACE_OPEN, _PH_BRACE_CLOSE = "", "", ""
_NON_BREAKING_SPACE = " "
_FORMATTING_CODE_WITH_ARGUMENT = re.compile(r"\\[A-Za-z][^;\\}]*;")
_FORMATTING_CODE_BARE = re.compile(r"\\[A-Za-z]")


def clean_mtext_formatting(raw):
    text = (raw
            .replace("\\\\", _PH_BACKSLASH)
            .replace("\\{", _PH_BRACE_OPEN)
            .replace("\\}", _PH_BRACE_CLOSE))
    text = (text
            .replace("\\P", "\n")
            .replace("\\~", _NON_BREAKING_SPACE)
            .replace("^I", "\t"))
    text = _FORMATTING_CODE_WITH_ARGUMENT.sub("", text)
    text = text.replace("{", "").replace("}", "")
    text = _FORMATTING_CODE_BARE.sub("", text)
    text = (text
            .replace(_PH_BACKSLASH, "\\")
            .replace(_PH_BRACE_OPEN, "{")
            .replace(_PH_BRACE_CLOSE, "}"))
    return text.strip()


def strip_mtext_inline_formatting(document):
    """Replace every MTEXT's raw content with its cleaned plain text (see clean_mtext_formatting)."""
    for mtext in document.modelspace().query("MTEXT"):
        try:
            mtext.text = clean_mtext_formatting(mtext.text)
        except Exception:
            pass


def strip_mtext_background(document):
    """Remove MTEXT background-mask fills.

    Some annotations carry a background mask (a filled box behind the text, so the label stays readable
    over geometry). Under a forced monochrome colour the mask and the text collapse to the same colour,
    so the text vanishes inside a solid black box. We render text on its own layer, so the mask is not
    needed - drop it and the labels read as plain glyphs.
    """
    for mtext in document.modelspace().query("MTEXT"):
        try:
            mtext.set_bg_color(None)
        except Exception:
            pass


def load_floor(dxf_path, rooms_json_path, floor_index):
    """Returns (document, floor_entities, extent, floor) for the 1-based floor_index of a plan."""
    document = ezdxf.readfile(dxf_path)
    strip_mtext_inline_formatting(document)
    strip_mtext_background(document)
    all_entities = list(document.modelspace().query(DRAWABLE))
    floors = json.load(open(rooms_json_path, encoding="utf-8"))["floors"]
    floor = floors[floor_index - 1]
    extent = floor_extent(floor["calibration"])
    floor_entities = [e for e in all_entities if in_floor(e, extent)]
    return document, floor_entities, extent, floor
