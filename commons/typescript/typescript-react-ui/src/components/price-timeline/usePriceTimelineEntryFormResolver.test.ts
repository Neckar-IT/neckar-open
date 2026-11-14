import { findOptionalFormSchemaFieldsRejectingEmptyValue } from "@neckar-hub/typescript-utils/zod-utils";
import { describe, expect, it } from "vitest";
import {
  priceTimelineEntryUpsertDefaultValues,
  priceTimelineEntryUpsertDefaultValuesFrom,
  priceTimelineEntryUpsertRequestSchema,
} from "./usePriceTimelineEntryFormResolver";

describe("priceTimelineEntryUpsertRequestSchema", () => {
  it("has no optional field that rejects one of the two empty encodings", () => {
    expect(findOptionalFormSchemaFieldsRejectingEmptyValue(priceTimelineEntryUpsertRequestSchema)).toEqual([]);
  });

  it("keeps the price mandatory", () => {
    expect(priceTimelineEntryUpsertRequestSchema.safeParse(priceTimelineEntryUpsertDefaultValues).success).toBe(false);
  });
});

describe("priceTimelineEntryUpsertDefaultValues", () => {
  it("holds the timing fields as the explicit empty value", () => {
    expect(priceTimelineEntryUpsertDefaultValues).toEqual({ type: "now", validFrom: null });
  });
});

describe("priceTimelineEntryUpsertDefaultValuesFrom", () => {
  it("starts from the price in effect", () => {
    expect(priceTimelineEntryUpsertDefaultValuesFrom({ price: 500 })).toEqual({ type: "now", validFrom: null, price: 500 });
  });

  it("leaves the price empty when no entry is in effect", () => {
    expect(priceTimelineEntryUpsertDefaultValuesFrom({ price: null })).toEqual({ type: "now", validFrom: null });
  });
});
