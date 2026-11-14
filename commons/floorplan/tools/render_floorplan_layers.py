#!/usr/bin/env python3
"""Render stackable, pixel-aligned floor-plan layers from each DXF via ezdxf.

One transparent PNG per *original DXF layer* (no invented grouping - the architect's own layer
structure is the layer structure), plus an extra `rooms` layer (our extraction) and a combined `mono`.
All PNGs of a floor share the exact same extent and pixel size, so a viewer can stack and toggle them.

For purpose-grouped, furniture-ghost-styled stackable layers use `render_floorplan_stack.py` instead;
this tool keeps every original DXF layer separate for maximum control.

Output (into the git-ignored `renders/` subfolder), per house and floor:
  <name>.floor<N>.layer-<dxf-layer>.png   one per DXF layer present on that floor   (transparent)
  <name>.floor<N>.rooms.png               the extracted rooms (blue) + names         (transparent)
  <name>.floor<N>.mono.png                everything, black on white                 (opaque)

Style: black, monochrome, using the architect's own DXF lineweights (RELATIVE_FIXED).

Dependencies: pip install -r requirements.txt
Usage: python3 render_floorplan_layers.py <CAD_DIR> [NAME_FILTER]

CAD_DIR holds the `<name>.dxf` files plus the `<name>.rooms.json` sidecars the room extractor
produces.
"""
import json
import os
import sys

from matplotlib.patches import Polygon as MplPolygon

import _floorplan_render as fr
from _floorplan_render import plt

LINE_COLOR = "#1a1a1a"
LINEWEIGHT_SCALING = 2.0
CONFIG = fr.mpl_config(fg=LINE_COLOR, lw_scaling=LINEWEIGHT_SCALING)


def draw_rooms(axis, floor):
    calibration = floor["calibration"]
    for room in floor["rooms"]:
        points = fr.room_points(room, calibration)
        axis.add_patch(MplPolygon(points, closed=True, facecolor="#4a90d9", alpha=0.30,
                                  edgecolor="#1a4a80", linewidth=1.5))
        center_x, center_y = fr.room_centroid(points)
        axis.text(center_x, center_y, room["name"], ha="center", va="center",
                  fontsize=8, color="#0a2a4a", weight="bold")


def render_house(dxf_path, rooms_json_path, renders_dir, base_name):
    floors = json.load(open(rooms_json_path, encoding="utf-8"))["floors"]

    for index, floor in enumerate(floors, start=1):
        document, floor_entities, extent, _ = fr.load_floor(dxf_path, rooms_json_path, index)

        # one transparent PNG per original DXF layer present on this floor
        layers = sorted({e.dxf.layer for e in floor_entities})
        for layer in layers:
            entities = [e for e in floor_entities if e.dxf.layer == layer]
            figure, axis = fr.new_figure(extent)
            fr.draw(axis, document, entities, CONFIG)
            figure.savefig(os.path.join(renders_dir, f"{base_name}.floor{index}.layer-{fr.sanitize(layer)}.png"), transparent=True)
            plt.close(figure)

        # extra: our extracted rooms
        figure, axis = fr.new_figure(extent)
        draw_rooms(axis, floor)
        figure.savefig(os.path.join(renders_dir, f"{base_name}.floor{index}.rooms.png"), transparent=True)
        plt.close(figure)

        # combined mono
        figure, axis = fr.new_figure(extent, facecolor="white")
        fr.draw(axis, document, floor_entities, CONFIG)
        figure.savefig(os.path.join(renders_dir, f"{base_name}.floor{index}.mono.png"), facecolor="white")
        plt.close(figure)

        print(f"  {base_name} floor{index}: {len(layers)} layers, {len(floor['rooms'])} rooms")


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: render_floorplan_layers.py <CAD_DIR> [NAME_FILTER]")
    cad_dir = sys.argv[1]
    name_filter = sys.argv[2] if len(sys.argv) > 2 else ""

    renders_dir = os.path.join(cad_dir, "renders")
    os.makedirs(renders_dir, exist_ok=True)

    dxf_files = sorted(name for name in os.listdir(cad_dir)
                       if name.lower().endswith(".dxf") and name_filter in name)
    if not dxf_files:
        raise SystemExit(f"no matching DXF files in {cad_dir}")

    for dxf_name in dxf_files:
        base_name = os.path.splitext(dxf_name)[0]
        rooms_json_path = os.path.join(cad_dir, base_name + ".rooms.json")
        if not os.path.exists(rooms_json_path):
            print(f"skip {dxf_name}: no {base_name}.rooms.json")
            continue
        print(f"{dxf_name}:")
        render_house(os.path.join(cad_dir, dxf_name), rooms_json_path, renders_dir, base_name)


if __name__ == "__main__":
    main()
