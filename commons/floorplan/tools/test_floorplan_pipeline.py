#!/usr/bin/env python3
"""Tests for floorplan_pipeline.py: ingest_to_dxf with a fake ODAFileConverter on PATH, the layer
matrix of generated drawings with blocks, and the render workers and progress messages of process.

Run:  the Gradle task `testTools` of the floorplan.neckar.it module
Exit 0 = all pass; non-zero = a failure (the failing test is printed).
"""
import json
import os
import signal
import stat
import subprocess
import sys
import tempfile
import threading
import time
from concurrent.futures import Future
from concurrent.futures import process as futures_process
from concurrent.futures.process import BrokenProcessPool

import ezdxf
import numpy
from PIL import Image

import floorplan_pipeline


def _ingest_with_fake_converter(converter_script):
    """Run ingest_to_dxf on a temporary .dwg and return the DXF path below the workdir and the DXF content.

    PATH holds only the fake converter, so xvfb-run stays out.
    """
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        bin_dir = os.path.join(workdir, "bin")
        os.makedirs(bin_dir)
        converter = os.path.join(bin_dir, "ODAFileConverter")
        with open(converter, "w") as file:
            file.write("#!/bin/sh\n" + converter_script)
        os.chmod(converter, os.stat(converter).st_mode | stat.S_IXUSR)
        input_path = os.path.join(workdir, "plan.dwg")
        with open(input_path, "wb") as file:
            file.write(b"AC1032")
        original_path = os.environ["PATH"]
        os.environ["PATH"] = bin_dir
        try:
            dxf = floorplan_pipeline.ingest_to_dxf(input_path, workdir)
        finally:
            os.environ["PATH"] = original_path
        with open(dxf) as file:
            return os.path.relpath(dxf, workdir), file.read()


def _error_message(converter_script):
    try:
        _ingest_with_fake_converter(converter_script)
    except SystemExit as error:
        return str(error)
    raise AssertionError("ingest_to_dxf raised no SystemExit")


def test_converter_failure_reports_exit_code_and_last_non_empty_stderr_lines():
    script = "for number in 1 2 3 4 5 6 7 8; do echo \"line $number\" >&2; echo '  ' >&2; done\nexit 134\n"
    message = _error_message(script)
    assert message == ("DWG-Konvertierung fehlgeschlagen (ODA File Converter): Exit-Code 134: "
                       "line 4 | line 5 | line 6 | line 7 | line 8"), message


def test_converter_failure_without_stderr_reports_the_exit_code_alone():
    assert _error_message("exit 3\n") == "DWG-Konvertierung fehlgeschlagen (ODA File Converter): Exit-Code 3"


def test_stderr_tail_is_cut_to_400_characters():
    message = _error_message("printf '%0600d\\n' 7 >&2\nexit 1\n")
    assert message.endswith(": " + "0" * 399 + "7"), message


def test_converter_timeout_reports_the_timeout_and_stderr():
    original_timeout = floorplan_pipeline._ODA_TIMEOUT_SECONDS
    floorplan_pipeline._ODA_TIMEOUT_SECONDS = 1
    started = time.monotonic()
    try:
        # The sleep outlives the killed shell and keeps stderr open, like the converter below xvfb-run.
        message = _error_message("echo 'still converting' >&2\n/bin/sleep 5\n")
    finally:
        floorplan_pipeline._ODA_TIMEOUT_SECONDS = original_timeout
    assert message == ("DWG-Konvertierung fehlgeschlagen (ODA File Converter): "
                       "Zeitüberschreitung nach 1 s: still converting"), message
    assert time.monotonic() - started < 4, "ingest_to_dxf waited for the child that outlived the timeout"


def test_longest_job_error_fits_the_500_characters_the_webapp_stores():
    original_timeout = floorplan_pipeline._ODA_TIMEOUT_SECONDS
    floorplan_pipeline._ODA_TIMEOUT_SECONDS = 1
    try:
        message = _error_message("printf '%0600d\\n' 7 >&2\nexec /bin/sleep 5\n")
    finally:
        floorplan_pipeline._ODA_TIMEOUT_SECONDS = original_timeout
    assert message.endswith("0" * 399 + "7"), message
    production_length = len(message) - len("1") + len(str(original_timeout))
    assert production_length <= 500, production_length


def test_stderr_names_paths_relative_to_the_workdir():
    message = _error_message("echo \"cannot read $1/input.dwg below ${1%/oda-in}\" >&2\nexit 1\n")
    assert message.endswith(": cannot read oda-in/input.dwg below ."), message


def test_successful_conversion_returns_the_converted_dxf():
    dxf, content = _ingest_with_fake_converter("echo converted > \"$2/input.dxf\"\n")
    assert (dxf, content) == (os.path.join("oda-out", "input.dxf"), "converted\n"), (dxf, content)


def test_conversion_without_output_reports_the_missing_dxf():
    assert _error_message("exit 0\n").startswith("DWG-Konvertierung ergab keine DXF-Datei")


def test_dxf_input_is_returned_without_a_converter_call():
    assert floorplan_pipeline.ingest_to_dxf("/nonexistent/plan.DXF", "/nonexistent") == "/nonexistent/plan.DXF"


# ----------------------------------------------------------------------------- layers inside blocks

_WINDOW = (0, 0, 10, 10)
_SIZE_PX = 200  # 20 px per drawing unit: the line at y = 2 is drawn in pixel row 160


def _block_drawing():
    """INSERT on layer PLACED of a block two levels deep: lines on A (y = 2), B (y = 8), 0 (y = 5), a TEXT on A (y = 6)."""
    doc = ezdxf.new("R2018")
    doc.header["$INSUNITS"] = 6
    inner = doc.blocks.new("INNER")
    inner.add_line((1, 2), (9, 2), dxfattribs={"layer": "A"})
    inner.add_line((1, 8), (9, 8), dxfattribs={"layer": "B"})
    outer = doc.blocks.new("OUTER")
    outer.add_blockref("INNER", (0, 0), dxfattribs={"layer": "0"})
    outer.add_line((1, 5), (9, 5), dxfattribs={"layer": "0"})
    outer.add_text("LABEL", height=0.8, dxfattribs={"layer": "A", "insert": (1, 6)})
    doc.modelspace().add_blockref("OUTER", (0, 0), dxfattribs={"layer": "PLACED"})
    return doc


def _layer_totals(doc):
    entities = list(doc.modelspace())
    counts = floorplan_pipeline.collect_layer_counts(doc, entities)
    return floorplan_pipeline.sum_layer_counts(zip(entities, counts))


def _row_pixels(doc, layer, color=floorplan_pipeline.BLACK, notext=False):
    """RGBA pixels of the row render of `layer` over the whole model space."""
    entities = list(doc.modelspace())
    entity_counts = list(zip(entities, floorplan_pipeline.collect_layer_counts(doc, entities)))
    row_entities = floorplan_pipeline.layer_entities(entity_counts, layer, notext, leader_margin=floorplan_pipeline.LEADER_MARGIN_M)
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        png = os.path.join(workdir, "row.png")
        floorplan_pipeline.render_layer_variant(doc, _WINDOW, row_entities, color, png, _SIZE_PX, _SIZE_PX, layer, notext)
        return numpy.array(Image.open(png).convert("RGBA"))


def _ink(pixels, y):
    """Summed alpha of the pixel rows around the drawing coordinate `y`."""
    row = int((_WINDOW[3] - y) * _SIZE_PX / (_WINDOW[3] - _WINDOW[1]))
    return int(pixels[row - 4:row + 5, :, 3].sum())


def test_block_two_levels_deep_reports_the_layers_of_its_content_and_its_layer_0_content_for_the_layer_of_the_insert():
    totals = _layer_totals(_block_drawing())
    assert totals == {"A": [2, 1], "B": [1, 0], "PLACED": [1, 0]}, totals


def test_content_on_layer_0_counts_for_the_layer_of_the_nearest_insert_off_layer_0():
    doc = ezdxf.new("R2018")
    doc.blocks.new("LEAF").add_line((0, 0), (1, 0), dxfattribs={"layer": "0"})
    middle = doc.blocks.new("MIDDLE")
    middle.add_blockref("LEAF", (0, 0), dxfattribs={"layer": "X"})
    middle.add_blockref("LEAF", (0, 1), dxfattribs={"layer": "0"})
    doc.modelspace().add_blockref("MIDDLE", (0, 0), dxfattribs={"layer": "PLACED"})
    doc.modelspace().add_blockref("LEAF", (0, 2), dxfattribs={"layer": "0"})
    totals = _layer_totals(doc)
    assert totals == {"X": [1, 0], "PLACED": [1, 0], "0": [1, 0]}, totals


def test_attdef_and_invisible_entities_are_absent_and_a_visible_attrib_counts_for_its_layer():
    doc = ezdxf.new("R2018")
    block = doc.blocks.new("TAGGED")
    block.add_attdef("ID", (0, 0), dxfattribs={"layer": "DEFINITION"})
    block.add_line((0, 0), (1, 0), dxfattribs={"layer": "HIDDEN", "invisible": 1})
    block.add_line((0, 1), (1, 1), dxfattribs={"layer": "DRAWN"})
    doc.blocks.new("UNSEEN").add_line((0, 2), (1, 2), dxfattribs={"layer": "NESTED"})
    block.add_blockref("UNSEEN", (0, 0), dxfattribs={"layer": "0", "invisible": 1})
    insert = doc.modelspace().add_blockref("TAGGED", (0, 0), dxfattribs={"layer": "PLACED"})
    insert.add_attrib("ID", "7", (0, 0), dxfattribs={"layer": "TAG"})
    insert.add_attrib("SECRET", "8", (0, 0), dxfattribs={"layer": "HIDDEN_TAG"}).is_invisible = True
    doc.modelspace().add_blockref("TAGGED", (5, 0), dxfattribs={"layer": "PLACED", "invisible": 1})
    totals = _layer_totals(doc)
    assert totals == {"DRAWN": [1, 0], "TAG": [1, 1]}, totals


def test_attrib_on_layer_0_counts_for_the_layer_of_its_insert_and_is_absent_from_the_notext_row_render():
    doc = ezdxf.new("R2018")
    doc.blocks.new("TAGGED").add_line((1, 2), (9, 2), dxfattribs={"layer": "DRAWN"})
    insert = doc.modelspace().add_blockref("TAGGED", (0, 0), dxfattribs={"layer": "PLACED"})
    insert.add_attrib("ID", "LABEL", (1, 6), dxfattribs={"layer": "0", "height": 0.8})
    assert _layer_totals(doc) == {"DRAWN": [1, 0], "PLACED": [1, 1]}, _layer_totals(doc)
    assert _ink(_row_pixels(doc, "PLACED"), 6.4) > 0, "the ATTRIB is missing in the row of its INSERT"
    assert _ink(_row_pixels(doc, "PLACED", notext=True), 6.4) == 0, "the ATTRIB is drawn in the -notext render"
    assert _ink(_row_pixels(doc, "DRAWN"), 6.4) == 0, "the ATTRIB is drawn in the row DRAWN"


def test_minsert_counts_its_block_content_and_its_attribs_once_per_grid_cell():
    doc = ezdxf.new("R2018")
    doc.blocks.new("CELL").add_line((0, 0), (1, 0), dxfattribs={"layer": "A"})
    insert = doc.modelspace().add_blockref("CELL", (0, 0), dxfattribs={
        "layer": "PLACED", "row_count": 2, "column_count": 3, "row_spacing": 2, "column_spacing": 2})
    insert.add_attrib("ID", "7", (0, 0), dxfattribs={"layer": "TAG"})
    assert _layer_totals(doc) == {"A": [6, 0], "TAG": [6, 6]}, _layer_totals(doc)


def test_insert_of_a_missing_block_definition_belongs_to_no_row():
    doc = ezdxf.new("R2018")
    doc.blocks.new("GONE")
    insert = doc.modelspace().add_blockref("GONE", (0, 0), dxfattribs={"layer": "PLACED"})
    insert.add_attrib("ID", "7", (0, 0), dxfattribs={"layer": "TAG"})
    doc.modelspace().add_line((1, 2), (9, 2), dxfattribs={"layer": "TAG"})
    doc.blocks.delete_block("GONE", safe=False)
    assert _layer_totals(doc) == {"TAG": [1, 0]}, _layer_totals(doc)
    assert _ink(_row_pixels(doc, "TAG"), 2) > 0, "the line of layer TAG is missing in its row"


def test_block_that_references_itself_ends_the_walk():
    doc = ezdxf.new("R2018")
    block = doc.blocks.new("LOOP")
    block.add_line((0, 0), (1, 0), dxfattribs={"layer": "A"})
    block.add_blockref("LOOP", (0, 0), dxfattribs={"layer": "0"})
    doc.modelspace().add_blockref("LOOP", (0, 0), dxfattribs={"layer": "0"})
    totals = _layer_totals(doc)
    assert totals == {"A": [1, 0]}, totals


def test_row_render_draws_the_block_content_of_its_layer_alone():
    doc = _block_drawing()
    row_a, row_b, row_placed = _row_pixels(doc, "A"), _row_pixels(doc, "B"), _row_pixels(doc, "PLACED")
    assert _ink(row_a, 2) > 0 and _ink(row_a, 8) == 0 and _ink(row_a, 5) == 0, "row A"
    assert _ink(row_b, 8) > 0 and _ink(row_b, 2) == 0 and _ink(row_b, 5) == 0, "row B"
    assert _ink(row_placed, 5) > 0 and _ink(row_placed, 2) == 0 and _ink(row_placed, 8) == 0, "row PLACED"


def test_layers_that_differ_in_case_alone_are_rows_that_each_draw_their_own_block_content():
    doc = ezdxf.new("R2018")
    block = doc.blocks.new("CASED")
    block.add_line((1, 2), (9, 2), dxfattribs={"layer": "wall"})
    block.add_line((1, 8), (9, 8), dxfattribs={"layer": "WALL"})
    doc.modelspace().add_blockref("CASED", (0, 0), dxfattribs={"layer": "PLACED"})
    assert _layer_totals(doc) == {"wall": [1, 0], "WALL": [1, 0]}, _layer_totals(doc)
    lower, upper = _row_pixels(doc, "wall"), _row_pixels(doc, "WALL")
    assert _ink(lower, 2) > 0 and _ink(lower, 8) == 0, "row wall"
    assert _ink(upper, 8) > 0 and _ink(upper, 2) == 0, "row WALL"


def test_byblock_line_renders_in_the_colour_of_its_insert():
    doc = ezdxf.new("R2018")
    doc.blocks.new("TINTED").add_line((1, 2), (9, 2), dxfattribs={"layer": "A", "color": 0})
    doc.modelspace().add_blockref("TINTED", (0, 0), dxfattribs={"layer": "PLACED", "color": 1})
    pixels = _row_pixels(doc, "A", color=None)
    column = pixels[:, _SIZE_PX // 2]
    red, green, blue, alpha = (int(value) for value in column[column[:, 3].argmax()])
    assert alpha > 0 and red > 200 and green < 60 and blue < 60, (red, green, blue, alpha)


def test_notext_row_render_hides_the_text_inside_the_block():
    doc = _block_drawing()
    with_text, without_text = _row_pixels(doc, "A"), _row_pixels(doc, "A", notext=True)
    assert _ink(with_text, 6.4) > 0, "the TEXT is missing in the row render"
    assert _ink(without_text, 6.4) == 0, "the TEXT is drawn in the -notext render"
    assert _ink(without_text, 2) > 0, "the line of layer A is missing in the -notext render"


def test_dimension_inside_a_block_keeps_its_generated_geometry_in_its_row():
    doc = ezdxf.new("R2018", setup=True)
    block = doc.blocks.new("MEASURED")
    dimension = block.add_linear_dim(base=(1, 5), p1=(1, 2), p2=(9, 2), dxfattribs={"layer": "DIM"}).render().dimension
    for generated in doc.blocks.get(dimension.dxf.geometry):
        generated.dxf.layer = "0"  # as AutoCAD writes it
    doc.modelspace().add_blockref("MEASURED", (0, 0), dxfattribs={"layer": "PLACED"})
    assert _layer_totals(doc) == {"DIM": [1, 1]}, _layer_totals(doc)
    assert _ink(_row_pixels(doc, "DIM"), 5) > 0, "the dimension line is missing in the row of the DIMENSION"
    assert _ink(_row_pixels(doc, "DIM", notext=True), 5) == 0, "the DIMENSION is drawn in the -notext render"


def test_drawing_without_insert_renders_a_row_like_a_plain_frontend():
    doc = ezdxf.new("R2018")
    modelspace = doc.modelspace()
    line_a = modelspace.add_line((1, 2), (9, 2), dxfattribs={"layer": "A"})
    modelspace.add_line((1, 8), (9, 8), dxfattribs={"layer": "B"})
    entities = list(modelspace)
    entity_counts = list(zip(entities, floorplan_pipeline.collect_layer_counts(doc, entities)))
    row_entities = floorplan_pipeline.layer_entities(entity_counts, "A", notext=False, leader_margin=floorplan_pipeline.LEADER_MARGIN_M)
    assert row_entities == [line_a], row_entities
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        row_png, plain_png = os.path.join(workdir, "row.png"), os.path.join(workdir, "plain.png")
        floorplan_pipeline.render_layer_variant(
            doc, _WINDOW, row_entities, floorplan_pipeline.BLACK, row_png, _SIZE_PX, _SIZE_PX, "A")
        floorplan_pipeline._render_entities(
            doc, _WINDOW, [line_a], floorplan_pipeline.BLACK, plain_png, _SIZE_PX, _SIZE_PX, background=None)
        with open(row_png, "rb") as row, open(plain_png, "rb") as plain:
            assert row.read() == plain.read(), "row render differs from the plain render"


def test_process_lists_the_block_layers_as_rows_with_entity_count():
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "blocks.dxf"), os.path.join(workdir, "out")
        _block_drawing().saveas(dxf)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=20, margin_m=1.0, want_preview=False)
        with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as handle:
            manifest = json.load(handle)
        assert manifest["all_layers"] == ["A", "B", "PLACED"], manifest["all_layers"]
        rows = {row["name"]: row for row in manifest["floors"][0]["layers"]}
        assert {name: row["entity_count"] for name, row in rows.items()} == {"A": 2, "B": 1, "PLACED": 1}, rows
        assert rows["A"]["dir"] == "layers/01-A", rows["A"]["dir"]
        assert rows["A"]["variants"] == ["original", "black", "teal", "gray", "original-notext", "black-notext",
                                         "teal-notext", "gray-notext"], rows["A"]["variants"]
        assert rows["B"]["variants"] == ["original", "black", "teal", "gray"], rows["B"]["variants"]
        assert [row["only_text"] for row in rows.values()] == [False, False, False], rows
        assert [row["initially_visible"] for row in rows.values()] == [True, True, True], rows
        for row in rows.values():
            for variant in row["variants"]:
                png = os.path.join(out_dir, manifest["floors"][0]["dir"], row["dir"], variant + ".png")
                assert os.path.isfile(png), png


def test_process_reads_a_drawing_without_insunits_and_without_stated_areas_in_metres():
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "plan.dxf"), os.path.join(workdir, "out")
        doc = ezdxf.new("R2018")
        del doc.header["$INSUNITS"]
        doc.modelspace().add_lwpolyline([(0, 0), (4, 0), (4, 4), (0, 4)], close=True, dxfattribs={"layer": "WALLS"})
        doc.saveas(dxf)
        with open(os.path.join(workdir, "plan.rooms.json"), "w", encoding="utf-8") as handle:
            json.dump({"floors": [{"label": "Geschoss 1", "calibration": {"dxfMinX": 0, "dxfMinY": 0, "dxfMaxX": 4, "dxfMaxY": 4, "scale": 1.0},
                                   "rooms": [{"name": "Flur", "statedAreaSquareMeters": None, "bounds": {"vertices": [
                                       {"x": 0, "y": 0}, {"x": 4, "y": 0}, {"x": 4, "y": 4}, {"x": 0, "y": 4}]}}]}]}, handle)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=20, margin_m=1.0, want_preview=False)
        with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as handle:
            manifest = json.load(handle)
        assert (manifest["meters_per_unit"], manifest["unit_known"]) == (1.0, False), manifest


def test_process_takes_the_unit_of_a_drawing_without_insunits_from_the_stated_room_areas():
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "plan.dxf"), os.path.join(workdir, "out")
        doc = ezdxf.new("R2018")
        del doc.header["$INSUNITS"]
        doc.modelspace().add_lwpolyline([(0, 0), (4000, 0), (4000, 4000), (0, 4000)], close=True, dxfattribs={"layer": "WALLS"})
        doc.saveas(dxf)
        # The sibling sidecar: one 16 m² room outlined in millimetres.
        with open(os.path.join(workdir, "plan.rooms.json"), "w", encoding="utf-8") as handle:
            json.dump({"floors": [{"label": "Geschoss 1", "calibration": {"dxfMinX": 0, "dxfMinY": 0, "dxfMaxX": 4000, "dxfMaxY": 4000, "scale": 1.0},
                                   "rooms": [{"name": "Wohnen", "statedAreaSquareMeters": 16.0, "bounds": {"vertices": [
                                       {"x": 0, "y": 0}, {"x": 4000, "y": 0}, {"x": 4000, "y": 4000}, {"x": 0, "y": 4000}]}}]}]}, handle)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=20, margin_m=1.0, want_preview=False)
        with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as handle:
            manifest = json.load(handle)
        assert manifest["meters_per_unit"] == 0.001, manifest["meters_per_unit"]
        with open(os.path.join(out_dir, manifest["floors"][0]["dir"], "coordinates.json"), encoding="utf-8") as handle:
            polygon_px = json.load(handle)["rooms"][0]["polygon_px"]
        assert polygon_px == [[20.0, 20.0], [100.0, 20.0], [100.0, 100.0], [20.0, 100.0]], polygon_px


def test_process_reports_each_step_and_counts_every_finished_render():
    messages = []
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "blocks.dxf"), os.path.join(workdir, "out")
        _block_drawing().saveas(dxf)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=20, margin_m=1.0, want_preview=True,
                                   progress=messages.append)
        assert os.path.isfile(os.path.join(out_dir, "Model", "preview.png"))
    # Rows A (with and without text), B and PLACED, plus the preview of the one floor.
    renders = [f"Erzeuge Vorschaubilder … {done} von 5" for done in range(1, 6)]
    assert messages == ["Lese Zeichnung …", "Erkenne Räume …", "Erzeuge Vorschaubilder …"] + renders, messages


def _row_png_of_line_drawing(insunits, units_per_meter):
    doc = ezdxf.new("R2018")
    doc.header["$INSUNITS"] = insunits
    doc.modelspace().add_line((0, 0), (8 * units_per_meter, 0), dxfattribs={"layer": "A"})
    doc.modelspace().add_line((0, 0), (0, 6 * units_per_meter), dxfattribs={"layer": "B"})
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "lines.dxf"), os.path.join(workdir, "out")
        doc.saveas(dxf)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=20, margin_m=1.0, want_preview=False)
        with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as handle:
            floor = json.load(handle)["floors"][0]
        row = next(row for row in floor["layers"] if row["name"] == "A")
        return numpy.asarray(Image.open(os.path.join(out_dir, floor["dir"], row["dir"], "black.png")))


def test_render_worker_draws_a_millimeter_drawing_at_meter_scale():
    millimeter = _row_png_of_line_drawing(insunits=4, units_per_meter=1000)
    meter = _row_png_of_line_drawing(insunits=6, units_per_meter=1)
    assert millimeter.shape == meter.shape == (160, 200, 4), (millimeter.shape, meter.shape)
    assert (millimeter[..., 3] > 0).sum() > 0 and (millimeter == meter).all()


_PARENT_OF_A_RENDER_WORKER = """
import os, sys, time
sys.path.insert(0, sys.argv[1])
import floorplan_pipeline
with floorplan_pipeline.render_pool(sys.argv[2], 1.0) as pool:
    with open(sys.argv[3] + ".tmp", "w") as handle:
        handle.write(str(pool.submit(os.getpid).result()))
    os.replace(sys.argv[3] + ".tmp", sys.argv[3])
    time.sleep(60)
"""


def test_render_worker_exits_when_its_parent_is_killed():
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf = os.path.join(workdir, "blocks.dxf")
        _block_drawing().saveas(dxf)
        tools_dir = os.path.dirname(os.path.abspath(__file__))
        pid_file = os.path.join(workdir, "worker.pid")
        parent = subprocess.Popen([sys.executable, "-c", _PARENT_OF_A_RENDER_WORKER, tools_dir, dxf, pid_file])
        try:
            deadline = time.monotonic() + 60
            while not os.path.exists(pid_file):
                assert parent.poll() is None and time.monotonic() < deadline, "the parent wrote no worker pid"
                time.sleep(0.1)
            with open(pid_file, encoding="utf-8") as handle:
                worker = int(handle.read())
            assert _is_render_worker(worker), f"the probe misses the live render worker {worker}"
        finally:
            parent.kill()
            parent.wait()
        deadline = time.monotonic() + 10
        while _is_render_worker(worker):
            if time.monotonic() > deadline:
                os.kill(worker, signal.SIGKILL)
                raise AssertionError(f"render worker {worker} outlived its killed parent by 10 s")
            time.sleep(0.1)


def _is_render_worker(pid):
    """A zombie or a reused pid is no running render worker."""
    try:
        with open(f"/proc/{pid}/stat", encoding="utf-8") as stat_file:
            state = stat_file.read().rsplit(")", 1)[1].split()[0]
        with open(f"/proc/{pid}/cmdline", "rb") as cmdline_file:
            command = cmdline_file.read()
    except (FileNotFoundError, ProcessLookupError):  # gone before or while it was read
        return False
    return state != "Z" and b"spawn_main" in command


class _PoolRaising:
    def __init__(self, error):
        self.error = error

    def submit(self, task, *args):
        raise self.error


def _assert_reports_the_lost_render_worker(call):
    try:
        call()
    except SystemExit as error:
        assert str(error) == "Ein Render-Prozess wurde unerwartet beendet (vermutlich zu wenig Arbeitsspeicher).", error
    else:
        raise AssertionError("no SystemExit")


def test_result_of_a_broken_pool_ends_the_run_with_the_message_for_the_user():
    future = Future()
    future.set_exception(BrokenProcessPool("A process in the process pool was terminated abruptly"))
    _assert_reports_the_lost_render_worker(lambda: floorplan_pipeline._render_result(future))


def test_submit_to_a_broken_pool_ends_the_run_with_the_message_for_the_user():
    broken = BrokenProcessPool("A child process terminated abruptly, the process pool is not usable anymore")
    _assert_reports_the_lost_render_worker(lambda: floorplan_pipeline._submit(_PoolRaising(broken), os.getpid))


def _submit_raising(error, exiting):
    """The exception `_submit` raises for `error` while the interpreter is `exiting` or not."""
    futures_process._global_shutdown, previous = exiting, futures_process._global_shutdown
    try:
        floorplan_pipeline._submit(_PoolRaising(error), os.getpid)
    except BaseException as raised:
        return raised
    finally:
        futures_process._global_shutdown = previous
    raise AssertionError("_submit raised nothing")


def test_interpreter_exit_interrupts_the_wait_for_queued_renders():
    raised = []

    def wait_for_a_render_that_never_finishes():
        try:
            floorplan_pipeline.await_renders([Future()], progress=print)
        except floorplan_pipeline.RenderInterrupted as error:
            raised.append(error)

    futures_process._global_shutdown, previous = True, futures_process._global_shutdown
    try:
        waiter = threading.Thread(target=wait_for_a_render_that_never_finishes, daemon=True)
        waiter.start()
        waiter.join(timeout=5)  # a wait that ignores the exit fails here instead of hanging testTools
    finally:
        futures_process._global_shutdown = previous
    assert len(raised) == 1, raised


def test_interpreter_exit_after_the_last_render_finishes_the_wait():
    finished = Future()
    finished.set_result(["black"])
    messages = []
    futures_process._global_shutdown, previous = True, futures_process._global_shutdown
    try:
        floorplan_pipeline.await_renders([finished], progress=messages.append)
    finally:
        futures_process._global_shutdown = previous
    assert messages == ["Erzeuge Vorschaubilder … 1 von 1"], messages


def test_failed_run_drops_the_renders_still_queued():
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf = os.path.join(workdir, "blocks.dxf")
        _block_drawing().saveas(dxf)
        futures = []
        try:
            with floorplan_pipeline.render_pool(dxf, 1.0) as pool:
                futures = [pool.submit(time.sleep, 1) for _ in range(8)]
                raise ValueError("a failing floor")
        except ValueError:
            pass
    # The workers and the call queue take at most five of the eight sleeps; the rest is never started.
    assert futures[-1].cancelled(), [future._state for future in futures]


def test_submit_after_the_interpreter_stopped_taking_work_interrupts_the_run():
    stopped = RuntimeError("cannot schedule new futures after interpreter shutdown")
    assert isinstance(_submit_raising(stopped, exiting=True), floorplan_pipeline.RenderInterrupted)


def test_submit_failing_while_the_interpreter_runs_ends_the_run_with_the_start_error():
    raised = _submit_raising(OSError(12, "Cannot allocate memory"), exiting=False)
    assert isinstance(raised, SystemExit), raised
    assert str(raised) == "Render-Prozess konnte nicht gestartet werden: [Errno 12] Cannot allocate memory", raised


def test_row_of_a_block_that_holds_text_alone_is_only_text_without_notext_variants():
    doc = ezdxf.new("R2018")
    doc.header["$INSUNITS"] = 6
    block = doc.blocks.new("LABELLED")
    block.add_text("LABEL", height=0.8, dxfattribs={"layer": "NOTE", "insert": (1, 6)})
    block.add_line((1, 2), (9, 2), dxfattribs={"layer": "A"})
    doc.modelspace().add_blockref("LABELLED", (0, 0), dxfattribs={"layer": "PLACED"})
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "labelled.dxf"), os.path.join(workdir, "out")
        doc.saveas(dxf)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=20, margin_m=1.0, want_preview=False)
        with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as handle:
            rows = {row["name"]: row for row in json.load(handle)["floors"][0]["layers"]}
    assert rows["NOTE"]["only_text"] is True and rows["NOTE"]["variants"] == ["original", "black", "teal", "gray"], rows
    assert rows["A"]["only_text"] is False, rows["A"]


def test_floor_with_100_rows_numbers_its_layer_directories_with_three_digits_and_opens_with_its_30_largest_rows():
    doc = ezdxf.new("R2018")
    doc.header["$INSUNITS"] = 6
    block = doc.blocks.new("MANY")
    for index in range(100):
        block.add_line((1, 1 + index * 0.05), (9, 1 + index * 0.05), dxfattribs={"layer": f"L{index:03d}"})
    block.add_line((1, 9), (9, 9), dxfattribs={"layer": "L099"})
    doc.modelspace().add_blockref("MANY", (0, 0), dxfattribs={"layer": "PLACED"})
    with tempfile.TemporaryDirectory(prefix="floorplan-pipeline-test-") as workdir:
        dxf, out_dir = os.path.join(workdir, "many.dxf"), os.path.join(workdir, "out")
        doc.saveas(dxf)
        floorplan_pipeline.process(dxf, out_dir, px_per_m=5, margin_m=1.0, want_preview=False)
        with open(os.path.join(out_dir, "manifest.json"), encoding="utf-8") as handle:
            rows = json.load(handle)["floors"][0]["layers"]
    assert [rows[0]["dir"], rows[99]["dir"]] == ["layers/001-L000", "layers/100-L099"], (rows[0], rows[99])
    visible = [row["name"] for row in rows if row["initially_visible"]]
    assert visible == [f"L{index:03d}" for index in range(29)] + ["L099"], visible


def main():
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    failed = 0
    for t in tests:
        try:
            t()
            print(f"PASS {t.__name__}")
        # ingest_to_dxf fails with SystemExit; a leaked RenderInterrupted is a BaseException as well
        except (Exception, SystemExit, floorplan_pipeline.RenderInterrupted) as exc:  # noqa: BLE001
            failed += 1
            print(f"FAIL {t.__name__}: {exc!r}")
    print(f"\n{len(tests) - failed}/{len(tests)} passed")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
