import { describe, expect, it } from "vitest";
import { toArticleConditionsPriceSummaryRequest } from "./toArticleConditionsPriceSummaryRequest";

describe("toArticleConditionsPriceSummaryRequest", () => {
  it("maps every checked part with its prices in cents", () => {
    expect(
      toArticleConditionsPriceSummaryRequest({
        partPurchasePrice: 2000,
        partSellingPrice: 2500,
        partUnit: null,
        withAccessory: true,
        accessoryPurchasePrice: 300,
        accessorySellingPrice: 450,
        withInstallationTime: true,
        installationTimeHours: 0.5,
      }),
    ).toEqual({
      part: { inputPrice: 2000, sellingPrice: 2500 },
      unit: null,
      accessory: { inputPrice: 300, sellingPrice: 450 },
      installation: { installationTime: "PT30M" },
    });
  });

  it("drops the rows of unchecked checkboxes, whatever their hidden fields still hold", () => {
    const request = toArticleConditionsPriceSummaryRequest({
      partPurchasePrice: 2000,
      partSellingPrice: 2500,
      withAccessory: false,
      accessoryPurchasePrice: 300,
      accessorySellingPrice: 450,
      withInstallationTime: false,
      installationTimeHours: 8,
    });

    expect(request.accessory).toBeNull();
    expect(request.installation).toBeNull();
  });

  it("sends empty fields as null and keeps a checked row that is still empty", () => {
    expect(
      toArticleConditionsPriceSummaryRequest({
        partPurchasePrice: undefined,
        partSellingPrice: 2500,
        withAccessory: true,
        accessoryPurchasePrice: null,
        accessorySellingPrice: null,
        withInstallationTime: true,
        installationTimeHours: null,
      }),
    ).toEqual({
      part: { inputPrice: null, sellingPrice: 2500 },
      unit: null,
      accessory: { inputPrice: null, sellingPrice: null },
      installation: { installationTime: null },
    });
  });

  it("normalizes a typed piece unit to null and passes any other unit through", () => {
    expect(toArticleConditionsPriceSummaryRequest({ partUnit: " Stück " }).unit).toBeNull();
    expect(toArticleConditionsPriceSummaryRequest({ partUnit: "Meter" }).unit).toBe("Meter");
  });
});
