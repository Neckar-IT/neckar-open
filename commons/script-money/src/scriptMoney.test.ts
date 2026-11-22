import { describe, expect, it } from "vitest";
import { assertSafeCents, centsTimes, centsTimesAndRound, formatEuro, netToGross, sumCents, vatOf } from "./scriptMoney";
import corpus from "./scriptMoney.cases.json";

/** The Kotlin `ScriptMoneyCorpusTest` holds `Money` to the same corpus. */
describe("scriptMoney against scriptMoney.cases.json", () => {
  it.each(corpus.vat)(
    "taxes $netCents ct at $vatRate with $vatCents ct to $grossCents ct",
    ({ netCents, vatRate, vatCents, grossCents }) => {
      expect(vatOf(netCents, vatRate)).toBe(vatCents);
      expect(netToGross(netCents, vatRate)).toBe(grossCents);
    },
  );

  it.each(corpus.timesAndRound)("multiplies $cents ct by $factor to $resultCents ct", ({ cents, factor, resultCents }) => {
    expect(centsTimesAndRound(cents, factor)).toBe(resultCents);
  });

  it.each(corpus.euro)("formats $cents ct as $text", ({ cents, text }) => {
    expect(formatEuro(cents)).toBe(text);
  });
});

describe("assertSafeCents", () => {
  it("returns a whole amount up to 2^53 - 1", () => {
    expect(assertSafeCents(Number.MAX_SAFE_INTEGER)).toBe(Number.MAX_SAFE_INTEGER);
  });

  it.each([2 ** 53, -(2 ** 53), 0.5, Number.NaN, Number.POSITIVE_INFINITY])("refuses %s", (cents) => {
    expect(() => assertSafeCents(cents)).toThrow(RangeError);
  });

  it("guards every function that takes an amount", () => {
    expect(() => centsTimes(0.5, 2)).toThrow(RangeError);
    expect(() => centsTimesAndRound(0.5, 2)).toThrow(RangeError);
    expect(() => sumCents([-1, 2 ** 53])).toThrow(RangeError);
    expect(() => vatOf(0.5, 0.19)).toThrow(RangeError);
    expect(() => netToGross(2 ** 53, 0.19)).toThrow(RangeError);
    expect(() => formatEuro(2 ** 53)).toThrow(RangeError);
  });
});

describe("centsTimes", () => {
  it("multiplies by a whole count exactly", () => {
    expect(centsTimes(1747, 3)).toBe(5241);
    expect(centsTimes(-150, 2)).toBe(-300);
    expect(centsTimes(-150, 0)).toBe(0);
  });

  it("refuses a product of 2^53 or more", () => {
    expect(() => centsTimes(2 ** 52, 2)).toThrow(RangeError);
  });

  it("refuses a fractional count, which needs a rounding", () => {
    expect(() => centsTimes(100, 1.5)).toThrow(RangeError);
  });
});

describe("centsTimesAndRound", () => {
  it("refuses an amount or a product beyond 2^52, where a double loses the half cent", () => {
    expect(() => centsTimesAndRound(2 ** 52 + 2, 0.5)).toThrow(RangeError);
    expect(() => centsTimesAndRound(2 ** 52, 1.5)).toThrow(RangeError);
  });

  it("refuses a factor that is not finite", () => {
    expect(() => centsTimesAndRound(100, Number.NaN)).toThrow(RangeError);
  });
});

describe("sumCents", () => {
  it("adds the amounts and answers 0 for none", () => {
    expect(sumCents([1747, 1443, -100])).toBe(3090);
    expect(sumCents([])).toBe(0);
  });

  it("refuses a sum of 2^53 or more", () => {
    expect(() => sumCents([Number.MAX_SAFE_INTEGER, 1])).toThrow(RangeError);
  });
});

describe("vatOf", () => {
  it("refuses a net amount whose product with the basis points leaves 2^53", () => {
    expect(() => vatOf(2 ** 50, 0.19)).toThrow(RangeError);
  });

  it.each([-0.01, 1.01, 0.12345, Number.NaN])("refuses the rate %s, as VatRate does", (vatRate) => {
    expect(() => vatOf(100, vatRate)).toThrow(/VAT rate/);
  });
});

describe("netToGross", () => {
  it("refuses a gross of 2^53 or more", () => {
    expect(() => netToGross(Number.MAX_SAFE_INTEGER, 0.0001)).toThrow(RangeError);
  });
});
