import {
  AdjustableValueAdjustedStringWithTypeType,
  AdjustableValueAutoStringWithTypeType,
  AdjustableValueManualStringWithTypeType,
} from "@neckar-hub/typescript-utils/openapi-types";
import { isEqual } from "lodash";
import type { AdjustableValueSelectValue } from "./AdjustableValueSelect";

/**
 * Destructure type variables to enhance readability
 */
const { Adjusted } = AdjustableValueAdjustedStringWithTypeType;
const { Auto } = AdjustableValueAutoStringWithTypeType;
const { Manual } = AdjustableValueManualStringWithTypeType;

/**
 * Refreshes the calculated side of an adjustable select value where the calculation may have no result
 * and nothing may be overridden yet — the generic counterpart of [recalculatedAdjustableValueText].
 *
 * The calculated value comes from the source the field follows (a sibling form field, a server
 * companion); this function only re-wraps the field around it: with a result, an inherited field
 * follows it (`Auto`) and an override keeps its value while `Manual` is promoted to `Adjusted`; without
 * a result an inherited field becomes `null` and an override drops to `Manual`, since there is no longer
 * a calculation it could be an adjustment of.
 */
export function recalculatedAdjustableValueSelect<T>(
  current: AdjustableValueSelectValue<T> | null,
  calculated: T | null,
): AdjustableValueSelectValue<T> | null {
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
      // An inherited field carried nothing but the calculation that just disappeared.
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
 * Structural equality for adjustable select values — used to avoid form updates that change nothing.
 * The effective and calculated values are compared with a deep equality, so option objects match by
 * content rather than by reference.
 */
export function adjustableValueSelectEquals<T>(a: AdjustableValueSelectValue<T> | null, b: AdjustableValueSelectValue<T> | null): boolean {
  if (a === null || b === null) {
    return a === b;
  }
  if (a.type !== b.type || !isEqual(a.value, b.value)) {
    return false;
  }
  if (a.type === Adjusted && b.type === Adjusted && !isEqual(a.calculated, b.calculated)) {
    return false;
  }
  return true;
}
