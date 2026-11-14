/**
 * Imports
 */

import type { FieldPath } from "react-hook-form";
import { z } from "zod";
import { extractFieldSchema } from "./extractFieldSchema";
import { unwrapSchema } from "./unwrapSchema";

/**
 * Extracts the enum for a given field path from a zod schema, throws an error if the field path is not an enum
 *
 * @param formSchema - The zod Schema
 * @param fieldName - The name of the field (i.e. the key in the schema)
 *
 * TODO: Make sure that only valid enum field paths can be passed.
 * ChatGPT suggested the following, but this has a type error although the functionality is correct:
 * ```
 * type EnumFieldPaths<T extends z.ZodType, Prefix extends string = ""> =
 *   T extends ZodObject<infer Shape>
 *     ? {
 *         [K in keyof Shape]: EnumFieldPaths<Shape[K], `${Prefix}${Prefix extends "" ? "" : "."}${Extract<K, string>}`>;
 *       }[keyof Shape]
 *     : T extends ZodEnum<any>
 *       ? Prefix
 *       : never;
 * ```
 */

export function extractEnumFromFormSchema<T extends z.ZodObject>(formSchema: T, fieldName: FieldPath<z.infer<T>>) {
  const field = extractFieldSchema(formSchema, fieldName);

  if (field === undefined) {
    throw new Error(`Field '${fieldName}' not found in schema.`);
  }

  const unwrapped = unwrapSchema(field);

  if (unwrapped instanceof z.ZodEnum) {
    return unwrapped.enum;
  } else {
    throw new Error(`Field '${fieldName}' is not an enum.`);
  }
}
