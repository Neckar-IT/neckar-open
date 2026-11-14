import { describe, expect, it } from "vitest";
import { startOfNextDay } from "./startOfNextDay";

describe("startOfNextDay", () => {
  it("returns the following day at midnight", () => {
    expect(startOfNextDay(new Date(2026, 6, 29, 15, 30))).toStrictEqual(new Date(2026, 6, 30));
  });

  it("crosses the end of the month", () => {
    expect(startOfNextDay(new Date(2026, 0, 31, 15, 30))).toStrictEqual(new Date(2026, 1, 1));
  });

  it("crosses the end of the year", () => {
    expect(startOfNextDay(new Date(2026, 11, 31, 15, 30))).toStrictEqual(new Date(2027, 0, 1));
  });

  it("crosses the end of February in a leap year", () => {
    expect(startOfNextDay(new Date(2028, 1, 28, 15, 30))).toStrictEqual(new Date(2028, 1, 29));
  });

  it("stays at local midnight across the daylight-saving switch", () => {
    expect(startOfNextDay(new Date(2026, 2, 28, 15, 30))).toStrictEqual(new Date(2026, 2, 29));
    expect(startOfNextDay(new Date(2026, 9, 24, 15, 30))).toStrictEqual(new Date(2026, 9, 25));
  });
});
