import { describe, expect, it } from "vitest";
import { formatExpiryDate, formatExpiryDateForDisplay, parseExpiryDate } from "./expiryDate";
import { expiryDateCorpus } from "./expiryDateCorpus";

const wireCases = expiryDateCorpus.cases.flatMap(({ wire, year, month }) => (wire == null ? [] : [{ wire, year, month }]));

describe("expiryDate against its corpus", () => {
  it.each(wireCases)("parses $wire", ({ wire, year, month }) => {
    expect(parseExpiryDate(wire)).toEqual({ year, month });
  });

  it.each(wireCases)("formats $year/$month as $wire", ({ wire, year, month }) => {
    expect(formatExpiryDate({ year, month })).toBe(wire);
  });
});

describe("formatExpiryDateForDisplay", () => {
  it("shows the month first", () => {
    expect(formatExpiryDateForDisplay("2099-03")).toBe("03/2099");
  });
});
