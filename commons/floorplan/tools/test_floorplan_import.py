#!/usr/bin/env python3
"""Tests for the import result of process() without the Kotlin background renderer, and floorplan_import.import_result.

The drawings are generated; a `<name>.rooms.json` sidecar next to each stands in for the room extractor. The tests of
the drawn background run in the task testImport of floorplan.neckar.it, which has the Kotlin jar, with the helpers of
this module.

Run:  the Gradle task `testTools` of the floorplan.neckar.it module
Exit 0 = all pass; non-zero = a failure (the failing test is printed).
"""
import base64
import io
import json
import os
import subprocess
import tempfile

import ezdxf
import numpy
from PIL import Image

import floorplan_import
import floorplan_pipeline

PX_PER_METER = 20
SIZE_PX = 240  # (10 m + 2 * 1 m margin) * 20 px


def square_drawing(path):
    """A 10 m square outline with the text WOHNEN and its leader at its centre and a block holding the text 12 m² below it."""
    doc = ezdxf.new("R2018")
    doc.header["$INSUNITS"] = 6
    doc.modelspace().add_lwpolyline([(0, 0), (10, 0), (10, 10), (0, 10)], close=True, dxfattribs={"layer": "WALLS"})
    doc.modelspace().add_text("WOHNEN", height=2, dxfattribs={"layer": "WALLS", "insert": (3, 4)})
    doc.modelspace().add_line((1, 8), (4, 5), dxfattribs={"layer": "WALLS"})  # the leader of WOHNEN, ends in its text box
    doc.blocks.new("AREA").add_text("12 m²", height=1.5, dxfattribs={"layer": "LABELS", "insert": (0, 0)})
    doc.modelspace().add_blockref("AREA", (3, 1.5), dxfattribs={"layer": "LABELS"})
    doc.saveas(path)


ONE_ROOM_FLOOR = {
    "label": "Geschoss 1",
    # The 4 m square at the top left of the drawing, in the floor-local y-down frame of the extractor.
    "rooms": [{"name": "Wohnen", "statedAreaSquareMeters": 16.0,
               "bounds": {"vertices": [{"x": 0, "y": 0}, {"x": 4, "y": 0}, {"x": 4, "y": 4}, {"x": 0, "y": 4}]}}],
    "calibration": {"dxfMinX": 0, "dxfMinY": 0, "dxfMaxX": 10, "dxfMaxY": 10, "scale": 1.0},
}


def import_square_drawing(floors):
    """process() on the square drawing with `floors` as its rooms sidecar, then import_result."""
    with tempfile.TemporaryDirectory(prefix="floorplan-import-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "plan.dxf"), os.path.join(workdir, "out")
        square_drawing(dxf)
        with open(os.path.join(workdir, "plan.rooms.json"), "w", encoding="utf-8") as handle:
            json.dump({"floors": floors, "verticalSpaces": []}, handle)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=PX_PER_METER, margin_m=1.0, want_preview=False)
        return floorplan_import.import_result(out_dir)


def background_pixels(floor):
    data_url = floor["background"]
    assert data_url.startswith("data:image/png;base64,"), data_url[:40]
    return numpy.array(Image.open(io.BytesIO(base64.b64decode(data_url.split(",", 1)[1]))).convert("RGB"))


def _import_with_backgrounds(render_backgrounds):
    """import_square_drawing with `render_backgrounds` standing in for the Kotlin call; the image is a white PNG."""
    original = floorplan_pipeline.render_backgrounds
    floorplan_pipeline.render_backgrounds = render_backgrounds
    try:
        return import_square_drawing([ONE_ROOM_FLOOR])
    finally:
        floorplan_pipeline.render_backgrounds = original


def _drawn(request, error=None, size=(SIZE_PX, SIZE_PX)):
    """A FloorBackgroundResult for `request` whose floors carry `error` or a white PNG of `size`."""
    floors = []
    for floor in request["floors"]:
        if error is None:
            Image.new("RGB", size, "white").save(floor["png"])
        floors.append({"png": floor["png"], "widthPx": size[0], "heightPx": size[1], "roomsPx": [], "error": error})
    return {"floors": floors}


def test_drawing_without_room_outlines_yields_no_floors():
    assert import_square_drawing([]) == {"floors": []}


def test_floor_whose_background_render_failed_has_no_import_result():
    try:
        _import_with_backgrounds(lambda dxf_path, request, workdir: None)
    except floorplan_import.ImportResultIncomplete as error:
        assert "Geschoss 1" in str(error), error
        return
    raise AssertionError("import_result raised no ImportResultIncomplete")


def test_floor_the_kotlin_renderer_reports_an_error_for_has_no_import_result():
    try:
        _import_with_backgrounds(lambda dxf_path, request, workdir: _drawn(request, error="too complex"))
    except floorplan_import.ImportResultIncomplete as error:
        assert "Geschoss 1" in str(error), error
        return
    raise AssertionError("import_result raised no ImportResultIncomplete")


def test_background_of_another_frame_size_has_no_import_result():
    try:
        _import_with_backgrounds(lambda dxf_path, request, workdir: _drawn(request, size=(SIZE_PX + 1, SIZE_PX)))
    except floorplan_import.ImportResultIncomplete as error:
        assert "Geschoss 1" in str(error), error
        return
    raise AssertionError("import_result raised no ImportResultIncomplete")


def test_a_background_call_beyond_its_timeout_draws_no_background():
    timeouts = []

    def run(command, **options):
        timeouts.append(options["timeout"])
        raise subprocess.TimeoutExpired(command, options["timeout"])

    originals = subprocess.run, floorplan_pipeline.shutil.which, floorplan_pipeline._EXTRACTOR_JAR
    subprocess.run, floorplan_pipeline.shutil.which, floorplan_pipeline._EXTRACTOR_JAR = run, lambda name: "/usr/bin/java", __file__
    try:
        with tempfile.TemporaryDirectory(prefix="floorplan-import-test-") as workdir:
            assert floorplan_pipeline.render_backgrounds("plan.dxf", {"floors": []}, workdir) is None
    finally:
        subprocess.run, floorplan_pipeline.shutil.which, floorplan_pipeline._EXTRACTOR_JAR = originals
    assert timeouts == [floorplan_pipeline._BACKGROUND_TIMEOUT_SECONDS], timeouts


def test_background_request_names_the_rooms_and_bounds_of_the_floor_in_metres():
    requests = []

    def record(dxf_path, request, workdir):
        requests.append(request)
        return _drawn(request)

    floor = _import_with_backgrounds(record)["floors"][0]
    assert background_pixels(floor).shape == (SIZE_PX, SIZE_PX, 3)
    request = requests[0]
    assert (request["metersPerUnit"], request["marginMeters"], request["pixelsPerMeter"]) == (1.0, 1.0, PX_PER_METER), request
    [drawn_floor] = request["floors"]
    assert drawn_floor["boundsMeters"] == {"minX": 0, "minY": 0, "maxX": 10, "maxY": 10}, drawn_floor["boundsMeters"]
    assert drawn_floor["roomsMeters"] == [[{"x": 0, "y": 10}, {"x": 4, "y": 10}, {"x": 4, "y": 6}, {"x": 0, "y": 6}]], drawn_floor["roomsMeters"]


def _import_result_of_manifest(manifest):
    with tempfile.TemporaryDirectory(prefix="floorplan-import-test-") as out_dir:
        with open(os.path.join(out_dir, "manifest.json"), "w", encoding="utf-8") as handle:
            json.dump(manifest, handle)
        return floorplan_import.import_result(out_dir)


def test_run_before_background_png_existed_has_no_import_result():
    try:
        _import_result_of_manifest({"floors": [{"label": "Geschoss 1", "dir": "g1", "room_count": 1}]})
    except floorplan_import.ImportResultFromEarlierVersion:
        return
    raise AssertionError("import_result raised no ImportResultFromEarlierVersion")


def _room_extraction_failed(sidecar=None):
    """process() on the square drawing with an unavailable extractor, or with `sidecar` as its rooms sidecar."""
    original = floorplan_pipeline._EXTRACTOR_JAR
    floorplan_pipeline._EXTRACTOR_JAR = ""
    try:
        with tempfile.TemporaryDirectory(prefix="floorplan-import-test-") as workdir:
            dxf, out_dir = os.path.join(workdir, "plan.dxf"), os.path.join(workdir, "out")
            square_drawing(dxf)
            if sidecar is not None:
                with open(os.path.join(workdir, "plan.rooms.json"), "w", encoding="utf-8") as handle:
                    json.dump(sidecar, handle)
            floorplan_pipeline.process(dxf, out_dir, px_per_m=PX_PER_METER, margin_m=1.0, want_preview=False)
            try:
                floorplan_import.import_result(out_dir)
            except floorplan_import.RoomExtractionFailed:
                return True
            return False
    finally:
        floorplan_pipeline._EXTRACTOR_JAR = original


def test_run_with_an_unavailable_room_extractor_has_no_import_result():
    assert _room_extraction_failed(), "import_result raised no RoomExtractionFailed"


def test_run_with_an_unreadable_rooms_sidecar_has_no_import_result():
    assert _room_extraction_failed(sidecar={"floors": [{}]}), "import_result raised no RoomExtractionFailed"


def test_floor_with_rooms_the_run_skipped_has_no_import_result():
    try:
        _import_result_of_manifest({"room_floor_count": 2, "room_extraction_failed": False, "floors": [
            {"label": "Geschoss 1", "dir": "g1", "room_count": 1, "background": "background.png"}]})
    except floorplan_import.ImportResultIncomplete as error:
        assert "1 floor" in str(error), error
        return
    raise AssertionError("import_result raised no ImportResultIncomplete")


def test_rooms_without_a_stated_area_leave_the_drawing_unit_open():
    rooms = [{"name": "Flur", "area_m2": None, "polygon_u": [(0, 0), (4000, 0), (4000, 4000)]},
             {"name": "Bad", "area_m2": 6.0, "polygon_u": [(0, 0), (1, 0), (2, 0)]}]  # degenerate: no area to compare
    assert floorplan_pipeline.meters_per_unit_from_room_areas([{"rooms": rooms}]) is None


def test_one_mislabelled_room_of_three_keeps_the_millimetre_unit():
    square = [(0, 0), (4000, 0), (4000, 4000), (0, 4000)]  # 16 m² in millimetres
    rooms = [{"name": "A", "area_m2": 16.0, "polygon_u": square}, {"name": "B", "area_m2": 16.0, "polygon_u": square},
             {"name": "C", "area_m2": 1600.0, "polygon_u": square}]
    assert floorplan_pipeline.meters_per_unit_from_room_areas([{"rooms": rooms}]) == 0.001


def test_metre_drawing_with_sloped_ceiling_rooms_stays_in_metres():
    square = [(0, 0), (4, 0), (4, 4), (0, 4)]  # 16 m² outlined, stated at 85 % under a sloped ceiling
    rooms = [{"name": name, "area_m2": 13.6, "polygon_u": square} for name in ("A", "B", "C")]
    assert floorplan_pipeline.meters_per_unit_from_room_areas([{"rooms": rooms}]) == 1.0


def test_unreadable_drawing_ends_the_run_with_a_message_free_of_server_paths():
    with tempfile.TemporaryDirectory(prefix="floorplan-import-test-") as workdir:
        not_a_dxf = os.path.join(workdir, "plan.dxf")
        with open(not_a_dxf, "wb") as handle:
            handle.write(b"%PDF-1.7")
        try:
            floorplan_pipeline.process(not_a_dxf, os.path.join(workdir, "out"), px_per_m=PX_PER_METER, margin_m=1.0,
                                       want_preview=False)
        except SystemExit as error:
            assert str(error) == "DXF konnte nicht gelesen werden (beschädigt oder kein gültiges DXF)", error
            return
    raise AssertionError("process raised no SystemExit")


def test_failed_background_render_closes_its_figure():
    import matplotlib.pyplot as plt

    doc = ezdxf.new("R2018")
    doc.modelspace().add_line((0, 0), (10, 10))
    original = floorplan_pipeline._NoAnnotationFrontend.draw_entities

    def fail(self, entities):
        raise RuntimeError("render failed")

    floorplan_pipeline._NoAnnotationFrontend.draw_entities = fail
    figures_before = plt.get_fignums()
    try:
        with tempfile.TemporaryDirectory(prefix="floorplan-import-test-") as workdir:
            floorplan_pipeline.render_background(doc, (0, 0, 10, 10), list(doc.modelspace()), os.path.join(workdir, "bg.png"), 50, 50)
    except RuntimeError:
        assert plt.get_fignums() == figures_before, (figures_before, plt.get_fignums())
        return
    finally:
        floorplan_pipeline._NoAnnotationFrontend.draw_entities = original
    raise AssertionError("render_background raised no RuntimeError")


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    failed = 0
    for t in tests:
        try:
            t()
            print(f"PASS {t.__name__}")
        except (Exception, SystemExit) as exc:  # noqa: BLE001
            failed += 1
            print(f"FAIL {t.__name__}: {exc!r}")
    print(f"\n{len(tests) - failed}/{len(tests)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
