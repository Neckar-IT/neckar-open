/**
 * PatchableRequired type and utilities for required (non-nullable) field patching.
 *
 * This type mirrors the Kotlin sealed interface `it.neckar.rest.patch.PatchableRequired<T>`.
 *
 * Unlike `Patchable`, this type does NOT have a `Delete` variant because
 * deleting (setting to null) is not valid for required/non-nullable fields.
 *
 * OpenAPI does not support generics, so we define this type manually to provide
 * proper generic TypeScript semantics that match the Kotlin implementation.
 */

/**
 * Generic PatchableRequired type for non-nullable/required fields.
 *
 * Variants:
 * - Update: Field has a new value
 * - Keep: Field should not be modified
 *
 * Note: No Delete variant - required fields cannot be deleted.
 */
export type PatchableRequired<T> = { type: "Update"; value: T } | { type: "Keep" };

/**
 * Factory object for creating PatchableRequired instances.
 * Note: There is no `delete()` method because required fields cannot be deleted.
 *
 * @example
 * import { PatchableRequired } from "@neckar-hub/typescript-utils/patchable";
 *
 * const updatePatch = PatchableRequired.update("new value");
 * const keepPatch = PatchableRequired.keep();
 * const fromValue = PatchableRequired.from(maybeValue);
 */
export const PatchableRequired = {
  /**
   * Creates an Update patch value for required fields.
   *
   * @example
   * const patch = PatchableRequired.update("new name");
   * // { type: "Update", value: "new name" }
   */
  update<T>(value: T): PatchableRequired<T> {
    return { type: "Update", value };
  },

  /**
   * Creates a Keep patch value for required fields.
   *
   * @example
   * const patch = PatchableRequired.keep();
   * // { type: "Keep" }
   */
  keep<T>(): PatchableRequired<T> {
    return { type: "Keep" };
  },

  /**
   * Creates a PatchableRequired from a value.
   * - `undefined` -> Keep (field was not specified, keep existing value)
   * - non-undefined -> Update (field has a new value)
   *
   * Note: Unlike `Patchable.from()`, `null` is treated as an Update (not Delete),
   * because required fields cannot be deleted/set to null.
   *
   * @example
   * PatchableRequired.from("value")   // { type: "Update", value: "value" }
   * PatchableRequired.from(undefined) // { type: "Keep" }
   */
  from<T>(value: T | undefined): PatchableRequired<T> {
    if (value === undefined) {
      return { type: "Keep" };
    }
    return { type: "Update", value };
  },
};

/**
 * Checks if the value is a PatchableRequired.Update variant.
 */
export function isPatchableRequiredUpdate<T>(value: PatchableRequired<T>): value is { type: "Update"; value: T } {
  return value.type === "Update";
}

/**
 * Checks if the value is a PatchableRequired.Keep variant.
 */
export function isPatchableRequiredKeep<T>(value: PatchableRequired<T>): value is { type: "Keep" } {
  return value.type === "Keep";
}

/**
 * Extracts the value from a PatchableRequired, applying the patch semantics:
 * - Update: returns the new value
 * - Keep: returns the current value
 *
 * Note: Unlike Patchable, there is no Delete variant.
 */
export function getPatchableRequiredValue<T>(patchValue: PatchableRequired<T>, currentValue: T): T {
  switch (patchValue.type) {
    case "Update":
      return patchValue.value;
    case "Keep":
      return currentValue;
    default:
      throw new Error(`Unexpected value in Switch Statement: patchValue.type`);
  }
}

/**
 * Creates a PatchableRequired from a value. Short alias for `PatchableRequired.from()`.
 *
 * - `undefined` -> Keep (field was not specified, keep existing value)
 * - non-undefined -> Update (field has a new value)
 *
 * Note: Unlike `patchable()`, `null` is treated as an Update (not Delete),
 * because required fields cannot be deleted/set to null.
 *
 * @example
 * import { patchableRequired } from "@neckar-hub/typescript-utils/patchable";
 *
 * patchableRequired("new name")  // { type: "Update", value: "new name" }
 * patchableRequired(undefined)   // { type: "Keep" }
 */
export function patchableRequired<T>(value: T | undefined): PatchableRequired<T> {
  return PatchableRequired.from(value);
}
