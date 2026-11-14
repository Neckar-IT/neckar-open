import { ConfirmationStatus } from "@neckar-hub/typescript-utils/openapi-types";

/**
 * Structural shape of a draft-confirmed entity relevant to the edit gate: its confirmation status
 * and the discriminator of its validation result. Kept structural (`validationResult: { type }`) so
 * it matches every product's generated `ValidationResult<Xxx.ValidationError>` union without
 * importing a product-specific error type.
 */
export interface HasConfirmationAndValidation {
  confirmationStatus: ConfirmationStatus;
  validationResult: { type: string };
}

/**
 * True when a draft still has unresolved validation errors and should be routed to editing.
 * Confirmed entities are always valid, so this returns false for them.
 */
export function needsEditing(entity: HasConfirmationAndValidation): boolean {
  return entity.confirmationStatus === ConfirmationStatus.Draft && entity.validationResult.type !== "Valid";
}
