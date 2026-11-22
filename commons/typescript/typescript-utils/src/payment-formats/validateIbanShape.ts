import { normalizeToAlphanumericUppercase } from "./normalizeToAlphanumericUppercase";

/** Names the violations of `it.neckar.customer.company.IBAN`, so the check depends on no service's OpenAPI schema. */
export const IbanFormatViolation = {
  InvalidLength: "InvalidLength",
} as const;

export type IbanFormatViolation = (typeof IbanFormatViolation)[keyof typeof IbanFormatViolation];

export type IbanShapeValidationResult = { valid: true } | { valid: false; violation: IbanFormatViolation };

export const IBAN_MIN_LENGTH = 15;
export const IBAN_MAX_LENGTH = 34;

/** Mirrors `it.neckar.customer.company.IBAN`; separators are stripped because the edit form prefills the space-grouped stored value. */
export function validateIbanShape(input: string): IbanShapeValidationResult {
  const cleaned = normalizeToAlphanumericUppercase(input);
  if (cleaned.length < IBAN_MIN_LENGTH || cleaned.length > IBAN_MAX_LENGTH) {
    return { valid: false, violation: IbanFormatViolation.InvalidLength };
  }
  return { valid: true };
}
