/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { isDefined, isNotDefined } from "./defined";

/**
 * Tests
 */

/** Every value the two guards are pinned against, paired with the answer `isDefined` owes it. */
const definedness: [label: string, value: unknown, defined: boolean][] = [
  ["null", null, false],
  ["undefined", undefined, false],
  ["an empty string", "", false],
  ["a string of only spaces", "   ", false],
  ["a string of only tabs and newlines", "\t\n", false],
  ["NaN", Number.NaN, false],
  ["a non-empty string", "as", true],
  ["a whitespace-padded string with content", "  a  ", true],
  ["the string zero", "0", true],
  ["the number zero", 0, true],
  ["a negative number", -7.5, true],
  ["Infinity", Number.POSITIVE_INFINITY, true],
  ["the boolean false", false, true],
  ["an object", { a: 7.0 }, true],
  ["an empty object", {}, true],
  ["an empty array", [], true],
  ["an empty map", new Map(), true],
  ["an empty set", new Set(), true],
];

describe("isDefined", () => {
  for (const [label, value, defined] of definedness) {
    it(`returns ${defined} for ${label}`, () => {
      expect(isDefined(value)).toBe(defined);
    });
  }
});

describe("isNotDefined", () => {
  for (const [label, value, defined] of definedness) {
    it(`returns ${!defined} for ${label}`, () => {
      expect(isNotDefined(value)).toBe(!defined);
    });
  }
});

describe("isDefined type narrowing", () => {
  it("narrows away null and undefined inside the guard", () => {
    const a = "asdf" as string | null | undefined;

    expect(isDefined(a)).toBe(true);

    if (isDefined(a)) {
      //Now TypeScript knows that 'a' is a string
      expect(a.length).toBe(4);
      //Ensure the compile type
      a satisfies string;
    }
  });
});

describe("isNotDefined type narrowing", () => {
  it("narrows away null and undefined after an early return", () => {
    const address = { city: "Musterstadt" } as { city: string } | null | undefined;

    if (isNotDefined(address)) {
      throw new Error("expected the address to be defined");
    }

    //Now TypeScript knows that 'address' carries a city
    expect(address.city).toBe("Musterstadt");
    //Ensure the compile type
    address satisfies { city: string };
  });
});
