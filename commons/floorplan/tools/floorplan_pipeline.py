#!/usr/bin/env python3
"""Floor-plan pipeline: one DWG/DXF in -> per-floor room coordinates, styled renders and layer PNGs.

Input
  A single .dwg or .dxf. A .dwg is converted to .dxf headlessly via the ODA File Converter. Rooms are
  then detected by running the Kotlin FloorPlanRoomExtractor (fat jar) on the DXF, producing the floor +
  room source. A committed sibling `<name>.rooms.json` (example data) is used directly instead. If no
  rooms are detected (a plan without the room-outline template), floors fall back to the paper-space
  layout viewports (no room polygons).

Output tree (per input)
  <out>/
    manifest.json                         index of floors, styles and every file produced
    <floor>/
      coordinates.json                    room polygons in metre + millimetre + pixel, with the transform
      background.png                      per floor with rooms: every layer in black on white, no annotation (import result)
      full/<style>.png                    complete floor render, one per style (pixel-matches coordinates)
      full/<style>.pdf                    vector render, one per style        (with --pdf)
      layers/<style>/NN-<layer>.png       one transparent, pixel-aligned PNG per layer per style (--layers)

Units
  Model space is metres (DXF $INSUNITS = 6). coordinates.json therefore gives every room polygon three
  ways: metre (model), millimetre (metre*1000) and pixel (mapped into that floor's PNGs). The pixel
  transform is:  px_x = (X - window.minX) * px_per_metre ;  px_y = (window.maxY - Y) * px_per_metre.

Usage
  floorplan_pipeline.py <input.dwg|.dxf> <out_dir>
      [--styles a,b,c] [--px-per-meter 200] [--margin-m 1.0] [--no-pdf] [--no-layers]
"""
import argparse
import contextlib
import json
import math
import multiprocessing
import os
import re
import shutil
import subprocess
import sys
import tempfile
import threading
import time
from concurrent.futures import FIRST_COMPLETED, ProcessPoolExecutor, wait
from concurrent.futures import process as futures_process
from concurrent.futures.process import BrokenProcessPool
from fnmatch import fnmatch

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

import ezdxf
from ezdxf import bbox
from ezdxf.math import Matrix44
from ezdxf.addons.drawing import RenderContext, Frontend
from ezdxf.addons.drawing.matplotlib import MatplotlibBackend
from ezdxf.addons.drawing.config import (
    Configuration, BackgroundPolicy, ColorPolicy, LineweightPolicy,
)

BACKGROUND_PNG = "background.png"  # per floor with rooms: every layer in black on white, without annotation
_RENDER_DPI = 100  # figsize is set in inches = px / _RENDER_DPI, so savefig hits an exact pixel size
_MAX_LONG_PX = 6000  # cap a floor's long side so a huge model-space extent can't explode the render
INITIAL_VISIBLE_ROWS = 30  # a floor with more rows opens with its rows of the highest entity_count switched on
# Each render worker holds its own copy of the drawing and peaks near 0.5 GB on a 7 MB DXF;
# two workers bound a render to about 1 GB.
RENDER_WORKERS = 2

import _ezdxf_font_guard  # noqa: F401  # font-metric divide-by-zero guard (import for side effect)
from floorplan_styles import LAYER_VARIANTS, ANNOTATION_TYPES, DEFAULT_PRESET, TEAL, LIGHT_GRAY, BLACK
import viewport_pipeline as vp


def load_preset(input_path, cli_preset):
    """Preset resolution order: --preset path, a sibling floorplan-preset.json, else the built-in default."""
    for candidate in (cli_preset, os.path.join(os.path.dirname(os.path.abspath(input_path)), "floorplan-preset.json")):
        if candidate and os.path.exists(candidate):
            return json.load(open(candidate, encoding="utf-8"))
    return DEFAULT_PRESET


def resolve_layer_init(preset, layer, variants):
    """Initial matrix state for a layer from the preset: {'color': <col|''>, 'textoff': bool}."""
    rule = preset.get("default", {"color": "original", "text_off": False})
    for candidate in preset.get("rules", []):
        patterns = candidate["match"] if isinstance(candidate["match"], list) else [candidate["match"]]
        if any(fnmatch(layer, pattern) for pattern in patterns):
            rule = candidate
            break
    if rule.get("off"):
        return {"color": "", "textoff": False}
    color = rule.get("color", "original")
    if color not in variants:            # requested colour not rendered for this layer -> show it plainly
        color = "original" if "original" in variants else (variants[0] if variants else "")
    return {"color": color, "textoff": bool(rule.get("text_off", False))}


# ----------------------------------------------------------------------------- ingest (DWG -> DXF)

_ODA_TIMEOUT_SECONDS = 300
# The webapp stores 500 characters of the job error; the tail leaves room for the message in front of it.
_STDERR_TAIL_LINES = 5
_STDERR_TAIL_CHARS = 400


def _stderr_tail(stderr, workdir):
    """Return `": <last non-empty stderr lines>"` for a job error, or `""` when the converter wrote none.

    Paths below `workdir` are reported relative to it, so the job error names no temporary directory.
    """
    stderr = stderr or ""
    if isinstance(stderr, bytes):  # TimeoutExpired carries the raw bytes even in text mode
        stderr = stderr.decode(errors="replace")
    stderr = stderr.replace(workdir + os.sep, "").replace(workdir, ".")
    lines = [line.strip() for line in stderr.splitlines() if line.strip()]
    if not lines:
        return ""
    return ": " + " | ".join(lines[-_STDERR_TAIL_LINES:])[-_STDERR_TAIL_CHARS:]


def ingest_to_dxf(input_path, workdir):
    """Return a path to a DXF: the input itself if already DXF, else an ODA-converted copy."""
    if input_path.lower().endswith(".dxf"):
        return input_path
    if not input_path.lower().endswith(".dwg"):
        raise SystemExit(f"unsupported input {input_path!r} (need .dwg or .dxf)")
    oda = shutil.which("ODAFileConverter")
    if not oda:
        raise SystemExit("DWG input needs ODAFileConverter on PATH (tools/oda-file-converter/install.sh)")
    in_dir = os.path.join(workdir, "oda-in")
    out_dir = os.path.join(workdir, "oda-out")
    os.makedirs(in_dir, exist_ok=True)
    os.makedirs(out_dir, exist_ok=True)
    shutil.copy(input_path, os.path.join(in_dir, "input.dwg"))
    # Recurse=0, Audit=1: the audit pass repairs structural defects during conversion — without it,
    # ODA emits a truncated DXF (missing ENDSEC) for e.g. elemental-quinta-monroy-arq.dwg.
    cmd = [oda, in_dir, out_dir, "ACAD2018", "DXF", "0", "1", "*.dwg"]
    if shutil.which("xvfb-run"):
        cmd = ["xvfb-run", "-a"] + cmd  # ODA's bundled Qt needs an X display
    failure = "DWG-Konvertierung fehlgeschlagen (ODA File Converter)"
    try:
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
                       text=True, errors="replace", timeout=_ODA_TIMEOUT_SECONDS)
    except subprocess.CalledProcessError as error:
        raise SystemExit(f"{failure}: Exit-Code {error.returncode}{_stderr_tail(error.stderr, workdir)}")
    except subprocess.TimeoutExpired as error:
        raise SystemExit(f"{failure}: Zeitüberschreitung nach {_ODA_TIMEOUT_SECONDS} s"
                         f"{_stderr_tail(error.stderr, workdir)}")
    except Exception as error:
        raise SystemExit(f"{failure}: {error}")
    dxf = os.path.join(out_dir, "input.dxf")
    if not os.path.exists(dxf):
        raise SystemExit("DWG-Konvertierung ergab keine DXF-Datei (Datei evtl. beschädigt oder nicht unterstützt)")
    return dxf


# ----------------------------------------------------------------------------- room extraction

_EXTRACTOR_JAR = os.environ.get("FLOORPLAN_EXTRACTOR_JAR", "")


def extract_rooms(dxf_path, out_json_path):
    """Run the Kotlin FloorPlanRoomExtractor on the DXF, writing the `<name>.rooms.json` sidecar.

    This is the pipeline's room-detection step: the same single-source extractor (it.neckar.floorplan)
    that generates the committed example sidecars, packaged as a fat jar so it runs without Gradle.
    $FLOORPLAN_EXTRACTOR_JAR points at that jar; the product owning the extractor builds it.

    Returns the sidecar path on success (even with zero rooms — a valid result for a plan that does not
    follow the room-outline template), or None if Java or the extractor jar is unavailable or the extraction fails.
    """
    java = shutil.which("java")
    if not java or not _EXTRACTOR_JAR or not os.path.exists(_EXTRACTOR_JAR):
        return None
    try:
        subprocess.run([java, "-jar", _EXTRACTOR_JAR, dxf_path, out_json_path], check=True,
                       stdout=subprocess.DEVNULL, timeout=120)
    except Exception as error:  # jar crash / malformed DXF / timeout -> the caller decides the fallback
        print(f"  room extraction failed ({error})")
        return None
    return out_json_path if os.path.exists(out_json_path) else None


_BACKGROUND_CLI_CLASS = "it.neckar.elektromeister.floorplan.dxf.FloorBackgroundCliKt"
_BACKGROUND_TIMEOUT_SECONDS = 120


def render_backgrounds(dxf_path, request, workdir):
    """Draws background.png of every floor of the FloorBackgroundRequest `request` with FloorBackgroundCli of the jar.

    Returns the FloorBackgroundResult; None if Java or the jar is unavailable or the call fails.
    """
    java = shutil.which("java")
    if not java or not _EXTRACTOR_JAR or not os.path.exists(_EXTRACTOR_JAR):
        print("  background renderer unavailable (no java or no $FLOORPLAN_EXTRACTOR_JAR)")
        return None
    request_path = os.path.join(workdir, "background-request.json")
    result_path = os.path.join(workdir, "background-result.json")
    with open(request_path, "w", encoding="utf-8") as handle:
        json.dump(request, handle)
    try:
        subprocess.run([java, "-cp", _EXTRACTOR_JAR, _BACKGROUND_CLI_CLASS, dxf_path, request_path, result_path],
                       check=True, stdout=subprocess.DEVNULL, timeout=_BACKGROUND_TIMEOUT_SECONDS)
        with open(result_path, encoding="utf-8") as handle:
            return json.load(handle)
    except Exception as error:  # jar crash / unreadable DXF / timeout -> no floor gets a background
        print(f"  background rendering failed ({error})")
        return None


def background_request_floor(floor, png_path):
    """One floor of the FloorBackgroundRequest of `render_backgrounds`, from a floor scaled to metres."""
    x0, y0, x1, y1 = floor["bbox_m"]
    return {"png": png_path, "boundsMeters": {"minX": x0, "minY": y0, "maxX": x1, "maxY": y1},
            "roomsMeters": [[{"x": x, "y": y} for x, y in room["polygon_u"]] for room in floor["rooms"]]}


# ----------------------------------------------------------------------------- floors + rooms

# DXF $INSUNITS -> metres per drawing unit (the common codes; extend if a drawing uses an exotic unit).
_INSUNITS_TO_M = {1: 0.0254, 2: 0.3048, 4: 0.001, 5: 0.01, 6: 1.0, 7: 1000.0, 8: 0.0000000254,
                  9: 0.0000254, 10: 0.9144, 14: 0.1}
LEADER_MARGIN_M = 0.15  # real-world leader tolerance; converted to drawing units per document
ENTITY_PAD_M = 0.5  # an entity whose bbox reaches this far beyond a floor's window is drawn into it


def meters_per_unit(doc):
    """Metres per drawing unit from $INSUNITS. Returns (metres_per_unit, unit_name, is_known)."""
    code = doc.header.get("$INSUNITS", 0)
    if code in _INSUNITS_TO_M:
        names = {1: "inch", 2: "foot", 4: "millimeter", 5: "centimeter", 6: "meter", 7: "kilometer",
                 8: "microinch", 9: "mil", 10: "yard", 14: "decimeter"}
        return _INSUNITS_TO_M[code], names[code], True
    return 1.0, "unitless (assumed meter)", False  # unknown -> assume metre, but flag it


_METRIC_METERS_PER_UNIT = (0.001, 0.01, 0.1, 1.0)  # mm, cm, dm, m


def _polygon_area(points):
    return abs(sum(x0 * y1 - x1 * y0 for (x0, y0), (x1, y1) in zip(points, points[1:] + points[:1]))) / 2


def meters_per_unit_from_room_areas(floors):
    """Metres per drawing unit from the m² each room label states against its outline, snapped to a metric unit.

    Metric only: a German plan states sloped-ceiling rooms at less than their outline, which would pull a metre drawing to the
    yard (0.9144 m), 9 % below the metre.

    None without a room that states an area; from three rooms on, the median keeps one mislabelled room from moving the unit.
    """
    ratios = sorted(room["area_m2"] / _polygon_area(room["polygon_u"]) for floor in floors for room in floor["rooms"]
                    if room["area_m2"] and _polygon_area(room["polygon_u"]) > 0)
    if not ratios:
        return None
    measured = math.sqrt(ratios[len(ratios) // 2])
    return min(_METRIC_METERS_PER_UNIT, key=lambda unit: abs(math.log(unit / measured)))


def room_abs_polygon(vertices, calibration):
    """Room vertices (rooms.json local frame) -> absolute model-space metres."""
    scale = calibration["scale"]
    x0, y1 = calibration["dxfMinX"], calibration["dxfMaxY"]
    return [(v["x"] / scale + x0, y1 - v["y"] / scale) for v in vertices]


def floors_from_rooms_json(rooms_json_path):
    """Authoritative floors: bbox + named room polygons (absolute metres)."""
    data = json.load(open(rooms_json_path, encoding="utf-8"))
    floors = []
    for i, floor in enumerate(data["floors"], 1):
        cal = floor["calibration"]
        rooms = [
            {
                "name": room["name"],
                "area_m2": room.get("statedAreaSquareMeters"),
                "polygon_u": room_abs_polygon(room["bounds"]["vertices"], cal),  # drawing units
            }
            for room in floor.get("rooms", [])
        ]
        floors.append({
            "label": floor.get("label") or f"floor-{i}",
            "bbox_m": (cal["dxfMinX"], cal["dxfMinY"], cal["dxfMaxX"], cal["dxfMaxY"]),
            "rooms": rooms,
        })
    return floors


def floors_from_viewports(doc):
    """Fallback floors from paper-space layout viewports (no room polygons); uses the open doc (no re-read).

    One floor per content viewport, not per layout: a single sheet often frames several model-space
    windows (floor plans, elevations, sections, site plan), and which of them is "the floor plan" is
    not decidable here — picking e.g. the largest viewport selects the elevations strip on
    autodesk-sample-building.dwg. Emitting every window keeps the choice with the user; floors whose
    window frames no geometry are dropped downstream anyway.
    """
    floors = []
    try:
        layout_names = doc.layouts.names_in_taborder()
    except Exception:
        return floors
    for name in layout_names:
        if name.lower() == "model":
            continue
        try:
            layout_obj = doc.layouts.get(name)
            content = [v for v in layout_obj.query("VIEWPORT") if not vp._is_overview_viewport(v)]
        except Exception:
            continue
        for index, viewport in enumerate(content, start=1):
            try:
                x0, y0, x1, y1 = vp._viewport_wcs_window(viewport)
            except Exception:
                continue  # a malformed viewport is skipped, not fatal
            label = name if len(content) == 1 else f"{name} vp{index}"
            floors.append({"label": label, "bbox_m": (x0, y0, x1, y1), "rooms": []})
    return floors


def floor_from_modelspace(doc):
    """Last-resort single 'floor' = the whole model-space extents (any drawing renders, no room polygons)."""
    try:
        extents = bbox.extents(doc.modelspace(), fast=True)
    except Exception as error:
        raise SystemExit(f"Modellraum konnte nicht vermessen werden: {error}")
    if not extents.has_data:
        raise SystemExit("Keine darstellbare Geometrie gefunden (Modellraum ist leer)")
    return [{"label": "Model", "bbox_m": (extents.extmin.x, extents.extmin.y,
                                          extents.extmax.x, extents.extmax.y), "rooms": []}]


# ----------------------------------------------------------------------------- rendering

def slug(text):
    table = str.maketrans({"ä": "ae", "ö": "oe", "ü": "ue", "Ä": "Ae", "Ö": "Oe", "Ü": "Ue", "ß": "ss"})
    return re.sub(r"[^A-Za-z0-9_-]+", "-", text.translate(table)).strip("-")


def entities_in_window(doc, window, pad, cache=None):
    """Model-space entities whose bbox intersects the window (+pad, drawing units); pre-filter.

    Pass a shared bbox.Cache so an INSERT's (expensive) block bounding box is computed once across all
    floors instead of once per floor — the dominant cost on block-heavy professional drawings.
    """
    x0, y0, x1, y1 = window
    if cache is None:
        cache = bbox.Cache()
    kept = []
    for entity in doc.modelspace():
        try:
            box = bbox.extents([entity], fast=True, cache=cache)
        except Exception:
            box = None
        if box is None or not box.has_data:
            continue
        if (box.extmax.x < x0 - pad or box.extmin.x > x1 + pad
                or box.extmax.y < y0 - pad or box.extmin.y > y1 + pad):
            continue
        kept.append(entity)
    return kept


def _config(color, lineweight_scaling):
    if color is None:  # original ACI colours; swap black<->white so white model-space lines show on white
        return Configuration(background_policy=BackgroundPolicy.OFF, color_policy=ColorPolicy.COLOR_SWAP_BW,
                             lineweight_policy=LineweightPolicy.RELATIVE_FIXED, lineweight_scaling=lineweight_scaling)
    return Configuration(background_policy=BackgroundPolicy.OFF, color_policy=ColorPolicy.CUSTOM,
                         custom_fg_color=color, lineweight_policy=LineweightPolicy.RELATIVE_FIXED,
                         lineweight_scaling=lineweight_scaling)


def _open_endpoints(entity):
    """Endpoints of an OPEN line/polyline (closed shapes -> [], they are never leaders)."""
    dxftype = entity.dxftype()
    try:
        if dxftype == "LINE":
            return [(entity.dxf.start.x, entity.dxf.start.y), (entity.dxf.end.x, entity.dxf.end.y)]
        if dxftype == "LWPOLYLINE" and not entity.closed:
            points = list(entity.get_points())
            if points:
                return [(points[0][0], points[0][1]), (points[-1][0], points[-1][1])]
        if dxftype == "POLYLINE" and not entity.is_closed:
            vertices = list(entity.vertices)
            if vertices:
                return [(vertices[0].dxf.location.x, vertices[0].dxf.location.y),
                        (vertices[-1].dxf.location.x, vertices[-1].dxf.location.y)]
    except (AttributeError, IndexError):
        return []
    return []


def text_bboxes(entities):
    """Bounding boxes of TEXT/MTEXT entities (used to detect leaders that point at them)."""
    cache = bbox.Cache()
    boxes = []
    for entity in entities:
        if entity.dxftype() in ("TEXT", "MTEXT"):
            try:
                box = bbox.extents([entity], fast=True, cache=cache)
            except Exception:
                box = None
            if box is not None and box.has_data:
                boxes.append((box.extmin.x, box.extmin.y, box.extmax.x, box.extmax.y))
    return boxes


def _is_orphan_leader(entity, boxes, margin):
    for x, y in _open_endpoints(entity):
        for a, b, c, d in boxes:
            if a - margin <= x <= c + margin and b - margin <= y <= d + margin:
                return True
    return False


def _add_layer_count(counts, layer, entity_count, annotation_count):
    total = counts.setdefault(layer, [0, 0])
    total[0] += entity_count
    total[1] += annotation_count


def _is_drawn_inside_block(entity):
    """False for what the frontend leaves out of a block reference: ATTDEF and the `invisible` flag."""
    return entity.dxftype() != "ATTDEF" and not entity.dxf.get("invisible", 0)


def _insert_layer_counts(doc, insert, block_counts, open_blocks):
    """`{layer: [entities, annotation entities]}` of what one INSERT draws; the INSERT itself counts nowhere.

    Layer `0` inside a block is the layer of the referencing INSERT, so the key `0` survives only for
    an INSERT that is itself on layer `0`: the INSERT one level up resolves it, and at the top level it
    is the row `0`.
    """
    counts = {}
    if insert.dxf.get("invisible", 0):
        return counts
    name = insert.dxf.name
    if doc.blocks.get(name) is None:  # the frontend raises on such an INSERT, so it belongs to no row
        return counts
    if name not in block_counts and name not in open_blocks:  # open_blocks ends a cyclic reference
        content = {}
        open_blocks.add(name)
        for entity in doc.blocks.get(name):
            if not _is_drawn_inside_block(entity):
                continue
            if entity.dxftype() == "INSERT":
                for layer, (entity_count, annotation_count) in _insert_layer_counts(
                        doc, entity, block_counts, open_blocks).items():
                    _add_layer_count(content, layer, entity_count, annotation_count)
            else:
                _add_layer_count(content, entity.dxf.layer, 1, int(entity.dxftype() in ANNOTATION_TYPES))
        open_blocks.discard(name)
        block_counts[name] = content
    repetitions = insert.mcount  # a MINSERT draws its block and its ATTRIBs once per grid cell
    for layer, (entity_count, annotation_count) in block_counts.get(name, {}).items():
        _add_layer_count(counts, layer, entity_count * repetitions, annotation_count * repetitions)
    for attrib in insert.attribs:
        if _is_drawn_inside_block(attrib) and not attrib.is_invisible:
            _add_layer_count(counts, attrib.dxf.layer, repetitions, repetitions)
    own_layer = insert.dxf.layer
    if own_layer != "0" and "0" in counts:
        entity_count, annotation_count = counts.pop("0")
        _add_layer_count(counts, own_layer, entity_count, annotation_count)
    return counts


def collect_layer_counts(doc, entities, block_counts=None):
    """Per entity `{layer: [entities, annotation entities]}` of what the frontend draws for it.

    An INSERT counts for the layers of its block content and its ATTRIBs, every other entity for its
    own layer. Pass a shared `block_counts` dict so a block definition is walked once across all floors.
    """
    if block_counts is None:
        block_counts = {}
    result = []
    for entity in entities:
        if entity.dxftype() == "INSERT":
            result.append(_insert_layer_counts(doc, entity, block_counts, set()))
        else:
            result.append({entity.dxf.layer: [1, int(entity.dxftype() in ANNOTATION_TYPES)]})
    return result


def sum_layer_counts(entity_counts, inserts_only=False):
    """`{layer: [entities, annotation entities]}` summed over the result of `collect_layer_counts`."""
    totals = {}
    for entity, counts in entity_counts:
        if inserts_only and entity.dxftype() != "INSERT":
            continue
        for layer, (entity_count, annotation_count) in counts.items():
            _add_layer_count(totals, layer, entity_count, annotation_count)
    return totals


def mark_initially_visible(matrix):
    """Sets `initially_visible` on every row; above INITIAL_VISIBLE_ROWS rows it is true for the rows of
    the highest `entity_count` alone, whatever the preset says about a row."""
    by_entity_count = sorted(matrix, key=lambda row: -row["entity_count"])
    visible = {row["name"] for row in by_entity_count[:INITIAL_VISIBLE_ROWS]}
    for row in matrix:
        row["initially_visible"] = row["name"] in visible


def layer_entities(entity_counts, layer, notext, leader_margin):
    """Entities that draw on one layer for a variant: `notext` drops annotation + the orphan leaders on it.

    `entity_counts` pairs each entity with its `collect_layer_counts` entry. An INSERT is kept when its
    block content draws on the layer; the row render hides the content of the other layers.

    Orphan-leader detection uses only THIS layer's own text boxes — a leader and its label are drawn on
    the same layer. Using global text boxes produced false positives: real geometry (a door opening)
    that merely sits near a text on another layer would be dropped. Per-layer text avoids that.
    """
    on_layer = [entity for entity, counts in entity_counts if layer in counts]
    if not notext:
        return on_layer
    boxes = text_bboxes(on_layer)  # text that lives on this layer only
    result = []
    for entity in on_layer:
        if entity.dxftype() in ANNOTATION_TYPES:
            continue
        if boxes and _is_orphan_leader(entity, boxes, leader_margin):
            continue
        result.append(entity)
    return result


def background_entities(doc, entities, block_counts, leader_margin):
    """The entities of the reference background: the text-free rows of every layer, as the layer matrix filters them."""
    entity_counts = list(zip(entities, collect_layer_counts(doc, entities, block_counts)))
    handles = set()
    for layer in sum_layer_counts(entity_counts):
        handles.update(_handles(layer_entities(entity_counts, layer, notext=True, leader_margin=leader_margin)))
    return [entity for entity in entities if entity.dxf.handle in handles]


def _strip_mtext_masks(doc):
    for mtext in doc.modelspace().query("MTEXT"):
        try:
            mtext.set_bg_color(None)
        except Exception:
            pass


def _ensure_text_styles(doc):
    """Register a default entry for every TEXT/MTEXT style missing from the STYLE table.

    ODA-converted drawings can reference styles that were dropped in conversion (observed:
    elemental-monterrey-arq.dwg) — ezdxf then raises DXFTableEntryError mid-render."""
    for entity in doc.modelspace().query("TEXT MTEXT"):
        name = entity.dxf.get_default("style")
        if name and name not in doc.styles:
            try:
                doc.styles.new(name)
            except Exception:
                pass


def read_document(dxf_path):
    """The drawing as every render draws it: MTEXT without background masks, every text style registered."""
    try:
        doc = ezdxf.readfile(dxf_path)
    except Exception as error:
        # ezdxf names the temporary path in English; the user reads the German sentence, the log the cause.
        print(f"  DXF unreadable: {error}")
        raise SystemExit("DXF konnte nicht gelesen werden (beschädigt oder kein gültiges DXF)")
    _strip_mtext_masks(doc)
    _ensure_text_styles(doc)
    return doc


def scale_to_meters(doc, source_mpu):
    """Scales the model space to metres; a mm drawing otherwise renders slowly with million-scale coordinates."""
    if source_mpu == 1.0:
        return
    scale = Matrix44.scale(source_mpu, source_mpu, source_mpu)
    for entity in list(doc.modelspace()):
        try:
            entity.transform(scale)
        except Exception:
            pass


def scale_floors_to_meters(doc, floors):
    """Scales the drawing and the bbox and room polygons of `floors` to metres; returns `meters_per_unit` of the source.

    A drawing without $INSUNITS takes its unit from the stated room areas, else it is assumed to be in metres.
    """
    source_mpu, source_unit, unit_known = meters_per_unit(doc)
    if not unit_known:
        derived = meters_per_unit_from_room_areas(floors)
        if derived is not None:
            source_mpu, source_unit, unit_known = derived, f"{derived} m per unit (from the stated room areas)", True
    scale_to_meters(doc, source_mpu)
    for floor in floors:
        floor["bbox_m"] = tuple(v * source_mpu for v in floor["bbox_m"])
        for room in floor["rooms"]:
            room["polygon_u"] = [(x * source_mpu, y * source_mpu) for x, y in room["polygon_u"]]
    return source_mpu, source_unit, unit_known


# ----------------------------------------------------------------------------- render workers

_worker_doc = None  # set once per worker process by _start_render_worker
_RENDER_WORKER_LOST = "Ein Render-Prozess wurde unerwartet beendet (vermutlich zu wenig Arbeitsspeicher)."
_MESSAGE_RENDERING = "Erzeuge Vorschaubilder …"


class RenderInterrupted(BaseException):
    """The interpreter shuts down and takes no more renders; the run is incomplete, not failed."""


def _exit_with_parent(parent_pid):
    # A worker outlives a killed parent and would keep its copy of the drawing in memory.
    while os.getppid() == parent_pid:
        time.sleep(1)
    os._exit(1)


def _start_render_worker(dxf_path, source_mpu, parent_pid):
    global _worker_doc
    threading.Thread(target=_exit_with_parent, args=(parent_pid,), daemon=True).start()
    _worker_doc = read_document(dxf_path)
    scale_to_meters(_worker_doc, source_mpu)


def _worker_entities(handles):
    """Reading the same file yields the same handles, so the parent passes handles instead of entities."""
    return [_worker_doc.entitydb[handle] for handle in handles]


def _render_row_task(window, handles, layer_dir, layer, notext, width_px, height_px):
    return emit_layer_variants(_worker_doc, window, _worker_entities(handles), layer_dir, layer, notext,
                               width_px, height_px)


def _render_preview_task(window, handles, png_path, width_px, height_px):
    render_preview(_worker_doc, window, _worker_entities(handles), png_path, width_px, height_px)


@contextlib.contextmanager
def render_pool(dxf_path, source_mpu):
    # spawn: _exit_with_parent needs this process as the parent (forkserver interposes its own), and a forked
    # child would inherit the locks held by gunicorn's threads.
    pool = ProcessPoolExecutor(max_workers=RENDER_WORKERS, mp_context=multiprocessing.get_context("spawn"),
                               initializer=_start_render_worker, initargs=(dxf_path, source_mpu, os.getpid()))
    try:
        yield pool
    except BaseException:
        pool.shutdown(cancel_futures=True)  # still runs the renders the workers and the call queue hold
        raise
    pool.shutdown()


def _interpreter_exits():
    return futures_process._global_shutdown  # set by the exit handler of concurrent.futures


def _submit(pool, task, *args):
    try:
        return pool.submit(task, *args)
    except BrokenProcessPool:
        raise SystemExit(_RENDER_WORKER_LOST)
    except Exception as error:
        if _interpreter_exits():
            raise RenderInterrupted(str(error))
        # The task may already be queued and run for a row the manifest would lack.
        raise SystemExit(f"Render-Prozess konnte nicht gestartet werden: {error}")


def _render_result(future):
    try:
        return future.result()
    except BrokenProcessPool:
        raise SystemExit(_RENDER_WORKER_LOST)


def await_renders(pending, progress):
    """Reports every finished render; the interpreter exit interrupts the wait, so render_pool cancels the rest."""
    remaining, done = set(pending), 0
    while remaining:
        finished, remaining = wait(remaining, timeout=1, return_when=FIRST_COMPLETED)
        if remaining and _interpreter_exits():
            raise RenderInterrupted("the interpreter exits while renders are queued")
        for _ in finished:
            done += 1
            progress(f"{_MESSAGE_RENDERING} {done} von {len(pending)}")


def _handles(entities):
    return [entity.dxf.handle for entity in entities]


def _hex_rgb(hex_color):
    h = hex_color.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def _normalize_png(path, width_px, height_px, transparent, background):
    """Force an exact pixel size (matplotlib rounds figsize*dpi by ~1px); anchor top-left so the
    model->pixel transform in coordinates.json stays exact."""
    from PIL import Image
    image = Image.open(path)
    if image.size == (width_px, height_px):
        return
    if transparent:
        canvas = Image.new("RGBA", (width_px, height_px), (0, 0, 0, 0))
        canvas.paste(image.convert("RGBA"), (0, 0))
    else:
        canvas = Image.new("RGB", (width_px, height_px), _hex_rgb(background or "#ffffff"))
        canvas.paste(image.convert("RGB"), (0, 0))
    canvas.save(path)


def _new_axes(window, width_px, height_px):
    """A figure whose data area is exactly the window and whose canvas is exactly width_px x height_px."""
    figure = plt.figure(figsize=(width_px / _RENDER_DPI, height_px / _RENDER_DPI), dpi=_RENDER_DPI)
    axis = figure.add_axes([0, 0, 1, 1])
    _apply_window(axis, window)
    return figure, axis


def _apply_window(axis, window):
    axis.set_xlim(window[0], window[2])
    axis.set_ylim(window[1], window[3])
    axis.set_aspect("equal")
    axis.axis("off")


class _LayerRowFrontend(Frontend):
    """Draws of a block reference the content of one layer; `notext` hides the annotation in it as well."""

    def __init__(self, ctx, out, config, layer, notext):
        super().__init__(ctx, out, config=config)
        self._row_layer = layer
        self._notext = notext
        self._composite_depth = 0

    def override_properties(self, entity, properties):
        if self._composite_depth or not self.ctx.inside_block_reference or entity.dxftype() == "INSERT":
            return
        if (properties.layer != self._row_layer
                or (self._notext and entity.dxftype() in ANNOTATION_TYPES)):
            properties.is_visible = False

    def draw_composite_entity(self, entity, properties):
        # The generated geometry of a DIMENSION or LEADER is on other layers and belongs to its row.
        is_insert = entity.dxftype() == "INSERT"
        self._composite_depth += 0 if is_insert else 1
        try:
            super().draw_composite_entity(entity, properties)
        finally:
            self._composite_depth -= 0 if is_insert else 1


class _NoAnnotationFrontend(Frontend):
    """Hides every annotation, inside block references as well; a hidden DIMENSION takes its generated geometry along."""

    def override_properties(self, entity, properties):
        if entity.dxftype() in ANNOTATION_TYPES:
            properties.is_visible = False


def _render_entities(doc, window, ents, color, png_path, width_px, height_px, background, layer=None, notext=False):
    """`layer` renders one matrix row; without it every entity is drawn, its annotation hidden with `notext`."""
    figure, axis = _new_axes(window, width_px, height_px)
    ctx, out, config = RenderContext(doc), MatplotlibBackend(axis), _config(color, 1.0)
    if layer is not None:
        frontend = _LayerRowFrontend(ctx, out, config, layer, notext)
    elif notext:
        frontend = _NoAnnotationFrontend(ctx, out, config=config)
    else:
        frontend = Frontend(ctx, out, config=config)
    transparent = background is None
    try:
        frontend.draw_entities(ents)
        _apply_window(axis, window)  # re-assert: draw_entities may autoscale
        figure.savefig(png_path, dpi=_RENDER_DPI, transparent=transparent, facecolor=background or "none")
    finally:
        plt.close(figure)  # a render worker draws many figures per run
    _normalize_png(png_path, width_px, height_px, transparent, background)


def render_layer_variant(doc, window, ents, color, png_path, width_px, height_px, layer, notext=False):
    """One layer's entities in `color` (None = original ACI), transparent background, exact pixel size."""
    _render_entities(doc, window, ents, color, png_path, width_px, height_px, background=None,
                     layer=layer, notext=notext)


def render_preview(doc, window, entities, png_path, width_px, height_px):
    """Quick static reference: every layer in original colours on white (all-layers view)."""
    _render_entities(doc, window, list(entities), None, png_path, width_px, height_px, background="#ffffff")


def render_background(doc, window, entities, png_path, width_px, height_px):
    """Every layer in black on white without annotation, the reference of the Kotlin FloorBackgroundGeometry.

    `entities` come from `background_entities`; floorplan.neckar.it compares both renderers in its task testImport.
    """
    _render_entities(doc, window, list(entities), BLACK, png_path, width_px, height_px, background="#ffffff", notext=True)


def _recolor(src_png, dst_png, hex_color):
    """Tint a single-ink transparent PNG to another colour (keeps the alpha / anti-aliasing).

    Colour variants of a layer are the SAME geometry — so the block-heavy matplotlib render is done once
    (in black) and the other single-ink colours are produced by this cheap pixel recolour instead of
    re-rendering (which on block-heavy drawings dominates the runtime).
    """
    from PIL import Image
    import numpy as np
    image = np.array(Image.open(src_png).convert("RGBA"))
    r, g, b = _hex_rgb(hex_color)
    image[..., 0], image[..., 1], image[..., 2] = r, g, b
    Image.fromarray(image).save(dst_png)


def emit_layer_variants(doc, window, ents, layer_dir, layer, notext, width_px, height_px):
    """All colour variants for one layer/notext-state: render 'original' + a black base, recolour the rest."""
    if not ents:
        return []
    suffix = "-notext" if notext else ""
    produced = []
    render_layer_variant(doc, window, ents, None, os.path.join(layer_dir, f"original{suffix}.png"),
                         width_px, height_px, layer, notext)
    produced.append(f"original{suffix}")
    black_path = os.path.join(layer_dir, f"black{suffix}.png")
    render_layer_variant(doc, window, ents, BLACK, black_path, width_px, height_px, layer, notext)
    produced.append(f"black{suffix}")
    for name, color in (("teal", TEAL), ("gray", LIGHT_GRAY)):
        _recolor(black_path, os.path.join(layer_dir, f"{name}{suffix}.png"), color)
        produced.append(f"{name}{suffix}")
    return produced


# ----------------------------------------------------------------------------- coordinates.json

def floor_frame(floor, margin, px_per_unit):
    """The render window of a floor (its bbox plus `margin`), the pixels per unit after the cap and the image size."""
    bx0, by0, bx1, by1 = floor["bbox_m"]
    window = (bx0 - margin, by0 - margin, bx1 + margin, by1 + margin)
    win_w, win_h = window[2] - window[0], window[3] - window[1]
    ppu = min(px_per_unit, _MAX_LONG_PX / max(win_w, win_h, 1e-9))  # cap huge extents
    return window, ppu, max(int(round(win_w * ppu)), 1), max(int(round(win_h * ppu)), 1)


def coordinates_json(floor, window, px_per_unit, mpu, source_unit, width_px, height_px):
    """`window` and room polygons are in drawing units; this converts to metre, millimetre and pixel.

    mpu = metres per drawing unit (from $INSUNITS), so nothing assumes the drawing is already in metres.
    """
    x0, y0, x1, y1 = window

    def to_px(p):
        return [round((p[0] - x0) * px_per_unit, 2), round((y1 - p[1]) * px_per_unit, 2)]

    def to_m(p):
        return [round(p[0] * mpu, 4), round(p[1] * mpu, 4)]

    def to_mm(p):
        return [round(p[0] * mpu * 1000, 1), round(p[1] * mpu * 1000, 1)]

    rooms = []
    for room in floor["rooms"]:
        poly_u = room["polygon_u"]
        rooms.append({
            "name": room["name"],
            "area_m2": room["area_m2"],
            "polygon_m": [to_m(p) for p in poly_u],
            "polygon_mm": [to_mm(p) for p in poly_u],
            "polygon_px": [to_px(p) for p in poly_u],
        })
    px_per_meter = px_per_unit / mpu
    return {
        "floor": floor["label"],
        "source_unit": source_unit,
        "unit_model": "meter",
        "floor_bbox_m": {"minX": round(floor["bbox_m"][0] * mpu, 4), "minY": round(floor["bbox_m"][1] * mpu, 4),
                         "maxX": round(floor["bbox_m"][2] * mpu, 4), "maxY": round(floor["bbox_m"][3] * mpu, 4)},
        "render_window_m": {"minX": round(x0 * mpu, 4), "minY": round(y0 * mpu, 4),
                            "maxX": round(x1 * mpu, 4), "maxY": round(y1 * mpu, 4)},
        "image": {"width_px": width_px, "height_px": height_px, "px_per_meter": round(px_per_meter, 4)},
        "transform_model_to_pixel": {
            "px_per_meter": round(px_per_meter, 4),
            "origin_model_top_left_m": [round(x0 * mpu, 4), round(y1 * mpu, 4)],
            "formula": "px_x = (X_m - minX_m) * px_per_meter ; px_y = (maxY_m - Y_m) * px_per_meter",
        },
        "rooms": rooms,
    }


# ----------------------------------------------------------------------------- rooms overlay + viewer

ROOM_PALETTE = [(31, 78, 95), (191, 87, 0), (58, 110, 74), (120, 64, 120),
                (150, 105, 20), (40, 90, 140), (150, 40, 60), (0, 120, 120)]


def _room_font(size_px):
    from PIL import ImageFont
    try:
        return ImageFont.truetype(os.path.join(matplotlib.get_data_path(), "fonts", "ttf", "DejaVuSans.ttf"), size_px)
    except Exception:
        return ImageFont.load_default()


def render_rooms_overlay(coords, path, width_px, height_px):
    """Transparent, pixel-aligned overlay: each room as a coloured, lightly-filled polygon + its name."""
    from PIL import Image, ImageDraw
    image = Image.new("RGBA", (width_px, height_px), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image, "RGBA")
    font = _room_font(max(height_px // 55, 14))
    outline_w = max(width_px // 500, 2)
    for index, room in enumerate(coords["rooms"]):
        points = [tuple(p) for p in room["polygon_px"]]
        if len(points) < 3:
            continue
        color = ROOM_PALETTE[index % len(ROOM_PALETTE)]
        draw.polygon(points, fill=color + (55,), outline=color + (255,), width=outline_w)
        cx = sum(p[0] for p in points) / len(points)
        cy = sum(p[1] for p in points) / len(points)
        for dx in (-2, 0, 2):
            for dy in (-2, 0, 2):
                draw.text((cx + dx, cy + dy), room["name"], font=font, fill=(255, 255, 255, 230), anchor="mm")
        draw.text((cx, cy), room["name"], font=font, fill=color + (255,), anchor="mm")
    image.save(path)


def write_viewer_html(path, floor_label, layer_matrix, base_colors, rooms_overlay, width_px, height_px,
                      preset_global=None):
    """Self-contained MATRIX: rows = layers, columns = colours (radio) + a per-row 'Text aus' checkbox.

    A row picks one colour cell (or 'aus' to hide the layer); ticking 'Text aus' switches that row to
    the colour's '-notext' render (text elements hidden, geometry kept). Column headers set a whole
    column at once. A row with `initially_visible` false opens switched off, every other row in the
    preset's initial state. The text field filters the rows by layer name. 'Preset exportieren' downloads
    the current state as a reusable preset JSON. Everything composes live and stays pixel-aligned.
    """
    data = json.dumps({"layers": layer_matrix, "colors": base_colors, "rooms": rooms_overlay,
                       "global": preset_global or {}, "w": width_px, "h": height_px}, ensure_ascii=False)
    html = """<!doctype html><html lang="de"><meta charset="utf-8">
<title>__TITLE__ — Layer-Matrix</title>
<style>
 body{margin:0;font:13px/1.4 system-ui,sans-serif;display:flex;height:100vh;color:#222}
 #panel{width:400px;flex:none;overflow:auto;padding:14px;border-right:1px solid #ddd;background:#fafafa}
 #stageWrap{flex:1;overflow:auto;display:flex;align-items:center;justify-content:center;
   background:repeating-conic-gradient(#eee 0% 25%,#fff 0% 50%) 50%/24px 24px}
 #stage{position:relative;width:min(88vh*__ASPECT__,95%);aspect-ratio:__W__/__H__;box-shadow:0 2px 12px #0003}
 #stage img{position:absolute;inset:0;width:100%;height:100%}
 h1{font-size:15px;margin:0 0 2px} .muted{color:#888;font-size:12px;margin:0 0 10px}
 table{border-collapse:collapse;width:100%;font-size:12px}
 th,td{padding:3px 4px;text-align:center;border-bottom:1px solid #eee}
 th.lyr,td.lyr{text-align:left;max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
 thead th{position:sticky;top:0;background:#fafafa;cursor:pointer;user-select:none}
 thead th.lyr{cursor:default} thead th:hover:not(.lyr){color:#1f4e5f;text-decoration:underline}
 .top{display:flex;gap:14px;margin:8px 0 12px}
 #filter{width:100%;box-sizing:border-box;margin:0 0 8px;padding:4px 6px;font:inherit}
</style>
<div id="panel">
 <h1>__TITLE__</h1><p class="muted">Matrix: pro Layer eine Farbe (Zeile) · Spaltenkopf = ganze Spalte · „Txt" = nur Text aus</p>
 <div class="top">
  <label><input type="checkbox" id="rooms" checked> ▣ Räume</label>
  <label><input type="checkbox" id="alltext"> alle „Text aus"</label>
  <button id="export">Preset exportieren</button>
 </div>
 <input type="search" id="filter" placeholder="Layer filtern">
 <table><thead><tr id="head"></tr></thead><tbody id="body"></tbody></table>
</div>
<div id="stageWrap"><div id="stage"></div></div>
<script>
const DATA=__DATA__, COLS=['',...DATA.colors], imgs={};
const stage=document.getElementById('stage');
function paint(L){
 const img=imgs[L.dir], c=L._color, t=L._text.checked;
 if(!c){img.style.display='none';return;}
 let v=c; if(t){ const nt=c+'-notext'; if(L.variants.includes(nt)) v=nt; else if(L.only_text){img.style.display='none';return;} }
 img.src=L.dir+'/'+v+'.png'; img.style.display='block';
}
// header: Layer | aus | <colors...> | Txt
const head=document.getElementById('head');
head.innerHTML='<th class="lyr">Layer</th>'+COLS.map(c=>`<th data-c="${c}">${c||'aus'}</th>`).join('')+'<th>Txt</th>';
const body=document.getElementById('body');
DATA.layers.forEach((L,i)=>{
 const img=new Image(); img.style.display='none'; stage.appendChild(img); imgs[L.dir]=img;
 const init=L.init||{color:'original',textoff:false}; L._color=L.initially_visible===false?'':init.color;
 const tr=document.createElement('tr');
 tr.innerHTML='<td class="lyr" title="'+L.name+'">'+L.name+'</td>'
  +COLS.map(c=>`<td><input type="radio" name="r${i}" value="${c}" ${c===L._color?'checked':''}></td>`).join('')
  +`<td><input type="checkbox" class="tx" ${init.textoff?'checked':''}></td>`;
 body.appendChild(tr);
 tr.querySelectorAll('input[type=radio]').forEach(r=>r.onchange=()=>{L._color=r.value;paint(L);});
 L._text=tr.querySelector('.tx'); L._text.onchange=()=>paint(L);
 paint(L);
});
document.getElementById('filter').oninput=e=>{ const q=e.target.value.toLowerCase();
 body.querySelectorAll('tr').forEach((tr,i)=>{ tr.hidden=!DATA.layers[i].name.toLowerCase().includes(q); }); };
// clickable column headers set the whole column
head.querySelectorAll('th[data-c]').forEach(th=>th.onclick=()=>{
 const c=th.dataset.c;
 DATA.layers.forEach((L,i)=>{ const r=body.querySelectorAll('tr')[i].querySelector(`input[value="${c}"]`); if(r){r.checked=true;} L._color=c; paint(L); });
});
document.getElementById('alltext').onchange=e=>{ DATA.layers.forEach(L=>{L._text.checked=e.target.checked;paint(L);}); };
const roomsCb=document.getElementById('rooms'); roomsCb.checked=DATA.global.rooms!==false;
if(DATA.rooms){ const r=new Image(); r.src=DATA.rooms; r.style.zIndex=999; r.style.display=roomsCb.checked?'block':'none'; stage.appendChild(r);
 roomsCb.onchange=e=>r.style.display=e.target.checked?'block':'none'; }
// export the current matrix as a reusable preset JSON (one exact-name rule per layer)
document.getElementById('export').onclick=()=>{
 const rules=DATA.layers.map(L=>L._color?{match:[L.name],color:L._color,text_off:L._text.checked}:{match:[L.name],off:true});
 const preset={global:{rooms:roomsCb.checked},default:{color:'gray',text_off:true},rules};
 const a=document.createElement('a');
 a.href='data:application/json;charset=utf-8,'+encodeURIComponent(JSON.stringify(preset,null,2));
 a.download='floorplan-preset.json'; a.click();
};
</script></html>"""
    aspect = width_px / height_px
    html = (html.replace("__DATA__", data).replace("__TITLE__", floor_label)
            .replace("__ASPECT__", f"{aspect:.4f}").replace("__W__", str(width_px)).replace("__H__", str(height_px)))
    with open(path, "w", encoding="utf-8") as handle:
        handle.write(html)


# ----------------------------------------------------------------------------- driver

def _ignore_progress(message):
    pass


def process(input_path, out_dir, px_per_m, margin_m, want_preview, cli_preset=None, progress=_ignore_progress):
    """`progress` receives a German status line for the user at each step and after each finished render."""
    os.makedirs(out_dir, exist_ok=True)
    preset = load_preset(input_path, cli_preset)
    with tempfile.TemporaryDirectory() as workdir:
        progress("Lese Zeichnung …")
        dxf_path = ingest_to_dxf(input_path, workdir)
        doc = read_document(dxf_path)

        progress("Erkenne Räume …")
        # Room detection is a pipeline step. Precedence: a committed sibling sidecar next to the input
        # (example data) wins; otherwise run the Kotlin room extractor on the (converted) DXF. Only if
        # extraction finds no rooms (a plan without the room-outline template) or the extractor is
        # unavailable do floors fall back to the paper-space layout viewports (no room polygons). Any
        # failure in the room source is non-fatal — it degrades to the next fallback, never a crash.
        base = os.path.splitext(input_path)[0]
        sibling = base + ".rooms.json"
        floors, room_source = None, None
        room_extraction_failed = False  # floorplan_import.import_result tells it from a drawing without room outlines
        try:
            if os.path.exists(sibling):
                floors = floors_from_rooms_json(sibling)
                room_source = f"room-extraction sidecar ({os.path.basename(sibling)})"
            else:
                extracted = os.path.join(workdir, "extracted.rooms.json")
                if extract_rooms(dxf_path, extracted):
                    candidate = floors_from_rooms_json(extracted)
                    if any(floor["rooms"] for floor in candidate):
                        floors, room_source = candidate, "room extraction (FloorPlanRoomExtractor)"
                else:
                    room_extraction_failed = True
        except Exception as error:
            print(f"  room source unusable ({error}); falling back to viewports")
            floors, room_extraction_failed = None, True
        if not floors:
            floors = floors_from_viewports(doc)
            room_source = ("paper-space viewports (room extraction failed)" if room_extraction_failed
                           else "paper-space viewports (no rooms detected)")

        if not floors:   # no rooms.json and no usable layouts -> render the whole model space as one sheet
            floors = floor_from_modelspace(doc)
            room_source = "whole model space (no rooms.json / no layout viewports)"

        # Unit normalisation: read the drawing unit ($INSUNITS) and scale everything to METRES, as every
        # render worker does, so nothing downstream assumes a unit.
        source_mpu, source_unit, unit_known = scale_floors_to_meters(doc, floors)
        px_per_unit = px_per_m    # everything is metres now
        margin_u, pad_u, leader_margin_u, mpu = margin_m, ENTITY_PAD_M, LEADER_MARGIN_M, 1.0

        if not unit_known:
            print("  WARNING: $INSUNITS not set — assuming metre; room coordinates may be off otherwise.")

        cache = bbox.Cache()  # shared across floors: an INSERT's block bbox is computed once, not per floor

        # A floor whose window frames no geometry (e.g. a viewport heuristic that does not fit this
        # drawing) is useless — drop it, and if none survive, render the whole model space instead.
        def _floor_has_content(fl):
            b = fl["bbox_m"]
            return bool(entities_in_window(doc, (b[0] - margin_u, b[1] - margin_u, b[2] + margin_u, b[3] + margin_u), pad_u, cache))
        non_empty = [f for f in floors if _floor_has_content(f)]
        if not non_empty:
            floors = floor_from_modelspace(doc)
            room_source += "  ->  fell back to whole model space (no floor framed any geometry)"
        else:
            floors = non_empty

        block_counts = {}  # shared across floors: a block definition is walked once
        try:
            modelspace = list(doc.modelspace())
            all_layers = sorted(sum_layer_counts(zip(modelspace, collect_layer_counts(doc, modelspace, block_counts))))
        except Exception:
            all_layers = []
        manifest = {"input": os.path.basename(input_path), "room_source": room_source,
                    "source_unit": source_unit, "meters_per_unit": source_mpu, "unit_known": unit_known,
                    "px_per_meter": px_per_m, "margin_m": margin_m, "all_layers": all_layers,
                    "layer_variants": [v["name"] for v in LAYER_VARIANTS],
                    # floorplan_import.import_result tells a floor with rooms the render skipped from one never found
                    "room_floor_count": sum(1 for floor in floors if floor["rooms"]),
                    "room_extraction_failed": room_extraction_failed, "floors": []}

        # A floor, layer, overlay or preview that raises is skipped; a lost worker or a refused render ends the run.
        used_slugs = set()
        submitted_floors = []
        # The backgrounds of all floors with rooms are drawn by one Kotlin call once the render pool is shut down.
        background_request = {"metersPerUnit": source_mpu, "marginMeters": margin_m, "pixelsPerMeter": px_per_m,
                              "floors": []}
        progress(_MESSAGE_RENDERING)
        with render_pool(dxf_path, source_mpu) as pool:
            for i, floor in enumerate(floors, 1):
                try:
                    window, ppu, width_px, height_px = floor_frame(floor, margin_u, px_per_unit)

                    floor_slug = slug(floor.get("label") or f"floor-{i}") or f"floor-{i}"
                    if floor_slug in used_slugs:
                        floor_slug = f"{floor_slug}-{i}"
                    used_slugs.add(floor_slug)
                    floor_dir = os.path.join(out_dir, floor_slug)
                    os.makedirs(floor_dir, exist_ok=True)
                    entities = entities_in_window(doc, window, pad_u, cache)

                    coords = coordinates_json(floor, window, ppu, mpu, source_unit, width_px, height_px)
                    with open(os.path.join(floor_dir, "coordinates.json"), "w", encoding="utf-8") as handle:
                        json.dump(coords, handle, ensure_ascii=False, indent=2)

                    entity_counts = list(zip(entities, collect_layer_counts(doc, entities, block_counts)))
                    layer_counts = sum_layer_counts(entity_counts)
                    block_layer_counts = sum_layer_counts(entity_counts, inserts_only=True)
                    layers = sorted(layer_counts)
                    index_width = max(2, len(str(len(layers))))

                    # the matrix: each layer x each variant (colour, with/without text) -> a transparent PNG
                    rows = []
                    for index, layer in enumerate(layers, 1):
                        layer_slug = f"{index:0{index_width}d}-{slug(layer)}"
                        layer_dir = os.path.join(floor_dir, "layers", layer_slug)
                        try:
                            full_ents = layer_entities(entity_counts, layer, notext=False,
                                                       leader_margin=leader_margin_u)
                            clean_ents = layer_entities(entity_counts, layer, notext=True,
                                                        leader_margin=leader_margin_u)
                            block_entities, block_annotations = block_layer_counts.get(layer, (0, 0))
                            only_text = (len(full_ents) > 0 and block_entities == block_annotations
                                         and all(entity.dxftype() == "INSERT" for entity in clean_ents))
                            # a -notext variant only if it differs
                            has_clean = (len(clean_ents) < len(full_ents) or block_annotations > 0) and not only_text
                            os.makedirs(layer_dir, exist_ok=True)
                            renders = [_submit(pool, _render_row_task, window, _handles(full_ents), layer_dir,
                                               layer, False, width_px, height_px)]
                            if has_clean:
                                renders.append(_submit(pool, _render_row_task, window, _handles(clean_ents), layer_dir,
                                                       layer, True, width_px, height_px))
                        except Exception as error:
                            print(f"    layer {layer!r} failed ({error}); skipped")
                            continue
                        rows.append({"name": layer, "dir": f"layers/{layer_slug}", "renders": renders,
                                     "only_text": only_text, "entity_count": layer_counts[layer][0]})

                    rooms_overlay = None
                    if floor["rooms"]:
                        try:
                            render_rooms_overlay(coords, os.path.join(floor_dir, "rooms-overlay.png"),
                                                 width_px, height_px)
                            rooms_overlay = "rooms-overlay.png"
                        except Exception as error:
                            print(f"    rooms overlay failed ({error}); skipped")
                    preview = None
                    if want_preview:
                        preview = _submit(pool, _render_preview_task, window, _handles(entities),
                                          os.path.join(floor_dir, "preview.png"), width_px, height_px)
                    background = None
                    if floor["rooms"]:  # the import result of a floor: its rooms on this drawing, see floorplan_import.py
                        background = len(background_request["floors"])
                        background_request["floors"].append(
                            background_request_floor(floor, os.path.join(floor_dir, BACKGROUND_PNG)))
                    submitted_floors.append({"floor": floor, "slug": floor_slug, "floor_dir": floor_dir, "rows": rows,
                                             "preview": preview, "background": background, "rooms_overlay": rooms_overlay,
                                             "width_px": width_px, "height_px": height_px})
                except Exception as error:
                    print(f"  floor {floor.get('label', '?')!r} failed ({error}); skipped")
                    continue

            pending = [future for submitted in submitted_floors
                       for row in submitted["rows"] for future in row["renders"]]
            pending += [submitted["preview"] for submitted in submitted_floors if submitted["preview"] is not None]
            await_renders(pending, progress)

        background_result = None
        if background_request["floors"]:
            progress("Zeichne Hintergründe …")
            background_result = render_backgrounds(dxf_path, background_request, workdir)

        for submitted in submitted_floors:
            floor = submitted["floor"]
            try:
                matrix = []
                for row in submitted["rows"]:
                    try:
                        produced = [variant for future in row["renders"] for variant in _render_result(future)]
                    except Exception as error:
                        print(f"    layer {row['name']!r} failed ({error}); skipped")
                        continue
                    if produced:
                        matrix.append({"name": row["name"], "dir": row["dir"], "variants": produced,
                                       "only_text": row["only_text"], "entity_count": row["entity_count"],
                                       "init": resolve_layer_init(preset, row["name"], produced)})
                mark_initially_visible(matrix)

                if submitted["preview"] is not None:
                    try:
                        _render_result(submitted["preview"])
                    except Exception as error:
                        print(f"    preview failed ({error}); skipped")
                background = None
                if submitted["background"] is not None and background_result is not None:
                    drawn = background_result["floors"][submitted["background"]]
                    if drawn["error"] is not None:
                        print(f"    background failed ({drawn['error']}); skipped")
                    elif (drawn["widthPx"], drawn["heightPx"]) != (submitted["width_px"], submitted["height_px"]):
                        print(f"    background frame {drawn['widthPx']}x{drawn['heightPx']}px differs from the floor's; skipped")
                    else:
                        background = BACKGROUND_PNG
                base_colors = [v["name"] for v in LAYER_VARIANTS if not v["notext"]]
                try:
                    write_viewer_html(os.path.join(submitted["floor_dir"], "index.html"), floor["label"],
                                      matrix, base_colors, submitted["rooms_overlay"], submitted["width_px"],
                                      submitted["height_px"], preset.get("global", {}))
                except Exception:
                    pass

                manifest["floors"].append({
                    "label": floor.get("label") or submitted["slug"], "dir": submitted["slug"],
                    "image_size_px": [submitted["width_px"], submitted["height_px"]],
                    "room_count": len(floor["rooms"]),
                    "rooms_overlay": submitted["rooms_overlay"], "background": background, "viewer": "index.html",
                    "layers": matrix})
                print(f"  floor {floor.get('label')!r}: {submitted['width_px']}x{submitted['height_px']}px, "
                      f"{len(floor['rooms'])} rooms, {len(matrix)} layers x {len(LAYER_VARIANTS)} variants")
            except Exception as error:
                print(f"  floor {floor.get('label', '?')!r} failed ({error}); skipped")
                continue

        if not manifest["floors"]:
            raise SystemExit("Keine darstellbaren Grundrisse gefunden (kein Geschoss enthielt renderbare Geometrie)")

        with open(os.path.join(out_dir, "manifest.json"), "w", encoding="utf-8") as handle:
            json.dump(manifest, handle, ensure_ascii=False, indent=2)
    print(f"done -> {out_dir}")


def main():
    parser = argparse.ArgumentParser(description="Floor-plan pipeline (DWG/DXF -> coordinates + layer matrix)")
    parser.add_argument("input")
    parser.add_argument("out_dir")
    parser.add_argument("--px-per-meter", type=int, default=200)
    parser.add_argument("--margin-m", type=float, default=1.0)
    parser.add_argument("--preset", default=None, help="preset JSON (else sibling floorplan-preset.json, else built-in)")
    parser.add_argument("--no-preview", action="store_true")
    args = parser.parse_args()

    print(f"pipeline: {os.path.basename(args.input)} -> {args.out_dir}  "
          f"variants={[v['name'] for v in LAYER_VARIANTS]}")
    process(args.input, args.out_dir, args.px_per_meter, args.margin_m,
            want_preview=not args.no_preview, cli_preset=args.preset)


if __name__ == "__main__":
    main()
