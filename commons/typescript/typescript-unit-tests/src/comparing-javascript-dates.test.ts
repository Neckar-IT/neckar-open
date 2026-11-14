/**
 * Why two Date objects standing for the same instant are not equal to each other, and which
 * conversions make them comparable.
 */

import { describe, expect, it } from "vitest";

describe("Comparing Javascript Dates", () => {
  const date1 = new Date("2021-01-01T12:00:00.000Z");
  const date2 = new Date("2021-01-01T12:00:00.000Z");

  it("should fail when directly comparing dates", () => {
    // Date has no value semantics: === and toBe compare object identity, and these are two distinct
    // objects. Neither one ever looks at the instant they hold.
    expect(date1).not.toBe(date2);
    expect(date1 === date2).toBeFalsy();
  });

  it("should pass when converting dates to primitive types first", () => {
    // Comparing means comparing primitives, and there are two ways to get one.

    // As a string. Readable in a failure message, and safe to compare because toISOString() always
    // normalises to UTC — two dates for the same instant produce the same string regardless of the
    // time zone they were constructed in.
    expect(date1.toISOString()).toBe("2021-01-01T12:00:00.000Z");
    expect(date1.toISOString() === date2.toISOString()).toBeTruthy();

    // As a number. Both methods return the identical value: milliseconds since the Unix epoch — not
    // seconds, which is what "timestamp" means in most other languages and in our own APIs.
    //
    // valueOf() is the conversion the engine performs implicitly, so it is what date1 - date2 and
    // date1 < date2 already do behind the scenes. getTime() computes the same number and says so at
    // the call site, which is why it is the one to write when the comparison is explicit.
    expect(date1.valueOf()).toBe(1609502400000);
    expect(date1.valueOf() === date2.valueOf()).toBeTruthy();

    expect(date1.getTime()).toBe(1609502400000);
    expect(date1.getTime() === date2.getTime()).toBeTruthy();
  });
});
