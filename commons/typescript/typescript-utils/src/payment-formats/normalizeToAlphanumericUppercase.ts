/** Mirrors the Kotlin function of the same name in `it.neckar.customer.company`: keeps every UTF-16 unit that is a letter or digit, then uppercases. */
export function normalizeToAlphanumericUppercase(input: string): string {
  return input
    .split("")
    .filter((unit) => /[\p{L}\p{Nd}]/u.test(unit))
    .join("")
    .toUpperCase();
}
