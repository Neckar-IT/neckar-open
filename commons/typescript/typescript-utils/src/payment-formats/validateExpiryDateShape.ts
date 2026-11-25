import type { ExpiryDateParts } from "./expiryDate";

/** Names the violations of `it.neckar.financial.payment.card.ExpiryDate`, which publishes no violation enum. */
export const ExpiryDateFormatViolation = {
  YearOutOfRange: "YearOutOfRange",
  InPast: "InPast",
} as const;

export type ExpiryDateFormatViolation = (typeof ExpiryDateFormatViolation)[keyof typeof ExpiryDateFormatViolation];

export type ExpiryDateShapeValidationResult = { valid: true } | { valid: false; violation: ExpiryDateFormatViolation };

/** The four-digit years the `YYYY-MM` wire form holds; the Kotlin class checks the month against `now` alone. */
const EXPIRY_MIN_YEAR = 1900;
const EXPIRY_MAX_YEAR = 9999;

/** Mirrors `it.neckar.financial.payment.card.ExpiryDate`: a card is valid through the end of its expiry month, so whole months are compared. */
export function validateExpiryDateShape({ year, month }: ExpiryDateParts, now: Date): ExpiryDateShapeValidationResult {
  if (year < EXPIRY_MIN_YEAR || year > EXPIRY_MAX_YEAR) {
    return { valid: false, violation: ExpiryDateFormatViolation.YearOutOfRange };
  }
  if (year * 12 + month < now.getFullYear() * 12 + now.getMonth() + 1) {
    return { valid: false, violation: ExpiryDateFormatViolation.InPast };
  }
  return { valid: true };
}
