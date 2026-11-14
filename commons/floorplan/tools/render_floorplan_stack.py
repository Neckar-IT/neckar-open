#!/usr/bin/env python3
"""Render a floor as a stack of transparent, pixel-aligned PNGs grouped by purpose.

This is the `furniture-ghost` look made controllable: instead of one baked image, each semantic group
is its own transparent PNG on an identical canvas, so they can be layered in any image editor and
toggled on/off individually.

Grouping rules:
  * Text is grouped by entity TYPE (TEXT / MTEXT / DIMENSION) from every layer, so annotations never
    pollute the geometry groups - whatever layer the architect put a label on, it lands in `text`.
  * Geometry is grouped by the architect's DXF layer: walls + door/window openings -> `structure`,
    breakthroughs/penetrations (`00_Durchbrüche`, the pipe/duct sleeves whose solid hatches read as
    black blocks) -> `penetrations`, `00_Möbel`/`00_Sanitär`/`00_Haustechnik` -> `furniture`,
    `00_Symbole` -> `symbols`, everything else -> `other`.
  * The extracted rooms (our analysis) -> `rooms`.

Each group is styled to match the furniture-ghost palette. The numeric filename prefix is the suggested
stacking order (low = back): rooms at the bottom, text on top.

Output per floor (all transparent, all same extent and pixel size):
  <name>.floor<N>.1-rooms.png         extracted rooms (translucent fill + names)
  <name>.floor<N>.2-other.png         every geometry layer not in a named group (light grey)
  <name>.floor<N>.3-penetrations.png  breakthroughs / pipe + duct sleeves (mid grey)
  <name>.floor<N>.4-furniture.png     furniture / sanitary / building services (ghost grey)
  <name>.floor<N>.5-symbols.png       symbols (mid grey)
  <name>.floor<N>.6-structure.png     walls + door/window openings (heavy black)
  <name>.floor<N>.7-text.png          all text + dimensions (dark grey)

Dependencies: pip install -r requirements.txt
Usage: python3 render_floorplan_stack.py <DXF> <ROOMS_JSON> <OUT_DIR> [FLOOR_INDEX]
"""
import os
import sys

from matplotlib.patches import Polygon as MplPolygon

import _floorplan_render as fr

# geometry groups: (filename key, set of DXF layers or None for "the rest", foreground colour, lineweight)
GEOMETRY_GROUPS = [
    ("6-structure", fr.WALL_LAYERS | fr.OPENING_LAYERS, "#101418", 3.5),
    ("5-symbols", fr.SYMBOL_LAYERS, "#8a99a3", 1.4),
    ("4-furniture", fr.FURNITURE_LAYERS, "#9aa7b0", 1.6),
    ("3-penetrations", fr.PENETRATION_LAYERS, "#5a6b73", 2.0),
    ("2-other", None, "#cdd6db", 1.0),
]
TEXT_COLOR = "#404a52"


def render_entities(document, entities, extent, color, lw_scaling, out_path):
    if not entities:
        return False
    figure, axis = fr.new_figure(extent)
    fr.draw(axis, document, entities, fr.mpl_config(fg=color, lw_scaling=lw_scaling))
    figure.savefig(out_path, transparent=True)
    fr.plt.close(figure)
    return True


def render_rooms(floor, extent, out_path):
    figure, axis = fr.new_figure(extent)
    calibration = floor["calibration"]
    for room in floor["rooms"]:
        points = fr.room_points(room, calibration)
        axis.add_patch(MplPolygon(points, closed=True, facecolor="#4a90d9", alpha=0.28,
                                  edgecolor="#1a4a80", linewidth=1.5))
        center_x, center_y = fr.room_centroid(points)
        axis.text(center_x, center_y, room["name"], ha="center", va="center",
                  fontsize=8, color="#0a2a4a", weight="bold")
    figure.savefig(out_path, transparent=True)
    fr.plt.close(figure)


def main():
    if len(sys.argv) < 4:
        raise SystemExit("usage: render_floorplan_stack.py <DXF> <ROOMS_JSON> <OUT_DIR> [FLOOR_INDEX]")
    dxf_path, rooms_json_path, out_dir = sys.argv[1], sys.argv[2], sys.argv[3]
    floor_index = int(sys.argv[4]) if len(sys.argv) > 4 else 2
    os.makedirs(out_dir, exist_ok=True)

    document, floor_entities, extent, floor = fr.load_floor(dxf_path, rooms_json_path, floor_index)
    name = os.path.splitext(os.path.basename(dxf_path))[0].split("-")[0]
    base = f"{name}.floor{floor_index}"
    print(f"{base}: {len(floor_entities)} entities, {len(floor['rooms'])} rooms")

    # text (any layer) goes to its own group; only the remaining geometry is grouped by layer.
    text_entities = [e for e in floor_entities if e.dxftype() in fr.TEXT_TYPES]
    geometry_entities = [e for e in floor_entities if e.dxftype() not in fr.TEXT_TYPES]

    grouped_layers = set()
    for _, layers, _, _ in GEOMETRY_GROUPS:
        if layers is not None:
            grouped_layers |= layers

    render_rooms(floor, extent, os.path.join(out_dir, f"{base}.1-rooms.png"))
    print("  1-rooms")

    for key, layers, color, lw_scaling in GEOMETRY_GROUPS:
        target_layers = layers
        if layers is None:  # "other" = every geometry layer not in a named group
            target_layers = {e.dxf.layer for e in geometry_entities} - grouped_layers
            if target_layers:
                print(f"  {key}: {', '.join(sorted(target_layers))}")
        entities = [e for e in geometry_entities if e.dxf.layer in target_layers]
        if render_entities(document, entities, extent, color, lw_scaling,
                           os.path.join(out_dir, f"{base}.{key}.png")):
            print(f"  {key}")

    if render_entities(document, text_entities, extent, TEXT_COLOR, 1.2,
                       os.path.join(out_dir, f"{base}.7-text.png")):
        print("  7-text")


if __name__ == "__main__":
    main()
