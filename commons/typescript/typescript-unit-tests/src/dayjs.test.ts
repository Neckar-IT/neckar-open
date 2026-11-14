/**
 * How dayjs treats absent input, and how it produces relative time strings.
 *
 * The trap this file exists for: `null` and `undefined` are not interchangeable as dayjs input. An
 * optional field arriving as `undefined` silently becomes today, which formats and renders like a real
 * value and is wrong everywhere it is shown.
 */

import dayjs from "dayjs";
import relativeTimePlugin from "dayjs/plugin/relativeTime";
import { describe, expect, it, vi } from "vitest";

describe("dayjs", () => {
  it("should return the correct date", () => {
    expect(dayjs("2021-01-01").format("YYYY-MM-DD")).toBe("2021-01-01");

    // The asymmetry to watch out for: null is rejected, undefined is read as "no argument given" and
    // therefore means now. An optional API field is undefined when absent, so passing it through
    // unchecked yields today's date instead of the invalid one the null case would have made obvious.
    expect(dayjs(null).format("YYYY-MM-DD")).toBe("Invalid Date");
    expect(dayjs(undefined).format("YYYY-MM-DD")).toBe(dayjs().format("YYYY-MM-DD"));

    expect(dayjs(null).toDate()).toStrictEqual(new Date(NaN));
    expect(dayjs(null).toDate().getTime()).toStrictEqual(NaN);

    // An invalid date propagates through arithmetic instead of throwing. Nothing on the way from the
    // bad input to the rendered string raises, so the first sign of it is "Invalid Date" in the UI.
    expect(dayjs(null).add(1, "day").format("YYYY-MM-DD")).toBe("Invalid Date");

    // Where the string comes from: dayjs formats an invalid date the way Date itself stringifies it.
    expect(new Date(NaN).toString()).toBe("Invalid Date");
  });

  it("should calculate the difference between two dates", () => {
    const date1 = dayjs("2021-01-01");
    const date2 = dayjs("2021-01-02");
    const date3 = dayjs("2021-01-03");

    // The sign follows receiver minus argument, so a date earlier than the one it is compared against
    // gives a negative difference.
    expect(date1.diff(date2, "day")).toBe(-1);
    expect(date2.diff(date1, "day")).toBe(1);
    expect(date1.diff(date3, "day")).toBe(-2);
    expect(date3.diff(date1, "day")).toBe(2);
  });

  it("should return the correct relative time string", () => {
    const date = new Date("2025-01-01T00:00:00.000Z");

    // fromNow() and toNow() do not exist on a plain dayjs object — the relativeTime plugin adds them,
    // and it augments the Dayjs interface, so TypeScript accepts the call whether or not anyone ever
    // called extend(). Forgetting it fails at runtime, not at compile time.
    dayjs.extend(relativeTimePlugin);

    // Both methods measure against the real clock, so "now" has to be pinned or the expected strings
    // drift with every run.
    vi.setSystemTime(date);

    // fromNow(): where the receiver lies relative to now. Ten seconds before now reads as past.
    expect(dayjs(date).subtract(10, "seconds").fromNow()).toBe("a few seconds ago");
    expect(dayjs(date).add(10, "seconds").fromNow()).toBe("in a few seconds");

    // toNow(): the same distance measured the other way round — where now lies relative to the
    // receiver. Same pair of timestamps, inverted wording. Reaching for toNow() when fromNow() was
    // meant produces a sentence that is grammatically fine and says the opposite.
    expect(dayjs(date).subtract(10, "seconds").toNow()).toBe("in a few seconds");
    expect(dayjs(date).add(10, "seconds").toNow()).toBe("a few seconds ago");
  });
});
