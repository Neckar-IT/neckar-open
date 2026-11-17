# Floor-plan visualization tools

Python renderers that turn an architect's DXF floor-plan export plus the extracted
`<name>.rooms.json` into preview images. The room extraction itself is Kotlin
(`it.neckar.floorplan.FloorPlanRoomExtractor`); these tools are a **preview/tooling dependency only**,
not a runtime dependency of the JVM module.

We do not hand-roll a DXF renderer — the tools drive [ezdxf](https://ezdxf.mozman.at/) (the `drawing`
add-on) and let it rasterize via Matplotlib or emit native SVG. Shared rendering helpers (floor crop,
figure setup, ezdxf config, room overlay) live in `_floorplan_render.py`; resolution is governed there
by `INCHES_PER_METER * DPI`, default **600 px per drawing-metre** and overridable via the
`FLOORPLAN_DPI` environment variable (e.g. `FLOORPLAN_DPI=900 python3 explore_floorplan_styles.py …`).

## Setup

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
```

## Input

A `CAD_DIR` containing, per house, `<name>.dxf` and `<name>.rooms.json` — the sidecar the room
extractor produces. Write renders into a git-ignored location; `renders/` under `CAD_DIR` is the
convention.

## Baseline — faithful "as-is" render with a real CAD renderer (QCAD, local & automatic)

To check the extractor and the styled render against an independent CAD engine, render the source with
[QCAD](https://qcad.org) rather than our own ezdxf pipeline. QCAD's headless `dwg2pdf` renders DXF
faithfully — native ACI layer colours, proper hatch patterns, working text masks — and, unlike ezdxf,
it does not flatten the solid colour-fills (`00_Farben`) to black blocks. DXF support is GPL; no DWG
plugin is needed. **Verified to match the Autodesk Viewer output on the same floor** (see below), so it
is the local, automatic equivalent of that baseline — no upload, no account.

Install QCAD once, then batch-render every plan. Two complementary baselines — generate both to compare:

```bash
bash setup_qcad.sh                          # fetch portable QCAD into tools/.qcad/ (~135 MB, git-ignored)
FLOORPLAN_RASTER_DPI=600 .venv/bin/python render_baselines.py   <CAD_DIR>   # full sheet, per plan
FLOORPLAN_RASTER_DPI=600 .venv/bin/python render_floor_crops.py <CAD_DIR>   # dimensioned single floors
# single plan: append a name filter matching its base name, e.g. ... render_floor_crops.py <CAD_DIR> "" <fragment>
```

- **`render_baselines.py` — full sheet (uncropped).** One `dwg2pdf` per DXF over the whole model space:
  the complete sheet as drawn, everything and every dimension, no cropping. The individual floor is a
  small patch on it — the vector PDF is zoomable, but it is the *context* baseline, not a per-floor image.
- **`render_floor_crops.py` — dimensioned single floors (cropped).** The source stacks the plan several
  times in model space: an upper band of undimensioned trade copies (plumbing/drainage) and a lower band
  of dimensioned architectural floors. This tool 2D-clusters the `00_Wände` polylines into building
  copies, keeps the dimensioned ones, crops each to its wall bbox + margin (site-context layers dropped),
  and renders it — a sharp, dimensioned per-floor sheet. This is the counterpart to the full sheet.

`find_qcad()` locates QCAD via `QCAD_DWG2PDF`, then `dwg2pdf` on `PATH`, then `tools/.qcad/`.

**Every file carries the tool that produced it**, so renders from different engines sit side by side in
one directory for comparison. Output layout under `<CAD_DIR>/renders/baseline/` (git-ignored):

```
<plan>.fullsheet.qcad.pdf   full-sheet QCAD render (vector, zoomable) — everything + all dimensions
<plan>.fullsheet.qcad.png   same, rasterized
<plan>.floor<N>.qcad.pdf    dimensioned single floor, cropped (N = left-to-right among dimensioned copies)
<plan>.floor<N>.qcad.png    same, rasterized
crops/<plan>.floor<N>.ezdxf-crop.dxf   the isolated single-floor DXF (ezdxf), input to QCAD
sharecad/<plan>.<view>.sharecad-*.png  ShareCAD model-space captures (colour / bw / empty layout)
```

`<plan>` is the base name of the source DXF.

`render_floor_faithful.py` (room-cluster single-floor crop) still exists for the per-floor UI use case,
but it is not the baseline path — its room-based window hit the undimensioned plumbing copy.

Notes:
- The trial build prints a nag and waits ~15 s per run (≈20 s/floor total); output is unwatermarked.
- The architect's **per-sheet PDFs cannot be reproduced** from these files: their paper-space layouts
  (`e`=Erdgeschoss, `u`=Keller, `o`=Obergeschoss, `d`=Dachgeschoss, `an/ao/as/aw`=elevations, `q`=section)
  render as empty frames — the viewports that would show one floor are inactive in the supplied DXF/DWG,
  so QCAD, ezdxf and the online ShareCAD viewer all draw only the title block. The plan geometry lives
  only in model space; that is why `render_floor_faithful.py` crops the floor out of the model space
  instead of using a layout.

### ezdxf fallback

If QCAD is unavailable, `.venv/bin/ezdxf draw --background WHITE -o <name>.baseline.pdf <name>.dxf`
gives a rougher render (it flattens solid colour-fills to black) but needs no extra tool.

### Autodesk Viewer — reference

An [Autodesk Viewer](https://viewer.autodesk.com/) export of one floor validated the local QCAD baseline above: same walls,
rooms and stated areas, so QCAD generates the baselines; the export stays outside the repository. One difference: Autodesk renders the whole sheet including
the outer dimension chains, while the QCAD render currently crops them off (they sit on `00_Text` just
outside the floor window) — widen the crop margin in `render_floor_faithful.py` to keep them.

The online [ShareCAD](https://sharecad.org/) viewer was also tried; it only shows the mixed model space
(all floors + sections + site plans, like QCAD's model space) and its `B/W` button just inverts the
background — no per-floor view, so it adds nothing over the local QCAD path.

Uploading a plan to any online viewer sends a customer's floor plan to a third party — the local QCAD
path avoids that entirely.

## `render_floor_faithful.py` — original-fidelity single floor (QCAD)

A faithful render of **one** floor in the architect's own appearance — native ACI colours, correct
lineweights, proper hatch patterns, working text masks. QCAD renders faithfully but only the whole
model space (all floors + sections + elevations + site plans together); this tool first uses ezdxf to
crop one floor's entities into a temporary DXF, then renders that with QCAD.

```bash
export QCAD_DWG2PDF=/tmp/qcad/dwg2pdf   # or put dwg2pdf on PATH
export CAD_DIR=/path/to/cad
.venv/bin/python render_floor_faithful.py <DXF> <ROOMS_JSON> <FLOOR_INDEX> <OUT_DIR> [MARGIN_M]
# e.g. floor 2 = Erdgeschoss:
.venv/bin/python render_floor_faithful.py \
  "$CAD_DIR/<plan>.dxf" \
  "$CAD_DIR/<plan>.rooms.json" \
  2 renders/faithful-floor
```

Writes `<name>.floor<N>.dxf` (the isolated single floor) plus, when QCAD is found, a faithful
`<name>.floor<N>.pdf` (vector) and `.png` (A3 @ 400 dpi, auto-fit + centred). The floor is isolated by
keeping every entity whose bounding box lies within the room cluster plus `MARGIN_M` (default 3.5 m, to
include walls, dimensions and notes); the site-context layers (`00_Grundstück`, `00_Gelände`,
`00_Bestandsgebäude`, drainage) and layout-spanning stray lines (> 30 m) are dropped so QCAD auto-fits
the floor itself. Other floors: pass `FLOOR_INDEX` 1 (Keller), 2 (Erdgeschoss), 3 (Obergeschoss), …

## `render_floorplan_stack.py` — controllable furniture-ghost layers

The `furniture-ghost` look made toggleable: one transparent, pixel-aligned PNG per **semantic group**,
each styled (structure heavy black, furniture/rest ghosted grey, room fills translucent). Stack them in
any image editor and switch groups on/off. The numeric filename prefix is the suggested stacking order
(low = back):

| Prefix | Group | Contents |
|---|---|---|
| `1-rooms` | rooms | extracted rooms (translucent fill + names) |
| `2-other` | other | every geometry layer not in a named group |
| `3-penetrations` | penetrations | breakthroughs / pipe + duct sleeves (`00_Durchbrüche`) |
| `4-furniture` | furniture | `00_Möbel` / `00_Sanitär` / `00_Haustechnik` |
| `5-symbols` | symbols | `00_Symbole` |
| `6-structure` | structure | walls + door/window openings |
| `7-text` | text | all text + dimensions, from every layer |

**Text is grouped by entity type** (`TEXT`/`MTEXT`/`DIMENSION`), not by layer, so labels never pollute
the geometry groups regardless of the layer the architect drew them on. The `other` group catches every
remaining geometry layer (printed on each run), so the union of all groups is the full plan.

```bash
.venv/bin/python render_floorplan_stack.py <DXF> <ROOMS_JSON> <OUT_DIR> [FLOOR_INDEX]
```

## `render_floorplan_layers.py` — raw per-layer stack

Maximum granularity: one transparent, pixel-aligned PNG per **original DXF layer** (the architect's own
layer structure, uniform mono style), plus an extracted-`rooms` layer and a combined `mono`. All PNGs of
a floor share extent and pixel size, so a viewer can stack and toggle them.

```bash
.venv/bin/python render_floorplan_layers.py <CAD_DIR> [NAME_FILTER]
```

## `explore_floorplan_styles.py` — breadth experiment

Renders **one floor of one plan** in many candidate display styles, so the nicest can be chosen later.
Each output file is named by its variant/strategy. Two rendering tools are exercised: the ezdxf
Matplotlib backend (raster PNG) and the ezdxf native SVG backend (vector SVG).

```bash
.venv/bin/python explore_floorplan_styles.py <DXF> <ROOMS_JSON> <OUT_DIR> [FLOOR_INDEX]
```

| Variant | Strategy |
|---|---|
| `mono-technical` | Black on white, architect's own lineweights |
| `dxf-original-colors` | The architect's ACI layer colours |
| `blueprint` | White lines on blueprint blue |
| `dark` | Light lines on dark background |
| `sepia` | Dark brown on cream |
| `hairline` | Uniform thin lines, minimal |
| `sketch-xkcd` | Hand-drawn (Matplotlib xkcd) |
| `heavy-structure` | Faint everything, walls + openings heavy black |
| `walls-openings` | Structural skeleton only |
| `furniture-ghost` | Walls black, furniture ghosted grey |
| `rooms-overlay` | Mono plan + translucent blue extracted rooms + names |
| `rooms-palette` | Faint plan + one colour per extracted room + names |
| `rooms-heatmap-area` | Faint plan + rooms coloured by stated area + names |
| `rooms-blueprint` | Blueprint + translucent room fills + names |
| `rooms-only-schematic` | Extracted room polygons + names + areas, no walls |
| `svg-native-color` | ezdxf native SVG backend, DXF colours (vector) |
| `svg-native-mono` | ezdxf native SVG backend, monochrome (vector) |
