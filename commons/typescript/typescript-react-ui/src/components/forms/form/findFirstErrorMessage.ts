/**
 * Imports
 */

import isPlainObject from "lodash/isPlainObject";

/**
 * Keys to ignore when searching for error messages
 */
const IGNORED_ERROR_KEYS = new Set(["message", "type", "ref", "types"]);

/**
 * Helper function that returns the first error message found in either the field itself or in any nested descendants.
 *
 * It will look for a "message" property on the object, then look into a "types" property (if present),
 * and finally recursively search all other properties (ignoring react-hook-form metadata keys).
 *
 * @param value The value to search for an error message
 * @returns The first error message found, or undefined if none is found
 */
export function findFirstErrorMessage(value: unknown): string | undefined {
  if (value == null) {
    return undefined;
  }

  if (typeof value === "string") {
    return value;
  }

  if (Array.isArray(value)) {
    for (const entry of value) {
      const message = findFirstErrorMessage(entry);
      if (message) {
        return message;
      }
    }
    return undefined;
  }

  if (isPlainObject(value)) {
    const record = value as Record<string, unknown>;

    if (typeof record.message === "string") {
      return record.message;
    }
    if (record.message != null) {
      // eslint-disable-next-line @typescript-eslint/no-base-to-string
      return String(record.message);
    }

    const typesMessage = findFirstErrorMessage(record.types);
    if (typesMessage) {
      return typesMessage;
    }

    for (const key of Object.keys(record)) {
      if (IGNORED_ERROR_KEYS.has(key)) {
        continue;
      }
      const nestedMessage = findFirstErrorMessage(record[key]);
      if (nestedMessage) {
        return nestedMessage;
      }
    }
  }

  return undefined;
}
