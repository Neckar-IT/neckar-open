import { type QueryKey } from "@neckar-hub/typescript-react-query";
import { type FieldValues, type UseFormReturn } from "react-hook-form";
import { type AdjustableValueTextExtended } from "../components/inputs/adjustable-value-text-input/AdjustableValueTextInput";
import {
  adjustableValueTextEquals,
  recalculatedAdjustableValueText,
} from "../components/inputs/adjustable-value-text-input/recalculatedAdjustableValueText";
import { useConnectFormCompanion } from "./useConnectFormCompanion";

/**
 * Every Orval envelope of a display-name companion endpoint is assignable to this.
 */
export type DisplayNameCompanionResponse = { type: "good"; data: { calculated: string | null } } | { type: `bad/${string}` };

export interface DisplayNameFormValues extends FieldValues {
  displayName: AdjustableValueTextExtended | null;
}

export interface UseConnectDisplayNameCompanionOptions<InputType extends Record<string, string | null>> {
  /**
   * The name fields the calculation reads, as the form holds them
   */
  input: InputType;
  /**
   * Cache key of the companion request for the debounced input
   */
  queryKey: (input: InputType) => QueryKey;
  /**
   * The generated client call of `…/{id}/companion/display-name`
   */
  fetch: (input: InputType) => Promise<DisplayNameCompanionResponse>;
}

/**
 * Connects a form to its display-name companion and writes the calculated name into `displayName`.
 *
 * A `bad/*` response is ignored: null is a valid calculated name, "no answer" is not. A recalculated value
 * equal to the current one writes nothing.
 */
export function useConnectDisplayNameCompanion<SchemaType extends DisplayNameFormValues, InputType extends Record<string, string | null>>(
  form: UseFormReturn<SchemaType>,
  { input, queryKey, fetch }: UseConnectDisplayNameCompanionOptions<InputType>,
): void {
  // react-hook-form cannot resolve the path "displayName" of a generic schema.
  const displayNameForm = form as unknown as UseFormReturn<DisplayNameFormValues>;

  useConnectFormCompanion<SchemaType, InputType, string | null>(form, {
    input,
    inputEquals: (left, right) => Object.keys(left).every((key) => left[key] === right[key]),
    queryKey,
    fetchValue: async (companionInput) => {
      const response = await fetch(companionInput);
      if (response.type !== "good") return undefined;
      return response.data.calculated;
    },
    applyValue: (calculated) => {
      const current = displayNameForm.getValues("displayName") ?? null;
      const next = recalculatedAdjustableValueText(current, calculated);
      if (adjustableValueTextEquals(current, next)) return;

      displayNameForm.setValue("displayName", next);
    },
  });
}
