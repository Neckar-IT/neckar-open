import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { findOptionalFormSchemaFieldsRejectingEmptyValue } from "@neckar-hub/typescript-utils/zod-utils";
import { renderHook } from "@testing-library/react";
import { createElement, type ReactNode } from "react";
import { describe, expect, it } from "vitest";
import {
  priceTimelineEntryUpsertDefaultValues,
  priceTimelineEntryUpsertDefaultValuesFrom,
  priceTimelineEntryUpsertRequestSchema,
  usePriceTimelineEntryFormResolver,
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

describe("usePriceTimelineEntryFormResolver", () => {
  it("reports a scheduled entry without validFrom at validFrom", async () => {
    const wrapper = ({ children }: { children: ReactNode }) => createElement(I18nProvider, { options: { lng: "de" } }, children);
    const resolver = renderHook(() => usePriceTimelineEntryFormResolver(), { wrapper }).result.current;

    const result = await resolver({ type: "scheduled", validFrom: null, price: 500 }, undefined, {
      fields: {},
      shouldUseNativeValidation: false,
    });

    expect(result.errors.validFrom?.message).toBe("Bitte Datum und Uhrzeit auswählen.");
  });
});
