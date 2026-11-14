/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { isFieldRequiredInFormSchema } from "./isFieldRequiredInFormSchema";

/**
 * Tests
 */

describe("isFieldRequiredInFormSchema", () => {
  it("should return false for optional, nullable and empty fields", () => {
    const schema = z.object({
      optionalField: z.string().optional(),
      nullableField: z.string().nullable(),
      emptyAllowedField: z.string(),
    });

    expect(isFieldRequiredInFormSchema(schema, "optionalField")).toBe(false);
    expect(isFieldRequiredInFormSchema(schema, "nullableField")).toBe(false);
    expect(isFieldRequiredInFormSchema(schema, "emptyAllowedField")).toBe(false);
  });

  it("should return true for required (not nullable, not optional, not empty) fields", () => {
    const schema = z.object({
      requiredField: z.string().nonempty(),
    });

    expect(isFieldRequiredInFormSchema(schema, "requiredField")).toBe(true);
  });

  it("should handle nested objects", () => {
    const schema = z.object({
      nested: z.object({
        requiredNestedField: z.string().nonempty(),
        optionalNestedField: z.string().optional(),
      }),
    });

    expect(isFieldRequiredInFormSchema(schema, "nested.requiredNestedField")).toBe(true);
    expect(isFieldRequiredInFormSchema(schema, "nested.optionalNestedField")).toBe(false);
  });

  it("should return false for number fields with nullish, nullable, or optional", () => {
    const schema = z.object({
      nullishNumber: z.number().nullish(),
      nullableNumber: z.number().nullable(),
      optionalNumber: z.number().optional(),
      nullishInt: z.number().int().nullish(),
    });

    expect(isFieldRequiredInFormSchema(schema, "nullishNumber")).toBe(false);
    expect(isFieldRequiredInFormSchema(schema, "nullableNumber")).toBe(false);
    expect(isFieldRequiredInFormSchema(schema, "optionalNumber")).toBe(false);
    expect(isFieldRequiredInFormSchema(schema, "nullishInt")).toBe(false);
  });

  it("should return true for required number fields", () => {
    const schema = z.object({
      requiredNumber: z.number(),
      requiredInt: z.number().int(),
    });

    expect(isFieldRequiredInFormSchema(schema, "requiredNumber")).toBe(true);
    expect(isFieldRequiredInFormSchema(schema, "requiredInt")).toBe(true);
  });

  it("should return true only for a list field that rejects the empty list", () => {
    const schema = z.object({
      emptyAllowedList: z.array(z.string()),
      nonEmptyList: z.array(z.string()).min(1),
    });

    expect(isFieldRequiredInFormSchema(schema, "emptyAllowedList")).toBe(false);
    expect(isFieldRequiredInFormSchema(schema, "nonEmptyList")).toBe(true);
  });

  it("should handle deeply nested objects", () => {
    const schema = z.object({
      level1: z.object({
        level2: z.object({
          requiredField: z.string().nonempty(),
          optionalField: z.string().optional(),
        }),
      }),
    });

    expect(isFieldRequiredInFormSchema(schema, "level1.level2.requiredField")).toBe(true);
    expect(isFieldRequiredInFormSchema(schema, "level1.level2.optionalField")).toBe(false);
  });
});
