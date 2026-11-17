import { type QueryKey } from "@neckar-hub/typescript-react-query";
import { type FieldValues, type UseFormReturn } from "react-hook-form";
import { type UseConnectCompanionOptions, useConnectCompanion } from "./useConnectCompanion";

/**
 * Builds the query key for a companion request: `[entity, entityId, "companion", aspect, input]`.
 *
 * One convention for every companion aspect, so identical inputs share the cache entry.
 */
export function companionQueryKey(entity: string, entityId: string, aspect: string, input: unknown): QueryKey {
  return [entity, entityId, "companion", aspect, input];
}

/**
 * Connects a form to a companion endpoint (form-companion pattern): reads the companion's inputs from
 * the form (the caller watches them), fetches debounced, and applies the result back - typically into
 * a form field.
 *
 * The companion only runs once the form is dirty: until the user touches the form, the loaded state is
 * already consistent, so no request is needed. Everything else is [useConnectCompanion].
 */
export function useConnectFormCompanion<SchemaType extends FieldValues, InputType, DerivedType>(
  form: UseFormReturn<SchemaType>,
  options: Omit<UseConnectCompanionOptions<InputType, DerivedType>, "enabled">,
): void {
  useConnectCompanion({
    ...options,
    enabled: form.formState.isDirty,
  });
}
