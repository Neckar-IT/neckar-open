import { describe, expect, it } from "vitest";
import { formatIsoDurationFromSeconds, parseIsoDurationSeconds } from "./isoDurationSeconds";

describe("parseIsoDurationSeconds", () => {
  it("parses a sub-second duration", () => {
    expect(parseIsoDurationSeconds("PT0.050S")).toBe(0.05);
  });

  it("parses whole seconds", () => {
    expect(parseIsoDurationSeconds("PT30S")).toBe(30);
  });

  it("parses minutes and seconds", () => {
    expect(parseIsoDurationSeconds("PT1M30S")).toBe(90);
  });

  it("parses hours", () => {
    expect(parseIsoDurationSeconds("PT2H")).toBe(7200);
  });

  it("parses a negative duration", () => {
    expect(parseIsoDurationSeconds("-PT1.5S")).toBe(-1.5);
  });

  it("returns null for null and undefined", () => {
    expect(parseIsoDurationSeconds(null)).toBeNull();
    expect(parseIsoDurationSeconds(undefined)).toBeNull();
  });

  it("returns null for an unparseable string", () => {
    expect(parseIsoDurationSeconds("0.05")).toBeNull();
    expect(parseIsoDurationSeconds("P1D")).toBeNull();
    expect(parseIsoDurationSeconds("PT")).toBeNull();
  });
});

describe("formatIsoDurationFromSeconds", () => {
  it("formats a sub-second duration", () => {
    expect(formatIsoDurationFromSeconds(0.05)).toBe("PT0.05S");
  });

  it("formats whole seconds beyond a minute as seconds", () => {
    expect(formatIsoDurationFromSeconds(90)).toBe("PT90S");
  });

  it("rounds to whole milliseconds", () => {
    expect(formatIsoDurationFromSeconds(0.0000001)).toBe("PT0S");
    expect(formatIsoDurationFromSeconds(1.23456)).toBe("PT1.235S");
  });

  it("formats a negative duration", () => {
    expect(formatIsoDurationFromSeconds(-1.5)).toBe("-PT1.5S");
  });

  it("round-trips through the parser", () => {
    expect(parseIsoDurationSeconds(formatIsoDurationFromSeconds(0.1))).toBe(0.1);
  });
});
