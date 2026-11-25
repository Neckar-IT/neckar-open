import { describe, expect, it } from "vitest";
import { expiryDateCorpus } from "./expiryDateCorpus";
import { ExpiryDateFormatViolation, validateExpiryDateShape } from "./validateExpiryDateShape";

const now = new Date(expiryDateCorpus.now);

describe("validateExpiryDateShape against its corpus", () => {
  const valid = expiryDateCorpus.cases.filter((testCase) => testCase.violation == null);
  const refused = expiryDateCorpus.cases.filter((testCase) => testCase.violation != null);

  it.each(valid)("accepts $year/$month", ({ year, month }) => {
    expect(validateExpiryDateShape({ year, month }, now)).toEqual({ valid: true });
  });

  it.each(refused)("rejects $year/$month with $violation", ({ year, month, violation }) => {
    expect(validateExpiryDateShape({ year, month }, now)).toEqual({ valid: false, violation });
  });

  it("covers every violation the check reports", () => {
    expect(new Set(refused.map((testCase) => testCase.violation))).toEqual(new Set(Object.values(ExpiryDateFormatViolation)));
    expect(new Set(expiryDateCorpus.frontendDetectable)).toEqual(new Set(Object.values(ExpiryDateFormatViolation)));
  });
});
