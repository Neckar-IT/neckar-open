/** Names the violations of `it.neckar.financial.payment.sepa.MandateReference`, which publishes no violation enum. */
export const MandateReferenceFormatViolation = {
  TooLong: "TooLong",
  InvalidCharacters: "InvalidCharacters",
} as const;

export type MandateReferenceFormatViolation = (typeof MandateReferenceFormatViolation)[keyof typeof MandateReferenceFormatViolation];

export type MandateReferenceShapeValidationResult = { valid: true } | { valid: false; violation: MandateReferenceFormatViolation };

export const MANDATE_MAX_LENGTH = 35;
/** SEPA-allowed character set for mandate references. */
const MANDATE_PATTERN = /^[A-Za-z0-9 '+?/\-:().,]+$/;

/** Mirrors `it.neckar.financial.payment.sepa.MandateReference`. */
export function validateMandateReferenceShape(input: string): MandateReferenceShapeValidationResult {
  if (input.length > MANDATE_MAX_LENGTH) {
    return { valid: false, violation: MandateReferenceFormatViolation.TooLong };
  }
  if (!MANDATE_PATTERN.test(input)) {
    return { valid: false, violation: MandateReferenceFormatViolation.InvalidCharacters };
  }
  return { valid: true };
}
