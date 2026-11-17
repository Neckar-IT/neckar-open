/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { combineDayAndTime, matchesDefaultTimeOfDay, resolveTimeComponents } from "./defaultTimeOfDay";

/**
 * Tests
 *
 * All Date values are built with the local-time constructor (`new Date(y, m, d, h, ...)`) so the
 * assertions are independent of the runner's timezone.
 */

describe("resolveTimeComponents", () => {
  it("resolves the symbolic start to the beginning of the day", () => {
    expect(resolveTimeComponents("start")).toEqual([0, 0, 0, 0]);
  });

  it("resolves the symbolic middle to noon", () => {
    expect(resolveTimeComponents("middle")).toEqual([12, 0, 0, 0]);
  });

  it("resolves the symbolic end to the last millisecond of the day", () => {
    expect(resolveTimeComponents("end")).toEqual([23, 59, 59, 999]);
  });

  it("resolves an explicit hours/minutes object, defaulting seconds and milliseconds to zero", () => {
    expect(resolveTimeComponents({ hours: 8, minutes: 30 })).toEqual([8, 30, 0, 0]);
  });

  it("resolves an explicit object with all components", () => {
    expect(resolveTimeComponents({ hours: 9, minutes: 15, seconds: 45, milliseconds: 250 })).toEqual([9, 15, 45, 250]);
  });
});

describe("combineDayAndTime", () => {
  const day = new Date(2026, 5, 15, 3, 4, 5, 6); // time portion must be ignored

  it("keeps the calendar day and applies the time from the time source", () => {
    const timeSource = new Date(2020, 0, 1, 8, 30, 15, 500);

    const result = combineDayAndTime(day, timeSource, "start");

    expect([result.getFullYear(), result.getMonth(), result.getDate()]).toEqual([2026, 5, 15]);
    expect([result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds()]).toEqual([8, 30, 15, 500]);
  });

  it("applies the default time-of-day when no time source is given", () => {
    const result = combineDayAndTime(day, undefined, "end");

    expect([result.getFullYear(), result.getMonth(), result.getDate()]).toEqual([2026, 5, 15]);
    expect([result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds()]).toEqual([23, 59, 59, 999]);
  });

  it("applies an explicit default object when no time source is given", () => {
    const result = combineDayAndTime(day, undefined, { hours: 8, minutes: 0 });

    expect([result.getHours(), result.getMinutes(), result.getSeconds(), result.getMilliseconds()]).toEqual([8, 0, 0, 0]);
  });
});

describe("matchesDefaultTimeOfDay", () => {
  it("returns true when the time portion equals the symbolic default", () => {
    const startOfDay = new Date(2026, 5, 15, 0, 0, 0, 0);

    expect(matchesDefaultTimeOfDay(startOfDay, "start")).toBe(true);
  });

  it("returns false when the time portion differs from the default", () => {
    const noon = new Date(2026, 5, 15, 12, 0, 0, 0);

    expect(matchesDefaultTimeOfDay(noon, "start")).toBe(false);
  });

  it("returns true for an end-of-day timestamp against the end default", () => {
    const endOfDay = new Date(2026, 5, 15, 23, 59, 59, 999);

    expect(matchesDefaultTimeOfDay(endOfDay, "end")).toBe(true);
  });

  it("matches against an explicit default object", () => {
    const businessStart = new Date(2026, 5, 15, 8, 0, 0, 0);

    expect(matchesDefaultTimeOfDay(businessStart, { hours: 8, minutes: 0 })).toBe(true);
  });

  it("returns false when only the milliseconds differ", () => {
    const almostEnd = new Date(2026, 5, 15, 23, 59, 59, 998);

    expect(matchesDefaultTimeOfDay(almostEnd, "end")).toBe(false);
  });
});
