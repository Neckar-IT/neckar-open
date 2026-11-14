/**
 * Imports
 */

import { type FieldPath } from "react-hook-form";
import { z } from "zod";
import { unwrapSchema } from "./unwrapSchema";

/**
 * Utility Function to extract a field schema from a nested zod object
 *
 * TODO: Create a zod type to make sure that only valid field paths can be passed in (replace FieldPath from react-hook-form)
 * TODO: should also be able to handle top-level discriminated unions
 */

export function extractFieldSchema<T extends z.ZodObject>(formSchema: T, fieldName: FieldPath<z.infer<T>>) {
  const keys = fieldName.split(".");
  let current: z.ZodType = unwrapSchema(formSchema);

  for (const [index, key] of keys.entries()) {
    current = unwrapSchema(current);

    /**
     * Handle discriminated unions
     */
    if (current instanceof z.ZodDiscriminatedUnion) {
      // Collect possible inner schemas for this field
      const optionSchemas = Array.from(current.options.values());
      const extracted = optionSchemas
        .map((opt): z.ZodType | undefined => {
          try {
            if (opt instanceof z.ZodObject) {
              return extractFieldSchema(opt, keys.slice(index).join("."));
            } else {
              return undefined;
            }
          } catch {
            return undefined;
          }
        })
        .filter((v): v is z.ZodType => v !== undefined);

      const [firstExtracted] = extracted;
      if (firstExtracted === undefined) throw new Error(`Field "${fieldName}" not found in any union variant.`);
      if (extracted.length === 1) return firstExtracted; // unwrap single match
      return z.union(extracted);
    }

    /**
     * Handle regular unions
     */
    if (current instanceof z.ZodUnion) {
      const extracted = current.def.options
        .map((opt): z.ZodType | undefined => {
          try {
            if (opt instanceof z.ZodObject) {
              return extractFieldSchema(opt, keys.slice(index).join("."));
            } else {
              return undefined;
            }
          } catch {
            return undefined;
          }
        })
        .filter((v): v is z.ZodType => v !== undefined);

      const [firstExtracted] = extracted;
      if (firstExtracted === undefined) throw new Error(`Field "${fieldName}" not found in any union option.`);
      if (extracted.length === 1) return firstExtracted;
      return z.union(extracted);
    }

    /**
     * Handle arrays
     */
    if (current instanceof z.ZodArray) {
      if (!/^\d+$/.test(key)) {
        throw new Error(`Expected numeric index for array, got "${key}" at "${keys.slice(0, index + 1).join(".")}"`);
      }

      const elementSchema = unwrapSchema(current.element as z.ZodType);

      // If the path ends at the index (e.g. "users.0"), return the element schema.
      if (index === keys.length - 1) {
        return elementSchema;
      }

      // Otherwise, consume the index and keep walking on the element schema.
      current = elementSchema;
      continue;
    }

    if (!(current instanceof z.ZodObject)) {
      throw new Error(`Path "${keys.slice(0, index).join(".")}" is not an object schema.`);
    }

    const shape = current.shape;
    const field = shape[key] as z.ZodType | undefined;

    if (!field) {
      throw new Error(`Property "${key}" not found in schema path "${keys.slice(0, index + 1).join(".")}".`);
    }

    if (index === keys.length - 1) {
      // Final key: return field as-is (including optional/nullable wrappers)
      return field;
    } else {
      current = field;
    }
  }

  throw new Error(`Invalid field path: ${fieldName}`);
}
