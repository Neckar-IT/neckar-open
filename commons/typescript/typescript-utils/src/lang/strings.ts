export function isNonEmptyString(value: unknown): value is string {
  return typeof value === "string" && value.trim() !== "";
}

/**
 * Splits a string by whitespace into non-blank tokens.
 * Returns undefined for undefined/blank input.
 */
export function splitByWhitespace(text: string | undefined): string[] | undefined {
  if (text === undefined) return undefined;
  const tokens = text.trim().split(/\s+/).filter(Boolean);
  return tokens.length > 0 ? tokens : undefined;
}
