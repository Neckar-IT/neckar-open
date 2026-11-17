# Floor-plan pipeline

One DWG/DXF in → per-floor **room coordinates** and a **layer × variant matrix** of transparent PNGs
you compose freely in a built-in HTML picker. No global "style" is imposed — every appearance choice is
made per layer in the UI. Fully automated, no manual steps.

## Run

A directory of plans in one reproducible sweep (bootstraps `.venv/` on first run, prefers the DXF
twin where one exists, wipes each target directory before its run):

```bash
internal/open/commons/floorplan/tools/render_examples.sh <CAD_DIR>             # every DWG/DXF plan in the directory
internal/open/commons/floorplan/tools/render_examples.sh <CAD_DIR> hospital    # only base names containing "hospital"
```

A single file:

```bash
# venv with the pipeline dependencies (created once; render_examples.sh does this automatically)
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt

.venv/bin/python floorplan_pipeline.py <input.dwg|.dxf> <out_dir>
    [--px-per-meter 200] [--margin-m 1.0] [--no-preview]
```

- **DWG or DXF** input. DWG is converted to DXF headlessly via the ODA File Converter
  (`tools/oda-file-converter/install.sh`; packages: [`tools/oda-file-converter/README.md`](../../../../../tools/oda-file-converter/README.md#conversion)); DXF is used directly.
- **Room detection** is a pipeline step, not a manual prerequisite: the pipeline runs the Kotlin
  `FloorPlanRoomExtractor` (packaged as `floorplan-extract.jar`) on the DXF to find floors + named,
  measured room polygons. `$FLOORPLAN_EXTRACTOR_JAR` points at that jar; `java` must be on PATH. A committed sibling `<input>.rooms.json`
  (example data) is used directly instead of re-running the extractor. If no rooms are detected — a plan
  that does not follow the room-outline template (room polylines on layer `00_Räume` + `… m²` labels) —
  floors fall back to the paper-space viewports (one floor per content viewport; a sheet often frames
  plans, elevations and sections) and carry no room polygons.

## Output

```
<out>/
  manifest.json                          floors, layers, variants, and every file produced
  <floor>/
    index.html                           layer × variant picker — open it and compose
    coordinates.json                     room polygons in metre + millimetre + pixel, with the transform
    rooms-overlay.png                    coloured, filled, named room polygons (transparent)
    preview.png                          quick all-layers reference (original colours)      (unless --no-preview)
    background.png                       per floor with rooms: every layer in black on white without annotation, drawn by
                                         FloorBackgroundCli of floorplan-extract.jar (render_backgrounds);
                                         floorplan_import.import_result serves it with the rooms as import.json
    layers/<NN-layer>/<variant>.png      the matrix: each layer in each variant, transparent + pixel-aligned
```

Every PNG of a floor is the **same pixel size** and **aligned**, so any combination of layer/variant
PNGs, `rooms-overlay.png`, and `polygon_px` in `coordinates.json` overlay exactly.

## The matrix (layer × variant)

Each layer is rendered in each **variant** (a column). Variants are appearance only — never a
layer-name rule:

| variant | colour | text |
|---|---|---|
| `original` | the layer's original ACI colours | kept |
| `teal` / `gray` / `black` | one ink | kept |
| `…-notext` | same inks | annotation + its orphan leaders removed |

The `-notext` copy is written only when a layer actually carries text (otherwise it would be identical).
Define/extend variants in [`floorplan_styles.py`](floorplan_styles.py) (`LAYER_VARIANTS`).

**The UI (`index.html`)** stacks the layers and lets you, per layer, pick a variant from a dropdown (or
hide the layer). Quick buttons set all layers to a colour at once; a checkbox switches everything to the
`-notext` variant where available; the room overlay toggles on top. There is **no** global "clean" —
you decide, per layer, colour and whether text stays.

## Layers inside blocks

A layer that the block content of an INSERT draws on is a row, at any nesting depth; the INSERT
alone adds no row for the layer it is placed on. `collect_layer_counts` reads the block definitions:

- Content on layer `0` inside a block and an ATTRIB on layer `0` count for the layer of the nearest
  enclosing INSERT off layer `0`.
- A visible ATTRIB counts for its layer; `ATTDEF` and entities with the `invisible` flag count nowhere.
- A DIMENSION, LEADER or other composite entity inside a block counts once, for its own layer, and
  keeps its generated geometry.
- A floor lists every layer of an INSERT whose bounding box crosses its window.

The render of a row draws the INSERTs that hold its layer and hides the block content of every other
layer; ezdxf expands the blocks and resolves BYBLOCK colour and lineweight. The `-notext` copy of a row
also hides the annotation inside blocks.

Every row is rendered in every colour, the rows are sorted by name, and each carries its `entity_count`
and `initially_visible` in `manifest.json`. Layers that differ in case alone are separate rows. Both
viewers (`index.html` and the webapp) filter the rows by layer name. A floor with more than
`INITIAL_VISIBLE_ROWS` = 30 rows sets `initially_visible` on its 30 rows of the highest `entity_count`,
and both viewers open with the other rows switched off; a viewer loads the PNG of a row when the row is
switched on. The index of a layer directory has two digits, and as many as the row count of its floor
above 99 rows (`layers/097-SF_A_GARD`).

Measured on 2026-10-06 with `autodesk-sample-home-floor-plan.dwg` (2.4 MB, 10 INSERTs and 3 TEXTs on
layer `0`) in `ubuntu-java-python-floorplan` (input-addressed tag from `docker/bake-image-input-fingerprints.lock`), `--px-per-meter 200`, one run:

| Measurement | Value |
|---|---|
| Runtime of the pipeline, DWG conversion included | 54 min 22 s |
| `all_layers` | 204 layers |
| Floors (one per content viewport) | 17 |
| Rows per floor | 6 to 119, 975 over the 17 floors |
| Time per floor | 8 s (7 rows, 2359×2520 px) to 437 s (84 rows, 6000×4036 px) |
| Output directory | 387 MB, 4349 PNGs |
| Webapp viewer on the 119-row floor (6000×2052 px) | 30 PNGs loaded at the start, 5 MB JavaScript heap |

## Render workers

`RENDER_WORKERS` = 2 spawned processes draw the rows and previews. Each worker reads the DXF once and
receives the entity handles of a row or preview; the parent decides floors, rows and windows and writes
`manifest.json`. The `progress` callback of `process` receives a German status line per step and after each
finished render; the webapp shows it on the progress page.

Measured on 2026-10-09 with `execution-plan-house-a.dxf` (7 MB, 3 floors, 43 rows, 64 renders),
32-core workstation, one run per value:

| `RENDER_WORKERS` | Runtime | Peak RSS of all processes |
|---|---|---|
| sequential, without render workers | 50.4 s | 1.27 GB |
| 2 | 27.1 s | 1.83 GB |
| 3 | 20.4 s | 2.30 GB |
| 4 | 16.5 s | 2.54 GB |

## Units

The drawing unit is read from `$INSUNITS` and the document is normalised to **metres** at ingest, so a
mm / cm / inch drawing produces the exact same real-world output (verified: a ×1000 mm copy yields
byte-identical `coordinates.json`). Without `$INSUNITS` the median of stated room area against outline area
decides, snapped to mm, cm, dm or m; metres are assumed only when no room states an area. `coordinates.json` gives every room polygon three ways:

- `polygon_m` — metres · `polygon_mm` — millimetres · `polygon_px` — pixels in that floor's PNGs

Pixel transform (top-left origin): `px_x = (X_m − minX_m) · px_per_meter`, `px_y = (maxY_m − Y_m) · px_per_meter`.

## Robustness — nothing "accidentally works" for one drawing

- **No layer names in the logic.** Colour and text are per-layer choices in the UI; the pipeline never
  hard-codes which layer is "walls" or "drainage". Semantic grouping is the user's job in the picker.
- **Annotation** is removed by DXF entity **type** (`TEXT/MTEXT/DIMENSION/LEADER/…`), universal.
- **Orphan leaders** are removed by **geometry** (open line whose endpoint lands in a removed text's
  bounding box) — no per-drawing tuning.
- **Units** normalised from `$INSUNITS`, or from the stated room areas without it (see above).
- `manifest.json` lists every layer that entities or block content draw on (`all_layers`) and the
  variants.

## Not committed

Renders land under `renders/` (git-ignored). Only the pipeline code
(`floorplan_pipeline.py`, `floorplan_styles.py`) is committed.
