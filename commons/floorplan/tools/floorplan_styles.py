"""Per-layer render variants for the floor-plan pipeline — the columns of the layer × variant matrix.

There is deliberately NO global "clean" / "teal-gray" preset: every layer is rendered in each variant
below, and the composition is chosen per layer in the UI. A variant is only appearance + a text switch,
never a layer-name rule — so nothing here depends on the architect's layer scheme.

A variant:
  name    file/id
  color   hex the layer is drawn in, or None for the layer's original ACI colours
  notext  True -> drop annotation on this layer (TEXT/MTEXT/DIMENSION/LEADER/…) and the orphan leader
          lines that pointed at removed text; False -> keep everything on the layer
"""

TEAL = "#1f4e5f"
LIGHT_GRAY = "#c4c8ca"
MID_GRAY = "#9aa0a3"
BLACK = "#1a1a1a"
CREAM = "#f5f2ec"

ANNOTATION_TYPES = ["TEXT", "MTEXT", "ATTRIB", "ATTDEF", "DIMENSION", "ARC_DIMENSION",
                    "LEADER", "MLEADER", "MULTILEADER", "TOLERANCE"]  # universal CAD annotation types

_COLORS = [("original", None), ("teal", TEAL), ("gray", LIGHT_GRAY), ("black", BLACK)]

# Default preset = the initial state of the matrix. It is DATA, not engine logic: layer-name patterns
# (fnmatch) map to a variant, first match wins, and everything unmatched falls back to `default`. So it
# reproduces a liked look yet stays general — a new/unknown layer just gets the grey fallback, and a
# different office adapts the patterns once (or exports its own from the UI). Nothing is hard-coded in
# the renderer; drop in a floorplan-preset.json next to the input to override.
DEFAULT_PRESET = {
    "global": {"rooms": True},
    "default": {"color": "gray", "text_off": True},          # everything not matched below
    "rules": [
        {"match": ["*Bemassung*", "*Bemaßung*", "*Dimension*"], "off": True},   # dimensions hidden
        {"match": ["*Entwäss*", "*Drain*"], "color": "gray", "text_off": True},  # drainage grey (before *Text*)
        {"match": ["*Wand*", "*Wände*", "*Mauer*", "*Wall*",
                   "*Öffnung*", "*Opening*", "*Schraffur*", "*Hatch*", "*Text*"],
         "color": "black", "text_off": True},                # structure emphasised
    ],
}

# Every colour, once as-is and once without annotation ("-notext"). The pipeline skips the -notext
# copy for a layer that carries no text (it would be identical), so no redundant files are written.
LAYER_VARIANTS = (
    [{"name": name, "color": color, "notext": False} for name, color in _COLORS]
    + [{"name": f"{name}-notext", "color": color, "notext": True} for name, color in _COLORS]
)
