import { normalizeToAlphanumericUppercase } from "./normalizeToAlphanumericUppercase";

/** Names the violations of `it.neckar.customer.company.BIC`, so the check depends on no service's OpenAPI schema. */
export const BicFormatViolation = {
  InvalidLength: "InvalidLength",
} as const;

export type BicFormatViolation = (typeof BicFormatViolation)[keyof typeof BicFormatViolation];

export type BicShapeValidationResult = { valid: true } | { valid: false; violation: BicFormatViolation };

export const BIC_SHORT_LENGTH = 8;
export const BIC_LONG_LENGTH = 11;

/** Mirrors `it.neckar.customer.company.BIC`. */
export function validateBicShape(input: string): BicShapeValidationResult {
  const cleaned = normalizeToAlphanumericUppercase(input);
  if (cleaned.length !== BIC_SHORT_LENGTH && cleaned.length !== BIC_LONG_LENGTH) {
    return { valid: false, violation: BicFormatViolation.InvalidLength };
  }
  return { valid: true };
}
