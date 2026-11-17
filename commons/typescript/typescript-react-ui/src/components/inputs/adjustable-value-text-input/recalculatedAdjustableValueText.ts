import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import type { AdjustableValueTextExtended } from "./AdjustableValueTextInput";

/**
 * Destructure type variables to enhance readability
 */
const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

/**
 * Refreshes the calculated side of an adjustable text value where the calculation may have no result and
 * nothing may be stored yet - the TypeScript counterpart of the Kotlin `AdjustableValue<T>?.recalculated`.
 *
 * The calculated value itself always comes from the server (form-companion pattern) - this
 * function only applies it to the wrapper: with a result, `Auto` follows it, an override keeps its
 * value, `Manual` is promoted to `Adjusted`. Without a result only a typed value survives - as `Manual`,
 * because there is no longer a calculation it could be an adjustment of.
 */
export function recalculatedAdjustableValueText(
  current: AdjustableValueTextExtended | null,
  calculated: string | null,
): AdjustableValueTextExtended | null {
  if (calculated !== null) {
    switch (current?.type) {
      case undefined:
      case Auto:
        return { type: Auto, value: calculated };
      case Adjusted:
      case Manual:
        return { type: Adjusted, calculated, value: current.value };
      default:
        throw new Error(`Unexpected value in Switch Statement: current.type`);
    }
  }

  switch (current?.type) {
    case undefined:
    case Auto:
      // Auto carried nothing but the calculation that just disappeared, and null stays null.
      return null;
    case Adjusted:
      return { type: Manual, value: current.value };
    case Manual:
      return current;
    default:
      throw new Error(`Unexpected value in Switch Statement: current.type`);
  }
}

/**
 * Structural equality for adjustable text values - used to avoid form updates that change nothing.
 */
export function adjustableValueTextEquals(a: AdjustableValueTextExtended | null, b: AdjustableValueTextExtended | null): boolean {
  if (a === null || b === null) {
    return a === b;
  }
  if (a.type !== b.type || a.value !== b.value) {
    return false;
  }
  if (a.type === Adjusted && b.type === Adjusted && a.calculated !== b.calculated) {
    return false;
  }
  return true;
}
