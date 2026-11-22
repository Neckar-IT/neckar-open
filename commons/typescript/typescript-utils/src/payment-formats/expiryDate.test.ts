import { describe, expect, it } from "vitest";
import { formatExpiryDate, formatExpiryDateForDisplay, parseExpiryDate } from "./expiryDate";
import { loadPaymentFormatCases } from "./paymentFormatsTestSupport";

interface ExpiryDateCorpus {
  purpose: string;
  cases: { wire: string; year: number; month: number }[];
}

const corpus = loadPaymentFormatCases<ExpiryDateCorpus>("commons/payment/src/jvmTest/resources/ExpiryDate.cases.json");

describe("expiryDate against its corpus", () => {
  it.each(corpus.cases)("parses $wire", ({ wire, year, month }) => {
    expect(parseExpiryDate(wire)).toEqual({ year, month });
  });

  it.each(corpus.cases)("formats $year/$month as $wire", ({ wire, year, month }) => {
    expect(formatExpiryDate({ year, month })).toBe(wire);
  });
});

describe("formatExpiryDateForDisplay", () => {
  it("shows the month first", () => {
    expect(formatExpiryDateForDisplay("2099-03")).toBe("03/2099");
  });
});
