/**
 * Imports
 */

import { describe, expect, it } from "vitest";
import { normalizeFloatDrift } from "./normalizeFloatDrift";

/**
 * Tests
 */

describe("normalizeFloatDrift", () => {
  it("collapses 147.11 / 0.01 drift back to the exact integer 14711", () => {
    const drifted = 147.11 / 0.01;
    expect(drifted).not.toBe(14711);
    expect(normalizeFloatDrift(drifted)).toBe(14711);
  });

  it("keeps an already-exact integer intact", () => {
    expect(normalizeFloatDrift(14711)).toBe(14711);
  });

  it("preserves genuine decimals below the 10-digit horizon", () => {
    expect(normalizeFloatDrift(3.14)).toBe(3.14);
    expect(normalizeFloatDrift(5.123)).toBe(5.123);
    expect(normalizeFloatDrift(-42.5)).toBe(-42.5);
  });

  it("maps zero and negative zero to zero", () => {
    // parseFloat(x.toFixed(10)) takes the number through a string round-trip,
    // which does not preserve the sign of -0. We don't care — in money /
    // measurement contexts, -0 and +0 are the same value. The test pins this
    // behavior so a future change that preserves -0 is a deliberate decision.
    expect(normalizeFloatDrift(0)).toBe(0);
    expect(normalizeFloatDrift(-0)).toBe(0);
  });

  it("collapses 1.1 + 2.2 = 3.3000000000000003 drift", () => {
    expect(1.1 + 2.2).not.toBe(3.3);
    expect(normalizeFloatDrift(1.1 + 2.2)).toBe(3.3);
  });

  it("handles negative drift — e.g. -147.11 / 0.01", () => {
    const drifted = -147.11 / 0.01;
    expect(drifted).not.toBe(-14711);
    expect(normalizeFloatDrift(drifted)).toBe(-14711);
  });
});
