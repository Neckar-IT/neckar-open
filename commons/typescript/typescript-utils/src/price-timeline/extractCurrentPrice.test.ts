import { describe, expect, it } from "vitest";
import type { PriceTimelineSummary } from "../openapi-types";
import { extractCurrentPrice } from "./extractCurrentPrice";

describe("extractCurrentPrice", () => {
  it("returns the price of the current entry", () => {
    const summary: PriceTimelineSummary = {
      currentEntry: { price: 1999, validFrom: "2026-01-01T00:00:00Z" },
      nextFutureEntry: null,
    };
    expect(extractCurrentPrice(summary)).toBe(1999);
  });

  it("returns null when the summary has no current entry", () => {
    const summary: PriceTimelineSummary = {
      currentEntry: null,
      nextFutureEntry: { price: 2500, validFrom: "2026-06-01T00:00:00Z" },
    };
    expect(extractCurrentPrice(summary)).toBeNull();
  });

  it("returns null when the summary is null", () => {
    expect(extractCurrentPrice(null)).toBeNull();
  });

  it("returns 0 when the current entry costs nothing", () => {
    const summary: PriceTimelineSummary = {
      currentEntry: { price: 0, validFrom: "2026-01-01T00:00:00Z" },
      nextFutureEntry: null,
    };
    expect(extractCurrentPrice(summary)).toBe(0);
  });
});
