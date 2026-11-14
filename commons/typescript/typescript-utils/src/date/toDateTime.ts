/**
 * Converts the given date to a date-time string in ISO 8601 format.
 *
 * @param date
 * @returns string `YYYY-MM-DDTHH:mm:ss.000Z`
 */
export function toDateTime(date: Date): string;
export function toDateTime(date: null): null;
export function toDateTime(date: undefined): undefined;
export function toDateTime(date: Date | null): string | null;
export function toDateTime(date: Date | undefined): string | undefined;
export function toDateTime(date: Date | null | undefined): string | null | undefined;
export function toDateTime(date: Date | null | undefined): string | null | undefined {
  if (date === null) return null;
  if (date === undefined) return undefined;

  return date.toISOString();
}
