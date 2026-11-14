#!/usr/bin/env python3
"""Import result of a floorplan_pipeline.process run: per floor with rooms, the rooms in the pixels of its background.

process() renders background.png per floor with rooms in the frame of that floor's coordinates.json, so a room
polygon lies on the drawing it was found in. floorplan.neckar.it serves the result of a job as /j/<job>/import.json.

Result (JSON):
  {"floors": [{"label": "Geschoss 1",
               "rooms": [{"name": "Wohnen", "statedAreaSquareMeters": 24.3,
                          "bounds": {"vertices": [{"x": 200.0, "y": 200.0}, ...]}}],
               "background": "data:image/png;base64,..."}]}
"""
import base64
import json
import os


class ImportResultMissing(Exception):
    """The run in an out dir yields no complete import result."""


class ImportResultFromEarlierVersion(ImportResultMissing):
    """The run predates background.png; a new run of the same drawing yields the result."""


class ImportResultIncomplete(ImportResultMissing):
    """A floor with rooms failed to render; the container log (stdout) of the run names the cause."""


class RoomExtractionFailed(ImportResultMissing):
    """The room extractor was unavailable or failed, so the run cannot tell which floors have rooms."""


def _imported_room(room):
    return {
        "name": room["name"],
        "statedAreaSquareMeters": room["area_m2"],
        "bounds": {"vertices": [{"x": x, "y": y} for x, y in room["polygon_px"]]},
    }


def _png_data_url(png_path):
    with open(png_path, "rb") as handle:
        return "data:image/png;base64," + base64.b64encode(handle.read()).decode("ascii")


def import_result(out_dir):
    """The floors of the run in `out_dir` that carry rooms and a background; a floor without rooms is left out."""
    with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as handle:
        manifest = json.load(handle)
    if "room_floor_count" not in manifest:
        raise ImportResultFromEarlierVersion("the run predates background.png")
    if manifest["room_extraction_failed"]:
        raise RoomExtractionFailed("the room extraction of the run failed")
    floors_with_rooms = [floor for floor in manifest["floors"] if floor["room_count"] > 0]
    if len(floors_with_rooms) < manifest["room_floor_count"]:
        raise ImportResultIncomplete(f"{manifest['room_floor_count'] - len(floors_with_rooms)} floor(s) with rooms failed")
    floors = []
    for floor in floors_with_rooms:
        if floor["background"] is None:
            raise ImportResultIncomplete(f"floor {floor['label']!r} has rooms but no background")
        floor_dir = os.path.join(out_dir, floor["dir"])
        with open(os.path.join(floor_dir, "coordinates.json"), encoding="utf-8") as handle:
            rooms = json.load(handle)["rooms"]
        floors.append({
            "label": floor["label"],
            "rooms": [_imported_room(room) for room in rooms],
            "background": _png_data_url(os.path.join(floor_dir, floor["background"])),
        })
    return {"floors": floors}
