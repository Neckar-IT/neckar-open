import { trimAsKotlin } from "./trimAsKotlin";

/** Names the violations of `it.neckar.financial.payment.card.CardNumber`, which publishes no violation enum. */
export const CardNumberFormatViolation = {
  InvalidDigitCount: "InvalidDigitCount",
} as const;

export type CardNumberFormatViolation = (typeof CardNumberFormatViolation)[keyof typeof CardNumberFormatViolation];

export type CardNumberShapeValidationResult = { valid: true } | { valid: false; violation: CardNumberFormatViolation };

/** `CardNumber.MaskedPattern`: groups count from the right, so a 15-digit card reads `*** **** **** 0005`. */
const MASKED_CARD_PATTERN = /^[*0-9]{1,4}( [*0-9]{4})* [0-9]{4}$/;
export const CARD_MIN_DIGITS = 13;
export const CARD_MAX_DIGITS = 19;

/**
 * Mirrors `CardNumber.invoke`: an already-masked value is the stored number prefilled on edit and passes unchanged. Any other input
 * is a raw PAN and holds 13..19 digits (ISO/IEC 7812), ignoring separators.
 */
export function validateCardNumberShape(input: string): CardNumberShapeValidationResult {
  const trimmed = trimAsKotlin(input);
  if (trimmed.includes("*") && MASKED_CARD_PATTERN.test(trimmed)) {
    return { valid: true };
  }
  // Kotlin's `isDigit()` counts every UTF-16 unit that is a decimal digit, `\d` only ASCII ones.
  const digitCount = trimmed.split("").filter((unit) => /\p{Nd}/u.test(unit)).length;
  if (digitCount < CARD_MIN_DIGITS || digitCount > CARD_MAX_DIGITS) {
    return { valid: false, violation: CardNumberFormatViolation.InvalidDigitCount };
  }
  return { valid: true };
}
