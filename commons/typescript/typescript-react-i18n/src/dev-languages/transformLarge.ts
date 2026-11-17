/**
 * Start and end markers for truncation detection.
 * If a rendered string is missing its start marker, it was clipped at the beginning.
 * If missing the end marker, it was clipped at the end.
 */
const START_MARKER = "«";
const END_MARKER = "»";

/**
 * Repeating fill pattern combining letters with diacritics above and descenders below.
 * Top clipping detection: Å (ring), Ŵ (circumflex), Ñ (tilde) — three distinct shapes
 * Bottom clipping detection: g (loop), j (curve + dot), p (straight stroke) — three distinct descenders
 */
const FILL_PATTERN = "ÅŴÑgjp";

/**
 * Inflation heuristic: targetLength = MIN_ADDITION + originalLength * MULTIPLIER
 * Short texts grow dramatically (buttons, labels), long texts grow moderately.
 *
 * Examples: "OK" (2) → 15 (7.5x), "Speichern" (9) → 26 (2.9x),
 *           "Autor erfolgreich gespeichert." (30) → 57 (1.9x)
 */
const MIN_ADDITION = 12;
const MULTIPLIER = 1.5;

/**
 * Large-language transformation: replaces a fully-interpolated translation
 * string with an inflated run of `ÅŴÑgjp` characters wrapped in `«…»` markers.
 *
 * Applied by the Neckar `useTranslation` / `useInitializeEmbeddedTranslations`
 * wrappers on every `t(key, …)` call when `i18n.language === "large"`. Because
 * the input is the already-interpolated string, placeholder preservation is
 * unnecessary — a finished translation is a single segment.
 *
 * Purpose: surface layout problems — text overflow, truncation, too-narrow
 * columns — by rendering a visually heavy string of the expected magnitude:
 * - `«` / `»` detect horizontal truncation in either direction
 * - `ÅŴÑ` (diacritics) detect top clipping
 * - `gjp` (descenders) detect bottom clipping
 */
export function transformLarge(text: string): string {
  const targetLength = MIN_ADDITION + Math.ceil(text.length * MULTIPLIER);
  return `${START_MARKER}${repeatPattern(FILL_PATTERN, targetLength)}${END_MARKER}`;
}

function repeatPattern(pattern: string, targetLength: number): string {
  if (targetLength <= 0) {
    return "";
  }
  const fullRepeats = Math.ceil(targetLength / pattern.length);
  return pattern.repeat(fullRepeats).slice(0, targetLength);
}
