/**
 * Imports
 */

import { customAlphabet } from "nanoid";

/**
 * Generate Nano Id
 */

export function generateNanoId(length = 10, alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"): string {
  const generator = customAlphabet(alphabet);
  const nanoId = generator(length);
  return nanoId;
}
