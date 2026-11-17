/**
 * Imports
 */

import type { z } from "zod";

/**
 * The optional fields of a form schema that accept only one of the two encodings of "empty".
 *
 * A form field reports an emptied input as `null`, while a field left out of `defaultValues` is
 * `undefined` — react-hook-form keeps both. A schema that accepts only one of them rejects the
 * form's own empty state at the object parse and blocks the submit, before any cross-field rule
 * runs. `nullish()` accepts both.
 *
 * Fields that accept neither encoding are required and never reported.
 */
export function findOptionalFormSchemaFieldsRejectingEmptyValue<T extends z.ZodObject>(formSchema: T): string[] {
  return Object.entries<z.ZodType>(formSchema.shape)
    .filter(([, fieldSchema]) => fieldSchema.safeParse(null).success !== fieldSchema.safeParse(undefined).success)
    .map(([fieldName]) => fieldName);
}
