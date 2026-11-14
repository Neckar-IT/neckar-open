"""Guard against ezdxf's font-metric divide-by-zero (import for side effect).

ezdxf's ``TTFontRenderer.get_scaling_factor`` divides by the measured cap height of the resolved
font file (the height of its "A" glyph). System fonts without a proper Latin "A" — e.g. the Greek
display font ``GFSSolomos.otf``, which MTEXT inline font overrides can resolve to — measure a cap
height of 0 and kill the whole render with a ``ZeroDivisionError`` (observed with
autodesk-sample-bygginga-plan.dwg). This module patches every class in ``ezdxf.fonts.ttfonts``
exposing ``get_scaling_factor`` to fall back to a factor of 1.0 for such degenerate fonts, so the
affected text renders at nominal size instead of crashing the pipeline.
"""
import ezdxf.fonts.ttfonts as _ttfonts


def _install():
    for name in dir(_ttfonts):
        cls = getattr(_ttfonts, name)
        if isinstance(cls, type) and "get_scaling_factor" in vars(cls):
            original = cls.get_scaling_factor

            def guarded(self, cap_height, _original=original):
                if getattr(self.font_measurements, "cap_height", 0):
                    return _original(self, cap_height)
                return 1.0

            cls.get_scaling_factor = guarded


_install()
