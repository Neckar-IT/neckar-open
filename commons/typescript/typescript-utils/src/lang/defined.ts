/**
 * Returns true if the value is not:
 * * undefined
 * * null
 * * empty string (after `.trim()`)
 * * NaN
 *
 * Empty objects, arrays, maps and sets are still treated as defined — check
 * `array.length === 0` / `map.size === 0` / etc. explicitly when you need that.
 */
export function isDefined<T>(value: T): value is NonNullable<T> {
  if (value === null || value === undefined) {
    return false;
  }

  if (typeof value === "string" && value.trim() === "") return false;

  if (Number.isNaN(value)) {
    return false;
  }

  return true;
}

/**
 * Inverse of isDefined
 */
export function isNotDefined<T>(value: T): value is Extract<T, null | undefined | string | number> {
  return (
    value === null ||
    value === undefined ||
    (typeof value === "string" && value.trim() === "") ||
    (typeof value === "number" && Number.isNaN(value))
  );
}
