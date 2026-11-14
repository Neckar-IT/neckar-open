/**
 * The key that identifies a feature flag.
 * Keys must not contain commas as they are used in comma-separated encoding.
 *
 * This is simply a string type alias for clarity.
 */
export type Key = string;

/**
 * Validates that a key does not contain a comma.
 * @throws Error if the value contains a comma
 */
export function validateKey(value: string): void {
  if (value.includes(",")) {
    throw new Error('Key must not contain ","');
  }
}
