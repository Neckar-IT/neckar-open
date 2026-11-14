/**
 * Interface for a validation error associated with a draft- confirmed entity
 *
 * @property message - The error message to display
 * @property linkTo - Optional URL to link to for more details about the error
 * @property rowIndex - Zero-based position of the row of a list field the message is about; absent when it is about the field as a whole
 */
export type FieldValidationError = { message: string; linkTo?: string; rowIndex?: number };

/**
 * Flattens a field-keyed validation error record into a flat error list.
 */
export function collectFieldErrors(fieldErrors: Partial<Record<string, FieldValidationError[]>>): FieldValidationError[] {
  return Object.values(fieldErrors)
    .filter((errors) => errors !== undefined)
    .flat();
}
