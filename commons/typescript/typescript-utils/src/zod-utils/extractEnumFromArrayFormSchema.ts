/**
 * Imports
 */

import type { FieldPath } from "react-hook-form";
import { z } from "zod";
import { extractFieldSchema } from "./extractFieldSchema";
import { unwrapSchema } from "./unwrapSchema";

/**
 * Extracts the enum of the elements of an array field from a zod schema, throws an error if the field is no array of an enum.
 * Unwraps optional, nullable and default around the array and around its element.
 */

export function extractEnumFromArrayFormSchema<T extends z.ZodObject>(formSchema: T, fieldName: FieldPath<z.infer<T>>) {
  const field = extractFieldSchema(formSchema, fieldName);

  if (field === undefined) {
    throw new Error(`Field '${fieldName}' not found in schema.`);
  }

  const array = unwrapSchema(field);
  const element = array instanceof z.ZodArray ? unwrapSchema(array.element as z.ZodType) : undefined;

  if (element instanceof z.ZodEnum) {
    return element.enum;
  } else {
    throw new Error(`Field '${fieldName}' is not an array of an enum.`);
  }
}
