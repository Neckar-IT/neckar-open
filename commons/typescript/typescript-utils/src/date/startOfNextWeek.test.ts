import dayjs from "dayjs";
import "dayjs/locale/de";
import { afterEach, describe, expect, it } from "vitest";
import { startOfNextWeek } from "./startOfNextWeek";

afterEach(() => {
  dayjs.locale("en");
});

describe("startOfNextWeek", () => {
  it("returns the same Monday for every day of the week it is called on", () => {
    const mondayOfNextWeek = new Date(2026, 7, 3);

    // Mon 2026-07-27 through Sun 2026-08-02.
    for (let dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
      expect(startOfNextWeek(new Date(2026, 6, 27 + dayOfWeek, 15, 30))).toStrictEqual(mondayOfNextWeek);
    }
  });

  it("treats Sunday as the last day of the week, so the next week starts the following day", () => {
    expect(startOfNextWeek(new Date(2026, 6, 26, 15, 30))).toStrictEqual(new Date(2026, 6, 27));
  });

  it("crosses the end of the month", () => {
    expect(startOfNextWeek(new Date(2026, 6, 30, 15, 30))).toStrictEqual(new Date(2026, 7, 3));
  });

  it("crosses the end of the year", () => {
    expect(startOfNextWeek(new Date(2026, 11, 31, 15, 30))).toStrictEqual(new Date(2027, 0, 4));
  });

  it("stays at local midnight across the daylight-saving switch", () => {
    expect(startOfNextWeek(new Date(2026, 2, 29, 15, 30))).toStrictEqual(new Date(2026, 2, 30));
    expect(startOfNextWeek(new Date(2026, 9, 25, 15, 30))).toStrictEqual(new Date(2026, 9, 26));
  });

  it("returns Monday no matter which locale is globally set", () => {
    // `startOf("week")` would return Sunday under "en" and Monday under "de" — the ISO week must not.
    const wednesday = new Date(2026, 6, 29, 15, 30);
    const mondayOfNextWeek = new Date(2026, 7, 3);

    dayjs.locale("en");
    expect(startOfNextWeek(wednesday)).toStrictEqual(mondayOfNextWeek);

    dayjs.locale("de");
    expect(startOfNextWeek(wednesday)).toStrictEqual(mondayOfNextWeek);
  });

  it("does not mutate the input", () => {
    const input = new Date(2026, 6, 29, 15, 30);
    startOfNextWeek(input);
    expect(input).toStrictEqual(new Date(2026, 6, 29, 15, 30));
  });
});
