#!/usr/bin/env python3
"""Breadth experiment: render ONE floor of ONE plan in many visual styles / tools.

Goal: produce many candidate renderings of the same floor so the "nicest" display style can be chosen
later. Each output file is named by its variant/strategy. Two rendering tools are exercised: the ezdxf
drawing add-on -> Matplotlib backend (raster PNG) and the ezdxf native SVG backend (vector SVG).

Dependencies: pip install -r requirements.txt
Usage: python3 explore_floorplan_styles.py <DXF> <ROOMS_JSON> <OUT_DIR> [FLOOR_INDEX]
"""
import os
import sys

from matplotlib.patches import Polygon as MplPolygon
from matplotlib.colors import Normalize

from ezdxf.addons.drawing import RenderContext, Frontend, layout, svg
from ezdxf.addons.drawing.config import ColorPolicy, LineweightPolicy, BackgroundPolicy

import _floorplan_render as fr
from _floorplan_render import plt, mpl_config as config, draw, new_figure, room_points, room_centroid

# A categorical palette for "one colour per room" strategies.
PALETTE = [
    "#e57373", "#64b5f6", "#81c784", "#ffb74d", "#ba68c8", "#4db6ac",
    "#f06292", "#90a4ae", "#a1887f", "#9575cd", "#4fc3f7", "#aed581",
    "#ff8a65", "#7986cb", "#dce775", "#4dd0e1",
]


def save(figure, out_dir, base, variant, facecolor=None):
    figure.savefig(os.path.join(out_dir, f"{base}.{variant}.png"), facecolor=facecolor)
    plt.close(figure)
    print(f"  {variant}")


# ---- plain-plan styles (no room overlay) -----------------------------------

def style_mono_technical(document, floor_entities, extent, out_dir, base):
    fig, ax = new_figure(extent)
    draw(ax, document, floor_entities, config(fg="#111111", lw_scaling=2.0))
    save(fig, out_dir, base, "mono-technical", facecolor="white")


def style_dxf_colors(document, floor_entities, extent, out_dir, base):
    fig, ax = new_figure(extent)
    draw(ax, document, floor_entities, config(color_policy=ColorPolicy.COLOR, lw_scaling=2.0))
    save(fig, out_dir, base, "dxf-original-colors", facecolor="white")


def style_blueprint(document, floor_entities, extent, out_dir, base):
    bg = "#0b3d91"
    fig, ax = new_figure(extent, facecolor=bg)
    draw(ax, document, floor_entities, config(fg="#dce6ff", bg=BackgroundPolicy.CUSTOM, bg_color=bg, lw_scaling=1.8))
    save(fig, out_dir, base, "blueprint", facecolor=bg)


def style_dark(document, floor_entities, extent, out_dir, base):
    bg = "#161a1d"
    fig, ax = new_figure(extent, facecolor=bg)
    draw(ax, document, floor_entities, config(fg="#e6e6e6", bg=BackgroundPolicy.CUSTOM, bg_color=bg, lw_scaling=2.0))
    save(fig, out_dir, base, "dark", facecolor=bg)


def style_sepia(document, floor_entities, extent, out_dir, base):
    bg = "#f4ecd8"
    fig, ax = new_figure(extent, facecolor=bg)
    draw(ax, document, floor_entities, config(fg="#5b4636", bg=BackgroundPolicy.CUSTOM, bg_color=bg, lw_scaling=2.2))
    save(fig, out_dir, base, "sepia", facecolor=bg)


def style_hairline(document, floor_entities, extent, out_dir, base):
    fig, ax = new_figure(extent)
    draw(ax, document, floor_entities, config(fg="#222222", lw_policy=LineweightPolicy.ABSOLUTE, lw_scaling=0.35))
    save(fig, out_dir, base, "hairline", facecolor="white")


def style_sketch(document, floor_entities, extent, out_dir, base):
    with plt.xkcd(scale=0.6, length=80, randomness=2):
        fig, ax = new_figure(extent)
        draw(ax, document, floor_entities, config(fg="#1a1a1a", lw_scaling=1.6))
        save(fig, out_dir, base, "sketch-xkcd", facecolor="white")


def _layer_subset(floor_entities, layers):
    return [e for e in floor_entities if e.dxf.layer in layers]


def style_heavy_structure(document, floor_entities, extent, out_dir, base):
    """Faint everything, then walls + openings heavy black on top."""
    fig, ax = new_figure(extent)
    draw(ax, document, floor_entities, config(fg="#cfcfcf", lw_scaling=1.0))
    structure = _layer_subset(floor_entities, fr.WALL_LAYERS | fr.OPENING_LAYERS)
    draw(ax, document, structure, config(fg="#000000", lw_scaling=4.5))
    save(fig, out_dir, base, "heavy-structure", facecolor="white")


def style_walls_openings(document, floor_entities, extent, out_dir, base):
    """Structural skeleton only: walls + openings, nothing else."""
    fig, ax = new_figure(extent)
    structure = _layer_subset(floor_entities, fr.WALL_LAYERS | fr.OPENING_LAYERS)
    draw(ax, document, structure, config(fg="#000000", lw_scaling=3.5))
    save(fig, out_dir, base, "walls-openings", facecolor="white")


def style_furniture_ghost(document, floor_entities, extent, out_dir, base):
    fig, ax = new_figure(extent)
    furniture = _layer_subset(floor_entities, fr.FURNITURE_LAYERS)
    draw(ax, document, furniture, config(fg="#b8c4cc", lw_scaling=1.4))
    structure = _layer_subset(floor_entities, fr.WALL_LAYERS | fr.OPENING_LAYERS)
    draw(ax, document, structure, config(fg="#101418", lw_scaling=3.5))
    save(fig, out_dir, base, "furniture-ghost", facecolor="white")


# ---- room-thematic styles (rooms.json overlay) -----------------------------

def _faint_plan(document, floor_entities, extent, fg="#d3d8dd", facecolor="white"):
    fig, ax = new_figure(extent, facecolor=facecolor)
    draw(ax, document, floor_entities, config(fg=fg, lw_scaling=1.4))
    return fig, ax


def style_rooms_overlay(document, floor_entities, extent, out_dir, base, floor):
    fig, ax = new_figure(extent)
    draw(ax, document, floor_entities, config(fg="#1a1a1a", lw_scaling=2.0))
    cal = floor["calibration"]
    for room in floor["rooms"]:
        pts = room_points(room, cal)
        ax.add_patch(MplPolygon(pts, closed=True, facecolor="#4a90d9", alpha=0.28, edgecolor="#1a4a80", linewidth=1.5))
        cx, cy = room_centroid(pts)
        ax.text(cx, cy, room["name"], ha="center", va="center", fontsize=8, color="#0a2a4a", weight="bold")
    save(fig, out_dir, base, "rooms-overlay", facecolor="white")


def style_rooms_palette(document, floor_entities, extent, out_dir, base, floor):
    fig, ax = _faint_plan(document, floor_entities, extent)
    cal = floor["calibration"]
    for i, room in enumerate(floor["rooms"]):
        pts = room_points(room, cal)
        ax.add_patch(MplPolygon(pts, closed=True, facecolor=PALETTE[i % len(PALETTE)], alpha=0.55, edgecolor="#37474f", linewidth=1.2))
        cx, cy = room_centroid(pts)
        ax.text(cx, cy, room["name"], ha="center", va="center", fontsize=8, color="#1c2833", weight="bold")
    save(fig, out_dir, base, "rooms-palette", facecolor="white")


def style_rooms_heatmap(document, floor_entities, extent, out_dir, base, floor):
    fig, ax = _faint_plan(document, floor_entities, extent)
    cal = floor["calibration"]
    areas = [r.get("statedAreaSquareMeters") or 0.0 for r in floor["rooms"]]
    norm = Normalize(vmin=min(areas), vmax=max(areas) if max(areas) else 1.0)
    cmap = plt.get_cmap("viridis")
    for room, area in zip(floor["rooms"], areas):
        pts = room_points(room, cal)
        ax.add_patch(MplPolygon(pts, closed=True, facecolor=cmap(norm(area)), alpha=0.65, edgecolor="#263238", linewidth=1.0))
        cx, cy = room_centroid(pts)
        label = f"{room['name']}\n{area:.1f} m²" if area else room["name"]
        ax.text(cx, cy, label, ha="center", va="center", fontsize=7, color="#0d0d0d", weight="bold")
    save(fig, out_dir, base, "rooms-heatmap-area", facecolor="white")


def style_rooms_blueprint(document, floor_entities, extent, out_dir, base, floor):
    bg = "#0b3d91"
    fig, ax = new_figure(extent, facecolor=bg)
    draw(ax, document, floor_entities, config(fg="#cdd9ff", bg=BackgroundPolicy.CUSTOM, bg_color=bg, lw_scaling=1.6))
    cal = floor["calibration"]
    for room in floor["rooms"]:
        pts = room_points(room, cal)
        ax.add_patch(MplPolygon(pts, closed=True, facecolor="#ffffff", alpha=0.12, edgecolor="#ffffff", linewidth=1.0))
        cx, cy = room_centroid(pts)
        ax.text(cx, cy, room["name"], ha="center", va="center", fontsize=8, color="#ffffff", weight="bold")
    save(fig, out_dir, base, "rooms-blueprint", facecolor=bg)


def style_rooms_only(document, floor_entities, extent, out_dir, base, floor):
    """Schematic: filled room polygons + names, no walls at all."""
    fig, ax = new_figure(extent)
    cal = floor["calibration"]
    for i, room in enumerate(floor["rooms"]):
        pts = room_points(room, cal)
        ax.add_patch(MplPolygon(pts, closed=True, facecolor=PALETTE[i % len(PALETTE)], alpha=0.8, edgecolor="#ffffff", linewidth=2.5))
        cx, cy = room_centroid(pts)
        area = room.get("statedAreaSquareMeters")
        label = f"{room['name']}\n{area:.1f} m²" if area else room["name"]
        ax.text(cx, cy, label, ha="center", va="center", fontsize=8, color="#102027", weight="bold")
    save(fig, out_dir, base, "rooms-only-schematic", facecolor="white")


# ---- native SVG backend (different tool) -----------------------------------

def style_svg_native(document, floor_entities, out_dir, base, variant, color_policy, fg):
    backend = svg.SVGBackend()
    cfg = config(fg=fg, color_policy=color_policy, bg=BackgroundPolicy.CUSTOM, bg_color="#ffffff", lw_scaling=1.0)
    Frontend(RenderContext(document), backend, config=cfg).draw_entities(floor_entities)
    page = layout.Page(0, 0, layout.Units.mm, margins=layout.Margins.all(5))
    with open(os.path.join(out_dir, f"{base}.{variant}.svg"), "w", encoding="utf-8") as handle:
        handle.write(backend.get_string(page))
    print(f"  {variant} (svg)")


def main():
    if len(sys.argv) < 4:
        raise SystemExit("usage: explore_floorplan_styles.py <DXF> <ROOMS_JSON> <OUT_DIR> [FLOOR_INDEX]")
    dxf_path, rooms_json_path, out_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    floor_index = int(sys.argv[4]) if len(sys.argv) > 4 else 2
    os.makedirs(out_dir, exist_ok=True)

    document, floor_entities, extent, floor = fr.load_floor(dxf_path, rooms_json_path, floor_index)
    name = os.path.splitext(os.path.basename(dxf_path))[0].split("-")[0]
    base = f"{name}.floor{floor_index}"
    print(f"{base}: {len(floor_entities)} entities, {len(floor['rooms'])} rooms")

    style_mono_technical(document, floor_entities, extent, out_dir, base)
    style_dxf_colors(document, floor_entities, extent, out_dir, base)
    style_blueprint(document, floor_entities, extent, out_dir, base)
    style_dark(document, floor_entities, extent, out_dir, base)
    style_sepia(document, floor_entities, extent, out_dir, base)
    style_hairline(document, floor_entities, extent, out_dir, base)
    style_sketch(document, floor_entities, extent, out_dir, base)
    style_heavy_structure(document, floor_entities, extent, out_dir, base)
    style_walls_openings(document, floor_entities, extent, out_dir, base)
    style_furniture_ghost(document, floor_entities, extent, out_dir, base)

    style_rooms_overlay(document, floor_entities, extent, out_dir, base, floor)
    style_rooms_palette(document, floor_entities, extent, out_dir, base, floor)
    style_rooms_heatmap(document, floor_entities, extent, out_dir, base, floor)
    style_rooms_blueprint(document, floor_entities, extent, out_dir, base, floor)
    style_rooms_only(document, floor_entities, extent, out_dir, base, floor)

    style_svg_native(document, floor_entities, out_dir, base, "svg-native-color", ColorPolicy.COLOR, "#111111")
    style_svg_native(document, floor_entities, out_dir, base, "svg-native-mono", ColorPolicy.CUSTOM, "#111111")


if __name__ == "__main__":
    main()
