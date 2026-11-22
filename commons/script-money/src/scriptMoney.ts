/**
 * Cent arithmetic with the semantics of `it.neckar.financial.currency.Money`; an amount is whole cents as a `number`.
 * `TypeScriptCode.fromClasspath` appends this file to every script: no import, and at the top level only function
 * declarations, which hoist, so a script may call them before this source runs.
 */

/** Returns `cents` unchanged; throws unless it is a whole number of cents within ±(2^53 - 1), as a `Long` of `Money` would hold it exactly. */
export function assertSafeCents(cents: number): number {
  if (!Number.isSafeInteger(cents)) {
    throw new RangeError(`Cents must be a whole number within ±${Number.MAX_SAFE_INTEGER}: ${cents}`);
  }
  return cents;
}

/** `Money.times(Int)`: the exact product with a whole `count`. */
export function centsTimes(cents: number, count: number): number {
  assertSafeCents(cents);
  if (!Number.isSafeInteger(count)) {
    throw new RangeError(`Count must be a whole number, use centsTimesAndRound for a fraction: ${count}`);
  }
  return assertSafeCents(withoutNegativeZero(cents * count));
}

/** `Money.timesAndRound(factor)` with `RoundingStrategy.Default`: a half cent moves away from zero, -199.5 to -200. */
export function centsTimesAndRound(cents: number, factor: number): number {
  assertSafeCents(cents);
  if (!Number.isFinite(factor)) {
    throw new RangeError(`Factor must be finite: ${factor}`);
  }
  assertExact(cents);
  const product = cents * factor;
  assertExact(product);
  const truncated = Math.trunc(product);
  const roundsAwayFromZero = Math.abs(product - truncated) >= 0.5;
  return withoutNegativeZero(roundsAwayFromZero ? truncated + Math.sign(product) : truncated);
}

export function sumCents(values: readonly number[]): number {
  return values.reduce((total, value) => assertSafeCents(total + assertSafeCents(value)), 0);
}

/** `Money.Net.vatAmount`: the tax on `netCents` at `vatRate` (`0.07` is 7 %), rounded once in integer arithmetic. */
export function vatOf(netCents: number, vatRate: number): number {
  assertSafeCents(netCents);
  const denominator = basisPointsPerUnit();
  const numerator = netCents * basisPointsOf(vatRate);
  if (!Number.isSafeInteger(numerator)) {
    throw new RangeError(`Net amount too large for an exact tax at ${vatRate}: ${netCents}`);
  }
  // `RoundingStrategy.roundQuotient`: the remainder decides the tie exactly, where a quotient as double would not.
  const remainder = numerator % denominator;
  const truncated = (numerator - remainder) / denominator;
  const remainderComparedToHalf = Math.abs(remainder) - (denominator - Math.abs(remainder));
  return remainderComparedToHalf >= 0 ? truncated + Math.sign(numerator) : truncated;
}

/** `Money.WithVat.gross`: `netCents` plus the tax rounded on its own, so the gross matches the invoice to the cent. */
export function netToGross(netCents: number, vatRate: number): number {
  return assertSafeCents(netCents + vatOf(netCents, vatRate));
}

/** `MoneyAmount.format(I18nConfiguration.Germany, WhitespaceConfig.Spaces)`: `100000` is `1.000,00 €`. */
export function formatEuro(cents: number): string {
  assertSafeCents(cents);
  const sign = cents < 0 ? "-" : "";
  const absolute = Math.abs(cents);
  const euros = Math.trunc(absolute / 100)
    .toString()
    .replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  return `${sign}${euros},${String(absolute % 100).padStart(2, "0")} €`;
}

/** `VatRate.init`: a rate outside `0..1` or finer than a basis point throws, so the tax stays in integers. */
function basisPointsOf(vatRate: number): number {
  if (!Number.isFinite(vatRate) || vatRate < 0 || vatRate > 1) {
    throw new RangeError(`VAT rate must lie between 0 and 1: ${vatRate}`);
  }
  const basisPoints = Math.round(vatRate * basisPointsPerUnit());
  // `VatRate.BasisPointTolerance`.
  if (Math.abs(vatRate * basisPointsPerUnit() - basisPoints) >= 1e-9) {
    throw new RangeError(`VAT rate ${vatRate} is not a whole number of basis points`);
  }
  return basisPoints;
}

/** `VatRate.BasisPointsPerUnit`. */
function basisPointsPerUnit(): number {
  return 10_000;
}

/** `Money.timesAndRound` refuses an input or result beyond 2^52, where a double no longer holds the half cent it rounds on. */
function assertExact(value: number): void {
  if (Math.abs(value) > 2 ** 52) {
    throw new RangeError(`Cents beyond exact double range: ${value}`);
  }
}

/** A `Long` knows no `-0`; `Object.is` and `toBe` tell it from `0`. */
function withoutNegativeZero(cents: number): number {
  return cents === 0 ? 0 : cents;
}
