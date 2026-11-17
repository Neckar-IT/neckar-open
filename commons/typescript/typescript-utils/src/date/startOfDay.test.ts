import { describe, expect, it } from "vitest";
import { startOfDay } from "./startOfDay";

describe("startOfDay", () => {
  it("cuts the time of day", () => {
    expect(startOfDay(new Date(2026, 6, 29, 15, 30, 45, 123))).toStrictEqual(new Date(2026, 6, 29));
  });

  it("keeps a date that is already at midnight", () => {
    expect(startOfDay(new Date(2026, 6, 29))).toStrictEqual(new Date(2026, 6, 29));
  });

  it("does not mutate the input", () => {
    const input = new Date(2026, 6, 29, 15, 30);
    startOfDay(input);
    expect(input).toStrictEqual(new Date(2026, 6, 29, 15, 30));
  });

  it("stays at local midnight across the daylight-saving switch", () => {
    // Last Sunday in March / October — the days that gain or lose an hour in Europe.
    expect(startOfDay(new Date(2026, 2, 29, 15, 30))).toStrictEqual(new Date(2026, 2, 29));
    expect(startOfDay(new Date(2026, 9, 25, 15, 30))).toStrictEqual(new Date(2026, 9, 25));
  });
});
