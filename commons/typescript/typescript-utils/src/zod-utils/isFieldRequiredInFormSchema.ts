/**
 * Imports
 */

import type { FieldPath } from "react-hook-form";
import type { z } from "zod";
import { extractFieldSchema } from "./extractFieldSchema";

/**
 * Determines whether a field is required (not null, undefined, empty string or empty list) in the given form schema
 *
 * @param formSchema - The zod Schema
 * @param fieldName - The name of the field (i.e. the key in the schema)
 *
 * @returns boolean
 */
export function isFieldRequiredInFormSchema<T extends z.ZodObject>(formSchema: T, fieldName: FieldPath<z.infer<T>>): boolean {
  const field = extractFieldSchema(formSchema, fieldName);

  const isOptional = field?.safeParse(undefined).success;
  const isNullable = field?.safeParse(null).success;
  const canBeEmpty = field?.safeParse("").success;
  const canBeEmptyList = field?.safeParse([]).success;

  return !isOptional && !isNullable && !canBeEmpty && !canBeEmptyList;
}
