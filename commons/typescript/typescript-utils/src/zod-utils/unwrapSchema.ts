/**
 * Imports
 */

import { z } from "zod";

/**
 * Utility to unwrap nested schema types (optional, nullable, default, effects)
 */

export function unwrapSchema(schema: z.ZodType): z.ZodType {
  while (schema instanceof z.ZodOptional || schema instanceof z.ZodNullable || schema instanceof z.ZodDefault) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    schema = (schema as any)._def.innerType ?? (schema as any)._def.schema;
  }
  return schema;
}
