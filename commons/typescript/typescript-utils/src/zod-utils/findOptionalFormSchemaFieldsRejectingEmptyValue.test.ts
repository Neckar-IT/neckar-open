/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { findOptionalFormSchemaFieldsRejectingEmptyValue } from "./findOptionalFormSchemaFieldsRejectingEmptyValue";

/**
 * Tests
 */

describe("findOptionalFormSchemaFieldsRejectingEmptyValue", () => {
  it("reports an optional field that rejects the null an emptied form field writes", () => {
    const schema = z.object({
      accessoryPurchasePrice: z.number().int().optional(),
      validFrom: z.date().optional(),
    });

    expect(findOptionalFormSchemaFieldsRejectingEmptyValue(schema)).toEqual(["accessoryPurchasePrice", "validFrom"]);
  });

  it("reports a nullable field that rejects the undefined of a missing default value", () => {
    const schema = z.object({
      partUnit: z.string().nullable(),
    });

    expect(findOptionalFormSchemaFieldsRejectingEmptyValue(schema)).toEqual(["partUnit"]);
  });

  it("accepts a nullish field", () => {
    const schema = z.object({
      accessoryPurchasePrice: z.number().int().nullish(),
      partUnit: z.string().nullish(),
      validFrom: z.date().nullish(),
    });

    expect(findOptionalFormSchemaFieldsRejectingEmptyValue(schema)).toEqual([]);
  });

  it("leaves required fields alone - they reject both encodings by design", () => {
    const schema = z.object({
      partPurchasePrice: z.number().int().nonnegative(),
      title: z.string(),
      type: z.enum(["now", "scheduled"]),
      withAccessory: z.boolean(),
    });

    expect(findOptionalFormSchemaFieldsRejectingEmptyValue(schema)).toEqual([]);
  });
});
