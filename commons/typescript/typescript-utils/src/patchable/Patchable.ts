/**
 * Patchable type and utilities for optional field patching.
 *
 * This type mirrors the Kotlin sealed interface `it.neckar.rest.patch.Patchable<T>`.
 *
 * OpenAPI does not support generics - it can only describe concrete types. When we export
 * `Patchable<String>` to OpenAPI, it generates a concrete type `PatchableString`.
 * For each type parameter (String, Url, Int, etc.), a separate concrete type is generated.
 *
 * To provide a proper generic TypeScript type that matches the Kotlin semantics, we define
 * this type manually. The concrete generated types (like `PatchableString`) are
 * compatible with this generic type.
 */

/**
 * Generic Patchable type that can hold any value type.
 *
 * Variants:
 * - Update: Field has a new value
 * - Keep: Field should not be modified
 * - Delete: Field should be set to null/deleted
 */
export type Patchable<T> = { type: "Update"; value: T } | { type: "Keep" } | { type: "Delete" };

/**
 * Factory object for creating Patchable instances.
 *
 * @example
 * import { Patchable } from "@neckar-hub/typescript-utils/patchable";
 *
 * const updatePatch = Patchable.update("new value");
 * const keepPatch = Patchable.keep();
 * const deletePatch = Patchable.delete();
 * const fromValue = Patchable.from(maybeValue);
 */
export const Patchable = {
  /**
   * Creates an Update patch value with the given value.
   *
   * @example
   * const patch = Patchable.update("new name");
   * // { type: "Update", value: "new name" }
   */
  update<T>(value: T): Patchable<T> {
    return { type: "Update", value };
  },

  /**
   * Creates a Keep patch value (field should not be modified).
   *
   * @example
   * const patch = Patchable.keep();
   * // { type: "Keep" }
   */
  keep<T>(): Patchable<T> {
    return { type: "Keep" };
  },

  /**
   * Creates a Delete patch value (field should be set to null/deleted).
   *
   * @example
   * const patch = Patchable.delete();
   * // { type: "Delete" }
   */
  delete<T>(): Patchable<T> {
    return { type: "Delete" };
  },

  /**
   * Creates a Patchable from a nullable value.
   * - `undefined` -> Keep (field was not specified, keep existing value)
   * - `null` -> Delete (field was explicitly cleared)
   * - non-null -> Update (field has a new value)
   *
   * This semantic is useful for form handling where:
   * - An untouched field (`undefined`) should keep its current value
   * - An explicitly cleared field (`null`) should be deleted
   * - A filled field should update to the new value
   *
   * @example
   * Patchable.from("value")   // { type: "Update", value: "value" }
   * Patchable.from(null)      // { type: "Delete" }
   * Patchable.from(undefined) // { type: "Keep" }
   */
  from<T>(value: T | null | undefined): Patchable<T> {
    if (value === undefined) {
      return { type: "Keep" };
    }
    if (value === null) {
      return { type: "Delete" };
    }
    return { type: "Update", value };
  },
};

/**
 * Checks if the value is a Patchable.Update variant.
 */
export function isPatchableUpdate<T>(value: Patchable<T>): value is { type: "Update"; value: T } {
  return value.type === "Update";
}

/**
 * Checks if the value is a Patchable.Keep variant.
 */
export function isPatchableKeep<T>(value: Patchable<T>): value is { type: "Keep" } {
  return value.type === "Keep";
}

/**
 * Checks if the value is a Patchable.Delete variant.
 */
export function isPatchableDelete<T>(value: Patchable<T>): value is { type: "Delete" } {
  return value.type === "Delete";
}

/**
 * Extracts the value from a Patchable, applying the patch semantics:
 * - Update: returns the new value
 * - Keep: returns the current value
 * - Delete: returns null
 */
export function getPatchableValue<T>(patchValue: Patchable<T>, currentValue: T | null): T | null {
  switch (patchValue.type) {
    case "Update":
      return patchValue.value;
    case "Keep":
      return currentValue;
    case "Delete":
      return null;
    default:
      throw new Error(`Unexpected value in Switch Statement: ${(patchValue as { type: string }).type}`);
  }
}

/**
 * Creates a Patchable from a nullable value. Short alias for `Patchable.from()`.
 *
 * - `undefined` -> Keep (field was not specified, keep existing value)
 * - `null` -> Delete (field was explicitly cleared)
 * - non-null -> Update (field has a new value)
 *
 * @example
 * import { patchable } from "@neckar-hub/typescript-utils/patchable";
 *
 * patchable("new name")  // { type: "Update", value: "new name" }
 * patchable(null)        // { type: "Delete" }
 * patchable(undefined)   // { type: "Keep" }
 *
 * // Typical form usage:
 * const request = {
 *   name: patchable(formData.name),
 *   avatar: patchable(formData.avatar),
 * };
 */
export function patchable<T>(value: T | null | undefined): Patchable<T> {
  return Patchable.from(value);
}
