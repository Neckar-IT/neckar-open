import { describe, expect, it } from "vitest";
import { startOfNextMonth } from "./startOfNextMonth";

describe("startOfNextMonth", () => {
  it("returns the first of the following month", () => {
    expect(startOfNextMonth(new Date(2026, 2, 15, 15, 30))).toStrictEqual(new Date(2026, 3, 1));
  });

  it("returns the first of February for the 31st of January", () => {
    // Plain `setMonth(getMonth() + 1)` overflows here into March.
    expect(startOfNextMonth(new Date(2026, 0, 31, 15, 30))).toStrictEqual(new Date(2026, 1, 1));
  });

  it("returns the first of April for the 31st of March", () => {
    expect(startOfNextMonth(new Date(2026, 2, 31, 15, 30))).toStrictEqual(new Date(2026, 3, 1));
  });

  it("crosses the end of the year", () => {
    expect(startOfNextMonth(new Date(2026, 11, 31, 15, 30))).toStrictEqual(new Date(2027, 0, 1));
  });

  it("handles the 29th of February in a leap year", () => {
    expect(startOfNextMonth(new Date(2028, 1, 29, 15, 30))).toStrictEqual(new Date(2028, 2, 1));
  });

  it("returns the first of February for the 31st of January in a leap year", () => {
    expect(startOfNextMonth(new Date(2028, 0, 31, 15, 30))).toStrictEqual(new Date(2028, 1, 1));
  });

  it("stays at local midnight across the daylight-saving switch", () => {
    expect(startOfNextMonth(new Date(2026, 2, 29, 15, 30))).toStrictEqual(new Date(2026, 3, 1));
    expect(startOfNextMonth(new Date(2026, 9, 25, 15, 30))).toStrictEqual(new Date(2026, 10, 1));
  });

  it("does not mutate the input", () => {
    const input = new Date(2026, 6, 29, 15, 30);
    startOfNextMonth(input);
    expect(input).toStrictEqual(new Date(2026, 6, 29, 15, 30));
  });
});
