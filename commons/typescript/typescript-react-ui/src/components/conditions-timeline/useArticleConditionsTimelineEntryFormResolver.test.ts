import { I18nProvider } from "@neckar-hub/typescript-react-i18n";
import { findOptionalFormSchemaFieldsRejectingEmptyValue } from "@neckar-hub/typescript-utils/zod-utils";
import { renderHook } from "@testing-library/react";
import { createElement, type ReactNode } from "react";
import { describe, expect, it } from "vitest";
import { bareConditions, conditionsWithAccessory, materialOnlyConditions, withoutAccessoryConditions } from "./conditionsFixtures";
import { toFullArticleConditions } from "./toArticleConditions";
import {
  articleConditionsTimelineEntryUpsertDefaultValues,
  articleConditionsTimelineEntryUpsertRequestSchema,
  useArticleConditionsTimelineEntryFormResolver,
} from "./useArticleConditionsTimelineEntryFormResolver";

describe("articleConditionsTimelineEntryUpsertRequestSchema", () => {
  it("has no optional field that rejects one of the two empty encodings", () => {
    // An emptied euro or hours input reports `null`, a field missing from `defaultValues` is
    // `undefined`. A field accepting only one of them fails the object parse and thereby skips the
    // checkbox rules that decide whether the value is needed at all.
    expect(findOptionalFormSchemaFieldsRejectingEmptyValue(articleConditionsTimelineEntryUpsertRequestSchema)).toEqual([]);
  });

  it("keeps the part prices mandatory", () => {
    const defaultValues = articleConditionsTimelineEntryUpsertDefaultValues(
      { withAccessory: false, withInstallationTime: false },
      { conditions: null },
    );

    expect(articleConditionsTimelineEntryUpsertRequestSchema.safeParse(defaultValues).success).toBe(false);
  });
});

describe("articleConditionsTimelineEntryUpsertDefaultValues", () => {
  it("holds every optional field as the explicit empty value", () => {
    expect(
      articleConditionsTimelineEntryUpsertDefaultValues({ withAccessory: false, withInstallationTime: false }, { conditions: null }),
    ).toEqual({
      type: "now",
      validFrom: null,
      partUnit: null,
      withAccessory: false,
      accessoryPurchasePrice: null,
      accessorySellingPrice: null,
      withInstallationTime: false,
      installationTimeHours: null,
    });
  });

  it("switches the gate checkboxes on for an article that ships with accessory and installation time", () => {
    const defaultValues = articleConditionsTimelineEntryUpsertDefaultValues(
      { withAccessory: true, withInstallationTime: true },
      { conditions: null },
    );

    expect(defaultValues.withAccessory).toBe(true);
    expect(defaultValues.withInstallationTime).toBe(true);
  });
});

describe("articleConditionsTimelineEntryUpsertDefaultValues from the conditions in effect", () => {
  const allParts = { withAccessory: true, withInstallationTime: true };

  it("fills every field of conditions carrying both optional parts", () => {
    expect(articleConditionsTimelineEntryUpsertDefaultValues(allParts, { conditions: conditionsWithAccessory })).toEqual({
      type: "now",
      validFrom: null,
      partPurchasePrice: 313045,
      partSellingPrice: 438263,
      partUnit: "Meter",
      withAccessory: true,
      accessoryPurchasePrice: 15000,
      accessorySellingPrice: 21000,
      withInstallationTime: true,
      installationTimeHours: 8.5,
    });
  });

  it("switches a block off where the entry in effect carries no such part", () => {
    const defaultValues = articleConditionsTimelineEntryUpsertDefaultValues(allParts, { conditions: materialOnlyConditions });

    expect(defaultValues.withAccessory).toBe(false);
    expect(defaultValues.accessoryPurchasePrice).toBeNull();
    expect(defaultValues.withInstallationTime).toBe(false);
    expect(defaultValues.installationTimeHours).toBeNull();
  });

  it("switches a block on where the entry in effect carries the part the page starts without", () => {
    const defaultValues = articleConditionsTimelineEntryUpsertDefaultValues(
      { withAccessory: false, withInstallationTime: false },
      { conditions: conditionsWithAccessory },
    );

    expect(defaultValues.withAccessory).toBe(true);
    expect(defaultValues.withInstallationTime).toBe(true);
  });

  it("switches off the accessory of bare conditions and of conditions supplied without one", () => {
    expect(articleConditionsTimelineEntryUpsertDefaultValues(allParts, { conditions: bareConditions }).withAccessory).toBe(false);
    expect(articleConditionsTimelineEntryUpsertDefaultValues(allParts, { conditions: withoutAccessoryConditions }).withAccessory).toBe(
      false,
    );
  });

  it("reads a missing unit as the per-piece default", () => {
    expect(articleConditionsTimelineEntryUpsertDefaultValues(allParts, { conditions: bareConditions }).partUnit).toBeNull();
  });

  it("rejects an installation time the wire format does not produce", () => {
    expect(() =>
      articleConditionsTimelineEntryUpsertDefaultValues(allParts, { conditions: { ...conditionsWithAccessory, installationTime: "P1D" } }),
    ).toThrow("Unreadable installation time: P1D");
  });

  it("gives back the conditions in effect when submitted unchanged", () => {
    const defaultValues = articleConditionsTimelineEntryUpsertDefaultValues(allParts, { conditions: conditionsWithAccessory });

    expect(toFullArticleConditions(articleConditionsTimelineEntryUpsertRequestSchema.parse(defaultValues))).toEqual(
      conditionsWithAccessory,
    );
  });
});

describe("useArticleConditionsTimelineEntryFormResolver", () => {
  it("reports a scheduled entry without validFrom at validFrom", async () => {
    const wrapper = ({ children }: { children: ReactNode }) => createElement(I18nProvider, { options: { lng: "de" } }, children);
    const resolver = renderHook(() => useArticleConditionsTimelineEntryFormResolver(), { wrapper }).result.current;
    const values = {
      ...articleConditionsTimelineEntryUpsertDefaultValues({ withAccessory: false, withInstallationTime: false }, { conditions: null }),
      type: "scheduled" as const,
      partPurchasePrice: 12000,
      partSellingPrice: 16800,
    };

    const result = await resolver(values, undefined, { fields: {}, shouldUseNativeValidation: false });

    expect(result.errors.validFrom?.message).toBe("Bitte Datum und Uhrzeit auswählen.");
  });
});
