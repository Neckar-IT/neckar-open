/**
 * Removes floating-point imprecision introduced by dividing by a small
 * `conversionFactor`. For example:
 *
 *     147.11 / 0.01 === 14711.000000000002   // JS float drift
 *
 * A downstream `z.number().int()` schema then (correctly) rejects the value.
 * Rounding to ten significant decimals via `toFixed(10)` + `parseFloat`
 * collapses the drift back to the nearest exact representable value, which
 * for integer-storage units (cents, grams, millimetres, …) is the integer
 * the user meant, while genuinely fractional storage (e.g. 3.14 m) stays
 * intact because 10 digits of precision far exceed any realistic
 * display/storage pairing.
 *
 * 10 digits is well below `Number.EPSILON`'s rounding horizon (~2.22e-16
 * relative) and well above any fractionDigits we actually use in forms.
 */
export function normalizeFloatDrift(value: number): number {
  return parseFloat(value.toFixed(10));
}
