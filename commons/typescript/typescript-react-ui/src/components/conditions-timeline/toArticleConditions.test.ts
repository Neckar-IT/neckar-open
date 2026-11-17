import type {
  ArticleConditionsWithAccessory,
  ArticleConditionsWithInstallationTime,
  FullArticleConditions,
} from "@neckar-hub/typescript-utils/openapi-types";
import { describe, expect, it } from "vitest";
import {
  toArticleConditions,
  toArticleConditionsWithAccessory,
  toArticleConditionsWithInstallationTime,
  toFullArticleConditions,
} from "./toArticleConditions";
import type { ArticleConditionsTimelineEntryUpsertRequestSchema } from "./useArticleConditionsTimelineEntryFormResolver";

const partOnly: ArticleConditionsTimelineEntryUpsertRequestSchema = {
  type: "now",
  validFrom: null,
  partPurchasePrice: 12000,
  partSellingPrice: 16800,
  partUnit: "",
  withAccessory: false,
  accessoryPurchasePrice: null,
  accessorySellingPrice: null,
  withInstallationTime: false,
  installationTimeHours: null,
};

describe("toArticleConditions", () => {
  it("maps the part prices onto the flat wire shape", () => {
    expect(toArticleConditions(partOnly).part).toEqual({ inputPrice: 12000, sellingPrice: 16800 });
  });

  it("normalizes an empty unit to null - the per-piece default", () => {
    expect(toArticleConditions(partOnly).unit).toBeNull();
  });

  it("normalizes a unit that was typed and cleared again to null", () => {
    expect(toArticleConditions({ ...partOnly, partUnit: null }).unit).toBeNull();
  });

  it("normalizes a typed piece unit to null, trimmed and case-insensitively", () => {
    expect(toArticleConditions({ ...partOnly, partUnit: "  Stück " }).unit).toBeNull();
    expect(toArticleConditions({ ...partOnly, partUnit: "stück" }).unit).toBeNull();
  });

  it("keeps a different unit, trimmed", () => {
    expect(toArticleConditions({ ...partOnly, partUnit: " Meter " }).unit).toBe("Meter");
  });

  it("carries only the part when the article type has nothing else", () => {
    expect(Object.keys(toArticleConditions(partOnly)).toSorted()).toEqual(["part", "unit"]);
  });

  it("carries the part and the installation time for a mounted article without accessory", () => {
    const conditions: ArticleConditionsWithInstallationTime = toArticleConditionsWithInstallationTime({
      ...partOnly,
      withInstallationTime: true,
      installationTimeHours: 16,
    });

    expect(Object.keys(conditions).toSorted()).toEqual(["installationTime", "part", "unit"]);
    expect(conditions.installationTime).toBe("PT16H");
  });

  it("sends a null installation time for a mounted article whose checkbox is unchecked", () => {
    expect(toArticleConditionsWithInstallationTime(partOnly).installationTime).toBeNull();
  });

  it("carries the part and the accessory for an article with accessory that is not mounted", () => {
    const conditions: ArticleConditionsWithAccessory = toArticleConditionsWithAccessory({
      ...partOnly,
      withAccessory: true,
      accessoryPurchasePrice: 15000,
      accessorySellingPrice: 21000,
    });

    expect(Object.keys(conditions).toSorted()).toEqual(["accessory", "part", "unit"]);
    expect(conditions.accessory).toEqual({ inputPrice: 15000, sellingPrice: 21000 });
  });

  it("sends a null accessory for an article with accessory whose checkbox is unchecked", () => {
    expect(toArticleConditionsWithAccessory(partOnly).accessory).toBeNull();
  });

  it("ignores the installation time an article with accessory cannot carry", () => {
    const conditions = toArticleConditionsWithAccessory({
      ...partOnly,
      withInstallationTime: true,
      installationTimeHours: 8.5,
    });

    expect(Object.keys(conditions).toSorted()).toEqual(["accessory", "part", "unit"]);
  });

  it("sends both optional parts as null when neither checkbox is checked", () => {
    const conditions: FullArticleConditions = toFullArticleConditions(partOnly);

    expect(conditions.accessory).toBeNull();
    expect(conditions.installationTime).toBeNull();
  });

  it("sends a null accessory when the hidden accessory fields still hold values", () => {
    const conditions: FullArticleConditions = toFullArticleConditions({
      ...partOnly,
      withAccessory: false,
      accessoryPurchasePrice: 15000,
      accessorySellingPrice: 21000,
    });

    expect(conditions.accessory).toBeNull();
  });

  it("sends a null installation time when the hidden field still holds a value", () => {
    const conditions: FullArticleConditions = toFullArticleConditions({
      ...partOnly,
      withInstallationTime: false,
      installationTimeHours: 8.5,
    });

    expect(conditions.installationTime).toBeNull();
  });

  it("sends a null accessory when a checked accessory's price was cleared again", () => {
    // Unreachable through the form - the resolver rejects it - so this pins the type guard only.
    const conditions: FullArticleConditions = toFullArticleConditions({
      ...partOnly,
      withAccessory: true,
      accessoryPurchasePrice: null,
      accessorySellingPrice: 21000,
    });

    expect(conditions.accessory).toBeNull();
  });

  it("sends a null installation time when a checked installation time was cleared again", () => {
    const conditions: FullArticleConditions = toFullArticleConditions({
      ...partOnly,
      withInstallationTime: true,
      installationTimeHours: null,
    });

    expect(conditions.installationTime).toBeNull();
  });

  it("sends the accessory when its checkbox is checked", () => {
    const conditions: FullArticleConditions = toFullArticleConditions({
      ...partOnly,
      withAccessory: true,
      accessoryPurchasePrice: 15000,
      accessorySellingPrice: 21000,
    });

    expect(conditions.accessory).toEqual({ inputPrice: 15000, sellingPrice: 21000 });
  });

  it("converts the installation time from hours to an ISO-8601 duration", () => {
    const conditions: FullArticleConditions = toFullArticleConditions({
      ...partOnly,
      withInstallationTime: true,
      installationTimeHours: 8.5,
    });

    expect(conditions.installationTime).toBe("PT8H30M");
  });

  it("sends both optional parts when both checkboxes are checked", () => {
    const conditions: FullArticleConditions = toFullArticleConditions({
      ...partOnly,
      withAccessory: true,
      accessoryPurchasePrice: 15000,
      accessorySellingPrice: 21000,
      withInstallationTime: true,
      installationTimeHours: 8.5,
    });

    expect(conditions.accessory).toEqual({ inputPrice: 15000, sellingPrice: 21000 });
    expect(conditions.installationTime).toBe("PT8H30M");
  });
});
