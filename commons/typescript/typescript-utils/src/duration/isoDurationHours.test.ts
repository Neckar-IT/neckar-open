import { describe, expect, it } from "vitest";
import { formatIsoDurationFromHours, parseIsoDurationHours } from "./isoDurationHours";

describe("parseIsoDurationHours", () => {
  it("parses hours and minutes", () => {
    expect(parseIsoDurationHours("PT8H30M")).toBe(8.5);
  });

  it("parses whole hours", () => {
    expect(parseIsoDurationHours("PT2H")).toBe(2);
  });

  it("parses minutes only", () => {
    expect(parseIsoDurationHours("PT45M")).toBe(0.75);
  });

  it("parses hours, minutes and seconds", () => {
    expect(parseIsoDurationHours("PT1H0M36S")).toBe(1.01);
  });

  it("parses a fractional hours component", () => {
    expect(parseIsoDurationHours("PT8.5H")).toBe(8.5);
  });

  it("parses a negative duration", () => {
    expect(parseIsoDurationHours("-PT1H30M")).toBe(-1.5);
  });

  it("returns null for null and undefined", () => {
    expect(parseIsoDurationHours(null)).toBeNull();
    expect(parseIsoDurationHours(undefined)).toBeNull();
  });

  it("returns null for an unparseable string", () => {
    expect(parseIsoDurationHours("8.5")).toBeNull();
    expect(parseIsoDurationHours("P1D")).toBeNull();
    expect(parseIsoDurationHours("PT")).toBeNull();
  });
});

describe("formatIsoDurationFromHours", () => {
  it("formats hours and minutes", () => {
    expect(formatIsoDurationFromHours(8.5)).toBe("PT8H30M");
  });

  it("omits zero components", () => {
    expect(formatIsoDurationFromHours(2)).toBe("PT2H");
    expect(formatIsoDurationFromHours(0.75)).toBe("PT45M");
  });

  it("formats zero as PT0S", () => {
    expect(formatIsoDurationFromHours(0)).toBe("PT0S");
  });

  it("keeps the seconds component when the hours do not divide into whole minutes", () => {
    expect(formatIsoDurationFromHours(1.01)).toBe("PT1H36S");
  });

  it("formats a negative duration", () => {
    expect(formatIsoDurationFromHours(-1.5)).toBe("-PT1H30M");
  });

  it("round-trips through the parser", () => {
    expect(parseIsoDurationHours(formatIsoDurationFromHours(8.5))).toBe(8.5);
    expect(parseIsoDurationHours(formatIsoDurationFromHours(0.25))).toBe(0.25);
  });
});
