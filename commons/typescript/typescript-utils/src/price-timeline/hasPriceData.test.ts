import { describe, expect, it } from "vitest";
import { hasPriceData } from "./hasPriceData";

describe("hasPriceData", () => {
  it("returns false when the summary is null", () => {
    expect(hasPriceData(null)).toBe(false);
  });

  it("returns true when only currentEntry is present", () => {
    expect(hasPriceData({ currentEntry: { price: 1499, validFrom: "2020-01-01T00:00:00Z" }, nextFutureEntry: null })).toBe(true);
  });

  it("returns true when only nextFutureEntry is present", () => {
    expect(hasPriceData({ currentEntry: null, nextFutureEntry: { price: 1800, validFrom: "2099-06-01T00:00:00Z" } })).toBe(true);
  });

  it("returns false when both entries are null", () => {
    expect(hasPriceData({ currentEntry: null, nextFutureEntry: null })).toBe(false);
  });
});
